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

import java.util.*;

/**
 * Simple utility class that creates a map for string-based keys.
 * This map can be set to use case-sensitive or case-insensitive
 * comparison when searching for the key.  Any keys put into this
 * map will be converted to a <tt>String</tt> using the
 * <tt>toString()</tt> method, since it is only intended to
 * compare strings.
 **/
public class StringMap extends AbstractMap<String, Object>
{
    private static final CharArrayComparator COMPARATOR = new CharArrayComparator();

    private final TreeMap<char[], KeyValueEntry> m_map = new TreeMap<char[], KeyValueEntry>(COMPARATOR);

    public StringMap(boolean caseSensitive)
    {
    }

    public StringMap(Map map, boolean caseSensitive)
    {
        putAll(map);
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
        return m_map.containsKey(toUpperCase(arg0.toString()));
    }

    @Override
    public boolean containsValue(Object arg0)
    {
        return m_map.containsValue(arg0);
    }

    @Override
    public Object get(Object arg0)
    {
        KeyValueEntry kve = m_map.get(toUpperCase(arg0.toString()));
        return (kve != null) ? kve.value : null;
    }

    @Override
    public Object put(String key, Object value)
    {
        KeyValueEntry kve = (KeyValueEntry) m_map.put(toUpperCase(key), new KeyValueEntry(key, value));
        return (kve != null) ? kve.value : null;
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> map)
    {
        for (Map.Entry<? extends String, ? extends Object> e : map.entrySet())
        {
            put(e.getKey(), e.getValue());
        }
    }

    @Override
    public Object remove(Object arg0)
    {
        KeyValueEntry kve = m_map.remove(toUpperCase(arg0.toString()));
        return (kve != null) ? kve.value : null;
    }

    @Override
    public void clear()
    {
        m_map.clear();
    }

    public Set<Entry<String, Object>> entrySet()
    {
        return new AbstractSet<Entry<String, Object>>()
        {
            @Override
            public Iterator<Entry<String, Object>> iterator()
            {
                return new Iterator<Entry<String, Object>>()
                {
                    Iterator<Entry<char[], KeyValueEntry>> it = m_map.entrySet().iterator();

                    public boolean hasNext()
                    {
                        return it.hasNext();
                    }

                    public Entry<String, Object> next()
                    {
                        return it.next().getValue();
                    }

                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override
            public int size()
            {
                return m_map.size();
            }
        };
    }

    private static char[] toUpperCase(String str)
    {
        char[] ch = str.toCharArray();
        for (int i = 0; i < ch.length; i++)
        {
            char c = ch[i];
            if (c < 128)
            {
                if ('a' <= c && c <= 'z')
                {
                    ch[i] = (char)(c - ('a' - 'A'));
                }
            }
            else
            {
                ch[i] = Character.toUpperCase(c);
            }
        }
        return ch;
    }

    private static class CharArrayComparator implements Comparator<char[]>
    {
        public int compare(char[] v1, char[] v2)
        {
            int len1 = v1.length;
            int len2 = v2.length;
            int n = Math.min(len1, len2);
            int k = 0;
            while (k < n)
            {
                char c1 = v1[k];
                char c2 = v2[k];
                if (c1 != c2)
                {
                    return c1 - c2;
                }
                k++;
            }
            return len1 - len2;
        }
    }

    private static class KeyValueEntry implements Map.Entry<String, Object>
    {
        private KeyValueEntry(String key, Object value)
        {
            this.key = key;
            this.value = value;
        }

        public String getKey()
        {
            return key;
        }

        public Object getValue()
        {
            return value;
        }

        public Object setValue(Object value)
        {
            Object v = this.value;
            this.value = value;
            return v;
        }
        String key;
        Object value;
    }
}