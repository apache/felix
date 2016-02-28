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
package org.apache.felix.dm.lambda;

import org.apache.felix.dm.lambda.callbacks.CbRefServiceRefService;
import org.apache.felix.dm.lambda.callbacks.CbRefServiceRefServiceComponent;
import org.apache.felix.dm.lambda.callbacks.CbService;
import org.apache.felix.dm.lambda.callbacks.CbServiceComponent;
import org.apache.felix.dm.lambda.callbacks.CbServiceComponentRef;
import org.apache.felix.dm.lambda.callbacks.CbServiceDict;
import org.apache.felix.dm.lambda.callbacks.CbServiceMap;
import org.apache.felix.dm.lambda.callbacks.CbServiceRef;
import org.apache.felix.dm.lambda.callbacks.CbServiceService;
import org.apache.felix.dm.lambda.callbacks.CbServiceServiceComponent;
import org.apache.felix.dm.lambda.callbacks.InstanceCbRefServiceRefService;
import org.apache.felix.dm.lambda.callbacks.InstanceCbRefServiceRefServiceComponent;
import org.apache.felix.dm.lambda.callbacks.InstanceCbService;
import org.apache.felix.dm.lambda.callbacks.InstanceCbServiceComponent;
import org.apache.felix.dm.lambda.callbacks.InstanceCbServiceComponentRef;
import org.apache.felix.dm.lambda.callbacks.InstanceCbServiceDict;
import org.apache.felix.dm.lambda.callbacks.InstanceCbServiceMap;
import org.apache.felix.dm.lambda.callbacks.InstanceCbServiceRef;
import org.apache.felix.dm.lambda.callbacks.InstanceCbServiceService;
import org.apache.felix.dm.lambda.callbacks.InstanceCbServiceServiceComponent;

