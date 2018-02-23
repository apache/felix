/*
 * Copyright (c) OSGi Alliance (2011, 2015). All Rights Reserved.
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

import org.osgi.annotation.versioning.ConsumerType;

/**
 * A wire connecting a {@link Capability} to a {@link Requirement}.
 * 
 * <p>
 * Instances of this type must be <i>effectively immutable</i>. That is, for a
 * given instance of this interface, the methods defined by this interface must
 * always return the same result.
 * 
 * @ThreadSafe
 * @author $Id: a0d1c39e16788708ffab9bb00707554f97f09998 $
 */
@ConsumerType
public interface Wire {
	/**
	 * Returns the {@link Capability} for this wire.
	 * 
	 * @return The {@link Capability} for this wire.
	 */
	Capability getCapability();

	/**
	 * Returns the {@link Requirement} for this wire.
	 * 
	 * @return The {@link Requirement} for this wire.
	 */
	Requirement getRequirement();

	/**
	 * Returns the resource providing the {@link #getCapability() capability}.
	 * 
	 * <p>
	 * The returned resource may differ from the resource referenced by the
	 * {@link #getCapability() capability}.
	 * 
	 * @return The resource providing the capability.
	 */
	Resource getProvider();

	/**
	 * Returns the resource who {@link #getRequirement() requires} the
	 * {@link #getCapability() capability}.
	 * 
	 * <p>
	 * The returned resource may differ from the resource referenced by the
	 * {@link #getRequirement() requirement}.
	 * 
	 * @return The resource who requires the capability.
	 */
	Resource getRequirer();

	/**
	 * Compares this {@code Wire} to another {@code Wire}.
	 * 
	 * <p>
	 * This {@code Wire} is equal to another {@code Wire} if they have the same
	 * capability, requirement, provider and requirer.
	 * 
	 * @param obj The object to compare against this {@code Wire}.
	 * @return {@code true} if this {@code Wire} is equal to the other object;
	 *         {@code false} otherwise.
	 */
	@Override
	boolean equals(Object obj);

	/**
	 * Returns the hashCode of this {@code Wire}.
	 * 
	 * @return The hashCode of this {@code Wire}.
	 */
	@Override
	int hashCode();
}
