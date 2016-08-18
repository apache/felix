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
package org.apache.felix.converter.impl.json;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.converter.impl.ConverterImpl;
import org.osgi.service.converter.Codec;
import org.osgi.service.converter.Converter;
import org.osgi.service.converter.Decoding;
import org.osgi.service.converter.Encoding;
import org.osgi.service.converter.TypeReference;

public class JsonCodecImpl implements Codec {
    private Map<String, Object> configuration = new ConcurrentHashMap<>();
    private ThreadLocal<Boolean> threadLocal = new ThreadLocal<>();
    private Converter converter = new ConverterImpl();

    @Override
    public Codec with(Converter c) {
        converter = c;
        return this;
    }

    @Override
    public <T> Decoding<T> decode(Class<T> cls) {
        return new JsonDecodingImpl<T>(converter, cls);
    }

    @Override
    public Encoding encode(Object obj) {
        Encoding encoding = new JsonEncodingImpl(converter, configuration, obj);

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

    private class EncodingWrapper implements Encoding {
        private final Encoding delegate;
        private String prefix;
        private String postfix;

        EncodingWrapper(String pre, Encoding encoding, String post) {
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
        public Encoding ignoreNull() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Encoding pretty() {
            // TODO Auto-generated method stub
            return null;
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
}
