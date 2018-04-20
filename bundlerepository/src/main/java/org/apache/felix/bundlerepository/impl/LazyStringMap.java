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
package org.apache.felix.bundlerepository.impl;

import java.util.Map;

import org.apache.felix.utils.collections.StringArrayMap;

/**
 * A map that can delay the computation of certain values up until the moment that they
 * are actually needed. Useful for expensive to compute values such as the SHA-256.
 * This map does <b>not</b> support {@code null} values.
 */
@SuppressWarnings("serial")
public class LazyStringMap<V> extends StringArrayMap<V>
{
    public LazyStringMap(Map<String, ? extends V> map) {
        super(map);
    }

    public LazyStringMap() {
    }

    public LazyStringMap(int capacity) {
        super(capacity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(Object key)
    {
        V val = super.get(key);
        if (val instanceof LazyValue) {
            val = ((LazyValue<V>) val).compute();
            if (val == null) {
                throw new NullPointerException("Lazy computed values may not be null");
            }
            put((String) key, val);
        }
        return val;
    }

    public void putLazy(String key, LazyValue<V> lazy) {
        super.doPut(key, lazy);
    }

    public interface LazyValue<V>
    {
        V compute();
    }
}
