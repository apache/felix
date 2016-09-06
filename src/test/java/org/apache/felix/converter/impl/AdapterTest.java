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
import org.osgi.service.converter.Adapter;
import org.osgi.service.converter.ConvertFunction;
import org.osgi.service.converter.ConvertResult;
import org.osgi.service.converter.Converter;
import org.osgi.service.converter.Rule;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class AdapterTest {
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
        Adapter ca = converter.newAdapter();
        ca.rule(String[].class, String.class,
                v -> Stream.of(v).collect(Collectors.joining(",")),
                v -> v.split(","));

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
        Adapter ca = converter.newAdapter();

        ca.rule(char[].class, String.class, AdapterTest::convertToString, null);
        ca.rule(new Rule<String, Number>(String.class, Number.class, new ConvertFunction<String, Number>() {
            @Override
            public ConvertResult<Number> convert(String obj, Type targetType) throws Exception {
                if (Integer.class.equals(targetType))
                    return new ConvertResult<Number>(Integer.valueOf(-1));
                else if (Long.class.equals(targetType))
                    return new ConvertResult<Number>(Long.valueOf(-1));
                return null;
            }
        }));

        assertEquals("hi", ca.convert(new char[] {'h', 'i'}).to(String.class));
        assertEquals(Integer.valueOf(-1), ca.convert("Hello").to(Integer.class));
        assertEquals(Long.valueOf(-1), ca.convert("Hello").to(Long.class));

        Adapter ca2 = ca.newAdapter();
        // Shadow the Integer variant but keep Long going to the Number variant.
        ca2.rule(String.class, Integer.class, v -> v.length(), null);
        assertEquals(5, (int) ca2.convert("Hello").to(Integer.class));
        assertEquals(Long.valueOf(-1), ca2.convert("Hello").to(Long.class));
    }

    @Test
    public void testCannotHandleSpecific() {
        Adapter ca = converter.newAdapter();

        ca.rule(new Rule<Integer, Long>(Integer.class, Long.class, new ConvertFunction<Integer,Long>() {
            @Override
            public ConvertResult<Long> convert(Integer obj, Type targetType) throws Exception {
                if (obj.intValue() != 1)
                    return new ConvertResult<Long>(new Long(-obj.intValue()));
                return ConvertResult.cannotConvert();
            }
        }));

        assertEquals(Long.valueOf(-2), ca.convert(Integer.valueOf(2)).to(Long.class));

        // This is the exception that the rule cannot handle
        assertEquals(Long.valueOf(1), ca.convert(Integer.valueOf(1)).to(Long.class));
    }

    @Test @SuppressWarnings("rawtypes")
    public void testWildcardAdapter() {
        ConvertFunction<List, Object> foo = new ConvertFunction<List, Object>() {
            @Override
            public ConvertResult<Object> convert(List t, Type type) throws Exception {
                if (type instanceof Class) {
                    if (Number.class.isAssignableFrom((Class<?>) type))
                        return new ConvertResult<Object>(converter.convert(t.size()).to(type));
                }
                return ConvertResult.cannotConvert();
            }
        };

        Rule<List, Object> r = new Rule<>(List.class, Object.class, foo);
        Rule<Object, Object> allCatch = new Rule<>(Object.class, Object.class,
                (v,t) -> new ConvertResult<Object>(v.toString()));

        Adapter ca = converter.newAdapter();
        ca.rule(r);
        ca.rule(allCatch);

        assertEquals(3L, (long) ca.convert(Arrays.asList("a", "b", "c")).to(Long.class));
        assertEquals(3, (long) ca.convert(Arrays.asList("a", "b", "c")).to(Integer.class));
        assertEquals("[a, b, c]", ca.convert(Arrays.asList("a", "b", "c")).to(String.class));
    }

    @Test @SuppressWarnings("rawtypes")
    public void testWildcardAdapter2() {
        Map<Object, Object> snooped = new HashMap<>();
        Rule<Object, ArrayList> r = new Rule<>(Object.class, ArrayList.class,
                (v,t) -> new ConvertResult<ArrayList>(null),
                (v,t) -> new ConvertResult<Object>("arraylist"));
        Rule<Object, List> r2 = new Rule<>(Object.class, List.class,
                (v,t) -> new ConvertResult<List>(null),
                (v,t) -> new ConvertResult<Object>("list"));
        Rule<Object, Object> allCatch = new Rule<>(Object.class, Object.class,
                (v,t) -> {snooped.put(v,t); return ConvertResult.cannotConvert();}, null);

        Adapter ca = converter.newAdapter();
        ca.rule(r);
        ca.rule(r2);
        ca.rule(allCatch);

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
}
