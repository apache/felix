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
package org.apache.felix.persister.test.objects.provider;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.felix.persister.test.backend.Persistence;
import org.apache.felix.persister.test.objects.SimpleManager;
import org.apache.felix.persister.test.objects.SimpleTop;

/**
 * Represents a service or perhaps a domain aggregate or service that
 * acts on the entity to be persisted. In this example the persistence mechanism
 * is injected directly into this service.
 */
public class SimpleManagerService implements SimpleManager {

	private final Persistence<SimpleTopEntity> persistence;

	public SimpleManagerService(Persistence<SimpleTopEntity> aBackend) {
	    persistence = aBackend;
    }

	@Override
	public void add(SimpleTop top) {
        persistence.put(top.id(), (SimpleTopEntity)top);
	}

	@Override
    public List<String> keys() {
        return persistence.keys();
    }

    @Override
	public List<SimpleTop> list() {
	    return persistence.list().stream().collect(Collectors.toList());
	}

	@Override
	public SimpleTop get(String key) {
	    return persistence.get(key);
	}

	@Override
	public void delete(String key) {
        persistence.remove(key);
	}

	@Override
	public void clear() {
        persistence.clear();
	}
}
