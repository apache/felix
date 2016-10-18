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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.felix.schematizer.TypeRule;
import org.apache.felix.serializer.test.objects.SimpleManager;
import org.apache.felix.serializer.test.objects.SimpleTop;
import org.apache.felix.serializer.test.prevayler.AggregateTypeReference;
import org.apache.felix.serializer.test.prevayler.MockPrevaylerBackedRepository;
import org.apache.felix.serializer.test.prevayler.Repository;
import org.osgi.util.converter.TypeReference;

public class SimpleManagerService
	implements SimpleManager
{
	static final String COMPONENT_NAME = "net.leangen.expedition.platform.test.Simple";

	private final Repository<SimpleTopEntity> repository;

	public SimpleManagerService() {
        final List<TypeRule<?>> rules = new ArrayList<>();
        rules.add( new TypeRule<>( "/entity", new TypeReference<Object>(){
            @Override
            public java.lang.reflect.Type getType()
            {
                return new AggregateTypeReference( null, SimpleTopEntity.class, new java.lang.reflect.Type(){} ).getType();
            }} ) );
	    repository = new MockPrevaylerBackedRepository<>(rules, SimpleTopEntity.class);
    }

	@Override
	public void add( SimpleTop top ) {
        repository.put( top.getId(), (SimpleTopEntity)top );
	}

	@Override
    public List<String> keys() {
        return repository.keys();
    }

    @Override
	public List<SimpleTop> list() {
	    return repository.list().stream().collect(Collectors.toList());
	}

	@Override
	public Optional<SimpleTop> get(String key) {
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
    public Repository<SimpleTop> repository() {
        return (Repository)repository;
    }
}
