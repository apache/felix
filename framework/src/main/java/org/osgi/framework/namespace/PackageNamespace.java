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
 * Package Capability and Requirement Namespace.
 * 
 * <p>
 * A resource provides zero or more package capabilities (this is, exported
 * packages) and requires zero or more package requirements (that is, imported
 * packages).
 * 
 * <p>
 * This class defines the names for the attributes and directives for this
 * namespace. All unspecified capability attributes are of type {@code String}
 * and are used as arbitrary matching attributes for the capability. The values
 * associated with the specified directive and attribute keys are of type
 * {@code String}, unless otherwise indicated.
 * 
 * <p>
 * Unless otherwise noted, all directives specified on the
 * {@code Export-Package} header are visible in the capability and all
 * directives specified on the {@code Import-Package} and
 * {@code DynamicImport-Package} headers are visible in the requirement.
 * 
 * <ul>
 * <li>The {@link Namespace#CAPABILITY_EFFECTIVE_DIRECTIVE effective}
 * {@link Namespace#REQUIREMENT_EFFECTIVE_DIRECTIVE directives} must be ignored.
 * This namespace is only effective at {@link Namespace#EFFECTIVE_RESOLVE
 * resolve} time. An {@code effective} directive specified on the
 * {@code Export-Package}, {@code Import-Package} or
 * {@code DynamicImport-Package} headers must be ignored. An {@code effective}
 * directive must not be present in a capability or requirement.</li>
 * <li>The {@link Namespace#REQUIREMENT_CARDINALITY_DIRECTIVE cardinality}
 * directive has limited applicability to this namespace. A {@code cardinality}
 * directive specified on the {@code Import-Package} or
 * {@code DynamicImport-Package} headers must be ignored. Only requirements with
 * {@link Namespace#REQUIREMENT_RESOLUTION_DIRECTIVE resolution} set to
 * {@link #RESOLUTION_DYNAMIC dynamic} and the package name contains a wildcard
 * must have the {@code cardinality} directive set to
 * {@link Namespace#CARDINALITY_MULTIPLE multiple}. Otherwise, a
 * {@code cardinality} directive must not be present in a requirement.</li>
 * </ul>
 * 
 * @Immutable
 * @version $Id: 5adc45bd1ae26120cbff3562c7c8cefc01e38bd3 $
 */
public final class PackageNamespace extends AbstractWiringNamespace {

	/**
	 * Namespace name for package capabilities and requirements.
	 * 
	 * <p>
	 * Also, the capability attribute used to specify the name of the package.
	 */
	public static final String	PACKAGE_NAMESPACE							= "osgi.wiring.package";

	/**
	 * The capability directive used to specify the comma separated list of
	 * classes which must be allowed to be exported.
	 */
	public final static String	CAPABILITY_INCLUDE_DIRECTIVE				= "include";

	/**
	 * The capability directive used to specify the comma separated list of
	 * classes which must not be allowed to be exported.
	 */
	public final static String	CAPABILITY_EXCLUDE_DIRECTIVE				= "exclude";

	/**
	 * The capability attribute contains the {@code Version} of the package if
	 * one is specified or {@code 0.0.0} if not specified. The value of this
	 * attribute must be of type {@code Version}.
	 */
	public final static String	CAPABILITY_VERSION_ATTRIBUTE				= "version";

	/**
	 * The capability attribute contains the symbolic name of the resource
	 * providing the package.
	 */
	public final static String	CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE	= "bundle-symbolic-name";

	/**
	 * The directive value identifying a dynamic requirement resolution type. A
	 * dynamic resolution type indicates that the requirement is resolved
	 * dynamically at runtime (such as a dynamically imported package) and the
	 * resource will be resolved without the requirement being resolved.
	 * 
	 * @see Namespace#REQUIREMENT_RESOLUTION_DIRECTIVE
	 */
	public final static String	RESOLUTION_DYNAMIC							= "dynamic";

	private PackageNamespace() {
		// empty
	}
}
