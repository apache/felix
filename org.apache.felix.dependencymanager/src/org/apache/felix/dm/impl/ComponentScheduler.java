/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.dm.impl;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executor;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentDeclaration;
import org.apache.felix.dm.context.ComponentContext;
import org.osgi.framework.BundleContext;

/**
 * The Dependency Manager delegates all components addition/removal to this class, which is in charge of tracking
 * a threadpool from the OSGi registry in order to handle components concurrently.
 * 
 * By default, an external management bundle may register a threadpool (java.util.concurrent.Executor) with a
 * special "target=org.apache.felix.dependencymanager" service property. So, if the threadpool is registered, then 
 * any components added will use that threadpool. 
 * 
 * If you want to ensure that all components must wait for a threadpool, before they are actually added
 * in a dependency manager, you can simply use the "org.apache.felix.dependencymanager.parallel" OSGi system 
 * property, which can specify the list of components which  must wait for the threadpool.
 * This property value can be a wildcard ("*"), or a list of components implementation class prefixes 
 * (comma seperated). So, all components class names starting with the specified prefixes will be cached until the 
 * threadpool becomes available.
 * Some class name prefixes can also be negated (using "!"), in order to exclude some components from the list of 
 * components using the threadpool.
 * 
 * Notice that if the threadpool and all the services it may depends on are also declared using the 
 * Dependency Manager API, then you have to list the package of such components with a "!" prefix, in order to 
 * indicate that those components must not wait for a threadpool (since they are part of the threadpool 
 * implementation !).
 * 
 * Examples:
 * 
 * org.apache.felix.dependencymanager.parallel=*   
 *      -> means all components must be cached until a threadpool comes up.
 * 
 * org.apache.felix.dependencymanager.parallel=foo.bar, foo.zoo
 *      -> means only components whose implementation class names is starting with "foo.bar" or "foo.zoo" must wait for and 
 *      use a threadpool.   
 * 
 * org.apache.felix.dependencymanager.parallel=!foo.threadpool, *
 *      -> means all components must wait for and use a threadpool, except the threadpool components implementations class names
 *       (starting with foo.threadpool prefix). 
 *       
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ComponentScheduler {
    private final static ComponentScheduler m_instance = new ComponentScheduler();
    private final static String PARALLEL = "org.apache.felix.dependencymanager.parallel";
    private volatile Executor m_threadPool;
    private final Executor m_serial = new SerialExecutor(null);
    private Set<Component> m_pending = new LinkedHashSet<>();

    public static ComponentScheduler instance() {
        return m_instance;
    }

    protected void bind(final Executor threadPool) {
        m_threadPool = threadPool;
        m_serial.execute(new Runnable() {
            @Override
            public void run() {
                for (Component c : m_pending) {
                    ((ComponentContext) c).setThreadPool(threadPool);
                    ((ComponentContext) c).start();
                }
                m_pending.clear();
            }
        });
    }

    protected void unbind(Executor threadPool) {
        m_threadPool = null;
    }

    public void add(final Component c) {
        if (mayStartNow(c)) {
            ((ComponentContext) c).start();
        }
        else {
            // The component requires a threadpool: delay execution until one is available.
            m_serial.execute(new Runnable() {
                @Override
                public void run() {
                    Executor threadPool = m_threadPool;
                    if (threadPool == null) {
                        m_pending.add(c);
                    }
                    else {
                        ((ComponentContext) c).setThreadPool(threadPool);
                        ((ComponentContext) c).start();
                    }
                }
            });
        }
    }

    public void remove(final Component c) {
        m_serial.execute(new Runnable() {
            @Override
            public void run() {
                if (!m_pending.remove(c)) {
                    ((ComponentContext) c).stop();
                }
            }
        });
    }

    private boolean mayStartNow(Component c) {
        Executor threadPool = m_threadPool;
        BundleContext ctx = c.getDependencyManager().getBundleContext();
        String parallel = ctx.getProperty(PARALLEL);

        if (threadPool == null) {
            // No threadpool available. If a "parallel" OSGi system property is specified, we have to wait for a
            // threadpool if the component class name is matching one of the prefixes specified in the "parallel"
            // system property.
            if (parallel != null && requiresThreadPool(c, parallel)) {
                return false; // wait for a threadpool
            } else {
                return true; // no threadpool required, start the component now, synchronously
            }
        }
        else {
            // A threadpool is there. If the "parallel" OSGi system property is not specified, we can start the component
            // now and we'll use the threadpool for it.
            // But if the "parallel" system property is specified, the component will use the threadpool only if it's
            // classname is starting with one of the prefixes specified in the property.
            if (parallel == null || requiresThreadPool(c, parallel)) {
                ((ComponentContext) c).setThreadPool(threadPool);
            }
            return true; // start the component now, possibly using the threadpool (see above).
        }
    }

    private boolean requiresThreadPool(Component c, String parallel) {
        // The component declared from our DM Activator can not be parallel.
        ComponentDeclaration decl = c.getComponentDeclaration();
        if (ComponentScheduler.class.getName().equals(decl.getName())) {
            return false;
        }

        for (String prefix : parallel.trim().split(",")) {
            prefix = prefix.trim();
            boolean not = prefix.startsWith("!");
            if (not) {
                prefix = prefix.substring(1).trim();
            }
            if ("*".equals(prefix) || c.getComponentDeclaration().getClassName().startsWith(prefix)) {
                return !not;
            }
        }
        return false;
    }
}
