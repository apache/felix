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

import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentStateListener;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ResourceDependency;

/**
 * Resource adapter service implementation. This class extends the FilterService in order to catch
 * some Service methods for configuring actual resource adapter service implementation.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ResourceAdapterServiceImpl extends FilterService {
    private Object m_callbackInstance = null;
    private String m_callbackChanged = "changed";
    private String m_callbackAdded = "setResource";
    
    /**
     * Creates a new Resource Adapter Service implementation.
     * @param dm the dependency manager used to create our internal adapter service
     */
    public ResourceAdapterServiceImpl(DependencyManager dm, String resourceFilter, boolean propagate, Object callbackInstance, String callbackSet, String callbackChanged) {
        super(dm.createComponent()); // This service will be filtered by our super class, allowing us to take control.
        m_callbackInstance = callbackInstance;
        m_callbackAdded = callbackSet;
        m_callbackChanged = callbackChanged;
        m_component.setImplementation(new ResourceAdapterImpl(propagate))
            .add(dm.createResourceDependency()
                 .setFilter(resourceFilter)
                 .setAutoConfig(false)
                 .setCallbacks("added", "removed"))
            .setCallbacks("init", null, "stop", null);
    }
    
    public ResourceAdapterServiceImpl(DependencyManager dm, String resourceFilter, Object propagateCallbackInstance, String propagateCallbackMethod, Object callbackInstance, String callbackSet, String callbackChanged) {
        super(dm.createComponent()); // This service will be filtered by our super class, allowing us to take control.
        m_callbackInstance = callbackInstance;
        m_callbackAdded = callbackSet;
        m_callbackChanged = callbackChanged;
        m_component.setImplementation(new ResourceAdapterImpl(propagateCallbackInstance, propagateCallbackMethod))
            .add(dm.createResourceDependency()
                 .setFilter(resourceFilter)
                 .setAutoConfig(false)
                 .setCallbacks("added", "removed"))
            .setCallbacks("init", null, "stop", null);
    }   

    public class ResourceAdapterImpl extends AbstractDecorator {
        private final boolean m_propagate;
        private final Object m_propagateCallbackInstance;
        private final String m_propagateCallbackMethod;

        public ResourceAdapterImpl(boolean propagate) {
            this(propagate, null, null);
        }

        public ResourceAdapterImpl(Object propagateCallbackInstance, String propagateCallbackMethod) {
            this(true, propagateCallbackInstance, propagateCallbackMethod);
        }
        
        private ResourceAdapterImpl(boolean propagate, Object propagateCallbackInstance, String propagateCallbackMethod) {
            m_propagate = propagate;
            m_propagateCallbackInstance = propagateCallbackInstance;
            m_propagateCallbackMethod = propagateCallbackMethod;
        }

        public Component createService(Object[] properties) {
            URL resource = (URL) properties[0]; 
            Properties props = new Properties();
            if (m_serviceProperties != null) {
                Enumeration e = m_serviceProperties.keys();
                while (e.hasMoreElements()) {
                    Object key = e.nextElement();
                    props.put(key, m_serviceProperties.get(key));
                }
            }
            List dependencies = m_component.getDependencies();
            // the first dependency is always the dependency on the resource, which
            // will be replaced with a more specific dependency below
            dependencies.remove(0);
            ResourceDependency resourceDependency = m_manager.createResourceDependency()
                 .setResource(resource)
                 .setCallbacks(m_callbackInstance, m_callbackAdded, m_callbackChanged, null)
                 .setAutoConfig(m_callbackAdded == null)
                 .setRequired(true);
            if (m_propagateCallbackInstance != null && m_propagateCallbackMethod != null) {
                resourceDependency.setPropagate(m_propagateCallbackInstance, m_propagateCallbackMethod);
            } else {
                resourceDependency.setPropagate(m_propagate);
            }
            Component service = m_manager.createComponent()
                .setInterface(m_serviceInterfaces, props)
                .setImplementation(m_serviceImpl)
                .setFactory(m_factory, m_factoryCreateMethod) // if not set, no effect
                .setComposition(m_compositionInstance, m_compositionMethod) // if not set, no effect
                .setCallbacks(m_callbackObject, m_init, m_start, m_stop, m_destroy) // if not set, no effect
                .add(resourceDependency);
            
            configureAutoConfigState(service, m_component);

            for (int i = 0; i < dependencies.size(); i++) {
                service.add(((Dependency) dependencies.get(i)).createCopy());
            }

            for (int i = 0; i < m_stateListeners.size(); i ++) {
                service.addStateListener((ComponentStateListener) m_stateListeners.get(i));
            }
            return service;
        }
    }
}
