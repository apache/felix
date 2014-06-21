/*
 * Copyright (c) OSGi Alliance (2014). All Rights Reserved.
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

package org.osgi.util.function;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * A function that accepts a single argument and produces a result.
 *
 * <p>
 * This is a functional interface and can be used as the assignment target for a
 * lambda expression or method reference.
 * 
 * @param <T> The type of the function input.
 * @param <R> The type of the function output.
 * 
 * @ThreadSafe
 * @author $Id: 5d812f75c0b4f88f01083189babb3ef7476b5ced $
 */
@ConsumerType
public interface Function<T, R> {
	/**
	 * Applies this function to the specified argument.
	 * 
	 * @param t The input to this function.
	 * @return The output of this function.
	 */
	R apply(T t);
}
