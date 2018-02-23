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

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ShrinkableMap<K, V> implements Map<K, V>
{
    private final Map<K, V> m_delegate;

    public ShrinkableMap(Map<K, V> delegate)
    {
        m_delegate = delegate;
    }

    public int size()
    {
        return m_delegate.size();
    }

    public boolean isEmpty()
    {
        return m_delegate.isEmpty();
    }

    public boolean containsKey(Object o)
    {
        return m_delegate.containsKey(o);
    }

    public boolean containsValue(Object o)
    {
        return m_delegate.containsValue(o);
    }

    public V get(Object o)
    {
        return m_delegate.get(o);
    }

    public V put(K k, V v)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public V remove(Object o)
    {
        return m_delegate.remove(o);
    }

    public void putAll(Map<? extends K, ? extends V> map)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void clear()
    {
        m_delegate.clear();
    }

    public Set<K> keySet()
    {
        return m_delegate.keySet();
    }

    public Collection<V> values()
    {
        return m_delegate.values();
    }

    public Set<Entry<K, V>> entrySet()
    {
        return m_delegate.entrySet();
    }
}