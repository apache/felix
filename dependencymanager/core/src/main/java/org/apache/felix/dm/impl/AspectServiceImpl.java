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
 * Aspect Service implementation. This class extends the FilterService in order to catch
 * some Service methods for configuring actual aspect service implementation.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AspectServiceImpl extends FilterService {
    public AspectServiceImpl(DependencyManager dm, Class aspectInterface, String aspectFilter, int ranking, String autoConfig, String add, String change, String remove, String swap)
    { 
        super(dm.createComponent()); // This service will be filtered by our super class, allowing us to take control.
        m_component.setImplementation(new AspectImpl(aspectInterface, aspectFilter, ranking, autoConfig, add, change, remove, swap))
             .add(dm.createServiceDependency()
                  .setService(aspectInterface, createDependencyFilterForAspect(aspectFilter))
                  .setAutoConfig(false)
                  .setCallbacks("added", "removed"));
    }

    private String createDependencyFilterForAspect(String filter) {
        // we only want to match services which are not themselves aspects
        if (filter == null || filter.length() == 0) {
            return "(!(" + DependencyManager.ASPECT + "=*))";
        }
        else {
            return "(&(!(" + DependencyManager.ASPECT + "=*))" + filter + ")";
        }        
    }
    
    /**
     * This class is the Aspect Implementation. It will create the actual Aspect Service, and
     * will use the Aspect Service parameters provided by our enclosing class.
     */
    class AspectImpl extends AbstractDecorator {
        private final Class m_aspectInterface; // the service decorated by this aspect
        private final String m_aspectFilter; // the service filter decorated by this aspect
        private final int m_ranking; // the aspect ranking
        private final String m_autoConfig; // the aspect impl field name where to inject decorated service
        private final String m_add;
        private final String m_change;
        private final String m_remove;
        private final String m_swap;
      
        public AspectImpl(Class aspectInterface, String aspectFilter, int ranking, String autoConfig, String add, String change, String remove, String swap) {
            m_aspectInterface = aspectInterface;
            m_aspectFilter = aspectFilter;
            m_ranking = ranking;
            m_autoConfig = autoConfig;
            m_add = add;
            m_change = change;
            m_remove = remove;
            m_swap = swap;
        }
        
        public Component createService(Object[] params) {
            List dependencies = m_component.getDependencies();
            // remove our internal dependency
            dependencies.remove(0);
            // replace it with one that points to the specific service that just was passed in
            Properties serviceProperties = getServiceProperties(params);
            String[] serviceInterfaces = getServiceInterfaces();
            ServiceReference ref = (ServiceReference) params[0];
            ServiceDependency dependency = m_manager.createServiceDependency().setService(m_aspectInterface, createAspectFilter(ref)).setRequired(true);
            if (m_autoConfig != null) {
                dependency.setAutoConfig(m_autoConfig);
            }
            if (m_add != null || m_change != null || m_remove != null || m_swap != null) {
                dependency.setCallbacks(m_add, m_change, m_remove, m_swap);
            }
            Component service = m_manager.createComponent()
                .setInterface(serviceInterfaces, serviceProperties)
                .setImplementation(m_serviceImpl)
                .setFactory(m_factory, m_factoryCreateMethod) // if not set, no effect
                .setComposition(m_compositionInstance, m_compositionMethod) // if not set, no effect
                .setCallbacks(m_callbackObject, m_init, m_start, m_stop, m_destroy) // if not set, no effect
                .add(dependency);
            
            configureAutoConfigState(service, m_component);
            
            for (int i = 0; i < dependencies.size(); i++) {
                service.add(((Dependency) dependencies.get(i)).createCopy());
            }

            for (int i = 0; i < m_stateListeners.size(); i++) {
                service.addStateListener((ComponentStateListener) m_stateListeners.get(i));
            }
            return service;                
        }
        
        private Properties getServiceProperties(Object[] params) {
            ServiceReference ref = (ServiceReference) params[0]; 
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
                Enumeration e = m_serviceProperties.keys();
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
        
        private String[] getServiceInterfaces() {
            List serviceNames = new ArrayList();
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
}
