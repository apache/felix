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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * @author $Id$
 */
abstract class AbstractCollectionDelegate<T> implements List<T> {
	@Override
	public boolean add(T e) {
		throw new UnsupportedOperationException(); // Never called
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException(); // Never called
	}

	@Override
	public boolean contains(Object o) {
		throw new UnsupportedOperationException(); // Never called
	}

	@Override
	public boolean containsAll(Collection< ? > c) {
		throw new UnsupportedOperationException(); // Never called
	}

	@Override
	public boolean addAll(Collection< ? extends T> c) {
		throw new UnsupportedOperationException(); // Never called
	}

	@Override
	public boolean addAll(int index, Collection< ? extends T> c) {
		throw new UnsupportedOperationException(); // Never called
	}

	@Override
	public boolean removeAll(Collection< ? > c) {
		throw new UnsupportedOperationException(); // Never called
	}

	@Override
	public boolean retainAll(Collection< ? > c) {
		throw new UnsupportedOperationException(); // Never called
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException(); // Never called
	}

	@Override
	public Iterator<T> iterator() {
		throw new UnsupportedOperationException(); // Never called
	}

	@Override
	public T set(int index, T element) {
		throw new UnsupportedOperationException(); // Never called
	}

	@Override
	public void add(int index, T element) {
		throw new UnsupportedOperationException(); // Never called
	}

	@Override
	public T remove(int index) {
		throw new UnsupportedOperationException(); // Never called
	}

	@Override
	public int indexOf(Object o) {
		Object[] arr = toArray();
		for (int i = 0; i < arr.length; i++) {
			if (o != null) {
				if (o.equals(arr[i]))
					return i;
			} else {
				if (arr[i] == null)
					return i;
			}
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		Object[] arr = toArray();
		for (int i = arr.length - 1; i >= 0; i--) {
			if (o != null) {
				if (o.equals(arr[i]))
					return i;
			} else {
				if (arr[i] == null)
					return i;
			}
		}
		return -1;
	}

	@Override
	public <X> X[] toArray(X[] a) {
		throw new UnsupportedOperationException(); // Never called
	}

	@Override
	public ListIterator<T> listIterator() {
		throw new UnsupportedOperationException(); // Never called
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		throw new UnsupportedOperationException(); // Never called
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException(); // Never called
	}
}
