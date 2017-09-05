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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.felix.serializer.Writer;
import org.osgi.dto.DTO;
import org.osgi.util.converter.Converter;

public class DebugJsonWriter implements Writer {

    private Converter converter;
    private final Map<String, List<String>> mapOrderingRules;
    private final Map<String, Comparator<?>> arrayOrderingRules;
    private final boolean ignoreNull = false;
    private final int indentation = 2;

    public DebugJsonWriter(Converter c, Map<String,List<String>> mapRules, Map<String, Comparator<?>> arrayRules) {
        converter = c;
        mapOrderingRules = mapRules;
        arrayOrderingRules = arrayRules;
    }

    @Override
    public String write(Object obj) {
        return encode(obj, "/", 0).trim();
    }

    @Override
    public Map<String, List<String>> mapOrderingRules() {
        return mapOrderingRules;
    }

    @Override
    public Map<String, Comparator<?>> arrayOrderingRules() {
        return arrayOrderingRules;
    }

    @SuppressWarnings("rawtypes")
    private String encode(Object obj, String path, int level) {
        if (obj == null) {
            return ignoreNull ? "" : "null";
        }

        if (obj instanceof String) {
            return "\"" + (String)obj + "\"";
        } else if (obj instanceof Map) {
            return encodeMap(orderMap((Map)obj, path), path, level);
        } else if (obj instanceof Collection) {
            return encodeCollection((Collection) obj, path, level);
        } else if (obj instanceof DTO) {
            Map converted = converter.convert(obj).sourceAsDTO().to(Map.class);
            return encodeMap(orderMap(converted, path), path, level);
        } else if (obj.getClass().isArray()) {
            return encodeCollection(asCollection(obj), path, level);
        } else if (obj instanceof Number) {
            return obj.toString();
        } else if (obj instanceof Boolean) {
            return obj.toString();
        }

        return "\"" + converter.convert(obj).to(String.class) + "\"";
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    private Map orderMap(Map unordered, String path) {
        Map ordered = (mapOrderingRules.containsKey(path)) ? new LinkedHashMap<>() : new TreeMap<>();
        List<String> keys = (mapOrderingRules.containsKey(path)) ? mapOrderingRules.get(path) : new ArrayList<>(unordered.keySet());
        for (String key : keys) {
            String itemPath = (path.endsWith("/")) ? path + key : path + "/" + key;
            Object value = unordered.get(key);
            if (value instanceof Map)
                ordered.put(key, orderMap((Map)value, itemPath));
            else if(value instanceof Collection)
                ordered.put(key, orderCollectionItems((Collection)value, itemPath));
            else
                ordered.put(key, value);
        }

        return ordered;
    }

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    private List orderCollectionItems(Collection unordered, String path) {
        List ordered = new ArrayList<>();
        for (Object obj: unordered) {
            if (obj instanceof Map)
                ordered.add(orderMap((Map)obj, path));
            else if(obj instanceof Collection)
                ordered.add(orderCollectionItems((Collection)obj, path));
            else
                ordered.add(obj);
        }

        if (arrayOrderingRules.containsKey(path)) {
            Comparator c = arrayOrderingRules.get(path);
            if (c == null)
                Collections.sort(ordered);
            else
                Collections.sort(ordered,c);
        }

        return ordered;
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

    private String encodeCollection(Collection<?> collection, String path, int level) {
        level++;
        StringBuilder sb = new StringBuilder("[\n");

        boolean first = true;
        for (Object o : collection) {
            if (first)
                first = false;
            else
                sb.append(",\n");

            sb.append(getIdentPrefix(level));
            sb.append(encode(o, path, level));
        }

        sb.append("\n");
        sb.append( getIdentPrefix(--level));
        sb.append("]");
        return sb.toString();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private String encodeMap(Map m, String path, int level) {
        level++;
        StringBuilder sb = new StringBuilder("{\n");
        for (Entry entry : (Set<Entry>) m.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null)
                if (ignoreNull)
                    continue;

            String itemPath = (path.endsWith("/")) ? path + entry.getKey() : path + "/" + entry.getKey();
            if (sb.length() > 2)
                sb.append(",\n");
            sb.append(getIdentPrefix(level));
            sb.append('"');
            sb.append(entry.getKey().toString());
            sb.append("\":");
            sb.append(encode(entry.getValue(), itemPath, level));
        }
        sb.append("\n");
        sb.append(getIdentPrefix(--level));
        sb.append("}");

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
