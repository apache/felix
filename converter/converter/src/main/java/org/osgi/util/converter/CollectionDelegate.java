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

import java.util.Collection;
import java.util.List;

/**
 * @author $Id: e71af7f75c634bd77b2c8ea6da52fe4788760e99 $
 */
class CollectionDelegate<T> extends AbstractCollectionDelegate<T>
		implements List<T> {
	private final Collection<T> delegate;

	CollectionDelegate(Collection<T> coll) {
		delegate = coll;
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
	public Object[] toArray() {
		return delegate.toArray();
	}

	@SuppressWarnings("unchecked")
	@Override
	public T get(int index) {
		Object[] arr = toArray();
		if (index > arr.length)
			throw new IndexOutOfBoundsException("" + index);
		return (T) arr[index];
	}
}
