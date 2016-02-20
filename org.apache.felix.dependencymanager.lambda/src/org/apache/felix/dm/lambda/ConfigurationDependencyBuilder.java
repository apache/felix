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

import java.util.Collection;
import java.util.Dictionary;
import java.util.Map;

import org.apache.felix.dm.ConfigurationDependency;
import org.apache.felix.dm.lambda.callbacks.CbConfiguration;
import org.apache.felix.dm.lambda.callbacks.CbConfigurationComponent;
import org.apache.felix.dm.lambda.callbacks.CbDictionary;
import org.apache.felix.dm.lambda.callbacks.CbDictionaryComponent;
import org.apache.felix.dm.lambda.callbacks.InstanceCbConfiguration;
import org.apache.felix.dm.lambda.callbacks.InstanceCbConfigurationComponent;
import org.apache.felix.dm.lambda.callbacks.InstanceCbDictionary;
import org.apache.felix.dm.lambda.callbacks.InstanceCbDictionaryComponent;

/**
 * Builds a Dependency Manager Configuration Dependency.
 * Two families of callbacks are supported: <p>
 * 
 * <ul> 
 * <li>reflection based callbacks: you specify a callback method name
 * <li>method reference callbacks: you specify a java8 method reference
 * </ul>
 * 
 * <p> Callbacks may accept a Dictionary, a Component, or a user defined configuration type interface.
 * 
 * If you only specify a pid, by default the callback method name is assumed to be "updated".
 * 
 * <p> Configuration types are a new feature that allows you to specify an interface that is implemented 
 * by DM and such interface is then injected to your callback instead of the actual Dictionary.
 * Using such configuration interface provides a way for creating type-safe configurations from a actual {@link Dictionary} that is
 * normally injected by Dependency Manager.
 * The callback accepts in argument an interface that you have to provide, and DM will inject a proxy that converts
 * method calls from your configuration-type to lookups in the actual map or dictionary. The results of these lookups are then
 * converted to the expected return type of the invoked configuration method.<br>
 * As proxies are injected, no implementations of the desired configuration-type are necessary!
 * </p>
 * <p>
 * The lookups performed are based on the name of the method called on the configuration type. The method names are
 * "mangled" to the following form: <tt>[lower case letter] [any valid character]*</tt>. Method names starting with
 * <tt>get</tt> or <tt>is</tt> (JavaBean convention) are stripped from these prefixes. For example: given a dictionary
 * with the key <tt>"foo"</tt> can be accessed from a configuration-type using the following method names:
 * <tt>foo()</tt>, <tt>getFoo()</tt> and <tt>isFoo()</tt>.
 * </p>
 * <p>
 * The return values supported are: primitive types (or their object wrappers), strings, enums, arrays of
 * primitives/strings, {@link Collection} types, {@link Map} types, {@link Class}es and interfaces. When an interface is
 * returned, it is treated equally to a configuration type, that is, it is returned as a proxy.
 * </p>
 * <p>
 * Arrays can be represented either as comma-separated values, optionally enclosed in square brackets. For example:
 * <tt>[ a, b, c ]</tt> and <tt>a, b,c</tt> are both considered an array of length 3 with the values "a", "b" and "c".
 * Alternatively, you can append the array index to the key in the dictionary to obtain the same: a dictionary with
 * "arr.0" =&gt; "a", "arr.1" =&gt; "b", "arr.2" =&gt; "c" would result in the same array as the earlier examples.
 * </p>
 * <p>
 * Maps can be represented as single string values similarly as arrays, each value consisting of both the key and value
 * separated by a dot. Optionally, the value can be enclosed in curly brackets. Similar to array, you can use the same
 * dot notation using the keys. For example, a dictionary with 
 * 
 * <pre>{@code "map" => "{key1.value1, key2.value2}"}</pre> 
 * 
 * and a dictionary with <p>
 * 
 * <pre>{@code "map.key1" => "value1", "map2.key2" => "value2"}</pre> 
 * 
 * result in the same map being returned.
 * Instead of a map, you could also define an interface with the methods <tt>getKey1()</tt> and <tt>getKey2</tt> and use
 * that interface as return type instead of a {@link Map}.
 * </p>
 * <p>
 * In case a lookup does not yield a value from the underlying map or dictionary, the following rules are applied:
 * <ol>
 * <li>primitive types yield their default value, as defined by the Java Specification;
 * <li>string, {@link Class}es and enum values yield <code>null</code>;
 * <li>for arrays, collections and maps, an empty array/collection/map is returned;
 * <li>for other interface types that are treated as configuration type a null-object is returned.
 * </ol>
 * </p>
 * 
 * <b> Sample codes: </b>
 * 
 * <p> Code example with a component that defines a Configuration Dependency using a specific callback method reference,
 * and the method accepts in argument a configuration type (the pid is assumed to be the fqdn of the configuration type):
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *     public void init(BundleContext ctx, DependencyManager dm) throws Exception { 
 *         component(comp -> comp
 *           .impl(ServiceImpl.class)
 *           .withCnf(conf -> conf.update(MyConfig.class, ServiceImpl::modified)));  
 *    }
 * }
 * }</pre>
 * 
 * <p> Code example with a component that defines a Configuration Dependency using a specific callback method reference
 * which accepts a Dictionary in argument:
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *     public void init(BundleContext ctx, DependencyManager dm) throws Exception { 
 *         component(comp -> comp
 *           .impl(ServiceImpl.class)
 *           .withCnf(conf -> conf.pid("my.pid").update(ServiceImpl::modified)));
 *    }
 * }
 * }</pre>
 * 
 * <p> Code example which defines a configuration dependency injected in the "ServiceImpl.updated(Dictionary)" callback:
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *     public void init(BundleContext ctx, DependencyManager dm) throws Exception { 
 *         component(comp -> comp.impl(ServiceImpl.class).withCnf("my.pid"));
 *    }
 * }
 * }</pre>
 * 
 * <p> Code example with a component that defines a Configuration Dependency using a specific callback method name:
 * 
 * <pre> {@code
 * public class Activator extends DependencyManagerActivator {
 *     public void init(BundleContext ctx, DependencyManager dm) throws Exception { 
 *         component(comp -> comp.impl(ServiceImpl.class).withCnf(conf -> conf.pid("my.pid").update("modified")));  
 *    }
 * }
 * }</pre>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ConfigurationDependencyBuilder extends DependencyBuilder<ConfigurationDependency> { 
    /**
     * Sets the pid for this configuration dependency.
     * 
     * @param pid the configuration dependency pid.
     * @return this builder
     */
    ConfigurationDependencyBuilder pid(String pid);
    
    /**
     * Sets propagation of the configuration to the service properties (false by default). 
     * All public configuration properties (not starting with a dot) will be propagated to the component service properties.
     * 
     * @return this builder
     */
    ConfigurationDependencyBuilder propagate();
    
    /**
     * Sets propagation of the configuration properties to the service properties (false by default).
     * 
     * @param propagate true if all public configuration properties (not starting with a dot) must be propagated to the component service properties (false by default)
     * @return this builder
     */
    ConfigurationDependencyBuilder propagate(boolean propagate);
        
    /**
     * Sets a callback method to call on the component implementation class(es) when the configuration is updated. When the configuration is lost, the callback is invoked
     * with a null dictionary. 
     * 
     * <p>The following callback signatures are supported and searched in the following order:
     * <ol>
     * <li>method(Dictionary)</li>
     * <li>method(Component, Dictionary)</li>
     * </ol>
     *
     * @param updateMethod the name of the callback
     * @return this builder
     */
    ConfigurationDependencyBuilder update(String updateMethod);
    
    /**
     * Sets a callback method to call on the component implementation class(es) when the configuration is updated. The callback is invoked with a configuration type
     * argument (null if the configuration is lost).
     * 
     * <p>The following callback signatures are supported and searched in the following order:
     * <ol>
     * <li>method(Dictionary)</li>
     * <li>method(Component, Dictionary)</li>
     * <li>method(Configuration) // same type as the one specified in the "configType" argument</li>
     * <li>method(Component, Configuration) // Configuration has the same type as the one specified in the "configType" argument</li>
     * </ol>
     * 
     * @param configType the type of a configuration that is passed as argument to the callback
     * @param updateMethod the callback to call on the component implementation class(es) when the configuration is updated.
     * @return this builder
     */
    ConfigurationDependencyBuilder update(Class<?> configType, String updateMethod);
    
    /**
     * Sets a callback method to call on a given Object instance when the configuration is updated.  
     * When the updated method is invoked, the Component implementation is not yet instantiated. This method
     * can be typically used by a Factory object which needs the configuration before it can create the actual 
     * component implementation instance(s).
     * 
     * When the configuration is lost, the callback is invoked with a null dictionary, and the following signatures are supported:
     * <ol>
     * <li>method(Dictionary)</li>
     * <li>method(Component, Dictionary)</li>
     * </ol>
     * 
     * @param callbackInstance the object instance on which the updatedMethod is invoked
     * @param updateMethod the callback to call on the callbackInstance when the configuration is updated.
     * @return this builder
     */
    ConfigurationDependencyBuilder update(Object callbackInstance, String updateMethod);

    /**
     * Sets a callback method to call on a given Object instance when the configuration is updated. 
     * When the updated method is invoked, the Component implementation is not yet instantiated. This method
     * can be typically used by a Factory object which needs the configuration before it can create the actual 
     * component implementation instance(s).
     * The callback is invoked with a configuration type argument (null of the configuration is lost).
     * 
     * <p>The following callback signatures are supported and searched in the following order:
     * <ol>
     * <li>method(Dictionary)</li>
     * <li>method(Component, Dictionary)</li>
     * <li>method(Configuration) // same type as the one specified in the "configType" argument</li>
     * <li>method(Component, Configuration) // Configuration has the same type as the one specified in the "configType" argument</li>
     * </ol>
     * 
     * @param configType the type of a configuration that is passed as argument to the callback
     * @param callbackInstance the object instance on which the updatedMethod is invoked
     * @param updateMethod the callback to call on the callbackInstance when the configuration is updated.
     * @return this builder
     */
    ConfigurationDependencyBuilder update(Class<?> configType, Object callbackInstance, String updateMethod);

    /**
     * Sets a reference to a "callback(Dictionary)" method from one of the component implementation classes. 
     * The method is invoked with a Dictionary argument (which is null if the configuration is lost).
     *
     * @param <T> The type of the target component implementation class on which the method is invoked
     * @param callback a reference to a method of one of the component implementation classes.
     * @return this builder
     */
    <T> ConfigurationDependencyBuilder update(CbDictionary<T> callback);

    /**
     * Sets a reference to a "callback(Dictionary, Component)" method from one of the component implementation classes. 
     * The method is invoked with Dictionary/Component arguments. When the configuration is lost, the Dictionary argument
     * is null.
     *
     * @param <T> The type of the target component implementation class on which the method is invoked
     * @param callback a reference to a method callback defined in one of the the component implementation classes.
     * @return this builder
     */
    <T> ConfigurationDependencyBuilder update(CbDictionaryComponent<T> callback);

    /**
     * Sets a reference to a "callback(Configuration)" method from one of the component implementation classes. 
     * The method is invoked with a configuration type argument (null if the configuration is lost).
     *
     * @param <T> The type of the target component implementation class on which the method is invoked
     * @param <U> the type of the configuration interface accepted by the callback method.
     * @param configType the type of a configuration that is passed as argument to the callback
     * @param callback the callback method reference which must point to a method from one of the component implementation classes. The method
     * takes as argument an interface which will be implemented by a dynamic proxy that wraps the actual configuration properties.
     * @return this builder
     */
    <T, U> ConfigurationDependencyBuilder update(Class<U> configType, CbConfiguration<T, U> callback);
    
    /**
     * Sets a reference to a "callback(Configuration, Component)" method from one of the component implementation classes. 
     * The method is invoked with two args: configuration type, Component. The configuration type argument is null if the configuration is lost.
     *
     * @param <T> The type of the target component implementation class on which the method is invoked
     * @param <U> the type of the configuration interface accepted by the callback method.
     * @param configType the type of a configuration that is passed as argument to the callback
     * @param callback the reference to a method from one of the component implementation classes. The method
     * takes as argument an interface which will be implemented by a dynamic proxy that wraps the actual configuration properties. It also
     * takes as the second argument a Component object.
     * @return this builder
     */
    <T, U> ConfigurationDependencyBuilder update(Class<U> configType, CbConfigurationComponent<T, U> callback);
    
    /**
     * Sets a reference to a "callback(Dictionary)" method from an Object instance.
     * 
     * @param callback a reference to an Object instance which takes as argument a Dictionary (null if the configuration is lost).
     * @return this builder
     */
    ConfigurationDependencyBuilder update(InstanceCbDictionary callback);
    
    /**
     * Sets a reference to a "callback(Dictionary, Component)" method from an Object instance. The method accepts
     * a Dictionary and a Component object. The passed Dictionary is null in case the configuration is lost.
     * 
     * @param callback a reference to method from an Object instance which takes as argument a Dictionary and a Component
     * @return this builder
     */
    ConfigurationDependencyBuilder update(InstanceCbDictionaryComponent callback);

    /**
     * Sets a reference to a "callback(Configuration)" method from an Object instance. The configuration type argument is null if the configuration is lost.
     *
     * @param <T> the type of the configuration interface accepted by the callback method.
     * @param configType the class of the configuration that is passed as argument to the callback
     * @param updated a reference to an Object instance which takes as argument the given configuration type
     * @return this builder
     */
    <T> ConfigurationDependencyBuilder update(Class<T> configType, InstanceCbConfiguration<T> updated);  
    
    /**
     * Sets a reference to a "callback(Configuration, Component)" method from an Object instance. The method accepts
     * a configuration type and a Component object. The configuration type argument is null if the configuration is lost.
     *
     * @param <T> the type of the configuration interface accepted by the callback method.
     * @param configType the class of the configuration that is passed as argument to the callback
     * @param updated a reference to an Object instance which takes as argument a the given configuration type, and a Component object.
     * @return this builder
     */
    <T> ConfigurationDependencyBuilder update(Class<T> configType, InstanceCbConfigurationComponent<T> updated);
}

