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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

@SuppressWarnings("NullableProblems")
public class CopyOnWriteList<E> implements List<E>, Cloneable {

    Object[] data;

    public CopyOnWriteList() {
        data = new Object[0];
    }

    public CopyOnWriteList(CopyOnWriteList<? extends E> col) {
        data = col.data;
    }

    public CopyOnWriteList(Collection<? extends E> col) {
        data = col.toArray(new Object[col.size()]);
    }

    public int size() {
        return data.length;
    }

    @SuppressWarnings("unchecked")
    public E get(int index) {
        return (E) data[index];
    }

    @SuppressWarnings("unchecked")
    public E set(int index, E element) {
        data = Arrays.copyOf(data, data.length);
        E prev = (E) data[index];
        data[index] = element;
        return prev;
    }

    public void add(int index, E element) {
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

    @SuppressWarnings("unchecked")
    public E remove(int index) {
        Object[] elements = data;
        int len = elements.length;
        E oldValue = (E) elements[index];
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

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    public Iterator<E> iterator() {
        return new Iterator<E>() {
            int idx = 0;
            public boolean hasNext() {
                return idx < data.length;
            }
            @SuppressWarnings("unchecked")
            public E next() {
                return (E) data[idx++];
            }
            public void remove() {
                CopyOnWriteList.this.remove(--idx);
            }
        };
    }

    public Object[] toArray() {
        return data.clone();
    }

    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        int size = data.length;
        if (a.length < size)
            // Make a new array of a's runtime type, but my contents:
            return (T[]) Arrays.copyOf(data, size, a.getClass());
        System.arraycopy(data, 0, a, 0, size);
        if (a.length > size)
            a[size] = null;
        return a;
    }

    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        int index;
        if ((index = indexOf(o)) >= 0) {
            remove(index);
            return true;
        }
        return false;
    }

    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection<?> c) {
        boolean modified = false;
        Object[] d = data, o = data;
        int idx = 0;
        for (int i = 0, l = o.length; i < l; i++) {
            if (c.contains(o[i])) {
                modified = true;
            } else if (modified) {
                if (idx == 0) {
                    d = o.clone();
                }
                d[idx++] = o[i];
            }
        }
        if (modified) {
            data = Arrays.copyOf(d, idx);
        }
        return modified;
    }

    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        data = new Object[0];
    }

    public int indexOf(Object o) {
        if (o == null) {
            Object[] d = data;
            for (int i = d.length; i-- > 0;) {
                if (d[i] == null)
                    return i;
            }
        } else {
            Object[] d = data;
            for (int i = d.length; i-- > 0;) {
                if (o.equals(d[i]))
                    return i;
            }
        }
        return -1;
    }

    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException();
    }

    public ListIterator<E> listIterator() {
        throw new UnsupportedOperationException();
    }

    public ListIterator<E> listIterator(int index) {
        throw new UnsupportedOperationException();
    }

    public List<E> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException();
    }

    /**
     * Clone this object
     *
     * @return a cloned object.
     */
    @Override
    @SuppressWarnings("unchecked")
    public CopyOnWriteList<E> clone() {
        try {
            return (CopyOnWriteList<E>) super.clone();
        } catch (CloneNotSupportedException exc) {
            InternalError e = new InternalError();
            e.initCause(exc);
            throw e; //should never happen since we are cloneable
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CopyOnWriteList)) {
            return false;
        }
        Object[] o1 = data;
        Object[] o2 = ((CopyOnWriteList) o).data;
        if (o1 == o2) {
            return true;
        }
        int i;
        if ((i = o1.length) != o2.length) {
            return false;
        }
        while (i-- > 0) {
            Object v1 = o1[i];
            Object v2 = o2[i];
            if (!(v1 == null ? v2 == null : v1.equals(v2)))
                return false;
        }
        return true;
    }
}
