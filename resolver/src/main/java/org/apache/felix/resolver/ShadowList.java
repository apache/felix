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
package org.apache.felix.resolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class ShadowList<T> implements List<T>
{
    private final List<T> m_original;
    private final List<T> m_shadow;

    public ShadowList(List<T> original)
    {
        m_original = original;
        m_shadow = new ArrayList<T>(original);
    }

    public List<T> getOriginal()
    {
        return m_original;
    }

    public int size()
    {
        return m_shadow.size();
    }

    public boolean isEmpty()
    {
        return m_shadow.isEmpty();
    }

    public boolean contains(Object o)
    {
        return m_shadow.contains(o);
    }

    public Iterator<T> iterator()
    {
        return m_shadow.iterator();
    }

    public Object[] toArray()
    {
        return m_shadow.toArray();
    }

    public <T> T[] toArray(T[] ts)
    {
        return m_shadow.toArray(ts);
    }

    public boolean add(T e)
    {
        return m_shadow.add(e);
    }

    public boolean remove(Object o)
    {
        return m_shadow.remove(o);
    }

    public boolean containsAll(Collection<?> clctn)
    {
        return m_shadow.containsAll(clctn);
    }

    public boolean addAll(Collection<? extends T> clctn)
    {
        return m_shadow.addAll(clctn);
    }

    public boolean addAll(int i, Collection<? extends T> clctn)
    {
        return m_shadow.addAll(i, clctn);
    }

    public boolean removeAll(Collection<?> clctn)
    {
        return m_shadow.removeAll(clctn);
    }

    public boolean retainAll(Collection<?> clctn)
    {
        return m_shadow.retainAll(clctn);
    }

    public void clear()
    {
        m_shadow.clear();
    }

    public T get(int i)
    {
        return m_shadow.get(i);
    }

    public T set(int i, T e)
    {
        return m_shadow.set(i, e);
    }

    public void add(int i, T e)
    {
        m_shadow.add(i, e);
    }

    public T remove(int i)
    {
        return m_shadow.remove(i);
    }

    public int indexOf(Object o)
    {
        return m_shadow.indexOf(o);
    }

    public int lastIndexOf(Object o)
    {
        return m_shadow.lastIndexOf(o);
    }

    public ListIterator<T> listIterator()
    {
        return m_shadow.listIterator();
    }

    public ListIterator<T> listIterator(int i)
    {
        return m_shadow.listIterator(i);
    }

    public List<T> subList(int i, int i1)
    {
        return m_shadow.subList(i, i1);
    }
}