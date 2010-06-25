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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.dependencies.Dependency;
import org.apache.felix.dm.service.Service;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;

/**
 * Allow Services to configure dynamically their dependency filters from their init() method.
 * Basically, this class acts as a service implementation lifecycle handler. When we detect that the Service is
 * called in its init() method, and if its init() method returns a Map, then the Map is assumed to contain
 * dependency filters, which will be applied to all named service dependencies. The Map optionally returned by
 * Service's init method has to contains the following keys:
 * <ul>
 *   <li>name.filter: the value must be a valid OSGi filter. the "name" prefix must match a ServiceDependency 
 *   name attribute</li>
 *   <li>name.required: the value is a boolean ("true"|"false") and must the "name" prefix must match a 
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
 *      // The returned Map will be used to configure our "dependency1" Dependency.
 *      &#64;Init
 *      Map init() {
 *          return new HashMap() {{
 *              put("dependency1.filter", m_config.get("filter"));
 *              put("dependency1.required", m_config.get("required"));
 *          }};
 *      } 
 *      
 *      &#64;ServiceDependency(name="dependency1") 
 *      void bindOtherService(OtherService other) {
 *         // the filter and required flag will be configured from our init method.
 *      }
 *  }
 *  </pre></blockquote>
 */
public class ServiceLifecycleHandler
{
    private String m_init;
    private String m_start;
    private String m_stop;
    private String m_destroy;
    private MetaData m_srvMeta;
    private List<MetaData> m_depsMeta;
    private List<Dependency> m_namedDeps = new ArrayList<Dependency>();
    private Bundle m_bundle;

    /**
     * Makes a new ServiceLifecycleHandler object. This objects allows to decorate the "init" service callback, in
     * order to see if "init" callback returns a dependency customization map.
     * 
     * @param srv The Service for the annotated class
     * @param srvBundle the Service bundle
     * @param dm The DependencyManager bundle
     * @param srvMeta The Service MetaData
     * @param depMeta The Dependencies MetaData
     */
    public ServiceLifecycleHandler(Service srv, Bundle srvBundle, DependencyManager dm,
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
     * Handles an "init" lifecycle service callback. We just catch the "init" method, and callback ourself 
     * the actual Service' init method, to see if it returns a dependency customization map.
     * 
     * @param service The Annotated Service
     * @throws Exception on any errors
     */
    @SuppressWarnings("unchecked")
    public void init(Service service)
        throws Exception
    {
        Object serviceInstance = service.getService();
        DependencyManager dm = service.getDependencyManager(); 
        
        // Invoke all composites' init methods, and for each one, check if a dependency
        // customization map is returned by the method. This map will be used to configure 
        // some dependency filters (or required flag).
      
        Map<String, String> customization = new HashMap<String, String>();
        Object[] composites = service.getCompositionInstances();
        for (Object composite : composites)
        {
            Object o = invokeMethod(composite, m_init, dm, service);
            if (o != null && Map.class.isAssignableFrom(o.getClass()))
            {
                customization.putAll((Map) o);
            }
        }
       
        Log.instance().log(LogService.LOG_DEBUG,
                           "ServiceLifecycleHandler.init: invoked init method from service %s " +
                           ", returned map: %s", serviceInstance, customization); 
                                 
        for (MetaData dependency : m_depsMeta) 
        {
            // Check if this dependency has a name, and if we find the name from the 
            // customization map, then apply filters and required flag from the map into it.
            
            String name = dependency.getString(Params.name, null);
            if (name != null) {
                String filter = customization.get(name + ".filter");
                String required = customization.get(name + ".required");

                if (filter != null || required != null)
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
                }

                DependencyBuilder depBuilder = new DependencyBuilder(dependency);
                Log.instance().log(LogService.LOG_INFO,
                                   "ServiceLifecycleHandler.init: adding dependency %s into service %s",
                                   dependency, m_srvMeta);
                Dependency d = depBuilder.build(m_bundle, dm, true);
                m_namedDeps.add(d);
                service.add(d);
            }
        }        
    }
    
    /**
     * Catches the Service's start lifecycle callback. We just invoke ourself the service "start" callback on 
     * the service instance, as well as on all eventual service composites.
     * 
     * @param service The Annotated Service
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public void start(Service service)
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        callbackComposites(service, m_start);
    }

    /**
     * Catches the Service's stop lifecycle callback. We just invoke ourself the service "stop" callback on 
     * the service instance, as well as on all eventual service composites.
     * 
     * @param service The Annotated Service
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public void stop(Service service)
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        callbackComposites(service, m_stop);
    }

    /**
     * Catches the Service's destroy lifecycle callback. We just invoke ourself the service "destroy" callback on 
     * the service instance, as well as on all eventual service composites.
     * 
     * @param service The Annotated Service
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public void destroy(Service service)
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        callbackComposites(service, m_destroy);
    }

    /**
     * Invoke a callback on the actual Service.
     * 
     * @param service the Service used to managed the annotated Service
     * @param callback the callback to invoke (init, start, stop, or destroy)
     */
    private void callbackComposites(Service service, String callback) 
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        Object serviceInstance = service.getService();
        Object[] composites = service.getCompositionInstances();
        for (Object composite: composites)
        {
            invokeMethod(composite, callback, service.getDependencyManager(), service);
        }
    }

    /**
     * Invoke a callback on an Object instance.
     */
    private Object invokeMethod(Object serviceInstance, String method, DependencyManager dm, Service service)
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        if (method != null) {
            try 
            {
                return InvocationUtil.invokeCallbackMethod(
                    serviceInstance, method, 
                    new Class[][] { { Service.class }, {} }, 
                    new Object[][] { { service }, {} }
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
}
