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

/**
 * Interface used to configure the various parameters needed when defining 
 * a Dependency Manager aspect component.
 * 
 * Aspects allow you to define an interceptor, or chain of interceptors 
 * for a service (to add features like caching or logging, etc ...). The dependency manager intercepts 
 * the original service, and allows you to execute some code before invoking the original service ...
 * The aspect will be applied to any service that matches the specified interface and filter and 
 * will be registered with the same interface and properties as the original service, plus any 
 * extra properties you supply here. If you declare the original service as a member it will be injected.
 * 
 * <h3>Usage Examples</h3>
 * 
 * Here is a sample showing a DatabaseCache aspect which is created each time a Database interface is registered in the registry.
 * 
 * <blockquote><pre>
 * {@code
 * public class Activator extends DependencyActivatorBase {
 *     &Override
 *     public void init(BundleContext context, DependencyManager dm) throws Exception {
 *         Component aspectComponent = createAspectComponent()
 *             .setAspect(Database.class, null, 10)
 *             .setImplementation(DatabaseCache.class);
 *         dm.add(aspectComponent);
 *     }
 * }
 * 
 * interface Database {
 *     String get(String key);
 * }
 * 
 * class DatabaseCache implements Database {
 *     volatile Database originalDatabase; // injected
 *     
 *     String get(String key) {
 *         String value = cache.get(key);
 *         if (value == null) {
 *             value = this.originalDatabase.get(key);
 *             store(key, value);
 *         }
 *         return value;
 *     }
 *     ... 
 * }
 * } </pre></blockquote>
 * 
 * <p> For "add", "change", "remove" callbacks, the following method signatures are supported:
 * 
 * <pre>{@code
 * (Component comp, ServiceReference ref, Service service)
 * (Component comp, ServiceReference ref, Object service)
 * (Component comp, ServiceReference ref)
 * (Component comp, Service service)
 * (Component comp, Object service)
 * (Component comp)
 * (Component comp, Map properties, Service service)
 * (ServiceReference ref, Service service)
 * (ServiceReference ref, Object service)
 * (ServiceReference ref)
 * (Service service)
 * (Service service, Map propeerties)
 * (Map properties, Service, service)
 * (Service service, Dictionary properties)
 * (Dictionary properties, Service service)
 * (Object service)
 * }</pre>
 * 
 * <p> For "swap" callbacks, the following method signatures are supported:
 * 
 * <pre>{@code
 * (Service old, Service replace)
 * (Object old, Object replace)
 * (ServiceReference old, Service old, ServiceReference replace, Service replace)
 * (ServiceReference old, Object old, ServiceReference replace, Object replace)
 * (Component comp, Service old, Service replace)
 * (Component comp, Object old, Object replace)
 * (Component comp, ServiceReference old, Service old, ServiceReference replace, Service replace)
 * (Component comp, ServiceReference old, Object old, ServiceReference replace, Object replace)
 * (ServiceReference old, ServiceReference replace)
 * (Component comp, ServiceReference old, ServiceReference replace)
 * }</pre>
 */
public interface AspectComponent extends Component<AspectComponent> {

    /**
     * Sets the service interface to apply the aspect to (required parameter)
     * @param service the service interface to apply the aspect to
     * @param filter the filter condition to use with the service aspect interface (null if no filter)
     * @param ranking  the level used to organize the aspect chain ordering
     * @return this aspect parameter instance
     */
	AspectComponent setAspect(Class<?> service, String filter, int ranking);
    
    /**
     * Sets the aspect implementation field name where to inject original service (optional parameter).
     * If not set or null, any field matching the original service will be injected.
     * @param autoConfig the aspect implementation field name where to inject original service
     * @return this aspect parameter instance
     */
	AspectComponent setAspectField(String autoConfig);

    /**
     * Sets name of the callbacks method to invoke on add,change,remove, or swap callbacks (optional parameter).
     * @param add name of the callback method to invoke on add
     * @param change name of the callback method to invoke on change
     * @param remove name of the callback method to invoke on remove
     * @param swap name of the callback method to invoke on swap
     * @return this aspect parameter instance
     */
	AspectComponent setAspectCallbacks(String add, String change, String remove, String swap);
    
    /**
     * Sets the instance to invoke the callbacks on (optional parameter). 
     * null means the callbacks will be invoked on the aspect implementation object.
     * @param callbackInstance the instance to invoke the callbacks on
     * @return this aspect parameter instance
     */
	AspectComponent setAspectCallbackInstance(Object callbackInstance);

}
