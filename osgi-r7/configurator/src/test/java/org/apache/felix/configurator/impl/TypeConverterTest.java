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
package org.apache.felix.configurator.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

import org.apache.felix.configurator.impl.json.JSONUtil;
import org.apache.felix.configurator.impl.json.JSONUtilTest;
import org.junit.Test;

public class TypeConverterTest {

    @Test public void testStringConversionNoTypeInfo() throws IOException {
        final String v_String = "world";
        final TypeConverter converter = new TypeConverter(null);
        final Object result = converter.convert(v_String, null);
        assertTrue(result instanceof String);
        assertEquals(v_String, result);
    }

    @Test public void testLongConversionNoTypeInfo() throws IOException {
        final long v_long = 3;
        final TypeConverter converter = new TypeConverter(null);
        final Object result = converter.convert(v_long, null);
        assertTrue(result instanceof Long);
        assertEquals(v_long, result);
    }

    @Test public void testIntegerConversionNoTypeInfo() throws IOException {
        final int v_int = 3;
        final TypeConverter converter = new TypeConverter(null);
        final Object result = converter.convert(v_int, null);
        assertTrue(result instanceof Long);
        assertEquals(3L, result);
    }

    @Test public void testShortConversionNoTypeInfo() throws IOException {
        final short v_short = 3;
        final TypeConverter converter = new TypeConverter(null);
        final Object result = converter.convert(v_short, null);
        assertTrue(result instanceof Long);
        assertEquals(3L, result);
    }

    @Test public void testByteConversionNoTypeInfo() throws IOException {
        final byte v_byte = 3;
        final TypeConverter converter = new TypeConverter(null);
        final Object result = converter.convert(v_byte, null);
        assertTrue(result instanceof Long);
        assertEquals(3L, result);
    }

    @Test public void testCharConversionNoTypeInfo() throws IOException {
        final char v_char = 'a';
        final TypeConverter converter = new TypeConverter(null);
        assertNull(converter.convert(v_char, null));
    }

    @Test public void testCharacterConversionNoTypeInfo() throws IOException {
        final Character v_Character = new Character('a');
        final TypeConverter converter = new TypeConverter(null);
        assertNull(converter.convert(v_Character, null));
    }

    @Test public void testFloatConversionNoTypeInfo() throws IOException {
        final float v_float = 3.1f;
        final TypeConverter converter = new TypeConverter(null);
        final Object result = converter.convert(v_float, null);
        assertTrue(result instanceof Double);
    }

    @Test public void testDoubleConversionNoTypeInfo() throws IOException {
        final double v_double = 3.0;
        final TypeConverter converter = new TypeConverter(null);
        final Object result = converter.convert(v_double, null);
        assertTrue(result instanceof Double);
        assertEquals(v_double, result);
    }

