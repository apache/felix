/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.serializer.impl.yaml;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.serializer.Writer;
import org.apache.felix.serializer.WriterFactory;
import org.osgi.util.converter.Converter;

public class YamlWriterFactory implements WriterFactory, WriterFactory.YamlWriterFactory {
    private final Map<String, List<String>> orderingRules = new HashMap<>();

    @Override
    public YamlWriterFactory orderMap(String path, List<String> keyOrder) {
        orderingRules.put(path,keyOrder);
        return this;
    }

    @Override
    public WriterFactory orderMap(Map<String, List<String>> rules) {
        orderingRules.putAll(rules);
        return this;
    }

    @Override
    public WriterFactory orderArray(String path) {
        return this;
    }

    @Override
    public WriterFactory orderArray(String path, Comparator<?> comparator) {
        return this;
    }

    @Override
    public Writer newDefaultWriter(Converter c) {
        return new DefaultYamlWriter(c);
    }

    @Override
    public Writer newDebugWriter(Converter c) {
        return new DefaultYamlWriter(c);
    }
}
