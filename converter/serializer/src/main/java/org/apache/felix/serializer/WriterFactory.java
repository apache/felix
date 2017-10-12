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
package org.apache.felix.serializer;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.util.converter.Converter;

/**
 * A factory to create a writer with the desired behaviour.
 *
 * @author $Id:$
 */
@ProviderType
public interface WriterFactory {

    /**
     * A default Writer, but which will use a custom Converter.
     *  
     * @param c the custom Converter that will be used by the Writer
     * @return a default Writer, but one which will use a custom Converter
     */
    Writer newDefaultWriter(Converter c);

    /**
     * A writer that is useful for debugging. This write outputs "pretty"
     * format.
     *
     * @param c the custom Converter that will be used by the Writer
     * @return A new writer useful for debugging
     */
    Writer newDebugWriter(Converter c);

    /**
     * Register an ordering rule for this writer.
     * 
     * An ordering rule causes the written json to be output in the order
     * specified. This can be useful, for example, for debugging or when
     * the data otherwise needs to be human consumable.
     * 
     * This rule only affects map-type objects located at the given path.
     *
     * @param path the path where the key is located in the object graph.
     * @param keyOrder A list with the desired key order.
     * @return This factory object to allow further invocations on it.
     */
    WriterFactory orderMap(String path, List<String> keyOrder);

    /**
     * Register multiple ordering rules for this writer.
     * 
     * An ordering rule causes the written json to be output in the order
     * specified. This can be useful, for example, for debugging or when
     * the data otherwise needs to be human consumable.
     * 
     * This rule only affects map-type objects located at the given path.
     * 
     * Each map entry is a path/keyOrder pair, the same as if calling
     * WriterFactory.orderMap(String,List<String>).
     *
     * @param orderingRules the rules to be added
     * @return This factory object to allow further invocations on it.
     */
    WriterFactory orderMap( Map<String, List<String>> orderingRules );

    /**
     * Register an ordering rule for this writer.
     * 
     * An ordering rule causes the written json to be output in the order
     * specified. This can be useful, for example, for debugging or when
     * the data otherwise needs to be human consumable.
     * 
     * This rule only affects array-type objects located at the given path.
     *
     * @param path the path where the key is located in the object graph.
     * @return This factory object to allow further invocations on it.
     */
    WriterFactory orderArray(String path);

    /**
     * Register an ordering rule for this writer.
     * 
     * An ordering rule causes the written json to be output in the order
     * specified. This can be useful, for example, for debugging or when
     * the data otherwise needs to be human consumable.
     * 
     * This rule only affects array-type objects located at the given path.
     *
     * @param path the path where the key is located in the object graph.
     * @param comparator A comparator that will be used to sort the items in the array.
     * @return This factory object to allow further invocations on it.
     */
    WriterFactory orderArray(String path, Comparator<?> comparator);

    /**
     * A convenience means of obtaining a JsonWriterFactory without having to
     * configure service settings.
     */
    static interface JsonWriterFactory extends WriterFactory {}

    /**
     * A convenience means of obtaining a YamlWriterFactory without having to
     * configure service settings.
     */
    static interface YamlWriterFactory extends WriterFactory {}
}
