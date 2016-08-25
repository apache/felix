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
 * This Runtime Exception is thrown when an object is requested to be converted
 * but the conversion cannot be done. For example when the String "test" is to
 * be converted into a Long.
 */
public class ConversionException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	/**
	 * Create a Conversion Exception with a message.
	 * 
	 * @param message The message for this exception.
	 */
	public ConversionException(String message) {
		super(message);
	}

	/**
	 * Create a Conversion Exception with a message and a nested cause.
	 * 
	 * @param message The message for this exception.
	 * @param cause The causing exception.
	 */
	public ConversionException(String message, Throwable cause) {
		super(message, cause);
	}
}
