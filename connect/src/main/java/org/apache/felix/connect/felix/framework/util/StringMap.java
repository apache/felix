/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.connect.felix.framework.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Simple utility class that creates a map for string-based keys. This map can
 * be set to use case-sensitive or case-insensitive comparison when searching
 * for the key. Any keys put into this map will be converted to a
 * <tt>String</tt> using the <tt>toString()</tt> method, since it is only
 * intended to compare strings.
 */
public class StringMap<T> implements Map<String, T>
{
    private TreeMap<String, T> m_map;

    public StringMap()
    {
        this(true);
    }

    public StringMap(boolean caseSensitive)
    {
        m_map = new TreeMap<String, T>(new StringComparator(caseSensitive));
    }

    public StringMap(Map<? extends String, ? extends T> map, boolean caseSensitive)
    {
        this(caseSensitive);
        putAll(map);
    }

    public boolean isCaseSensitive()
    {
        return ((StringComparator) m_map.comparator()).isCaseSensitive();
    }

    public void setCaseSensitive(boolean b)
    {
        if (isCaseSensitive() != b)
        {
            TreeMap<String, T> map = new TreeMap<String, T>(new StringComparator(b));
            map.putAll(m_map);
            m_map = map;
        }
    }

    @Override
    public int size()
    {
        return m_map.size();
    }

    @Override
    public boolean isEmpty()
    {
        return m_map.isEmpty();
    }

    @Override
    public boolean containsKey(Object arg0)
    {
        return m_map.containsKey(arg0);
    }

    @Override
    public boolean containsValue(Object arg0)
    {
        return m_map.containsValue(arg0);
    }

    @Override
    public T get(Object arg0)
    {
        return m_map.get(arg0);
    }

    @Override
    public T put(String key, T value)
    {
        return m_map.put(key, value);
    }

    @Override
    public void putAll(Map<? extends String, ? extends T> map)
    {
        for (Entry<? extends String, ? extends T> entry : map.entrySet())
        {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public T remove(Object arg0)
    {
        return m_map.remove(arg0);
    }

    @Override
    public void clear()
    {
        m_map.clear();
    }

    @Override
    public Set<String> keySet()
    {
        return m_map.keySet();
    }

    @Override
    public Collection<T> values()
    {
        return m_map.values();
    }

    @Override
    public Set<Entry<String, T>> entrySet()
    {
        return m_map.entrySet();
    }

    public String toString()
    {
        return m_map.toString();
    }
}