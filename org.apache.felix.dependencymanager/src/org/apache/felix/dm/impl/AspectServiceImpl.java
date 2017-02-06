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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ServiceDependency;
import org.apache.felix.dm.context.DependencyContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AspectServiceImpl extends FilterComponent {
	
	private final String m_add;
	private final String m_change;
	private final String m_remove;
	private final String m_swap;
	private final int m_ranking;
    private final Object m_dependencyCallbackInstance;
    
	public AspectServiceImpl(DependencyManager dm, Class<?> aspectInterface, String aspectFilter, int ranking, String autoConfig, Object callbackInstance, String add, String change, String remove, String swap) {
		super(dm.createComponent());
		m_ranking = ranking;
		m_add = add;
		m_change = change;
		m_remove = remove;
		m_swap = swap;
		m_dependencyCallbackInstance = callbackInstance;
		
		m_component.setImplementation(new AspectImpl(aspectInterface, autoConfig))
			.add(dm.createServiceDependency()
				   .setService(aspectInterface, createDependencyFilterForAspect(aspectFilter))
				   .setAutoConfig(false)
				   .setCallbacks("added", "removed"))
				   .setCallbacks("init", null, "stop", null);
		
//		m_component.setDebug("aspectfactory-" + m_ranking);
	}
	
	private String createDependencyFilterForAspect(String aspectFilter) {
        // we only want to match services which are not themselves aspects
        if (aspectFilter == null || aspectFilter.length() == 0) {
            return "(!(" + DependencyManager.ASPECT + "=*))";
        }
        else {
            return "(&(!(" + DependencyManager.ASPECT + "=*))" + aspectFilter + ")";
        }  
	}
	
    private Hashtable<String, Object> getServiceProperties(ServiceReference<?> originalServiceRef) {
        Hashtable<String, Object> props = new Hashtable<>();
        if (m_serviceProperties != null) {
            Enumeration<String> e = m_serviceProperties.keys();
            while (e.hasMoreElements()) {
                String key = e.nextElement();
                props.put(key, m_serviceProperties.get(key));
            }
        }
        return props;
    }
	
	class AspectImpl extends AbstractDecorator {
		private final Class<?> m_aspectInterface;
		private final String m_autoConfig;

		public AspectImpl(Class<?> aspectInterface, String autoConfig) {
			this.m_aspectInterface = aspectInterface;
			this.m_autoConfig = autoConfig;
		}

        /**
         * Creates an aspect implementation component for a new original service.
         * @param param First entry contains the ref to the original service
         */
		@SuppressWarnings("unchecked")
		@Override
        public Component createService(Object[] params) {
            // Get the new original service reference.
            ServiceReference<?> originalServiceRef = (ServiceReference<Object>) params[0];
            List<DependencyContext> dependencies = m_component.getDependencies();
            // Remove our internal dependency, replace it with one that points to the specific service that just was passed in.
            dependencies.remove(0);
            Hashtable<String, Object> serviceProperties = getServiceProperties(originalServiceRef);
            String[] serviceInterfaces = getServiceInterfaces();
            
            ServiceDependency aspectDependency = (ServiceDependencyImpl) m_manager.createServiceDependency()
            		.setService(m_aspectInterface, createAspectFilter(originalServiceRef))
            		.setRequired(true)
            		.setPropagate(new AspectPropagateCallback(originalServiceRef), "propagateAspectPropertyChange")
            		.setCallbacks(m_dependencyCallbackInstance, m_add,  m_change, m_remove, m_swap);
                    
            //aspectDependency.setDebug("aspect " + m_ranking);
            
            if (m_autoConfig != null) {
                aspectDependency.setAutoConfig(m_autoConfig);
            } else if (m_add == null && m_change == null && m_remove == null && m_swap == null) {
                // Since we have set callbacks, we must reactivate setAutoConfig because user has not specified any callbacks.
                aspectDependency.setAutoConfig(true);
            }
            
            Component service = m_manager.createComponent()
                .setInterface(serviceInterfaces, serviceProperties)
                .setImplementation(m_serviceImpl)
                .setFactory(m_factory, m_factoryCreateMethod) // if not set, no effect
                .setComposition(m_compositionInstance, m_compositionMethod) // if not set, no effect
                .setCallbacks(m_callbackObject, m_init, m_start, m_stop, m_destroy) // if not set, no effect
                .add(aspectDependency);
            
            //service.setDebug("aspectimpl-" + m_ranking);
            
            configureAutoConfigState(service, m_component);            
            copyDependencies(dependencies, service);
            m_stateListeners.forEach(service::add);
            return service;                
        }
                        
        private String[] getServiceInterfaces() {
            List<String> serviceNames = new ArrayList<>();
            // Of course, we provide the aspect interface.
            serviceNames.add(m_aspectInterface.getName());
            // But also append additional aspect implementation interfaces.
            if (m_serviceInterfaces != null) {
                for (int i = 0; i < m_serviceInterfaces.length; i ++) {
                    if (!m_serviceInterfaces[i].equals(m_aspectInterface.getName())) {
                        serviceNames.add(m_serviceInterfaces[i]);
                    }
                }
            }
            return serviceNames.toArray(new String[serviceNames.size()]);
        }
        
        private String createAspectFilter(ServiceReference<?> ref) {
            Long sid = (Long) ref.getProperty(Constants.SERVICE_ID);
            return "(&(|(!(" + Constants.SERVICE_RANKING + "=*))(" + Constants.SERVICE_RANKING + "<=" + (m_ranking - 1) + "))(|(" + Constants.SERVICE_ID + "=" + sid + ")(" + DependencyManager.ASPECT + "=" + sid + ")))";
        }
		
        public String toString() {
            return "Aspect for " + m_aspectInterface.getName();
        }
	}
	
	class AspectPropagateCallback {
        private final ServiceReference<?> m_originalServiceRef;
        
        AspectPropagateCallback(ServiceReference<?> originalServiceRef) {
        	m_originalServiceRef = originalServiceRef;
		}
        
        Hashtable<String, Object> propagateAspectPropertyChange(ServiceReference<?> ref) {
        	// ignore ref, which might come from a lower ranked aspect only apply original service ref. 
            Hashtable<String, Object> props = new Hashtable<>();
            String[] keys = m_originalServiceRef.getPropertyKeys();
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];
                if (ServiceUtil.NOT_PROPAGATABLE_SERVICE_PROPERTIES.contains(key)) {
                    // do not copy this key, which is not propagatable.
                }
                else {
                    props.put(key, m_originalServiceRef.getProperty(key));
                }
            }
            // finally add our aspect property
            props.put(DependencyManager.ASPECT, m_originalServiceRef.getProperty(Constants.SERVICE_ID));
            // and the ranking
            props.put(Constants.SERVICE_RANKING, Integer.valueOf(m_ranking));
            return props;
	    }	    	
	}
}
