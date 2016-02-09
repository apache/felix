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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentStateListener;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ServiceDependency;
import org.apache.felix.dm.context.DependencyContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * Adapter Service implementation. This class extends the FilterService in order to catch
 * some Service methods for configuring actual adapter service implementation.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AdapterServiceImpl extends FilterComponent {
    /**
     * Creates a new Adapter Service implementation.
     * 
     * @param dm the dependency manager used to create our internal adapter service
     * @param adapteeInterface the service interface to apply the adapter to
     * @param adapteeFilter the filter condition to use with the service interface
     * @param autoConfig the name of the member to inject the service into
     * @param callbackInstance the instance to invoke the callback on, or null 
     * @param add name of the callback method to invoke on add
     * @param change name of the callback method to invoke on change
     * @param remove name of the callback method to invoke on remove
     * @param swap name of the callback method to invoke on swap
     * @param propagate true if the adaptee service properties should be propagated to the adapter service consumers
     */
    public AdapterServiceImpl(DependencyManager dm, Class<?> adapteeInterface, String adapteeFilter, String autoConfig, 
        Object callbackInstance, String add, String change, String remove, String swap, boolean propagate)
    {
        super(dm.createComponent()); // This service will be filtered by our super class, allowing us to take control.
        m_component.setImplementation(new AdapterImpl(adapteeInterface, adapteeFilter, autoConfig, callbackInstance, add, change, remove, swap, propagate))            
            .add(dm.createServiceDependency()
                   .setService(adapteeInterface, adapteeFilter)
                   .setAutoConfig(false)
                   .setCallbacks("added", null, "removed", "swapped"))
            .setCallbacks("init", null, "stop", null);
    }	
	    
    public class AdapterImpl extends AbstractDecorator {
        private final Class<?> m_adapteeInterface;
        private final String m_adapteeFilter;
        private final Object m_dependencyCallbackInstance;
        private final String m_add;
        private final String m_change;
        private final String m_remove;
        private final String m_swap;
        private final String m_autoConfig;
        private final boolean m_propagate;
        
        public AdapterImpl(Class<?> adapteeInterface, String adapteeFilter, String autoConfig, Object callbackInstance, String add, 
            String change, String remove, String swap, boolean propagate) {
            m_adapteeInterface = adapteeInterface;
            m_adapteeFilter = adapteeFilter;
            m_autoConfig = autoConfig;
            m_dependencyCallbackInstance = callbackInstance;
            m_add = add;
            m_change = change;
            m_swap = swap;
            m_remove = remove;
            m_propagate = propagate;
        }
        
        public Component createService(Object[] properties) {
            ServiceReference ref = (ServiceReference) properties[0]; 
            Object aspect = ref.getProperty(DependencyManager.ASPECT);            
            String serviceIdToTrack = (aspect != null) ? aspect.toString() : ref.getProperty(Constants.SERVICE_ID).toString();
            List<DependencyContext> dependencies = m_component.getDependencies();
            dependencies.remove(0);
            ServiceDependency dependency = m_manager.createServiceDependency()
            	 // create a dependency on both the service id we're adapting and possible aspects for this given service id
            	 .setService(m_adapteeInterface, "(|(" + Constants.SERVICE_ID + "=" + serviceIdToTrack 
            			 	+ ")(" + DependencyManager.ASPECT + "=" + serviceIdToTrack + "))")
                 .setRequired(true);
            if (m_add != null || m_change != null || m_remove != null || m_swap != null) {
                dependency.setCallbacks(m_dependencyCallbackInstance, m_add, m_change, m_remove, m_swap);
            }
            if (m_autoConfig != null) {
                dependency.setAutoConfig(m_autoConfig);
            }
            
            if (m_propagate) {
                dependency.setPropagate(this, "propagateAdapteeProperties");
            }
            
//            dependency.setDebug("AdapterDependency#" + m_adapteeInterface.getSimpleName());

            Component service = m_manager.createComponent()
                .setInterface(m_serviceInterfaces, getServiceProperties(ref))
                .setImplementation(m_serviceImpl)
                .setFactory(m_factory, m_factoryCreateMethod) // if not set, no effect
                .setComposition(m_compositionInstance, m_compositionMethod) // if not set, no effect
                .setCallbacks(m_callbackObject, m_init, m_start, m_stop, m_destroy) // if not set, no effect
                .add(dependency);
            
            configureAutoConfigState(service, m_component);
            copyDependencies(dependencies, service);

            for (ComponentStateListener stateListener : m_stateListeners) {
                service.add(stateListener);
            }
            return service;
        }
        
        public String toString() {
            return "Adapter for " + m_adapteeInterface + ((m_adapteeFilter != null) ? " with filter " + m_adapteeFilter : "");
        }
        
        public Dictionary<String, Object> getServiceProperties(ServiceReference ref) {
            Dictionary<String, Object> props = new Hashtable<>();
            if (m_serviceProperties != null) {
                Enumeration<String> e = m_serviceProperties.keys();
                while (e.hasMoreElements()) {
                    String key = e.nextElement();
                    props.put(key, m_serviceProperties.get(key));
                }
            }
            return props;
        }
        
        public Dictionary<String, Object> propagateAdapteeProperties(ServiceReference ref) {
            Dictionary<String, Object> props = new Hashtable<>();
            String[] keys = ref.getPropertyKeys();
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];
                if (key.equals(DependencyManager.ASPECT) || key.equals(Constants.SERVICE_ID) || key.equals(Constants.SERVICE_RANKING) || key.equals(Constants.OBJECTCLASS)) {
                    // do not copy these either
                }
                else {
                    props.put(key, ref.getProperty(key));
                }
            }
            return props;
        }
    }
}
