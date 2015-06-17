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
 * Configuration dependency that can track the availability of a (valid) configuration. To use
 * it, specify a PID for the configuration. The dependency is always required, because if it is
 * not, it does not make sense to use the dependency manager. In that scenario, simply register
 * your component as a <code>ManagedService(Factory)</code> and handle everything yourself. Also,
 * only managed services are supported, not factories. If you need support for factories, then
 * you can use 
 * {@link DependencyManager#createFactoryConfigurationAdapterService(String, String, boolean)}.
 * There are a couple of things you need to be aware of when implementing the 
 * <code>updated(Dictionary)</code> method:
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
 * The callback invoked when a configuration dependency is updated can supports the following signatures:<p>
 * <ul><li> updated(Dictionary)
 *     <li> updated(Component, Dictionary)
 * </ul>
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ConfigurationDependency extends Dependency, ComponentDependencyDeclaration {
    /**
     * Sets the name of the callback method that should be invoked when a configuration
     * is available. The contract for this method is identical to that of
     * <code>ManagedService.updated(Dictionary) throws ConfigurationException</code>.
     * By default, if this method is not called, the callback name is "updated".
     * 
     * @param callback the name of the callback method
     */
	ConfigurationDependency setCallback(String callback);

    /**
     * Sets the name of the callback method that should be invoked when a configuration
     * is available. The contract for this method is identical to that of
     * <code>ManagedService.updated(Dictionary) throws ConfigurationException</code>.
     * 
     * @param instance the instance to call the callbacks on
     * @param callback the name of the callback method
     */
    ConfigurationDependency setCallback(Object instance, String callback);

    /**
     * Sets the <code>service.pid</code> of the configuration you are depending
     * on.
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
