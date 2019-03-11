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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author $Id$
 */
class SetDelegate<T> implements Set<T> {
	private volatile Set<T>			delegate;
	private volatile boolean		cloned;
	private final ConvertingImpl	convertingImpl;
	private final InternalConverter converter;

	static <T> Set<T> forCollection(Collection<T> collection,
			ConvertingImpl converting, InternalConverter c) {
		if (collection instanceof Set) {
			return new SetDelegate<T>((Set<T>) collection, converting, c);
		}
		return new SetDelegate<T>(new CollectionSetDelegate<>(collection),
				converting, c);
	}

	SetDelegate(Set<T> collection, ConvertingImpl converting, InternalConverter c) {
		delegate = collection;
		convertingImpl = converting;
		converter = c;
	}

	// Whenever a modification is made, the delegate is cloned and detached.
	private void cloneDelegate() {
		if (cloned) {
			return;
		} else {
			cloned = true;
			delegate = new HashSet<>(delegate);
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
		return new SetDelegateIterator();
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

		Iterator<T> it = iterator();
		for (int i = 0; i < a.length; i++) {
			if (mySize > i && it.hasNext()) {
				a[i] = (X) it.next();
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
	public boolean retainAll(Collection< ? > c) {
		cloneDelegate();

		return delegate.retainAll(c);
	}

	@Override
	public boolean removeAll(Collection< ? > c) {
		cloneDelegate();

		return delegate.removeAll(c);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof Set))
			return false;

		Set< ? > s1 = new HashSet<>(this);
		Set< ? > s2 = new HashSet<>((Set< ? >) obj);
		return s1.equals(s2);
		// cannot call delegate.equals() because they are of different types
	}

	@Override
	public String toString() {
		return delegate.toString();
	}

	@Override
	public void clear() {
		cloned = true;
		delegate = new HashSet<>();
	}

	private class SetDelegateIterator implements Iterator<T> {
		final Iterator< ? > delegateIterator;

		@SuppressWarnings("synthetic-access")
		SetDelegateIterator() {
			delegateIterator = delegate.iterator();
		}

		@Override
		public boolean hasNext() {
			return delegateIterator.hasNext();
		}

		@SuppressWarnings({
				"unchecked", "synthetic-access"
		})
		@Override
		public T next() {
			Object obj = delegateIterator.next();
			return (T) convertingImpl.convertCollectionValue(obj, converter);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException("remove");
		}
	}
}
