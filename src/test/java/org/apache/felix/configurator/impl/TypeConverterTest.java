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

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

import org.apache.felix.configurator.impl.yaml.YAMLUtil;
import org.apache.felix.configurator.impl.yaml.YAMLUtilTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TypeConverterTest {

    @Test public void testStringConversionNoTypeInfo() throws IOException {
        final String v_String = "world";
        final TypeConverter converter = new TypeConverter(null);
        final Object result = converter.convert(v_String, null, null);
        assertTrue(result instanceof String);
        assertEquals(v_String, result);
    }

    @Test public void testLongConversionNoTypeInfo() throws IOException {
        final long v_long = 3;
        final TypeConverter converter = new TypeConverter(null);
        final Object result = converter.convert(v_long, null, null);
        assertTrue(result instanceof Long);
        assertEquals(v_long, result);
    }

    @Test public void testIntegerConversionNoTypeInfo() throws IOException {
        final int v_int = 3;
        final TypeConverter converter = new TypeConverter(null);
        final Object result = converter.convert(v_int, null, null);
        assertTrue(result instanceof Long);
        assertEquals(3L, result);
    }

    @Test public void testShortConversionNoTypeInfo() throws IOException {
        final short v_short = 3;
        final TypeConverter converter = new TypeConverter(null);
        final Object result = converter.convert(v_short, null, null);
        assertTrue(result instanceof Long);
        assertEquals(3L, result);
    }

    @Test public void testByteConversionNoTypeInfo() throws IOException {
        final byte v_byte = 3;
        final TypeConverter converter = new TypeConverter(null);
        final Object result = converter.convert(v_byte, null, null);
        assertTrue(result instanceof Long);
        assertEquals(3L, result);
    }

    @Test public void testCharConversionNoTypeInfo() throws IOException {
        final char v_char = 'a';
        final TypeConverter converter = new TypeConverter(null);
        final Object result = converter.convert(v_char, "a", null);
        assertTrue(result instanceof String);
        assertEquals("a", result);
    }

    @Test public void testCharacterConversionNoTypeInfo() throws IOException {
        final Character v_Character = new Character('a');
        final TypeConverter converter = new TypeConverter(null);
        final Object result = converter.convert(v_Character, "a", null);
        assertTrue(result instanceof String);
        assertEquals("a", result);
    }

    @Test public void testFloatConversionNoTypeInfo() throws IOException {
        final float v_float = 3.1f;
        final TypeConverter converter = new TypeConverter(null);
        final Object result = converter.convert(v_float, null, null);
        assertTrue(result instanceof Double);
    }

    @Test public void testDoubleConversionNoTypeInfo() throws IOException {
        final double v_double = 3.0;
        final TypeConverter converter = new TypeConverter(null);
        final Object result = converter.convert(v_double, null, null);
        assertTrue(result instanceof Double);
        assertEquals(v_double, result);
    }

    @Test public void testSimpleTypeConversions() throws Exception {
        final TypeConverter converter = new TypeConverter(null);

        final Object obj = YAMLUtil.parseYAML("a", YAMLUtilTest.readYAML("yaml/simple-types.yaml"));
        @SuppressWarnings("unchecked")
        final Map<String, Object> config = (Map<String, Object>)obj;
        @SuppressWarnings("unchecked")
        final Map<String, Object> properties = (Map<String, Object>)config.get("config");

        assertTrue(converter.convert(properties.get("string"), null, null) instanceof String);
        assertTrue(converter.convert(properties.get("boolean"), null, null) instanceof Boolean);
        assertTrue(converter.convert(properties.get("number"), null, null) instanceof Long);
        assertTrue(converter.convert(properties.get("float"), null, null) instanceof Double);

        // arrays
        assertTrue(converter.convert(properties.get("string.array"), null, null).getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("string.array"), null, null), 0) instanceof String);
        assertTrue(Array.get(converter.convert(properties.get("string.array"), null, null), 1) instanceof String);

        assertTrue(converter.convert(properties.get("boolean.array"), null, null).getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("boolean.array"), null, null), 0) instanceof Boolean);
        assertTrue(Array.get(converter.convert(properties.get("boolean.array"), null, null), 1) instanceof Boolean);

        assertTrue(converter.convert(properties.get("number.array"), null, null).getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("number.array"), null, null), 0) instanceof Long);
        assertTrue(Array.get(converter.convert(properties.get("number.array"), null, null), 1) instanceof Long);

        assertTrue(converter.convert(properties.get("float.array"), null, null).getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("float.array"), null, null), 0) instanceof Double);
        assertTrue(Array.get(converter.convert(properties.get("float.array"), null, null), 1) instanceof Double);
    }

    @Test public void testSimpleTypeConversionsWithTypeHint() throws Exception {
        final TypeConverter converter = new TypeConverter(null);

        final Object obj = YAMLUtil.parseYAML("a", YAMLUtilTest.readYAML("yaml/simple-types.yaml"));
        @SuppressWarnings("unchecked")
        final Map<String, Object> config = (Map<String, Object>)obj;
        @SuppressWarnings("unchecked")
        final Map<String, Object> properties = (Map<String, Object>)config.get("config");

        assertTrue(converter.convert(properties.get("string"), null, "String") instanceof String);
        assertTrue(converter.convert(properties.get("boolean"), null, "Boolean") instanceof Boolean);
        assertTrue(converter.convert(properties.get("boolean"), null, "boolean") instanceof Boolean);
        assertTrue(converter.convert(properties.get("number"), null, "Integer") instanceof Integer);
        assertTrue(converter.convert(properties.get("number"), null, "int") instanceof Integer);
        assertTrue(converter.convert(properties.get("number"), null, "Long") instanceof Long);
        assertTrue(converter.convert(properties.get("number"), null, "long") instanceof Long);
        assertTrue(converter.convert(properties.get("float"), null, "Double") instanceof Double);
        assertTrue(converter.convert(properties.get("float"), null, "double") instanceof Double);
        assertTrue(converter.convert(properties.get("float"), null, "Float") instanceof Float);
        assertTrue(converter.convert(properties.get("float"), null, "float") instanceof Float);
        assertTrue(converter.convert(properties.get("number"), null, "Byte") instanceof Byte);
        assertTrue(converter.convert(properties.get("number"), null, "byte") instanceof Byte);
        assertTrue(converter.convert(properties.get("number"), null, "Short") instanceof Short);
        assertTrue(converter.convert(properties.get("number"), null, "short") instanceof Short);
        assertTrue(converter.convert(properties.get("string"), null, "Character") instanceof Character);
        assertTrue(converter.convert(properties.get("string"), null, "char") instanceof Character);

        // arrays
        assertTrue(converter.convert(properties.get("string.array"), null, "String[]").getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("string.array"), null, "String[]"), 0) instanceof String);
        assertTrue(Array.get(converter.convert(properties.get("string.array"), null, "String[]"), 1) instanceof String);

        assertTrue(converter.convert(properties.get("boolean.array"), null, "Boolean[]").getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("boolean.array"), null, "Boolean[]"), 0) instanceof Boolean);
        assertTrue(Array.get(converter.convert(properties.get("boolean.array"), null, "Boolean[]"), 1) instanceof Boolean);

        // the following would throw class cast exceptions
        boolean[] a0 = (boolean[])converter.convert(properties.get("boolean.array"), null, "boolean[]");
        assertNotNull(a0);
        int[] a1 = (int[])converter.convert(properties.get("number.array"), null, "int[]");
        assertNotNull(a1);
        long[] a2 = (long[])converter.convert(properties.get("number.array"), null, "long[]");
        assertNotNull(a2);
        double[] a3 = (double[])converter.convert(properties.get("float.array"), null, "double[]");
        assertNotNull(a3);
        float[] a4 = (float[])converter.convert(properties.get("float.array"), null, "float[]");
        assertNotNull(a4);
        byte[] a5 = (byte[])converter.convert(properties.get("number.array"), null, "byte[]");
        assertNotNull(a5);
        short[] a6 = (short[])converter.convert(properties.get("number.array"), null, "short[]");
        assertNotNull(a6);
        char[] a7 = (char[])converter.convert(properties.get("string.array"), null, "char[]");
        assertNotNull(a7);

        assertTrue(converter.convert(properties.get("number.array"), null, "Integer[]").getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("number.array"), null, "Integer[]"), 0) instanceof Integer);
        assertTrue(Array.get(converter.convert(properties.get("number.array"), null, "Integer[]"), 1) instanceof Integer);

        assertTrue(converter.convert(properties.get("number.array"), null, "Long[]").getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("number.array"), null, "Long[]"), 0) instanceof Long);
        assertTrue(Array.get(converter.convert(properties.get("number.array"), null, "Long[]"), 1) instanceof Long);

        assertTrue(converter.convert(properties.get("number.array"), null, "Byte[]").getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("number.array"), null, "Byte[]"), 0) instanceof Byte);
        assertTrue(Array.get(converter.convert(properties.get("number.array"), null, "Byte[]"), 1) instanceof Byte);

        assertTrue(converter.convert(properties.get("number.array"), null, "Short[]").getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("number.array"), null, "Short[]"), 0) instanceof Short);
        assertTrue(Array.get(converter.convert(properties.get("number.array"), null, "Short[]"), 1) instanceof Short);

        assertTrue(converter.convert(properties.get("float.array"), null, "Float[]").getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("float.array"), null, "Float[]"), 0) instanceof Float);
        assertTrue(Array.get(converter.convert(properties.get("float.array"), null, "Float[]"), 1) instanceof Float);

        assertTrue(converter.convert(properties.get("float.array"), null, "Double[]").getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("float.array"), null, "Double[]"), 0) instanceof Double);
        assertTrue(Array.get(converter.convert(properties.get("float.array"), null, "Double[]"), 1) instanceof Double);

        assertTrue(converter.convert(properties.get("string.array"), null, "Character[]").getClass().isArray());
        assertTrue(Array.get(converter.convert(properties.get("string.array"), null, "Character[]"), 0) instanceof Character);
        assertTrue(Array.get(converter.convert(properties.get("string.array"), null, "Character[]"), 1) instanceof Character);
    }

    @SuppressWarnings("unchecked")
    @Test public void testCollectionTypeConversion() throws Exception {
        final TypeConverter converter = new TypeConverter(null);
        final Object obj = YAMLUtil.parseYAML("a", YAMLUtilTest.readYAML("yaml/simple-types.yaml"));
        final Map<String, Object> config = (Map<String, Object>)obj;
        final Map<String, Object> properties = (Map<String, Object>)config.get("config");

        assertTrue(converter.convert(properties.get("string.array"), null, "Collection<String>") instanceof Collection<?>);
        assertTrue(((Collection<String>)converter.convert(properties.get("string.array"), null, "Collection<String>")).iterator().next() instanceof String);

        assertTrue(converter.convert(properties.get("number.array"), null, "Collection<Integer>") instanceof Collection<?>);
        assertTrue(((Collection<Integer>)converter.convert(properties.get("number.array"), null, "Collection<Integer>")).iterator().next() instanceof Integer);

        assertTrue(converter.convert(properties.get("number.array"), null, "Collection<Long>") instanceof Collection<?>);
        assertTrue(((Collection<Long>)converter.convert(properties.get("number.array"), null, "Collection<Long>")).iterator().next() instanceof Long);

        assertTrue(converter.convert(properties.get("float.array"), null, "Collection<Float>") instanceof Collection<?>);
        assertTrue(((Collection<Float>)converter.convert(properties.get("float.array"), null, "Collection<Float>")).iterator().next() instanceof Float);

        assertTrue(converter.convert(properties.get("float.array"), null, "Collection<Double>") instanceof Collection<?>);
        assertTrue(((Collection<Double>)converter.convert(properties.get("float.array"), null, "Collection<Double>")).iterator().next() instanceof Double);

        assertTrue(converter.convert(properties.get("number.array"), null, "Collection<Short>") instanceof Collection<?>);
        assertTrue(((Collection<Short>)converter.convert(properties.get("number.array"), null, "Collection<Short>")).iterator().next() instanceof Short);

        assertTrue(converter.convert(properties.get("number.array"), null, "Collection<Byte>") instanceof Collection<?>);
        assertTrue(((Collection<Byte>)converter.convert(properties.get("number.array"), null, "Collection<Byte>")).iterator().next() instanceof Byte);

        assertTrue(converter.convert(properties.get("string.array"), null, "Collection<Character>") instanceof Collection<?>);
        assertTrue(((Collection<Character>)converter.convert(properties.get("string.array"), null, "Collection<Character>")).iterator().next() instanceof Character);

        assertTrue(converter.convert(properties.get("boolean.array"), null, "Collection<Boolean>") instanceof Collection<?>);
        assertTrue(((Collection<Boolean>)converter.convert(properties.get("boolean.array"), null, "Collection<Boolean>")).iterator().next() instanceof Boolean);
    }
}
