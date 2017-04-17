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
package org.apache.felix.serializer.test.objects.provider;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.felix.serializer.test.objects.ComplexManager;
import org.apache.felix.serializer.test.objects.ComplexTop;
import org.apache.felix.serializer.test.prevayler.MockPrevaylerBackedRepository;
import org.apache.felix.serializer.test.prevayler.Repository;

public class ComplexManagerService
	implements ComplexManager
{
    private final Repository<ComplexTopEntity> repository;

    public ComplexManagerService() {
        repository = new MockPrevaylerBackedRepository<>(ComplexTopEntity.class);
    }

    @Override
	public void add(ComplexTop top) {
        repository.put(top.getId(), (ComplexTopEntity)top);
	}

	@Override
    public void addAll(Collection<ComplexTop> tops) {
	    tops.stream().forEach(t -> add(t));
    }

	@Override
    public List<String> keys() {
        return repository.keys();
    }

    @Override
	public List<ComplexTop> list()
	{
	    return repository.list().stream().map(e -> e).collect(Collectors.toList());
	}

	@Override
	public Optional<ComplexTop> get(String key) {
		return Optional.of( repository.get( key ).get() );
	}

    @Override
	public void delete(String key) {
        repository.remove(key);
	}

	@Override
	public void clear() {
	    repository.clear();
	}

    @SuppressWarnings( { "unchecked", "rawtypes" } )
    @Override
    public Repository<ComplexTop> repository() {
        return (Repository)repository;
    }
}
