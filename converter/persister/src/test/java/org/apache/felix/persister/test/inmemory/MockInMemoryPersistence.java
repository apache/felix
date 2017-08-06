/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.persister.test.inmemory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.felix.persister.Persister;
import org.apache.felix.persister.test.backend.Persistence;

public class MockInMemoryPersistence<E> implements Persistence<E> {

    private final Map<String, String> store = new HashMap<>();
    private final Persister<E> persister;

    public MockInMemoryPersistence(Persister<E> aPersister) {
        persister = aPersister;
    }

    @Override
    public List<String> keys() {
        return store.keySet().stream()
                .collect(Collectors.toList());
    }

    @Override
    public List<E> list() {
        return store.values().stream()
                .map(s -> deserialize(s))
                .collect(Collectors.toList());
    }

    @Override
    public E get( String key ) {
        return deserialize(store.get(key));
    }

    @Override
    public int size() {
        return store.size();
    }

    @Override
    public void put(String key, E entity) {
        store.put(key, serialize(entity));
    }

    @Override
    public void putAll(Map<String, E> entityMap) {
        entityMap.entrySet().stream()
            .forEach(e -> store.put(e.getKey(),null));
    }

    @Override
    public void remove(String key) {
        store.remove(key);
    }

    @Override
    public void clear() {
        store.clear();
    }

    private E deserialize(String json) {
        InputStream in = new ByteArrayInputStream(json.getBytes(Charset.forName("UTF-8")));
        return persister.deserialize( in );
    }

    private String serialize(E entity) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            persister.serialize(out, entity);
            return out.toString("UTF-8");
        } catch ( UnsupportedEncodingException e ) {
            return "ERROR";
        }
    }
}
