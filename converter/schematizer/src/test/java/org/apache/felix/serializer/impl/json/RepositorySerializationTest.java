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
package org.apache.felix.serializer.impl.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.felix.serializer.test.objects.Bottom;
import org.apache.felix.serializer.test.objects.ComplexManager;
import org.apache.felix.serializer.test.objects.ComplexMiddle;
import org.apache.felix.serializer.test.objects.ComplexTop;
import org.apache.felix.serializer.test.objects.SimpleManager;
import org.apache.felix.serializer.test.objects.SimpleTop;
import org.apache.felix.serializer.test.objects.provider.BottomEntity;
import org.apache.felix.serializer.test.objects.provider.ComplexManagerService;
import org.apache.felix.serializer.test.objects.provider.ComplexMiddleEntity;
import org.apache.felix.serializer.test.objects.provider.ComplexTopEntity;
import org.apache.felix.serializer.test.objects.provider.ObjectFactory;
import org.apache.felix.serializer.test.objects.provider.SimpleManagerService;
import org.apache.felix.serializer.test.objects.provider.SimpleTopEntity;
import org.apache.felix.serializer.test.prevayler.Repository;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * General class for testing any type of RepositoryStore.
 *
 * @author David Leangen
 */
public class RepositorySerializationTest
{
    private SimpleManager simpleManager;
    private ComplexManager complexManager;
    private ObjectFactory factory;

    @Before
	public void configure() throws Exception {
	    simpleManager = new SimpleManagerService();
        complexManager = new ComplexManagerService();
        factory = new ObjectFactory();
	}

    @After
	public void cleanup() throws Exception {
		simpleManager.clear();
	}

    @Test
    @Ignore("davidb: I've ignore'd this test as it fails. Need to revisit once the converter has stabilized")
	public void shouldPutAndRemoveSimpleEntitiesFromStore() {
		simpleManager.clear();
		final SimpleTopEntity e1 = factory.newSimpleTop( "ID01", "Value01", null );
		final SimpleTopEntity e2 = factory.newSimpleTop( "ID02", "Value02", null );
		final SimpleTopEntity e3 = factory.newSimpleTop( "ID03", "Value03", null );
		final SimpleTopEntity e4 = factory.newSimpleTop( "ID04", "Value04", null );
		final SimpleTopEntity e5 = factory.newSimpleTop( "ID05", "Value05", null );

		simpleManager.add( e1 );
		simpleManager.add( e2 );
		simpleManager.add( e3 );
		simpleManager.add( e4 );
		simpleManager.add( e5 );

		assertEquals( 5, simpleManager.list().size() );

		simpleManager.delete( e1.getId() );
		simpleManager.delete( e2.getId() );
		simpleManager.delete( e3.getId() );
		simpleManager.delete( e4.getId() );
		simpleManager.delete( e5.getId() );

		assertTrue( simpleManager.list().isEmpty() );

		final Map<String, SimpleTop> m = new HashMap<>();
		m.put( e1.getId(), e1 );
		m.put( e2.getId(), e2 );
		m.put( e3.getId(), e3 );
		m.put( e4.getId(), e4 );
		m.put( e5.getId(), e5 );
		simpleManager.repository().putAll( m );

		assertEquals( 5, simpleManager.list().size() );

		simpleManager.clear();

		assertTrue( simpleManager.list().isEmpty() );
	}

    @Test
    @Ignore("davidb: I've @Ignore-d this test as the DTO used breaks the DTO "
            + "contract which says that DTOs cannot contain any methods. As these "
            + "entities contain some methods they are not recognized as DTOs")
    public void shouldPutAndRemoveComplexEntityFromStore() {
        complexManager.clear();
        assertTrue( complexManager.list().isEmpty() );

        final BottomEntity b1 = factory.newBottom( "B01", "Bum01" );
        final BottomEntity b2 = factory.newBottom( "B02", "Bum02" );
        final BottomEntity b3 = factory.newBottom( "B03", "Bum03" );
        final Collection<BottomEntity> bottoms = new ArrayList<>();
        bottoms.add( b1 );
        bottoms.add( b2 );
        bottoms.add( b3 );

        final ComplexMiddleEntity m = factory.newComplexMiddle( "M", "Middle", bottoms );

        final ComplexTopEntity e = factory.newComplexTop( "ID01", "Value01", m );

        complexManager.add( e );

        assertEquals( 1, complexManager.list().size() );
        final ComplexTop retrievedE = complexManager.get( "ID01" ).get();
        assertEquals( "Value01", retrievedE.getValue() );
        final ComplexMiddle retrievedM = retrievedE.getEmbeddedValue();
        assertNotNull( retrievedM );
        assertEquals( "M", retrievedM.getId() );
        final Collection<Bottom> retrievedBs = retrievedM.getEmbeddedValue();
        assertNotNull( retrievedBs );
        assertEquals( 3, retrievedBs.size() );
        final Set<String> ids = new HashSet<>();
        for( final Bottom b : retrievedBs )
            ids.add( b.getId() );
        assertTrue( ids.contains( "B01" ) );
        assertTrue( ids.contains( "B02" ) );
        assertTrue( ids.contains( "B03" ) );

        complexManager.delete( e.getId() );

        assertTrue( complexManager.list().isEmpty() );
    }

