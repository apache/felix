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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.util.converter.ConvertFunction;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.ConverterBuilder;
import org.osgi.util.converter.Rule;
import org.osgi.util.converter.TypeReference;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ConverterBuilderTest {
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
    public void testStringArrayToStringAdapter() {
        ConverterBuilder cb = converter.newConverterBuilder();
        Converter ca = cb.rule(String[].class, String.class,
                v -> Stream.of(v).collect(Collectors.joining(",")),
                v -> v.split(",")).build();

        assertEquals("A", converter.convert(new String[] {"A", "B"}).to(String.class));
        assertEquals("A,B", ca.convert(new String[] {"A", "B"}).to(String.class));

        assertArrayEquals(new String [] {"A,B"},
                converter.convert("A,B").to(String[].class));
        assertArrayEquals(new String [] {"A","B"},
                ca.convert("A,B").to(String[].class));
    }

    static String convertToString(char[] a) {
        StringBuilder sb = new StringBuilder();
        for (char c : a) {
            sb.append(c);
        }
        return sb.toString();
    }

    @Test
    public void testSecondLevelAdapter() {
        ConverterBuilder cb = converter.newConverterBuilder();

        cb.rule(char[].class, String.class, ConverterBuilderTest::convertToString, null);
        cb.rule(new Rule<String, Number>(String.class, Number.class, new ConvertFunction<String, Number>() {
            @Override
            public Number convert(String obj, Type targetType, Object root, Object[] key) throws Exception {
                if (Integer.class.equals(targetType))
                    return Integer.valueOf(-1);
                else if (Long.class.equals(targetType))
                    return Long.valueOf(-1);
                return null;
            }
        }));
        Converter ca = cb.build();

        assertEquals("hi", ca.convert(new char[] {'h', 'i'}).to(String.class));
        assertEquals(Integer.valueOf(-1), ca.convert("Hello").to(Integer.class));
        assertEquals(Long.valueOf(-1), ca.convert("Hello").to(Long.class));

        // Shadow the Integer variant but keep Long going to the Number variant.
        Converter ca2 = ca.newConverterBuilder().rule(String.class, Integer.class, v -> v.length(), null).build();
        assertEquals(5, (int) ca2.convert("Hello").to(Integer.class));
        assertEquals(Long.valueOf(-1), ca2.convert("Hello").to(Long.class));
    }

    @Test
    public void testCannotHandleSpecific() {
        Converter ca = converter.newConverterBuilder().rule(
                new Rule<Integer, Long>(Integer.class, Long.class, new ConvertFunction<Integer,Long>() {
            @Override
            public Long convert(Integer obj, Type targetType, Object root, Object[] key) throws Exception {
                if (obj.intValue() != 1)
                    return new Long(-obj.intValue());
                return null;
            }
        })).build();

        assertEquals(Long.valueOf(-2), ca.convert(Integer.valueOf(2)).to(Long.class));

        // This is the exception that the rule cannot handle
        assertEquals(Long.valueOf(1), ca.convert(Integer.valueOf(1)).to(Long.class));
    }

    @Test @SuppressWarnings("rawtypes")
    public void testWildcardAdapter() {
        ConvertFunction<List, Object> foo = new ConvertFunction<List, Object>() {
            @Override
            public Object convert(List t, Type type, Object root, Object[] key) throws Exception {
                if (type instanceof Class) {
                    if (Number.class.isAssignableFrom((Class<?>) type))
                        return converter.convert(t.size()).to(type);
                }
                return null;
            }
        };

        Rule<List, Object> r = new Rule<>(List.class, Object.class, foo);
        Rule<Object, Object> allCatch = new Rule<>(Object.class, Object.class,
                (v,t,o,k) -> v.toString());

        ConverterBuilder cb = converter.newConverterBuilder();
        cb.rule(r);
        cb.rule(allCatch);
        Converter ca = cb.build();

        assertEquals(3L, (long) ca.convert(Arrays.asList("a", "b", "c")).to(Long.class));
        assertEquals(3, (long) ca.convert(Arrays.asList("a", "b", "c")).to(Integer.class));
        assertEquals("[a, b, c]", ca.convert(Arrays.asList("a", "b", "c")).to(String.class));
    }

    @Test @SuppressWarnings("rawtypes")
    public void testWildcardAdapter2() {
        Map<Object, Object> snooped = new HashMap<>();
        Rule<Object, ArrayList> r = new Rule<>(Object.class, ArrayList.class,
                (v,t,o,k) -> null,
                (v,t,o,k) -> "arraylist");
        Rule<Object, List> r2 = new Rule<>(Object.class, List.class,
                (v,t,o,k) -> null,
                (v,t,o,k) -> "list");
        Rule<Object, Object> allCatch = new Rule<>(Object.class, Object.class,
                (v,t,o,k) -> {snooped.put(v,t); return null;}, null);

        ConverterBuilder cb = converter.newConverterBuilder();
        cb.rule(r);
        cb.rule(r2);
        cb.rule(allCatch);
        Converter ca = cb.build();

        assertEquals("Precondition", 0, snooped.size());
        assertEquals("arraylist", ca.convert(
                new ArrayList<String>(Arrays.asList("a", "b", "c"))).to(String.class));
        assertEquals("Precondition", 0, snooped.size());
        assertEquals("list",ca.convert(
                new LinkedList<String>(Arrays.asList("a", "b", "c"))).to(String.class));
        assertEquals("Precondition", 0, snooped.size());
        assertEquals("a", ca.convert(
                new HashSet<String>(Arrays.asList("a", "b", "c"))).to(String.class));
        assertEquals(String.class, snooped.get(new HashSet<String>(Arrays.asList("a", "b", "c"))));
    }

    @Test
    public void testConvertAs() {
        ConverterBuilder cb = converter.newConverterBuilder();
        cb.rule(new Rule<>(MyIntf.class, MyCustomDTO.class,
                (i, t, o, k) -> { MyCustomDTO dto = new MyCustomDTO(); dto.field = "" + i.value(); return dto; }));
        cb.rule(new Rule<>(MyBean.class, MyCustomDTO.class,
                (b, t, o, k) -> { MyCustomDTO dto = new MyCustomDTO(); dto.field = b.getValue(); return dto; }));
        Converter cc = cb.build();

        MyBean mb = new MyBean();
        mb.intfVal = 17;
        mb.beanVal = "Hello";

        assertNull(converter.convert(mb).to(MyCustomDTO.class).field);
        assertNull(converter.convert(mb).sourceType(MyIntf.class).to(MyCustomDTO.class).field);
        assertEquals("Hello", cc.convert(mb).to(MyCustomDTO.class).field);
        assertEquals("17", cc.convert(mb).sourceType(MyIntf.class).to(MyCustomDTO.class).field);
    }

    @Test
    public void testConvertWithKeys() {
        ConverterBuilder cb = converter.newConverterBuilder();
        ConvertFunction<Number, String> ntc = new ConvertFunction<Number, String>() {
            @Override
            public String convert(Number obj, Type targetType, Object root, Object[] key) throws Exception {
                if ("cost".equals(key[0]))
                    return "$" + obj + ".00";
                else
                    return "" + obj;
            }
        };
        ConvertFunction<String, Number> ctn = new ConvertFunction<String, Number>() {
            @Override
            public Number convert(String obj, Type targetType, Object root, Object[] key) throws Exception {
                if ("cost".equals(key[0])) {
                    int dotIdx = obj.indexOf('.');
                    obj = obj.substring(1, dotIdx); // eat off dollar sign and decimals
                }
                return Integer.parseInt(obj);
            }
        };
        cb.rule(new Rule<Number, String>(Number.class, String.class, ntc, ctn));
        Converter c = cb.build();

        Map<String, Integer> m = new HashMap<>();
        m.put("amount", 7);
        m.put("cost", 100);

        // Convert to Dictionary<String,String>
        Dictionary<String,String> d = c.convert(m).to(new TypeReference<Dictionary<String, String>>(){});
        assertEquals(2, d.size());
        assertEquals("7", d.get("amount"));
        assertEquals("$100.00", d.get("cost"));

        // Convert back to HashMap<String,Integer>
        HashMap<String, Integer> hm = c.convert(d).to(new TypeReference<HashMap<String, Integer>>() {});
        assertEquals(2, hm.size());
        assertEquals(7, (int) hm.get("amount"));
        assertEquals(100, (int) hm.get("cost"));
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testConvertWithKeysDeep() {
        MyDTO6 subsubDTO1 = new MyDTO6();
        subsubDTO1.chars = Arrays.asList('a', 'b', 'c');
        MyDTO6 subsubDTO2 = new MyDTO6();
        subsubDTO2.chars = Arrays.asList('z', 'z', 'z', 'z');
        MyDTO6 subsubDTO3 = new MyDTO6();
        subsubDTO3.chars = Arrays.asList('8');
        MyDTO6 subsubDTO4 = new MyDTO6();
        subsubDTO4.chars = Arrays.asList(' ');
        MyDTO5 subDTO1 = new MyDTO5();
        subDTO1.subsub1 = subsubDTO1;
        subDTO1.subsub2 = subsubDTO2;
        MyDTO5 subDTO2 = new MyDTO5();
        subDTO2.subsub1 = subsubDTO3;
        subDTO2.subsub2 = subsubDTO4;
        MyDTO4 dto = new MyDTO4();
        dto.sub1 = subDTO1;
        dto.sub2 = subDTO2;

        ConverterBuilder cb = converter.newConverterBuilder();
        ConvertFunction<MyDTO6, Map> fun = new ConvertFunction<MyDTO6, Map>() {
            @Override @SuppressWarnings("unchecked")
            public Map convert(MyDTO6 obj, Type targetType, Object root, Object[] keys) throws Exception {
                StringBuilder sb = new StringBuilder();
                for (Character c : obj.chars) {
                    sb.append(c);
                }

                if ("sub2".equals(keys[0]) && "subsub1".equals(keys[1])) {
                    sb.append(sb.toString());
                }

                Map m = new HashMap();
                m.put("chars", sb.toString());
                return m;
            }
        };
        cb.rule(new Rule<MyDTO6, Map>(new TypeReference<MyDTO6>() {},
                new TypeReference<Map>() {}, fun));
        Converter c = cb.build();

        Map m = c.convert(dto).to(Map.class);
        assertEquals(2, m.size());
        Map m1 = (Map) m.get("sub1");
        Map m2 = (Map) m.get("sub2");
        Map m11 = (Map) m1.get("subsub1");
        assertEquals("abc", m11.get("chars"));
        Map m12 = (Map) m1.get("subsub2");
        assertEquals("zzzz", m12.get("chars"));
        Map m21 = (Map) m2.get("subsub1");
        assertEquals("String should be doubled by special converter rule", "88", m21.get("chars"));
        Map m22 = (Map) m2.get("subsub2");
        assertEquals(" ", m22.get("chars"));
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

    static class MyCustomDTO {
        public String field;
    }
}
