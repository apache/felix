/*
 * Copyright (c) OSGi Alliance (2015, 2016). All Rights Reserved.
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
package org.osgi.service.converter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * An object does not carry any runtime information about its generic type.
 * However sometimes it is necessary to specify a generic type, that is the
 * purpose of this class. It allows you to specify an generic type by defining a
 * type T, then subclassing it. The subclass will have a reference to the super
 * class that contains this generic information. Through reflection, we pick
 * this reference up and return it with the getType() call.
 *
 * <pre>
 * List&lt;String&gt; result = converter.convert(Arrays.asList(1, 2, 3))
 * 		.to(new TypeReference&lt;List&lt;String&gt;&gt;() {});
 * </pre>
 *
 * @param <T> The target type for the conversion.
 * @author $Id$
 * @Immutable
 */
@ConsumerType
public class TypeReference<T> {
	/**
	 * A {@link TypeReference} cannot be directly instantiated. To use it, it
	 * has to be extended, typically as an anonymous inner class.
	 */
	protected TypeReference() {}

	/**
	 * Return the actual type of this Type Reference
	 *
	 * @return the type of this reference.
	 */
	public Type getType() {
		return ((ParameterizedType) getClass().getGenericSuperclass())
				.getActualTypeArguments()[0];
	}
}
