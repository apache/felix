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
import org.osgi.util.converter.Converter;

public class DefaultYamlWriter implements Writer {

    private final Converter converter;
    private final int indentation = 2;

    public DefaultYamlWriter(Converter c) {
        converter = c;
    }

    @Override
    public String write(Object obj) {
        return encode(obj, 0).trim();
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
    private String encode(Object obj, int level) {
        if (obj == null)
            return "";

        if (obj instanceof Map) {
            return encodeMap((Map) obj, level);
        } else if (obj instanceof Collection) {
            return encodeCollection((Collection) obj, level);
        } else if (obj.getClass().isArray()) {
            return encodeCollection(asCollection(obj), level);
        } else if (obj instanceof Number)  {
            return obj.toString();
        } else if (obj instanceof Boolean) {
            return obj.toString();
        }

        return "'" + converter.convert(obj).to(String.class) + "'";
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

    private String encodeCollection(Collection<?> collection, int level) {
        StringBuilder sb = new StringBuilder();
        for (Object o : collection) {
            sb.append("\n");
            sb.append(getIdentPrefix(level));
            sb.append("- ");
            sb.append(encode(o, level + 1));
        }
        return sb.toString();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private String encodeMap(Map m, int level) {
        StringBuilder sb = new StringBuilder();
        for (Entry entry : (Set<Entry>) m.entrySet()) {
            sb.append("\n");
            sb.append(getIdentPrefix(level));
            sb.append(entry.getKey().toString());
            sb.append(": ");
            sb.append(encode(entry.getValue(), level + 1));
        }

        return sb.toString();
    }

    private String getIdentPrefix(int level) {
        int numSpaces = indentation * level;
        StringBuilder sb = new StringBuilder(numSpaces);
        for (int i=0; i < numSpaces; i++)
            sb.append(' ');
        return sb.toString();
    }
}
