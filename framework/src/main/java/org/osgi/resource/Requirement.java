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
 * A requirement that has been declared from a {@link Resource} .
 * 
 * <p>
 * Instances of this type must be <i>effectively immutable</i>. That is, for a
 * given instance of this interface, the methods defined by this interface must
 * always return the same result.
 * 
 * @ThreadSafe
 * @author $Id: 252221aeaaf9b189eef6e0c3798d0076a0032242 $
 */
@ConsumerType
public interface Requirement {
	/**
	 * Returns the namespace of this requirement.
	 * 
	 * @return The namespace of this requirement.
	 */
	String getNamespace();

	/**
	 * Returns the directives of this requirement.
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
	 * @return The resource declaring this requirement. This can be {@code null}
	 *         if this requirement is synthesized.
	 */
	Resource getResource();

	/**
	 * Compares this {@code Requirement} to another {@code Requirement}.
	 * 
	 * <p>
	 * This {@code Requirement} is equal to another {@code Requirement} if they
	 * have the same namespace, directives and attributes and are declared by
	 * the same resource.
	 * 
	 * @param obj The object to compare against this {@code Requirement}.
	 * @return {@code true} if this {@code Requirement} is equal to the other
	 *         object; {@code false} otherwise.
	 */
	boolean equals(Object obj);

	/**
	 * Returns the hashCode of this {@code Requirement}.
	 * 
	 * @return The hashCode of this {@code Requirement}.
	 */
	int hashCode();
}
