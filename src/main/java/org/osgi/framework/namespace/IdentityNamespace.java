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
 * Identity Capability and Requirement Namespace.
 * 
 * <p>
 * This class defines the names for the attributes and directives for this
 * namespace. All unspecified capability attributes are of type {@code String}
 * and are used as arbitrary matching attributes for the capability. The values
 * associated with the specified directive and attribute keys are of type
 * {@code String}, unless otherwise indicated.
 * 
 * <p>
 * Each resource provides exactly one<sup>&#8224;</sup> identity capability that
 * can be used to identify the resource.
 * 
 * <p>
 * The bundle wiring for the bundle revision provides exactly
 * one<sup>&#8224;</sup> identity capability.
 * 
 * <p>
 * &#8224; A resource with no symbolic name must not provide an identity
 * capability.
 * 
 * @Immutable
 * @version $Id: e34dcaba1f828326a0db13b3d811b2d170ff97a5 $
 */
public final class IdentityNamespace extends Namespace {

	/**
	 * Namespace name for identity capabilities and requirements.
	 * 
	 * <p>
	 * Also, the capability attribute used to specify the symbolic name of the
	 * resource.
	 */
	public static final String	IDENTITY_NAMESPACE					= "osgi.identity";

	/**
	 * The capability directive identifying if the resource is a singleton. A
	 * {@code String} value of &quot;true&quot; indicates the resource is a
	 * singleton; any other value or {@code null} indicates the resource is not
	 * a singleton.
	 */
	public static final String	CAPABILITY_SINGLETON_DIRECTIVE		= "singleton";

	/**
	 * The capability attribute identifying the {@code Version} of the resource
	 * if one is specified or {@code 0.0.0} if not specified. The value of this
	 * attribute must be of type {@code Version}.
	 */
	public static final String	CAPABILITY_VERSION_ATTRIBUTE		= "version";

	/**
	 * The capability attribute identifying the resource type. If the resource
	 * has no type then the value {@link #TYPE_UNKNOWN unknown} must be used for
	 * the attribute.
	 * 
	 * @see #TYPE_BUNDLE
	 * @see #TYPE_FRAGMENT
	 * @see #TYPE_UNKNOWN
	 */
	public static final String	CAPABILITY_TYPE_ATTRIBUTE			= "type";

	/**
	 * The attribute value identifying the resource
	 * {@link #CAPABILITY_TYPE_ATTRIBUTE type} as an OSGi bundle.
	 * 
	 * @see #CAPABILITY_TYPE_ATTRIBUTE
	 */
	public static final String	TYPE_BUNDLE							= "osgi.bundle";

	/**
	 * The attribute value identifying the resource
	 * {@link #CAPABILITY_TYPE_ATTRIBUTE type} as an OSGi fragment.
	 * 
	 * @see #CAPABILITY_TYPE_ATTRIBUTE
	 */
	public static final String	TYPE_FRAGMENT						= "osgi.fragment";

	/**
	 * The attribute value identifying the resource
	 * {@link #CAPABILITY_TYPE_ATTRIBUTE type} as unknown.
	 * 
	 * @see #CAPABILITY_TYPE_ATTRIBUTE
	 */
	public static final String	TYPE_UNKNOWN						= "unknown";

	/**
	 * The capability attribute that contains a human readable copyright notice
	 * for the resource. See the {@code Bundle-Copyright} manifest header.
	 */
	public static final String	CAPABILITY_COPYRIGHT_ATTRIBUTE		= "copyright";

	/**
	 * The capability attribute that contains a human readable description for
	 * the resource. See the {@code Bundle-Description} manifest header.
	 */
	public static final String	CAPABILITY_DESCRIPTION_ATTRIBUTE	= "description";

	/**
	 * The capability attribute that contains the URL to documentation for the
	 * resource. See the {@code Bundle-DocURL} manifest header.
	 */
	public static final String	CAPABILITY_DOCUMENTATION_ATTRIBUTE	= "documentation";

	/**
	 * The capability attribute that contains the URL to the license for the
	 * resource. See the {@code name} portion of the {@code Bundle-License}
	 * manifest header.
	 */
	public static final String	CAPABILITY_LICENSE_ATTRIBUTE		= "license";

	/**
	 * The requirement directive that classifies the relationship with another
	 * resource.
	 * 
	 * @see #CLASSIFIER_SOURCES
	 * @see #CLASSIFIER_JAVADOC
	 */
	public static final String	REQUIREMENT_CLASSIFIER_DIRECTIVE	= "classifier";

	/**
	 * The attribute value identifying the resource
	 * {@link #REQUIREMENT_CLASSIFIER_DIRECTIVE classifier} as an archive
	 * containing the source code in the same directory layout as the resource.
	 * 
	 * @see #REQUIREMENT_CLASSIFIER_DIRECTIVE
	 */

	public static final String	CLASSIFIER_SOURCES					= "sources";
	/**
	 * The attribute value identifying the resource
	 * {@link #REQUIREMENT_CLASSIFIER_DIRECTIVE classifier} as an archive
	 * containing the javadoc in the same directory layout as the resource.
	 * 
	 * @see #REQUIREMENT_CLASSIFIER_DIRECTIVE
	 */
	public static final String	CLASSIFIER_JAVADOC					= "javadoc";

	private IdentityNamespace() {
		// empty
	}
}
