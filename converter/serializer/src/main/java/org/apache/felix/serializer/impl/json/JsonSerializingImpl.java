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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.felix.serializer.Serializing;
import org.apache.felix.serializer.Writer;
import org.apache.felix.serializer.impl.AbstractSpecifying;
import org.osgi.util.converter.ConversionException;
import org.osgi.util.converter.Converter;

public class JsonSerializingImpl extends AbstractSpecifying<Serializing> implements Serializing {
    private volatile Converter converter;
    private volatile boolean useCustomWriter;
    private volatile Writer writer;
    private final Object object;

    JsonSerializingImpl(Converter c, Writer w, Object obj) {
        converter = c;
        writer = w;
        object = obj;
    }

    @Override
    public Appendable to(Appendable out) {
        try {
            out.append(writer.write(object));
            return out;
        } catch (IOException e) {
            throw new ConversionException("Problem converting to JSON", e);
        }
    }

    @Override
    public void to(OutputStream os, Charset charset) {
        try {
            os.write(writer.write(object).getBytes(charset));
        } catch (IOException e) {
            throw new ConversionException("Problem converting to JSON", e);
        }
    }

    @Override
    public void to(OutputStream out) throws IOException {
        to(out, StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return writer.write(object);
    }

    @Override
    public Serializing convertWith(Converter c) {
        converter = c;
        if (!useCustomWriter)
            writer = new DefaultJsonWriter(converter);
        return this;
    }

    @Override
    public Serializing writeWith(Writer w) {
        writer = w;
        return this;
    }
}
