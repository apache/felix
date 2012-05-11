/*
 * Copyright (c) OSGi Alliance (2012). All Rights Reserved.
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

package org.osgi.framework.namespace;

import org.osgi.resource.Namespace;

/**
 * Execution Environment Capability and Requirement Namespace.
 * 
 * <p>
 * This class defines the names for the attributes and directives for this
 * namespace. All unspecified capability attributes are of type {@code String}
 * and are used as arbitrary matching attributes for the capability. The values
 * associated with the specified directive and attribute keys are of type
 * {@code String}, unless otherwise indicated.
 * 
 * @Immutable
 * @version $Id: e1c30aac8efacc1b21ab20ffebcc1af30a1054a8 $
 */
public final class ExecutionEnvironmentNamespace extends Namespace {

	/**
	 * Namespace name for execution environment capabilities and requirements.
	 * 
	 * <p>
	 * Also, the capability attribute used to specify the name of the execution
	 * environment.
	 */
	public static final String	EXECUTION_ENVIRONMENT_NAMESPACE	= "osgi.ee";

	/**
	 * The capability attribute contains the versions of the execution
	 * environment. The value of this attribute must be of type
	 * {@code List<Version>}.
	 */
	public final static String	CAPABILITY_VERSION_ATTRIBUTE	= "version";

	private ExecutionEnvironmentNamespace() {
		// empty
	}
}
