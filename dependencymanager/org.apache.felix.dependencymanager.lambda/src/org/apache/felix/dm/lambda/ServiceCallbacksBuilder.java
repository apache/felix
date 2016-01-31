package org.apache.felix.dm.lambda;

import org.apache.felix.dm.lambda.callbacks.CbComponent;
import org.apache.felix.dm.lambda.callbacks.CbComponentRef;
import org.apache.felix.dm.lambda.callbacks.CbComponentRefService;
import org.apache.felix.dm.lambda.callbacks.CbComponentRefServiceRefService;
import org.apache.felix.dm.lambda.callbacks.CbComponentService;
import org.apache.felix.dm.lambda.callbacks.CbComponentServiceService;
import org.apache.felix.dm.lambda.callbacks.CbRef;
import org.apache.felix.dm.lambda.callbacks.CbRefService;
import org.apache.felix.dm.lambda.callbacks.CbRefServiceRefService;
import org.apache.felix.dm.lambda.callbacks.CbService;
import org.apache.felix.dm.lambda.callbacks.CbServiceDict;
import org.apache.felix.dm.lambda.callbacks.CbServiceMap;
import org.apache.felix.dm.lambda.callbacks.CbServiceService;
import org.apache.felix.dm.lambda.callbacks.CbTypeComponent;
import org.apache.felix.dm.lambda.callbacks.CbTypeComponentRef;
import org.apache.felix.dm.lambda.callbacks.CbTypeComponentRefService;
import org.apache.felix.dm.lambda.callbacks.CbTypeComponentRefServiceRefService;
import org.apache.felix.dm.lambda.callbacks.CbTypeComponentService;
import org.apache.felix.dm.lambda.callbacks.CbTypeComponentServiceService;
import org.apache.felix.dm.lambda.callbacks.CbTypeRef;
import org.apache.felix.dm.lambda.callbacks.CbTypeRefService;
import org.apache.felix.dm.lambda.callbacks.CbTypeRefServiceRefService;
import org.apache.felix.dm.lambda.callbacks.CbTypeService;
import org.apache.felix.dm.lambda.callbacks.CbTypeServiceDict;
import org.apache.felix.dm.lambda.callbacks.CbTypeServiceMap;
import org.apache.felix.dm.lambda.callbacks.CbTypeServiceService;