    @Test public void testSimpleTypeConversions() throws Exception {
        final TypeConverter converter = new TypeConverter(null);

        final Map config = JSONUtil.parseJSON("a", JSONUtilTest.readJSON("json/simple-types.json"));
        final Map properties = (Map)config.get("config");

        assertTrue(converter.convert(properties.get("string"), null) instanceof String);
        assertTrue(converter.convert(properties.get("boolean"), null) instanceof Boolean);
        assertTrue(converter.convert(properties.get("number"), null) instanceof Long);
        assertTrue(converter.convert(properties.get("float"), null) instanceof Double);

        // arrays
        assertTrue(converter.convert(properties.get("string.array"), null).getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("string.array"), null), 0) instanceof String);
        assertTrue(Array.get(converter.convert(properties.get("string.array"), null), 1) instanceof String);

        assertTrue(converter.convert(properties.get("boolean.array"), null).getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("boolean.array"), null), 0) instanceof Boolean);
        assertTrue(Array.get(converter.convert(properties.get("boolean.array"), null), 1) instanceof Boolean);

        assertTrue(converter.convert(properties.get("number.array"), null).getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("number.array"), null), 0) instanceof Long);
        assertTrue(Array.get(converter.convert(properties.get("number.array"), null), 1) instanceof Long);

        assertTrue(converter.convert(properties.get("float.array"), null).getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("float.array"), null), 0) instanceof Double);
        assertTrue(Array.get(converter.convert(properties.get("float.array"), null), 1) instanceof Double);
    }

    @Test public void testSimpleTypeConversionsWithTypeHint() throws Exception {
        final TypeConverter converter = new TypeConverter(null);

        final Map config = JSONUtil.parseJSON("a", JSONUtilTest.readJSON("json/simple-types.json"));
        final Map properties = (Map)config.get("config");

        assertTrue(converter.convert(properties.get("string"), "String") instanceof String);
        assertTrue(converter.convert(properties.get("boolean"), "Boolean") instanceof Boolean);
        assertTrue(converter.convert(properties.get("boolean"), "boolean") instanceof Boolean);
        assertTrue(converter.convert(properties.get("number"), "Integer") instanceof Integer);
        assertTrue(converter.convert(properties.get("number"), "int") instanceof Integer);
        assertTrue(converter.convert(properties.get("number"), "Long") instanceof Long);
        assertTrue(converter.convert(properties.get("number"), "long") instanceof Long);
        assertTrue(converter.convert(properties.get("float"), "Double") instanceof Double);
        assertTrue(converter.convert(properties.get("float"), "double") instanceof Double);
        assertTrue(converter.convert(properties.get("float"), "Float") instanceof Float);
        assertTrue(converter.convert(properties.get("float"), "float") instanceof Float);
        assertTrue(converter.convert(properties.get("number"), "Byte") instanceof Byte);
        assertTrue(converter.convert(properties.get("number"), "byte") instanceof Byte);
        assertTrue(converter.convert(properties.get("number"), "Short") instanceof Short);
        assertTrue(converter.convert(properties.get("number"), "short") instanceof Short);
        assertTrue(converter.convert(properties.get("string"), "Character") instanceof Character);
        assertTrue(converter.convert(properties.get("string"), "char") instanceof Character);

        // arrays
        assertTrue(converter.convert(properties.get("string.array"), "String[]").getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("string.array"), "String[]"), 0) instanceof String);
        assertTrue(Array.get(converter.convert(properties.get("string.array"), "String[]"), 1) instanceof String);

        assertTrue(converter.convert(properties.get("boolean.array"), "Boolean[]").getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("boolean.array"), "Boolean[]"), 0) instanceof Boolean);
        assertTrue(Array.get(converter.convert(properties.get("boolean.array"), "Boolean[]"), 1) instanceof Boolean);

        // the following would throw class cast exceptions
        boolean[] a0 = (boolean[])converter.convert(properties.get("boolean.array"), "boolean[]");
        assertNotNull(a0);
        int[] a1 = (int[])converter.convert(properties.get("number.array"), "int[]");
        assertNotNull(a1);
        long[] a2 = (long[])converter.convert(properties.get("number.array"), "long[]");
        assertNotNull(a2);
        double[] a3 = (double[])converter.convert(properties.get("float.array"), "double[]");
        assertNotNull(a3);
        float[] a4 = (float[])converter.convert(properties.get("float.array"), "float[]");
        assertNotNull(a4);
        byte[] a5 = (byte[])converter.convert(properties.get("number.array"), "byte[]");
        assertNotNull(a5);
        short[] a6 = (short[])converter.convert(properties.get("number.array"), "short[]");
        assertNotNull(a6);
        char[] a7 = (char[])converter.convert(properties.get("string.array"), "char[]");
        assertNotNull(a7);

        assertTrue(converter.convert(properties.get("number.array"), "Integer[]").getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("number.array"), "Integer[]"), 0) instanceof Integer);
        assertTrue(Array.get(converter.convert(properties.get("number.array"), "Integer[]"), 1) instanceof Integer);

        assertTrue(converter.convert(properties.get("number.array"), "Long[]").getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("number.array"), "Long[]"), 0) instanceof Long);
        assertTrue(Array.get(converter.convert(properties.get("number.array"), "Long[]"), 1) instanceof Long);

        assertTrue(converter.convert(properties.get("number.array"), "Byte[]").getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("number.array"), "Byte[]"), 0) instanceof Byte);
        assertTrue(Array.get(converter.convert(properties.get("number.array"), "Byte[]"), 1) instanceof Byte);

        assertTrue(converter.convert(properties.get("number.array"), "Short[]").getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("number.array"), "Short[]"), 0) instanceof Short);
        assertTrue(Array.get(converter.convert(properties.get("number.array"), "Short[]"), 1) instanceof Short);

        assertTrue(converter.convert(properties.get("float.array"), "Float[]").getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("float.array"), "Float[]"), 0) instanceof Float);
        assertTrue(Array.get(converter.convert(properties.get("float.array"), "Float[]"), 1) instanceof Float);

        assertTrue(converter.convert(properties.get("float.array"), "Double[]").getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("float.array"), "Double[]"), 0) instanceof Double);
        assertTrue(Array.get(converter.convert(properties.get("float.array"), "Double[]"), 1) instanceof Double);

        assertTrue(converter.convert(properties.get("string.array"), "Character[]").getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("string.array"), "Character[]"), 0) instanceof Character);
        assertTrue(Array.get(converter.convert(properties.get("string.array"), "Character[]"), 1) instanceof Character);
    }

    @SuppressWarnings("unchecked")
    @Test public void testCollectionTypeConversion() throws Exception {
        final TypeConverter converter = new TypeConverter(null);
        final Map config = JSONUtil.parseJSON("a", JSONUtilTest.readJSON("json/simple-types.json"));
        final Map properties = (Map)config.get("config");

        assertTrue(converter.convert(properties.get("string.array"), "Collection<String>") instanceof Collection<?>);
        assertTrue(((Collection<String>)converter.convert(properties.get("string.array"), "Collection<String>")).iterator().next() instanceof String);

        assertTrue(converter.convert(properties.get("number.array"), "Collection<Integer>") instanceof Collection<?>);
        assertTrue(((Collection<Integer>)converter.convert(properties.get("number.array"), "Collection<Integer>")).iterator().next() instanceof Integer);

        assertTrue(converter.convert(properties.get("number.array"), "Collection<Long>") instanceof Collection<?>);
        assertTrue(((Collection<Long>)converter.convert(properties.get("number.array"), "Collection<Long>")).iterator().next() instanceof Long);

        assertTrue(converter.convert(properties.get("float.array"), "Collection<Float>") instanceof Collection<?>);
        assertTrue(((Collection<Float>)converter.convert(properties.get("float.array"), "Collection<Float>")).iterator().next() instanceof Float);

        assertTrue(converter.convert(properties.get("float.array"), "Collection<Double>") instanceof Collection<?>);
        assertTrue(((Collection<Double>)converter.convert(properties.get("float.array"), "Collection<Double>")).iterator().next() instanceof Double);

        assertTrue(converter.convert(properties.get("number.array"), "Collection<Short>") instanceof Collection<?>);
        assertTrue(((Collection<Short>)converter.convert(properties.get("number.array"), "Collection<Short>")).iterator().next() instanceof Short);

        assertTrue(converter.convert(properties.get("number.array"), "Collection<Byte>") instanceof Collection<?>);
        assertTrue(((Collection<Byte>)converter.convert(properties.get("number.array"), "Collection<Byte>")).iterator().next() instanceof Byte);

        assertTrue(converter.convert(properties.get("string.array"), "Collection<Character>") instanceof Collection<?>);
        assertTrue(((Collection<Character>)converter.convert(properties.get("string.array"), "Collection<Character>")).iterator().next() instanceof Character);

        assertTrue(converter.convert(properties.get("boolean.array"), "Collection<Boolean>") instanceof Collection<?>);
        assertTrue(((Collection<Boolean>)converter.convert(properties.get("boolean.array"), "Collection<Boolean>")).iterator().next() instanceof Boolean);
    }
}
