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
package dm.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import dm.Component;
import dm.ComponentStateListener;
import dm.Dependency;
import dm.DependencyManager;
import dm.ServiceDependency;
import dm.context.DependencyContext;

public class AspectServiceImpl extends FilterComponent {
	
	private final String m_add;
	private final String m_change;
	private final String m_remove;
	private final String m_swap;
	private int m_ranking;

	public AspectServiceImpl(DependencyManager dm, Class<?> aspectInterface, String aspectFilter, int ranking, String autoConfig, String add, String change, String remove, String swap) {
		super(dm.createComponent());
		this.m_ranking = ranking;
		this.m_add = add;
		this.m_change = change;
		this.m_remove = remove;
		this.m_swap = swap;
		
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
	
    private Properties getServiceProperties(ServiceReference ref) {
        Properties props = new Properties();
        String[] keys = ref.getPropertyKeys();
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            if (key.equals(Constants.SERVICE_ID) || key.equals(Constants.SERVICE_RANKING) || key.equals(DependencyManager.ASPECT) || key.equals(Constants.OBJECTCLASS)) {
                // do not copy these
            }
            else {
                props.put(key, ref.getProperty(key));
            }
        }
        if (m_serviceProperties != null) {
            Enumeration<?> e = m_serviceProperties.keys();
            while (e.hasMoreElements()) {
                Object key = e.nextElement();
                props.put(key, m_serviceProperties.get(key));
            }
        }
        // finally add our aspect property
        props.put(DependencyManager.ASPECT, ref.getProperty(Constants.SERVICE_ID));
        // and the ranking
        props.put(Constants.SERVICE_RANKING, Integer.valueOf(m_ranking));
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
		@Override
        public Component createService(Object[] params) {
            // Get the new original service reference.
            ServiceReference ref = (ServiceReference) params[0];
            List<DependencyContext> dependencies = m_component.getDependencies();
            // remove our internal dependency, replace it with one that points to the specific service that just was passed in.
            dependencies.remove(0);
            Properties serviceProperties = getServiceProperties(ref);
            String[] serviceInterfaces = getServiceInterfaces();
            
            ServiceDependency aspectDependency = (ServiceDependencyImpl) 
                    m_manager.createServiceDependency().setService(m_aspectInterface, createAspectFilter(ref)).setRequired(true);
            //aspectDependency.setDebug("aspect " + m_ranking);

            aspectDependency.setCallbacks(new CallbackProxy(aspectDependency, ref), 
                            m_add != null ? "addAspect" : null, 
                            "changeAspect", // We have to propagate in case aspect does not have a change callback
                            m_remove != null ? "removeAspect" : null, 
                            m_swap != null ? "swapAspect" : null);
            
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
            
            for (DependencyContext dc : dependencies) {
                service.add((Dependency) dc.createCopy());
            }

            for (int i = 0; i < m_stateListeners.size(); i++) {
                service.add((ComponentStateListener) m_stateListeners.get(i));
            }
            return service;                
        }
        
        /**
         * Modify some specific aspect service properties.
         */
		@Override
        public void setServiceProperties(Dictionary props) {
            Map<Object, Component> services = super.getServices();
            Iterator it = services.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                ServiceReference originalServiceRef = (ServiceReference) entry.getKey();
                Component c = (Component) entry.getValue();
                // m_serviceProperties is already set to the new service properties; and the getServiceProperties will
                // merge m_serviceProperties with the original service properties.
                Dictionary newProps = getServiceProperties(originalServiceRef);                
                c.setServiceProperties(newProps);
            }
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
            return (String[]) serviceNames.toArray(new String[serviceNames.size()]);
        }
        
        private String createAspectFilter(ServiceReference ref) {
            Long sid = (Long) ref.getProperty(Constants.SERVICE_ID);
            return "(&(|(!(" + Constants.SERVICE_RANKING + "=*))(" + Constants.SERVICE_RANKING + "<=" + (m_ranking - 1) + "))(|(" + Constants.SERVICE_ID + "=" + sid + ")(" + DependencyManager.ASPECT + "=" + sid + ")))";
        }
		
	}
	
    class CallbackProxy {
        private final ServiceDependencyImpl m_aspectDependency;
        private final ServiceReference m_originalServiceRef;

        CallbackProxy(ServiceDependency aspectDependency, ServiceReference originalServiceRef) {
            m_aspectDependency = (ServiceDependencyImpl) aspectDependency;
            m_originalServiceRef = originalServiceRef;
        }

        @SuppressWarnings("unused")
		private void addAspect(Component c, ServiceReference ref, Object service) {
            // Just forward "add" service dependency callback.
        	
        	// Invoke is done on dependency.getInstances() which unfortunately returns this callback instance...
        	ServiceEventImpl event = new ServiceEventImpl(ref, service);
        	m_aspectDependency.invoke(m_add, event, m_aspectDependency.getComponentContext().getInstances());
        }

        @SuppressWarnings("unused")
		private void changeAspect(Component c, ServiceReference ref, Object service) {
            // Invoke "change" service dependency callback
            if (m_change != null) {
            	ServiceEventImpl event = new ServiceEventImpl(ref, service);
                m_aspectDependency.invoke(m_change, event, m_aspectDependency.getComponentContext().getInstances());
            }
            // Propagate change to immediate higher aspect, or to client using our aspect.
            // We always propagate our own properties, and the ones from the original service, but we don't inherit
            // from lower ranked aspect service properties.
            Dictionary<?,?> props = getServiceProperties(m_originalServiceRef);
            c.setServiceProperties(props);
        }

        @SuppressWarnings("unused")
		private void removeAspect(Component c, ServiceReference ref, Object service) {
            // Just forward "remove" service dependency callback.
        	ServiceEventImpl event = new ServiceEventImpl(ref, service);
        	m_aspectDependency.invoke(m_remove, event, m_aspectDependency.getComponentContext().getInstances());
        }

        @SuppressWarnings("unused")
		private void swapAspect(Component c, ServiceReference prevRef, Object prev, ServiceReference currRef,
                                Object curr) {
        	Object[] instances = m_aspectDependency.getComponentContext().getInstances();
        	
        	// TODO ASPECTS: It sometimes appears (mostly on component/dependency remove) the size of the instances array is 0, meaning the component
        	// is no longer registered. This should not happen! Figure out why it happens anyway. 
//        	System.out.println("[proxy] swapAspect..." + instances.length);
        	
            // Just forward "swap" service dependency callback.
        	m_aspectDependency.invokeSwap(m_swap, prevRef, prev, currRef, curr, m_aspectDependency.getComponentContext().getInstances());
        }
        
        @Override
        public String toString() {
        	return "CallbackProxy";
        }
    }

}