/**
 * Builds a service dependency callback (required by default).
 * 
 * A Service may be injected in a bind-method of a component or an object instance using this builder.
 * The builder supports the following kind of method signatures for bind methods:
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
 * The following families of callbacks are supported:
 * 
 * <ul>
 * <li> "cb(String ... callback)": stands for "callback" and specifies a list of callbacks from the component instances. When using one arg, it stands for the "add" callback. 
 * When using two args, it stands for "add/remove" callbacks. When using three args, it stands for "add/change/remove" callbacks. When using four args, it stands for "add/change/remove/swap" callbacks. 
 * <li> "cbi(Object callbackInstance, String ... callbacks)": stands for "callback instance" and specifies some callbacks on a given object instance.
 * <li> "cb(lambda) ": stands for "callback" and specifies a method reference of a callback from a given component class.
 * <li> "cbi(lambda)": stands for "callback instance" and specifies a method reference from a given object instance.
 * <li> "sw(lambda)":  stands for "swap callback" and specifies a method reference of a swap callback from a given component class.
 * <li> "swi(lambda)": stands for "swap callback instance" and specifies a method reference of a swap callback from a given object instance.
 * </ul>
 *
 * <p> Here is an example of a Component that defines a dependency of a LogService which is injected in the "bindLogService" method using a ServiceCallbacksBuilder:
 * The withSrv(...)" declaration defines a method reference on the "ComponentImpl::bindLogService" method (using a lambda):
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *    public void activate() throws Exception { 
 *       component(comp -> comp.impl(ComponentImpl.class).withSrv(LogService.class, log -> log.cb(ComponentImpl::bindLogService)));
 *    }
 * }}</pre>
 *
 * <p> Same example, but we inject the dependency in an object instance that we already have in hand:
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *    public void activate() throws Exception {
 *       ComponentImpl impl = new ComponentImpl();
 *       component(comp -> comp.impl(impl).withSrv(LogService.class, log -> log.cbi(impl::bindLogService)));
 *    }
 * }}</pre>
 * 
 * <p> Here, we inject a service using method reflection (as it is the case in original DM api):
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *    public void activate() throws Exception {
 *       component(comp -> comp.impl(ComponentImpl::class).withSrv(LogService.class, log -> log.cb("bindLogService")));
 *    }
 * }}</pre>
 *
 * <p> Same example, but we inject the dependency in an object instance that we already have in hand:
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *    public void activate() throws Exception {
 *       ComponentImpl impl = new ComponentImpl();
 *       component(comp -> comp.impl(impl).withSrv(LogService.class, log -> log.cbi(impl, "bindLogService")));
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
     * Sets <code>callback</code> methods to invoke on the component instance(s). When a service matches the service 
     * filter, then the service is injected using the specified callback methods. When you specify one callback, it stands for the "add" callback.
     * When you specify two callbacks, the first one corresponds to the "add" callback, and the second one to the "remove" callback. When you specify three
     * callbacks, the first one stands for the "add" callback, the second one for the "change" callback, and the third one for the "remove" callback.
     * When you specify four callbacks, it stands for "add"/"change"/"remove"/swap callbacks.
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
     * swapMethod(S oldService, S newService)
     * swapMethod(ServiceReference<S> oldRef, S old, ServiceReference<S> newRef, S newService)
     * swapMethod(Component component, S oldService, S newService)
     * swapMethod(Component component, ServiceReference<S> oldRef, S old, ServiceReference<S> newRef, S newService)
     * }</pre>
     * 
     * @param callbacks a list of callbacks (1 param: "add", 2 params: "add"/remove", 3 params: "add"/"change"/"remove", 4 params: "add"/"change"/"remove"/"swap" callbacks).
     * @return this builder
     */
    B cb(String ... callbacks);
    
    /**
     * Sets <code>callback instance</code> methods to invoke on a given Object instance. When a service matches the service 
     * filter, then the service is injected using the specified callback methods. When you specify one callback, it stands for the "add" callback.
     * When you specify two callbacks, the first one corresponds to the "add" callback, and the second one to the "remove" callback. When you specify three
     * callbacks, the first one stands for the "add" callback, the second one for the "change" callback, and the third one for the "remove" callback.
     * 
     * @param callbackInstance the object on which the callback is invoked.
     * @param callbacks a list of callbacks (1 param : "add", 2 params : "add"/remove", 3 params : "add"/"change"/"remove", 4 params : "add"/"change"/"remove"/"swap" callbacks).
     * @see #cb(String...)
     * @return this builder
     */
    B cbi(Object callbackInstance, String ... callbacks);

    /**
     * Sets a <code>callback</code> invoked when a service is added.
     * The method reference must point to a Component implementation class method. Callback argument(s): the service.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    <T> B cb(CbTypeService<T, S> add);
    
    /**
     * Sets a <code>callback</code> invoked when a service is added or removed.
     * The method reference must point to a Component implementation class method. Callback argument(s): the service.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B cb(CbTypeService<T, S> add, CbTypeService<T, S> remove);
  
    /**
     * Sets a <code>callback</code> invoked when a service is added, changed, or removed.
     * The method reference must point to a Component implementation class method. Callback argument(s): the service.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @param change the method reference invoked when a service is changed.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B cb(CbTypeService<T, S> add, CbTypeService<T, S> change, CbTypeService<T, S> remove);
    
    /**
     * Sets a <code>callback</code> invoked when a service is added.
     * The method reference must point to a Component implementation class method. Callback argument(s): the service, and a properties map.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    <T> B cb(CbTypeServiceMap<T, S> add);
    
    /**
     * Sets a <code>callback</code> invoked when a service is added or removed.
     * The method reference must point to a Component implementation class method. Callback argument(s): the service, and a properties map.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B cb(CbTypeServiceMap<T, S> add, CbTypeServiceMap<T, S> remove);
    
    /**
     * Sets a <code>callback</code> invoked when a service is added, changed, or removed.
     * The method reference must point to a Component implementation class method. Callback argument(s): the service, and a properties map.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @param change the method reference invoked when a service is changed.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B cb(CbTypeServiceMap<T, S> add, CbTypeServiceMap<T, S> change, CbTypeServiceMap<T, S> remove);
  
    /**
     * Sets a <code>callback</code> invoked when a service is added.
     * The method reference must point to a Component implementation class method. Callback argument(s): the service, and a properties dictionary.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    <T> B cb(CbTypeServiceDict<T, S> add);
    
    /**
     * Sets a <code>callback</code> invoked when a service is added or removed.
     * The method reference must point to a Component implementation class method. Callback argument(s): the service, and a properties dictionary.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B cb(CbTypeServiceDict<T, S> add, CbTypeServiceDict<T, S> remove);
    
    /**
     * Sets a <code>callback</code> invoked when a service is added, changed, or removed.
     * The method reference must point to a Component implementation class method. Callback argument(s): the service, and a properties dictionary.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @param change the method reference invoked when a service is changed.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B cb(CbTypeServiceDict<T, S> add, CbTypeServiceDict<T, S> change, CbTypeServiceDict<T, S> remove);

    /**
     * Sets a <code>callback</code> invoked when a service is added.
     * The method reference must point to a Component implementation class method. Callback argument(s): the service reference, and the service.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    <T> B cb(CbTypeRefService<T, S> add);
    
    /**
     * Sets a <code>callback</code> invoked when a service is added or removed.
     * The method reference must point to a Component implementation class method. Callback argument(s): the service reference, and the service.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B cb(CbTypeRefService<T, S> add, CbTypeRefService<T, S> remove);
 
    /**
     * Sets a <code>callback</code> invoked when a service is added, changed, or removed.
     * The method reference must point to a Component implementation class method. Callback argument(s): the service reference, and the service.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @param change the method reference invoked when a service is changed.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B cb(CbTypeRefService<T, S> add, CbTypeRefService<T, S> change, CbTypeRefService<T, S> remove);

    /**
     * Sets a <code>callback</code> invoked when a service is added.
     * The method reference must point to a Component implementation class method. Callback argument(s): the service reference.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    <T> B cb(CbTypeRef<T, S> add);
  
    /**
     * Sets a <code>callback</code> invoked when a service is added or removed.
     * The method reference must point to a Component implementation class method. Callback argument(s): the service reference.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B cb(CbTypeRef<T, S> add, CbTypeRef<T, S> remove);
  
    /**
     * Sets a <code>callback</code> invoked when a service is added, changed, or removed.
     * The method reference must point to a Component implementation class method. Callback argument(s): the service reference.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @param change the method reference invoked when a service is changed.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B cb(CbTypeRef<T, S> add, CbTypeRef<T, S> change, CbTypeRef<T, S> remove);

    /**
     * Sets a <code>callback</code> invoked when a service is added.
     * The method reference must point to a Component implementation class method. Callback argument(s): the Component.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    <T> B cb(CbTypeComponent<T> add);
  
    /**
     * Sets a <code>callback</code> invoked when a service is added or removed.
     * The method reference must point to a Component implementation class method. Callback argument(s): the Component.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B cb(CbTypeComponent<T> add, CbTypeComponent<T> remove);
  
    /**
     * Sets a <code>callback</code> invoked when a service is added, changed, or removed.
     * The method reference must point to a Component implementation class method. Callback argument(s): the Component.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @param change the method reference invoked when a service is changed.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B cb(CbTypeComponent<T> add, CbTypeComponent<T> change, CbTypeComponent<T> remove);

    /**
     * Sets a <code>callback</code> invoked when a service is added.
     * The method reference must point to a Component implementation class method. Callback argument(s): the Component, and the service reference.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    <T> B cb(CbTypeComponentRef<T, S> add);
  
    /**
     * Sets a <code>callback</code> invoked when a service is added or removed.
     * The method reference must point to a Component implementation class method. Callback argument(s): the Component, and the service reference.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B cb(CbTypeComponentRef<T, S> add, CbTypeComponentRef<T, S> remove);
  
    /**
     * Sets a <code>callback</code> invoked when a service is added, changed, or removed.
     * The method reference must point to a Component implementation class method. Callback argument(s): the Component, and the service reference.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @param change the method reference invoked when a service is changed.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B cb(CbTypeComponentRef<T, S> add, CbTypeComponentRef<T, S> change, CbTypeComponentRef<T, S> remove);

    /**
     * Sets a <code>callback</code> invoked when a service is added.
     * The method reference must point to a Component implementation class method. Callback argument(s): the Component, and the service.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    <T> B cb(CbTypeComponentService<T, S> add);

    /**
     * Sets a <code>callback</code> invoked when a service is added or removed.
     * The method reference must point to a Component implementation class method. Callback argument(s): the Component, and the service.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B cb(CbTypeComponentService<T, S> add, CbTypeComponentService<T, S> remove);
    
    /**
     * Sets a <code>callback</code> invoked when a service is added, changed, or removed.
     * The method reference must point to a Component implementation class method. Callback argument(s): the Component, and the service.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @param change the method reference invoked when a service is changed.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B cb(CbTypeComponentService<T, S> add, CbTypeComponentService<T, S> change, CbTypeComponentService<T, S> remove);

    /**
     * Sets a <code>callback</code> invoked when a service is added.
     * The method reference must point to a Component implementation class method. Callback argument(s): the Component, the service Reference and the service.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    <T> B cb(CbTypeComponentRefService<T, S> add);
 
    /**
     * Sets a <code>callback</code> invoked when a service is added or removed.
     * The method reference must point to a Component implementation class method. Callback argument(s): the Component, the service Reference and the service.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B cb(CbTypeComponentRefService<T, S> add, CbTypeComponentRefService<T, S> remove);
    
    /**
     * Sets a <code>callback</code> invoked when a service is added, changed, or removed.
     * The method reference must point to a Component implementation class method. Callback argument(s): the Component, the service Reference and the service.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param add the method reference invoked when a service is added.
     * @param change the method reference invoked when a service is changed.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    <T> B cb(CbTypeComponentRefService<T, S> add, CbTypeComponentRefService<T, S> change, CbTypeComponentRefService<T, S> remove);

    /**
     * Sets a <code>swap callback(Service, Service)</code> invoked when a service is swapped.
     * The method reference must point to a Component implementation class method. Callback argument(s): the old service and the new replacing service.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param swap the method reference invoked when the service is swapped.
     * @return this builder
     */
    <T> B sw(CbTypeServiceService<T, S> swap);
 
    /**
     * Sets a <code>swap callback(Component, Service, Service)</code> invoked when a service is swapped.
     * The method reference must point to a Component implementation class method. Callback argument(s): the component, the old service and the new replacing service.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param swap the method reference invoked when the service is swapped.
     * @return this builder
     */
    <T> B sw(CbTypeComponentServiceService<T, S> swap);
    
    /**
     * Sets a <code>swap callback(ServiceReference, Service, ServiceReference, Service)</code> invoked when a service is swapped.
     * The method reference must point to a Component implementation class method. Callback argument(s): the old service reference, the old service, the new service reference, and
     * the new service.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param swap the method reference invoked when the service is swapped.
     * @return this builder
     */
    <T> B sw(CbTypeRefServiceRefService<T, S> swap);
    
    /**
     * Sets a swap <code>callback</code> invoked when a service is swapped.
     * The method reference must point to a Component implementation class method. Callback argument(s): the component, the old service reference, the old service, the new service reference, and
     * the new service.
     * 
     * @param <T> the type of the component instance class on which the callback is invoked.
     * @param swap the method reference invoked when the service is swapped.
     * @return this builder
     */
    <T> B sw(CbTypeComponentRefServiceRefService<T, S> swap);

    /**
     * Sets a <code>callback instance</code> invoked when a service is added.
     * The method reference must point to a method from an Object instance. Callback argument(s): a service.
     * 
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    B cbi(CbService<S> add);
    
    /**
     * Sets a <code>callback instance</code> invoked when a service is added/removed.
     * The method reference must point to method from an Object instance. Callback argument(s): a service.
     * 
     * @param add the method reference invoked when a service is added.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B cbi(CbService<S> add, CbService<S> remove);
  
    /**
     * Sets a <code>callback instance</code> invoked when a service is added/changed/removed.
     * The method reference must point to method from an Object instance. Callback argument(s): a service.
     * 
     * @param add the method reference invoked when a service is added.
     * @param change the method reference invoked when a service is changed.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B cbi(CbService<S> add, CbService<S> change, CbService<S> remove);

    /**
     * Sets a <code>callback instance</code> invoked when a service is added.
     * The method reference must point to a method from an Object instance. Callback argument(s): a service and a properties Map.
     * 
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    B cbi(CbServiceMap<S> add);
  
    /**
     * Sets a <code>callback instance</code> invoked when a service is added/removed.
     * The method reference must point to method from an Object instance. Callback argument(s): a service and a properties Map.
     * 
     * @param add the method reference invoked when a service is added.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B cbi(CbServiceMap<S> add, CbServiceMap<S> remove);
  
    /**
     * Sets a <code>callback instance</code> invoked when a service is added/changed/removed.
     * The method reference must point to method from an Object instance. Callback argument(s): a service and a properties Map.
     * 
     * @param add the method reference invoked when a service is added.
     * @param change the method reference invoked when a service is changed.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B cbi(CbServiceMap<S> add, CbServiceMap<S> change, CbServiceMap<S> remove);

    /**
     * Sets a <code>callback instance</code> invoked when a service is added.
     * The method reference must point to a method from an Object instance. Callback argument(s): a service and a properties Dictionary.
     * 
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    B cbi(CbServiceDict<S> add);
   
    /**
     * Sets a <code>callback instance</code> invoked when a service is added/removed.
     * The method reference must point to method from an Object instance. Callback argument(s): a service and a properties Dictionary.
     * 
     * @param add the method reference invoked when a service is added.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B cbi(CbServiceDict<S> add, CbServiceDict<S> remove);
 
    /**
     * Sets a <code>callback instance</code> invoked when a service is added/changed/removed.
     * The method reference must point to method from an Object instance. Callback argument(s): a service and a properties Dictionary.
     * 
     * @param add the method reference invoked when a service is added.
     * @param change the method reference invoked when a service is changed.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B cbi(CbServiceDict<S> add, CbServiceDict<S> change, CbServiceDict<S> remove);

    /**
     * Sets a <code>callback instance</code> invoked when a service is added.
     * The method reference must point to a method from an Object instance. Callback argument(s): a service reference and a service.
     * 
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    B cbi(CbRefService<S> add);
    
    /**
     * Sets a <code>callback instance</code> invoked when a service is added/removed.
     * The method reference must point to method from an Object instance. Callback argument(s): a service reference and a service.
     * 
     * @param add the method reference invoked when a service is added.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B cbi(CbRefService<S> add, CbRefService<S> remove);
  
    /**
     * Sets a <code>callback instance</code> invoked when a service is added/changed/removed.
     * The method reference must point to method from an Object instance. Callback argument(s): a service reference and a service.
     * 
     * @param add the method reference invoked when a service is added.
     * @param change the method reference invoked when a service is changed.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B cbi(CbRefService<S> add, CbRefService<S> change, CbRefService<S> remove);

    /**
     * Sets a <code>callback instance</code> invoked when a service is added.
     * The method reference must point to a method from an Object instance. Callback argument(s): a service reference.
     * 
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    B cbi(CbRef<S> add);
  
    /**
     * Sets a <code>callback instance</code> invoked when a service is added/removed.
     * The method reference must point to method from an Object instance. Callback argument(s): a service reference.
     * 
     * @param add the method reference invoked when a service is added.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B cbi(CbRef<S> add, CbRef<S> remove);
    
    /**
     * Sets a <code>callback instance</code> invoked when a service is added/changed/removed.
     * The method reference must point to method from an Object instance. Callback argument(s): a service reference.
     * 
     * @param add the method reference invoked when a service is added.
     * @param change the method reference invoked when a service is changed.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B cbi(CbRef<S> add, CbRef<S> change, CbRef<S> remove);

    /**
     * Sets a <code>callback instance</code> invoked when a service is added.
     * The method reference must point to a method from an Object instance. Callback argument(s): a Component.
     * 
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    B cbi(CbComponent add);
    
    /**
     * Sets a <code>callback instance</code> invoked when a service is added/removed.
     * The method reference must point to method from an Object instance. Callback argument(s): a Component.
     * 
     * @param add the method reference invoked when a service is added.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B cbi(CbComponent add, CbComponent remove);
 
    /**
     * Sets a <code>callback instance</code> invoked when a service is added/changed/removed.
     * The method reference must point to method from an Object instance. Callback argument(s): a Component.
     * 
     * @param add the method reference invoked when a service is added.
     * @param change the method reference invoked when a service is changed.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B cbi(CbComponent add, CbComponent change, CbComponent remove);

    /**
     * Sets a <code>callback instance</code> invoked when a service is added.
     * The method reference must point to a method from an Object instance. Callback argument(s): a Component and a service reference.
     * 
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    B cbi(CbComponentRef<S> add);
  
    /**
     * Sets a <code>callback instance</code> invoked when a service is added/removed.
     * The method reference must point to method from an Object instance. Callback argument(s): a Component and a service reference.
     * 
     * @param add the method reference invoked when a service is added.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B cbi(CbComponentRef<S> add, CbComponentRef<S> remove);
 
    /**
     * Sets a <code>callback instance</code> invoked when a service is added/changed/removed.
     * The method reference must point to method from an Object instance. Callback argument(s): a Component and a service reference.
     * 
     * @param add the method reference invoked when a service is added.
     * @param change the method reference invoked when a service is changed.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B cbi(CbComponentRef<S> add, CbComponentRef<S> change, CbComponentRef<S> remove);

    /**
     * Sets a <code>callback instance</code> invoked when a service is added.
     * The method reference must point to a method from an Object instance. Callback argument(s): a Component and a service.
     * 
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    B cbi(CbComponentService<S> add);    
  
    /**
     * Sets a <code>callback instance</code> invoked when a service is added/removed.
     * The method reference must point to method from an Object instance. Callback argument(s): a Component and a service.
     * 
     * @param add the method reference invoked when a service is added.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B cbi(CbComponentService<S> add, CbComponentService<S> remove);
   
    /**
     * Sets a <code>callback instance</code> invoked when a service is added/changed/removed.
     * The method reference must point to method from an Object instance. Callback argument(s): a Component and a service.
     * 
     * @param add the method reference invoked when a service is added.
     * @param change the method reference invoked when a service is changed.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B cbi(CbComponentService<S> add, CbComponentService<S> change, CbComponentService<S> remove);

    /**
     * Sets a <code>callback instance</code> invoked when a service is added.
     * The method reference must point to a method from an Object instance. Callback argument(s): a Component, a service reference, and a service.
     * 
     * @param add the method reference invoked when a service is added.
     * @return this builder
     */
    B cbi(CbComponentRefService<S> add);
   
    /**
     * Sets a <code>callback instance</code> invoked when a service is added/removed.
     * The method reference must point to method from an Object instance. Callback argument(s): a Component, a service reference, and a service.
     * 
     * @param add the method reference invoked when a service is added.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B cbi(CbComponentRefService<S> add, CbComponentRefService<S> remove);
  
    /**
     * Sets a <code>callback instance</code> invoked when a service is added/changed/removed.
     * The method reference must point to method from an Object instance. Callback argument(s): a Component, a service reference, and a service.
     * 
     * @param add the method reference invoked when a service is added.
     * @param change the method reference invoked when a service is changed.
     * @param remove the method reference invoked when a service is removed.
     * @return this builder
     */
    B cbi(CbComponentRefService<S> add, CbComponentRefService<S> change, CbComponentRefService<S> remove);
    
    /**
     * Sets a swap <code>callback instance</code> invoked when a service is swapped.
     * The method reference must point to a method from an Object instance. Callback argument(s): the old service, and the new service.
     * the new service.
     *
     * @param swap the method reference invoked when the service is swapped.
     * @return this builder
     */
    B swi(CbServiceService<S> swap);
   
    /**
     * Sets a swap <code>callback instance</code> invoked when a service is swapped.
     * The method reference must point to a method from an Object instance. Callback argument(s): the component, the old service, and the new service.
     * the new service.
     *
     * @param swap the method reference invoked when the service is swapped.
     * @return this builder
     */
    B swi(CbComponentServiceService<S> swap);
  
    /**
     * Sets a swap <code>callback instance</code> invoked when a service is swapped.
     * The method reference must point to a method from an Object instance. Callback argument(s): the old service reference, the old service, the 
     * new service reference, and the new service.
     *
     * @param swap the method reference invoked when the service is swapped.
     * @return this builder
     */
    B swi(CbRefServiceRefService<S> swap);
  
    /**
     * Sets a swap <code>callback instance</code> invoked when a service is swapped.
     * The method reference must point to a method from an Object instance. Callback argument(s): the component, old service reference, the old service, the 
     * new service reference, and the new service.
     *
     * @param swap the method reference invoked when the service is swapped.
     * @return this builder
     */
    B swi(CbComponentRefServiceRefService<S> swap);        
}
