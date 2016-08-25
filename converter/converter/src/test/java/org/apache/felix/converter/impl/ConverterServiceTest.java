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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.felix.converter.impl.MyDTO.Count;
import org.apache.felix.converter.impl.MyEmbeddedDTO.Alpha;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.converter.Adapter;
import org.osgi.service.converter.ConversionException;
import org.osgi.service.converter.TypeReference;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConverterServiceTest {
    private ConverterService converter;

    @Before
    public void setUp() {
        converter = new ConverterService();
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
        assertTrue(converter.convert(72).to(boolean.class));
        assertFalse(converter.convert("false").to(boolean.class));
        assertFalse(converter.convert("bleh").to(boolean.class));
        assertFalse(converter.convert((char) 0).to(boolean.class));
        assertFalse(converter.convert(null).to(boolean.class));
        assertFalse(converter.convert(Collections.emptyList()).to(boolean.class));

        // Converstions to integer
        assertEquals(Integer.valueOf(123), converter.convert("123").to(int.class));
        assertEquals(1, (int) converter.convert(true).to(int.class));
        assertEquals(0, (int) converter.convert(false).to(int.class));

        // Conversions to Class
        assertEquals(BigDecimal.class, converter.convert("java.math.BigDecimal").to(Class.class));
        assertNull(converter.convert(null).to(Class.class));
        assertNull(converter.convert(Collections.emptyList()).to(Class.class));

        assertEquals(Integer.valueOf(123), converter.convert("123").to(Integer.class));
        assertEquals(Long.valueOf(123), converter.convert("123").to(Long.class));
        assertEquals('1', (char) converter.convert("123").to(Character.class));
        assertEquals('Q', (char) converter.convert(null).defaultValue('Q').to(Character.class));
        assertEquals((char) 123, (char) converter.convert(123L).to(Character.class));
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
    public void testFromGenericSetToLinkedList() {
        Set<Integer> s = new LinkedHashSet<>();
        s.add(123);
        s.add(456);

        LinkedList<String> ll = converter.convert(s).to(new TypeReference<LinkedList<String>>() {});
        assertEquals(Arrays.asList("123", "456"), ll);
    }

    @Test
    public void testFromArrayToGenericOrderPreservingSet() {
        String[] sa = {"567", "-765", "0", "-900"};

        // Returned set should be order preserving
        Set<Long> s = converter.convert(sa).to(new TypeReference<Set<Long>>() {});

        List<String> sl = new ArrayList<>(Arrays.asList(sa));
        for (long l : s) {
            long expected = Long.parseLong(sl.remove(0));
            assertEquals(expected, l);
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
    public void testCharArrayConversion() {
        char[] ca = converter.convert(new int[] {9,8,7}).to(char[].class);
        assertArrayEquals(new char[] {9,8,7}, ca);
        Character[] ca2 = converter.convert((long) 17).to(Character[].class);
        assertArrayEquals(new Character[] {(char)17}, ca2);
        char[] ca3 = converter.convert(new short[] {257}).to(char[].class);
        assertArrayEquals(new char[] {257}, ca3);
        char c = converter.convert(new char[] {'x', 'y'}).to(char.class);
        assertEquals('x', c);
        char[] ca4a = {'x', 'y'};
        char[] ca4b = converter.convert(ca4a).to(char[].class);
        assertArrayEquals(new char [] {'x', 'y'}, ca4b);
        assertNotSame("Should have created a new instance", ca4a, ca4b);
    }

    @Test
    public void testLongCollectionConversion() {
        long[] l = converter.convert(Long.MAX_VALUE).to(long[].class);
        assertArrayEquals(new long[] {Long.MAX_VALUE}, l);
        Long[] l2 = converter.convert(Long.MAX_VALUE).to(Long[].class);
        assertArrayEquals(new Long[] {Long.MAX_VALUE}, l2);
        List<Long> ll = converter.convert(new long[] {Long.MIN_VALUE, Long.MAX_VALUE}).to(new TypeReference<List<Long>>() {});
        assertEquals(Arrays.asList(Long.MIN_VALUE, Long.MAX_VALUE), ll);
        List<Long> ll2 = converter.convert(Arrays.asList(123, 345)).to(new TypeReference<List<Long>>() {});
        assertEquals(Arrays.asList(123L, 345L), ll2);

    }

    @Test
    public void testExceptionDefaultValue() {
        assertEquals(42, (int) converter.convert("haha").defaultValue(42).to(int.class));
        assertNull(converter.convert("haha").defaultValue(null).to(int.class));
        try {
            converter.convert("haha").to(int.class);
            fail("Should have thrown an exception");
        } catch (ConversionException ex) {
            // good
        }
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

    @Test
    public void testUUIDConversion() {
        ConverterService cs = new ConverterService();
        UUID uuid = UUID.randomUUID();
        String s = cs.convert(uuid).to(String.class);
        assertTrue("UUID should be something", s.length() > 0);
        UUID uuid2 = cs.convert(s).to(UUID.class);
        assertEquals(uuid, uuid2);
    }

    @Test
    public void testPatternConversion() {
        String p = "\\S*";
        Pattern pattern = converter.convert(p).to(Pattern.class);
        Matcher matcher = pattern.matcher("hi");
        assertTrue(matcher.matches());
        String p2 = converter.convert(pattern).to(String.class);
        assertEquals(p, p2);
    }

    @Test
    public void testLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.now();
        String s = converter.convert(ldt).to(String.class);
        assertTrue(s.length() > 0);
        LocalDateTime ldt2 = converter.convert(s).to(LocalDateTime.class);
        assertEquals(ldt, ldt2);
    }

    @Test
    public void testLocalDate() {
        LocalDate ld = LocalDate.now();
        String s = converter.convert(ld).to(String.class);
        assertTrue(s.length() > 0);
        LocalDate ld2 = converter.convert(s).to(LocalDate.class);
        assertEquals(ld, ld2);
    }

    @Test
    public void testLocalTime() {
        LocalTime lt = LocalTime.now();
        String s = converter.convert(lt).to(String.class);
        assertTrue(s.length() > 0);
        LocalTime lt2 = converter.convert(s).to(LocalTime.class);
        assertEquals(lt, lt2);
    }

    @Test
    public void testOffsetDateTime() {
        OffsetDateTime ot = OffsetDateTime.now();
        String s = converter.convert(ot).to(String.class);
        assertTrue(s.length() > 0);
        OffsetDateTime ot2 = converter.convert(s).to(OffsetDateTime.class);
        assertEquals(ot, ot2);
    }

    @Test
    public void testOffsetTime() {
        OffsetTime ot = OffsetTime.now();
        String s = converter.convert(ot).to(String.class);
        assertTrue(s.length() > 0);
        OffsetTime ot2 = converter.convert(s).to(OffsetTime.class);
        assertEquals(ot, ot2);
    }

    @Test
    public void testZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.now();
        String s = converter.convert(zdt).to(String.class);
        assertTrue(s.length() > 0);
        ZonedDateTime zdt2 = converter.convert(s).to(ZonedDateTime.class);
        assertEquals(zdt, zdt2);
    }

    @Test
    public void testDefaultValue() {
        long l = converter.convert(null).defaultValue("12").to(Long.class);
        assertEquals(12L, l);
    }

    @Test
    public void testDTO2Map() {
        MyEmbeddedDTO embedded = new MyEmbeddedDTO();
        embedded.marco = "hohoho";
        embedded.polo = Long.MAX_VALUE;
        embedded.alpha = Alpha.A;

        MyDTO dto = new MyDTO();
        dto.ping = "lalala";
        dto.pong = Long.MIN_VALUE;
        dto.count = Count.ONE;
        dto.embedded = embedded;

        @SuppressWarnings("rawtypes")
        Map m = converter.convert(dto).to(Map.class);
        assertEquals(4, m.size());
        assertEquals("lalala", m.get("ping"));
        assertEquals(Long.MIN_VALUE, m.get("pong"));
        assertEquals(Count.ONE, m.get("count"));
        assertNotNull(m.get("embedded"));
        @SuppressWarnings("rawtypes")
        Map e = (Map)m.get("embedded");
        assertEquals("hohoho", e.get("marco"));
        assertEquals(Long.MAX_VALUE, e.get("polo"));
        assertEquals(Alpha.A, e.get("alpha"));
    }

    @Test @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testDTOFieldShadowing() {
        MySubDTO dto = new MySubDTO();
        dto.ping = "test";
        dto.count = Count.THREE;

        Map m = converter.convert(dto).to(new TypeReference<Map<String,String>>() {});

        Map<String, String> expected = new HashMap<>();
        expected.put("ping", "test");
        expected.put("count", "THREE");
        expected.put("pong", "0");
        expected.put("embedded", null);
        assertEquals(expected, new HashMap<String, String>(m));

        MySubDTO dto2 = converter.convert(m).to(MySubDTO.class);
        assertEquals("test", dto2.ping);
        assertEquals(Count.THREE, dto2.count);
        assertEquals(0L, dto2.pong);
        assertNull(dto2.embedded);
    }

    @Test
    public void testMap2DTO() {
        Map<String, Object> m = new HashMap<>();
        m.put("ping", "abc xyz");
        m.put("pong", 42L);
        m.put("count", Count.ONE);
        Map<String, Object> e = new HashMap<>();
        e.put("marco", "ichi ni san");
        e.put("polo", 64L);
        e.put("alpha", Alpha.A);
        m.put("embedded", e);

        MyDTO dto = converter.convert(m).to(MyDTO.class);
        assertEquals("abc xyz", dto.ping);
        assertEquals(42L, dto.pong);
        assertEquals(Count.ONE, dto.count);
        assertNotNull(dto.embedded);
        assertEquals(dto.embedded.marco, "ichi ni san");
        assertEquals(dto.embedded.polo, 64L);
        assertEquals(dto.embedded.alpha, Alpha.A);
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
