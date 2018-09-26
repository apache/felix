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
package org.apache.felix.dm.compat;

import org.apache.felix.dm.AdapterComponent;
import org.apache.felix.dm.AspectComponent;
import org.apache.felix.dm.BundleComponent;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.ConfigurationDependency;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.FactoryComponent;
import org.apache.felix.dm.PropertyMetaData;
import org.apache.felix.dm.ResourceComponent;

/**
 * This class contains some methods which have been deprecated from the DependencyManager class.
 */
public abstract class DependencyManagerCompat {

    /**
     * Creates a new aspect component. The aspect will be applied to any service that
     * matches the specified interface and filter. For each matching service
     * an aspect will be created based on the aspect implementation class.
     * The aspect will be registered with the same interface and properties
     * as the original service, plus any extra properties you supply here.
     * It will also inherit all dependencies, and if you declare the original
     * service as a member it will be injected.
     * 
     * @return an aspect component
     */
    public abstract AspectComponent createAspectComponent();

    /**
     * Creates a new adapter component. The adapter will be applied to any service that
     * matches the specified interface and filter. For each matching service
     * an adapter will be created based on the adapter implementation class.
     * The adapter will be registered with the specified interface and existing properties
     * from the original service plus any extra properties you supply here.
     * It will also inherit all dependencies, and if you declare the original
     * service as a member it will be injected.
     * 
     * @return an adapter component
     */
    public abstract AdapterComponent createAdapterComponent();

    /**
     * Creates a new Factory Component. For each new factory configuration matching
     * the factoryPid, a component will be created based on the component implementation class.
     * The component will be registered with the specified interface, and with the specified service properties.
     * Depending on the <code>propagate</code> parameter, every public factory configuration properties 
     * (which don't start with ".") will be propagated along with the adapter service properties. 
     * It will also inherit all dependencies.
     * 
     * @return a factory pid component
     */    
    public abstract FactoryComponent createFactoryComponent();
    
    /**
     * Creates a new bundle adapter. The adapter will be applied to any bundle that
     * matches the specified bundle state mask and filter condition. For each matching
     * bundle an adapter will be created based on the adapter implementation class.
     * The adapter will be registered with the specified interface
     * 
     * TODO and existing properties from the original resource plus any extra properties you supply here.
     * It will also inherit all dependencies, and if you declare the original
     * service as a member it will be injected.
     * 
     * @return a service that acts as a factory for generating bundle adapters
     */
    public abstract BundleComponent createBundleComponent();

    /**
     * Creates a new resource adapter component. The adapter will be applied to any resource that
     * matches the specified filter condition. For each matching resource
     * an adapter will be created based on the adapter implementation class.
     * The adapter will be registered with the specified interface and existing properties
     * from the original resource plus any extra properties you supply here.
     * It will also inherit all dependencies, and if you declare the original
     * service as a member it will be injected.
     * 
     * @return a Resource Adapter Component
     */
    public abstract ResourceComponent createResourceComponent();

    /**
     * Creates a new aspect. The aspect will be applied to any service that
     * matches the specified interface and filter. For each matching service
     * an aspect will be created based on the aspect implementation class.
     * The aspect will be registered with the same interface and properties
     * as the original service, plus any extra properties you supply here.
     * It will also inherit all dependencies, and if you declare the original
     * service as a member it will be injected.
     * 
     * <p>Usage example:
     * 
     * <blockquote><pre>
     * manager.createAspectService(ExistingService.class, "(foo=bar)", 10, "m_service")
     *     .setImplementation(ExistingServiceAspect.class)
     * );
     * </pre></blockquote>
     * 
     * @param serviceInterface the service interface to apply the aspect to
     * @param serviceFilter the filter condition to use with the service interface
     * @param ranking the level used to organize the aspect chain ordering
     * @param autoConfig the aspect implementation field name where to inject original service. 
     *     If null, any field matching the original service will be injected.
     * @return a service that acts as a factory for generating aspects
     * @deprecated use {@link DependencyManager#createAspectComponent()}
     */
    public Component createAspectService(Class<?> serviceInterface, String serviceFilter, int ranking, String autoConfig) {
        return createAspectComponent()
            .setAspect(serviceInterface, serviceFilter, ranking)
            .setAspectField(autoConfig);
    }

