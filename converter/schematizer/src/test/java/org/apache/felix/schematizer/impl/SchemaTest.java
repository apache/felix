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
package org.apache.felix.schematizer.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.felix.schematizer.Schema;
import org.apache.felix.schematizer.impl.MyDTO3.Count;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.util.converter.TypeReference;

import junit.framework.AssertionFailedError;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SchemaTest {
    private SchematizerImpl schematizer;

    @Before
    public void setUp() {
        schematizer = new SchematizerImpl();
    }

    @After
    public void tearDown() {
        schematizer = null;
    }

    @Test
    public void testValues() {
        Optional<Schema> opt = schematizer
                .rule("MyDTO", new TypeReference<MyDTO3<MyEmbeddedDTO2<String>>>(){})
                .rule("MyDTO", "/embedded", new TypeReference<MyEmbeddedDTO2<String>>(){})
                .rule("MyDTO", "/embedded/value", String.class)
                .get("MyDTO");

        assertTrue(opt.isPresent());
        Schema s = opt.get();
        assertNotNull(s);

        MyEmbeddedDTO2<String> embedded1 = new MyEmbeddedDTO2<>();
        embedded1.value = "value1";
        MyEmbeddedDTO2<String> embedded2 = new MyEmbeddedDTO2<>();
        embedded2.value = "value2";
        MyEmbeddedDTO2<String> embedded3 = new MyEmbeddedDTO2<>();
        embedded3.value = "value3";

        MyDTO3<MyEmbeddedDTO2<String>> dto = new MyDTO3<>();
        dto.ping = "lalala";
        dto.pong = Long.MIN_VALUE;
        dto.count = Count.ONE;
        dto.embedded = new ArrayList<>();
        dto.embedded.add(embedded1);
        dto.embedded.add(embedded2);
        dto.embedded.add(embedded3);

        assertEquals("lalala", s.valuesAt("/ping", dto).iterator().next());
        assertEquals(Long.MIN_VALUE, s.valuesAt("/pong", dto).iterator().next());
        assertEquals(Count.ONE, s.valuesAt("/count", dto).iterator().next());
        assertNotNull(s.valuesAt("/embedded", dto));
        assertListEquals(Arrays.asList(new String[]{"value1", "value2", "value3"}), s.valuesAt("/embedded/value", dto));
    }

    @Test
    public void testNullValues() {
        Optional<Schema> opt = schematizer
                .rule("MyDTO", new TypeReference<MyDTO3<MyEmbeddedDTO2<String>>>(){})
                .rule("MyDTO", "/embedded", new TypeReference<MyEmbeddedDTO2<String>>(){})
                .rule("MyDTO", "/embedded/value", String.class)
                .get("MyDTO");

        assertTrue(opt.isPresent());
        Schema s = opt.get();
        assertNotNull(s);

        MyEmbeddedDTO2<String> embedded1 = new MyEmbeddedDTO2<>();
        MyEmbeddedDTO2<String> embedded2 = new MyEmbeddedDTO2<>();
        MyEmbeddedDTO2<String> embedded3 = new MyEmbeddedDTO2<>();

        MyDTO3<MyEmbeddedDTO2<String>> dto = new MyDTO3<>();
        dto.ping = "lalala";
        dto.pong = Long.MIN_VALUE;
        dto.count = Count.ONE;
        dto.embedded = new ArrayList<>();
        dto.embedded.add(embedded1);
        dto.embedded.add(embedded2);
        dto.embedded.add(embedded3);

        assertEquals("lalala", s.valuesAt("/ping", dto).iterator().next());
        assertEquals(Long.MIN_VALUE, s.valuesAt("/pong", dto).iterator().next());
        assertEquals(Count.ONE, s.valuesAt("/count", dto).iterator().next());
        assertNotNull(s.valuesAt("/embedded", dto));
        assertListEquals(Arrays.asList(new String[]{null, null, null}), s.valuesAt("/embedded/value", dto));
    }

    @SuppressWarnings( { "rawtypes", "unchecked" } )
    private boolean assertListEquals(List<?> expected, Collection<?> actual) {
        if (expected == null || actual == null)
            throw new AssertionFailedError("The collection is null");

        if (expected.size() != actual.size())
            throw new AssertionFailedError("Expected list size of " + expected.size() + ", but was: " + actual.size());

        List actualList = new ArrayList<>();
        if (actual instanceof List)
            actualList = (List)actual;
        else
            actualList.addAll(actual);

        for (int i = 0; i < actual.size(); i++)
            assertEquals(expected.get(i), actualList.get(i));

        return true;
    }
}
