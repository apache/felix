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

/**
 * A function that can throw an exception.
 *
 * @param <T> The type of the input to the function.
 * @param <R> The type of the result of the function.
 */
@FunctionalInterface
public interface FunctionThrowsException<T, R> {
    /**
	 * Applies this function to the argument.
	 *
	 * @param t The function argument
	 * @return The function result
	 * @throws Exception An exception can be thrown by the function.
	 */
    R apply(T t) throws Exception;
}
