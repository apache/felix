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
package org.apache.felix.utils.collections;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

public class StringArrayMap<V> implements Map<String, V> {

    protected Object[] table;
    protected int size;

    public static <T> Map<String, T> reduceMemory(Map<String, T> map) {
        if (map == null) {
            return Collections.emptyMap();
        }
        switch (map.size()) {
            case 0:
                return Collections.emptyMap();
            case 1:
                Entry<String, T> e = map.entrySet().iterator().next();
                return Collections.singletonMap(e.getKey().intern(), e.getValue());
            default:
                if (map instanceof StringArrayMap) {
                    @SuppressWarnings("unchecked")
                    StringArrayMap<T> m = (StringArrayMap) map;
                    if (m.size == m.table.length / 2) {
                        return map;
                    }
                }
                return new StringArrayMap<>(map);
        }
    }

    public StringArrayMap(Map<String, ? extends V> map) {
        if (map instanceof StringArrayMap) {
            size = ((StringArrayMap) map).size;
            table = Arrays.copyOf(((StringArrayMap) map).table, size * 2);
        } else {
            size = 0;
            table = new Object[map.size() * 2];
            for (Entry<String, ? extends V> e : map.entrySet()) {
                int i = size++ << 1;
                table[i++] = e.getKey().intern();
                table[i] = e.getValue();
            }
        }
    }

    public StringArrayMap() {
        this(32);
    }

    public StringArrayMap(int capacity) {
        table = new Object[capacity * 2];
        size = 0;
    }

    @SuppressWarnings("unchecked")
    public V get(Object key) {
        String k = ((String) key).intern();
        for (int i = 0, l = size << 1; i < l; i += 2) {
            if (k == table[i]) {
                return (V) table[i + 1];
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public V put(String key, V value) {
        return (V) doPut(key, value);
    }

    protected Object doPut(String key, Object value) {
        key = key.intern();
        for (int i = 0, l = size << 1; i < l; i += 2) {
            if (key == table[i]) {
                Object old = table[i + 1];
                table[i + 1] = value;
                return old;
            }
        }
        if (table.length == 0) {
            table = new Object[2];
        } else if (size * 2 == table.length) {
            Object[] n = new Object[table.length * 2];
            System.arraycopy(table, 0, n, 0, table.length);
            table = n;
        }
        int i = size++ << 1;
        table[i++] = key;
        table[i] = value;
        return null;
    }

    public Set<String> keySet() {
        return new AbstractSet<String>() {
            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {
                    int index = 0;

                    @Override
                    public boolean hasNext() {
                        return index < size;
                    }

                    @Override
                    public String next() {
                        if (index >= size) {
                            throw new NoSuchElementException();
                        }
                        return (String) table[(index++ << 1)];
                    }

                    public void remove() {
                        throw new UnsupportedOperationException("remove");
                    }
                };
            }

            @Override
            public int size() {
                return size;
            }
        };
    }

    public Collection<V> values() {
        return new AbstractCollection<V>() {
            @Override
            public Iterator<V> iterator() {
                return new Iterator<V>() {
                    int index = 0;

                    public boolean hasNext() {
                        return index < size;
                    }

                    @SuppressWarnings("unchecked")
                    public V next() {
                        if (index >= size) {
                            throw new NoSuchElementException();
                        }
                        return (V) table[(index++ << 1) + 1];
                    }

                    public void remove() {
                        throw new UnsupportedOperationException("remove");
                    }
                };
            }

            @Override
            public int size() {
                return size;
            }
        };
    }

    public Set<Entry<String, V>> entrySet() {
        return new AbstractSet<Entry<String, V>>() {
            @Override
            public Iterator<Entry<String, V>> iterator() {
                return new Iterator<Entry<String, V>>() {
                    int index = 0;

                    public boolean hasNext() {
                        return index < size;
                    }

                    @SuppressWarnings("unchecked")
                    public Entry<String, V> next() {
                        if (index >= size) {
                            throw new NoSuchElementException();
                        }
                        final int i = index << 1;
                        index++;
                        return new Entry<String, V>() {

                            public String getKey() {
                                return (String) table[i];
                            }

                            public V getValue() {
                                return (V) table[i + 1];
                            }

                            public V setValue(V value) {
                                throw new UnsupportedOperationException();
                            }
                        };
                    }

                    public void remove() {
                        throw new UnsupportedOperationException("remove");
                    }
                };
            }

            @Override
            public int size() {
                return size;
            }
        };
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean containsKey(Object key) {
        String k = ((String) key).intern();
        for (int i = 0, l = size * 2; i < l; i += 2) {
            if (table[i] == k) {
                return true;
            }
        }
        return false;
    }

    public boolean containsValue(Object value) {
        for (int i = 0, l = size * 2; i < l; i += 2) {
            if (Objects.equals(table[i + 1], value)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        String k = ((String) key).intern();
        for (int i = 0, l = size * 2; i < l; i += 2) {
            if (table[i] == k) {
                Object v = table[i + 1];
                if (i < l - 2) {
                    System.arraycopy(table, i + 2, table, i, l - 2 - i);
                }
                table[l - 1] = null;
                table[l - 2] = null;
                size--;
                return (V) v;
            }
        }
        return null;
    }

    public void putAll(Map<? extends String, ? extends V> m) {
        for (Entry<? extends String, ? extends V> e : m.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    public void clear() {
        size = 0;
        Arrays.fill(table, null);
    }

    public int hashCode() {
        int result = 1;
        for (int i = 0; i < size * 2; i++)
            result = 31 * result + (table[i] == null ? 0 : table[i].hashCode());
        return result;
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Map))
            return false;
        Map<?,?> m = (Map<?,?>) o;
        if (m.size() != size())
            return false;
        try {
            for (int i = 0, l = size * 2; i < l; i += 2) {
                Object key = table[i];
                Object value = table[i+1];
                if (value == null) {
                    if (!(m.get(key)==null && m.containsKey(key)))
                        return false;
                } else {
                    if (!value.equals(m.get(key)))
                        return false;
                }
            }
        } catch (ClassCastException | NullPointerException unused) {
            return false;
        }
        return true;
    }

    public String toString() {
        if (size == 0)
            return "{}";

        StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (int i = 0, l = size * 2; i < l; i += 2) {
            if (i > 0) {
                sb.append(',').append(' ');
            }
            sb.append(table[i]);
            sb.append('=');
            sb.append(table[i+1] == this ? "(this Map)" : table[i+1]);
        }
        return sb.append('}').toString();
    }

}
