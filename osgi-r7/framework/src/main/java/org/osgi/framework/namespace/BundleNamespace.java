/*
 * Copyright (c) OSGi Alliance (2012, 2013). All Rights Reserved.
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
 * Bundle Capability and Requirement Namespace.
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
 * {@code Bundle-SymbolicName} header are visible in the capability and all
 * directives specified on the {@code Require-Bundle} header are visible in the
 * requirement.
 * 
 * <ul>
 * <li>The {@link Namespace#CAPABILITY_USES_DIRECTIVE uses} directive must be
 * ignored. A {@code uses} directive specified on the
 * {@code Bundle-SymbolicName} header must be ignored. A {@code uses} directive
 * must not be present in the capability.</li>
 * <li>The {@link Namespace#CAPABILITY_EFFECTIVE_DIRECTIVE effective}
 * {@link Namespace#REQUIREMENT_EFFECTIVE_DIRECTIVE directives} must be ignored.
 * This namespace is only effective at {@link Namespace#EFFECTIVE_RESOLVE
 * resolve} time. An {@code effective} directive specified on the
 * {@code Bundle-SymbolicName} or {@code Require-Bundle} headers must be
 * ignored. An {@code effective} directive must not be present in a capability
 * or requirement.</li>
 * <li>The {@link Namespace#REQUIREMENT_CARDINALITY_DIRECTIVE cardinality}
 * directive must be ignored. A {@code cardinality} directive specified on the
 * {@code Require-Bundle} header must be ignored. A {@code cardinality}
 * directive must not be present in a requirement.</li>
 * </ul>
 * 
 * <p>
 * A non-fragment resource with the {@link IdentityNamespace#TYPE_BUNDLE
 * osgi.bundle} type {@link IdentityNamespace#CAPABILITY_TYPE_ATTRIBUTE
 * identity} provides exactly one<sup>&#8224;</sup> bundle capability (that is,
 * the bundle can be required by another bundle). A fragment resource with the
 * {@link IdentityNamespace#TYPE_FRAGMENT osgi.fragment} type
 * {@link IdentityNamespace#CAPABILITY_TYPE_ATTRIBUTE identity} must not declare
 * a bundle capability. A resource requires zero or more bundle requirements
 * (that is, required bundles).
 * <p>
 * &#8224; A resource with no symbolic name must not provide a bundle
 * capability.
 * 
 * @Immutable
 * @author $Id: 2672d40cf3705b2cf21d01530e4bdfa2cdc61764 $
 */
public final class BundleNamespace extends AbstractWiringNamespace {

	/**
	 * Namespace name for bundle capabilities and requirements.
	 * 
	 * <p>
	 * Also, the capability attribute used to specify the symbolic name of the
	 * bundle.
	 */
	public static final String	BUNDLE_NAMESPACE							= "osgi.wiring.bundle";

	/**
	 * The capability directive identifying if the resource is a singleton. A
	 * {@code String} value of &quot;{@code true}&quot; indicates the resource
	 * is a singleton; any other value or {@code null} indicates the resource is
	 * not a singleton.
	 * 
	 * <p>
	 * This directive should be examined using the {@link IdentityNamespace
	 * identity} namespace.
	 * 
	 * @see IdentityNamespace#CAPABILITY_SINGLETON_DIRECTIVE
	 */
	public static final String	CAPABILITY_SINGLETON_DIRECTIVE				= "singleton";

	/**
	 * The capability directive identifying if and when a fragment may attach to
	 * a host bundle.
	 * 
	 * <p>
	 * This directive should be examined using the {@link HostNamespace host}
	 * namespace.
	 * 
	 * @see HostNamespace#CAPABILITY_FRAGMENT_ATTACHMENT_DIRECTIVE
	 */
	public static final String	CAPABILITY_FRAGMENT_ATTACHMENT_DIRECTIVE	= "fragment-attachment";

	/**
	 * The requirement directive used to specify the type of the extension
	 * fragment.
	 * 
	 * <p>
	 * This directive should be examined using the {@link HostNamespace host}
	 * namespace.
	 * 
	 * @see HostNamespace#REQUIREMENT_EXTENSION_DIRECTIVE
	 */
	public final static String	REQUIREMENT_EXTENSION_DIRECTIVE				= "extension";

	/**
	 * The requirement directive used to specify the visibility type for a
	 * requirement. The default value is {@link #VISIBILITY_PRIVATE private}.
	 * 
	 * @see #VISIBILITY_PRIVATE private
	 * @see #VISIBILITY_REEXPORT reexport
	 */
	public final static String	REQUIREMENT_VISIBILITY_DIRECTIVE			= "visibility";

	/**
	 * The directive value identifying a private
	 * {@link #REQUIREMENT_VISIBILITY_DIRECTIVE visibility} type. A private
	 * visibility type indicates that any {@link PackageNamespace packages} that
	 * are exported by the required bundle are not made visible on the export
	 * signature of the requiring bundle. .
	 * 
	 * @see #REQUIREMENT_VISIBILITY_DIRECTIVE
	 */
	public final static String	VISIBILITY_PRIVATE							= "private";

	/**
	 * The directive value identifying a reexport
	 * {@link #REQUIREMENT_VISIBILITY_DIRECTIVE visibility} type. A reexport
	 * visibility type indicates any {@link PackageNamespace packages} that are
	 * exported by the required bundle are re-exported by the requiring bundle.
	 * 
	 * @see #REQUIREMENT_VISIBILITY_DIRECTIVE
	 */
	public final static String	VISIBILITY_REEXPORT							= "reexport";

	private BundleNamespace() {
		// empty
	}
}
