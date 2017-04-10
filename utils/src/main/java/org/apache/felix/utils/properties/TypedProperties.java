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
package org.apache.felix.utils.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.felix.utils.properties.InterpolationHelper.substVars;

/**
 * <p>
 * Map to load / store / update untyped or typed properties.
 * The map is untyped if all properties are strings.
 * When this is the case, the properties are stored without
 * any encoding, else all properties are encoded using
 * the {@link ConfigurationHandler}.
 * </p>
 *
 * @author gnodet
 */
public class TypedProperties extends AbstractMap<String, Object> {

    public static final String ENV_PREFIX = "env:";

    private final Properties storage;
    private final SubstitutionCallback callback;
    private final boolean substitute;

    public TypedProperties() {
        this(null, true);
    }

    public TypedProperties(boolean substitute) {
        this(null, substitute);
    }

    public TypedProperties(SubstitutionCallback callback) {
        this(callback, true);
    }

    public TypedProperties(SubstitutionCallback callback, boolean substitute) {
        this.storage = new Properties(false);
        this.callback = callback;
        this.substitute = substitute;
    }

    public void load(File location) throws IOException {
        InputStream is = new FileInputStream(location);
        try {
            load(is);
        } finally {
            is.close();
        }
    }

    public void load(URL location) throws IOException {
        InputStream is = location.openStream();
        try {
            load(is);
        } finally {
            is.close();
        }
    }

    public void load(InputStream is) throws IOException {
        load(new InputStreamReader(is, Properties.DEFAULT_ENCODING));
    }

    public void load(Reader reader) throws IOException {
        storage.loadLayout(reader, true);
        substitute(callback);
    }

    public void save(File location) throws IOException {
        storage.save(location);
    }

    public void save(OutputStream os) throws IOException {
        storage.save(os);
    }

    public void save(Writer writer) throws IOException {
        storage.save(writer);
    }

    /**
     * Store a properties into a output stream, preserving comments, special character, etc.
     * This method is mainly to be compatible with the java.util.Properties class.
     *
     * @param os an output stream.
     * @param comment this parameter is ignored as this Properties
     * @throws IOException If storing fails
     */
    public void store(OutputStream os, String comment) throws IOException {
        storage.store(os, comment);
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return new AbstractSet<Entry<String, Object>>() {
            @Override
            public Iterator<Entry<String, Object>> iterator() {
                return new Iterator<Entry<String, Object>>() {
                    final Iterator<String> keyIterator = storage.keySet().iterator();
                    public boolean hasNext() {
                        return keyIterator.hasNext();
                    }
                    public Entry<String, Object> next() {
                        final String key = keyIterator.next();
                        return new Entry<String, Object>() {
                            public String getKey() {
                                return key;
                            }
                            public Object getValue() {
                                return TypedProperties.this.get(key);
                            }
                            public Object setValue(Object value) {
                                return TypedProperties.this.put(key, value);
                            }
                        };
                    }
                    public void remove() {
                        keyIterator.remove();
                    }
                };
            }

            @Override
            public int size() {
                return storage.size();
            }
        };
    }

    @Override
    public Object put(String key, Object value) {
        if (value instanceof String && !storage.typed) {
            return storage.put(key, (String) value);
        } else {
            ensureTyped();
            String old = storage.put(key, convertToString(value));
            return old != null ? convertFromString(old) : null;
        }
    }

    @Override
    public Object get(Object key) {
        String v = storage.get(key);
        return storage.typed && v != null ? convertFromString(v) : v;
    }

    public Object put(String key, List<String> commentLines, Object value) {
        if (value instanceof String && !storage.typed) {
            return storage.put(key, commentLines, (String) value);
        } else {
            ensureTyped();
            return put(key, commentLines, Arrays.asList(convertToString(value).split("\n")));
        }
    }

    public Object put(String key, String comment, Object value) {
        return put(key, Collections.singletonList(comment), value);
    }

    private Object put(String key, List<String> commentLines, List<String> valueLines) {
        String old = storage.put(key, commentLines, valueLines);
        return old != null ? storage.typed ? convertFromString(old) : old : null;
    }

    private void ensureTyped() {
        if (!storage.typed) {
            storage.typed = true;
            Set<String> keys = new HashSet<String>(storage.keySet());
            for (String key : keys) {
                storage.put(key,
                            storage.getComments(key),
                            Arrays.asList(convertToString(storage.get(key)).split("\n")));
            }
        }
    }

    public boolean update(Map<String, Object> props) {
        TypedProperties properties;
        if (props instanceof TypedProperties) {
            properties = (TypedProperties) props;
        } else {
            properties = new TypedProperties();
            for (Entry<String, Object> e : props.entrySet()) {
                properties.put(e.getKey(), e.getValue());
            }
        }
        return update(properties);
    }

    public boolean update(TypedProperties properties) {
        return storage.update(properties.storage);
    }

    public List<String> getRaw(String key) {
        return storage.getRaw(key);
    }

    public List<String> getComments(String key) {
        return storage.getComments(key);
    }

    @Override
    public Object remove(Object key) {
        return storage.remove(key);
    }

    @Override
    public void clear() {
        storage.clear();
    }

    /**
     * Return the comment header.
     *
     * @return the comment header
     */
    public List<String> getHeader()
    {
        return storage.getHeader();
    }