    /**
     * Creates a new aspect. The aspect will be applied to any service that
     * matches the specified interface and filter. For each matching service
     * an aspect will be created based on the aspect implementation class.
     * The aspect will be registered with the same interface and properties
     * as the original service, plus any extra properties you supply here.
     * It will also inherit all dependencies, and if you declare the original
     * service as a member it will be injected.
     * 
     * <p>Usage example:
     * 
     * <blockquote><pre>
     * manager.createAspectService(ExistingService.class, "(foo=bar)", 10)
     *     .setImplementation(ExistingServiceAspect.class)
     * );
     * </pre></blockquote>
     * 
     * @param serviceInterface the service interface to apply the aspect to
     * @param serviceFilter the filter condition to use with the service interface
     * @param ranking the level used to organize the aspect chain ordering
     * @return a service that acts as a factory for generating aspects
     * @deprecated use {@link DependencyManager#createAspectComponent()}
     */
    public Component createAspectService(Class<?> serviceInterface, String serviceFilter, int ranking) {
        return createAspectComponent()
            .setAspect(serviceInterface, serviceFilter, ranking);
    }

    /**
     * Creates a new aspect. The aspect will be applied to any service that
     * matches the specified interface and filter. For each matching service
     * an aspect will be created based on the aspect implementation class.
     * The aspect will be registered with the same interface and properties
     * as the original service, plus any extra properties you supply here.
     * It will also inherit all dependencies, and if you declare the original
     * service as a member it will be injected.
     * 
     * <p>Usage example:
     * 
     * <blockquote><pre>
     * manager.createAspectService(ExistingService.class, "(foo=bar)", 10, "add", "change", "remove")
     *     .setImplementation(ExistingServiceAspect.class)
     * );
     * </pre></blockquote>
     * 
     * @param serviceInterface the service interface to apply the aspect to
     * @param serviceFilter the filter condition to use with the service interface
     * @param ranking the level used to organize the aspect chain ordering
     * @param add name of the callback method to invoke on add
     * @param change name of the callback method to invoke on change
     * @param remove name of the callback method to invoke on remove
     * @return a service that acts as a factory for generating aspects
     * @deprecated use {@link DependencyManager#createAspectComponent()}
     */
    public Component createAspectService(Class<?> serviceInterface, String serviceFilter, int ranking, String add,
        String change, String remove)
    {
        return createAspectComponent()
            .setAspect(serviceInterface, serviceFilter, ranking)
            .setAspectCallbacks(add, change, remove, null);
    }

    /**
     * Creates a new aspect. The aspect will be applied to any service that
     * matches the specified interface and filter. For each matching service
     * an aspect will be created based on the aspect implementation class.
     * The aspect will be registered with the same interface and properties
     * as the original service, plus any extra properties you supply here.
     * It will also inherit all dependencies, and if you declare the original
     * service as a member it will be injected.
     * 
     * <p>Usage example:
     * 
     * <blockquote><pre>
     * manager.createAspectService(ExistingService.class, "(foo=bar)", 10, "add", "change", "remove")
     *     .setImplementation(ExistingServiceAspect.class)
     * );
     * </pre></blockquote>
     * 
     * @param serviceInterface the service interface to apply the aspect to
     * @param serviceFilter the filter condition to use with the service interface
     * @param ranking the level used to organize the aspect chain ordering
     * @param add name of the callback method to invoke on add
     * @param change name of the callback method to invoke on change
     * @param remove name of the callback method to invoke on remove
     * @param swap name of the callback method to invoke on swap
     * @return a service that acts as a factory for generating aspects
     * @deprecated use {@link DependencyManager#createAspectComponent()}
     */
    public Component createAspectService(Class<?> serviceInterface, String serviceFilter, int ranking, String add, String change, String remove, String swap)
    {
        return createAspectComponent()
            .setAspect(serviceInterface, serviceFilter, ranking)
            .setAspectCallbacks(add, change, remove, swap);
    }

