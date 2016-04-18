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
package org.apache.felix.converter.impl;

import java.math.BigInteger;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.converter.Converter;
import org.osgi.service.converter.TypeReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ConverterMapTest {
    private Converter converter;

    @Before
    public void setUp() {
        converter = new ConverterImpl();
    }

    @After
    public void tearDown() {
        converter = null;
    }

    @Test
    public void testGenericMapConversion() {
        Map<Integer, String> m1 = Collections.singletonMap(42, "987654321");
        Map<String, Long> m2 = converter.convert(m1).to(new TypeReference<Map<String, Long>>(){});
        assertEquals(1, m2.size());
        assertEquals(987654321L, (long) m2.get("42"));
    }

    @Test
    public void testConvertMapToDictionary() throws Exception {
        Map<BigInteger, URL> m = new HashMap<>();
        BigInteger bi = new BigInteger("123");
        URL url = new URL("http://0.0.0.0:123");
        m.put(bi, url);

        @SuppressWarnings("unchecked")
        Dictionary<BigInteger, URL> d = converter.convert(m).to(Dictionary.class);
        assertEquals(1, d.size());
        assertSame(bi, d.keys().nextElement());
        assertSame(url, d.get(bi));
    }

    @Test
    public void testInterfaceToMap() {
        Object obj = new Object();
        TestInterface impl = new TestInterface() {
            @Override
            public String getFoo() {
                return "Chocolate!";
            }

            @Override
            public int getbar() {
                return 76543;
            }

            @SuppressWarnings("unused")
            public long getL() {
                return 1L;
            }

            @SuppressWarnings("unused")
            public boolean isSomething() {
                return true;
            }

            @SuppressWarnings("unused")
            public Object getBlah() {
                return obj;
            }

            @SuppressWarnings("unused")
            private byte getByte() {
                return (byte) 12;
            }

            @SuppressWarnings("unused")
            public String getAlt(int arg) {
                return "some value";
            }
        };

        @SuppressWarnings("rawtypes")
        Map m = converter.convert(impl).to(Map.class);
        assertEquals(5, m.size());
        assertEquals("Chocolate!", m.get("foo"));
        assertEquals(76543, (int) m.get("bar"));
        assertEquals(1L, (long) m.get("l"));
        assertTrue((boolean) m.get("something"));
        assertSame(obj, m.get("blah"));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testMapToInterface1() {
        Map m = new HashMap<>();
        m.put("foo", 12345);
        m.put("bar", "999");
        m.put("alt", "someval");

        TestInterface ti = converter.convert(m).to(TestInterface.class);
        assertEquals("12345", ti.getFoo());
        assertEquals(999, ti.getbar());
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testMapToInterface2() {
        Map m = new HashMap<>();

        TestInterface ti = converter.convert(m).to(TestInterface.class);
        assertNull(ti.getFoo());
        assertEquals(0, ti.getbar());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testCopyMap() {
        Object obj = new Object();
        Map m = new HashMap<>();
        m.put("key", obj);
        Map cm = converter.convert(m).to(Map.class);
        assertNotSame(m, cm);
        assertSame(m.get("key"), cm.get("key"));
    }

    interface TestInterface {
        String getFoo();
        int getbar();
    }
}
