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
 * An functional interface with a single convert method that is passed the
 * object to convert. This interface implements the {@link ConvertFunction}
 * interface via a default method and can be used in cases where the target type
 * does not need to be passed to the {@link #convert} method, for example in
 * Lambda expressions.
 * 
 * @param <F> Type parameter for the source object.
 * @param <T> Type parameter for the converted object.
 * @author $Id$
 */
@FunctionalInterface
public interface SimpleConvertFunction<F, T> extends ConvertFunction<F,T> {
	@Override
	default T convert(F obj, Type type) throws Exception {
		return convert(obj);
	}

	/**
	 * Convert the object into the target type.
	 * 
	 * @param obj The object to be converted.
	 * @return The converted object or {@link #CANNOT_CONVERT} to indicate that
	 *         this converter cannot handle the conversion.
	 */
	T convert(F obj) throws Exception;
}