    /**
     * Set the comment header.
     *
     * @param header the header to use
     */
    public void setHeader(List<String> header)
    {
        storage.setHeader(header);
    }

    /**
     * Return the comment footer.
     *
     * @return the comment footer
     */
    public List<String> getFooter()
    {
        return storage.getFooter();
    }

    /**
     * Set the comment footer.
     *
     * @param footer the footer to use
     */
    public void setFooter(List<String> footer)
    {
        storage.setFooter(footer);
    }

    public void substitute(final SubstitutionCallback cb) {
        if (!substitute) {
            return;
        }
        final SubstitutionCallback callback = cb != null ? cb  : new SubstitutionCallback() {
            public String getValue(String name, String key, String value) {
                if (value.startsWith(ENV_PREFIX))
                {
                    return System.getenv(value.substring(ENV_PREFIX.length()));
                }
                else
                {
                    return System.getProperty(value);
                }
            }
        }; //wrap(new BundleContextSubstitutionCallback(null));
        Map<String, TypedProperties> props = Collections.singletonMap("root", this);
        substitute(props, prepare(props), callback, true);
    }

    private static SubstitutionCallback wrap(final InterpolationHelper.SubstitutionCallback cb) {
        return new SubstitutionCallback() {
            public String getValue(String name, String key, String value) {
                return cb.getValue(value);
            }
        };
    }

    public interface SubstitutionCallback {

        String getValue(String name, String key, String value);

    }

    public static Map<String, Map<String, String>> prepare(Map<String, TypedProperties> properties) {
        Map<String, Map<String, String>> dynamic = new HashMap<String, Map<String, String>>();
        for (Map.Entry<String, TypedProperties> entry : properties.entrySet()) {
            String name = entry.getKey();
            dynamic.put(name, new DynamicMap(name, entry.getValue().storage));
        }
        return dynamic;
    }

    public static void substitute(Map<String, TypedProperties> properties,
                                  Map<String, Map<String, String>> dynamic,
                                  SubstitutionCallback callback,
                                  boolean finalSubstitution) {
        for (Map<String, String> map : dynamic.values()) {
            ((DynamicMap) map).init(callback, finalSubstitution);
        }
        for (Map.Entry<String, TypedProperties> entry : properties.entrySet()) {
            entry.getValue().storage.putAll(dynamic.get(entry.getKey()));
        }
    }

    private static String convertToString(Object value) {
        try {
            return ConfigurationHandler.write(value);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Object convertFromString(String value) {
        try {
            return ConfigurationHandler.read(value);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class DynamicMap extends AbstractMap<String, String> {
        private final String name;
        private final Properties storage;
        private final Map<String, String> computed;
        private final Map<String, String> cycles;
        private SubstitutionCallback callback;
        private boolean finalSubstitution;

        public DynamicMap(String name, Properties storage) {
            this.name = name;
            this.storage = storage;
            this.computed = new HashMap<String, String>();
            this.cycles = new HashMap<String, String>();
        }

        public void init(SubstitutionCallback callback, boolean finalSubstitution) {
            this.callback = callback;
            this.finalSubstitution = finalSubstitution;
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            return new AbstractSet<Entry<String, String>>() {
                @Override
                public Iterator<Entry<String, String>> iterator() {
                    final Iterator<String> iterator = storage.keySet().iterator();
                    return new Iterator<Entry<String, String>>() {
                        public boolean hasNext() {
                            return iterator.hasNext();
                        }
                        public Entry<String, String> next() {
                            final String key = iterator.next();
                            return new Entry<String, String>() {
                                public String getKey() {
                                    return key;
                                }
                                public String getValue() {
                                    String v = computed.get(key);
                                    if (v == null) {
                                        v = compute(key);
//                                        computed.put(key, v);
                                    }
                                    return v;
                                }
                                public String setValue(String value) {
                                    throw new UnsupportedOperationException();
                                }
                            };
                        }
                        public void remove() {
                            throw new UnsupportedOperationException();
                        }
                    };
                }

                @Override
                public int size() {
                    return storage.size();
                }
            };
        }

        private String compute(final String key) {
            InterpolationHelper.SubstitutionCallback wrapper = new InterpolationHelper.SubstitutionCallback() {
                public String getValue(String value) {
                    if (finalSubstitution) {
                        String str = DynamicMap.this.get(value);
                        if (str != null) {
                            if (storage.typed) {
                                boolean mult;
                                boolean hasType;
                                char t = str.charAt(0);
                                if (t == '[' || t == '(') {
                                    mult = true;
                                    hasType = false;
                                } else if (t == '"') {
                                    mult = false;
                                    hasType = false;
                                } else {
                                    t = str.charAt(1);
                                    mult = t == '[' || t == '(';
                                    hasType = true;
                                }
                                if (mult) {
                                    throw new IllegalArgumentException("Can't substitute from a collection/array value: " + value);
                                }
                                return (String) convertFromString(hasType ? str.substring(1) : str);
                            } else {
                                return str;
                            }
                        }
                    }
                    return callback.getValue(name, key, value);
                }
            };
            String value = storage.get(key);
            String v = substVars(value, key, cycles, this, wrapper, false, finalSubstitution, finalSubstitution);
            return v;
        }
    }
}
