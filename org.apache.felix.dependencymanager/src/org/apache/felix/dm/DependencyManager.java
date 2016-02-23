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
package org.apache.felix.dm;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.dm.impl.AdapterServiceImpl;
import org.apache.felix.dm.impl.AspectServiceImpl;
import org.apache.felix.dm.impl.BundleAdapterImpl;
import org.apache.felix.dm.impl.BundleDependencyImpl;
import org.apache.felix.dm.impl.ComponentImpl;
import org.apache.felix.dm.impl.ComponentScheduler;
import org.apache.felix.dm.impl.ConfigurationDependencyImpl;
import org.apache.felix.dm.impl.FactoryConfigurationAdapterImpl;
import org.apache.felix.dm.impl.ResourceAdapterImpl;
import org.apache.felix.dm.impl.ResourceDependencyImpl;
import org.apache.felix.dm.impl.ServiceDependencyImpl;
import org.apache.felix.dm.impl.TemporalServiceDependencyImpl;
import org.apache.felix.dm.impl.index.AdapterFilterIndex;
import org.apache.felix.dm.impl.index.AspectFilterIndex;
import org.apache.felix.dm.impl.index.ServiceRegistryCache;
import org.apache.felix.dm.impl.index.multiproperty.MultiPropertyFilterIndex;
import org.apache.felix.dm.impl.metatype.PropertyMetaDataImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;

