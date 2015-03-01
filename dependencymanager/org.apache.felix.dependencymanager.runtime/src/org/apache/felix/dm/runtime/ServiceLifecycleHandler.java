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
package org.apache.felix.dm.runtime;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.Bundle;

/**
 * Caution: this class *MUST* be immutable, because it may be shared between Aspects/Adapters 
 * and concrete Aspect/Adapter instance.
 * 
 * This class allows Services to configure dynamically their dependency filters from their init() method.
 * Basically, this class acts as a service implementation lifecycle handler. When we detect that the Service is
 * called in its init() method, and if init() returns a Map, then the Map is assumed to contain
 * dependency filters, which will be applied to all named dependencies. The Map optionally returned by
 * Service's init method may contain the following keys:
 * <ul>
 *   <li>name.filter: the value must be a valid OSGi filter, and the "name" prefix must match a ServiceDependency 
 *   name attribute</li>
 *   <li>name.required: the value is a boolean ("true"|"false") and the "name" prefix must match a 
 *   ServiceDependency name attribute</li>
 * </ul>
 * 
 * <p>Dependencies which provide a name attribute will be activated after the init method returns. Other
 * dependencies are injected before the init method.
 * 
 * <p>Example of a Service whose dependency filter is configured from ConfigAdmin:
 * 
 * <blockquote><pre>
 *  &#47;**
 *    * A Service whose service dependency filter/require attribute may be configured from ConfigAdmin
 *    *&#47;
 *  &#64;Service
 *  class X {
 *      private Dictionary m_config;
 *      
 *      &#64;ConfigurationDependency(pid="MyPid")
 *      void configure(Dictionary conf) {
 *           // Initialize our service from config ...
 *           
 *           // And store the config for later usage (from our init method)
 *           m_config = config;
 *      }
 *      
 *      &#64;ServiceDependency(name="dependency1") 
 *      void bindOtherService(OtherService other) {
 *         // the filter and required flag will be configured from our init method.
 *      }
 *
 *      // The returned Map will be used to configure our "dependency1" Dependency.
 *      &#64;Init
 *      Map init() {
 *          return new HashMap() {{
 *              put("dependency1.filter", m_config.get("filter"));
 *              put("dependency1.required", m_config.get("required"));
 *          }};
 *      } 
 *  }
 *  </pre></blockquote>
 *  
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceLifecycleHandler
{
    private final String m_init;
    private final String m_start;
    private final String m_stop;
    private final String m_destroy;
    private final MetaData m_srvMeta;
    private final List<MetaData> m_depsMeta;
    private final Bundle m_bundle;
    private final static Object SYNC = new Object();

    /**
     * Makes a new ServiceLifecycleHandler object. This objects allows to decorate the "init" service callback, in
     * order to see if "init" callback returns a dependency customization map.
     * 
     * @param srv The Service for the annotated class
     * @param srvBundle the Service bundle
     * @param dm The DependencyManager that was used to create the service
     * @param srvMeta The Service MetaData
     * @param depMeta The Dependencies MetaData
     */
    public ServiceLifecycleHandler(Component srv, Bundle srvBundle, DependencyManager dm,
                                   MetaData srvMeta, List<MetaData> depMeta)
    {
        m_srvMeta = srvMeta;
        m_init = srvMeta.getString(Params.init, null);
        m_start = srvMeta.getString(Params.start, null);
        m_stop = srvMeta.getString(Params.stop, null);
        m_destroy = srvMeta.getString(Params.destroy, null);
        m_bundle = srvBundle;
        m_depsMeta = depMeta;
    }

    /**
     * Handles an "init" lifecycle service callback. We just catch the "init" method, and callback 
     * the actual Service' init method, to see if a dependency customization map is returned.
     * We also check if a Lifecycle Controller is used. In this case, we add a hidden custom dependency,
     * allowing to take control of when the component is actually started/stopped.
     * We also handle an edge case described in FELIX-4050, where component state calculation 
     * may mess up if some dependencies are added using the API from the init method.
     * 
     * @param c The Annotated Component
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void init(Component c)
        throws Exception
    {
        Object serviceInstance = c.getInstances()[0];
        DependencyManager dm = c.getDependencyManager();

        // Check if a lifecycle controller is defined for this service. If true, then 
        // We'll use the ToggleServiceDependency in order to manually activate/deactivate 
        // the component ...
        String starter = m_srvMeta.getString(Params.starter, null);
        String stopper = m_srvMeta.getString(Params.stopper, null);

        List<Dependency> instanceBoundDeps = new ArrayList<>();
            
        if (starter != null)
        {
            // We'll inject two runnables: one that will start or service, when invoked, and the other
            // that will stop our service, when invoked. We'll use a shared atomic boolean in order to
            // synchronize both runnables.
            Log.instance().debug("Setting up a lifecycle controller for service %s", serviceInstance);
            String componentName = serviceInstance.getClass().getName();
            // Create a toggle service, used to start/stop our service.
            ToggleServiceDependency toggle = new ToggleServiceDependency();
            AtomicBoolean startFlag = new AtomicBoolean(false);
            // Add the toggle to the service.
            instanceBoundDeps.add(toggle);
            // Inject the runnable that will start our service, when invoked.
            setField(serviceInstance, starter, Runnable.class, new ComponentStarter(componentName, toggle, startFlag));
            if (stopper != null) {
                // Inject the runnable that will stop our service, when invoked.
                setField(serviceInstance, stopper, Runnable.class, new ComponentStopper(componentName, toggle, startFlag));
            }
        }

        // Before invoking an optional init method, we have to handle an edge case (FELIX-4050), where
        // init may add dependencies using the API and also return a map for configuring some
        // named dependencies. We have to add a hidden toggle dependency in the component, which we'll 
        // active *after* the init method is called, and possibly *after* named dependencies are configured.
        
        ToggleServiceDependency initToggle = null;
        if (m_init != null) 
        {
            initToggle = new ToggleServiceDependency();
            c.add(initToggle); 
        }
        
        // Invoke component and all composites init methods, and for each one, check if a dependency
        // customization map is returned by the method. This map will be used to configure 
        // some dependency filters (or required flag).

        Map<String, String> customization = new HashMap<String, String>();
        Object[] composites = c.getInstances();
        for (Object composite: composites)
        {
            Object o = invokeMethod(composite, m_init, dm, c);
            if (o != null && Map.class.isAssignableFrom(o.getClass()))
            {
                customization.putAll((Map) o);
            }
        }

        Log.instance().debug("ServiceLifecycleHandler.init: invoked init method from service %s " +
                             ", returned map: %s", serviceInstance, customization);
        
        // Apply name dependency filters possibly returned by the init() method.
        
        for (MetaData dependency: m_depsMeta)
        {
            // Check if this dependency has a name, and if we find the name from the 
            // customization map, then apply filters and required flag from the map into it.
            // Also parse optional pid/propagate flags for named Configuration dependencies

            String name = dependency.getString(Params.name, null);
            if (name != null)
            {
                String filter = customization.get(name + ".filter");
                String required = customization.get(name + ".required");
                String pid = customization.get(name + ".pid");
                String propagate = customization.get(name + ".propagate");

                if (filter != null || required != null || pid != null || propagate != null)
                {
                    dependency = (MetaData) dependency.clone();
                    if (filter != null)
                    {
                        dependency.setString(Params.filter, filter);
                    }
                    if (required != null)
                    {
                        dependency.setString(Params.required, required);
                    }
                    if (pid != null)
                    {
                        dependency.setString(Params.pid, pid);
                    }
                    if (propagate != null)
                    {
                        dependency.setString(Params.propagate, propagate);
                    }
                }

                DependencyBuilder depBuilder = new DependencyBuilder(dependency);
                Log.instance().info("ServiceLifecycleHandler.init: adding dependency %s into service %s",
                                   dependency, m_srvMeta);
                Dependency d = depBuilder.build(m_bundle, dm);
                instanceBoundDeps.add(d);
            }            
        }
        
        // Add all extra dependencies in one shot, in order to calculate state changes for all dependencies at a time.
        if (instanceBoundDeps.size() > 0) 
        {
            Log.instance().info("ServiceLifecycleHandler.init: adding extra/named dependencies %s",
                instanceBoundDeps);
            c.add(instanceBoundDeps.toArray(new Dependency[instanceBoundDeps.size()]));
        }    

        // init method fully handled, and all possible named dependencies have been configured. Now, activate the 
        // hidden toggle, and then remove it from the component, because we don't need it anymore.
        if (initToggle != null) 
        {
            c.remove(initToggle);
        } 
    }

    /**
     * Handles the Service's start lifecycle callback. We just invoke the service "start" service callback on 
     * the service instance, as well as on all eventual service composites.
     * We take care to check if a start callback returns a Map, which is meant to contain
     * some additional properties which must be appended to existing service properties.
     * Such extra properties takes precedence over existing service properties.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void start(Component service)
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        // Check if some extra service properties are returned by start method.
        
        DependencyManager dm = service.getDependencyManager();
        Map<String, String> extraProperties = new HashMap<String, String>();
        Object[] composites = service.getInstances();
        for (Object composite: composites)
        {
            Object o = invokeMethod(composite, m_start, dm, service);
            if (o != null && Map.class.isAssignableFrom(o.getClass()))
            {
                extraProperties.putAll((Map) o);
            }
        }

        if (extraProperties.size() > 0)
        {
            // Store extra properties returned by start callbacks into existing service properties
            Dictionary existingProperties = service.getServiceProperties();
            if (existingProperties != null)
            {
                Hashtable props = new Hashtable();
                Enumeration e = existingProperties.keys();
                while (e.hasMoreElements())
                {
                    Object key = e.nextElement();
                    props.put(key, existingProperties.get(key));
                }
                props.putAll(extraProperties);
                service.setServiceProperties(props);
            }
            else
            {
                service.setServiceProperties(new Hashtable(extraProperties));
            }
        }        
    }

    /**
     * Handles the Service's stop lifecycle callback. We just invoke the service "stop" callback on 
     * the service instance, as well as on all eventual service composites.
     */
    public void stop(Component service)
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        callbackComposites(service, m_stop);
    }

    /**
     * Handles the Service's destroy lifecycle callback. We just invoke the service "destroy" callback on 
     * the service instance, as well as on all eventual service composites.
     */
    public void destroy(Component service)
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        callbackComposites(service, m_destroy);
    }

    /**
     * Invoke a callback on all Service compositions.
     */
    private void callbackComposites(Component service, String callback)
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        Object[] composites = service.getInstances();
        for (Object composite: composites)
        {
            invokeMethod(composite, callback, service.getDependencyManager(), service);
        }
    }

    /**
     * Invoke a callback on an Object instance.
     */
    private Object invokeMethod(Object serviceInstance, String method, DependencyManager dm, Component c)
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        if (method != null)
        {
            try
            {
                return InvocationUtil.invokeCallbackMethod(serviceInstance, method,
                                                           new Class[][] { { Component.class }, {} },
                                                           new Object[][] { { c }, {} }
                    );
            }

            catch (NoSuchMethodException e)
            {
                // ignore this
            }

            // Other exception will be thrown up to the ServiceImpl.invokeCallbackMethod(), which is 
            // currently invoking our method. So, no need to log something here, since the invokeCallbackMethod 
            // method is already logging any thrown exception.
        }
        return null;
    }

    /**
     * Sets a field of an object by reflexion.
     */
    private void setField(Object instance, String fieldName, Class<?> fieldClass, Object fieldValue)
    {
        Object serviceInstance = instance;
        Class<?> serviceClazz = serviceInstance.getClass();
        if (Proxy.isProxyClass(serviceClazz))
        {
            serviceInstance = Proxy.getInvocationHandler(serviceInstance);
            serviceClazz = serviceInstance.getClass();
        }
        while (serviceClazz != null)
        {
            Field[] fields = serviceClazz.getDeclaredFields();
            for (int j = 0; j < fields.length; j++)
            {
                Field field = fields[j];
                Class<?> type = field.getType();
                if (field.getName().equals(fieldName) && type.isAssignableFrom(fieldClass))
                {
                    try
                    {
                        field.setAccessible(true);
                        // synchronized makes sure the field is actually written to immediately
                        synchronized (SYNC)
                        {
                            field.set(serviceInstance, fieldValue);
                        }
                    }
                    catch (Throwable e)
                    {
                        throw new RuntimeException("Could not set field " + field, e);
                    }
                }
            }
            serviceClazz = serviceClazz.getSuperclass();
        }
    }
    
    private static class ComponentStarter implements Runnable {
        private final String m_componentName;
        private final ToggleServiceDependency m_toggle;
        private final AtomicBoolean m_startFlag;

        public ComponentStarter(String name, ToggleServiceDependency toggle, AtomicBoolean startFlag)
        {
            m_componentName = name;
            m_toggle = toggle;
            m_startFlag = startFlag;
        }

        @SuppressWarnings("synthetic-access")
        public void run()
        {
            if (m_startFlag.compareAndSet(false, true)) {
                Log.instance().debug("Lifecycle controller is activating the component %s",
                                     m_componentName);
                m_toggle.activate(true);
            }
        }
    }
    
    private static class ComponentStopper implements Runnable {
        private final Object m_componentName;
        private final ToggleServiceDependency m_toggle;
        private final AtomicBoolean m_startFlag;

        public ComponentStopper(String componentName, ToggleServiceDependency toggle, AtomicBoolean startFlag)
        {
            m_componentName = componentName;
            m_toggle = toggle;
            m_startFlag = startFlag;
        }

        @SuppressWarnings("synthetic-access")
        public void run()
        {
            if (m_startFlag.compareAndSet(true, false)) {
                Log.instance().debug("Lifecycle controller is deactivating the component %s",
                                    m_componentName);
                m_toggle.activate(false);
            }
        }
    }
}
