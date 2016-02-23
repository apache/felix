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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentDeclaration;
import org.apache.felix.dm.ComponentExecutorFactory;
import org.apache.felix.dm.context.ComponentContext;
import org.osgi.framework.BundleContext;

/**
 * The Dependency Manager delegates all components addition/removal to this class.
 * If a ComponentExecutorFactory is registered in the OSGi registry, this class will use it to get an 
 * Executor used for components management and lifecycle callbacks.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ComponentScheduler {
    private final static ComponentScheduler m_instance = new ComponentScheduler();
    private final static String PARALLEL = "org.apache.felix.dependencymanager.parallel";
    private volatile ComponentExecutorFactory m_componentExecutorFactory;
    private final Executor m_serial = new SerialExecutor(null);
    private ConcurrentMap<Component, Component> m_pending = new ConcurrentHashMap<>();

    public static ComponentScheduler instance() {
        return m_instance;
    }

    protected void bind(final ComponentExecutorFactory componentExecutorFactory) {
        m_componentExecutorFactory = componentExecutorFactory;
        m_serial.execute(new Runnable() {
            @Override
            public void run() {
                for (Component c : m_pending.keySet()) {
                    createComponentExecutor(m_componentExecutorFactory, c);
                    ((ComponentContext) c).start();
                }
                m_pending.clear();
            }
        });
    }

    protected void unbind(ComponentExecutorFactory threadPool) {
        m_componentExecutorFactory = null;
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
                    ComponentExecutorFactory execFactory = m_componentExecutorFactory;
                    if (execFactory == null) {
                        m_pending.put(c, c);
                    }
                    else {
                        createComponentExecutor(execFactory, c);
                        ((ComponentContext) c).start();
                    }
                }
            });
        }
    }

    public void remove(final Component c) {
        m_pending.remove(c);
        ((ComponentContext) c).stop();
    }

    private boolean mayStartNow(Component c) {
        ComponentExecutorFactory execFactory = m_componentExecutorFactory;
        BundleContext ctx = c.getDependencyManager().getBundleContext();
        String parallel = ctx.getProperty(PARALLEL);

        if (execFactory == null) {
            // No ComponentExecutorFactory available. If a "parallel" OSGi system property is specified, 
            // we have to wait for a ComponentExecutorFactory servoce if the component class name is matching one of the 
            // prefixes specified in the "parallel" system property.
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
		createComponentExecutor(execFactory, c);
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
    
    private void createComponentExecutor(ComponentExecutorFactory execFactory, Component c) {
        Executor exec = execFactory.getExecutorFor(c);
        if (exec != null) {
            ((ComponentContext) c).setThreadPool(exec);
        }
    }
}
