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

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

public class CopyOnWriteSet<E> extends AbstractSet<E> {

    Object[] data;

    public CopyOnWriteSet() {
        data = new Object[0];
    }

    public CopyOnWriteSet(Collection<E> col) {
        if (col instanceof CopyOnWriteSet) {
            data = ((CopyOnWriteSet) col).data;
        } else {
            data = col.toArray(new Object[col.size()]);
        }
    }

    @Override
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

    @Override
    public int size() {
        return data.length;
    }

    @Override
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

    @Override
    public boolean equals(Object o) {
        if (o instanceof CopyOnWriteSet && ((CopyOnWriteSet) o).data == data) {
            return true;
        }
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }

}