    /**
     * Creates a new aspect. The aspect will be applied to any service that
     * matches the specified interface and filter. For each matching service
     * an aspect will be created based on the aspect implementation class.
     * The aspect will be registered with the same interface and properties
     * as the original service, plus any extra properties you supply here.
     * It will also inherit all dependencies, and if you declare the original
     * service as a member it will be injected.
     * 
     * <p>Usage example:
     * 
     * <blockquote><pre>
     * manager.createAspectService(ExistingService.class, "(foo=bar)", 10, "add", "change", "remove")
     *     .setImplementation(ExistingServiceAspect.class)
     * );
     * </pre></blockquote>
     * 
     * @param serviceInterface the service interface to apply the aspect to
     * @param serviceFilter the filter condition to use with the service interface
     * @param ranking the level used to organize the aspect chain ordering
     * @param callbackInstance the instance to invoke the callbacks on, or null if the callbacks have to be invoked on the aspect itself
     * @param add name of the callback method to invoke on add
     * @param change name of the callback method to invoke on change
     * @param remove name of the callback method to invoke on remove
     * @param swap name of the callback method to invoke on swap
     * @return a service that acts as a factory for generating aspects
     * @deprecated use {@link DependencyManager#createAspectComponent()}
     */
    public Component createAspectService(Class<?> serviceInterface, String serviceFilter, int ranking, Object callbackInstance, 
        String add, String change, String remove, String swap)        
    {
        return createAspectComponent()
            .setAspect(serviceInterface, serviceFilter, ranking)
            .setAspectCallbackInstance(callbackInstance)
            .setAspectCallbacks(add, change, remove, swap);
    }
    
    /**
     * Creates a new adapter. The adapter will be applied to any service that
     * matches the specified interface and filter. For each matching service
     * an adapter will be created based on the adapter implementation class.
     * The adapter will be registered with the specified interface and existing properties
     * from the original service plus any extra properties you supply here.
     * It will also inherit all dependencies, and if you declare the original
     * service as a member it will be injected.
     * 
     * <p>Usage example:
     * 
     * <blockquote><pre>
     * manager.createAdapterService(AdapteeService.class, "(foo=bar)")
     *     .setInterface(AdapterService.class, new Hashtable() {{ put("extra", "property"); }})
     *     .setImplementation(AdapterImpl.class);
     * </pre></blockquote>
     * 
     * @param serviceInterface the service interface to apply the adapter to
     * @param serviceFilter the filter condition to use with the service interface
     * @return a service that acts as a factory for generating adapters
     * @deprecated use {@link DependencyManager#createAdapterComponent()}
     */
    public Component createAdapterService(Class<?> serviceInterface, String serviceFilter) {
    	return createAdapterComponent().setAdaptee(serviceInterface, serviceFilter);    			
    }

    /**
     * Creates a new adapter. The adapter will be applied to any service that
     * matches the specified interface and filter. For each matching service
     * an adapter will be created based on the adapter implementation class.
     * The adapter will be registered with the specified interface and existing properties
     * from the original service plus any extra properties you supply here.
     * It will also inherit all dependencies, and if you declare the original
     * service as a member it will be injected.
     * 
     * <p>Usage example:
     * 
     * <blockquote><pre>
     * manager.createAdapterService(AdapteeService.class, "(foo=bar)", "m_service")
     *     .setInterface(AdapterService.class, new Hashtable() {{ put("extra", "property"); }})
     *     .setImplementation(AdapterImpl.class);
     * </pre></blockquote>
     * 
     * @param serviceInterface the service interface to apply the adapter to
     * @param serviceFilter the filter condition to use with the service interface
     * @param autoConfig the name of the member to inject the service into
     * @return a service that acts as a factory for generating adapters
     * @deprecated use {@link DependencyManager#createAdapterComponent()}
     */
    public Component createAdapterService(Class<?> serviceInterface, String serviceFilter, String autoConfig) {
    	return createAdapterComponent()
    			.setAdaptee(serviceInterface, serviceFilter)
    			.setAdapteeField(autoConfig);
    }

