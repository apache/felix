/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.dm.impl;

import static org.apache.felix.dm.impl.Configurable.*;
import static org.junit.Assert.*;

import org.apache.felix.dm.impl.Configurable;
import org.junit.Test;

import java.util.*;

/**
 * Test cases for {@link Configurable}.
 */
public class ConfigurableTest {
    private static final double ERROR_MARGIN = 0.0001;

    static enum EnumValue {
        ONE, TWO, THREE;
    }

    static interface IfaceA {
        int getInt();

        double getDouble();
        
        Byte getByte();

        String getString();

        int[] getArray();
        
        boolean isBoolean();

        List<String> getList();

        SortedMap<String, Object> getMap();

        EnumValue getEnum();
        
        Map<String, List<Number>> getMapStringList();

        List<String>[] getArrayOfList();
    }

    static interface IfaceB {
        Stack<Integer> getA();

        Set<Integer> getB();

        Queue<String> getC();
    }

    static interface IfaceC {
        IfaceA getIfaceA();
        
        Class<?> getType();
    }

    @Test
    public void testHandleImplicitDefaultValues() {
        IfaceA ifaceA = create(IfaceA.class, createMap());
        assertNotNull(ifaceA);

        assertEquals(0, ifaceA.getInt());
        assertEquals(0.0, ifaceA.getDouble(), ERROR_MARGIN);
        assertEquals(null, ifaceA.getString());
        assertArrayEquals(new int[0], ifaceA.getArray());
        assertEquals(Collections.emptyList(), ifaceA.getList());
        assertEquals(Collections.emptyMap(), ifaceA.getMap());
        assertEquals(null, ifaceA.getEnum());

        IfaceB ifaceB = create(IfaceB.class, createMap());
        assertNotNull(ifaceB);
        
        assertEquals(new Stack<>(), ifaceB.getA());
        assertEquals(Collections.emptySet(), ifaceB.getB());
        assertEquals(new LinkedList<>(), ifaceB.getC());

        IfaceC ifaceC = create(IfaceC.class, createMap());
        assertNotNull(ifaceC);

        assertNotNull(ifaceC.getIfaceA());
        assertNull(ifaceC.getType());
    }

    @Test
    public void testHandleInt() {
        IfaceA cfg = create(IfaceA.class, createMap("int", 41));
        assertNotNull(cfg);

        assertEquals(41, cfg.getInt());
    }

    @Test
    public void testHandleDouble() {
        IfaceA cfg = create(IfaceA.class, createMap("double", 3.141));
        assertNotNull(cfg);

        assertEquals(3.141, cfg.getDouble(), ERROR_MARGIN);
    }

    @Test
    public void testHandleString() {
        IfaceA cfg = create(IfaceA.class, createMap("string", "hello"));
        assertNotNull(cfg);

        assertEquals("hello", cfg.getString());
    }

    @Test
    public void testHandleEnum() {
        IfaceA cfg = create(IfaceA.class, createMap("enum", "two"));
        assertNotNull(cfg);

        assertEquals(EnumValue.TWO, cfg.getEnum());
    }

    @Test
    public void testHandleMapFromString() {
        IfaceA cfg = create(IfaceA.class, createMap("map.a", "1", "map.b", "2", "map.c", "hello world", "map.d", "[4, 5, 6]"));
        assertNotNull(cfg);

        Map<String, Object> map = cfg.getMap();
        assertEquals("1", map.get("a"));
        assertEquals("2", map.get("b"));
        assertEquals("hello world", map.get("c"));
        assertEquals("[4, 5, 6]", map.get("d"));
        
        cfg = create(IfaceA.class, createMap("map", "{a.1, b.2, c.3}"));
        assertNotNull(cfg);

        map = cfg.getMap();
        assertEquals("1", map.get("a"));
        assertEquals("2", map.get("b"));
        assertEquals("3", map.get("c"));
    }

