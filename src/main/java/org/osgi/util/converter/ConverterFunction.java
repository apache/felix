/*
 * Copyright (c) OSGi Alliance (2017). All Rights Reserved.
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
package org.osgi.util.converter;

import java.lang.reflect.Type;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * An functional interface with a convert method that is passed the original
 * object and the target type to perform a custom conversion.
 * <P>
 * This interface can also be used to register a custom error handler.
 *
 * @author $Id$
 */
@ConsumerType
public interface ConverterFunction {
	/**
	 * Special object to indicate that a custom converter rule or error handler
	 * cannot handle the conversion.
	 */
	static final Object CANNOT_HANDLE = new Object();

	/**
	 * Convert the object into the target type.
	 *
	 * @param obj The object to be converted. This object will never be
	 *            {@code null} as the convert function will not be invoked for
	 *            null values.
	 * @param targetType The target type.
	 * @return The conversion result or {@link #CANNOT_HANDLE} to indicate that
	 *         the convert function cannot handle this conversion. In this case
	 *         the next matching rule or parent converter will be given a
	 *         opportunity to convert.
	 * @throws Exception the operation can throw an exception if the conversion
	 *             can not be performed due to incompatible types.
	 */
	Object apply(Object obj, Type targetType) throws Exception;
}