    /**
     * Creates a new adapter. The adapter will be applied to any service that
     * matches the specified interface and filter. For each matching service
     * an adapter will be created based on the adapter implementation class.
     * The adapter will be registered with the specified interface and existing properties
     * from the original service plus any extra properties you supply here.
     * It will also inherit all dependencies, and if you declare the original
     * service as a member it will be injected.
     * 
     * <p>Usage example:
     * 
     * <blockquote><pre>
     * manager.createAdapterService(AdapteeService.class, "(foo=bar)", "add", "change", "remove")
     *     .setInterface(AdapterService.class, new Hashtable() {{ put("extra", "property"); }})
     *     .setImplementation(AdapterImpl.class);
     * </pre></blockquote>
     * 
     * @param serviceInterface the service interface to apply the adapter to
     * @param serviceFilter the filter condition to use with the service interface
     * @param add name of the callback method to invoke on add
     * @param change name of the callback method to invoke on change
     * @param remove name of the callback method to invoke on remove
     * @return a service that acts as a factory for generating adapters
     * @deprecated use {@link DependencyManager#createAdapterComponent()}
    */
    public Component createAdapterService(Class<?> serviceInterface, String serviceFilter, String add, String change, String remove) {
        return createAdapterComponent()
            .setAdaptee(serviceInterface, serviceFilter)
            .setAdapteeCallbacks(add, change, remove, null);
    }

    /**
     * Creates a new adapter. The adapter will be applied to any service that
     * matches the specified interface and filter. For each matching service
     * an adapter will be created based on the adapter implementation class.
     * The adapter will be registered with the specified interface and existing properties
     * from the original service plus any extra properties you supply here.
     * It will also inherit all dependencies, and if you declare the original
     * service as a member it will be injected.
     * 
     * <p>Usage example:
     * 
     * <blockquote><pre>
     * manager.createAdapterService(AdapteeService.class, "(foo=bar)", "add", "change", "remove", "swap")
     *     .setInterface(AdapterService.class, new Hashtable() {{ put("extra", "property"); }})
     *     .setImplementation(AdapterImpl.class);
     * </pre></blockquote>
     * 
     * @param serviceInterface the service interface to apply the adapter to
     * @param serviceFilter the filter condition to use with the service interface
     * @param add name of the callback method to invoke on add
     * @param change name of the callback method to invoke on change
     * @param remove name of the callback method to invoke on remove
     * @param swap name of the callback method to invoke on swap
     * @return a service that acts as a factory for generating adapters
     * @deprecated use {@link DependencyManager#createAdapterComponent()}
     */
    public Component createAdapterService(Class<?> serviceInterface, String serviceFilter, String add, String change, String remove, String swap) {
        return createAdapterComponent()
            .setAdaptee(serviceInterface, serviceFilter)
            .setAdapteeCallbacks(add, change, remove, swap);
    }

