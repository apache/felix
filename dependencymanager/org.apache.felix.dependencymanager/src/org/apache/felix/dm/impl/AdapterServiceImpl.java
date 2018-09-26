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

import org.apache.felix.dm.AdapterComponent;
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
public class AdapterServiceImpl extends FilterComponent<AdapterComponent> implements AdapterComponent {
	
    private volatile Class<?> m_adapteeInterface;
    private volatile String m_adapteeFilter;
    private volatile String m_adapteeAutoConfig;
    private volatile String m_adapteeAdd;
    private volatile String m_adapteeChange;
    private volatile String m_adapteeRemove;
    private volatile String m_adapteeSwap;
    private volatile boolean m_adapteePropage = true;
    private volatile Object m_adapteeCallbackInstance;

	/**
     * Creates a new Adapter Service implementation.
     */
    public AdapterServiceImpl(DependencyManager dm) {
		super(dm.createComponent());
    }
    
    @Override
    public AdapterServiceImpl setAdaptee(Class<?> service, String filter) {
        m_adapteeInterface = service;
        m_adapteeFilter = filter;
        return this;
    }
    @Override
    public AdapterServiceImpl setAdapteeField(String autoConfig) {
        m_adapteeAutoConfig = autoConfig;
        return this;
    }
    @Override
    public AdapterServiceImpl setAdapteeCallbacks(String add, String change, String remove, String swap) {
        m_adapteeAdd = add;
        m_adapteeChange = change;
        m_adapteeRemove = remove;
        m_adapteeSwap = swap;
        return this;
    }
    @Override
    public AdapterServiceImpl setPropagate(boolean propagate) {
        m_adapteePropage = propagate;
        return this;
    }
    @Override
    public AdapterServiceImpl setAdapteeCallbackInstance(Object callbackInstance) {
        m_adapteeCallbackInstance = callbackInstance;
        return this;
    }    
    
    @Override
    protected void startInitial() {
        DependencyManager dm = getDependencyManager();
        m_component
        	.setImplementation(new AdapterImpl())            
        	.setCallbacks("init", null, "stop", null)
        	.add(dm.createServiceDependency()
                   .setService(m_adapteeInterface, m_adapteeFilter)
                   .setAutoConfig(false)
                   .setCallbacks("added", null, "removed", "swapped")); 
    }	
    
    public class AdapterImpl extends AbstractDecorator {
        
        public Component createService(Object[] properties) {
            ServiceReference<?> ref = (ServiceReference<?>) properties[0]; 
            Object aspect = ref.getProperty(DependencyManager.ASPECT);            
            String serviceIdToTrack = (aspect != null) ? aspect.toString() : ref.getProperty(Constants.SERVICE_ID).toString();
            List<DependencyContext> dependencies = m_component.getDependencies();
            dependencies.remove(0);
            ServiceDependency dependency = m_manager.createServiceDependency()
            	 // create a dependency on both the service id we're adapting and possible aspects for this given service id
            	 .setService(m_adapteeInterface, "(|(" + Constants.SERVICE_ID + "=" + serviceIdToTrack 
            			 	+ ")(" + DependencyManager.ASPECT + "=" + serviceIdToTrack + "))")
                 .setRequired(true);
            if (m_adapteeAdd != null || m_adapteeChange != null || m_adapteeRemove != null || m_adapteeSwap != null) {
                dependency.setCallbacks(m_adapteeCallbackInstance, m_adapteeAdd, m_adapteeChange, m_adapteeRemove, m_adapteeSwap);
            }
            if (m_adapteeAutoConfig != null) {
                dependency.setAutoConfig(m_adapteeAutoConfig);
            }
            
            if (m_adapteePropage) {
                dependency.setPropagate(this, "propagateAdapteeProperties");
            }
            
//            dependency.setDebug("AdapterDependency#" + m_adapteeInterface.getSimpleName());

            Component service = m_manager.createComponent()
                .setInterface(m_serviceInterfaces, getServiceProperties(ref))
                .setImplementation(m_serviceImpl)
                .setFactory(m_factory, m_factoryCreateMethod) // if not set, no effect
                .setComposition(m_compositionInstance, m_compositionMethod) // if not set, no effect
                .setCallbacks(m_callbackObject, m_init, m_start, m_stop, m_destroy) // if not set, no effect
                .setScope(m_scope)
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
        
        public Dictionary<String, Object> getServiceProperties(ServiceReference<?> ref) {
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
        
        public Dictionary<String, Object> propagateAdapteeProperties(ServiceReference<?> ref) {
            Dictionary<String, Object> props = new Hashtable<>();
            String[] keys = ref.getPropertyKeys();
            for (int i = 0; i < keys.length; i++) {
                String key = keys[i];
                if (ServiceUtil.NOT_PROPAGATABLE_SERVICE_PROPERTIES.contains(key)) {
                    // do not copy this key which is not propagatable.
                }
                else {
                	props.put(key, ref.getProperty(key));
                }
            }
            return props;
        }
    }
}
