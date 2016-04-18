/*
 * Copyright (c) OSGi Alliance (2016). All Rights Reserved.
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

import java.lang.reflect.Type;

/**
 * This interface is used to specify the target that an object should be
 * converted to. A {@link Converting} instance can be obtained via the
 * {@link Converter} service by starting a conversion for a specific object.
 *
 * @author $Id:$
 */
public interface Converting {
	/**
	 * Specify the target object type for the conversion as a class object.
	 *
	 * @param cls The class to convert to.
	 * @return The converted object.
	 */
    @SuppressWarnings("unchecked")
    default <T> T to(Class<T> cls) {
        Type type = cls;
        return (T) to(type);
    }

	/**
	 * Specify the target object type as a {@link TypeReference}. If the target
	 * class carries generics information a TypeReference should be used as this
	 * preserves the generic information whereas a Class object has this
	 * information erased. Example use:
	 *
	 * <pre>
	 *  List<String> result =
	 *      converter.convert(Arrays.asList(1,2,3)).
	 *          to(new TypeReference&lt;List&lt;String&gt;&gt;() {});
	 * </pre>
	 *
	 * @param ref A type reference to the object being converted to.
	 * @return The converted object.
	 */
	@SuppressWarnings("unchecked")
    default <T> T to(TypeReference<T> ref) {
        return (T) to(ref.getType());
	}

	/**
	 * Specify the target object type as a Java Refelection Type object.
	 *
	 * @param type A Type object to represent the target type to be converted
	 *        to.
	 * @return The converted object.
	 */
	Object to(Type type);

	/**
	 * Same as to(String.class)
	 * @return The converted object.
	 */
    @Override
    String toString();
}