    /**
     * Creates a new adapter. The adapter will be applied to any service that
     * matches the specified interface and filter. For each matching service
     * an adapter will be created based on the adapter implementation class.
     * The adapter will be registered with the specified interface (and existing properties
     * from the original service if you set the propagate flag) plus any extra properties you supply here.
     * It will also inherit all dependencies, and if you declare the original
     * service as a member it will be injected.
     * 
     * <p>Usage example:
     * 
     * <blockquote><pre>
     * manager.createAdapterService(AdapteeService.class, "(foo=bar)", "add", "change", "remove", "swap")
     *     .setInterface(AdapterService.class, new Hashtable() {{ put("extra", "property"); }})
     *     .setImplementation(AdapterImpl.class);
     * </pre></blockquote>
     * 
     * @param serviceInterface the service interface to apply the adapter to
     * @param serviceFilter the filter condition to use with the service interface
     * @param autoConfig the name of the member to inject the service into, or null.
     * @param callbackInstance the instance to invoke the callbacks on, or null if the callbacks have to be invoked on the adapter itself
     * @param add name of the callback method to invoke on add
     * @param change name of the callback method to invoke on change
     * @param remove name of the callback method to invoke on remove
     * @param swap name of the callback method to invoke on swap
     * @param propagate true if the adaptee service properties should be propagated to the adapter service consumers
     * @return a service that acts as a factory for generating adapters
     * @deprecated use {@link DependencyManager#createAdapterComponent()}
     */
    public Component createAdapterService(Class<?> serviceInterface, String serviceFilter, String autoConfig, Object callbackInstance, String add, String change, String remove, String swap, boolean propagate) {
        return createAdapterComponent()
            .setAdaptee(serviceInterface, serviceFilter)
            .setAdapteeField(autoConfig)
            .setAdapteeCallbackInstance(callbackInstance)
            .setAdapteeCallbacks(add, change, remove, swap)
            .setPropagate(propagate);
    }

    /**
     * Creates a new Factory Configuration Adapter. For each new factory configuration matching
     * the factoryPid, an adapter will be created based on the adapter implementation class.
     * The adapter will be registered with the specified interface, and with the specified adapter service properties.
     * Depending on the <code>propagate</code> parameter, every public factory configuration properties 
     * (which don't start with ".") will be propagated along with the adapter service properties. 
     * It will also inherit all dependencies.
     * <p> The callback you specify may accept the following method signatures:
     * <ul><li> updated(Dictionary)
     * <li> updated(Component, Dictionary)
     * </ul>
     * 
     * <p>Usage example:
     * 
     * <blockquote><pre>
     *  manager.createFactoryConfigurationAdapterService("MyFactoryPid",  "update", true)
     *         // The interface to use when registering adapter
     *         .setInterface(AdapterService.class.getName(), new Hashtable() {{ put("foo", "bar"); }})
     *         // the implementation of the adapter
     *         .setImplementation(AdapterServiceImpl.class);
     * </pre></blockquote>
     * 
     * @param factoryPid the pid matching the factory configuration
     * @param update the adapter method name that will be notified when the factory configuration is created/updated.
     * @param propagate true if public factory configuration should be propagated to the adapter service properties
     * @return a service that acts as a factory for generating the managed service factory configuration adapter
     * @deprecated use {@link DependencyManager#createFactoryComponent()}
     */
    public Component createFactoryConfigurationAdapterService(String factoryPid, String update, boolean propagate) {
        return createFactoryComponent()
            .setFactoryPid(factoryPid)
            .setUpdated(update)
            .setPropagate(propagate);
    }

    /**
     * Creates a new Factory Configuration Adapter using a specific update callback instance. 
     * For each new factory configuration matching the factoryPid, an adapter will be created 
     * based on the adapter implementation class.
     * The adapter will be registered with the specified interface, and with the specified adapter service properties.
     * Depending on the <code>propagate</code> parameter, every public factory configuration properties 
     * (which don't start with ".") will be propagated along with the adapter service properties. 
     * It will also inherit all dependencies.
     * <p> The callback you specify may accept the following method signatures:
     * <ul><li> updated(Dictionary)
     * <li> updated(Component, Dictionary)
     * </ul>
     * 
     * @param factoryPid the pid matching the factory configuration
     * @param update the adapter method name that will be notified when the factory configuration is created/updated.
     * @param propagate true if public factory configuration should be propagated to the adapter service properties
     * @param callbackInstance the object on which the updated callback will be invoked.
     * @return a service that acts as a factory for generating the managed service factory configuration adapter
     * @deprecated use {@link DependencyManager#createFactoryComponent()}
     */
    public Component createFactoryConfigurationAdapterService(String factoryPid, String update, boolean propagate, Object callbackInstance) {
         return createFactoryComponent()
            .setFactoryPid(factoryPid)
            .setUpdated(update)
            .setPropagate(propagate)
            .setUpdateInstance(callbackInstance);
    }

