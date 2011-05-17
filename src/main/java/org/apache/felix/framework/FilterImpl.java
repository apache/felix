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
package org.apache.felix.framework;

import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.felix.framework.ServiceRegistrationImpl.ServiceReferenceImpl;
import org.apache.felix.framework.capabilityset.CapabilitySet;
import org.apache.felix.framework.capabilityset.SimpleFilter;
import org.apache.felix.framework.util.StringMap;
import org.apache.felix.framework.wiring.BundleCapabilityImpl;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleRevision;

public class FilterImpl implements Filter
{
    private final SimpleFilter m_filter;

    public FilterImpl(String filterStr) throws InvalidSyntaxException
    {
        try
        {
            m_filter = SimpleFilter.parse(filterStr);
        }
        catch (Throwable th)
        {
            throw new InvalidSyntaxException(th.getMessage(), filterStr);
        }
    }

    public boolean match(ServiceReference sr)
    {
        return CapabilitySet.matches((ServiceReferenceImpl) sr, m_filter);
    }

    public boolean match(Dictionary<String, ? > dctnr)
    {
        return CapabilitySet.matches(new DictionaryCapability(dctnr, false), m_filter);
    }

    public boolean matchCase(Dictionary<String, ? > dctnr)
    {
        return CapabilitySet.matches(new DictionaryCapability(dctnr, true), m_filter);
    }

    public boolean matches(Map<String, ?> map)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean equals(Object o)
    {
        return toString().equals(o.toString());
    }

    public int hashCode()
    {
        return toString().hashCode();
    }

    public String toString()
    {
        return m_filter.toString();
    }

    static class DictionaryCapability extends BundleCapabilityImpl
    {
        private final Map m_map;

        public DictionaryCapability(Dictionary dict, boolean caseSensitive)
        {
            super(null, null, Collections.EMPTY_MAP, Collections.EMPTY_MAP);
            m_map = new DictionaryMap(dict, caseSensitive);
        }

        @Override
        public BundleRevision getRevision()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getNamespace()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Map<String, String> getDirectives()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Map<String, Object> getAttributes()
        {
            return m_map;
        }

        @Override
        public List<String> getUses()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private static class DictionaryMap implements Map
    {
        private final StringMap m_map;
        private final Dictionary m_dict;

        public DictionaryMap(Dictionary dict, boolean caseSensitive)
        {
            m_dict = dict;
            if (!caseSensitive)
            {
                m_map = new StringMap(false);
                if (dict != null)
                {
                    Enumeration keys = dict.keys();
                    while (keys.hasMoreElements())
                    {
                        Object key = keys.nextElement();
                        if (m_map.get(key) == null)
                        {
                            m_map.put(key, key);
                        }
                        else
                        {
                            throw new IllegalArgumentException(
                                "Duplicate attribute: " + key.toString());
                        }
                    }
                }
            }
            else
            {
                m_map = null;
            }
        }

        public int size()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isEmpty()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean containsKey(Object o)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean containsValue(Object o)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Object get(Object o)
        {
            String key = (String) o;
            Object value = null;
            if (m_dict != null)
            {
                // If attribute names are case insensitive, then look in
                // the case insensitive key map to find the actual case of
                // the key.
                if (m_map != null)
                {
                    key = (String) m_map.get(o);
                }
                // If the key could not be found in the case insensitive
                // key map, then avoid doing the dictionary lookup on it.
                if (key != null)
                {
                    value = m_dict.get(key);
                }
            }
            return value;
        }

        public Object put(Object k, Object v)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Object remove(Object o)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void putAll(Map map)
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void clear()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Set<Object> keySet()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Collection<Object> values()
        {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Set<Entry<Object, Object>> entrySet()
        {
            return Collections.EMPTY_SET;
        }
    }
}