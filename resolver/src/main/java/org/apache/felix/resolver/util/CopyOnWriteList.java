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
package org.apache.felix.resolver.util;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.Collection;

public class CopyOnWriteList<T> extends AbstractList<T> {

    Object[] data;

    public CopyOnWriteList() {
        data = new Object[0];
    }

    public CopyOnWriteList(Collection<T> col) {
        if (col instanceof CopyOnWriteList) {
            data = ((CopyOnWriteList) col).data;
        } else {
            data = col.toArray(new Object[col.size()]);
        }
    }

    @Override
    public int size() {
        return data.length;
    }

    public T get(int index) {
        return (T) data[index];
    }

    @Override
    public T set(int index, T element) {
        data = Arrays.copyOf(data, data.length);
        T prev = (T) data[index];
        data[index] = element;
        return prev;
    }

    @Override
    public void add(int index, T element) {
        Object[] elements = data;
        int len = elements.length;
        Object[] newElements = new Object[len + 1];
        int numMoved = len - index;
        if (index > 0) {
            System.arraycopy(elements, 0, newElements, 0, index);
        }
        if (numMoved > 0) {
            System.arraycopy(elements, index, newElements, index + 1, numMoved);
        }
        newElements[index] = element;
        data = newElements;
    }

    public T remove(int index) {
        Object[] elements = data;
        int len = elements.length;
        T oldValue = (T) elements[index];
        Object[] newElements = new Object[len - 1];
        int numMoved = len - index - 1;
        if (index > 0) {
            System.arraycopy(elements, 0, newElements, 0, index);
        }
        if (numMoved > 0) {
            System.arraycopy(elements, index + 1, newElements, index, numMoved);
        }
        data = newElements;
        return oldValue;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }
}
