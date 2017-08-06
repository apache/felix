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
package org.apache.felix.persister.test.backend;

import java.util.List;
import java.util.Map;

/**
 * Represents the interface to some persistence mechanism, such as 
 * a generic Repository, Datastore, or Database.
 * It could be a key/value store, or something else, and the actual methods
 * exposed may be different. But in any case we consider some basic CRUD 
 * operations, which will require serialization and of course deserialization.
 */
public interface Persistence<E> {

    List<String> keys();

    List<E> list();

    E get(String key);

    int size();

    void put(String key, E entity);

    void putAll(Map<String, E> entityMap);

    void remove(String key);

    void clear();
}
