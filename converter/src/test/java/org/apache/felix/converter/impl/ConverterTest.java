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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.converter.Adapter;
import org.osgi.service.converter.Converter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ConverterTest {
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
    public void testSimpleConversions() {
        // Conversions to String
        assertEquals("abc", converter.convert("abc").to(String.class));
        assertEquals("true", converter.convert(Boolean.TRUE).to(String.class));
        assertEquals("c", converter.convert('c').to(String.class));
        assertEquals("123", converter.convert(123).to(String.class));
        assertEquals("" + Long.MAX_VALUE, converter.convert(Long.MAX_VALUE).to(String.class));
        assertEquals("12.3", converter.convert(12.3f).to(String.class));
        assertEquals("12.345", converter.convert(12.345d).to(String.class));
        assertEquals(null, converter.convert(null).to(String.class));
        assertEquals(null, converter.convert(Collections.emptyList()).to(String.class));

        String bistr = "999999999999999999999"; // more than Long.MAX_VALUE
        assertEquals(bistr, converter.convert(new BigInteger(bistr)).to(String.class));

        // Conversions to boolean
        assertTrue(converter.convert("true").to(boolean.class));
        assertTrue(converter.convert("TRUE").to(boolean.class));
        assertTrue(converter.convert('x').to(boolean.class));
        assertTrue(converter.convert(Long.MIN_VALUE).to(boolean.class));
        assertFalse(converter.convert("false").to(boolean.class));
        assertFalse(converter.convert("bleh").to(boolean.class));
        assertFalse(converter.convert((char) 0).to(boolean.class));
        assertFalse(converter.convert(null).to(boolean.class));
        assertFalse(converter.convert(Collections.emptyList()).to(boolean.class));

        // Converstions to integer
        assertEquals(Integer.valueOf(123), converter.convert("123").to(int.class));
        assertEquals(1, (int) converter.convert(true).to(int.class));

        // Conversions to Class
        assertEquals(BigDecimal.class, converter.convert("java.math.BigDecimal").to(Class.class));
        assertNull(converter.convert(null).to(Class.class));
        assertNull(converter.convert(Collections.emptyList()).to(Class.class));

        assertEquals(Integer.valueOf(123), converter.convert("123").to(Integer.class));
        assertEquals(Long.valueOf(123), converter.convert("123").to(Long.class));
//        assertEquals(Character.valueOf(123), c.convert("123").to(Character.class));
        assertEquals(Byte.valueOf((byte) 123), converter.convert("123").to(Byte.class));
        assertEquals(Float.valueOf("12.3"), converter.convert("12.3").to(Float.class));
        assertEquals(Double.valueOf("12.3"), converter.convert("12.3").to(Double.class));
    }

    enum TestEnum { FOO, BAR, BLAH, FALSE, X};
    @Test
    public void testEnums() {
        assertEquals(TestEnum.BLAH, converter.convert("BLAH").to(TestEnum.class));
        assertEquals(TestEnum.X, converter.convert('X').to(TestEnum.class));
        assertEquals(TestEnum.FALSE, converter.convert(false).to(TestEnum.class));
        assertEquals(TestEnum.BAR, converter.convert(1).to(TestEnum.class));
        assertNull(converter.convert(null).to(TestEnum.class));
        assertNull(converter.convert(Collections.emptySet()).to(TestEnum.class));
    }

    @Test
    public void testIdentialTarget() {
        Object o = new Object();
        assertSame(o, converter.convert(o).to(Object.class));

        Thread t = new Thread(); // No converter available
        assertSame(t, converter.convert(t).to(Thread.class));
        assertSame(t, converter.convert(t).to(Runnable.class));
        assertSame(t, converter.convert(t).to(Object.class));

        Thread st = new Thread() {}; // Subclass of Thread
        assertSame(st, converter.convert(st).to(Thread.class));
    }

    @Test
    public void testFromUnknownDataTypeViaString() {
        class MyClass {
            @Override
            public String toString() {
                return "1234";
            }
        };
        MyClass o = new MyClass();

        assertEquals(1234, (int) converter.convert(o).to(int.class));
        assertEquals("1234", converter.convert(o).to(String.class));
    }

    @Test
    public void testToUnknownViaStringCtor() {
        class MyClass {
            @Override
            public String toString() {
                return "http://127.0.0.1:1234/blah";
            }
        };
        MyClass o = new MyClass();

        URL url = converter.convert(o).to(URL.class);
        assertEquals("http://127.0.0.1:1234/blah", url.toString());
        assertEquals("http", url.getProtocol());
        assertEquals("127.0.0.1", url.getHost());
        assertEquals(1234, url.getPort());
        assertEquals("/blah", url.getPath());

        assertNull(converter.convert(null).to(URL.class));
        assertNull(converter.convert(Collections.emptyList()).to(URL.class));
    }

    @Test
    public void testFromMultiToSingle() {
        assertEquals("abc", converter.convert(Collections.singleton("abc")).to(String.class));
        assertEquals("abc", converter.convert(Arrays.asList("abc", "def", "ghi")).to(String.class));
        assertEquals(42, (int) converter.convert(Arrays.asList("42", "17")).to(Integer.class));
        MyClass2 mc = converter.convert(new String[] {"xxx", "yyy", "zzz"}).to(MyClass2.class);
        assertEquals("xxx", mc.toString());
        MyClass2[] arr = new MyClass2[] {new MyClass2("3.1412"), new MyClass2("6.2824")};
        assertEquals(Float.valueOf(3.1412f), Float.valueOf(converter.convert(arr).to(float.class)));
    }

    @Test
    public void testFromListToSet() {
        List<Object> l = new ArrayList<>(Arrays.asList("A", 'B', 333));

        Set<?> s = converter.convert(l).to(Set.class);
        assertEquals(3, s.size());

        for (Object o : s) {
            Object expected = l.remove(0);
            assertEquals(expected, o);
        }
    }

    @Test
    public void testFromSetToArray() {
        Set<Integer> s = new LinkedHashSet<>();
        s.add(Integer.MIN_VALUE);

        long[] la = converter.convert(s).to(long[].class);
        assertEquals(1, la.length);
        assertEquals(Integer.MIN_VALUE, la[0]);
    }

    @Test
    public void testStringArrayToIntegerArray() {
        String[] sa = {"999", "111", "-909"};
        Integer[] ia = converter.convert(sa).to(Integer[].class);
        assertEquals(3, ia.length);
        assertArrayEquals(new Integer[] {999, 111, -909}, ia);
    }

    @Test
    public void testStandardStringArrayConversion() {
        String[] sa = {"A", "B"};
        assertEquals("A", converter.convert(sa).toString());
        assertEquals("A", converter.convert(sa).to(String.class));

        String[] sa2 = {"A"};
        assertArrayEquals(sa2, converter.convert("A").to(String[].class));
    }

    @Test
    public void testCustomStringArrayConverstion() {
        Adapter adapter = converter.getAdapter();
        adapter.rule(String[].class, String.class,
                v -> Stream.of(v).collect(Collectors.joining(",")),
                v -> v.split(","));

        String[] sa = {"A", "B"};
        assertEquals("A,B", adapter.convert(sa).to(String.class));
        assertArrayEquals(sa, adapter.convert("A,B").to(String[].class));
    }

    static class MyClass2 {
        private final String value;
        public MyClass2(String v) {
            value = v;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
