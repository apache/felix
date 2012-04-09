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

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ImmutableMap<K, V> extends AbstractMap<K, V>
{
    final Entry<K, V>[] entries;

    public static <K, V> ImmutableMap<K, V> newInstance(Entry<K, V>... entries)
    {
        return new ImmutableMap<K, V>(entries);
    }

    public static <K, V> ImmutableMap<K, V> newInstance(Map<K, V> entries)
    {
        if (entries instanceof ImmutableMap)
        {
            return (ImmutableMap<K, V>) entries;
        }
        else
        {
            return new ImmutableMap<K, V>(entries);
        }
    }

    protected ImmutableMap(Entry<K, V>[] entries)
    {
        this.entries = entries.clone();
    }

    protected ImmutableMap(Map<K, V> map)
    {
        this.entries = map.entrySet().toArray(new Entry[map.size()]);
    }

    @Override
    public V get(Object key)
    {
        if (key == null)
        {
            for (int i = 0; i < entries.length; i++)
            {
                if (entries[i].getKey() == null)
                {
                    return entries[i].getValue();
                }
            }
        }
        else
        {
            for (int i = 0; i < entries.length; i++)
            {
                if (key.equals(entries[i].getKey()))
                {
                    return entries[i].getValue();
                }
            }
        }
        return null;
    }

    @Override
    public Set<Entry<K, V>> entrySet()
    {
        return new EntrySet();
    }

    private class EntrySet extends AbstractSet<Entry<K, V>>
    {
        @Override
        public Iterator<Entry<K, V>> iterator()
        {
            return new EntryItr(0);
        }

        @Override
        public int size()
        {
            return entries.length;
        }
    }

    private class EntryItr implements Iterator<Entry<K, V>>
    {
        int cursor;

        private EntryItr(int cursor)
        {
            this.cursor = cursor;
        }

        public boolean hasNext()
        {
            return cursor != size();
        }

        public Entry<K, V> next()
        {
            return entries[cursor++];
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}