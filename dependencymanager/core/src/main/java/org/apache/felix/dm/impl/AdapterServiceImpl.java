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
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentStateListener;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ServiceDependency;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * Adapter Service implementation. This class extends the FilterService in order to catch
 * some Service methods for configuring actual adapter service implementation.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AdapterServiceImpl extends FilterService {
    /**
     * Creates a new Adapter Service implementation.
     * 
     * @param dm the dependency manager used to create our internal adapter service
     * @param adapteeInterface the service interface to apply the adapter to
     * @param adapteeFilter the filter condition to use with the service interface
     * @param add
     * @param change
     * @param remove
     */
    public AdapterServiceImpl(DependencyManager dm, Class adapteeInterface, String adapteeFilter, String autoConfig, String add, String change, String remove, String swap) {
        super(dm.createComponent()); // This service will be filtered by our super class, allowing us to take control.
        m_component.setImplementation(new AdapterImpl(adapteeInterface, adapteeFilter, autoConfig, add, change, remove, swap))
                 .add(dm.createServiceDependency()
                      .setService(adapteeInterface, adapteeFilter)
                      .setAutoConfig(false)
                      .setCallbacks("added", null, "removed", "swapped"))
                 .setCallbacks("init", null, "stop", null);
    }	
	
    public AdapterServiceImpl(DependencyManager dm, Class adapteeInterface, String adapteeFilter, String autoConfig, String add, String change, String remove) {
        super(dm.createComponent()); // This service will be filtered by our super class, allowing us to take control.
        m_component.setImplementation(new AdapterImpl(adapteeInterface, adapteeFilter, autoConfig, add, change, remove, null))
                 .add(dm.createServiceDependency()
                      .setService(adapteeInterface, adapteeFilter)
                      .setAutoConfig(false)
                      .setCallbacks("added", null, "removed", "swapped"))
                 .setCallbacks("init", null, "stop", null);
    }
    
    public class AdapterImpl extends AbstractDecorator {
        private final Class m_adapteeInterface;
        private final String m_adapteeFilter;
        private final String m_add;
        private final String m_change;
        private final String m_remove;
        private final String m_swap;
        private final String m_autoConfig;
        
        public AdapterImpl(Class adapteeInterface, String adapteeFilter, String autoConfig, String add, String change, String remove, String swap) {
            m_adapteeInterface = adapteeInterface;
            m_adapteeFilter = adapteeFilter;
            m_autoConfig = autoConfig;
            m_add = add;
            m_change = change;
            m_swap = swap;
            m_remove = remove;
        }
        
        public Component createService(Object[] properties) {
            ServiceReference ref = (ServiceReference) properties[0]; 
            Properties props = new Properties();
            String[] keys = ref.getPropertyKeys();
            String serviceIdToTrack = null;
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];
                if (key.equals(DependencyManager.ASPECT)) {
                	// if we're handed an aspect fetch the aspect property as the service id to track, but do not copy it
                	serviceIdToTrack = ref.getProperty(key).toString();
                }
                if (key.equals(DependencyManager.ASPECT) || key.equals(Constants.SERVICE_ID) || key.equals(Constants.SERVICE_RANKING) || key.equals(Constants.OBJECTCLASS)) {
                    // do not copy these either
                }
                else {
                    props.put(key, ref.getProperty(key));
                }
            }
            if (serviceIdToTrack == null) {
            	// we're not handed an aspect so we can use the service id to track
            	serviceIdToTrack = ref.getProperty(Constants.SERVICE_ID).toString();
            }
            if (m_serviceProperties != null) {
                Enumeration e = m_serviceProperties.keys();
                while (e.hasMoreElements()) {
                    Object key = e.nextElement();
                    props.put(key, m_serviceProperties.get(key));
                }
            }
            List dependencies = m_component.getDependencies();
            dependencies.remove(0);
            ServiceDependency dependency = m_manager.createServiceDependency()
            	 // create a dependency on both the service id we're adapting and possible aspects for this given service id
            	 .setService(m_adapteeInterface, "(|(" + Constants.SERVICE_ID + "=" + serviceIdToTrack 
            			 	+ ")(" + DependencyManager.ASPECT + "=" + serviceIdToTrack + "))")
                 .setRequired(true);
            if (m_autoConfig != null) {
                dependency.setAutoConfig(m_autoConfig);
            }
            if (m_add != null || m_change != null || m_remove != null || m_swap != null) {
                dependency.setCallbacks(m_add, m_change, m_remove, m_swap);
            }

            Component service = m_manager.createComponent()
                .setInterface(m_serviceInterfaces, props)
                .setImplementation(m_serviceImpl)
                .setFactory(m_factory, m_factoryCreateMethod) // if not set, no effect
                .setComposition(m_compositionInstance, m_compositionMethod) // if not set, no effect
                .setCallbacks(m_callbackObject, m_init, m_start, m_stop, m_destroy) // if not set, no effect
                .add(dependency);
            
            configureAutoConfigState(service, m_component);
            
            for (int i = 0; i < dependencies.size(); i++) {
                service.add(((Dependency) dependencies.get(i)).createCopy());
            }
            
            for (int i = 0; i < m_stateListeners.size(); i ++) {
                service.addStateListener((ComponentStateListener) m_stateListeners.get(i));
            }
            return service;
        }
        public String toString() {
            return "Adapter for " + m_adapteeInterface + ((m_adapteeFilter != null) ? " with filter " + m_adapteeFilter : "");
        }
    }
}
