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
package org.apache.felix.persister.impl;


import org.apache.felix.serializer.impl.json.JsonSerializerImpl;
import org.apache.felix.persister.Persister;
import org.apache.felix.persister.PersisterFactory;
import org.apache.felix.persister.test.backend.Persistence;
import org.apache.felix.persister.test.inmemory.MockInMemoryPersistence;
import org.apache.felix.persister.test.objects.Bottom;
import org.apache.felix.persister.test.objects.SimpleMiddle;
import org.apache.felix.persister.test.objects.SimpleTop;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.serializer.Serializer;

import static org.junit.Assert.*;

public class InMemoryPersisterTest {
    private PersisterFactory factory;

    @Before
    public void setUp() {
        Serializer serializer = new JsonSerializerImpl();
        factory = new PersisterFactoryService(serializer);
    }

    @After
    public void tearDown() {
        factory = null;
    }

    @Test
    public void testInMemoryPersister() {
        Persister<SimpleTop.SimpleTopDTO> persister = factory.newPersister(SimpleTop.SimpleTopDTO.class);
        Persistence<SimpleTop.SimpleTopDTO> p = new MockInMemoryPersistence<>(persister);
        SimpleTop.SimpleTopDTO top = newMockSimpleTop();
        p.put(top.id, top);
        SimpleTop.SimpleTopDTO result = p.get(top.id);

        assertEquals(top.id,result.id);
        assertEquals(top.value1,result.value1);
        assertEquals(top.value2,result.value2);
        assertEquals(top.embedded.id,result.embedded.id);
        assertEquals(top.embedded.value,result.embedded.value);
        assertEquals(top.embedded.embedded.id,result.embedded.embedded.id);
        assertEquals(top.embedded.embedded.cul,result.embedded.embedded.cul);
    }

    private SimpleTop.SimpleTopDTO newMockSimpleTop() {
        SimpleTop.SimpleTopDTO top = new SimpleTop.SimpleTopDTO();
        top.id = "TOP";
        top.value1 = "top-value1";
        top.value2 = "top-value2";

        SimpleMiddle.SimpleMiddleDTO mid = new SimpleMiddle.SimpleMiddleDTO();
        mid.id = "MID";
        mid.value = "mid-value";

        Bottom.BottomDTO bum = new Bottom.BottomDTO();
        bum.id = "BUM";
        bum.cul = "moncul";

        mid.embedded = bum;
        top.embedded = mid;

        return top;
    }
}