    @Test
    public void testHandleObjectFromString() {
        IfaceC cfg = create(IfaceC.class, createMap(
            "ifaceA.array", "[4, 5, 6]", 
            "ifaceA.arrayOfList.0", "[foo, bar]",
            "ifaceA.arrayOfList.1", "[qux]",
            "ifaceA.byte", "127",
            "ifaceA.double", "3.141",
            "ifaceA.enum", "THREE", 
            "ifaceA.int", "123",
            "ifaceA.list", "[qux, quu]",
            "ifaceA.map.c", "d",
            "ifaceA.map.a", "b",
            "ifaceA.mapStringList.x", "[1, 2, 3]",
            "ifaceA.mapStringList.y", "[4, 5, 6]",
            "ifaceA.mapStringList.z", "[7, 8, 9]",
            "ifaceA.string", "hello world", 
            "ifaceA.boolean", "true",
            "type", "java.lang.String"));
        assertNotNull(cfg);
        
        SortedMap<String, Object> sortedMap = new TreeMap<>();
        sortedMap.put("a", "b");
        sortedMap.put("c", "d");
        
        Map<String, List<Number>> mapStringList = new HashMap<>();
        mapStringList.put("x", Arrays.<Number> asList(1L, 2L, 3L));
        mapStringList.put("y", Arrays.<Number> asList(4L, 5L, 6L));
        mapStringList.put("z", Arrays.<Number> asList(7L, 8L, 9L));

        assertEquals(String.class, cfg.getType());

        IfaceA ifaceA = cfg.getIfaceA();
        assertNotNull(ifaceA);

        assertArrayEquals(new int[] { 4, 5, 6 }, ifaceA.getArray());
        assertArrayEquals(new List[] { Arrays.asList("foo", "bar"), Arrays.asList("qux") }, ifaceA.getArrayOfList());
        assertEquals(Byte.valueOf("127"), ifaceA.getByte());
        assertEquals(3.141, ifaceA.getDouble(), ERROR_MARGIN);
        assertEquals(EnumValue.THREE, ifaceA.getEnum());
        assertEquals(123, ifaceA.getInt());
        assertEquals(Arrays.asList("qux", "quu"), ifaceA.getList());
        assertEquals(sortedMap, ifaceA.getMap());
        assertEquals(mapStringList, ifaceA.getMapStringList());
        assertEquals("hello world", ifaceA.getString());
        assertEquals(true, ifaceA.isBoolean());
    }

    @Test
    public void testHandleArrayDirect() {
        IfaceA cfg = create(IfaceA.class, createMap("array", new int[] { 2, 3, 4, 5 }));
        assertNotNull(cfg);

        int[] vals = cfg.getArray();
        assertEquals(2, vals[0]);
        assertEquals(3, vals[1]);
        assertEquals(4, vals[2]);
        assertEquals(5, vals[3]);
    }

    @Test
    public void testHandleArrayFromString() {
        IfaceA cfg = create(IfaceA.class, createMap("array", "[2,3,4,5]"));
        assertNotNull(cfg);

        int[] vals = cfg.getArray();
        assertEquals(2, vals[0]);
        assertEquals(3, vals[1]);
        assertEquals(4, vals[2]);
        assertEquals(5, vals[3]);
    }

    @Test
    public void testHandleListDirect() {
        IfaceA cfg = create(IfaceA.class, createMap("list", Arrays.asList("2", "3", "4", "5")));
        assertNotNull(cfg);

        List<String> list = cfg.getList();
        assertEquals("2", list.get(0));
        assertEquals("3", list.get(1));
        assertEquals("4", list.get(2));
        assertEquals("5", list.get(3));
    }

    @Test
    public void testHandleListFromString() {
        IfaceA cfg = create(IfaceA.class, createMap("list", "[2,3,4,5]"));
        assertNotNull(cfg);

        List<String> list = cfg.getList();
        assertEquals("2", list.get(0));
        assertEquals("3", list.get(1));
        assertEquals("4", list.get(2));
        assertEquals("5", list.get(3));
    }

    private static Map<?, ?> createMap(Object... vals) {
        Map<String, Object> result = new HashMap<>();
        for (int i = 0; i < vals.length; i += 2) {
            result.put(vals[i].toString(), vals[i + 1]);
        }
        return result;
    }
}
