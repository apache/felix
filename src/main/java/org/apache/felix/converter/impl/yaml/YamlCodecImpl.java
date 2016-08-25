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
package org.apache.felix.converter.impl.yaml;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.converter.impl.ConverterImpl;
import org.osgi.service.converter.Codec;
import org.osgi.service.converter.Converter;
import org.osgi.service.converter.Decoding;
import org.osgi.service.converter.Encoding;
import org.osgi.service.converter.TypeReference;

public class YamlCodecImpl implements Codec {
    private Map<String, Object> configuration = new ConcurrentHashMap<>();
    private Converter converter = new ConverterImpl();

    @Override
    public Codec with(Converter c) {
        converter = c;
        return this;
    }

    @Override
    public <T> Decoding<T> decode(Class<T> cls) {
        return new YamlDecodingImpl<T>(converter, cls);
    }

    @Override
    public <T> Decoding<T> decode(TypeReference<T> ref) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Decoding<?> decode(Type type) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Encoding encode(Object obj) {
        return new YamlEncodingImpl(converter, configuration, obj);
    }
}
