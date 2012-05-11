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

package org.osgi.resource;

/**
 * Capability and Requirement Namespaces base class.
 * 
 * <p>
 * This class is the common class shared by all OSGi defined namespaces. It
 * defines the names for the common attributes and directives for the OSGi
 * specified namespaces.
 * 
 * <p>
 * The OSGi Alliance reserves the right to extend the set of directives and
 * attributes which have specified semantics for all of the specified
 * namespaces.
 * 
 * <p>
 * The values associated with these keys are of type {@code String}, unless
 * otherwise indicated.
 * 
 * @Immutable
 * @version $Id: 43c9ff5cea19546d71c4703db71a2b5070a3f2fa $
 */
public abstract class Namespace {

	/**
	 * The capability directive used to specify the comma separated list of
	 * package names used by a capability.
	 */
	public final static String	CAPABILITY_USES_DIRECTIVE			= "uses";

	/**
	 * The capability directive used to specify the effective time for the
	 * capability. The default value is {@link #EFFECTIVE_RESOLVE resolve}.
	 * 
	 * @see #EFFECTIVE_RESOLVE resolve
	 * @see #EFFECTIVE_ACTIVE active
	 */
	public final static String	CAPABILITY_EFFECTIVE_DIRECTIVE		= "effective";

	/**
	 * The requirement directive used to specify a capability filter. This
	 * filter is used to match against a capability's attributes.
	 */
	public final static String	REQUIREMENT_FILTER_DIRECTIVE		= "filter";

	/**
	 * The requirement directive used to specify the resolution type for a
	 * requirement. The default value is {@link #RESOLUTION_MANDATORY mandatory}
	 * .
	 * 
	 * @see #RESOLUTION_MANDATORY mandatory
	 * @see #RESOLUTION_OPTIONAL optional
	 */
	public final static String	REQUIREMENT_RESOLUTION_DIRECTIVE	= "resolution";

	/**
	 * The directive value identifying a mandatory requirement resolution type.
	 * A mandatory resolution type indicates that the requirement must be
	 * resolved when the resource is resolved. If such a requirement cannot be
	 * resolved, the resource fails to resolve.
	 * 
	 * @see #REQUIREMENT_RESOLUTION_DIRECTIVE
	 */
	public final static String	RESOLUTION_MANDATORY				= "mandatory";

	/**
	 * The directive value identifying an optional requirement resolution type.
	 * An optional resolution type indicates that the requirement is optional
	 * and the resource may be resolved without the requirement being resolved.
	 * 
	 * @see #REQUIREMENT_RESOLUTION_DIRECTIVE
	 */
	public final static String	RESOLUTION_OPTIONAL					= "optional";

	/**
	 * The requirement directive used to specify the effective time for the
	 * requirement. The default value is {@link #EFFECTIVE_RESOLVE resolve}.
	 * 
	 * @see #EFFECTIVE_RESOLVE resolve
	 * @see #EFFECTIVE_ACTIVE active
	 */
	public final static String	REQUIREMENT_EFFECTIVE_DIRECTIVE		= "effective";

	/**
	 * The directive value identifying a {@link #CAPABILITY_EFFECTIVE_DIRECTIVE
	 * capability} or {@link #REQUIREMENT_EFFECTIVE_DIRECTIVE requirement} that
	 * is effective at resolve time. Capabilities and requirements with an
	 * effective time of resolve are the only capabilities which are processed
	 * while resolving a resource.
	 * 
	 * @see #REQUIREMENT_EFFECTIVE_DIRECTIVE
	 * @see #CAPABILITY_EFFECTIVE_DIRECTIVE
	 */
	public final static String	EFFECTIVE_RESOLVE					= "resolve";

	/**
	 * The directive value identifying a {@link #CAPABILITY_EFFECTIVE_DIRECTIVE
	 * capability} or {@link #REQUIREMENT_EFFECTIVE_DIRECTIVE requirement} that
	 * is effective at active time. Capabilities and requirements with an
	 * effective time of active are ignored while resolving a resource.
	 * 
	 * @see #REQUIREMENT_EFFECTIVE_DIRECTIVE
	 * @see #CAPABILITY_EFFECTIVE_DIRECTIVE
	 */
	public final static String	EFFECTIVE_ACTIVE					= "active";

	/**
	 * The requirement directive used to specify the cardinality for a
	 * requirement. The default value is {@link #CARDINALITY_SINGLE single}.
	 * 
	 * @see #CARDINALITY_MULTIPLE multiple
	 * @see #CARDINALITY_SINGLE single
	 */
	public final static String	REQUIREMENT_CARDINALITY_DIRECTIVE	= "cardinality";

	/**
	 * The directive value identifying a multiple
	 * {@link #REQUIREMENT_CARDINALITY_DIRECTIVE cardinality} type.
	 * 
	 * @see #REQUIREMENT_CARDINALITY_DIRECTIVE
	 */
	public final static String	CARDINALITY_MULTIPLE				= "multiple";

	/**
	 * The directive value identifying a
	 * {@link #REQUIREMENT_CARDINALITY_DIRECTIVE cardinality} type of single.
	 * 
	 * @see #REQUIREMENT_CARDINALITY_DIRECTIVE
	 */
	public final static String	CARDINALITY_SINGLE					= "single";

	/**
	 * Protected constructor for Namespace sub-types.
	 */
	protected Namespace() {
		// empty
	}
}
