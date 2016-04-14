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
import java.util.Collections;
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

        assertEquals(Integer.valueOf(123), converter.convert("123").to(Integer.class));
        assertEquals(Long.valueOf(123), converter.convert("123").to(Long.class));
//        assertEquals(Character.valueOf(123), c.convert("123").to(Character.class));
        assertEquals(Byte.valueOf((byte) 123), converter.convert("123").to(Byte.class));
        assertEquals(Float.valueOf("12.3"), converter.convert("12.3").to(Float.class));
        assertEquals(Double.valueOf("12.3"), converter.convert("12.3").to(Double.class));
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

}
