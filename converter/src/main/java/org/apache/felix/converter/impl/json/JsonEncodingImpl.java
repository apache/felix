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
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.osgi.dto.DTO;
import org.osgi.service.converter.ConversionException;
import org.osgi.service.converter.Converter;
import org.osgi.service.converter.Encoding;

public class JsonEncodingImpl implements Encoding {
    private final Converter converter;
    private final Map<String, Object> configuration;
    private final Object object;
    private final boolean ignoreNull;

    JsonEncodingImpl(Converter c, Map<String, Object> cfg, Object obj) {
        converter = c;
        configuration = cfg;
        ignoreNull = Boolean.TRUE.equals(Boolean.parseBoolean((String) configuration.get("ignoreNull")));
        object = obj;
    }

    @Override
    public Appendable to(Appendable out) {
        try {
            out.append(encode(object));
            return out;
        } catch (IOException e) {
            throw new ConversionException("Problem converting to JSON", e);
        }
    }

    @Override
    public void to(OutputStream os, Charset charset) {
        try {
            os.write(encode(object).getBytes(charset));
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
        return encode(object);
    }

    @SuppressWarnings("rawtypes")
    private String encode(Object obj) {
        if (obj == null) {
            return ignoreNull ? "" : "null";
        }

        if (obj instanceof Map) {
            return encodeMap((Map) obj);
        } else if (obj instanceof Collection) {
            return encodeCollection((Collection) obj);
        } else if (obj instanceof DTO) {
            return encodeMap(converter.convert(obj).to(Map.class));
        } else if (obj.getClass().isArray()) {
            return encodeCollection(asCollection(obj));
        } else if (obj instanceof Number) {
            return obj.toString();
        } else if (obj instanceof Boolean) {
            return obj.toString();
        }

        return "\"" + converter.convert(obj).to(String.class) + "\"";
    }

    private Collection<?> asCollection(Object arr) {
        // Arrays.asList() doesn't work for primitive arrays
        int len = Array.getLength(arr);
        List<Object> l = new ArrayList<>(len);
        for (int i=0; i<len; i++) {
            l.add(Array.get(arr, i));
        }
        return l;
    }

    private String encodeCollection(Collection<?> collection) {
        StringBuilder sb = new StringBuilder("[");

        boolean first = true;
        for (Object o : collection) {
            if (first)
                first = false;
            else
                sb.append(',');

            sb.append(encode(o));
        }

        sb.append("]");
        return sb.toString();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private String encodeMap(Map m) {
        StringBuilder sb = new StringBuilder("{");
        for (Entry entry : (Set<Entry>) m.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null)
                if (ignoreNull)
                    continue;

            if (sb.length() > 1)
                sb.append(',');
            sb.append('"');
            sb.append(entry.getKey().toString());
            sb.append("\":");
            sb.append(encode(entry.getValue()));
        }
        sb.append("}");

        return sb.toString();
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

}