    /**
     * Creates a new Factory Configuration Adapter. For each new factory configuration matching
     * the factoryPid, an adapter will be created based on the adapter implementation class.
     * The adapter will be registered with the specified interface, and with the specified adapter service properties.
     * Depending on the <code>propagate</code> parameter, every public factory configuration properties 
     * (which don't start with ".") will be propagated along with the adapter service properties. 
     * It will also inherit all dependencies.
     * <p> The callback you specify may accept the following method signatures:
     * <ul><li> updated(Dictionary)
     * <li> updated(Component, Dictionary)
     * <li> updated(ConfigurationType)
     * <li> updated(Component, ConfigurationType)
     * </ul>
     * <p>The ConfigurationType parameter is an implementation of the <code>configType</code> argument.
     * 
     * @param factoryPid the pid matching the factory configuration
     * @param update the adapter method name that will be notified when the factory configuration is created/updated.<p>
     * @param propagate true if public factory configuration should be propagated to the adapter service properties
     * @param configType the configuration type to use instead of a dictionary. See the javadoc from {@link ConfigurationDependency} for
     * more informations about type-safe configuration.
     * @return a service that acts as a factory for generating the managed service factory configuration adapter
     * @see ConfigurationDependency
     * @deprecated use {@link DependencyManager#createFactoryComponent()}
     */
    public Component createFactoryConfigurationAdapterService(String factoryPid, String update, boolean propagate, Class<?> configType) {
        return createFactoryComponent()
            .setFactoryPid(factoryPid)
            .setUpdated(update)
            .setPropagate(propagate)
            .setConfigType(configType);
    }

    /**
     * Creates a new Factory Configuration Adapter using a specific update callback instance. 
     * For each new factory configuration matching the factoryPid, an adapter will be created 
     * based on the adapter implementation class.
     * The adapter will be registered with the specified interface, and with the specified adapter service properties.
     * Depending on the <code>propagate</code> parameter, every public factory configuration properties 
     * (which don't start with ".") will be propagated along with the adapter service properties. 
     * It will also inherit all dependencies.
     * <p> The callback you specify may accept the following method signatures:
     * <ul><li> updated(Dictionary)
     * <li> updated(Component, Dictionary)
     * <li> updated(ConfigurationType)
     * <li> updated(Component, ConfigurationType)
     * </ul>
     * <p>The ConfigurationType parameter is an implementation of the <code>configType</code> argument.
     * 
     * @param factoryPid the pid matching the factory configuration
     * @param propagate true if public factory configuration should be propagated to the adapter service properties
     * @param callbackInstance the object on which the updated callback will be invoked.
     * @param configType the configuration type to use instead of a dictionary.  See the javadoc from {@link ConfigurationDependency} for
     * more informations about type-safe configuration.
     * @return a service that acts as a factory for generating the managed service factory configuration adapter
     * @deprecated use {@link DependencyManager#createFactoryComponent()}
     */
    public Component createFactoryConfigurationAdapterService(String factoryPid, String update, boolean propagate, Object callbackInstance, Class<?> configType) {
        return createFactoryComponent()
            .setFactoryPid(factoryPid)
            .setUpdated(update)
            .setPropagate(propagate)
            .setUpdateInstance(callbackInstance)
            .setConfigType(configType);
    }

