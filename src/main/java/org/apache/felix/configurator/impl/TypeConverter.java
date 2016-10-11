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
package org.apache.felix.configurator.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.converter.Converter;
import org.osgi.converter.StandardConverter;
import org.osgi.converter.TypeReference;

public class TypeConverter {

    public static Converter getConverter() {
        return new StandardConverter();
    }

    private final List<File> allFiles = new ArrayList<File>();

    private final List<File> files = new ArrayList<File>();

    private final Bundle bundle;

    /**
     * Create a new instance
     * @param bundle The bundle, might be {@code null}.
     */
    public TypeConverter(final Bundle bundle) {
        this.bundle = bundle;
    }

    /**
     * Convert a value to the given type
     * @param value The value
     * @param typeInfo Optional type info, might be {@code null}
     * @return The converted value or {@code null} if the conversion failed.
     * @throws IOException If an error happens
     */
    public Object convert(final Object value,
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
                if ( firstObject instanceof String ) {
                    return getConverter().convert(list).defaultValue(null).to(String[].class);
                } else if ( firstObject instanceof Boolean ) {
                    return getConverter().convert(list).defaultValue(null).to(Boolean[].class);
                } else if ( firstObject instanceof Long || firstObject instanceof Integer ) {
                    return getConverter().convert(list).defaultValue(null).to(Long[].class);
                } else if ( firstObject instanceof Double || firstObject instanceof Float ) {
                    return getConverter().convert(list).defaultValue(null).to(Double[].class);
                }
            }
            return null;
        }

        // binary
        if ( "binary".equals(typeInfo) ) {
            if ( bundle == null ) {
                throw new IOException("Binary files only allowed within a bundle");
            }
            final String path = getConverter().convert(value).defaultValue(null).to(String.class);
            if ( path == null ) {
                throw new IOException("Invalid path for binary property: " + value);
            }
            final File filePath = Util.extractFile(bundle, path);
            if ( filePath == null ) {
                throw new IOException("Invalid path for binary property: " + value);
            }
            files.add(filePath);
            allFiles.add(filePath);
            return filePath.getAbsolutePath();

        } else if ( "binary[]".equals(typeInfo) ) {
            if ( bundle == null ) {
                throw new IOException("Binary files only allowed within a bundle");
            }
            final String[] paths = getConverter().convert(value).defaultValue(null).to(String[].class);
            if ( paths == null ) {
                throw new IOException("Invalid paths for binary[] property: " + value);
            }
            final String[] filePaths = new String[paths.length];
            int i = 0;
            while ( i < paths.length ) {
                final File filePath = Util.extractFile(bundle, paths[i]);
                if ( filePath == null ) {
                    throw new IOException("Invalid path for binary property: " + value);
                }
                files.add(filePath);
                allFiles.add(filePath);
                filePaths[i] = filePath.getAbsolutePath();
                i++;
            }
            return filePaths;
        }

        // scalar types and primitive types
        if ( "String".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(String.class);

        } else if ( "Integer".equals(typeInfo) || "int".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(Integer.class);

        } else if ( "Long".equals(typeInfo) || "long".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(Long.class);

        } else if ( "Float".equals(typeInfo) || "float".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(Float.class);

        } else if ( "Double".equals(typeInfo) || "double".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(Double.class);

        } else if ( "Byte".equals(typeInfo) || "byte".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(Byte.class);

        } else if ( "Short".equals(typeInfo) || "short".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(Short.class);

        } else if ( "Character".equals(typeInfo) || "char".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(Character.class);

        } else if ( "Boolean".equals(typeInfo) || "boolean".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(Boolean.class);

        }

        // array of scalar types and primitive types
        if ( "String[]".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(String[].class);

        } else if ( "Integer[]".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(Integer[].class);

        } else if ( "int[]".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(int[].class);

        } else if ( "Long[]".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(Long[].class);

        } else if ( "long[]".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(long[].class);

        } else if ( "Float[]".equals(typeInfo)  ) {
            return getConverter().convert(value).defaultValue(null).to(Float[].class);

        } else if ( "float[]".equals(typeInfo)  ) {
            return getConverter().convert(value).defaultValue(null).to(float[].class);

        } else if ( "Double[]".equals(typeInfo)  ) {
            return getConverter().convert(value).defaultValue(null).to(Double[].class);

        } else if ( "double[]".equals(typeInfo)  ) {
            return getConverter().convert(value).defaultValue(null).to(double[].class);

        } else if ( "Byte[]".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(Byte[].class);

        } else if ( "byte[]".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(byte[].class);

        } else if ( "Short[]".equals(typeInfo)  ) {
            return getConverter().convert(value).defaultValue(null).to(Short[].class);

        } else if ( "short[]".equals(typeInfo)  ) {
            return getConverter().convert(value).defaultValue(null).to(short[].class);

        } else if ( "Character[]".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(Character[].class);

        } else if ( "char[]".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(char[].class);

        } else if ( "Boolean[]".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(Boolean[].class);

        } else if ( "boolean[]".equals(typeInfo) ) {
            return getConverter().convert(value).defaultValue(null).to(boolean[].class);
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
