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
package org.apache.felix.dm.lambda.impl;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.ServiceReference;

/**
 * Maps a ServiceReference to a Map.
 */
public class SRefAsMap extends AbstractMap<String, Object> {
    private final ServiceReference<?> m_ref;

    public SRefAsMap(ServiceReference<?> ref) {
        m_ref = ref;
    }
    
    public Object get(Object key) {
        return m_ref.getProperty(key.toString());
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return new AbstractSet<Entry<String, Object>>() {
            @Override
            public Iterator<Entry<String, Object>> iterator() {
                final Enumeration<String> e = Collections.enumeration(Arrays.asList(m_ref.getPropertyKeys()));
                
                return new Iterator<Entry<String, Object>>() {
                    private String key;

                    public boolean hasNext() {
                        return e.hasMoreElements();
                    }

                    public Entry<String, Object> next() {
                        key = e.nextElement();
                        return new KeyEntry(key);
                    }

                    public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override
            public int size() {
                return m_ref.getPropertyKeys().length;
            }
        };
    }

    @Override
    public Object put(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    class KeyEntry implements Map.Entry<String, Object> {
        private final String key;

        KeyEntry(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public Object getValue() {
            return m_ref.getProperty(key);
        }

        public Object setValue(Object value) {
            return SRefAsMap.this.put(key, value);
        }
    }
}
