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
package org.apache.felix.serializer.test.prevayler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.felix.schematizer.TypeRule;

public class MockPrevaylerBackedRepository<E>
    implements Repository<E>
{
    private final Map<String, E> store = new HashMap<>();
    private final DTOSerializer<CommandDTO<?>> serializer;
    private final MockPrevaylerSerializer prevayler;

    public MockPrevaylerBackedRepository(List<TypeRule<?>> rules, Class<E> anEntityType) {
        serializer = new DTOSerializer<>(rules, anEntityType);
        prevayler = new SerializerAdapter(serializer);
    }

    public List<String> keys() {
        return store.keySet().stream().collect(Collectors.toList());
    }

    public List<E> list() {
        return store.values().stream().collect(Collectors.toList());
    }

    public Optional<E> get(String key) {
        return Optional.ofNullable(store.get(key));
    }

    public int size() {
        return store.size();
    }

    public void put(String key, E entity) {
        PutCommand<E> command = new PutCommand<>();
        command.key = key;
        command.entity = entity;
        PutCommand<E> result = outAndIn(command);
        store.put(result.key, result.entity);
    }

    public void putAll(Map<String, E> entityMap) {
        PutAllCommand<E> command = new PutAllCommand<>();
        command.entityMap = entityMap;
        PutAllCommand<E> result = outAndIn(command);
        store.putAll(result.entityMap);
    }

    public void remove(String key) {
        RemoveCommand<E> command = new RemoveCommand<>();
        command.key = key;
        RemoveCommand<E> result = outAndIn(command);
        store.remove(result.key);
    }

    public void clear() {
        try
        {
            ClearCommand<E> command = new ClearCommand<>();
            outAndIn(command);
            store.clear();
        } catch ( Exception e ) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings( "unchecked" )
    private <C extends CommandDTO<E>>C outAndIn(C command) {
        try
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            prevayler.writeObject(out, command);
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            C result = (C)prevayler.readObject(in);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
