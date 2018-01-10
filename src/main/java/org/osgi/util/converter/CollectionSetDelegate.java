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
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author $Id$
 */
class CollectionSetDelegate<T> implements Set<T> {
	private final Collection<T> delegate;

	CollectionSetDelegate(Collection<T> coll) {
		delegate = coll;
	}

	private Set<T> setSnapshot() {
		Set<T> s = new LinkedHashSet<>();
		for (T o : delegate) {
			s.add(o);
		}
		return s;
	}

	@Override
	public int size() {
		return toArray().length;
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return delegate.contains(o);
	}

	@Override
	public Iterator<T> iterator() {
		return setSnapshot().iterator();
	}

	@Override
	public Object[] toArray() {
		return toArray(new Object[] {});
	}

	@Override
	public <X> X[] toArray(X[] a) {
		Set<T> s = setSnapshot();
		return s.toArray(a);
	}

	@Override
	public boolean add(Object e) {
		throw new UnsupportedOperationException(); // Never called
	}

	@Override
	public boolean remove(Object o) {
		throw new UnsupportedOperationException(); // Never called
	}

	@Override
	public boolean containsAll(Collection< ? > c) {
		return delegate.containsAll(c);
	}

	@Override
	public boolean addAll(Collection< ? extends T> c) {
		throw new UnsupportedOperationException(); // Never called
	}

	@Override
	public boolean retainAll(Collection< ? > c) {
		throw new UnsupportedOperationException(); // Never called
	}

	@Override
	public boolean removeAll(Collection< ? > c) {
		throw new UnsupportedOperationException(); // Never called
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException(); // Never called
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		throw new UnsupportedOperationException(); // Never called
	}

	@Override
	public String toString() {
		return delegate.toString();
	}
}
