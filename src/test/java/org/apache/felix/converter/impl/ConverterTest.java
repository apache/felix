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

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import org.osgi.util.converter.ConversionException;
import org.osgi.util.converter.ConvertFunction;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.ConverterBuilder;
import org.osgi.util.converter.Rule;
import org.osgi.util.converter.StandardConverter;
import org.osgi.util.converter.TypeReference;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConverterTest {
    private Converter converter;

    @Before
    public void setUp() {
        converter = new StandardConverter();
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
    enum TestEnum2 { BLAH };
    @Test
    public void testEnums() {
        assertSame(TestEnum.BLAH, converter.convert("BLAH").to(TestEnum.class));
        assertSame(TestEnum.X, converter.convert('X').to(TestEnum.class));
        assertSame(TestEnum.FALSE, converter.convert(false).to(TestEnum.class));
        assertSame(TestEnum.BAR, converter.convert(1).to(TestEnum.class));
        assertSame(TestEnum.BLAH, converter.convert(TestEnum2.BLAH).to(TestEnum.class));
        assertNull(converter.convert(null).to(TestEnum.class));
        assertNull(converter.convert(Collections.emptySet()).to(TestEnum.class));
    }

    @Test
    public void testToReflectType() {
        Type t = TestEnum.class;
        TestEnum e = converter.convert("X").to(t);
        assertEquals(TestEnum.X, e);
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
    public void testCustomStringArrayConversion() {
        ConverterBuilder cb = converter.newConverterBuilder();
        cb.rule(String[].class, String.class,
                v -> Stream.of(v).collect(Collectors.joining(",")),
                v -> v.split(","));
        Converter adapted = cb.build();

        String[] sa = {"A", "B"};
        assertEquals("A,B", adapted.convert(sa).to(String.class));
        assertArrayEquals(sa, adapted.convert("A,B").to(String[].class));
    }

    @Test
    public void testCustomIntArrayConversion() {
        ConverterBuilder cb = converter.newConverterBuilder();
        cb.rule(int[].class, String.class,
                v -> Arrays.stream(v).mapToObj(Integer::toString).collect(Collectors.joining(",")),
                v -> Arrays.stream(v.split(",")).mapToInt(Integer::parseInt).toArray());
        Converter adapted = cb.build();

        int[] ia = {1, 2};
        assertEquals("1,2", adapted.convert(ia).to(String.class));
        assertArrayEquals(ia, adapted.convert("1,2").to(int[].class));
    }

    @Test
    public void testCustomErrorHandling() {
        ConvertFunction<String,Integer> func = new ConvertFunction<String,Integer>() {
            @Override
            public Integer convert(String obj, Type targetType) throws Exception {
                return null;
            }

            @Override
            public Integer handleError(String obj, Type targetType) {
                if ("hello".equals(obj)) {
                    return -1;
                }
                return null;
            }
        };

        ConverterBuilder cb = converter.newConverterBuilder();
        cb.rule(new Rule<>(String.class, Integer.class, func));
        Converter adapted = cb.build();

        assertEquals(new Integer(12), adapted.convert("12").to(Integer.class));
        assertEquals(new Integer(-1), adapted.convert("hello").to(Integer.class));

        // This is with the non-adapted converter
        try {
            converter.convert("hello").to(Integer.class);
            fail("Should have thrown a Conversion Exception when converting 'hello' to a number");
        } catch (ConversionException ce) {
            // good
        }
    }

    @Test
    public void testUUIDConversion() {
        UUID uuid = UUID.randomUUID();
        String s = converter.convert(uuid).to(String.class);
        assertTrue("UUID should be something", s.length() > 0);
        UUID uuid2 = converter.convert(s).to(UUID.class);
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
    public void testCalendarDate() {
        Calendar cal = new GregorianCalendar(2017, 1, 13);
        Date d = cal.getTime();

        Converter c = new StandardConverter();

        String s = c.convert(d).toString();
        assertEquals(d, c.convert(s).to(Date.class));

        String s2 = c.convert(cal).toString();
        assertEquals(cal, c.convert(s2).to(Calendar.class));
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

        MyEmbeddedDTO e = (MyEmbeddedDTO) m.get("embedded");
        assertEquals("hohoho", e.marco);
        assertEquals(Long.MAX_VALUE, e.polo);
        assertEquals(Alpha.A, e.alpha);
        /*
        Map e = (Map)m.get("embedded");
        assertEquals("hohoho", e.get("marco"));
        assertEquals(Long.MAX_VALUE, e.get("polo"));
        assertEquals(Alpha.A, e.get("alpha"));
        */
    }

    @Test
    public void testDTO2Map2() {
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
        Map m = converter.convert(dto).sourceAsDTO().to(Map.class);
        assertEquals(4, m.size());
        assertEquals("lalala", m.get("ping"));
        assertEquals(Long.MIN_VALUE, m.get("pong"));
        assertEquals(Count.ONE, m.get("count"));
        assertNotNull(m.get("embedded"));

        MyEmbeddedDTO e = (MyEmbeddedDTO) m.get("embedded");
        assertEquals("hohoho", e.marco);
        assertEquals(Long.MAX_VALUE, e.polo);
        assertEquals(Alpha.A, e.alpha);

        /* TODO this is the way it was, but it does not seem right
        Map e = (Map)m.get("embedded");
        assertEquals("hohoho", e.get("marco"));
        assertEquals(Long.MAX_VALUE, e.get("polo"));
        assertEquals(Alpha.A, e.get("alpha"));
        */
    }

    @Test
    public void testDTO2Map3() {
        MyEmbeddedDTO embedded2 = new MyEmbeddedDTO();
        embedded2.marco = "hohoho";
        embedded2.polo = Long.MAX_VALUE;
        embedded2.alpha = Alpha.A;

        MyDTOWithMethods embedded = new MyDTOWithMethods();
        embedded.ping = "lalala";
        embedded.pong = Long.MIN_VALUE;
        embedded.count = Count.ONE;
        embedded.embedded = embedded2;

        MyDTO8 dto = new MyDTO8();
        dto.ping = "lalala";
        dto.pong = Long.MIN_VALUE;
        dto.count = MyDTO8.Count.ONE;
        dto.embedded = embedded;

        @SuppressWarnings("rawtypes")
        Map m = converter.convert(dto).sourceAsDTO().to(Map.class);
        assertEquals(4, m.size());
        assertEquals("lalala", m.get("ping"));
        assertEquals(Long.MIN_VALUE, m.get("pong"));
        assertEquals(MyDTO8.Count.ONE, m.get("count"));
        assertNotNull(m.get("embedded"));
        assertTrue(m.get( "embedded" ) instanceof MyDTOWithMethods);
        MyDTOWithMethods e = (MyDTOWithMethods)m.get("embedded");
        assertEquals("lalala", e.ping);
        assertEquals(Long.MIN_VALUE, e.pong);
        assertEquals(Count.ONE, e.count);
        assertNotNull(e.embedded);
        assertTrue(e.embedded instanceof MyEmbeddedDTO);
        MyEmbeddedDTO e2 = e.embedded;
        assertEquals("hohoho", e2.marco);
        assertEquals(Long.MAX_VALUE, e2.polo);
        assertEquals(Alpha.A, e2.alpha);
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

    @Test
    public void testMap2DTOView() {
        Map<String, Object> src = Collections.singletonMap("pong", 42);
        MyDTOWithMethods dto = converter.convert(src).targetAs(MyDTO.class).to(MyDTOWithMethods.class);
        assertEquals(42, dto.pong);
    }

    @Test @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testDTOWithGenerics() {
        MyDTO2 dto = new MyDTO2();
        dto.longList = Arrays.asList(999L, 1000L);
        dto.dtoMap = new LinkedHashMap<>();

        MyDTO3 subDTO1 = new MyDTO3();
        subDTO1.charSet = new HashSet<>(Arrays.asList('f', 'o', 'o'));
        dto.dtoMap.put("zzz", subDTO1);

        MyDTO3 subDTO2 = new MyDTO3();
        subDTO2.charSet = new HashSet<>(Arrays.asList('b', 'a', 'r'));
        dto.dtoMap.put("aaa", subDTO2);

        Map m = converter.convert(dto).to(Map.class);
        assertEquals(2, m.size());

        assertEquals(Arrays.asList(999L, 1000L), m.get("longList"));
        Map nestedMap = (Map) m.get("dtoMap");

        // Check iteration order is preserved by iterating
        int i=0;
        for (Iterator<Map.Entry> it = nestedMap.entrySet().iterator(); it.hasNext(); i++) {
            Map.Entry entry = it.next();
            switch (i) {
            case 0:
                assertEquals("zzz", entry.getKey());
                MyDTO3 dto1 = (MyDTO3) entry.getValue();
                assertNotSame("Should have created a copy", subDTO1, dto1);
                assertEquals(new HashSet<Character>(Arrays.asList('f', 'o')), dto1.charSet);
                break;
            case 1:
                assertEquals("aaa", entry.getKey());
                MyDTO3 dto2 = (MyDTO3) entry.getValue();
                assertNotSame("Should have created a copy", subDTO2, dto2);
                assertEquals(new HashSet<Character>(Arrays.asList('b', 'a', 'r')), dto2.charSet);
                break;
            default:
                fail("Unexpected number of elements on map");
            }
        }

        // convert back
        MyDTO2 dto2 = converter.convert(m).to(MyDTO2.class);
        assertEquals(dto.longList, dto2.longList);

        // Cannot simply do dto.equals() as the DTOs don't implement that
        assertEquals(dto.dtoMap.size(), dto2.dtoMap.size());
        MyDTO3 dto2SubZZZ = dto2.dtoMap.get("zzz");
        assertEquals(dto2SubZZZ.charSet, new HashSet<Character>(Arrays.asList('f', 'o')));
        MyDTO3 dto2SubAAA = dto2.dtoMap.get("aaa");
        assertEquals(dto2SubAAA.charSet, new HashSet<Character>(Arrays.asList('b', 'a', 'r')));
    }

    @Test
    public void testMapToDTOWithSurplusMapFiels() {
        Map<String, String> m = new HashMap<>();
        m.put("foo", "bar");
        MyDTO3 dtoDoesNotMap = converter.convert(m).to(MyDTO3.class);
        assertNull(dtoDoesNotMap.charSet);
    }

    @Test @SuppressWarnings("rawtypes")
    public void testCopyMap() {
        Map m = new HashMap();
        Map m2 = converter.convert(m).to(Map.class);
        assertEquals(m, m2);
        assertNotSame(m, m2);
    }

    @Test @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testCopyMap2() {
        Map m = new HashMap();
        m.put("key", Arrays.asList("a", "b", "c"));
        Map m2 = converter.convert(m).to(Map.class);
        assertEquals(m, m2);
        assertNotSame(m, m2);
    }

    @Test
    public void testConversionPriority() {
        MyBean mb = new MyBean();
        mb.intfVal = 17;
        mb.beanVal = "Hello";

        assertEquals(Collections.singletonMap("value", "Hello"),
                converter.convert(mb).sourceAsBean().to(Map.class));
    }

    @Test
    public void testConvertAsInterface() {
        MyBean mb = new MyBean();
        mb.intfVal = 17;
        mb.beanVal = "Hello";

        assertEquals(17,
                converter.convert(mb).sourceAs(MyIntf.class).to(Map.class).get("value"));
    }

    @Test
    public void testConvertAsBean() {
        MyBean mb = new MyBean();
        mb.intfVal = 17;
        mb.beanVal = "Hello";

        assertEquals(Collections.singletonMap("value", "Hello"),
                converter.convert(mb).sourceAsBean().to(Map.class));
    }

    @Test
    public void testConvertAsDTO() {
        MyClass3 mc3 = new MyClass3(17);

        assertEquals(17,
                converter.convert(mc3).sourceAsDTO().to(Map.class).get("value"));
    }

    @Test
    public void testDTONameMangling() {
        Map<String,String> m = new HashMap<>();
        m.put("org.osgi.framework.uuid", "test123");
        m.put("myProperty143", "true");
        m.put("my$prop", "42");
        m.put("dot.prop", "456");
        m.put(".secret", " ");
        m.put("another_prop", "lalala");
        m.put("three_.prop", "hi ha ho");
        m.put("four._prop", "");
        m.put("five..prop", "test");

        MyDTO7 dto = converter.convert(m).to(MyDTO7.class);
        assertEquals("test123", dto.org_osgi_framework_uuid);
        assertTrue(dto.myProperty143);
        assertEquals(42, dto.my$$prop);
        assertEquals(Long.valueOf(456L), dto.dot_prop);
        assertEquals(' ', dto._secret);
        assertEquals("lalala", dto.another__prop);
        assertEquals("hi ha ho", dto.three___prop);
        assertEquals("", dto.four_$__prop);
        assertEquals("test", dto.five_$_prop);

        // And convert back
        Map<String, String> m2 = converter.convert(dto).to(new TypeReference<Map<String,String>>() {});
        assertEquals(new HashMap<String,String>(m), new HashMap<String,String>(m2));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLiveMapFromInterface() {
        int[] val = new int[1];
        val[0] = 51;

        MyIntf intf = new MyIntf() {
            @Override
            public int value() {
                return val[0];
            }
        };

        @SuppressWarnings("rawtypes")
        Map m = converter.convert(intf).to(Map.class);
        assertEquals(51, m.get("value"));

        val[0] = 52;
        assertEquals("Changes to the backing map should be reflected",
                52, m.get("value"));

        m.put("value", 53);
        assertEquals(53, m.get("value"));

        val[0] = 54;
        assertEquals("Changes to the backing map should not be reflected any more",
                53, m.get("value"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testLiveMapFromDTO() {
        MyDTO8 myDTO = new MyDTO8();

        myDTO.count = MyDTO8.Count.TWO;
        myDTO.pong = 42L;

        @SuppressWarnings("rawtypes")
        Map m = converter.convert(myDTO).to(Map.class);
        assertEquals(42L, m.get("pong"));

        myDTO.ping = "Ping!";
        assertEquals("Ping!", m.get("ping"));
        myDTO.pong = 52L;
        assertEquals(52L, m.get("pong"));
        myDTO.ping = "Pong!";
        assertEquals("Pong!", m.get("ping"));

        m.put("pong", 62L);
        myDTO.ping = "Poing!";
        myDTO.pong = 72L;
        assertEquals("Pong!", m.get("ping"));
        assertEquals(62L, m.get("pong"));
    }

    @Test
    public void testLiveMapFromDictionary() throws URISyntaxException {
        URI testURI = new URI("http://foo");
        Hashtable<String, Object> d = new Hashtable<>();
        d.put("test", testURI);

        Map<String, Object> m = converter.convert(d).to(new TypeReference<Map<String, Object>>(){});
        assertEquals(testURI, m.get("test"));

        URI testURI2 = new URI("http://bar");
        d.put("test2", testURI2);
        assertEquals(testURI2, m.get("test2"));
        assertEquals(testURI, m.get("test"));
    }

    @Test
    public void testLiveMapFromMap() {
        Map<String, String> s = new HashMap<>();

        s.put("true", "123");
        s.put("false", "456");

        Map<Boolean, Short> m = converter.convert(s).to(new TypeReference<Map<Boolean, Short>>(){});
        assertEquals(Short.valueOf("123"), m.get(Boolean.TRUE));
        assertEquals(Short.valueOf("456"), m.get(Boolean.FALSE));

        s.remove("true");
        assertNull(m.get(Boolean.TRUE));

        s.put("TRUE", "999");
        assertEquals(Short.valueOf("999"), m.get(Boolean.TRUE));
    }

    @Test
    public void testLiveMapFromBean() {
        MyBean mb = new MyBean();
        mb.beanVal = "" + Long.MAX_VALUE;

        Map<SomeEnum, Long> m = converter.convert(mb).sourceAsBean().to(new TypeReference<Map<SomeEnum, Long>>(){});
        assertEquals(1, m.size());
        assertEquals(Long.valueOf(Long.MAX_VALUE), m.get(SomeEnum.VALUE));

        mb.beanVal = "" + Long.MIN_VALUE;
        assertEquals(Long.valueOf(Long.MIN_VALUE), m.get(SomeEnum.VALUE));

        m.put(SomeEnum.GETVALUE, 123L);
        mb.beanVal = "12";
        assertEquals(Long.valueOf(Long.MIN_VALUE), m.get(SomeEnum.VALUE));
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

    static interface MyIntf {
        int value();
    }

    static class MyBean implements MyIntf {
        int intfVal;
        String beanVal;

        @Override
        public int value() {
            return intfVal;
        }

        public String getValue() {
            return beanVal;
        }
    }

    static class MyClass3 {
        public int value;
        public String string = "String";

        public MyClass3( int value ) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    static @interface MyAnnotation {
        int value() default 17;
    }

    enum SomeEnum { VALUE, GETVALUE };
}
