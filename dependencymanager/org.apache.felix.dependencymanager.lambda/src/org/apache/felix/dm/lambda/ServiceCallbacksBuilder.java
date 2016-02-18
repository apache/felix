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
 * Builds a service dependency callback (required by default).
 * 
 * TODO: fix javadoc for method reference (do the same as in ConfigurationDependencyBuilder: use double quotes ...
 * 
 * A Service may be injected in a bind-method of a component or an object instance using this builder.
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
 * <p> Here is an example of a Component that defines a dependency of a LogService which is injected in the "bindLogService" method using a ServiceCallbacksBuilder:
 * The withSvc(...)" declaration defines a method reference on the "ComponentImpl::bindLogService" method (using a lambda):
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *    public void init(BundleContext ctx, DependencyManager dm) throws Exception { 
 *       component(comp -> comp.impl(ComponentImpl.class).withSvc(LogService.class, log -> log.add(ComponentImpl::bindLogService)));
 *    }
 * }}</pre>
 *
 * <p> Same example, but we inject the dependency in an object instance that we already have in hand:
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *    public void init(BundleContext ctx, DependencyManager dm) throws Exception {
 *       ComponentImpl impl = new ComponentImpl();
 *       component(comp -> comp.impl(impl).withSvc(LogService.class, log -> log.add(impl::bindLogService)));
 *    }
 * }}</pre>
 * 
 * <p> Here, we inject a service using method reflection (as it is the case in original DM api):
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *    public void init(BundleContext ctx, DependencyManager dm) throws Exception {
 *       component(comp -> comp.impl(ComponentImpl::class).withSvc(LogService.class, log -> log.add("bindLogService")));
 *    }
 * }}</pre>
 *
 * <p> Same example, but we inject the dependency in an object instance that we already have in hand:
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *    public void init(BundleContext ctx, DependencyManager dm) throws Exception {
 *       ComponentImpl impl = new ComponentImpl();
 *       component(comp -> comp.impl(impl).withSvc(LogService.class, log -> log.callbackInstance(impl).add("bindLogService")));
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
     * Sets <code>callback</code> methods to invoke when a service is added. When a service matches the service 
     * filter, then the service is injected using the specified callback method. The callback is invoked on the component instances, or on the callback
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
     * Sets <code>callback</code> methods to invoke when a service is changed. When a changed service matches the service 
     * filter, then the service is injected using the specified callback method. The callback is invoked on the component instances, or on the callback
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
     * Sets <code>callback</code> methods to invoke when a service is removed. When a removed service matches the service 
     * filter, then the specified callback in invoked with the removed service. The callback is invoked on the component instances, or on the callback
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
     * Sets <code>callback</code> methods to invoke when a service is swapped. The callback is invoked on the component instances, or on the callback
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
     * Sets a <code>component instance callback(Service)</code> callback. The callback is invoked when a service is added.
     * The method reference must point to a Component implementation class method.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    <T> B add(CbService<T, S> add);
    
    /**
     * Sets a <code>component instance callback(Service)</code> callback. The callback is invoked when a service is changed.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param change the method reference invoked when a service is changed.
     * @return this builder
     */
    <T> B change(CbService<T, S> change);
    
    /**
     * Sets a <code>component instance callback(Service)</code> callback. The callback is invoked when a service is removed.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B remove(CbService<T, S> remove);
  
    /**
     * Sets a {@code component instance callback(Service, Map<String, Object>)} callback. The callback is  invoked when a service is added.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    <T> B add(CbServiceMap<T, S> add);
    
    /**
     * Sets a {@code component instance callback(Service, Map<String, Object>)} callback. The callback is  invoked when a service is changed.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param change the method reference invoked when a service is changed.
     * @return this builder
     */
    <T> B change(CbServiceMap<T, S> change);
  
    /**
     * Sets a {@code component instance callback(Service, Map<String, Object></code>)} callback. The callback is  invoked when a service is removed.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B remove(CbServiceMap<T, S> remove);
    
    /**
     * Sets a {@code component instance callback(Service, Dictionary<String, Object>)} callback. The callback is  invoked when a service is added.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    <T> B add(CbServiceDict<T, S> add);
    
    /**
     * Sets a {@code component instance callback(Service, Dictionary<String, Object>)} callback. The callback is  invoked when a service is changed.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param change the method reference invoked when a service is changed.
     * @return this builder
     */
    <T> B change(CbServiceDict<T, S> change);

    /**
     * Sets a {@code component instance callback(Service, Dictionary<String, Object>)} callback. The callback is  invoked when a service is removed.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B remove(CbServiceDict<T, S> remove);
    
    /**
     * Sets a <code>component instance callback(Service, ServiceReference)</code> callback. The callback is  invoked when a service is added.
     * The method reference must point to a Component implementation class method. 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    <T> B add(CbServiceRef<T, S> add);
  
    /**
     * Sets a <code>component instance callback(Service, ServiceReference)</code> callback. The callback is  invoked when a service is changed.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param change the method reference invoked when a service is changed.
     * @return this builder
     */
    <T> B change(CbServiceRef<T, S> change);

    /**
     * Sets a <code>component instance callback(Service, ServiceReference)</code> callback. The callback is  invoked when a service is removed.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B remove(CbServiceRef<T, S> remove);
      
    /**
     * Sets a <code>component instance callback(Service, Component)</code> callback. The callback is invoked when a service is added.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    <T> B add(CbServiceComponent<T, S> add);

    /**
     * Sets a <code>component instance callback(Service, Component)</code> callback. The callback is invoked when a service is changed.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param change the method reference invoked when a service is changed.
     * @return this builder
     */
    <T> B change(CbServiceComponent<T, S> change);

    /**
     * Sets a <code>component instance callback(Service, Component)</code> callback. The callback is invoked when a service is removed.
     * The method reference must point to a Component implementation class method. 
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B remove(CbServiceComponent<T, S> remove);
    
    /**
     * Sets a <code>component instance callback(Service, Component, ServiceReference ref)</code> callback. The callback is invoked when a service is added.
     * The method reference must point to a Component implementation class method.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    <T> B add(CbServiceComponentRef<T, S> add);
 
    /**
     * Sets a <code>component instance callback(Service, Component, ServiceReference)</code> callback. The callback is invoked when a service is changed.
     * The method reference must point to a Component implementation class method.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param change the method reference invoked when a service is changed.
     * @return this builder
     */
    <T> B change(CbServiceComponentRef<T, S> change);

    /**
     * Sets a <code>component instance callback(Service, Component, ServiceReference)</code> callback. The callback is invoked when a service is removed.
     * The method reference must point to a Component implementation class method.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B remove(CbServiceComponentRef<T, S> remove);
    
    /**
     * Sets an <code>object instance callback(Service)</code> callback. The callback is invoked when a service is added.
     * The method reference must point to a method from an Object instance. 
     * 
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    B add(InstanceCbService<S> add);
    
    /**
     * Sets an <code>object instance callback(Service)</code> callback. The callback is invoked when a service is changed.
     * The method reference must point to method from an Object instance.
     * 
     * @param change the method reference invoked when a service is changed.
     * @return this builder
     */
    B change(InstanceCbService<S> change);

    /**
     * Sets an <code>object instance callback(Service)</code> callback. The callback is invoked when a service is removed.
     * The method reference must point to method from an Object instance. 
     * 
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B remove(InstanceCbService<S> remove);
  
    /**
     * Sets an {@code object instance callback(Service, Map<String, Object>)} callback. The callback is invoked when a service is added.
     * The method reference must point to a method from an Object instance. 
     * 
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    B add(InstanceCbServiceMap<S> add);
  
    /**
     * Sets an {@code object instance callback(Service, Map<String, Object>)} callback. The callback is invoked when a service is changed.
     * The method reference must point to method from an Object instance. 
     * 
     * @param change the method reference invoked when a service is changed.
     * @return this builder
     */
    B change(InstanceCbServiceMap<S> change);

    /**
     * Sets an {@code object instance callback(Service, Map<String, Object>)} callback. The callback is invoked when a service is removed.
     * The method reference must point to method from an Object instance. 
     * 
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B remove(InstanceCbServiceMap<S> remove);
  
    /**
     * Sets an {@code object instance callback(Service svc, Dictionary<String, Object>} callback. The callback is invoked when a service is added.
     * The method reference must point to a method from an Object instance. 
     * 
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    B add(InstanceCbServiceDict<S> add);
   
    /**
     * Sets an {@code object instance callback(Service, Dictionary<String, Object>)} callback. The callback is invoked when a service is changed.
     * The method reference must point to method from an Object instance. 
     * 
     * @param change the method reference invoked when a service is changed.
     * @return this builder
     */
    B change(InstanceCbServiceDict<S> change);

    /**
     * Sets an {@code object instance callback(Service, Dictionary<String, Object>)} callback. The callback is invoked when a service is removed.
     * The method reference must point to method from an Object instance.
     * 
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B remove(InstanceCbServiceDict<S> remove);
 
    /**
     * Sets an <code>object instance callback(Service, ServiceReference)</code> callback. The callback is invoked when a service is added.
     * The method reference must point to a method from an Object instance. 
     * 
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    B add(InstanceCbServiceRef<S> add);
  
    /**
     * Sets an <code>object instance callback(Service, ServiceReference)</code> callback. The callback is invoked when a service is changed.
     * The method reference must point to method from an Object instance. 
     * 
     * @param change the method reference invoked when a service is changed.
     * @return this builder
     */
    B change(InstanceCbServiceRef<S> change);

    /**
     * Sets an <code>object instance callback(Service, ServiceReference)</code> callback. The callback is invoked when a service is removed.
     * The method reference must point to method from an Object instance. 
     * 
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B remove(InstanceCbServiceRef<S> remove);
    
    /**
     * Sets an <code>object instance callback(Service, Component)</code> callback invoked. The callback is when a service is added.
     * The method reference must point to a method from an Object instance. 
     * 
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    B add(InstanceCbServiceComponent<S> add);    
  
    /**
     * Sets an <code>object instance callback(Service, Component)</code> callback invoked. The callback is when a service is changed.
     * The method reference must point to method from an Object instance.
     * 
     * @param change the method reference invoked when a service is changed.
     * @return this builder
     */
    B change(InstanceCbServiceComponent<S> change);

    /**
     * Sets an <code>object instance callback(Service, Component)</code> callback. The callback is invoked when a service is removed.
     * The method reference must point to method from an Object instance. 
     * 
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B remove(InstanceCbServiceComponent<S> remove);
   
    /**
     * Sets an <code>object instance callback(Service, Component, ServiceReference)</code> callback. The callback is invoked when a service is added.
     * The method reference must point to a method from an Object instance. 
     * 
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    B add(InstanceCbServiceComponentRef<S> add);
   
    /**
     * Sets an <code>object instance callback(Service, Component, ServiceReference)</code> callback. The callback is invoked when a service is changed.
     * The method reference must point to method from an Object instance. 
     * 
     * @param change the method reference invoked when a service is changed.
     * @return this builder
     */
    B change(InstanceCbServiceComponentRef<S> change);
    
    /**
     * Sets an <code>object instance callback(Service, Component, ServiceReference)</code> callback. The callback is invoked when a service is removed.
     * The method reference must point to method from an Object instance. 
     * 
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B remove(InstanceCbServiceComponentRef<S> remove);
  
    /**
     * Sets a swap <code>component instance callback(Service, Service)</code> method reference. The callback is invoked when a service is swapped.
     * The method reference must point to a Component implementation class method.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param swap the method reference invoked when the service is swapped.
     * @return this builder
     */
    <T> B swap(CbServiceService<T, S> swap);
 
    /**
     * Sets a wap <code>component instance callback(Service, Service, Component)</code> method reference. The callback is invoked when a service is swapped.
     * The method reference must point to a Component implementation class method.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param swap the method reference invoked when the service is swapped.
     * @return this builder
     */
    <T> B swap(CbServiceServiceComponent<T, S> swap);
    
    /**
     * Sets a swap <code>component instance callback(ServiceReference, Service, ServiceReference, Service)</code> method reference. The callback is invoked when a service is swapped.
     * The method reference must point to a Component implementation class method. 
     * the new service.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param swap the method reference invoked when the service is swapped.
     * @return this builder
     */
    <T> B swap(CbRefServiceRefService<T, S> swap);
    
    /**
     * Sets a swap <code>component instance callback(ServiceReference, Service, ServiceReference, Service, Component</code> method reference. The callback is invoked when a service is swapped.
     * The method reference must point to a Component implementation class method. 
     * the new service.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
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
