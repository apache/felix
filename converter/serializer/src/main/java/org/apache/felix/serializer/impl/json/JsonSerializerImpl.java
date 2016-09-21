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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.converter.Converter;
import org.osgi.service.converter.StandardConverter;
import org.osgi.service.converter.TypeReference;
import org.osgi.service.serializer.Deserializing;
import org.osgi.service.serializer.Serializer;
import org.osgi.service.serializer.Serializing;

public class JsonSerializerImpl implements Serializer {
    private final Map<String, Object> configuration = new ConcurrentHashMap<>();
    private final ThreadLocal<Boolean> threadLocal = new ThreadLocal<>();
    private final Converter converter = new StandardConverter();

    @Override
    public <T> Deserializing<T> deserialize(Class<T> cls) {
        return new JsonDeserializingImpl<T>(converter, cls);
    }

    @Override
    public Serializing serialize(Object obj) {
        Serializing encoding = new JsonSerializingImpl(converter, configuration, obj);

        if (pretty()) {
            Boolean top = threadLocal.get();
            if (top == null) {
                threadLocal.set(Boolean.TRUE);

                // TODO implement this properly, the following it just a dev temp thing
                encoding = new EncodingWrapper("{}{}{}{}{}", encoding, "{}{}{}{}{}");
            }
        }
        return encoding;
    }

    private boolean pretty() {
        return Boolean.TRUE.equals(Boolean.parseBoolean((String) configuration.get("pretty")));
    }

    private class EncodingWrapper implements Serializing {
        private final Serializing delegate;
        private String prefix;
        private String postfix;

        EncodingWrapper(String pre, Serializing encoding, String post) {
            prefix = pre;
            delegate = encoding;
            postfix = post;
        }

        @Override
        public void to(OutputStream os) {
            try {
                os.write(toString().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String toString() {
            try {
                return prefix + delegate.toString() + postfix;
            } finally {
                threadLocal.set(null);
            }
        }

        @Override
        public Serializing ignoreNull() {
            return this;
        }

        @Override
        public Serializing pretty() {
            return this;
        }

        @Override
        public void to(OutputStream out, Charset charset) {
            // TODO Auto-generated method stub

        }

        @Override
        public Appendable to(Appendable out) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Serializing with(Converter converter) {
            delegate.with(converter);
            return this;
        }
    }

    @Override
    public <T> Deserializing<T> deserialize(TypeReference<T> ref) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Deserializing<?> deserialize(Type type) {
        // TODO Auto-generated method stub
        return null;
    }
}
