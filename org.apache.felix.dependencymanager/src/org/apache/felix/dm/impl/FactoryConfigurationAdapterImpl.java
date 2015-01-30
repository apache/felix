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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.Logger;
import org.apache.felix.dm.PropertyMetaData;
import org.apache.felix.dm.context.DependencyContext;
import org.apache.felix.dm.impl.metatype.MetaTypeProviderImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.metatype.MetaTypeProvider;
import org.osgi.service.metatype.ObjectClassDefinition;

/**
 * Factory configuration adapter service implementation. This class extends the FilterService in order to catch
 * some Service methods for configuring actual adapter service implementation.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FactoryConfigurationAdapterImpl extends FilterComponent {
    // Our Managed Service Factory PID
    protected final String m_factoryPid;
    
    // Our logger
    protected final Logger m_logger;
    
    public FactoryConfigurationAdapterImpl(DependencyManager dm, String factoryPid, String update, boolean propagate) {
        super(dm.createComponent()); // This service will be filtered by our super class, allowing us to take control.
        m_factoryPid = factoryPid;
        m_logger = ((ComponentImpl) m_component).getLogger();

        Hashtable<String, Object> props = new Hashtable<>();
        props.put(Constants.SERVICE_PID, factoryPid);
        m_component
            .setInterface(ManagedServiceFactory.class.getName(), props)
            .setImplementation(new AdapterImpl(update, propagate))
            .setCallbacks("init", null, "stop", null);
    }
    
    public FactoryConfigurationAdapterImpl(DependencyManager dm, String factoryPid, String update, boolean propagate,
        BundleContext bctx, Logger logger, String heading, String description, String localization, PropertyMetaData[] properyMetaData) {
        super(dm.createComponent()); // This service will be filtered by our super class, allowing us to take control.
        m_factoryPid = factoryPid;
        m_logger = logger;
        Hashtable<String, Object> props = new Hashtable<>();
        props.put(Constants.SERVICE_PID, factoryPid);
        m_component
            .setInterface(ManagedServiceFactory.class.getName(), props)
            .setImplementation(new MetaTypeAdapterImpl(update, propagate,
                bctx, logger, heading, description,
                localization, properyMetaData))
            .setCallbacks("init", null, "stop", null);
    }
    
    public String getName() {
        return "Adapter for factory pid " + m_factoryPid;
    }
    
    /**
     * Creates, updates, or removes a service, when a ConfigAdmin factory configuration is created/updated or deleted.
     */
    public class AdapterImpl extends AbstractDecorator implements ManagedServiceFactory {        
        // The adapter "update" method used to provide the configuration
        protected final String m_update;

        // Tells if the CM config must be propagated along with the adapter service properties
        protected final boolean m_propagate;

        /**
         * Creates a new CM factory configuration adapter.
         * 
         * @param factoryPid
         * @param updateMethod
         * @param adapterInterface
         * @param adapterImplementation
         * @param adapterProperties
         * @param propagate
         */
        public AdapterImpl(String updateMethod, boolean propagate) {
            m_update = updateMethod;
            m_propagate = propagate;
        }

        /**
         * Returns the managed service factory name.
         */
        public String getName() {
            return m_factoryPid;
        }
      
        /**
         * Method called from our superclass, when we need to create a service.
         */
        @SuppressWarnings("unchecked")
        public Component createService(Object[] properties) {
            Dictionary<String, ?> settings = (Dictionary<String, ?>) properties[0];     
            Component newService = m_manager.createComponent();        
            Object impl = null;
            
            try {
                if (m_serviceImpl != null) {
                    impl = (m_serviceImpl instanceof Class) ? ((Class<?>) m_serviceImpl).newInstance() : m_serviceImpl;
                }
                else {
                    impl = instantiateFromFactory(m_factory, m_factoryCreateMethod);
                }
                InvocationUtil.invokeCallbackMethod(impl, m_update, 
                    new Class[][] {{ Dictionary.class }, {}}, 
                    new Object[][] {{ settings }, {}});
            }
            
            catch (Throwable t) {
               handleException(t);
            }

            // Merge adapter service properties, with CM settings 
            Dictionary<String, Object> serviceProperties = getServiceProperties(settings);
            newService.setInterface(m_serviceInterfaces, serviceProperties);
            newService.setImplementation(impl);
            newService.setComposition(m_compositionInstance, m_compositionMethod); // if not set, no effect
            newService.setCallbacks(m_callbackObject, m_init, m_start, m_stop, m_destroy); // if not set, no effect
            configureAutoConfigState(newService, m_component);
            
            for (DependencyContext dc : m_component.getDependencies()) {
                newService.add((Dependency) dc.createCopy());
            }
            
            for (int i = 0; i < m_stateListeners.size(); i ++) {
                newService.add(m_stateListeners.get(i));
            }
            
            return newService;
        }

        /**
         * Method called from our superclass, when we need to update a Service, because 
         * the configuration has changed.
         */
        @SuppressWarnings("unchecked")
        public void updateService(Object[] properties) {
            Dictionary<String, ?> cmSettings = (Dictionary<String, ?>) properties[0];
            Component service = (Component) properties[1];
            Object impl = service.getInstances()[0];

            try {
                InvocationUtil.invokeCallbackMethod(impl, m_update, 
                    new Class[][] {{ Dictionary.class }, {}}, 
                    new Object[][] {{ cmSettings }, {}});
                if (m_serviceInterfaces != null && m_propagate == true) {
                    Dictionary<String, ?> serviceProperties = getServiceProperties(cmSettings);
                    service.setServiceProperties(serviceProperties);
                }
            }
            
            catch (Throwable t) {
                handleException(t);
            }
        }   

        /**
         * Merge CM factory configuration setting with the adapter service properties. The private CM factory configuration 
         * settings are ignored. A CM factory configuration property is private if its name starts with a dot (".").
         * 
         * @param adapterProperties
         * @param settings
         * @return
         */
        private Dictionary<String, Object> getServiceProperties(Dictionary<String, ?> settings) {
            Dictionary<String, Object> props = new Hashtable<>();
            
            // Add adapter Service Properties
            if (m_serviceProperties != null) {
                Enumeration<String> keys = m_serviceProperties.keys();
                while (keys.hasMoreElements()) {
                    String key = keys.nextElement();
                    Object val = m_serviceProperties.get(key);
                    props.put(key, val);
                }
            }

            if (m_propagate) {
                // Add CM setting into adapter service properties.
                // (CM setting will override existing adapter service properties).
                Enumeration<String> keys = settings.keys();
                while (keys.hasMoreElements()) {
                    String key = keys.nextElement();
                    if (! key.toString().startsWith(".")) {
                        // public properties are propagated
                        Object val = settings.get(key);
                        props.put(key, val);
                    }
                }
            }

            
            return props;
        }
    
        private Object instantiateFromFactory(Object mFactory, String mFactoryCreateMethod) {
            Object factory = null;
            if (m_factory instanceof Class) {
                try {
                    factory = createInstance((Class<?>) m_factory);
                }
                catch (Throwable t) {
                    handleException(t);
                }
            }
            else {
                factory = m_factory;
            }

            try {
                return InvocationUtil.invokeMethod(factory, factory.getClass(), m_factoryCreateMethod, new Class[][] { {} }, new Object[][] { {} }, false);
            }
            catch (Throwable t) {
                handleException(t);
                return null;
            }
        }

        private Object createInstance(Class<?> clazz) throws SecurityException, NoSuchMethodException, InstantiationException, IllegalAccessException {
            Constructor<?> constructor = clazz.getConstructor(new Class[] {});
            constructor.setAccessible(true);
            return clazz.newInstance();
        }
    
        private void handleException(Throwable t) {
            m_logger.log(Logger.LOG_ERROR, "Got exception while handling configuration update for factory pid " + m_factoryPid, t);
            if (t instanceof InvocationTargetException) {
                // Our super class will check if the target exception is itself a ConfigurationException.
                // In this case, it will simply re-thrown.
                throw new RuntimeException(((InvocationTargetException) t).getTargetException());
            }
            else if (t instanceof RuntimeException) {
                throw (RuntimeException) t;
            }
            else {
                throw new RuntimeException(t);
            }
        }
    }

    
    /**
     * Extends AdapterImpl for MetaType support.
     */
    class MetaTypeAdapterImpl extends AdapterImpl implements MetaTypeProvider {
        // Our MetaType Provider for describing our properties metadata
        private final MetaTypeProviderImpl m_metaType;
        
        public MetaTypeAdapterImpl(String updateMethod, boolean propagate,
                                   BundleContext bctx, Logger logger, String heading, 
                                   String description, String localization,
                                   PropertyMetaData[] properyMetaData) {
            super(updateMethod, propagate);
            m_metaType = new MetaTypeProviderImpl(m_factoryPid, bctx, logger, null, this);
            m_metaType.setName(heading);
            m_metaType.setDescription(description);
            if (localization != null) {
                m_metaType.setLocalization(localization);
            }
            for (int i = 0; i < properyMetaData.length; i++) {
                m_metaType.add(properyMetaData[i]);
            }
        }
        
        public String[] getLocales() {
            return m_metaType.getLocales();
        }

        public ObjectClassDefinition getObjectClassDefinition(String id, String locale) {
            return m_metaType.getObjectClassDefinition(id, locale);
        }
    }    
}
