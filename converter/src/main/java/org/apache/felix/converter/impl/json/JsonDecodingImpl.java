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

import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.converter.Converter;
import org.osgi.service.converter.Decoding;

public class JsonDecodingImpl<T> implements Decoding<T> {
    private final Class<T> clazz;
    private final Converter converter;

    public JsonDecodingImpl(Converter c, Class<T> cls) {
        converter = c;
        clazz = cls;
    }

    @Override
    public T from(CharSequence in) {
        if (Map.class.isAssignableFrom(clazz)) {
            return createMapFromJSONString(in);
        }
        return deserializeSingleJSONValue(clazz, in);
    }

    private T createMapFromJSONString(CharSequence in) {
        Map m = new HashMap();
        String s = in.toString().trim();
        if (!s.startsWith("{") || !s.endsWith("}"))
            throw new IllegalArgumentException("JSON Should start and end with '{' and '}': " + s);

        // Eat braces
        s = s.substring(1, s.length() - 1);

        int commaIdx = -1;
        do {
            int colonIdx = s.indexOf(':');
            if (colonIdx <= 0)
                throw new IllegalArgumentException("JSON Should contain key-value pairs: " + s);

            String key = s.substring(0, colonIdx).trim();
            if (!key.startsWith("\"") || !key.endsWith("\""))
                throw new IllegalArgumentException("JSON key should be double-quoted: " + s);
            key = key.substring(1, key.length() - 1);

            // move to after ':'
            s = s.substring(colonIdx + 1);

            commaIdx = getNextComma(s);
            String val;
            if (commaIdx > 0) {
                val = s.substring(0, commaIdx);

                // move to after ','
                s = s.substring(commaIdx + 1);
            } else {
                val = s;
            }


            val = val.trim();
            Object parsed;
            if (val.startsWith("{")) {
                parsed = new JsonCodecImpl().decode(Map.class).from(val);
            } else {
                if ("null".equals(val))
                    parsed = null;
                else if ("true".equals(val))
                    parsed = true;
                else if ("false".equals(val))
                    parsed = false;
                else if (val.startsWith("\"") && val.endsWith("\""))
                    parsed = val.substring(1, val.length() - 1);
                else if (val.contains("."))
                    parsed = Double.valueOf(val);
                else
                    parsed = Long.valueOf(val);
            }
            m.put(key, parsed);
        } while (commaIdx > 0);

        return (T) m;
    }

    private int getNextComma(String s) {
        int bracelevel = 0;
        for (int i=0; i<s.length(); i++) {
            switch(s.charAt(i)) {
            case '{': bracelevel++;
                break;
            case '}': bracelevel--;
                break;
            case ',': if (bracelevel == 0) return i;
                break;
            }
        }
        return -1;
    }

    @SuppressWarnings("unchecked")
    private <T> T deserializeSingleJSONValue(Class<T> cls, CharSequence cs) {
        try {
            Method m = cls.getDeclaredMethod("valueOf", String.class);
            if (m != null) {
                return (T) m.invoke(null, cs);
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    @Override
    public T from(InputStream in) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public T from(InputStream in, Charset charset) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public T from(Readable in) {
        // TODO Auto-generated method stub
        return null;
    }
}
