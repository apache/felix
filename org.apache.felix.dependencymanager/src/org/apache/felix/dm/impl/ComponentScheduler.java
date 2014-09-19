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

import java.util.Dictionary;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Executor;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentDeclaration;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.context.ComponentContext;

/**
 * When a DependencyManager is not explicitly configured with a threadpool, and when parallel mode is enabled,
 * then added components are delegated to this class, which will cache all added components until one threadpool
 * is registered in the OSGi service registry.
 *  
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ComponentScheduler {
    private final static ComponentScheduler m_instance = new ComponentScheduler();
    private volatile Executor m_threadPool;
    private final Executor m_serial = new SerialExecutor(null);
    private boolean m_started;
    private Set<Component> m_pending = new LinkedHashSet<>();

    public static ComponentScheduler instance() {
        return m_instance;
    }

    protected void start() {
        m_serial.execute(new Runnable() {
            @Override
            public void run() {
                m_started = true;
                for (Component c : m_pending) {
                    addUsingThreadPool(c);
                }
                m_pending.clear();
            }
        });
    }

    public void add(final Component c) {
        if (!isParallelComponent(c)) {
            ((ComponentContext) c).start();
        }
        else {
            m_serial.execute(new Runnable() {
                @Override
                public void run() {
                    if (!m_started) {
                        m_pending.add(c);
                    }
                    else {
                        addUsingThreadPool(c);
                    }
                }
            });
        }
    }

    public void remove(final Component c) {
        if (!isParallelComponent(c)) {
            ((ComponentContext) c).stop();
        }
        else {
            m_serial.execute(new Runnable() {
                @Override
                public void run() {
                    if (!m_started) {
                        m_pending.remove(c);
                    }
                    else {
                        ((ComponentContext) c).stop();
                    }
                }
            });
        }
    }

    private void addUsingThreadPool(Component c) {
        ((ComponentContext) c).setThreadPool(m_threadPool);
        ((ComponentContext) c).start();
    }

    private boolean isParallelComponent(Component c) {
        // The component declared from our DM Activator can not be parallel.
        ComponentDeclaration decl = c.getComponentDeclaration();
        if (ComponentScheduler.class.getName().equals(decl.getName())) {
            return false;
        }

        // A threadpool component declared by a "management agent" using DM API cannot be itself parallel.
        String[] services = decl.getServices();
        if (services != null) {
            for (String service : services) {
                if (Executor.class.getName().equals(service)) {
                    Dictionary<?, ?> props = decl.getServiceProperties();
                    if (props != null && DependencyManager.THREADPOOL.equals(props.get("target"))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
