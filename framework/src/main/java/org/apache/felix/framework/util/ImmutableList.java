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
package org.apache.felix.framework.util;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.RandomAccess;

public class ImmutableList<E> extends AbstractList<E> implements RandomAccess
{
    final Object[] elements;

    public static <E> ImmutableList<E> newInstance(E... elements)
    {
        return new ImmutableList<E>(elements);
    }

    public static <E> ImmutableList<E> newInstance(Collection<? extends E> elements)
    {
        if (elements instanceof ImmutableList)
        {
            return (ImmutableList<E>) elements;
        }
        else
        {
            return new ImmutableList<E>(elements);
        }
    }

    protected ImmutableList(E... elements)
    {
        this.elements = elements.clone();
    }

    protected ImmutableList(Collection<? extends E> elements)
    {
        this.elements = elements.toArray();
    }

    public E get(int index)
    {
        return (E) elements[index];
    }

    public int size()
    {
        return elements.length;
    }

    @Override
    public boolean remove(Object o)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> clctn)
    {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void clear()
    {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean retainAll(java.util.Collection<?> c) 
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<E> iterator()
    {
        return listIterator();
    }

    @Override
    public ListIterator<E> listIterator(int index)
    {
        return new ListItr(index);
    }

    private class ListItr implements ListIterator<E>
    {
        int cursor;

        private ListItr(int cursor)
        {
            this.cursor = cursor;
        }

        public boolean hasNext()
        {
            return cursor != size();
        }

        public E next()
        {
            return (E) elements[cursor++];
        }

        public boolean hasPrevious()
        {
            return cursor != 0;
        }

        public E previous()
        {
            return (E) elements[--cursor];
        }

        public int nextIndex()
        {
            return cursor;
        }

        public int previousIndex()
        {
            return cursor - 1;
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        public void set(E e)
        {
            throw new UnsupportedOperationException();
        }

        public void add(E e)
        {
            throw new UnsupportedOperationException();
        }
    }
}