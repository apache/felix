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

import java.util.Dictionary;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.lambda.callbacks.InstanceCb;
import org.apache.felix.dm.lambda.callbacks.InstanceCbComponent;

/**
 * Builds a Dependency Manager Component. <p> Components are the main building blocks for OSGi applications. 
 * They can publish themselves as a service, and they can have dependencies. 
 * These dependencies will influence their life cycle as component will only be activated when all 
 * required dependencies are available. This interface is also the base interface for extended components like 
 * aspects, adapters, etc ...
 *
 * <p> Example of a component that depends on a LogServce service. The dependency is injected by reflection
 * on fields having a compatible type with the LogService interface:
 * 
 * <pre>{@code
 * public class Activator extends DependencyManagerActivator {
 *   public void init(BundleContext ctx, DependencyManager dm) throws Exception {
 *       component(comp -> comp.impl(Pojo.class).withSvc(LogService.class));
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
     * Sets the factory to use when creating the implementation. You can specify both the factory class and method to invoke. The method should return the implementation, 
     * and can use any method to create it. Actually, this can be used together with setComposition to create a composition of instances that work together to implement 
     * a component. The factory itself can also be instantiated lazily by not specifying an instance, but a Class. 
     * 
     * @param factory the factory instance, or the factory class.
     * @param createMethod the create method called on the factory in order to instantiate the component.
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
     * Configures a factory used to create this component implementation using a Factory object and a "getComposition" factory method.
     * the Factory method may then return multiple objects that will be part of this component implementation, and 
     * all of them will be searched when injecting any of the dependencies.
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
     * Here, the CompositionManager will act as a factory (the create method will return the component implementation object), the
     * CompositionManager.getComposition() method will return all the objects that are also part of the component implementation, 
     * and all of them will be searched for injecting any of the dependencies.
     * 
     * @param <U> the type of the object returned by the supplier factory
     * @param factory the function used to create the Factory itself
     * @param create the Factory method used to create the main component implementation object
     * @param getComposition the Factory method used to return the list of objects that are also part of the component implementation.
     * @return this builder
     */
    <U> B factory(Supplier<U> factory, Function<U, ?> create, Function<U, Object[]> getComposition);

    /**
     * Sets the public interface under which this component should be registered in the OSGi service registry.
     * 
     * @param iface the public interface to register in the OSGI service registry.
     * @return this builder
     */
	B provides(Class<?>  iface);
	
	/**
     * Sets the public interface under which this component should be registered in the OSGi service registry.
     * 
     * @param iface the public interface to register in the OSGI service registry.
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
     * @param iface the public interface to register in the OSGI service registry.
	 * @param properties a list of fluent service properties for the provided service. You can specify a list of lambda expression, each one implementing the
	 * {@link FluentProperty} interface that allows to define a property name using a lambda parameter.
     * @return this builder.
	 */
	B provides(Class<?>  iface, FluentProperty ... properties);
	
	/**
     * Sets the public interface under which this component should be registered in the OSGi service registry.
     * @param iface the public interface to register in the OSGI service registry.
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
     * 
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
     * @param iface the public interface to register in the OSGI service registry.
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
     * @param iface the public interface to register in the OSGI service registry.
     * @param properties a list of fluent service properties for the provided service. You can specify a list of lambda expression, each one implementing the
     * {@link FluentProperty} interface that allows to define a property name using a lambda parameter.
     * @return this builder.
     */
    B provides(String iface, FluentProperty ... properties);
    
    /**
     * Sets the public interface under which this component should be registered in the OSGi service registry.
     * @param iface the public interface to register in the OSGI service registry.
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
     * 
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
     * Adds a service dependency built using a Consumer lambda that is provided with a ServiceDependencyBuilder. 
     * 
     * @param <U> the type of the dependency service
     * @param service the service
     * @param consumer the lambda used to build the service dependency
     * @return this builder.
     */
    <U> B withSvc(Class<U> service, Consumer<ServiceDependencyBuilder<U>> consumer);
    
    /**
     * Adds in one shot multiple service dependencies injected in compatible class fields.
     * 
     * @param services some dependencies to inject in compatible class fields.
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    default B withSvc(Class<?> ... services) {
        Stream.of(services).forEach(s -> withSvc(s, svc -> svc.autoConfig()));
        return (B) this;
    }

    /**
     * Adds in one shot multiple service dependencies injected in compatible class fields.
     * 
     * @param required true if the dependency is required, false if not
     * @param services some dependencies to inject in compatible class fields.
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    default B withSvc(boolean required, Class<?> ... services) {
        Stream.of(services).forEach(s -> withSvc(s, svc -> svc.required(required)));
        return (B) this;
    }
    
    /**
     * Adds a service dependency injected in compatible class fields.
     * 
     * @param service a service dependency
     * @param required true if the dependency is required, false if not
     * @return this builder
     */
    @SuppressWarnings("unchecked")
    default B withSvc(Class<?> service, boolean required) {
        withSvc(service, svc -> svc.required(required));
        return (B) this;
    }
      
    /**
     * Adds a service dependency injected in compatible class fields.
     * 
     * @param <T> the service dependency type
     * @param service the service dependency.
     * @param filter the service filter
     * @param required true if the dependency is required, false if not
     * @return this builder
     */
    default <T> B withSvc(Class<T> service, String filter, boolean required) {
        return withSvc(service, svc -> svc.filter(filter).required(required));
    }
    
    /**
     * Adds a service dependency injected in a given compatible class field.
     * 
     * @param <T> the service dependency type
     * @param service the service dependency
     * @param filter the service filter
     * @param field the class field when the dependency has to be injected
     * @param required true if the dependency is required, false if not
     * @return this builder
     */
    default <T> B withSvc(Class<T> service, String filter, String field, boolean required) {
        return withSvc(service, svc -> svc.filter(filter).autoConfig(field).required(required));
    }
    
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
     * Adds a configuration dependency using a configuration type. The configuration is injected in an updated callback which takes in argument
     * an implementation of the specified configuration type.
     * 
     * @param configType the configuration type that will be injected to the "updated" callback
     * @return this builder
     * @see ConfigurationDependencyBuilder
     */
    default B withCnf(Class<?> configType) {
        return withCnf(cnf -> cnf.update(configType, "updated"));
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
     * <p>The dependency manager will look for a method of this name with the following signatures,
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
     * Sets a callback instance and the name of the method used as the "init" callback. This method, when found, is
     * invoked as part of the life cycle management of the component implementation. 
     * This method is useful because when it is invoked, all required dependencies defines in the Activator
     * are already injected, and you can then add more extra dependencies from the init() method.
     * And once all extra dependencies will be available and injected, then the "start" callback will be invoked.
     * <p>The dependency manager will look for a method of this name with the following signatures,
     * in this order:
     * <ol>
     * <li>method(Component component)</li>
     * <li>method()</li>
     * </ol>
     * 
     * @param callbackInstance a callback instance object the callback is invoked on
     * @param callback the callback name
     * @return this builder.
     */
    B init(Object callbackInstance, String callback);

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
    B init(InstanceCb callback);
 
    /**
     * Sets an Object instance method reference used as the "init" callback. It is invoked as part of the life cycle management of the component 
     * implementation. 
     * This method is useful because when it is invoked, all required dependencies defines in the Activator
     * are already injected, and you can then add more extra dependencies from the init() method.
     * And once all extra dependencies will be available and injected, then the "start" callback will be invoked.
     * The method takes as argument a Component parameter.
     * 
     * @param callback an Object instance method reference. The method takes as argument a Component parameter.
     * @return this builder
     */
    B init(InstanceCbComponent callback);
   
    /**
     * Sets a callback instance and the name of the method used as the "start" callback. This method, when found, is
     * invoked as part of the life cycle management of the component implementation. <p>The
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
     * Sets the name of the method used as the "start" callback. This method, when found, is
     * invoked as part of the life cycle management of the component implementation. <p>The
     * dependency manager will look for a method of this name with the following signatures,
     * in this order:
     * <ol>
     * <li>method(Component component)</li>
     * <li>method()</li>
     * </ol>
     * 
     * @param callbackInstance a callback instance object the callback is invoked on
     * @param callback the callback name
     * @return this builder.
     */
    B start(Object callbackInstance, String callback);

    /**
     * Sets an Object instance method reference used as the "start" callback. 
     * This method is invoked as part of the life cycle management of the component implementation. 
     * The method does not take any parameters.
     *
     * @param callback an Object instance method reference. The method does not take any parameters.
     * @return this builder.
     */
    B start(InstanceCb callback);
  
    /**
     * Sets an Object instance method reference used as the "start" callback.
     * This method is invoked as part of the life cycle management of the component implementation. 
     * The method takes as argument a Component parameter.
     *
     * @param callback an Object instance method reference. The method takes as argument a Component parameter.
     * @return this builder.
     */
    B start(InstanceCbComponent callback);
    
    /**
     * Sets the name of the method used as the "stop" callback. This method, when found, is
     * invoked as part of the life cycle management of the component implementation. <p>The
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
     * Sets a callback instance and the name of the method used as the "stop" callback. This method, when found, is
     * invoked as part of the life cycle management of the component implementation. <p>The
     * dependency manager will look for a method of this name with the following signatures,
     * in this order:
     * <ol>
     * <li>method(Component component)</li>
     * <li>method()</li>
     * </ol>
     * 
     * @param callbackInstance a callback instance object the callback is invoked on
     * @param callback the callback name
     * @return this builder.
     */
    B stop(Object callbackInstance, String callback);

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
    B stop(InstanceCb callback);
  
    /**
     * Sets an Object instance method reference used as the "stop" callback. 
     * This method is invoked as part of the life cycle management of the component implementation. 
     * The method takes as argument a Component parameter.
     *
     * @param callback an Object instance method reference. The method takes as argument a Component parameter.
     * @return this builder.
     */
    B stop(InstanceCbComponent callback);
  
    /**
     * Sets the name of the method used as the "destroy" callback. This method, when found, is
     * invoked as part of the life cycle management of the component implementation. <p>The
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
     * Sets a callback instance and the name of the method used as the "destroy" callback. This method, when found, is
     * invoked as part of the life cycle management of the component implementation. <p>The
     * dependency manager will look for a method of this name with the following signatures,
     * in this order:
     * <ol>
     * <li>method(Component component)</li>
     * <li>method()</li>
     * </ol>
     * 
     * @param callbackInstance a callback instance object the callback is invoked on
     * @param callback the callback name
     * @return this builder.
     */
    B destroy(Object callbackInstance, String callback);

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
    B destroy(InstanceCb callback);

    /**
     * Sets an Object instance method reference used as the "destroy" callback. 
     * This method is invoked as part of the life cycle management of the component implementation. 
     * The method takes as argument a Component parameter.
     *
     * @param callback an Object instance method reference. The method takes as argument a Component parameter.
     * @return this builder.
     */
    B destroy(InstanceCbComponent callback);

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
     * All of them will be searched to inject any of the dependencies. The method that
     * is invoked must return an <code>Object[]</code>.
     * 
     * @param getCompositionMethod the method to invoke
     * @return this builder
     */
    B composition(String getCompositionMethod);
    
    /**
     * Sets the instance and method to invoke to get back all instances that
     * are part of a composition and need dependencies injected. All of them
     * will be searched to inject any of the dependencies. The method that is
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
