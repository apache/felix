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

import java.util.Collection;
import java.util.Dictionary;
import java.util.Map;

import aQute.bnd.annotation.ProviderType;

/**
 * Configuration dependency that can track the availability of a (valid) configuration. To use
 * it, specify a PID for the configuration. The dependency is always required, because if it is
 * not, it does not make sense to use the dependency manager. In that scenario, simply register
 * your component as a <code>ManagedService(Factory)</code> and handle everything yourself. Also,
 * only managed services are supported, not factories. If you need support for factories, then
 * you can use 
 * {@link DependencyManager#createFactoryConfigurationAdapterService(String, String, boolean)}.
 * There are a couple of things you need to be aware of when implementing the 
 * <code>updated(Dictionary)</code> method:<p>
 * <ul>
 * <li>Make sure it throws a <code>ConfigurationException</code> or any other exception when you 
 * get a configuration that is invalid. In this case, the dependency will not change: 
 * if it was not available, it will still not be. If it was available, it will remain available 
 * and implicitly assume you keep working with your old configuration.</li>
 * <li>This method will be called before all required dependencies are available. Make sure you
 * do not depend on these to parse your settings.</li>
 * <li>unlike all other DM dependency callbacks, the update method is called from the CM configuration
 * update thread, and is not serialized with the internal queue maintained by the DM component.
 * So, take care to concurrent calls between updated callback and your other lifecycle callbacks.
 * <li>When the configuration is lost, updated callback is invoked with a null dictionary parameter,
 * and then the component stop lifecycle callback is invoked.
 * <li>When the DM component is stopped, then updated(null) is not invoked.
 * </ul>
 * 
 * <p> The callback invoked when a configuration dependency is updated can supports the following signatures:<p>
 * <ul><li> updated(Dictionary)
 *     <li> updated(Component, Dictionary)
 *     <li> updated(ConfigurationType)
 *     <li> updated(Component, ConfigurationType)
 * </ul>
 * 
 * <p> Support for a custom Configuration type is a new feature that allows you to specify an interface that is implemented 
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
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@ProviderType
public interface ConfigurationDependency extends Dependency, ComponentDependencyDeclaration {
    /**
     * Sets the name of the callback method that should be invoked when a configuration
     * is available. The contract for this method is identical to that of
     * <code>ManagedService.updated(Dictionary) throws ConfigurationException</code>.
     * By default, if this method is not called, the callback name is "updated".
     * 
     * <p> The callback is invoked on the instantiated component.
     * 
     * @param callback the name of the callback method
     */
	ConfigurationDependency setCallback(String callback);

    /**
     * Sets the name of the callback method that should be invoked when a configuration
     * is available. The contract for this method is identical to that of
     * <code>ManagedService.updated(Dictionary) throws ConfigurationException</code>.
     * 
     * <p> the callback is invoked on the callback instance, and the component is not 
     * yet instantiated at the time the callback is invoked.
     * 
     * @param instance the object to invoke the callback on
     * @param callback the name of the callback method
     */
    ConfigurationDependency setCallback(Object instance, String callback);

    /**
     * Sets the name of the callback method that should be invoked when a configuration
     * is available. The contract for this method is identical to that of
     * <code>ManagedService.updated(Dictionary) throws ConfigurationException</code>.
     * 
     * <p> the callback is invoked on the callback instance, and if <code>needsInstance</code> is true, 
     * the component is instantiated at the time the callback is invoked 
     * 
     * @param instance the object to invoke the callback on.
     * @param callback the name of the callback method
     * @param needsInstance true if the component must be instantiated before the callback is invoked on the callback instance.
     */
    ConfigurationDependency setCallback(Object instance, String callback, boolean needsInstance);

    /**
     * Sets the name of the callback method that should be invoked when a configuration
     * is available. The contract for this method is identical to that of
     * <code>ManagedService.updated(Dictionary) throws ConfigurationException</code> with the difference that 
     * instead of a Dictionary it accepts an interface of the given configuration type.<br>
     * By default, the pid is assumed to match the fqdn of the configuration type.
     * 
     * <p>The callback is invoked on the instantiated component.
     * 
     * @param callback the name of the callback method
     * @param configType the configuration type that the callback method accepts.
     */
    ConfigurationDependency setCallback(String callback, Class<?> configType);

    /**
     * Sets the name of the callback method that should be invoked when a configuration
     * is available. The contract for this method is identical to that of
     * <code>ManagedService.updated(Dictionary) throws ConfigurationException</code> with the difference that 
     * instead of a Dictionary it accepts an interface of the given configuration type.<br>
     * 
     * <p> The callback is invoked on the callback instance, and at this point the component is not yet instantiated.
     * 
     * @param instance the object to invoke the callback on.
     * @param callback the name of the callback method
     * @param configType the configuration type that the callback method accepts.
     */
    ConfigurationDependency setCallback(Object instance, String callback, Class<?> configType);

    /**
     * Sets the name of the callback method that should be invoked when a configuration
     * is available. The contract for this method is identical to that of
     * <code>ManagedService.updated(Dictionary) throws ConfigurationException</code> with the difference that 
     * instead of a Dictionary it accepts an interface of the given configuration type.<br>
     * 
     * <p> the callback is invoked on the callback instance, and if <code>needsInstance</code> is true, 
     * the component is instantiated at the time the callback is invoked 
     * 
     * @param instance the object to invoke the callback on.
     * @param callback the name of the callback method
     * @param configType the configuration type that the callback method accepts.
     * @param needsInstance true if the component must be instantiated before the callback is invoked on the callback instance.
     */
    ConfigurationDependency setCallback(Object instance, String callback, Class<?> configType, boolean needsInstance);

    /**
     * Sets the <code>service.pid</code> of the configuration you are depending on.
     */
	ConfigurationDependency setPid(String pid);

    /**
     * Sets propagation of the configuration properties to the service
     * properties. Any additional service properties specified directly are
     * merged with these.
     */
	ConfigurationDependency setPropagate(boolean propagate);

    /**
     * The label used to display the tab name (or section) where the properties
     * are displayed. Example: "Printer Service".
     * 
     * @return The label used to display the tab name where the properties are
     *         displayed (may be localized)
     */
	ConfigurationDependency setHeading(String heading);

    /**
     * A human readable description of the PID this configuration is associated
     * with. Example: "Configuration for the PrinterService bundle".
     * 
     * @return A human readable description of the PID this configuration is
     *         associated with (may be localized)
     */
	ConfigurationDependency setDescription(String description);

    /**
     * Points to the basename of the Properties file that can localize the Meta
     * Type informations. The default localization base name for the properties
     * is OSGI-INF/l10n/bundle, but can be overridden by the manifest
     * Bundle-Localization header (see core specification, in section
     * Localization on page 68). You can specify a specific localization
     * basename file using this method (e.g.
     * <code>setLocalization("person")</code> will match person_du_NL.properties
     * in the root bundle directory.
     */
	ConfigurationDependency setLocalization(String path);

    /**
     * Adds a MetaData regarding a given configuration property.
     */
	ConfigurationDependency add(PropertyMetaData properties);
}
