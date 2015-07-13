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
package org.apache.felix.resolver.util;

public class OpenHashMapList<K, V> extends OpenHashMap<K, CopyOnWriteList<V>> {

    public OpenHashMapList() {
        super();
    }

    public OpenHashMapList(int initialCapacity) {
        super(initialCapacity);
    }

<<<<<<< HEAD
=======
    @SuppressWarnings("unchecked")
>>>>>>> d6bbce6... Speed up collections a bit
    public OpenHashMapList<K, V> deepClone() {
        OpenHashMapList<K, V> copy = (OpenHashMapList<K, V>) super.clone();
        Object[] values = copy.value;
        for (int i = values.length; i-- > 0;) {
            if (values[i] != null) {
                values[i] = new CopyOnWriteList<V>((CopyOnWriteList<V>) values[i]);
            }
        }
        return copy;
    }

    @Override
    protected CopyOnWriteList<V> compute(K key) {
        return new CopyOnWriteList<V>();
    }

}
