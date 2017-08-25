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
package org.apache.felix.serializer.impl.json;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.serializer.Writer;
import org.osgi.service.serializer.WriterFactory;
import org.osgi.util.converter.Converter;

public class JsonWriterFactory implements WriterFactory, WriterFactory.JsonWriterFactory {
    private final Map<String, List<String>> orderingRules = new HashMap<>();

    @Override
    public JsonWriterFactory orderBy(String path, List<String> keyOrder) {
        orderingRules.put(path, keyOrder);
        return this;
    }

    @Override
    public Writer newDefaultWriter(Converter c) {
        return new DefaultJsonWriter(c);
    }

    @Override
    public Writer newDebugWriter(Converter c) {
        return new DebugJsonWriter(c, orderingRules);
    }
}
