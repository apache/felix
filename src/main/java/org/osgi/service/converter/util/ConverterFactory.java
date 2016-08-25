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
package org.osgi.service.converter.util;

import org.apache.felix.converter.impl.ConverterService;
import org.osgi.service.converter.Converter;

/**
 * Static factory class for obtaining a converter. A converter is normally
 * obtained from the Service Registry.
 */
public class ConverterFactory {
	/**
	 * Return the standard converter
	 *
	 * @return The converter.
	 */
    public static Converter standardConverter() {
        // Implementations must replace this class to return the actual
        // implementation.
	    return new ConverterService();
	}
}
