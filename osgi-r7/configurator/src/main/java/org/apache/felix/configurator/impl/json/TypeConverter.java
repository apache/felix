/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.configurator.impl.json;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonStructure;

import org.osgi.util.converter.Converter;
import org.osgi.util.converter.ConverterFunction;
import org.osgi.util.converter.Converters;
import org.osgi.util.converter.TargetRule;
import org.osgi.util.converter.TypeReference;

public class TypeConverter {

    public static Converter getConverter() {
        return Converters.standardConverter().newConverterBuilder().rule(new TargetRule() {

            @Override
            public Type getTargetType() {
                return String.class;
            }

            @Override
            public ConverterFunction getFunction() {
                return new ConverterFunction() {

                    @Override
                    public Object apply(final Object obj, final Type targetType) throws Exception {
                        if ( obj instanceof Map || obj instanceof List ) {
                            final JsonStructure json = JSONUtil.build(obj);
                            final StringWriter w = new StringWriter();
                            Json.createWriter(w).write(json);
                            return w.toString();
                        }
                        return CANNOT_HANDLE;
                    }
                };
            }
        }).build();
    }

    private static final Map<String, Class<?>> TYPE_MAP = new HashMap<>();
    static {
        // scalar types and primitive types
        TYPE_MAP.put("String", String.class);
        TYPE_MAP.put("Integer", Integer.class);
        TYPE_MAP.put("int", Integer.class);
        TYPE_MAP.put("Long", Long.class);
        TYPE_MAP.put("long", Long.class);
        TYPE_MAP.put("Float", Float.class);
        TYPE_MAP.put("float", Float.class);
        TYPE_MAP.put("Double", Double.class);
        TYPE_MAP.put("double", Double.class);
        TYPE_MAP.put("Byte", Byte.class);
        TYPE_MAP.put("byte", Byte.class);
        TYPE_MAP.put("Short", Short.class);
        TYPE_MAP.put("short", Short.class);
        TYPE_MAP.put("Character", Character.class);
        TYPE_MAP.put("char", Character.class);
        TYPE_MAP.put("Boolean", Boolean.class);
        TYPE_MAP.put("boolean", Boolean.class);
         // array of scalar types and primitive types
        TYPE_MAP.put("String[]", String[].class);
        TYPE_MAP.put("Integer[]", Integer[].class);
        TYPE_MAP.put("int[]", int[].class);
        TYPE_MAP.put("Long[]", Long[].class);
        TYPE_MAP.put("long[]", long[].class);
        TYPE_MAP.put("Float[]", Float[].class);
        TYPE_MAP.put("float[]", float[].class);
        TYPE_MAP.put("Double[]", Double[].class);
        TYPE_MAP.put("double[]", double[].class);
        TYPE_MAP.put("Byte[]", Byte[].class);
        TYPE_MAP.put("byte[]", byte[].class);
        TYPE_MAP.put("Short[]", Short[].class);
        TYPE_MAP.put("short[]", short[].class);
        TYPE_MAP.put("Boolean[]", Boolean[].class);
        TYPE_MAP.put("boolean[]", boolean[].class);
        TYPE_MAP.put("Character[]", Character[].class);
        TYPE_MAP.put("char[]", char[].class);
    }

    private final List<File> allFiles = new ArrayList<>();

    private final List<File> files = new ArrayList<>();

    private final BinUtil.ResourceProvider provider;

    /**
     * Create a new instance
     * @param provider The bundle provider, might be {@code null}.
     */
    public TypeConverter(final BinUtil.ResourceProvider provider) {
        this.provider = provider;
    }

