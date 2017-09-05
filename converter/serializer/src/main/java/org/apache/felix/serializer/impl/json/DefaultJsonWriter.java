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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.felix.serializer.Writer;
import org.osgi.dto.DTO;
import org.osgi.util.converter.Converter;

public class DefaultJsonWriter implements Writer {

    private final Converter converter;
    private final boolean ignoreNull = false;

    public DefaultJsonWriter(Converter c) {
        converter = c;
    }

    @Override
    public String write(Object obj) {
        return encode(obj);
    }

    @Override
    public Map<String, List<String>> mapOrderingRules() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Comparator<?>> arrayOrderingRules() {
        return Collections.emptyMap();
    }

    @SuppressWarnings("rawtypes")
    private String encode(Object obj) {
        if (obj == null) {
            return ignoreNull ? "" : "null";
        }

        if (obj instanceof String) {
            return "\"" + (String)obj + "\"";
        } else if (obj instanceof Map) {
            return encodeMap((Map) obj);
        } else if (obj instanceof Collection) {
            return encodeCollection((Collection) obj);
        } else if (obj instanceof DTO) {
            return encodeMap(converter.convert(obj).sourceAsDTO().to(Map.class));
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
}
