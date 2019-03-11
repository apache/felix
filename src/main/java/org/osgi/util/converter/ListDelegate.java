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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * @author $Id$
 */
class ListDelegate<T> implements List<T> {
	private volatile List<T>		delegate;
	private volatile boolean		cloned;
	private final ConvertingImpl	convertingImpl;
	private final InternalConverter converter;

	@SuppressWarnings({
			"unchecked", "rawtypes"
	})
	static <T> List<T> forArray(Object arr, ConvertingImpl converting, InternalConverter c) {
		return new ListDelegate<T>(new ArrayDelegate(arr), converting, c);
	}

	static <T> List<T> forCollection(Collection<T> object,
			ConvertingImpl converting, InternalConverter c) {
		if (object instanceof List) {
			return new ListDelegate<T>((List<T>) object, converting, c);
		}
		return new ListDelegate<T>(new CollectionDelegate<>(object),
				converting, c);
	}

	private ListDelegate(List<T> del, ConvertingImpl conv, InternalConverter c) {
		delegate = del;
		convertingImpl = conv;
		converter = c;
	}

	// Whenever a modification is made, the delegate is cloned and detached.
	@SuppressWarnings("unchecked")
	private void cloneDelegate() {
		if (cloned) {
			return;
		} else {
			cloned = true;
			delegate = new ArrayList<T>((List<T>) Arrays.asList(toArray()));
		}
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return containsAll(Collections.singletonList(o));
	}

	@Override
	public Iterator<T> iterator() {
		return listIterator();
	}

	@Override
	public Object[] toArray() {
		return toArray(new Object[size()]);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <X> X[] toArray(X[] a) {
		int mySize = size();
		if (Array.getLength(a) < size()) {
			a = (X[]) Array.newInstance(a.getClass().getComponentType(),
					mySize);
		}

		for (int i = 0; i < a.length; i++) {
			if (mySize > i) {
				a[i] = (X) get(i);
			} else {
				a[i] = null;
			}
		}
		return a;
	}

	@Override
	public boolean add(T e) {
		cloneDelegate();

		return delegate.add(e);
	}

	@Override
	public boolean remove(Object o) {
		cloneDelegate();

		return delegate.remove(o);
	}

	@Override
	public boolean containsAll(Collection< ? > c) {
		List<Object> l = Arrays.asList(toArray());
		for (Object o : c) {
			if (!l.contains(o))
				return false;
		}

		return true;
	}

	@Override
	public boolean addAll(Collection< ? extends T> c) {
		cloneDelegate();

		return delegate.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection< ? extends T> c) {
		cloneDelegate();

		return delegate.addAll(index, c);
	}

	@Override
	public boolean removeAll(Collection< ? > c) {
		cloneDelegate();

		return delegate.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection< ? > c) {
		cloneDelegate();

		return delegate.retainAll(c);
	}

	@Override
	public void clear() {
		cloned = true;
		delegate = new ArrayList<>();
	}

	@SuppressWarnings("unchecked")
	@Override
	public T get(int index) {
		return (T) convertingImpl.convertCollectionValue(delegate.get(index), converter);
	}

	@Override
	public T set(int index, T element) {
		cloneDelegate();

		return delegate.set(index, element);
	}

	@Override
	public void add(int index, T element) {
		cloneDelegate();

		delegate.add(index, element);
	}

	@Override
	public T remove(int index) {
		cloneDelegate();

		return delegate.remove(index);
	}

	@Override
	public int indexOf(Object o) {
		return delegate.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return delegate.lastIndexOf(o);
	}

	@SuppressWarnings("unchecked")
	@Override
	public ListIterator<T> listIterator() {
		return (ListIterator<T>) Arrays.asList(toArray()).listIterator();
	}

	@SuppressWarnings("unchecked")
	@Override
	public ListIterator<T> listIterator(int index) {
		return (ListIterator<T>) Arrays.asList(toArray()).listIterator(index);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		return (List<T>) Arrays.asList(toArray()).subList(fromIndex, toIndex);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof List))
			return false;

		List< ? > l1 = new ArrayList<>(this);
		List< ? > l2 = new ArrayList<>((List< ? >) obj);
		return l1.equals(l2);
	}

	@Override
	public String toString() {
		return delegate.toString();
	}
}