/**
 * The dependency manager manages all components and their dependencies. Using 
 * this API you can declare all components and their dependencies. Under normal
 * circumstances, you get passed an instance of this class through the
 * <code>DependencyActivatorBase</code> subclass you use as your
 * <code>BundleActivator</code>, but it is also possible to create your
 * own instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DependencyManager {    
    /**
     * The DependencyManager Activator will wait for a threadpool before creating any DM components if the following
     * OSGi system property is set to true.
     */
    public final static String PARALLEL = "org.apache.felix.dependencymanager.parallel";

    public static final String ASPECT = "org.apache.felix.dependencymanager.aspect";
    public static final String SERVICEREGISTRY_CACHE_INDICES = "org.apache.felix.dependencymanager.filterindex";
    public static final String METHOD_CACHE_SIZE = "org.apache.felix.dependencymanager.methodcache";
    
    private final BundleContext m_context;
    private final Logger m_logger;
    private final ConcurrentHashMap<Component, Component> m_components = new ConcurrentHashMap<>();

    // service registry cache
    private static ServiceRegistryCache m_serviceRegistryCache;
    private static final Set<WeakReference<DependencyManager>> m_dependencyManagers = new HashSet<>();
    static {
        try {
	    	Bundle bundle = FrameworkUtil.getBundle(DependencyManager.class);
	        if (bundle != null) {
	            if (bundle.getState() != Bundle.ACTIVE) {	        
	                bundle.start();
	            }
	            BundleContext bundleContext = bundle.getBundleContext();
	            String index = bundleContext.getProperty(SERVICEREGISTRY_CACHE_INDICES);
	            if (index != null) {
	            	m_serviceRegistryCache = new ServiceRegistryCache(bundleContext);
	            	m_serviceRegistryCache.open(); // TODO close it somewhere
	            	String[] props = index.split(";");
	            	for (int i = 0; i < props.length; i++) {
	            		if (props[i].equals("*aspect*")) {
	            			m_serviceRegistryCache.addFilterIndex(new AspectFilterIndex());
	            		}
	            		else if (props[i].equals("*adapter*")) {
	            			m_serviceRegistryCache.addFilterIndex(new AdapterFilterIndex());
	            		}
	            		else {
	            			m_serviceRegistryCache.addFilterIndex(new MultiPropertyFilterIndex(props[i]));
	            		}
	            	}
	            }
	        }
        }
        catch (BundleException e) {
        	// if we cannot start ourselves, we cannot use the indices
        	e.printStackTrace();
        }
    }

    /**
     * Creates a new dependency manager. You need to supply the
     * <code>BundleContext</code> to be used by the dependency
     * manager to register services and communicate with the 
     * framework.
     * 
     * @param context the bundle context
     */
    public DependencyManager(BundleContext context) {
        this(context, new Logger(context));
    }

    DependencyManager(BundleContext context, Logger logger) {
        m_context = createContext(context);
        m_logger = logger;
        synchronized (m_dependencyManagers) {
            m_dependencyManagers.add(new WeakReference<DependencyManager>(this));
        }
    }
    
    /**
     * Returns the list of currently created dependency managers.
     * @return the list of currently created dependency managers
     */
    public static List<DependencyManager> getDependencyManagers() {
        List<DependencyManager> result = new ArrayList<>();
        synchronized (m_dependencyManagers) {
            Iterator<WeakReference<DependencyManager>> iterator = m_dependencyManagers.iterator();
            while (iterator.hasNext()) {
                WeakReference<DependencyManager> reference = iterator.next();
                DependencyManager manager = reference.get();
                if (manager != null) {
                    try {
                        manager.getBundleContext().getBundle();
                        result.add(manager);
                        continue;
                    }
                    catch (IllegalStateException e) {
                    }
                }
                iterator.remove();
            }
        }
        return result;
    }

    /**
     * Returns the bundle context associated with this dependency manager.
     * @return the bundle context associated with this dependency manager.
     */
    public BundleContext getBundleContext() {
        return m_context;
    }

    /**
     * Adds a new component to the dependency manager. After the service is added
     * it will be started immediately.
     * 
     * @param c the service to add
     */
    public void add(Component c) {
        m_components.put(c, c);
        ComponentScheduler.instance().add(c);
    }

    /**
     * Removes a service from the dependency manager. Before the service is removed
     * it is stopped first.
     * 
     * @param c the component to remove
     */
    public void remove(Component c) {
        ComponentScheduler.instance().remove(c);
        m_components.remove(c);
    }

    /**
     * Creates a new component.
     * 
     * @return the new component
     */
    public Component createComponent() {
        return new ComponentImpl(m_context, this, m_logger);
    }

    /**
     * Creates a new service dependency.
     * 
     * @return the service dependency
     */
    public ServiceDependency createServiceDependency() {
        return new ServiceDependencyImpl();
    }

    /**
     * Creates a new configuration dependency.
     * 
     * @return the configuration dependency
     */
    public ConfigurationDependency createConfigurationDependency() {
        return new ConfigurationDependencyImpl(m_context, m_logger);
    }

    /**
     * Creates a new bundle dependency.
     * 
     * @return a new BundleDependency instance.
     */
    public BundleDependency createBundleDependency() {
        return new BundleDependencyImpl();
    }

    /**
     * Creates a new resource dependency.
     * 
     * @return the resource dependency
     */
    public ResourceDependency createResourceDependency() {
        return new ResourceDependencyImpl();
    }

    /**
     * Creates a new timed required service dependency. A timed dependency blocks the invoker thread is the required dependency
     * is currently unavailable, until it comes up again. 
     * 
     * @return a new timed service dependency
     */
    public ServiceDependency createTemporalServiceDependency(long timeout) {
        return new TemporalServiceDependencyImpl(m_context, timeout);
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
     */
    public Component createAdapterService(Class<?> serviceInterface, String serviceFilter) {
        return new AdapterServiceImpl(this, serviceInterface, serviceFilter, null, null, null, null, null, null, true);
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
     */
    public Component createAdapterService(Class<?> serviceInterface, String serviceFilter, String autoConfig) {
        return new AdapterServiceImpl(this, serviceInterface, serviceFilter, autoConfig, null, null, null, null, null, true);
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
     */
    public Component createAdapterService(Class<?> serviceInterface, String serviceFilter, String add, String change,
        String remove)
    {
        return new AdapterServiceImpl(this, serviceInterface, serviceFilter, null, null, add, change, remove, null, true);
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
     */
    public Component createAdapterService(Class<?> serviceInterface, String serviceFilter, String add, String change,
        String remove, String swap)
    {
        return new AdapterServiceImpl(this, serviceInterface, serviceFilter, null, null, add, change, remove, swap, true);
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
     */
    public Component createAdapterService(Class<?> serviceInterface, String serviceFilter, 
        String autoConfig, Object callbackInstance, String add, String change, String remove, 
        String swap, boolean propagate)
    {
        return new AdapterServiceImpl(this, serviceInterface, serviceFilter, autoConfig, callbackInstance, add, change, remove, swap, propagate);
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
     */
    public Component createFactoryConfigurationAdapterService(String factoryPid, String update, boolean propagate) {
        return new FactoryConfigurationAdapterImpl(this, factoryPid, update, propagate, null, null);
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
     */
    public Component createFactoryConfigurationAdapterService(String factoryPid, String update, boolean propagate, Object callbackInstance) {
        return new FactoryConfigurationAdapterImpl(this, factoryPid, update, propagate, callbackInstance, null);
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
     */
    public Component createFactoryConfigurationAdapterService(String factoryPid, String update, boolean propagate, Class<?> configType) {
        return new FactoryConfigurationAdapterImpl(this, factoryPid, update, propagate, null, configType);
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
     */
    public Component createFactoryConfigurationAdapterService(String factoryPid, String update, boolean propagate, Object callbackInstance, Class<?> configType) {
        return new FactoryConfigurationAdapterImpl(this, factoryPid, update, propagate, callbackInstance, configType);
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
     */
    public Component createAdapterFactoryConfigurationService(String factoryPid, String update, boolean propagate,
        String heading, String desc, String localization, PropertyMetaData[] propertiesMetaData)
    {
        return new FactoryConfigurationAdapterImpl(this, factoryPid, update, propagate, null, m_context, m_logger, heading,
            desc, localization, propertiesMetaData);
    }

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
     * <p>Usage example:
     * 
     * <blockquote><pre>
     *  manager.createBundleAdapterService(Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE, 
     *                                     "(Bundle-SymbolicName=org.apache.felix.dependencymanager)",
     *                                     true)
     *         // The interface to use when registering adapter
     *         .setInterface(AdapterService.class.getName(), new Hashtable() {{ put("foo", "bar"); }})
     *         // the implementation of the adapter
     *         .setImplementation(AdapterServiceImpl.class);
     * </pre></blockquote>
     * 
     * @param bundleStateMask the bundle state mask to apply
     * @param bundleFilter the filter to apply to the bundle manifest
     * @param propagate <code>true</code> if properties from the bundle should be propagated to the service
     * @return a service that acts as a factory for generating bundle adapters
     */
    public Component createBundleAdapterService(int bundleStateMask, String bundleFilter, boolean propagate) {
        return new BundleAdapterImpl(this, bundleStateMask, bundleFilter, propagate);
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
     */
    public Component createBundleAdapterService(int bundleStateMask, String bundleFilter, boolean propagate,
    		Object callbackInstance, String add, String change, String remove) {
        return new BundleAdapterImpl(this, bundleStateMask, bundleFilter, propagate, callbackInstance, add, change, remove);
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
     * <p>Usage example:
     * 
     * <blockquote><pre>
     *  manager.createResourceAdapterService("(&(path=/test)(repository=TestRepository))", true)
     *         // The interface to use when registering adapter
     *         .setInterface(AdapterService.class.getName(), new Hashtable() {{ put("foo", "bar"); }})
     *         // the implementation of the adapter
     *         .setImplementation(AdapterServiceImpl.class);
     * </pre></blockquote>
     *
     * @param resourceFilter the filter condition to use with the resource
     * @param propagate <code>true</code> if properties from the resource should be propagated to the service
     * @param callbackInstance instance to invoke the callback on
     * @param callbackChanged the name of the callback method
     * @return a service that acts as a factory for generating resource adapters
     */
    public Component createResourceAdapterService(String resourceFilter, boolean propagate, Object callbackInstance,
        String callbackChanged)
    {
        return new ResourceAdapterImpl(this, resourceFilter, propagate, callbackInstance, null, callbackChanged);
    }

    /** @see DependencyManager#createResourceAdapterService(String, boolean, Object, String) */
    public Component createResourceAdapterService(String resourceFilter, boolean propagate, Object callbackInstance,
        String callbackSet, String callbackChanged)
    {
        return new ResourceAdapterImpl(this, resourceFilter, propagate, callbackInstance, callbackSet, callbackChanged);
    }

    /** @see DependencyManager#createResourceAdapterService(String, boolean, Object, String) */
    public Component createResourceAdapterService(String resourceFilter, Object propagateCallbackInstance,
        String propagateCallbackMethod, Object callbackInstance, String callbackChanged)
    {
        return new ResourceAdapterImpl(this, resourceFilter, propagateCallbackInstance, propagateCallbackMethod,
            callbackInstance, null, callbackChanged);
    }

    /** @see DependencyManager#createResourceAdapterService(String, boolean, Object, String) */
    public Component createResourceAdapterService(String resourceFilter, Object propagateCallbackInstance,
        String propagateCallbackMethod, Object callbackInstance, String callbackSet, String callbackChanged)
    {
        return new ResourceAdapterImpl(this, resourceFilter, propagateCallbackInstance, propagateCallbackMethod,
            callbackInstance, callbackSet, callbackChanged);
    }

    /**
     * Returns a list of components.
     * 
     * @return a list of components
     */
    public List<Component> getComponents() {
        return Collections.list(m_components.elements());
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
     */
    public Component createAspectService(Class<?> serviceInterface, String serviceFilter, int ranking, String autoConfig) {
        return new AspectServiceImpl(this, serviceInterface, serviceFilter, ranking, autoConfig, null, null, null, null, null);
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
     */
    public Component createAspectService(Class<?> serviceInterface, String serviceFilter, int ranking) {
        return new AspectServiceImpl(this, serviceInterface, serviceFilter, ranking, null, null, null, null, null, null);
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
     */
    public Component createAspectService(Class<?> serviceInterface, String serviceFilter, int ranking, String add,
        String change, String remove)
    {
        return new AspectServiceImpl(this, serviceInterface, serviceFilter, ranking, null, null, add, change, remove, null);
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
     */
    public Component createAspectService(Class<?> serviceInterface, String serviceFilter, int ranking, String add,
        String change, String remove, String swap)
    {
        return new AspectServiceImpl(this, serviceInterface, serviceFilter, ranking, null, null, add, change, remove, swap);
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
     */
    public Component createAspectService(Class<?> serviceInterface, String serviceFilter, int ranking, Object callbackInstance, 
        String add, String change, String remove, String swap)        
    {
        return new AspectServiceImpl(this, serviceInterface, serviceFilter, ranking, null, callbackInstance, add, change, remove, swap);
    }

    /**
     * Removes all components and their dependencies.
     */
    public void clear() {
        for (Component component : m_components.keySet()) {
            remove(component);
        }
        m_components.clear();
    }

    /**
     * Creates a new configuration property metadata.
     * 
     * @return the configuration property metadata.
     */
    public PropertyMetaData createPropertyMetaData() {
        return new PropertyMetaDataImpl();
    }

    private BundleContext createContext(BundleContext context) {
        if (m_serviceRegistryCache != null) {
            return m_serviceRegistryCache.createBundleContextInterceptor(context);
        }
        else {
            return context;
        }
    }
}