/**
 * Builds a service dependency callback. 
 * 
 * <p> A Service may be injected in a bind-method of a component or an object instance using this builder.
 * The builder supports reflection based callbacks (same as with the original DM API), as well as java8 method reference based callbacks.
 * 
 * <p> <b> List of signatures supported using reflection based callbacks (same as original DM API): </b>
 * 
 * <pre> {@code
 * method(S service)
 * method(S service, Map<String, Object> serviceProperties)
 * method(S service, Dictionary<String, Object> serviceProperties)
 * method(ServiceReference<S> serviceRef, S service),
 * method(ServiceReference<S> serviceRef)
 * method(Component serviceComponent)
 * method(Component serviceComponent, ServiceReference<S> serviceRef)
 * method(Component serviceComponent, S service) 
 * method(Component serviceComponent, ServiceReference<S> serviceRef, S service)
 * swapMethod(S oldService, S newService)
 * swapMethod(ServiceReference<S> oldRef, S old, ServiceReference<S> newRef, S newService)
 * swapMethod(Component component, S oldService, S newService)
 * swapMethod(Component component, ServiceReference<S> oldRef, S old, ServiceReference<S> newRef, S newService)
 * }</pre>
 * 
 * <b> List of signatures supported using java 8 method references: </b>
 *
 * <pre> {@code
 * method(S service)
 * method(S service, ServiceReference<S> serviceRef),
 * method(S service, Map<String, Object> serviceProperties)
 * method(S service, Dictionary<String, Object> serviceProperties)
 * method(S service, Component serviceComponent)
 * method(S service, Component serviceComponent, ServiceReference<S> serviceRef)
 * swapMethod(S oldService, S newService)
 * swapMethod(S oldService, S newService, Component component))
 * swapMethod(ServiceReference<S> oldRef, S old, ServiceReference<S> newRef, S newService)
 * swapMethod(ServiceReference<S> oldRef, S old, ServiceReference<S> newRef, S newService, Component component)
 * }</pre>
 * 
 * <p> Here is an example of a Component that defines a dependency of a LogService which is injected in the "setLog" method using a ServiceCallbacksBuilder:
 * The withSvc(...)" declaration defines a method reference on the "Pojo::setLog" method (using a lambda):
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *    public void init(BundleContext ctx, DependencyManager dm) throws Exception { 
 *       component(comp -> comp.impl(Pojo.class).withSvc(LogService.class, log -> log.add(Pojo::setLog)));
 *    }
 * }}</pre>
 *
 * <p> Same example, but we inject the dependency to an object instance that we already have in hand:
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *    public void init(BundleContext ctx, DependencyManager dm) throws Exception {
 *       Pojo impl = new Pojo();
 *       component(comp -> comp.impl(impl).withSvc(LogService.class, log -> log.add(impl::setLog)));
 *    }
 * }}</pre>
 * 
 * <p> Here, we inject a service using method reflection (as it is the case in original DM api):
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *    public void init(BundleContext ctx, DependencyManager dm) throws Exception {
 *       component(comp -> comp.impl(Pojo::class).withSvc(LogService.class, log -> log.add("setLog")));
 *    }
 * }}</pre>
 *
 * @param <S> the service dependency type
 * @param <B> the type of a sub interface that may extends this interface.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ServiceCallbacksBuilder<S, B extends ServiceCallbacksBuilder<S, B>> {
    
    /**
     * Sets the callback instance used for reflection based callbacks.
     * @param callbackInstance the object on which reflection based callbacks are invoked on.
     * @return this builder
     */
    B callbackInstance(Object callbackInstance);
    
    /**
     * Sets a <code>callback</code> method to invoke when a service is added. When a service matches the service 
     * filter, then the service is injected using the specified callback method. The callback is invoked on the component implementation, or on the callback
     * instance, is specified using the {@link #callbackInstance(Object)} method.
     * 
     * The following method signature are supported:
     * <pre>{@code
     * method(S service)
     * method(S service, Map<String, Object> serviceProperties)
     * method(S service, Dictionary<String, Object> serviceProperties)
     * method(ServiceReference<S> serviceRef, S service),
     * method(ServiceReference<S> serviceRef)
     * method(Component serviceComponent)
     * method(Component serviceComponent, ServiceReference<S> serviceRef)
     * method(Component serviceComponent, S service) 
     * method(Component serviceComponent, ServiceReference<S> serviceRef, S service)
     * }</pre>
     * 
     * @param callback the add callback
     * @return this builder
     * @see #callbackInstance(Object)
     */
    B add(String callback);
    
    /**
     * Sets a <code>callback</code> methods to invoke when a service is changed. When a changed service matches the service 
     * filter, then the service is injected using the specified callback method. The callback is invoked on the component implementation, or on the callback
     * instance, is specified using the {@link #callbackInstance(Object)} method.
     * 
     * The following method signature are supported:
     * <pre>{@code
     * method(S service)
     * method(S service, Map<String, Object> serviceProperties)
     * method(S service, Dictionary<String, Object> serviceProperties)
     * method(ServiceReference<S> serviceRef, S service),
     * method(ServiceReference<S> serviceRef)
     * method(Component serviceComponent)
     * method(Component serviceComponent, ServiceReference<S> serviceRef)
     * method(Component serviceComponent, S service) 
     * method(Component serviceComponent, ServiceReference<S> serviceRef, S service)
     * }</pre>
     * 
     * @param callback the change callback
     * @return this builder
     * @see #callbackInstance(Object)
     */
    B change(String callback);

    /**
     * Sets a <code>callback</code> method to invoke when a service is removed. When a removed service matches the service 
     * filter, then the specified callback in invoked with the removed service. The callback is invoked on the component implementation, or on the callback
     * instance, is specified using the {@link #callbackInstance(Object)} method.
     * 
     * The following method signature are supported:
     * <pre>{@code
     * method(S service)
     * method(S service, Map<String, Object> serviceProperties)
     * method(S service, Dictionary<String, Object> serviceProperties)
     * method(ServiceReference<S> serviceRef, S service),
     * method(ServiceReference<S> serviceRef)
     * method(Component serviceComponent)
     * method(Component serviceComponent, ServiceReference<S> serviceRef)
     * method(Component serviceComponent, S service) 
     * method(Component serviceComponent, ServiceReference<S> serviceRef, S service)
     * }</pre>
     * 
     * @param callback the remove callback
     * @return this builder
     * @see #callbackInstance(Object)
     */
    B remove(String callback);
    
    /**
     * Sets a <code>callback</code> method to invoke when a service is swapped. The callback is invoked on the component implementation, or on the callback
     * instance, is specified using the {@link #callbackInstance(Object)} method.
     * 
     * The following method signature are supported:
     * <pre>{@code
     * swapMethod(S oldService, S newService)
     * swapMethod(ServiceReference<S> oldRef, S old, ServiceReference<S> newRef, S newService)
     * swapMethod(Component component, S oldService, S newService)
     * swapMethod(Component component, ServiceReference<S> oldRef, S old, ServiceReference<S> newRef, S newService)
     * }</pre>
     * 
     * @param callback the remove callback
     * @return this builder
     * @see #callbackInstance(Object)
     */
    B swap(String callback);
    
    /**
     * Sets a <code>component callback(Service)</code> method reference. The callback is invoked when a service is added.
     * The method reference must point to a Component implementation class method.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    <T> B add(CbService<T, S> add);
    
    /**
     * Sets a <code>component callback(Service)</code> method reference. The callback is invoked when a service is changed.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param change the method reference invoked when a service is changed.
     * @return this builder
     */
    <T> B change(CbService<T, S> change);
    
    /**
     * Sets a <code>component callback(Service)</code> method reference. The callback is invoked when a service is removed.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B remove(CbService<T, S> remove);
  
    /**
     * Sets a {@code component callback(Service, Map<String, Object>)} method reference. The callback is  invoked when a service is added.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    <T> B add(CbServiceMap<T, S> add);
    
    /**
     * Sets a {@code component callback(Service, Map<String, Object>)} method reference. The callback is  invoked when a service is changed.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param change the method reference invoked when a service is changed.
     * @return this builder
     */
    <T> B change(CbServiceMap<T, S> change);
  
    /**
     * Sets a {@code component callback(Service, Map<String, Object></code>)} method reference. The callback is  invoked when a service is removed.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B remove(CbServiceMap<T, S> remove);
    
    /**
     * Sets a {@code component callback(Service, Dictionary<String, Object>)} method reference. The callback is  invoked when a service is added.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    <T> B add(CbServiceDict<T, S> add);
    
    /**
     * Sets a {@code component callback(Service, Dictionary<String, Object>)} method reference. The callback is  invoked when a service is changed.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param change the method reference invoked when a service is changed.
     * @return this builder
     */
    <T> B change(CbServiceDict<T, S> change);

    /**
     * Sets a {@code component callback(Service, Dictionary<String, Object>)} method reference. The callback is  invoked when a service is removed.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B remove(CbServiceDict<T, S> remove);
    
    /**
     * Sets a <code>component callback(Service, ServiceReference)</code> method reference. The callback is  invoked when a service is added.
     * The method reference must point to a Component implementation class method. 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    <T> B add(CbServiceRef<T, S> add);
  
    /**
     * Sets a <code>component callback(Service, ServiceReference)</code> method reference. The callback is  invoked when a service is changed.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param change the method reference invoked when a service is changed.
     * @return this builder
     */
    <T> B change(CbServiceRef<T, S> change);

    /**
     * Sets a <code>component callback(Service, ServiceReference)</code> method reference. The callback is  invoked when a service is removed.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B remove(CbServiceRef<T, S> remove);
      
    /**
     * Sets a <code>component callback(Service, Component)</code> method reference. The callback is invoked when a service is added.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    <T> B add(CbServiceComponent<T, S> add);

    /**
     * Sets a <code>component callback(Service, Component)</code> method reference. The callback is invoked when a service is changed.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param change the method reference invoked when a service is changed.
     * @return this builder
     */
    <T> B change(CbServiceComponent<T, S> change);

    /**
     * Sets a <code>component callback(Service, Component)</code> method reference. The callback is invoked when a service is removed.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B remove(CbServiceComponent<T, S> remove);
    
    /**
     * Sets a <code>component callback(Service, Component, ServiceReference ref)</code> method reference. The callback is invoked when a service is added.
     * The method reference must point to a Component implementation class method.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    <T> B add(CbServiceComponentRef<T, S> add);
 
    /**
     * Sets a <code>component callback(Service, Component, ServiceReference)</code> method reference. The callback is invoked when a service is changed.
     * The method reference must point to a Component implementation class method.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param change the method reference invoked when a service is changed.
     * @return this builder
     */
    <T> B change(CbServiceComponentRef<T, S> change);

    /**
     * Sets a <code>component callback(Service, Component, ServiceReference)</code> method reference. The callback is invoked when a service is removed.
     * The method reference must point to a Component implementation class method.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B remove(CbServiceComponentRef<T, S> remove);
    
    /**
     * Sets an <code>Object instance callback(Service)</code> method reference. The callback is invoked when a service is added.
     * The method reference must point to a method from an Object instance. 
     * 
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    B add(InstanceCbService<S> add);
    
    /**
     * Sets an <code>Object instance callback(Service)</code> method reference. The callback is invoked when a service is changed.
     * The method reference must point to method from an Object instance.
     * 
     * @param change the method reference invoked when a service is changed.
     * @return this builder
     */
    B change(InstanceCbService<S> change);

    /**
     * Sets an <code>Object instance callback(Service)</code> method reference. The callback is invoked when a service is removed.
     * The method reference must point to method from an Object instance. 
     * 
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B remove(InstanceCbService<S> remove);
  
    /**
     * Sets an {@code Object instance callback(Service, Map<String, Object>)} method reference. The callback is invoked when a service is added.
     * The method reference must point to a method from an Object instance. 
     * 
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    B add(InstanceCbServiceMap<S> add);
  
    /**
     * Sets an {@code Object instance callback(Service, Map<String, Object>)} method reference. The callback is invoked when a service is changed.
     * The method reference must point to method from an Object instance. 
     * 
     * @param change the method reference invoked when a service is changed.
     * @return this builder
     */
    B change(InstanceCbServiceMap<S> change);

    /**
     * Sets an {@code Object instance callback(Service, Map<String, Object>)} method reference. The callback is invoked when a service is removed.
     * The method reference must point to method from an Object instance. 
     * 
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B remove(InstanceCbServiceMap<S> remove);
  
    /**
     * Sets an {@code Object instance callback(Service svc, Dictionary<String, Object>} method reference. The callback is invoked when a service is added.
     * The method reference must point to a method from an Object instance. 
     * 
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    B add(InstanceCbServiceDict<S> add);
   
    /**
     * Sets an {@code Object instance callback(Service, Dictionary<String, Object>)} method reference. The callback is invoked when a service is changed.
     * The method reference must point to method from an Object instance. 
     * 
     * @param change the method reference invoked when a service is changed.
     * @return this builder
     */
    B change(InstanceCbServiceDict<S> change);

    /**
     * Sets an {@code Object instance callback(Service, Dictionary<String, Object>)} method reference. The callback is invoked when a service is removed.
     * The method reference must point to method from an Object instance.
     * 
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B remove(InstanceCbServiceDict<S> remove);
 
    /**
     * Sets an <code>Object instance callback(Service, ServiceReference)</code> method reference. The callback is invoked when a service is added.
     * The method reference must point to a method from an Object instance. 
     * 
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    B add(InstanceCbServiceRef<S> add);
  
    /**
     * Sets an <code>Object instance callback(Service, ServiceReference)</code> method reference. The callback is invoked when a service is changed.
     * The method reference must point to method from an Object instance. 
     * 
     * @param change the method reference invoked when a service is changed.
     * @return this builder
     */
    B change(InstanceCbServiceRef<S> change);

    /**
     * Sets an <code>Object instance callback(Service, ServiceReference)</code> method reference. The callback is invoked when a service is removed.
     * The method reference must point to method from an Object instance. 
     * 
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B remove(InstanceCbServiceRef<S> remove);
    
    /**
     * Sets an <code>Object instance callback(Service, Component)</code> method reference. The callback is when a service is added.
     * The method reference must point to a method from an Object instance. 
     * 
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    B add(InstanceCbServiceComponent<S> add);    
  
    /**
     * Sets an <code>Object instance callback(Service, Component)</code> method reference. The callback is when a service is changed.
     * The method reference must point to method from an Object instance.
     * 
     * @param change the method reference invoked when a service is changed.
     * @return this builder
     */
    B change(InstanceCbServiceComponent<S> change);

    /**
     * Sets an <code>Object instance callback(Service, Component)</code> method reference. The callback is invoked when a service is removed.
     * The method reference must point to method from an Object instance. 
     * 
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B remove(InstanceCbServiceComponent<S> remove);
   
    /**
     * Sets an <code>Object instance callback(Service, Component, ServiceReference)</code> method reference. The callback is invoked when a service is added.
     * The method reference must point to a method from an Object instance. 
     * 
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    B add(InstanceCbServiceComponentRef<S> add);
   
    /**
     * Sets an <code>Object instance callback(Service, Component, ServiceReference)</code> method reference. The callback is invoked when a service is changed.
     * The method reference must point to method from an Object instance. 
     * 
     * @param change the method reference invoked when a service is changed.
     * @return this builder
     */
    B change(InstanceCbServiceComponentRef<S> change);
    
    /**
     * Sets an <code>Object instance callback(Service, Component, ServiceReference)</code> method reference. The callback is invoked when a service is removed.
     * The method reference must point to method from an Object instance. 
     * 
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B remove(InstanceCbServiceComponentRef<S> remove);
  
    /**
     * Sets a swap <code>component callback(Service, Service)</code> method reference. The callback is invoked when a service is swapped.
     * The method reference must point to a Component implementation class method.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param swap the method reference invoked when the service is swapped.
     * @return this builder
     */
    <T> B swap(CbServiceService<T, S> swap);
 
    /**
     * Sets a wap <code>component callback(Service, Service, Component)</code> method reference. The callback is invoked when a service is swapped.
     * The method reference must point to a Component implementation class method.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param swap the method reference invoked when the service is swapped.
     * @return this builder
     */
    <T> B swap(CbServiceServiceComponent<T, S> swap);
    
    /**
     * Sets a swap <code>component callback(ServiceReference, Service, ServiceReference, Service)</code> method reference. The callback is invoked when a service is swapped.
     * The method reference must point to a Component implementation class method. 
     * the new service.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param swap the method reference invoked when the service is swapped.
     * @return this builder
     */
    <T> B swap(CbRefServiceRefService<T, S> swap);
    
    /**
     * Sets a swap <code>component callback(ServiceReference, Service, ServiceReference, Service, Component</code> method reference. The callback is invoked when a service is swapped.
     * The method reference must point to a Component implementation class method. 
     * the new service.
     * 
     * @param <T> the type of the component implementation class on which the callback is invoked.
     * @param swap the method reference invoked when the service is swapped.
     * @return this builder
     */
    <T> B swap(CbRefServiceRefServiceComponent<T, S> swap);

    /**
     * Sets a swap <code>instance callback(Service, Service)</code> method reference. The callback is invoked when a service is swapped.
     * The method reference must point to a method from an Object instance. 
     *
     * @param swap the method reference invoked when the service is swapped.
     * @return this builder
     */
    B swap(InstanceCbServiceService<S> swap);
   
    /**
     * Sets a swap <code>instance callback(Service, Service, Component)</code> method reference. The callback is invoked when a service is swapped.
     * The method reference must point to a method from an Object instance. 
     *
     * @param swap the method reference invoked when the service is swapped.
     * @return this builder
     */
    B swap(InstanceCbServiceServiceComponent<S> swap);
  
    /**
     * Sets a swap <code>instance callback(ServiceReference, Service, ServiceReference, Service)</code> method reference. The callback is invoked when a service is swapped.
     * The method reference must point to a method from an Object instance. 
     *
     * @param swap the method reference invoked when the service is swapped.
     * @return this builder
     */
    B swap(InstanceCbRefServiceRefService<S> swap);
  
    /**
     * Sets a swap <code>instance callback(ServiceReference, Service, ServiceReference, Service, Component)</code> method reference. The callback is invoked when a service is swapped.
     * The method reference must point to a method from an Object instance.  
     *
     * @param swap the method reference invoked when the service is swapped.
     * @return this builder
     */
    B swap(InstanceCbRefServiceRefServiceComponent<S> swap);        
}
