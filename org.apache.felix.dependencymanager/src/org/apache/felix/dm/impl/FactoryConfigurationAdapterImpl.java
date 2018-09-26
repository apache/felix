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

import static org.apache.felix.dm.impl.ConfigurationDependencyImpl.createCallbackType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Stream;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ConfigurationDependency;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.FactoryComponent;
import org.apache.felix.dm.Logger;
import org.apache.felix.dm.PropertyMetaData;
import org.apache.felix.dm.context.ComponentContext;
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
public class FactoryConfigurationAdapterImpl extends FilterComponent<FactoryComponent> implements FactoryComponent {
    /**
     * The pid matching the factory configuration.
     */
    private volatile String m_factoryPid;
    
    /**
     * The adapter method name that will be notified when the factory configuration is created/updated.
     */
    private volatile String m_update = "updated";
    
    /**
     * true if public factory configuration should be propagated to the adapter service properties.
     */
    private volatile boolean m_propagate;
    
    /**
     * The object on which the updated callback will be invoked.
     */
    private volatile Object m_callbackInstance;
    
    /**
     * the configuration type's) to use instead of a dictionary.  See the javadoc from {@link ConfigurationDependency} for
     */
    private volatile Class<?>[] m_configTypes;
    
    /**
     * The label used to display the tab name (or section) where the properties are displayed. 
     */
    private volatile String m_heading;
    
    /**
     * A human readable description of the factory PID this configuration is associated with.
     */
    private volatile String m_desc;
    
    /**
     * Points to the basename of the Properties file that can localize the Meta Type informations.
     */
    private volatile String m_localization;
    
    /**
     * Array of MetaData regarding configuration properties.
     */
    private volatile List<PropertyMetaData> m_propertiesMetaData = new ArrayList<>(0);
    
    // Our logger
    protected final Logger m_logger;
    
    public FactoryConfigurationAdapterImpl(DependencyManager dm) {
    	super(dm.createComponent());
    	m_logger = dm.getLogger();
    }
    
    /**
     * Sets the pid matching the factory configuration
     * @param factoryPid the pid matching the factory configuration
     */
    public FactoryConfigurationAdapterImpl setFactoryPid(String factoryPid) {
        this.m_factoryPid = factoryPid;
        return this;
    }

    /**
     * Sets the pid matching the factory configuration using the specified class.
     * The FQDN of the specified class will be used as the class name.
     * @param m_factoryPid the pid matching the factory configuration
     */
    public FactoryConfigurationAdapterImpl setFactoryPid(Class<?> clazz) {
        this.m_factoryPid = clazz.getName();
        return this;
    }

    /**
     * Sets the method name that will be notified when the factory configuration is created/updated.
     * By default, the callback name used is <code>updated</<code>
     * @TODO describe supported signatures
     * @param update the method name that will be notified when the factory configuration is created/updated.
     */
    public FactoryConfigurationAdapterImpl setUpdated(String update) {
        this.m_update = update;
        return this;
    }

    /**
     * Sets the propagate flag (true means all public configuration properties are propagated to service properties).
     * By default, public configurations are not propagated.
     * @param propagate the propagate flag (true means all public configuration properties are propagated to service properties).
     */
    public FactoryConfigurationAdapterImpl setPropagate(boolean propagate) {
        this.m_propagate = propagate;
        return this;
    }

    /**
     * Sets the object on which the updated callback will be invoked. 
     * By default, the callback is invoked on the component instance.
     * @param callbackInstance the object on which the updated callback will be invoked.
     */
    public FactoryConfigurationAdapterImpl setUpdateInstance(Object callbackInstance) {
        this.m_callbackInstance = callbackInstance;
        return this;
    }

    /**
     * Sets the configuration type to use instead of a dictionary. The updated callback is assumed to take
     * as arguments the specified configuration types in the same order they are provided in this method. 
     * @param configTypes the configuration type to use instead of a dictionary
     * @see ConfigurationDependency
     */
    public FactoryConfigurationAdapterImpl setConfigType(Class<?> ... configTypes) {
        this.m_configTypes = configTypes;
        return this;
    }

    /**
     * Sets the label used to display the tab name (or section) where the properties are displayed. 
     * Example: "Printer Service"
     * @param heading the label used to display the tab name (or section) where the properties are displayed.
     */
    public FactoryConfigurationAdapterImpl setHeading(String heading) {
        this.m_heading = heading;
        return this;
    }

    /**
     * A human readable description of the factory PID this configuration is associated with.
     * @param desc
     */
    public FactoryConfigurationAdapterImpl setDesc(String desc) {
        this.m_desc = desc;
        return this;
    }

    /**
     * Points to the basename of the Properties file that can localize the Meta Type informations.
     * The default localization base name for the properties is OSGI-INF/l10n/bundle, but can
     * be overridden by the manifest Bundle-Localization header (see core specification, in section Localization 
     * on page 68). You can specify a specific localization basename file using this parameter 
     * (e.g. <code>"person"</code> will match person_du_NL.properties in the root bundle directory).
     * @param localization 
     */
    public FactoryConfigurationAdapterImpl setLocalization(String localization) {
        this.m_localization = localization;
        return this;
    }

    /**
     * Sets MetaData regarding configuration properties.
     * @param metaData the metadata regarding configuration properties 
     */
    public FactoryConfigurationAdapterImpl add(PropertyMetaData ... metaData) {
        Stream.of(metaData).forEach(m_propertiesMetaData::add);
        return this;
    }
    