    /**
     * Convert a value to the given type
     * @param value The value
     * @param typeInfo Optional type info, might be {@code null}
     * @return The converted value or {@code null} if the conversion failed.
     * @throws IOException If an error happens
     */
    public Object convert(
            final String pid,
            final Object value,
            final String typeInfo) throws IOException {
        if ( typeInfo == null ) {
            if ( value instanceof String || value instanceof Boolean ) {
                return value;
            } else if ( value instanceof Long || value instanceof Double ) {
                return value;
            } else if ( value instanceof Integer ) {
                return ((Integer)value).longValue();
            } else if ( value instanceof Short ) {
                return ((Short)value).longValue();
            } else if ( value instanceof Byte ) {
                return ((Byte)value).longValue();
            } else if ( value instanceof Float ) {
                return ((Float)value).doubleValue();
            }
            if ( value instanceof List ) {
                @SuppressWarnings("unchecked")
                final List<Object> list = (List<Object>)value;
                if ( list.isEmpty() ) {
                    return new String[0];
                }
                final Object firstObject = list.get(0);
                Object convertedValue = null;
                if ( firstObject instanceof Boolean ) {
                    convertedValue = getConverter().convert(list).defaultValue(null).to(Boolean[].class);
                } else if ( firstObject instanceof Long || firstObject instanceof Integer || firstObject instanceof Byte || firstObject instanceof Short ) {
                    convertedValue =  getConverter().convert(list).defaultValue(null).to(Long[].class);
                } else if ( firstObject instanceof Double || firstObject instanceof Float ) {
                    convertedValue =  getConverter().convert(list).defaultValue(null).to(Double[].class);
                }
                if ( convertedValue == null ) {
                    // convert to String (TODO)
                    convertedValue = getConverter().convert(list).defaultValue(null).to(String[].class);
                }
                return convertedValue;
            }
            return null;
        }

        // binary
        if ( "binary".equals(typeInfo) ) {
            if ( provider == null ) {
                throw new IOException("Binary files only allowed within a bundle");
            }
            final String path = getConverter().convert(value).defaultValue(null).to(String.class);
            if ( path == null ) {
                throw new IOException("Invalid path for binary property: " + value);
            }
            final File filePath;
            try {
                filePath = BinUtil.extractFile(provider, pid, path);
            } catch ( final IOException ioe ) {
                throw new IOException("Unable to read " + path +
                        " in bundle " + provider.getIdentifier() +
                        " for pid " + pid +
                        " and write to " + BinUtil.binDirectory + " : " + ioe.getMessage(), ioe);
            }
            if ( filePath == null ) {
                throw new IOException("Entry " + path + " not found in bundle " + provider.getIdentifier());
            }
            files.add(filePath);
            allFiles.add(filePath);
            return filePath.getAbsolutePath();

        } else if ( "binary[]".equals(typeInfo) ) {
            if ( provider == null ) {
                throw new IOException("Binary files only allowed within a bundle");
            }
            final String[] paths = getConverter().convert(value).defaultValue(null).to(String[].class);
            if ( paths == null ) {
                throw new IOException("Invalid paths for binary[] property: " + value);
            }
            final String[] filePaths = new String[paths.length];
            int i = 0;
            while ( i < paths.length ) {
                final File filePath;
                try {
                    filePath = BinUtil.extractFile(provider, pid, paths[i]);
                } catch ( final IOException ioe ) {
                    throw new IOException("Unable to read " + paths[i] +
                            " in bundle " + provider.getIdentifier() +
                            " for pid " + pid +
                            " and write to " + BinUtil.binDirectory + " : " + ioe.getMessage(), ioe);
                }
                if ( filePath == null ) {
                    throw new IOException("Entry " + paths[i] + " not found in bundle " + provider.getIdentifier());
                }
                files.add(filePath);
                allFiles.add(filePath);
                filePaths[i] = filePath.getAbsolutePath();
                i++;
            }
            return filePaths;
        }

        final Class<?> typeClass = TYPE_MAP.get(typeInfo);
        if ( typeClass != null ) {
            return getConverter().convert(value).defaultValue(null).to(typeClass);
        }

        // Collections of scalar types
        if ( "Collection<String>".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(new TypeReference<List<String>>() {});

        } else if ( "Collection<Integer>".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(new TypeReference<List<Integer>>() {});

        } else if ( "Collection<Long>".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(new TypeReference<List<Long>>() {});

        } else if ( "Collection<Float>".equals(typeInfo)  ) {
            return getConverter().convert(value).defaultValue(null).to(new TypeReference<List<Float>>() {});

        } else if ( "Collection<Double>".equals(typeInfo)  ) {
            return getConverter().convert(value).defaultValue(null).to(new TypeReference<List<Double>>() {});

        } else if ( "Collection<Byte>".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(new TypeReference<List<Byte>>() {});

        } else if ( "Collection<Short>".equals(typeInfo)  ) {
            return getConverter().convert(value).defaultValue(null).to(new TypeReference<List<Short>>() {});

        } else if ( "Collection<Character>".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(new TypeReference<List<Character>>() {});

        } else if ( "Collection<Boolean>".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(new TypeReference<List<Boolean>>() {});
        } else if ( "Collection".equals(typeInfo) ) {
            if ( value instanceof List ) {
                @SuppressWarnings("unchecked")
                final List<Object> list = (List<Object>)value;
                if ( list.isEmpty() ) {
                    return new String[0];
                }
                final Object firstObject = list.get(0);
                Object convertedValue = null;
                if ( firstObject instanceof Boolean ) {
                    convertedValue = getConverter().convert(list).defaultValue(null).to(new TypeReference<List<Boolean>>() {});
                } else if ( firstObject instanceof Long || firstObject instanceof Integer || firstObject instanceof Byte || firstObject instanceof Short) {
                    convertedValue = getConverter().convert(list).defaultValue(null).to(new TypeReference<List<Long>>() {});
                } else if ( firstObject instanceof Double || firstObject instanceof Float ) {
                    convertedValue = getConverter().convert(list).defaultValue(null).to(new TypeReference<List<Double>>() {});
                }
                if ( convertedValue == null ) {

                    // convert to String (TODO)
                    convertedValue = getConverter().convert(list).defaultValue(null).to(new TypeReference<List<String>>() {});
                }
                return convertedValue;
            }
            return getConverter().convert(value).defaultValue(null).to(Collection.class);
        }

        // unknown type - ignore configuration
        throw new IOException("Invalid type information: " + typeInfo);
    }

    public void cleanupFiles() {
        for(final File f : allFiles) {
            f.delete();
        }
    }

    public List<File> flushFiles() {
        if ( this.files.isEmpty() ) {
            return null;
        } else {
            final List<File> result = new ArrayList<>(this.files);
            this.files.clear();
            return result;
        }
    }
}
