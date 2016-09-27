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

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.converter.Converter;
import org.osgi.service.converter.StandardConverter;
import org.osgi.service.converter.TypeReference;
import org.osgi.service.serializer.Deserializing;
import org.osgi.service.serializer.Serializer;
import org.osgi.service.serializer.Serializing;

public class YamlSerializerImpl implements Serializer {
    private final Map<String, Object> configuration = new ConcurrentHashMap<>();
    private final Converter converter = new StandardConverter();

    @Override
    public <T> Deserializing<T> deserialize(Class<T> cls) {
        return new YamlDeserializingImpl<T>(converter, cls);
    }

    @Override
    public <T> Deserializing<T> deserialize(TypeReference<T> ref) {
        return new YamlDeserializingImpl<T>(converter, ref.getType());
    }

    @Override @SuppressWarnings("rawtypes")
    public Deserializing<?> deserialize(Type type) {
        return new YamlDeserializingImpl(converter, type);
    }

    @Override
    public Serializing serialize(Object obj) {
        return new YamlSerializingImpl(converter, configuration, obj);
    }
}
