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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * A map that can delay the computation of certain values up until the moment that they
 * are actually needed. Useful for expensive to compute values such as the SHA-256.
 * This map does <b>not</b> support {@code null} values.
 */
@SuppressWarnings("serial")
public class LazyHashMap<K,V> extends HashMap<K,V> implements Map<K,V>
{
    private final Map<K, Callable<V>> lazyValuesMap = new HashMap<K, Callable<V>>();

    /**
     * This map behaves like a normal HashMap, expect for the entries passed in as lazy values.
     * A lazy value is a Callable object associated with a key. When the key is looked up and it's
     * one of the lazy values, the value will be computed at that point and stored in the map. If
     * the value is looked up again, it will be served from the map as usual.
     * @param lazyValues
     */
    public LazyHashMap(Collection<LazyValue<K,V>> lazyValues)
    {
        for (LazyValue<K,V> lv : lazyValues)
        {
            lazyValuesMap.put(lv.key, lv.callable);
        }
    }

    @Override
    // @SuppressWarnings("unchecked")
    public V get(Object key)
    {
        V val = super.get(key);
        if (val == null)
        {
            Callable<V> callable = lazyValuesMap.get(key);
            if (callable != null)
            {
                // Invoke the lazy computation
                try
                {
                    val = callable.call();
                    if (val == null)
                        throw new NullPointerException("Lazy computed values may not be null");

                    // callable is defined for key, so we know key is of type K
                    @SuppressWarnings("unchecked")
                    K genericKey = (K) key;

                    put(genericKey, val);
                } catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
        return val;
    }

    @Override
    public V put(K key, V value)
    {
        // We cannot support the null value as this is an indication for lazy values that
        // it hasn't been computed yet.
        if (value == null)
            throw new NullPointerException();

        return super.put(key, value);
    }

    public static class LazyValue<K,V>
    {
        final K key;
        final Callable<V> callable;

        public LazyValue(K key, Callable<V> callable)
        {
            this.key = key;
            this.callable = callable;
        }
    }
}