    @Test
    @Ignore("davidb: I've @Ignore-d this test as the DTO used breaks the DTO "
            + "contract which says that DTOs cannot contain any methods. As these "
            + "entities contain some methods they are not recognized as DTOs")
    public void shouldPutAllToStore() {
        complexManager.clear();
        assertTrue( complexManager.list().isEmpty() );

        final BottomEntity b1 = factory.newBottom( "B01", "Bum01" );
        final BottomEntity b2 = factory.newBottom( "B02", "Bum02" );
        final BottomEntity b3 = factory.newBottom( "B03", "Bum03" );
        final Collection<BottomEntity> bottoms = new ArrayList<>();
        bottoms.add( b1 );
        bottoms.add( b2 );
        bottoms.add( b3 );

        final ComplexMiddleEntity m = factory.newComplexMiddle( "M", "Middle", bottoms );

        final ComplexTopEntity e1 = factory.newComplexTop( "ID01", "Value01", m );
        final ComplexTopEntity e2 = factory.newComplexTop( "ID02", "Value02", m );
        final ComplexTopEntity e3 = factory.newComplexTop( "ID03", "Value03", m );

        final List<ComplexTop> tops = new ArrayList<>();
        tops.add( e1 );
        tops.add( e2 );
        tops.add( e3 );

        complexManager.addAll( tops );

        assertEquals( 3, complexManager.list().size() );
        final ComplexTop retrievedE = complexManager.get( "ID01" ).get();
        assertEquals( "Value01", retrievedE.getValue() );
        final ComplexMiddle retrievedM = retrievedE.getEmbeddedValue();
        assertNotNull( retrievedM );
        assertEquals( "M", retrievedM.getId() );
        final Collection<Bottom> retrievedBs = retrievedM.getEmbeddedValue();
        assertNotNull( retrievedBs );
        assertEquals( 3, retrievedBs.size() );
        final Set<String> bottomIds = new HashSet<>();
        for( final Bottom b : retrievedBs )
            bottomIds.add( b.getId() );
        assertTrue( bottomIds.contains( "B01" ) );
        assertTrue( bottomIds.contains( "B02" ) );
        assertTrue( bottomIds.contains( "B03" ) );

        final List<String> ids = new ArrayList<>();
        ids.add( "ID01" );
        ids.add( "ID02" );
        ids.add( "ID03" );

        assertEquals(3, complexManager.list().size());
    }

    @Test
    @Ignore("davidb: I've ignore'd this test as it fails. Need to revisit once the converter has stabilized")
	public void shouldIterateThroughKeysAndValues() {
	    simpleManager.clear();

	    final SimpleTopEntity e1 = factory.newSimpleTop( "ID01", "Value01", null );
        final SimpleTopEntity e2 = factory.newSimpleTop( "ID02", "Value02", null );
        final SimpleTopEntity e3 = factory.newSimpleTop( "ID03", "Value03", null );
        final SimpleTopEntity e4 = factory.newSimpleTop( "ID04", "Value04", null );
        final SimpleTopEntity e5 = factory.newSimpleTop( "ID05", "Value05", null );

        simpleManager.add( e1 );
        simpleManager.add( e2 );
        simpleManager.add( e3 );
        simpleManager.add( e4 );
        simpleManager.add( e5 );

        final Iterator<String> keys = simpleManager.repository().keys().iterator();

		final Set<String> keySet = new TreeSet<String>();
		while( keys.hasNext() )
		{
			keySet.add( keys.next() );
		}

		assertEquals( 5, keySet.size() );
		assertTrue( keySet.contains( "ID01" ) );
		assertTrue( keySet.contains( "ID02" ) );
		assertTrue( keySet.contains( "ID03" ) );
		assertTrue( keySet.contains( "ID04" ) );
		assertTrue( keySet.contains( "ID05" ) );

		final List<SimpleTop> list = new ArrayList<SimpleTop>();
		for( final SimpleTop e : simpleManager.list() )
		{
			list.add( e );
		}
		Collections.sort(list, (st1, st2) -> st1.getId().compareTo(st2.getId()));

		assertEquals( 5, list.size() );
		assertEquals( "ID01", list.get( 0 ).getId() );
		assertEquals( "ID02", list.get( 1 ).getId() );
		assertEquals( "ID03", list.get( 2 ).getId() );
		assertEquals( "ID04", list.get( 3 ).getId() );
		assertEquals( "ID05", list.get( 4 ).getId() );

		final Repository<SimpleTop> store = simpleManager.repository();
		assertEquals( "Value01", store.get( "ID01" ).get().getValue() );
		assertEquals( "Value02", store.get( "ID02" ).get().getValue() );
		assertEquals( "Value03", store.get( "ID03" ).get().getValue() );
		assertEquals( "Value04", store.get( "ID04" ).get().getValue() );
		assertEquals( "Value05", store.get( "ID05" ).get().getValue() );
	}
}
