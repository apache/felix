package org.apache.felix.dm.lambda;

import java.util.Dictionary;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.lambda.callbacks.CbComponent;
import org.apache.felix.dm.lambda.callbacks.CbConsumer;
import org.apache.felix.dm.lambda.callbacks.CbTypeComponent;

/**
 * Builds a Dependency Manager Component. Components are the main building blocks for OSGi applications. 
 * They can publish themselves as a service, and they can have dependencies. 
 * These dependencies will influence their life cycle as component will only be activated when all 
 * required dependencies are available.
 * 
 * <p> This interface is also the base interface for extended components like aspects, adapters, etc ...
 *
 * <p> Example of a component that depends on a ConfigurationAdmin service. The dependency is injected by reflection
 * on a class field which type matches the ConfigurationAdmin interface:
 * 
 * <pre>{@code
 * public class Activator extends DependencyManagerActivator {
 *   public void activate() throws Exception {
 *       component(comp -> comp.impl(Configurator.class).withSrv(ConfigurationAdmin.class));
 *   }
 * }
 * } </pre>
 *
 * @param <B> the type of a builder that may extends this builder interface (aspect/adapter).
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ComponentBuilder<B extends ComponentBuilder<B>> {
    /**
     * Configures the component implementation. Can be a class name, or a component implementation object.
     * 
     * @param impl the component implementation (a class, or an Object).
     * @return this builder
     */
    B impl(Object impl);   
    
    /**
     * Sets the factory to use to create the implementation. You can specify both the factory class and method to invoke. The method should return the implementation, 
     * and can use any method to create it. Actually, this can be used together with setComposition to create a composition of instances that work together to implement 
     * a component. The factory itself can also be instantiated lazily by not specifying an instance, but a Class. 
     * 
     * @param factory the factory instance, or the factory class.
     * @param createMethod the create method called on the factory in order to instantiate the component instance.
     * @return this builder
     */
    B factory(Object factory, String createMethod);
        
    /**
     * Configures a factory that can be used to create this component implementation.
     * Example: 
     * 
     * <pre> {@code
     * factory(ComponentImpl::new)", or "factory(() -> new ComponentImpl())
     * }</pre>
     * 
     * @param create the factory used to create the component implementation.
     * @return this builder
     */
    B factory(Supplier<?> create);
    
    /**
     * Configures a factory used to create this component implementation using a Factory object and a method in the Factory object.
     * Example:
     * 
     * <pre> {@code
     * factory(Factory::new, Factory::create)
     * }</pre>
     * 
     * @param <U> the type of the factory returned by the supplier
     * @param <V> the type of the object that is returned by the factory create method.
     * @param factory the function used to create the Factory itself
     * @param create the method reference on the Factory method that is used to create the Component implementation
     * @return this builder
     */
    <U, V> B factory(Supplier<U> factory, Function<U, V> create);
        
    /**
     * Configures a factory used to create this component implementation using a Factory object and a "getComponent" factory method.
     * the Factory method may then return multiple objects that will be part of this component implementation.
     * 
     * Example:
     * 
     * <pre> {@code
     * CompositionManager mngr = new CompositionManager();
     * ...
     * factory(mngr::create, mngr::getComposition)
     * }</pre>
     * 
     * @param factory the supplier used to return the main component implementation instance
     * @param getComposition the supplier that returns the list of instances that are part of the component implementation classes.
     * @return this builder
     */
    B factory(Supplier<?> factory, Supplier<Object[]> getComposition);

    /**
     * Configures a factory that also returns a composition of objects for this component implemenation.
     * 
     * Example:
     * 
     * <pre> {@code
     * factory(CompositionManager::new, CompositionManager::create, CompositionManager::getComposition).
     * }</pre>
     * 
     * Here, the CompositionManager will act as a factory (the create method will return the component implementation object), and the
     * CompositionManager.getComposition() method will return all the objects that are also part of the component implementation.
     * 
     * @param <U> the type of the object returned by the supplier factory
     * @param factory the function used to create the Factory itself
     * @param create the Factory method used to create the main component implementation object
     * @param getComposition the Factory method used to return the list of objects that are also part of the component implementation.
     * @return this builder
     */
    <U> B factory(Supplier<U> factory, Function<U, ?> create, Function<U, Object[]> getComposition);

    /**
     * Sets the public interfaces under which this component should be registered in the OSGi service registry.
     * 
     * @param iface the public interfaces to register in the OSGI service registry.
     * @return this builder
     */
	B provides(Class<?>  iface);
	
	/**
     * Sets the public interface under which this component should be registered in the OSGi service registry.
     * 
     * @param iface the public interfaces to register in the OSGI service registry.
	 * @param name a property name for the provided service
	 * @param value a property value for the provided service
	 * @param rest the rest of property name/value pairs.
	 * @return this builder.
	 */
	B provides(Class<?>  iface, String name, Object value, Object ... rest);
	
	/**
     * Sets the public interface under which this component should be registered in the OSGi service registry.
     * Warning: you can only use this method if you compile your application using the "-parameters" javac option.
     * 
     * code example:
     * 
     * <pre> {@code
     *  provides(MyService.class, property1 -> "value1", property2 -> 123);
     * }</pre>
     *
     * @param iface the public interfaces to register in the OSGI service registry.
	 * @param properties a list of fluent service properties for the provided service. You can specify a list of lambda expression, each one implementing the
	 * {@link FluentProperty} interface that allows to define a property name using a lambda parameter.
     * @return this builder.
	 */
	B provides(Class<?>  iface, FluentProperty ... properties);
	
	/**
     * Sets the public interface under which this component should be registered in the OSGi service registry.
     * @param iface the public interfaces to register in the OSGI service registry.
	 * @param properties the properties for the provided service
     * @return this builder.
	 */
    B provides(Class<?>  iface, Dictionary<?,?> properties);
    
    /**
     * Sets the public interfaces under which this component should be registered in the OSGi service registry.
     * 
     * @param ifaces list of services provided by the component.
     * @return this builder.
     */
    B provides(Class<?>[] ifaces);
    
    /**
     * Sets the public interfaces under which this component should be registered in the OSGi service registry.
     * 
     * @param ifaces the public interfaces to register in the OSGI service registry.
     * @param name a property name for the provided service
     * @param value a property value for the provided service
     * @param rest the rest of property name/value pairs.
     * @return this builder.
     */
    B provides(Class<?>[] ifaces, String name, Object value, Object ... rest);
    
    /**
     * Sets the public interfaces under which this component should be registered in the OSGi service registry.
     * Warning: you can only use this method if you compile your application using the "-parameters" javac option. 
     * code example:
     * 
     * <pre> {@code
     *    provides(new Class[] { MyService.class, MyService2.class }, property1 -> "value1", property2 -> 123);
     * }</pre>
     *
     * @param ifaces the public interfaces to register in the OSGI service registry.
     * @param properties a list of fluent service properties for the provided service. You can specify a list of lambda expression, each one implementing the
     * {@link FluentProperty} interface that allows to define a property name using a lambda parameter.
     * @return this builder.
     */
    B provides(Class<?>[] ifaces, FluentProperty ... properties);
    
    /**
     * Sets the public interfaces under which this component should be registered in the OSGi service registry.
     * @param ifaces the public interfaces to register in the OSGI service registry.
     * @param properties the properties for the provided service
     * @return this builder.
     */
    B provides(Class<?>[] ifaces, Dictionary<?,?> properties);

    /**
     * Sets the public interface under which this component should be registered in the OSGi service registry.
     * 
     * @param iface the service provided by this component.
     * @return this builder.
     */
    B provides(String iface);
    
    /**
     * Sets the public interface under which this component should be registered in the OSGi service registry.
     * 
     * @param iface the public interfaces to register in the OSGI service registry.
     * @param name a property name for the provided service
     * @param value a property value for the provided service
     * @param rest the rest of property name/value pairs.
     * @return this builder.
     */
    B provides(String iface, String name, Object value, Object ... rest);
    
    /**
     * Sets the public interface under which this component should be registered in the OSGi service registry. 
     * Warning: you can only use this method if you compile your application using the "-parameters" javac option.
     * code example:
     * 
     * <pre> {@code 
     * provides(MyService.class, property1 -> "value1", property2 -> 123);
     * }</pre>
     *
     * @param iface the public interfaces to register in the OSGI service registry.
     * @param properties a list of fluent service properties for the provided service. You can specify a list of lambda expression, each one implementing the
     * {@link FluentProperty} interface that allows to define a property name using a lambda parameter.
     * @return this builder.
     */
    B provides(String iface, FluentProperty ... properties);
    
    /**
     * Sets the public interface under which this component should be registered in the OSGi service registry.
     * @param iface the public interfaces to register in the OSGI service registry.
     * @param properties the properties for the provided service
     * @return this builder.
     */
    B provides(String iface, Dictionary<?,?> properties);
    
    /**
     * Sets the public interfaces under which this component should be registered in the OSGi service registry.
     * 
     * @param ifaces the list of services provided by the component.
     * @return this builder.
     */
    B provides(String[] ifaces);
    
    /**
     * Sets the public interfaces under which this component should be registered in the OSGi service registry.
     * 
     * @param ifaces the public interfaces to register in the OSGI service registry.
     * @param name a property name for the provided service
     * @param value a property value for the provided service
     * @param rest the rest of property name/value pairs.
     * @return this builder.
     */
    B provides(String[] ifaces, String name, Object value, Object ... rest);
    
    /**
     * Sets the public interfaces under which this component should be registered in the OSGi service registry.
     * Warning: you can only use this method if you compile your application using the "-parameters" javac option.
     * 
     * code example:
     * <pre> {@code 
     * provides(new Class[] { MyService.class, MyService2.class }, property1 -> "value1", property2 -> 123);
     * }</pre>
     * 
     * @param ifaces the public interfaces to register in the OSGI service registry.
     * @param properties a list of fluent service properties for the provided service. You can specify a list of lambda expression, each one implementing the
     * {@link FluentProperty} interface that allows to define a property name using a lambda parameter.
     * @return this builder.
     */
    B provides(String[] ifaces, FluentProperty ... properties);
    
    /**
     * Sets the public interfaces under which this component should be registered in the OSGi service registry.
     * @param ifaces the public interfaces to register in the OSGI service registry.
     * @param properties the properties for the provided service
     * @return this builder.
     */
    B provides(String[] ifaces, Dictionary<?,?> properties);

    /**
     * Sets the component's service properties
     * @param properties the component's service properties
     * @return this builder
     */
    B properties(Dictionary<?,?> properties);     
    
    /**
     * Sets the components's service properties using varargs. The number of parameters must be even, representing a list of pair property key-value.
     * 
     * <pre> {@code 
     * Example: properties("param1", "value1", "service.ranking", 3)
     * }</pre>
     * 
     * @param name the first property name
     * @param value the first property value
     * @param rest the rest of properties key/value pairs.
     * @return this builder
     */
    B properties(String name, Object value, Object ... rest);  
    
    /**
     * Sets the components's service properties using List of lamda properties. 
     *  
     * Example: 
     * 
     * <pre> {@code
     *   properties(param1 -> "value1, param2 -> 2);
     * }</pre>
     * 
     * When you use this method, you must compile your source code using the "-parameters" option, and the "arg0" parameter
     * name is now allowed.
     * 
     * @param properties the fluent properties
     * @return this builder
     */
    B properties(FluentProperty ... properties);  

    /**
     * Adds a required/autoconfig service dependency.
     * 
     * @param service the service dependency filter
     * @param filter the service filter
     * @return this builder
     */
    B withSrv(Class<?> service, String filter);

    /**
     * Adds in one shot multiple required/autoconfig service dependencies.
     * @param services the dependencies that are required and that will be injected in any field with the same dependency type.
     * @return this builder
     */
    B withSrv(Class<?> ... services);
           
    /**
     * Adds a service dependency built using a Consumer lambda that is provided with a ServiceDependencyBuilder. 
     * 
     * @param <U> the type of the dependency service
     * @param service the service
     * @param consumer the lambda for building the service dependency
     * @return this builder.
     */
    <U> B withSrv(Class<U> service, Consumer<ServiceDependencyBuilder<U>> consumer);
    
    /**
     * Adds a configuration dependency.
     * @param consumer the lambda used to build the configuration dependency.
     * @return this builder.
     */
    B withCnf(Consumer<ConfigurationDependencyBuilder> consumer);     
    
    /**
     * Adds multiple configuration dependencies in one single call. All configurations are injected by default in the "updated" callback.
     * @param pids list of configuration pids.
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    default B withCnf(String ... pids) {
        Stream.of(pids).forEach(pid -> withCnf(cnf -> cnf.pid(pid)));
        return (B) this;
    }
    
    /**
     * Adds multiple configuration dependencies in one single call. 
     * @param pids list of configuration pids
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    default B withCnf(Class<?> ... pids) {
        Stream.of(pids).forEach(pid -> withCnf(cnf -> cnf.pid(pid)));
        return (B) this;
    }

    /**
     * Adds a bundle dependency.
     * @param consumer the lambda used to build the bundle dependency.
     * @return this builder.
     */
    B withBundle(Consumer<BundleDependencyBuilder> consumer);        

    /**
     * Adds a CompletableFuture dependency.
     * 
     * @param <U> the type of the result of the CompletableFuture.
     * @param future a CompletableFuture on which the dependency will wait for
     * @param consumer the builder used to build the dependency
     * @return this builder.
     */
    <U> B withFuture(CompletableFuture<U> future, Consumer<FutureDependencyBuilder<U>> consumer);
    
    /**
     * Sets the name of the method used as the "init" callback. This method, when found, is
     * invoked as part of the life cycle management of the component implementation. 
     * This method is useful because when it is invoked, all required dependencies defines in the Activator
     * are already injected, and you can then add more extra dependencies from the init() method.
     * And once all extra dependencies will be available and injected, then the "start" callback will be invoked.
     * The dependency manager will look for a method of this name with the following signatures,
     * in this order:
     * <ol>
     * <li>method(Component component)</li>
     * <li>method()</li>
     * </ol>
     * 
     * @param callback the callback name
     * @return this builder.
     */
    B init(String callback);
    
    /**
     * Sets the name of the method used as the "start" callback. This method, when found, is
     * invoked as part of the life cycle management of the component implementation. The
     * dependency manager will look for a method of this name with the following signatures,
     * in this order:
     * <ol>
     * <li>method(Component component)</li>
     * <li>method()</li>
     * </ol>
     * 
     * @param callback the callback name
     * @return this builder.
     */
    B start(String callback);
    
    /**
     * Sets the name of the method used as the "stop" callback. This method, when found, is
     * invoked as part of the life cycle management of the component implementation. The
     * dependency manager will look for a method of this name with the following signatures,
     * in this order:
     * <ol>
     * <li>method(Component component)</li>
     * <li>method()</li>
     * </ol>
     * 
     * @param callback the callback name
     * @return this builder.
     */
    B stop(String callback);
    
    /**
     * Sets the name of the method used as the "destroy" callback. This method, when found, is
     * invoked as part of the life cycle management of the component implementation. The
     * dependency manager will look for a method of this name with the following signatures,
     * in this order:
     * <ol>
     * <li>method(Component component)</li>
     * <li>method()</li>
     * </ol>
     * 
     * @param callback the callback name
     * @return this builder.
     */
    B destroy(String callback);
    
    /**
     * Sets the name of the methods used as init callback that is invoked on a given Object instance. 
     * These methods, when found, are invoked on the specified instance as part of the life cycle management 
     * of the component implementation.
     * <p>
     * Specifying an instance means you can create a manager
     * that will be invoked whenever the life cycle of a component changes and this manager
     * can then decide how to expose this life cycle to the actual component, offering an
     * important indirection when developing your own component models.
     * 
     * @see #init(String)
     * @param callbackInstance the instance the callback will be invoked on.
     * @param callback the callback name
     * @return this builder.
     */
    B init(Object callbackInstance, String callback);
    
    /**
     * Sets the name of the methods used as start callback that is invoked on a given Object instance. 
     * These methods, when found, are invoked on the specified instance as part of the life cycle management 
     * of the component implementation.
     * <p>
     * Specifying an instance means you can create a manager
     * that will be invoked whenever the life cycle of a component changes and this manager
     * can then decide how to expose this life cycle to the actual component, offering an
     * important indirection when developing your own component models.
     * 
     * @see #start(String)
     * @param callbackInstance the instance the callback will be invoked on.
     * @param callback the name of the start method
     * @return this builder.
     */
    B start(Object callbackInstance, String callback);
   
    /**
     * Sets the name of the methods used as stop callback that is invoked on a given Object instance. 
     * These methods, when found, are invoked on the specified instance as part of the life cycle management 
     * of the component implementation.
     * <p>
     * Specifying an instance means you can create a manager
     * that will be invoked whenever the life cycle of a component changes and this manager
     * can then decide how to expose this life cycle to the actual component, offering an
     * important indirection when developing your own component models.
     * 
     * @see #stop(String)
     * @param callbackInstance the instance the callback will be invoked on.
     * @param callback the name of the stop method
     * @return this builder.
     */
    B stop(Object callbackInstance, String callback);
  
    /**
     * Sets the name of the methods used as destroy callback that is invoked on a given Object instance. 
     * These methods, when found, are invoked on the specified instance as part of the life cycle management 
     * of the component implementation.
     * <p>
     * Specifying an instance means you can create a manager
     * that will be invoked whenever the life cycle of a component changes and this manager
     * can then decide how to expose this life cycle to the actual component, offering an
     * important indirection when developing your own component models.
     * 
     * @see #destroy(String)
     * @param callbackInstance the instance the callback will be invoked on.
     * @param callback the name of the destroy method
     * @return this builder.
     */
    B destroy(Object callbackInstance, String callback);

    /**
     * Sets a method reference used as the "init" callback. This method reference must point to method from one 
     * of the component instance classes. It is invoked as part of the life cycle management of the component implementation. 
     * This method is useful because when it is invoked, all required dependencies defines in the Activator
     * are already injected, and you can then add more extra dependencies from the init() method.
     * And once all extra dependencies will be available and injected, then the "start" callback will be invoked.
     * The method does not take any parameters.
     * 
     * @param <U> the type of the component class on which the callback is invoked on.
     * @param callback a method reference must point to method from one of the component instance classes.
     * @return this builder
     */
    <U> B init(CbConsumer<U> callback);
  
    /**
     * Sets a method reference used as the "start" callback. This method reference must point to method from one 
     * of the component instance classes. This method is invoked as part of the life cycle management of the component implementation. 
     * 
     * @param <U> the type of the component class on which the callback is invoked on.
     * @param callback  a method reference must point to method from one of the component instance classes.
     * @return this builder.
     */
    <U> B start(CbConsumer<U> callback);
   
    /**
     * Sets a method reference used as the "stop" callback. This method reference must point to method from one 
     * of the component instance classes. This method is invoked as part of the life cycle management of the component implementation. 
     * 
     * @param <U> the type of the component class on which the callback is invoked on.
     * @param callback  a method reference must point to method from one of the component instance classes.
     * @return this builder.
     */
    <U> B stop(CbConsumer<U> callback);
  
    /**
     * Sets a method reference used as the "destroy" callback. This method reference must point to method from one 
     * of the component instance classes. This method is invoked as part of the life cycle management of the component implementation. 
     * 
     * @param <U> the type of the component class on which the callback is invoked on.
     * @param callback  a method reference must point to method from one of the component instance classes.
     * @return this builder.
     */
    <U> B destroy(CbConsumer<U> callback);

    /**
     * Sets a method reference used as the "init" callback. This method reference must point to method from one 
     * of the component instance classes. It is invoked as part of the life cycle management of the component implementation. 
     * This method is useful because when it is invoked, all required dependencies defines in the Activator
     * are already injected, and you can then add more extra dependencies from the init() method.
     * And once all extra dependencies will be available and injected, then the "start" callback will be invoked.
     * The method takes as argument a Component parameter.
     * 
     * @param <U> the type of the component class on which the callback is invoked on.
     * @param callback a method reference must point to method from one of the component instance classes. The method takes as argument a Component parameter.
     * @return this builder
     */
    <U> B init(CbTypeComponent<U> callback);
    
    /**
     * Sets a method reference used as the "start" callback. This method reference must point to method from one 
     * of the component instance classes. This method is invoked as part of the life cycle management of the component implementation. 
     * The method takes as argument a Component parameter.
     * 
     * @param <U> the type of the component class on which the callback is invoked on.
     * @param callback a method reference must point to method from one of the component instance classes. The method takes as argument a Component parameter.
     * @return this builder.
     */
    <U> B start(CbTypeComponent<U> callback);
  
    /**
     * Sets a method reference used as the "stop" callback. This method reference must point to method from one 
     * of the component instance classes. This method is invoked as part of the life cycle management of the component implementation. 
     * The method takes as argument a Component parameter.
     * 
     * @param <U> the type of the component class on which the callback is invoked on.
     * @param callback a method reference must point to method from one of the component instance classes. The method takes as argument a Component parameter.
     * @return this builder.
     */
    <U> B stop(CbTypeComponent<U> callback);
  
    /**
     * Sets a method reference used as the "destroy" callback. This method reference must point to method from one 
     * of the component instance classes. This method is invoked as part of the life cycle management of the component implementation. 
     * The method takes as argument a Component parameter.
     * 
     * @param <U> the type of the component class on which the callback is invoked on.
     * @param callback a method reference must point to method from one of the component instance classes. The method takes as argument a Component parameter.
     * @return this builder.
     */
    <U> B destroy(CbTypeComponent<U> callback);

    /**
     * Sets an Object instance method reference used as the "init" callback. It is invoked as part of the life cycle management of the component 
     * implementation. 
     * This method is useful because when it is invoked, all required dependencies defines in the Activator
     * are already injected, and you can then add more extra dependencies from the init() method.
     * And once all extra dependencies will be available and injected, then the "start" callback will be invoked.
     * The method does not take any parameters.
     * 
     * @param callback an Object instance method reference. The method does not take any parameters.
     * @return this builder
     */
    B initInstance(Runnable callback);
 
    /**
     * Sets an Object instance method reference used as the "start" callback. This method reference must point to method from one 
     * of the component instance classes. This method is invoked as part of the life cycle management of the component implementation. 
     * The method does not take any parameters.
     *
     * @param callback an Object instance method reference. The method does not take any parameters.
     * @return this builder.
     */
    B startInstance(Runnable callback);
  
    /**
     * Sets an Object instance method reference used as the "stop" callback. It is invoked as part of the life cycle management of the component 
     * implementation. 
     * This method is useful because when it is invoked, all required dependencies defines in the Activator
     * are already injected, and you can then add more extra dependencies from the init() method.
     * And once all extra dependencies will be available and injected, then the "start" callback will be invoked.
     * The method does not take any parameters.
     * 
     * @param callback an Object instance method reference. The method does not take any parameters.
     * @return this builder
     */
    B stopInstance(Runnable callback);
  
    /**
     * Sets an Object instance method reference used as the "destroy" callback. It is invoked as part of the life cycle management of the component 
     * implementation. 
     * This method is useful because when it is invoked, all required dependencies defines in the Activator
     * are already injected, and you can then add more extra dependencies from the init() method.
     * And once all extra dependencies will be available and injected, then the "start" callback will be invoked.
     * The method does not take any parameters.
     * 
     * @param callback an Object instance method reference. The method does not take any parameters.
     * @return this builder
     */
    B destroyInstance(Runnable callback);

    /**
     * Sets an Object instance method reference used as the "init" callback. It is invoked as part of the life cycle management of the component 
     * implementation. 
     * This method is useful because when it is invoked, all required dependencies defines in the Activator
     * are already injected, and you can then add more extra dependencies from the init() method.
     * And once all extra dependencies will be available and injected, then the "start" callback will be invoked.
     * The method takes as argument a Component parameter.
     * 
     * @param callback an Object instance method reference.
     * @return this builder
     */
    B initInstance(CbComponent callback);
   
    /**
     * Sets an Object instance method reference used as the "start" callback. This method reference must point to method from one 
     * of the component instance classes. This method is invoked as part of the life cycle management of the component implementation. 
     * The method takes as argument a Component parameter.
     *
     * @param callback an Object instance method reference. The method takes as argument a Component parameter.
     * @return this builder.
     */
    B startInstance(CbComponent callback);
    
    /**
     * Sets an Object instance method reference used as the "stop" callback. This method reference must point to method from one 
     * of the component instance classes. This method is invoked as part of the life cycle management of the component implementation. 
     * The method takes as argument a Component parameter.
     *
     * @param callback an Object instance method reference. The method takes as argument a Component parameter.
     * @return this builder.
     */
    B stopInstance(CbComponent callback);
  
    /**
     * Sets an Object instance method reference used as the "destroy" callback. This method reference must point to method from one 
     * of the component instance classes. This method is invoked as part of the life cycle management of the component implementation. 
     * The method takes as argument a Component parameter.
     *
     * @param callback an Object instance method reference. The method takes as argument a Component parameter.
     * @return this builder.
     */
    B destroyInstance(CbComponent callback);

    /**
     * Configures OSGi object (BundleContext, Component, etc ...) that will be injected in any field having the same OSGi object type.
     * @param clazz the OSGi object type (BundleContext, Component, DependencyManager).
     * @param autoConfig true if the OSGi object has to be injected, false if not
     * @return this builder
     */
    B autoConfig(Class<?> clazz, boolean autoConfig); 
    
    /**
     * Configures OSGi object (BundleContext, Component, etc ...) that will be injected in a given field.
     * @param clazz the OSGi object type (BundleContext, Component, DependencyManager).
     * @param field the field that will be injected with the OSGI object
     * @return this builder
     */
    B autoConfig(Class<?> clazz, String field);
    
    /**
     * Activates debug mode
     * @param label the debug label
     * @return this builder
     */
    B debug(String label);
    
    /**
     * Automatically adds this component to its DependencyManager object. When a lambda builds a Component using this builder, by default
     * the built component is auto added to its DependencyManager object, unless you invoke autoAdd(false).
     * 
     * @param autoAdd true for automatically adding this component to the DependencyManager object, false if not
     * @return this builder
     */
    B autoAdd(boolean autoAdd);
    
    /**
     * Sets the method to invoke on the service implementation to get back all
     * instances that are part of a composition and need dependencies injected.
     * All of them will be searched for any of the dependencies. The method that
     * is invoked must return an <code>Object[]</code>.
     * 
     * @param getCompositionMethod the method to invoke
     * @return this builder
     */
    B composition(String getCompositionMethod);
    
    /**
     * Sets the instance and method to invoke to get back all instances that
     * are part of a composition and need dependencies injected. All of them
     * will be searched for any of the dependencies. The method that is
     * invoked must return an <code>Object[]</code>.
     * 
     * @param instance the instance that has the method
     * @param getCompositionMethod the method to invoke
     * @return this builder
     */
    B composition(Object instance, String getCompositionMethod);

    /**
     * Sets a java8 method reference to a Supplier that returns all instances that are part of a composition and need dependencies injected.
     * All of them will be searched for any of the dependencies. The method that
     * is invoked must return an <code>Object[]</code>.
     * 
     * @param getCompositionMethod the method to invoke
     * @return this builder
     */
    B composition(Supplier<Object[]> getCompositionMethod);

    /**
     * Builds the real DependencyManager Component.
     * @return the real DependencyManager Component.
     */
    Component build();
}