    @Override
    protected void startInitial() {
        Hashtable<String, Object> props = new Hashtable<>();
        props.put(Constants.SERVICE_PID, m_factoryPid);
        
        if (m_propertiesMetaData.size() == 0) {            
            m_component
                .setInterface(ManagedServiceFactory.class.getName(), props)
                .setImplementation(new AdapterImpl())
                .setCallbacks("init", null, "stop", null);
        } else {
            String[] ifaces = { ManagedServiceFactory.class.getName(), MetaTypeProvider.class.getName() };
            props.put(MetaTypeProvider.METATYPE_FACTORY_PID, m_factoryPid);
            m_component
                .setInterface(ifaces, props)
                .setImplementation(new MetaTypeAdapterImpl())
                .setCallbacks("init", null, "stop", null);
        }
    }
            
    public String getName() {
        return "Adapter for factory pid " + m_factoryPid;
    }

    /**
     * Creates, updates, or removes a service, when a ConfigAdmin factory configuration is created/updated or deleted.
     */
    public class AdapterImpl extends AbstractDecorator implements ManagedServiceFactory {        

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
        public Component createService(Object[] properties) throws Exception {
            Dictionary<String, ?> settings = (Dictionary<String, ?>) properties[0];     
            Component newService = m_manager.createComponent();        

            // Merge adapter service properties, with CM settings 
            Dictionary<String, Object> serviceProperties = getServiceProperties(settings);
            newService.setInterface(m_serviceInterfaces, serviceProperties);
            newService.setImplementation(m_serviceImpl);
            newService.setFactory(m_factory, m_factoryCreateMethod); // if not set, no effect
            newService.setComposition(m_compositionInstance, m_compositionMethod); // if not set, no effect
            newService.setCallbacks(m_callbackObject, m_init, m_start, m_stop, m_destroy); // if not set, no effect
            newService.setScope(m_scope);
            configureAutoConfigState(newService, m_component);
            
            copyDependencies(m_component.getDependencies(), newService);
            
            for (int i = 0; i < m_stateListeners.size(); i ++) {
                newService.add(m_stateListeners.get(i));
            }
            
            // Instantiate the component, because we need to invoke the updated callback synchronously, in the CM calling thread.
            ((ComponentContext) newService).instantiateComponent();

            CallbackTypeDef callbackInfo = createCallbackType(m_logger, newService, m_configTypes, settings);
            invokeUpdated(newService, callbackInfo);

            return newService;
        }

        /**
         * Method called from our superclass, when we need to update a Service, because 
         * the configuration has changed.
         */
        @SuppressWarnings("unchecked")
        public void updateService(Object[] properties) throws Exception {
            Dictionary<String, ?> cmSettings = (Dictionary<String, ?>) properties[0];
            Component service = (Component) properties[1];
            CallbackTypeDef callbackInfo = createCallbackType(m_logger, service, m_configTypes, cmSettings);

            invokeUpdated(service, callbackInfo);

            if (m_serviceInterfaces != null && m_propagate == true) {
                Dictionary<String, ?> serviceProperties = getServiceProperties(cmSettings);
                service.setServiceProperties(serviceProperties);
            }
        }
        
        private void invokeUpdated(Component service, CallbackTypeDef callbackInfo) throws Exception {
        	if (m_component.injectionDisabled()) {
        		return;
        	}
            boolean callbackFound = false;
            Object[] instances = getUpdateCallbackInstances(service);
            for (Object instance : instances) {
                try {
                	// Only inject if the component instance is not a prototype instance
                	InvocationUtil.invokeCallbackMethod(instance, m_update, callbackInfo.m_sigs, callbackInfo.m_args);
                	callbackFound |= true;
                }
                catch (NoSuchMethodException e) {
                    // if the method does not exist, ignore it
                }
            }
            
            if (! callbackFound) {
                String[] instanceClasses = Stream.of(instances).map(c -> c.getClass().getName()).toArray(String[]::new);
                m_logger.log(Logger.LOG_ERROR, "\"" + m_update + "\" configuration callback not found in any of the component classes: " + Arrays.toString(instanceClasses));                    
            }
        }
        
        /**
         * Returns the Update callback instances.
         */
        private Object[] getUpdateCallbackInstances(Component comp) {
            if (m_callbackInstance == null) {
                return comp.getInstances();
            } else {
                return new Object[] { m_callbackInstance };
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
    }
    
    /**
     * Extends AdapterImpl for MetaType support (deprecated, now users can directly use bnd metatypes).
     */
    class MetaTypeAdapterImpl extends AdapterImpl implements MetaTypeProvider {
        // Our MetaType Provider for describing our properties metadata
        private final MetaTypeProviderImpl m_metaType;
        
        public MetaTypeAdapterImpl() {            
            BundleContext bctx = getBundleContext();
            Logger logger = getLogger();
            m_metaType = new MetaTypeProviderImpl(m_factoryPid, bctx, logger, null, this);
            m_metaType.setName(m_heading);
            m_metaType.setDescription(m_desc);
            if (m_localization != null) {
                m_metaType.setLocalization(m_localization);
            }
            for (PropertyMetaData properyMetaData : m_propertiesMetaData) {
                m_metaType.add(properyMetaData);
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
