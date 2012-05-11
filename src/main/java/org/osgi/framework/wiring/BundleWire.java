/*
 * Copyright (c) OSGi Alliance (2011, 2012). All Rights Reserved.
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

import org.osgi.resource.Wire;

/**
 * A wire connecting a {@link BundleCapability} to a {@link BundleRequirement}.
 * 
 * @ThreadSafe
 * @noimplement
 * @version $Id: 02e7cd6ec0fa9fdb73f782a6890984d5d4e7ca21 $
 */
public interface BundleWire extends Wire {
	/**
	 * Returns the {@link BundleCapability} for this wire.
	 * 
	 * @return The {@link BundleCapability} for this wire.
	 */
	BundleCapability getCapability();

	/**
	 * Return the {@link BundleRequirement} for this wire.
	 * 
	 * @return The {@link BundleRequirement} for this wire.
	 */
	BundleRequirement getRequirement();

	/**
	 * Returns the bundle wiring {@link BundleWiring#getProvidedWires(String)
	 * providing} the {@link #getCapability() capability}.
	 * 
	 * <p>
	 * The bundle revision referenced by the returned bundle wiring may differ
	 * from the bundle revision referenced by the {@link #getCapability()
	 * capability}.
	 * 
	 * @return The bundle wiring providing the capability. If the bundle wiring
	 *         providing the capability is not {@link BundleWiring#isInUse() in
	 *         use}, {@code null} will be returned.
	 */
	BundleWiring getProviderWiring();

	/**
	 * Returns the bundle wiring who
	 * {@link BundleWiring#getRequiredWires(String) requires} the
	 * {@link #getCapability() capability}.
	 * 
	 * <p>
	 * The bundle revision referenced by the returned bundle wiring may differ
	 * from the bundle revision referenced by the {@link #getRequirement()
	 * requirement}.
	 * 
	 * @return The bundle wiring whose requirement is wired to the capability.
	 *         If the bundle wiring requiring the capability is not
	 *         {@link BundleWiring#isInUse() in use}, {@code null} will be
	 *         returned.
	 */
	BundleWiring getRequirerWiring();

	/**
	 * Returns the resource providing the {@link #getCapability() capability}.
	 * 
	 * <p>
	 * The returned resource may differ from the resource referenced by the
	 * {@link #getCapability() capability}.
	 * 
	 * <p>
	 * This method returns the same value as {@link #getProviderWiring()}.
	 * {@link BundleWiring#getRevision() getRevision()}.
	 * 
	 * @return The resource providing the capability.
	 * @since 1.1
	 */
	BundleRevision getProvider();

	/**
	 * Returns the resource who {@link #getRequirement() requires} the
	 * {@link #getCapability() capability}.
	 * 
	 * <p>
	 * The returned resource may differ from the resource referenced by the
	 * {@link #getRequirement() requirement}.
	 * 
	 * <p>
	 * This method returns the same value as {@link #getRequirerWiring()}.
	 * {@link BundleWiring#getRevision() getRevision()}.
	 * 
	 * @return The resource who requires the capability.
	 * @since 1.1
	 */
	BundleRevision getRequirer();

}
