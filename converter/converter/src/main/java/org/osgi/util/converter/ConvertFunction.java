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
package org.osgi.util.converter;

import java.lang.reflect.Type;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * An functional interface with a single convert method that is passed the
 * original object and the target type.
 *
 * @param <F> Type parameter for the source object.
 * @param <T> Type parameter for the converted object.
 * @author $Id$
 */
@ConsumerType
@FunctionalInterface
public interface ConvertFunction<F, T> {
	/**
	 * Convert the object into the target type.
	 *
	 * @param obj The object to be converted. This object will never be
	 *            {@code null} as the convert function will not be invoked for
	 *            null values.
	 * @param targetType The target type.
	 * @return The conversion result or {@code null} to indicate that the
	 *         convert function cannot handle this conversion. In this case the
	 *         next matching rule or adapter will be given a opportunity to
	 *         convert.
	 * @throws Exception
	 */
	T convert(F obj, Type targetType) throws Exception;
}
