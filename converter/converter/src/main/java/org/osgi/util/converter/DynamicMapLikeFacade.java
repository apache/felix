/*
 * Copyright (c) OSGi Alliance (2017). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author $Id$
 */
abstract class DynamicMapLikeFacade<K, V> implements Map<K,V> {
	protected final ConvertingImpl convertingImpl;

	protected DynamicMapLikeFacade(ConvertingImpl convertingImpl) {
		this.convertingImpl = convertingImpl;
	}

	@Override
	public int size() {
		return keySet().size();
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		return keySet().contains(key);
	}

	@Override
	public boolean containsValue(Object value) {
		for (Entry<K,V> entry : entrySet()) {
			if (value == null) {
				if (entry.getValue() == null) {
					return true;
				}
			} else if (value.equals(entry.getValue())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public V put(K key, V value) {
		// Should never be called; the delegate should swap to a copy in this
		// case
		throw new UnsupportedOperationException();
	}

	@Override
	public V remove(Object key) {
		// Should never be called; the delegate should swap to a copy in this
		// case
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map< ? extends K, ? extends V> m) {
		// Should never be called; the delegate should swap to a copy in this
		// case
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		// Should never be called; the delegate should swap to a copy in this
		// case
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<V> values() {
		List<V> res = new ArrayList<>();

		for (Map.Entry<K,V> entry : entrySet()) {
			res.add(entry.getValue());
		}
		return res;
	}

	@Override
	public Set<Entry<K,V>> entrySet() {
		Set<K> ks = keySet();

		Set<Entry<K,V>> res = new LinkedHashSet<>(ks.size());

		for (K k : ks) {
			V v = get(k);
			res.add(new MapDelegate.MapEntry<K,V>(k, v));
		}
		return res;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append('{');
		boolean first = true;
		for (Map.Entry<K,V> entry : entrySet()) {
			if (first)
				first = false;
			else
				sb.append(", ");

			sb.append(entry.getKey());
			sb.append('=');
			sb.append(entry.getValue());
		}
		sb.append('}');

		return sb.toString();
	}
}

class DynamicBeanFacade extends DynamicMapLikeFacade<String,Object> {
	private Map<String,Method>	keys	= null;
	private final Object		backingObject;
	private final Class< ? >	beanClass;

	DynamicBeanFacade(Object backingObject, Class< ? > beanClass,
			ConvertingImpl convertingImpl) {
		super(convertingImpl);
		this.backingObject = backingObject;
		this.beanClass = beanClass;
	}

	@Override
	public Object get(Object key) {
		Method m = getKeys().get(key);
		try {
			return m.invoke(backingObject);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Set<String> keySet() {
		return getKeys().keySet();
	}

	private Map<String,Method> getKeys() {
		if (keys == null)
			keys = Util.getBeanKeys(beanClass);

		return keys;
	}
}

class DynamicDictionaryFacade<K, V> extends DynamicMapLikeFacade<K,V> {
	private final Dictionary<K,V> backingObject;

	DynamicDictionaryFacade(Dictionary<K,V> backingObject,
			ConvertingImpl convertingImpl) {
		super(convertingImpl);
		this.backingObject = backingObject;
	}

	@Override
	public V get(Object key) {
		return backingObject.get(key);
	}

	@Override
	public Set<K> keySet() {
		return new HashSet<>(Collections.list(backingObject.keys()));
	}
}

class DynamicMapFacade<K, V> extends DynamicMapLikeFacade<K,V> {
	private final Map<K,V> backingObject;

	DynamicMapFacade(Map<K,V> backingObject, ConvertingImpl convertingImpl) {
		super(convertingImpl);
		this.backingObject = backingObject;
	}

	@Override
	public V get(Object key) {
		return backingObject.get(key);
	}

	@Override
	public Set<K> keySet() {
		Map<K,V> m = backingObject;
		return m.keySet();
	}
}

class DynamicDTOFacade extends DynamicMapLikeFacade<String,Object> {
	private Map<String,Field>	keys	= null;
	private final Object		backingObject;
	private final Class< ? >	dtoClass;

	DynamicDTOFacade(Object backingObject, Class< ? > dtoClass,
			ConvertingImpl converting) {
		super(converting);
		this.backingObject = backingObject;
		this.dtoClass = dtoClass;
	}

	@Override
	public Object get(Object key) {
		Field f = getKeys().get(key);
		if (f == null)
			return null;

		try {
			return f.get(backingObject);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Set<String> keySet() {
		return getKeys().keySet();
	}

	private Map<String,Field> getKeys() {
		if (keys == null)
			keys = Util.getDTOKeys(dtoClass);

		return keys;
	}
}

class DynamicInterfaceFacade extends DynamicMapLikeFacade<String,Object> {
	private Map<String,Set<Method>>	keys	= null;
	private final Object			backingObject;
	private final Class< ? >		theInterface;

	DynamicInterfaceFacade(Object backingObject, Class< ? > intf,
			ConvertingImpl convertingImpl) {
		super(convertingImpl);
		this.backingObject = backingObject;
		this.theInterface = intf;
	}

	@Override
	public Object get(Object key) {
		Set<Method> set = getKeys().get(key);
		if (set == null)
			return null;
		for (Iterator<Method> iterator = set.iterator(); iterator.hasNext();) {
			Method m = iterator.next();
			if (m.getParameterTypes().length > 0)
				continue;
			try {
				return m.invoke(backingObject);
			} catch (Exception e) {
				if (RuntimeException.class
						.isAssignableFrom(e.getCause().getClass()))
					throw ((RuntimeException) e.getCause());
				throw new RuntimeException(e);
			}
		}
		throw new ConversionException("Missing no-arg method for key: " + key);
	}

	@Override
	public Set<String> keySet() {
		return getKeys().keySet();
	}

	private Map<String,Set<Method>> getKeys() {
		if (keys == null)
			keys = Util.getInterfaceKeys(theInterface, backingObject);

		return keys;
	}
}
