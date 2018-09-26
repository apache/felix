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
 * a Dependency Manager adapter component.
 * 
 * Adapters, like {@link AspectComponent}, are used to "extend" 
 * existing services, and can publish different services based on the existing one. 
 * An example would be implementing a management interface for an existing service, etc .... 
 * <p>When you create an adapter component, it will be applied 
 * to any service that matches the implemented interface and filter. The adapter will be registered 
 * with the specified interface and existing properties from the original service plus any extra 
 * properties you supply here. If you declare the original service as a member it will be injected.
 * 
 * <h3>Usage Examples</h3>
 * 
 * Here is a sample showing a HelloServlet adapter component which creates a servlet each time a HelloService is registered in the
 * osgi service registry with the "foo=bar" service property.
 * 
 * <blockquote><pre>
 * {@code
 * public class Activator extends DependencyActivatorBase {
 *     &Override
 *     public void init(BundleContext context, DependencyManager dm) throws Exception {
 *         Component adapterComponent = createAdapterComponent()
 *             .setAdaptee(HelloService.class, "(foo=bar)")
 *             .setInterface(HttpServlet.class.getName(), null)
 *             .setImplementation(HelloServlet.class);
 *         dm.add(adapterComponent);
 *     }
 * }
 * 
 * public interface HelloService {
 *     String sayHello();
 * }
 * 
 * public class HelloServlet extends HttpServlet {
 *     volatile HelloService adatpee; // injected
 *     
 *     void doGet(HttpServletRequest req, HttpServletResponse resp) {
 *         ...
 *         resp.getWriter().println(adaptee.sayHello());
 *     }
 * }
 * } </pre></blockquote>
 * 
 * <p> When you use callbacks to get injected with the adaptee service, the "add", "change", "remove" callbacks
 * support the following method signatures:
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
 * 
 * @see DependencyManager#createAdapterComponent()
 */
public interface AdapterComponent extends Component<AdapterComponent> {
    
    /**
     * Sets the service interface to apply the adapter to
     * @param service the service interface to apply the adapter to
     * @param filter the filter condition to use with the service interface
     * @return this adapter parameter instance
     */
	AdapterComponent setAdaptee(Class<?> service, String filter);
        
    /**
     * Sets the name of the member to inject the service into
     * @param autoConfig the name of the member to inject the service into
     * @return this adapter parameter instance
     */
	AdapterComponent setAdapteeField(String autoConfig);
    
    /**
     * Sets the callbacks to invoke when injecting the adaptee service into the adapter component.
     * @param add name of the callback method to invoke on add
     * @param change name of the callback method to invoke on change
     * @param remove name of the callback method to invoke on remove
     * @param swap name of the callback method to invoke on swap
     * @return this adapter parameter instance
     */
	AdapterComponent setAdapteeCallbacks(String add, String change, String remove, String swap);
    
    /**
     * Sets the instance to invoke the callbacks on (null by default, meaning the callbacks have to be invoked on the adapter itself)
     * @param callbackInstance the instance to invoke the callbacks on (null by default, meaning the callbacks have to be invoked on the adapter itself)
     * @return this adapter parameter instance
     */
	AdapterComponent setAdapteeCallbackInstance(Object callbackInstance);

    /**
     * Sets if the adaptee service properties should be propagated to the adapter service consumer (true by default)
     * @param propagate true if the adaptee service properties should be propagated to the adapter service consumers
     * @return this adapter parameter instance
     */
	AdapterComponent setPropagate(boolean propagate);
    
}
