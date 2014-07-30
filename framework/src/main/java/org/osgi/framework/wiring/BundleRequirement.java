/*
 * Copyright (c) OSGi Alliance (2010, 2013). All Rights Reserved.
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

package org.osgi.framework.wiring;

import java.util.Map;
import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.namespace.AbstractWiringNamespace;
import org.osgi.resource.Requirement;

/**
 * A requirement that has been declared from a {@link BundleRevision bundle
 * revision}.
 * 
 * @ThreadSafe
 * @author $Id: ac0578af567754bcd12c63a350c06afdd1bfec05 $
 */
@ProviderType
public interface BundleRequirement extends Requirement {
	/**
	 * Returns the bundle revision declaring this requirement.
	 * 
	 * @return The bundle revision declaring this requirement.
	 */
	BundleRevision getRevision();

	/**
	 * Returns whether the specified capability matches this requirement.
	 * 
	 * @param capability The capability to match to this requirement.
	 * @return {@code true} if the specified capability has the same
	 *         {@link #getNamespace() namespace} as this requirement and the
	 *         filter for this requirement matches the
	 *         {@link BundleCapability#getAttributes() attributes of the
	 *         specified capability}; {@code false} otherwise.
	 */
	boolean matches(BundleCapability capability);

	/**
	 * Returns the namespace of this requirement.
	 * 
	 * @return The namespace of this requirement.
	 */
	String getNamespace();

	/**
	 * Returns the directives of this requirement.
	 * 
	 * <p>
	 * All requirement directives not specified by the
	 * {@link AbstractWiringNamespace wiring namespaces} have no specified
	 * semantics and are considered extra user defined information.
	 * 
	 * @return An unmodifiable map of directive names to directive values for
	 *         this requirement, or an empty map if this requirement has no
	 *         directives.
	 */
	Map<String, String> getDirectives();

	/**
	 * Returns the attributes of this requirement.
	 * 
	 * <p>
	 * Requirement attributes have no specified semantics and are considered
	 * extra user defined information.
	 * 
	 * @return An unmodifiable map of attribute names to attribute values for
	 *         this requirement, or an empty map if this requirement has no
	 *         attributes.
	 */
	Map<String, Object> getAttributes();

	/**
	 * Returns the resource declaring this requirement.
	 * 
	 * <p>
	 * This method returns the same value as {@link #getRevision()}.
	 * 
	 * @return The resource declaring this requirement. This can be {@code null}
	 *         if this requirement is synthesized.
	 * @since 1.1
	 */
	BundleRevision getResource();
}
