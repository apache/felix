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

import java.lang.reflect.Array;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

@SuppressWarnings("NullableProblems")
public class CopyOnWriteSet<E> implements Set<E>, Cloneable {

    Object[] data;

    public CopyOnWriteSet() {
        data = new Object[0];
    }

    public CopyOnWriteSet(CopyOnWriteSet<? extends E> col) {
        data = col.data;
    }

    public CopyOnWriteSet(Collection<? extends E> col) {
        data = col.toArray(new Object[col.size()]);
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
                CopyOnWriteSet.this.remove(--idx);
            }
        };
    }

    public int size() {
        return data.length;
    }

    public boolean add(E e) {
        Object[] d = data;
        if (d.length == 0) {
            data = new Object[] {e};
        } else {
            for (Object o : d) {
                if (o == null ? e == null : o.equals(e)) {
                    return false;
                }
            }
            Object[] a = new Object[d.length + 1];
            System.arraycopy(d, 0, a, 0, d.length);
            a[d.length] = e;
            data = a;
        }
        return true;
    }

    private void remove(int index) {
        Object[] d = data;
        int len = d.length;
        Object[] a = new Object[len - 1];
        int numMoved = len - index - 1;
        if (index > 0) {
            System.arraycopy(d, 0, a, 0, index);
        }
        if (numMoved > 0) {
            System.arraycopy(d, index + 1, a, index, numMoved);
        }
        data = a;
    }

    public Object[] toArray() {
        return data.clone();
    }

    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        int size = data.length;
        if (a.length < size)
            // Make a new array of a's runtime type, but my contents:
            return (T[]) copyOf(data, size, a.getClass());
        System.arraycopy(data, 0, a, 0, size);
        if (a.length > size)
            a[size] = null;
        return a;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CopyOnWriteSet)) {
            return false;
        }
        Object[] o1 = data;
        Object[] o2 = ((CopyOnWriteSet) o).data;
        if (o1 == o2) {
            return true;
        }
        int l = o1.length;
        if (l != o2.length) {
            return false;
        }
        loop:
        for (int i = l; i-- > 0;) {
            Object v1 = o1[i];
            for (int j = l; j-- > 0;) {
                Object v2 = o2[j];
                if (v1 == v2)
                    continue loop;
                if (v1 != null && v1.equals(v2))
                    continue loop;
            }
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

    /**
     * Clone this object
     *
     * @return a cloned object.
     */
    @Override
    @SuppressWarnings("unchecked")
    public CopyOnWriteSet<E> clone() {
        try {
            return (CopyOnWriteSet<E>) super.clone();
        } catch (CloneNotSupportedException exc) {
            InternalError e = new InternalError();
            e.initCause(exc);
            throw e; //should never happen since we are cloneable
        }
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public boolean contains(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        int index;
        if ((index = indexOf(o, data, data.length)) >= 0) {
            remove(index);
            return true;
        }
        return false;
    }

    private static int indexOf(Object o, Object[] d, int len) {
        if (o == null) {
            for (int i = len; i-- > 0;) {
                if (d[i] == null)
                    return i;
            }
        } else {
            for (int i = len; i-- > 0;) {
                if (o.equals(d[i]))
                    return i;
            }
        }
        return -1;
    }

    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection<? extends E> c) {
        Object[] cs = c.toArray();
        if (cs.length == 0)
            return false;
        Object[] elements = data;
        int len = elements.length;
        int added = 0;
        // uniquify and compact elements in cs
        for (int i = 0; i < cs.length; ++i) {
            Object e = cs[i];
            if (indexOf(e, elements, len) < 0 &&
                    indexOf(e, cs, added) < 0)
                cs[added++] = e;
        }
        if (added > 0) {
            Object[] newElements = copyOf(elements, len + added);
            System.arraycopy(cs, 0, newElements, len, added);
            data = newElements;
            return true;
        }
        return false;
    }

    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] copyOf(T[] original, int newLength) {
        return (T[]) copyOf(original, newLength, original.getClass());
    }

    @SuppressWarnings("unchecked")
    public static <T,U> T[] copyOf(U[] original, int newLength, Class<? extends T[]> newType) {
        T[] copy;
        if ((Object) newType == Object[].class) {
            copy = (T[]) new Object[newLength];
        } else {
            copy = (T[]) Array.newInstance(newType.getComponentType(), newLength);
        }
        System.arraycopy(original, 0, copy, 0, Math.min(original.length, newLength));
        return copy;
    }
}