   /**
     * Creates a new Managed Service Factory Configuration Adapter with meta type support. For each new 
     * factory configuration matching the factoryPid, an adapter will be created based on the adapter implementation 
     * class. The adapter will be registered with the specified interface, and with the specified adapter service 
     * properties. Depending on the <code>propagate</code> parameter, every public factory configuration properties 
     * (which don't start with ".") will be propagated along with the adapter service properties. 
     * It will also inherit all dependencies.
     * 
     * <p>Usage example:
     * 
     * <blockquote><pre>
     *       PropertyMetaData[] propertiesMetaData = new PropertyMetaData[] {
     *            manager.createPropertyMetaData()
     *               .setCardinality(Integer.MAX_VALUE)
     *               .setType(String.class)
     *               .setHeading("English words")
     *               .setDescription("Declare here some valid english words")
     *               .setDefaults(new String[] {"hello", "world"})
     *               .setId("words")
     *       };
     * 
     *       manager.add(createFactoryConfigurationAdapterService("FactoryPid", 
     *                                                            "updated",
     *                                                            true, // propagate CM settings
     *                                                            "EnglishDictionary",
     *                                                            "English dictionary configuration properties",
     *                                                            null,
     *                                                            propertiesMetaData)
     *               .setImplementation(Adapter.class));
     * </pre></blockquote>
     * 
     * @param factoryPid the pid matching the factory configuration
     * @param update the adapter method name that will be notified when the factory configuration is created/updated.<p>
     *  The following signatures are supported:<p>
     *        <ul><li> updated(Dictionary)
     *        <li> updated(Component, Dictionary)
     *        </ul>
     * @param propagate true if public factory configuration should be propagated to the adapter service properties
     * @param heading The label used to display the tab name (or section) where the properties are displayed. 
     *        Example: "Printer Service"
     * @param desc A human readable description of the factory PID this configuration is associated with. 
     *        Example: "Configuration for the PrinterService bundle"
     * @param localization Points to the basename of the Properties file that can localize the Meta Type informations.
     *        The default localization base name for the properties is OSGI-INF/l10n/bundle, but can
     *        be overridden by the manifest Bundle-Localization header (see core specification, in section Localization 
     *        on page 68). You can specify a specific localization basename file using this parameter 
     *        (e.g. <code>"person"</code> will match person_du_NL.properties in the root bundle directory).
     * @param propertiesMetaData Array of MetaData regarding configuration properties
     * @return a service that acts as a factory for generating the managed service factory configuration adapter
     * @deprecated use {@link DependencyManager#createFactoryComponent()}
     */
    public Component createAdapterFactoryConfigurationService(String factoryPid, String update, boolean propagate,
        String heading, String desc, String localization, PropertyMetaData[] propertiesMetaData)
    {
        return createFactoryComponent()
            .setFactoryPid(factoryPid)
            .setUpdated(update)
            .setPropagate(propagate)
            .setHeading(heading)
            .setDesc(desc)
            .setLocalization(localization)
            .add(propertiesMetaData);
    }
    
    /**
     * Creates a new bundle adapter. The adapter will be applied to any bundle that
     * matches the specified bundle state mask and filter condition. For each matching
     * bundle an adapter will be created based on the adapter implementation class.
     * The adapter will be registered with the specified interface
     * 
     * @param bundleStateMask the bundle state mask to apply
     * @param bundleFilter the filter to apply to the bundle manifest
     * @param propagate <code>true</code> if properties from the bundle should be propagated to the service
     * @return a service that acts as a factory for generating bundle adapters
     * @deprecated use {@link DependencyManager#createBundleComponent()}
     */
    public Component createBundleAdapterService(int bundleStateMask, String bundleFilter, boolean propagate) {
        return createBundleComponent()
        		.setBundleFilter(bundleStateMask, bundleFilter)
        		.setPropagate(propagate);        		
    }

