/*
 * Copyright (c) OSGi Alliance (2017). All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgi.service.configurator;

/**
 * Defines standard constants for the Configurator services.
 * 
 * @author $Id$
 */
public final class ConfiguratorConstants {
	private ConfiguratorConstants() {
		// non-instantiable
	}

	/**
	 * Framework property specifying initial configurations to be applied by the
	 * Configurator on startup.
	 * <p>
	 * If the value of this property starts with a '{' (ignoring leading
	 * whitespace) it is interpreted as JSON and directly feed into the
	 * Configurator.
	 * <p>
	 * Otherwise the value is interpreted as a comma separated list of URLs
	 * pointing to JSON documents.
	 */
	public static final String	CONFIGURATOR_INITIAL	= "configurator.initial";

	/**
	 * Framework property specifying the directory to be used by the
	 * Configurator to store binary files.
	 * <p>
	 * If a value is specified, the Configurator will write all binaries to the
	 * given directory. Therefore the Configurator bundle needs read and write
	 * access to this directory.
	 * <p>
	 * If this property is not specified, the Configurator will store all binary
	 * files in its bundle private data area.
	 */
	public static final String	CONFIGURATOR_BINARIES	= "configurator.binaries";

	/**
	 * Prefix to mark properties as input for the Configurator when processing a
	 * configuration.
	 */
	public static final String	PROPERTY_PREFIX			= ":configurator:";

	/**
	 * Global property in the configuration JSON specifying the version of the
	 * JSON format.
	 * <p>
	 * Currently only version {@code 1} is defined for the JSON format and
	 * therefore the only allowed value is {@code 1} for this property. If this
	 * property is not specified, {@code 1} is assumed.
	 */
	public static final String	PROPERTY_JSON_VERSION	= PROPERTY_PREFIX
			+ "json-version";

	/**
	 * Configuration property holding the optional information about the
	 * environments where the configuration applies.
	 * <p>
	 * The value of this property must either be of type {@code String} or {code
	 * String[]}.
	 */
	public static final String	PROPERTY_ENVIRONMENTS	= PROPERTY_PREFIX
			+ "environments";

	/**
	 * Configuration property for the configuration ranking.
	 * <p>
	 * The value of this property must be convertible to a number.
	 */
	public static final String	PROPERTY_RANKING		= PROPERTY_PREFIX
			+ "ranking";

	/**
	 * Configuration property for the configuration policy.
	 * <p>
	 * Allowed values are {@link #POLICY_DEFAULT} and {@link #POLICY_FORCE}
	 * 
	 * @see #POLICY_DEFAULT
	 * @see #POLICY_FORCE
	 */
	public static final String	PROPERTY_POLICY			= PROPERTY_PREFIX
			+ "policy";

	/**
	 * Value for defining the default policy.
	 * 
	 * @see #PROPERTY_POLICY
	 */
	public static final String	POLICY_DEFAULT			= "default";

	/**
	 * Value for defining the force policy.
	 * 
	 * @see #PROPERTY_POLICY
	 */
	public static final String	POLICY_FORCE			= "force";

	/**
	 * Key in the JSON that denotes the section containing all configurations.
	 */
	public static final String	KEY_CONFIGURATIONS		= "configurations";
}
