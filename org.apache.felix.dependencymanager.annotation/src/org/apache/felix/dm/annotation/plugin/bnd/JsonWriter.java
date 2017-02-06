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
package org.apache.felix.dm.annotation.plugin.bnd;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;

/**
 * Class used to serialize a map into the json format.
 * Code adapted from Apache Felix Converter (org.apache.felix.serializer.impl.json.JsonSerializingImpl)
 * 
 * The JSON output is parsed into an object structure in the following way:
 * <ul>
 * <li>Object names are represented as a {@link String}.
 * <li>String values are represented as a {@link String}.
 * <li>Numeric values without a decimal separator are represented as a {@link Long}.
 * <li>Numeric values with a decimal separator are represented as a {@link Double}.
 * <li>Boolean values are represented as a {@link Boolean}.
 * <li>Nested JSON objects are parsed into a {@link java.util.Map Map&lt;String, Object&gt;}.
 * <li>JSON lists are parsed into a {@link java.util.List} which may contain any of the above values.
 * </ul>
 */
public class JsonWriter {
    private final Object object;
    private final boolean ignoreNull;
    private final Function<Object, String> converter;
    
    JsonWriter(Object object) {
    	this(object, null, false);
    }
    
    JsonWriter(Object object, Function<Object, String> converter, boolean ignoreNull) {
    	this.object = object;
    	this.converter = converter;
    	this.ignoreNull = ignoreNull;
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
        } else if (obj.getClass().isArray()) {
            return encodeCollection(asCollection(obj));
        } else if (obj instanceof Number) {
            return obj.toString();
        } else if (obj instanceof Boolean) {
            return obj.toString();
        }

        String result = (converter != null) ? converter.apply(obj) : obj.toString();        
        return "\"" + result + "\"";
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
