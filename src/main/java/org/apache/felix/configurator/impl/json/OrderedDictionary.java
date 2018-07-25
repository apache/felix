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

import java.util.*;

/**
 * A dictionary implementation with predictable iteration order.
 *
 * Actually this class is a simple adapter from the Dictionary interface
 * to a synchronized LinkedHashMap
 *
 * @param <K>
 * @param <V>
 */
public class OrderedDictionary<K, V> extends Dictionary<K, V> implements Map<K, V> {
    private static class EnumarationImpl<E> implements Enumeration<E> {
        private final Iterator<E> iterator;

        public EnumarationImpl(Iterator<E> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasMoreElements() {
            return iterator.hasNext();
        }

        @Override
        public E nextElement() {
            return iterator.next();
        }
    }

    private Map map = Collections.synchronizedMap(new LinkedHashMap());

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public Enumeration<K> keys() {
        return new EnumarationImpl<>(map.keySet().iterator());
    }

    @Override
    public Enumeration<V> elements() {
        return new EnumarationImpl<>(map.values().iterator());
    }

    @Override
    public V get(Object key) {
        return (V) map.get(key);
    }

    @Override
    public V put(K key, V value) {
        // Make sure the value is not null
        if (value == null) {
            throw new NullPointerException();
        }

        return (V) map.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return (V) map.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<K> keySet() {
        return map.entrySet();
    }

    @Override
    public Collection<V> values() {
        return map.values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return map.equals(o);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }
}
