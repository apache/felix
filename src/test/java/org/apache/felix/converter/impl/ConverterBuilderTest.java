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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.ConverterBuilder;
import org.osgi.util.converter.Rule;
import org.osgi.util.converter.TypeRule;
import org.osgi.util.function.Function;

import static org.apache.felix.converter.impl.Helper.convert;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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
        Converter ca = cb.
                rule(new TypeRule<String[],String>(String[].class, String.class,
                        v -> Stream.of(v).collect(Collectors.joining(",")))).
                rule(new TypeRule<String,String[]>(String.class, String[].class,
                        v -> v.split(","))).
                build();

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
        cb.rule(new TypeRule<>(char[].class, String.class, ConverterBuilderTest::convertToString));
        cb.rule(Integer.class, convert((f,t) -> -1));
        cb.rule(Long.class, convert((f,t) -> -1L));
        Converter ca = cb.build();

        assertEquals("hi", ca.convert(new char[] {'h', 'i'}).to(String.class));
        assertEquals(Integer.valueOf(-1), ca.convert("Hello").to(Integer.class));
        assertEquals(Long.valueOf(-1), ca.convert("Hello").to(Long.class));

        // Shadow the Integer variant but keep Long going to the Number variant.
        Converter ca2 = ca.newConverterBuilder().rule(
                new TypeRule<String,Integer>(String.class, Integer.class, s -> s.length())).build();
        assertEquals(5, (int) ca2.convert("Hello").to(Integer.class));
        assertEquals(Long.valueOf(-1), ca2.convert("Hello").to(Long.class));
    }

    @Test
    public void testCannotHandleSpecific() {
        Converter ca = converter.newConverterBuilder().rule(
            new TypeRule<>(Integer.class, Long.class, new Function<Integer,Long>() {
                @Override
                public Long apply(Integer obj) {
                    if (obj.intValue() != 1)
                        return new Long(-obj.intValue());
                    return null;
                }
            })).build();


        assertEquals(Long.valueOf(-2), ca.convert(Integer.valueOf(2)).to(Long.class));

        // This is the exception that the rule cannot handle
        assertEquals(Long.valueOf(1), ca.convert(Integer.valueOf(1)).to(Long.class));
    }

    @Test
    public void testWildcardAdapter() {
        Helper.ConvertFunctionConverter<Object> foo = new Helper.ConvertFunctionConverter<Object>() {
            @Override
            public Object convert(Object obj, Type type) throws Exception {
                if (!(obj instanceof List))
                    return null;

                List<?> t = (List<?>) obj;
                if (type instanceof Class) {
                    if (Number.class.isAssignableFrom((Class<?>) type))
                        return converter.convert(t.size()).to(type);
                }
                return null;
            }
        };

        ConverterBuilder cb = converter.newConverterBuilder();
        cb.rule(convert(foo));
        cb.rule(convert((v,t) -> v.toString()));
        Converter ca = cb.build();

        assertEquals(3L, (long) ca.convert(Arrays.asList("a", "b", "c")).to(Long.class));
        assertEquals(3, (long) ca.convert(Arrays.asList("a", "b", "c")).to(Integer.class));
        assertEquals("[a, b, c]", ca.convert(Arrays.asList("a", "b", "c")).to(String.class));
    }

    @Test
    public void testWildcardAdapter1() {
        Helper.ConvertFunctionConverter<Object> foo = new Helper.ConvertFunctionConverter<Object>() {
            @Override
            public Object convert(Object obj, Type type) throws Exception {
                if (!(obj instanceof List))
                    return null;

                List<?> t = (List<?>) obj;
                if (type instanceof Class) {
                    if (Number.class.isAssignableFrom((Class<?>) type))
                        return converter.convert(t.size()).to(type);
                }
                return null;
            }
        };

        ConverterBuilder cb = converter.newConverterBuilder();
        cb.rule(convert((v,t) -> converter.convert(1).to(t)));
        cb.rule(convert(foo));
        Converter ca = cb.build();

        // The catchall converter should be called always because it can handle all and was registered first
        assertEquals(1L, (long) ca.convert(Arrays.asList("a", "b", "c")).to(Long.class));
        assertEquals(1, (int) ca.convert(Arrays.asList("a", "b", "c")).to(Integer.class));
        assertEquals("1", ca.convert(Arrays.asList("a", "b", "c")).to(String.class));
    }

    @Test
    public void testWildcardAdapter2() {
        Map<Object, Object> snooped = new HashMap<>();
        ConverterBuilder cb = converter.newConverterBuilder();
        cb.rule(new Rule<String[],ArrayList<String>>(v -> {
                Arrays.sort(v, Collections.reverseOrder());
                return new ArrayList<>(Arrays.asList(v));
            }) {});
        cb.rule(new Rule<String[],List<String>>(v -> {
                Arrays.sort(v, Collections.reverseOrder());
                return new CopyOnWriteArrayList<>(Arrays.asList(v));
            }) {});
        cb.rule(convert((v,t) -> { snooped.put(v,t); return null;}));
        Converter ca = cb.build();

        assertEquals(new ArrayList<>(Arrays.asList("c", "b", "a")), ca.convert(
                new String [] {"a", "b", "c"}).to(ArrayList.class));
        assertEquals("Precondition", 0, snooped.size());
        String[] sa0 = new String [] {"a", "b", "c"};
        assertEquals(new LinkedList<>(Arrays.asList("a", "b", "c")), ca.convert(
                sa0).to(LinkedList.class));
        assertEquals(1, snooped.size());
        assertEquals(LinkedList.class, snooped.get(sa0));
        assertEquals(new CopyOnWriteArrayList<>(Arrays.asList("c", "b", "a")), ca.convert(
                new String [] {"a", "b", "c"}).to(List.class));

        snooped.clear();
        String[] sa = new String [] {"a", "b", "c"};
        assertEquals(new CopyOnWriteArrayList<>(Arrays.asList("a", "b", "c")), ca.convert(
                sa).to(CopyOnWriteArrayList.class));
        assertEquals(1, snooped.size());
        assertEquals(CopyOnWriteArrayList.class, snooped.get(sa));
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
