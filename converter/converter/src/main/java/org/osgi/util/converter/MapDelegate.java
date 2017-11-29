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
package org.osgi.util.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author $Id: 935220b2f53a4f2cf970d3a704c03611ddd753fd $
 */
class MapDelegate<K, V> implements Map<K, V> {
	// not synchronized. Worst that can happen is that cloning is done more than
	// once, which is harmless.
	private volatile boolean		cloned	= false;
    private final ConvertingImpl convertingImpl;
    Map<K, V> delegate;

    private MapDelegate(ConvertingImpl converting, Map<K, V> del) {
        convertingImpl = converting;
        delegate = del;
    }

	static MapDelegate<String,Object> forBean(Object b, Class< ? > beanClass,
			ConvertingImpl converting) {
		return new MapDelegate<>(converting,
				new DynamicBeanFacade(b, beanClass, converting));
    }

    static <K, V> Map<K, V> forMap(Map<K, V> m, ConvertingImpl converting) {
        return new MapDelegate<>(converting, new DynamicMapFacade<>(m, converting));
    }

    static <K, V> MapDelegate<K, V> forDictionary(Dictionary<K, V> d, ConvertingImpl converting) {
        return new MapDelegate<>(converting, new DynamicDictionaryFacade<>(d, converting));
    }

	static MapDelegate<String,Object> forDTO(Object obj, Class< ? > dtoClass,
			ConvertingImpl converting) {
		return new MapDelegate<>(converting,
				new DynamicDTOFacade(obj, dtoClass, converting));
    }

	static MapDelegate<String,Object> forInterface(Object obj, Class< ? > intf,
			ConvertingImpl converting) {
		return new MapDelegate<>(converting,
				new DynamicInterfaceFacade(obj, intf, converting));
    }

	@Override
	public int size() {
		// Need to convert the entire map to get the size
		Set<Object> keys = new HashSet<>();

		Set<K> ks = delegate.keySet();
		for (K key : ks) {
			keys.add(getConvertedKey(key));
		}

        return keys.size();
    }

	@Override
	public boolean isEmpty() {
        return delegate.isEmpty();
    }

	@Override
	public boolean containsKey(Object key) {
		return keySet().contains(key);
    }

	@Override
	public boolean containsValue(Object value) {
		return values().contains(value);
    }

	@Override
	@SuppressWarnings("unchecked")
    public V get(Object key) {
        V val = null;
		if (internalKeySet().contains(key)) {
            val = delegate.get(key);
        }

        if (val == null) {
			key = findConvertedKey(internalKeySet(), key);
			val = delegate.get(key);
        }

        if (val == null)
            return null;
        else
			return (V) getConvertedValue(val);
    }

	private Object getConvertedKey(Object key) {
		return convertingImpl.convertMapKey(key);
	}

	private Object getConvertedValue(Object val) {
        return convertingImpl.convertMapValue(val);
    }

    private Object findConvertedKey(Set<?> keySet, Object key) {
        for (Object k : keySet) {
            if (key.equals(k))
                return k;
        }

        for (Object k : keySet) {
            Object c = convertingImpl.converter.convert(k).to(key.getClass());
            if (c != null && c.equals(key))
                return k;
        }
        return key;
    }

	@Override
	public V put(K key, V value) {
        cloneDelegate();

        return delegate.put(key, value);
    }

	@Override
	public V remove(Object key) {
        cloneDelegate();

        return delegate.remove(key);
    }

	@Override
	public void putAll(Map< ? extends K, ? extends V> m) {
        cloneDelegate();

        delegate.putAll(m);
    }

	@Override
	public void clear() {
		cloned = true;
        delegate = new HashMap<>();
    }

    private Set<K> internalKeySet() {
        return delegate.keySet();
    }

	@SuppressWarnings("unchecked")
	@Override
	public Set<K> keySet() {
        Set<K> keys = new HashSet<>();
		for (Object key : internalKeySet()) {
			keys.add((K) getConvertedKey(key));
        }
        return keys;
    }

	@Override
	public Collection<V> values() {
        List<V> values = new ArrayList<>();
        for (Map.Entry<K,V> entry : entrySet()) {
            values.add(entry.getValue());
        }
        return values;
    }

	@Override
	@SuppressWarnings("unchecked")
    public Set<java.util.Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K,V>> result = new HashSet<>();
        for (Map.Entry<?,?> entry : delegate.entrySet()) {
            K key = (K) findConvertedKey(internalKeySet(), entry.getKey());
			V val = (V) getConvertedValue(entry.getValue());
            result.add(new MapEntry<K,V>(key, val));
        }
        return result;
    }

	@Override
	public boolean equals(Object o) {
        return delegate.equals(o);
    }

	@Override
	public int hashCode() {
        return delegate.hashCode();
    }

    private void cloneDelegate() {
		if (cloned) {
			return;
		} else {
			cloned = true;
			delegate = new HashMap<>(delegate);
		}
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