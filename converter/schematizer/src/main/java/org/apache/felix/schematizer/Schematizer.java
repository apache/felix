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
package org.apache.felix.schematizer;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.TypeReference;

@ProviderType
public interface Schematizer {
    /**
     * Can be a class or a TypeReference.
     * 
     * TODO: Consider splitting into two separate methods.
     */
    Schematizer schematize(String schemaName, Object type);

    Schema get(String schemaName);

    /**
     * Associates a type rule. Useful for classes that have type parameters, which
     * get lost at runtime.
     * 
     * @param name the name of the Schema to which this rule is to be associated
     * @param path the path in the object graph where the rule gets applied
     * @param type the type
     */
    Schematizer type(String name, String path, TypeReference<?> type);

    /**
     * Associates a type rule. Useful for classes that have type parameters, which
     * get lost at runtime.
     * 
     * @param name the name of the Schema to which this rule is to be associated
     * @param path the path in the object graph where the rule gets applied
     * @param type the type
     */
    Schematizer type(String name, String path, Class<?> type);

    /**
     * Returns a Converter for the Schema corresponding to the given name.
     * The Schema must already have been schematized using the given name.
     */
    Converter converterFor(String schemaName);

//    /**
//     * Returns a Converter for the provided Schema.
//     */
//    Converter converterFor(Schema s);
}
