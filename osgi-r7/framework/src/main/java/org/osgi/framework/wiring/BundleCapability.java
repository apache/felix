/*
 * Copyright (c) OSGi Alliance (2010, 2015). All Rights Reserved.
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
import org.osgi.resource.Capability;

/**
 * A capability that has been declared from a {@link BundleRevision bundle
 * revision}.
 * 
 * @ThreadSafe
 * @author $Id: b4eb0df09a8354afdb46f013926f81f70f817f43 $
 */
@ProviderType
public interface BundleCapability extends Capability {

	/**
	 * Returns the bundle revision declaring this capability.
	 * 
	 * @return The bundle revision declaring this capability.
	 */
	BundleRevision getRevision();

	/**
	 * Returns the namespace of this capability.
	 * 
	 * @return The namespace of this capability.
	 */
	@Override
	String getNamespace();

	/**
	 * Returns the directives of this capability.
	 * 
	 * <p>
	 * All capability directives not specified by the
	 * {@link AbstractWiringNamespace wiring namespaces} have no specified
	 * semantics and are considered extra user defined information.
	 * 
	 * @return An unmodifiable map of directive names to directive values for
	 *         this capability, or an empty map if this capability has no
	 *         directives.
	 */
	@Override
	Map<String, String> getDirectives();

	/**
	 * Returns the attributes of this capability.
	 * 
	 * @return An unmodifiable map of attribute names to attribute values for
	 *         this capability, or an empty map if this capability has no
	 *         attributes.
	 */
	@Override
	Map<String, Object> getAttributes();

	/**
	 * Returns the resource declaring this capability.
	 * 
	 * <p>
	 * This method returns the same value as {@link #getRevision()}.
	 * 
	 * @return The resource declaring this capability.
	 * @since 1.1
	 */
	@Override
	BundleRevision getResource();
}
