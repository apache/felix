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
package org.apache.felix.converter.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

class MapDelegate<K, V> implements Map<K, V> {
    private final ConvertingImpl convertingImpl;
    Map<K, V> delegate;

    private MapDelegate(ConvertingImpl converting, Map<K, V> del) {
        convertingImpl = converting;
        delegate = del;
    }

    static MapDelegate<String, Object> forBean(Object b, ConvertingImpl converting) {
        return new MapDelegate<>(converting, new DynamicBeanFacade(b, converting));
    }

    static <K, V> Map<K, V> forMap(Map<K, V> m, ConvertingImpl converting) {
        return new MapDelegate<>(converting, new DynamicMapFacade<>(m, converting));
    }

    static <K, V> MapDelegate<K, V> forDictionary(Dictionary<K, V> d, ConvertingImpl converting) {
        return new MapDelegate<>(converting, new DynamicDictionaryFacade<>(d, converting));
    }

    static MapDelegate<String, Object> forDTO(Object obj, ConvertingImpl converting) {
        return new MapDelegate<>(converting, new DynamicDTOFacade(obj, converting));
    }

    static MapDelegate<String, Object> forInterface(Object obj, ConvertingImpl converting) {
        return new MapDelegate<>(converting, new DynamicInterfaceFacade(obj, converting));
    }

    public int size() {
        return delegate.size();
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @SuppressWarnings("unchecked")
    public V get(Object key) {
        V val = null;
        if (keySet().contains(key)) {
            val = delegate.get(key);
        }

        if (val == null) {
            key = findConvertedKey(keySet(), key);
            val = delegate.get(key);
        }

        if (val == null)
            return null;
        else
            return (V) getConvertedValue(key, val);
    }

    private Object getConvertedValue(Object key, Object val) {
        return convertingImpl.convertMapValue(val);
    }

    private Object findConvertedKey(Set<?> keySet, Object key) {
        for (Object k : keySet) {
            Object c = convertingImpl.converter.convert(k).to(key.getClass());
            if (c != null && c.equals(key))
                return k;

//          Maybe the other way around too?
//            Object c2 = facade.convertingImpl.converter.convert(key).to(k.getClass());
//            if (c2 != null && c2.equals(key))
//                return c2;
        }
        return key;
    }

    public V put(K key, V value) {
        cloneDelegate();

        return delegate.put(key, value);
    }

    public V remove(Object key) {
        cloneDelegate();

        return delegate.remove(key);
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        cloneDelegate();

        delegate.putAll(m);
    }

    public void clear() {
        delegate = new HashMap<>();
    }

    private Set<K> internalKeySet() {
        return delegate.keySet();
    }

    public Set<K> keySet() {
        Set<K> keys = new HashSet<>();
        for (Map.Entry<K,V> entry : entrySet()) {
            keys.add(entry.getKey());
        }
        return keys;
    }

    public Collection<V> values() {
        List<V> values = new ArrayList<>();
        for (Map.Entry<K,V> entry : entrySet()) {
            values.add(entry.getValue());
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K,V>> result = new HashSet<>();
        for (Map.Entry<?,?> entry : delegate.entrySet()) {
            K key = (K) findConvertedKey(internalKeySet(), entry.getKey());
            V val = (V) getConvertedValue(key, entry.getValue());
            result.add(new MapEntry<K,V>(key, val));
        }
        return result;
    }

    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    public int hashCode() {
        return delegate.hashCode();
    }

    public V getOrDefault(Object key, V defaultValue) {
        return delegate.getOrDefault(key, defaultValue);
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        delegate.forEach(action);
    }

    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        cloneDelegate();

        delegate.replaceAll(function);
    }

    public V putIfAbsent(K key, V value) {
        cloneDelegate();

        return delegate.putIfAbsent(key, value);
    }

    public boolean remove(Object key, Object value) {
        cloneDelegate();

        return delegate.remove(key, value);
    }

    public boolean replace(K key, V oldValue, V newValue) {
        cloneDelegate();

        return delegate.replace(key, oldValue, newValue);
    }

    public V replace(K key, V value) {
        cloneDelegate();

        return delegate.replace(key, value);
    }

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        return delegate.computeIfAbsent(key, mappingFunction);
    }

    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return delegate.computeIfPresent(key, remappingFunction);
    }

    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return delegate.compute(key, remappingFunction);
    }

    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        cloneDelegate();

        return delegate.merge(key, value, remappingFunction);
    }

    private void cloneDelegate() {
        delegate = new HashMap<>(delegate);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    static class MapEntry<K,V> implements Map.Entry<K,V> {
        private final K key;
        private final V value;

        MapEntry(K k, V v) {
            key = k;
            value = v;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return value;
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }
    }
}