   /**
    * Creates a new bundle adapter using specific callback instance. 
    * The adapter will be applied to any bundle that matches the specified bundle state mask and filter condition. 
    * For each matching bundle an adapter will be created based on the adapter implementation class, and
    * The adapter will be registered with the specified interface.
    * 
    * @param bundleStateMask the bundle state mask to apply
    * @param bundleFilter the filter to apply to the bundle manifest
    * @param propagate <code>true</code> if properties from the bundle should be propagated to the service
    * @param callbackInstance the instance to invoke the callbacks on, or null if the callbacks have to be invoked on the adapter itself
    * @param add name of the callback method to invoke on add
    * @param change name of the callback method to invoke on change
    * @param remove name of the callback method to invoke on remove
    * @return a service that acts as a factory for generating bundle adapters
    * @deprecated use {@link DependencyManager#createBundleComponent()}
    */
   public Component createBundleAdapterService(int bundleStateMask, String bundleFilter, boolean propagate,
   		Object callbackInstance, String add, String change, String remove) {
       return createBundleComponent()
       		.setBundleFilter(bundleStateMask,  bundleFilter)
       		.setPropagate(propagate)
       		.setBundleCallbacks(add, change, remove)
       		.setBundleCallbackInstance(callbackInstance);
   }

   /**
    * Creates a new resource adapter. The adapter will be applied to any resource that
    * matches the specified filter condition. For each matching resource
    * an adapter will be created based on the adapter implementation class.
    * The adapter will be registered with the specified interface and existing properties
    * from the original resource plus any extra properties you supply here.
    * It will also inherit all dependencies, and if you declare the original
    * service as a member it will be injected.
    * 
    * @param resourceFilter the filter condition to use with the resource
    * @param propagate <code>true</code> if properties from the resource should be propagated to the service
    * @param callbackInstance instance to invoke the callback on
    * @param callbackChanged the name of the callback method
    * @return a service that acts as a factory for generating resource adapters
    * @deprecated use {@link DependencyManager#createResourceComponent()} 
    */
   public Component createResourceAdapterService(String resourceFilter, boolean propagate, Object callbackInstance,
       String callbackChanged)
   {
       return createResourceComponent()
       		.setResourceFilter(resourceFilter)
       		.setPropagate(propagate)
       		.setBundleCallbackInstance( callbackInstance)
       		.setBundleCallbacks(null, callbackChanged);
   }

   /**
    * @see DependencyManager#createResourceAdapterService(String, boolean, Object, String)
    * @deprecated use {@link DependencyManager#createResourceComponent()} 
    **/
   public Component createResourceAdapterService(String resourceFilter, boolean propagate, Object callbackInstance,
       String callbackSet, String callbackChanged)
   {
       return createResourceComponent()
       		.setResourceFilter(resourceFilter)
       		.setPropagate(propagate)
       		.setBundleCallbackInstance( callbackInstance)
       		.setBundleCallbacks(callbackSet, callbackChanged);
   }

   /** 
    * @see DependencyManager#createResourceAdapterService(String, boolean, Object, String)
    * @deprecated use {@link DependencyManager#createResourceComponent()} 
    * */
   public Component createResourceAdapterService(String resourceFilter, Object propagateCallbackInstance,
       String propagateCallbackMethod, Object callbackInstance, String callbackChanged)
   {
       return createResourceComponent()
       		.setResourceFilter(resourceFilter)
       		.setPropagate(propagateCallbackInstance, propagateCallbackMethod)
       		.setBundleCallbackInstance( callbackInstance)
       		.setBundleCallbacks(null, callbackChanged);
   }

   /** 
    * @see DependencyManager#createResourceAdapterService(String, boolean, Object, String)
    * @deprecated use {@link DependencyManager#createResourceComponent()} 
    **/
   public Component createResourceAdapterService(String resourceFilter, Object propagateCallbackInstance,
       String propagateCallbackMethod, Object callbackInstance, String callbackSet, String callbackChanged)
   {
       return createResourceComponent()
       		.setResourceFilter(resourceFilter)
       		.setPropagate(propagateCallbackInstance, propagateCallbackMethod)
       		.setBundleCallbackInstance( callbackInstance)
       		.setBundleCallbacks(callbackSet, callbackChanged);
   }

}
