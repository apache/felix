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
import java.util.List;

/**
 * @author $Id$
 */
class ArrayDelegate<T> extends AbstractCollectionDelegate<T>
		implements List<T> {
	// An array, either scalar or primitive
	private final Object backingArray;

	ArrayDelegate(Object arr) {
		backingArray = arr;
	}

	@Override
	public int size() {
		return Array.getLength(backingArray);
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public Object[] toArray() {
		Object[] arr = (Object[]) Array.newInstance(Object.class, size());
		for (int i = 0; i < size(); i++) {
			arr[i] = Array.get(backingArray, i);
		}
		return arr;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T get(int index) {
		return (T) Array.get(backingArray, index);
	}
}
