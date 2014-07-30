/*
 * Copyright (c) OSGi Alliance (2011, 2013). All Rights Reserved.
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

import java.util.Map;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * A capability that has been declared from a {@link Resource}.
 * 
 * <p>
 * Instances of this type must be <i>effectively immutable</i>. That is, for a
 * given instance of this interface, the methods defined by this interface must
 * always return the same result.
 * 
 * @ThreadSafe
 * @author $Id: e79d11402e14e170443c8a2a9da835391cd1ccc8 $
 */
@ConsumerType
public interface Capability {

	/**
	 * Returns the namespace of this capability.
	 * 
	 * @return The namespace of this capability.
	 */
	String getNamespace();

	/**
	 * Returns the directives of this capability.
	 * 
	 * @return An unmodifiable map of directive names to directive values for
	 *         this capability, or an empty map if this capability has no
	 *         directives.
	 */
	Map<String, String> getDirectives();

	/**
	 * Returns the attributes of this capability.
	 * 
	 * @return An unmodifiable map of attribute names to attribute values for
	 *         this capability, or an empty map if this capability has no
	 *         attributes.
	 */
	Map<String, Object> getAttributes();

	/**
	 * Returns the resource declaring this capability.
	 * 
	 * @return The resource declaring this capability.
	 */
	Resource getResource();

	/**
	 * Compares this {@code Capability} to another {@code Capability}.
	 * 
	 * <p>
	 * This {@code Capability} is equal to another {@code Capability} if they
	 * have the same namespace, directives and attributes and are declared by
	 * the same resource.
	 * 
	 * @param obj The object to compare against this {@code Capability}.
	 * @return {@code true} if this {@code Capability} is equal to the other
	 *         object; {@code false} otherwise.
	 */
	boolean equals(Object obj);

	/**
	 * Returns the hashCode of this {@code Capability}.
	 * 
	 * @return The hashCode of this {@code Capability}.
	 */
	int hashCode();
}
