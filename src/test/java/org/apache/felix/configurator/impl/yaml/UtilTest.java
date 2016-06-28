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
package org.apache.felix.configurator.impl.yaml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.net.URL;
import java.util.Collection;
import java.util.Map;

import org.apache.felix.configurator.impl.model.ConfigurationFile;
import org.junit.Test;

public class UtilTest {

    /** Read the  model from that name */
    public static String readYAML(final String name) throws Exception {

        try ( final Reader reader = new InputStreamReader(UtilTest.class.getResourceAsStream("/" + name), "UTF-8");
              final Writer writer = new StringWriter()) {

            final char[] buf = new char[2048];
            int len = 0;
            while ((len = reader.read(buf)) > 0) {
                writer.write(buf, 0, len);
            }

            return writer.toString();
        }
    }

    @Test public void testReadYAML() throws Exception {
        final ConfigurationFile cg = Util.readYAML("a", new URL("http://a"), 1, readYAML("yaml/valid.yaml"));
        assertNotNull(cg);
        assertEquals(2, cg.getConfigurations().size());
    }

    @Test public void testSimpleTypeConversions() throws Exception {
        final Object obj = Util.parseYAML("a", readYAML("yaml/simple-types.yaml"));
        @SuppressWarnings("unchecked")
        final Map<String, Object> config = (Map<String, Object>)obj;
        @SuppressWarnings("unchecked")
        final Map<String, Object> properties = (Map<String, Object>)config.get("config");

        assertTrue(Util.convert(properties.get("string"), null) instanceof String);
        assertTrue(Util.convert(properties.get("boolean"), null) instanceof Boolean);
        assertTrue(Util.convert(properties.get("number"), null) instanceof Long);
        assertTrue(Util.convert(properties.get("float"), null) instanceof Double);

        // arrays
        assertTrue(Util.convert(properties.get("string.array"), null).getClass().isArray());
        assertTrue(Array.get(Util.convert(properties.get("string.array"), null), 0) instanceof String);
        assertTrue(Array.get(Util.convert(properties.get("string.array"), null), 1) instanceof String);

        assertTrue(Util.convert(properties.get("boolean.array"), null).getClass().isArray());
        assertTrue(Array.get(Util.convert(properties.get("boolean.array"), null), 0) instanceof Boolean);
        assertTrue(Array.get(Util.convert(properties.get("boolean.array"), null), 1) instanceof Boolean);

        assertTrue(Util.convert(properties.get("number.array"), null).getClass().isArray());
        assertTrue(Array.get(Util.convert(properties.get("number.array"), null), 0) instanceof Long);
        assertTrue(Array.get(Util.convert(properties.get("number.array"), null), 1) instanceof Long);

        assertTrue(Util.convert(properties.get("float.array"), null).getClass().isArray());
        assertTrue(Array.get(Util.convert(properties.get("float.array"), null), 0) instanceof Double);
        assertTrue(Array.get(Util.convert(properties.get("float.array"), null), 1) instanceof Double);
    }

    @Test public void testSimpleTypeConversionsWithTypeHint() throws Exception {
        final Object obj = Util.parseYAML("a", readYAML("yaml/simple-types.yaml"));
        @SuppressWarnings("unchecked")
        final Map<String, Object> config = (Map<String, Object>)obj;
        @SuppressWarnings("unchecked")
        final Map<String, Object> properties = (Map<String, Object>)config.get("config");

        assertTrue(Util.convert(properties.get("string"), "String") instanceof String);
        assertTrue(Util.convert(properties.get("boolean"), "Boolean") instanceof Boolean);
        assertTrue(Util.convert(properties.get("boolean"), "boolean") instanceof Boolean);
        assertTrue(Util.convert(properties.get("number"), "Integer") instanceof Integer);
        assertTrue(Util.convert(properties.get("number"), "int") instanceof Integer);
        assertTrue(Util.convert(properties.get("number"), "Long") instanceof Long);
        assertTrue(Util.convert(properties.get("number"), "long") instanceof Long);
        assertTrue(Util.convert(properties.get("float"), "Double") instanceof Double);
        assertTrue(Util.convert(properties.get("float"), "double") instanceof Double);
        assertTrue(Util.convert(properties.get("float"), "Float") instanceof Float);
        assertTrue(Util.convert(properties.get("float"), "float") instanceof Float);
        assertTrue(Util.convert(properties.get("number"), "Byte") instanceof Byte);
        assertTrue(Util.convert(properties.get("number"), "byte") instanceof Byte);
        assertTrue(Util.convert(properties.get("number"), "Short") instanceof Short);
        assertTrue(Util.convert(properties.get("number"), "short") instanceof Short);
        assertTrue(Util.convert(properties.get("string"), "Character") instanceof Character);
        assertTrue(Util.convert(properties.get("string"), "char") instanceof Character);

        // arrays
        assertTrue(Util.convert(properties.get("string.array"), "String[]").getClass().isArray());
        assertTrue(Array.get(Util.convert(properties.get("string.array"), "String[]"), 0) instanceof String);
        assertTrue(Array.get(Util.convert(properties.get("string.array"), "String[]"), 1) instanceof String);

        assertTrue(Util.convert(properties.get("boolean.array"), "Boolean[]").getClass().isArray());
        assertTrue(Array.get(Util.convert(properties.get("boolean.array"), "Boolean[]"), 0) instanceof Boolean);
        assertTrue(Array.get(Util.convert(properties.get("boolean.array"), "Boolean[]"), 1) instanceof Boolean);

        // the following would throw class cast exceptions
        boolean[] a0 = (boolean[])Util.convert(properties.get("boolean.array"), "boolean[]");
        assertNotNull(a0);
        int[] a1 = (int[])Util.convert(properties.get("number.array"), "int[]");
        assertNotNull(a1);
        long[] a2 = (long[])Util.convert(properties.get("number.array"), "long[]");
        assertNotNull(a2);
        double[] a3 = (double[])Util.convert(properties.get("float.array"), "double[]");
        assertNotNull(a3);
        float[] a4 = (float[])Util.convert(properties.get("float.array"), "float[]");
        assertNotNull(a4);
        byte[] a5 = (byte[])Util.convert(properties.get("number.array"), "byte[]");
        assertNotNull(a5);
        short[] a6 = (short[])Util.convert(properties.get("number.array"), "short[]");
        assertNotNull(a6);
        char[] a7 = (char[])Util.convert(properties.get("string.array"), "char[]");
        assertNotNull(a7);

        assertTrue(Util.convert(properties.get("number.array"), "Integer[]").getClass().isArray());
        assertTrue(Array.get(Util.convert(properties.get("number.array"), "Integer[]"), 0) instanceof Integer);
        assertTrue(Array.get(Util.convert(properties.get("number.array"), "Integer[]"), 1) instanceof Integer);

        assertTrue(Util.convert(properties.get("number.array"), "Long[]").getClass().isArray());
        assertTrue(Array.get(Util.convert(properties.get("number.array"), "Long[]"), 0) instanceof Long);
        assertTrue(Array.get(Util.convert(properties.get("number.array"), "Long[]"), 1) instanceof Long);

        assertTrue(Util.convert(properties.get("number.array"), "Byte[]").getClass().isArray());
        assertTrue(Array.get(Util.convert(properties.get("number.array"), "Byte[]"), 0) instanceof Byte);
        assertTrue(Array.get(Util.convert(properties.get("number.array"), "Byte[]"), 1) instanceof Byte);

        assertTrue(Util.convert(properties.get("number.array"), "Short[]").getClass().isArray());
        assertTrue(Array.get(Util.convert(properties.get("number.array"), "Short[]"), 0) instanceof Short);
        assertTrue(Array.get(Util.convert(properties.get("number.array"), "Short[]"), 1) instanceof Short);

        assertTrue(Util.convert(properties.get("float.array"), "Float[]").getClass().isArray());
        assertTrue(Array.get(Util.convert(properties.get("float.array"), "Float[]"), 0) instanceof Float);
        assertTrue(Array.get(Util.convert(properties.get("float.array"), "Float[]"), 1) instanceof Float);

        assertTrue(Util.convert(properties.get("float.array"), "Double[]").getClass().isArray());
        assertTrue(Array.get(Util.convert(properties.get("float.array"), "Double[]"), 0) instanceof Double);
        assertTrue(Array.get(Util.convert(properties.get("float.array"), "Double[]"), 1) instanceof Double);

        assertTrue(Util.convert(properties.get("string.array"), "Character[]").getClass().isArray());
        assertTrue(Array.get(Util.convert(properties.get("string.array"), "Character[]"), 0) instanceof Character);
        assertTrue(Array.get(Util.convert(properties.get("string.array"), "Character[]"), 1) instanceof Character);
    }

    @SuppressWarnings("unchecked")
    @Test public void testCollectionTypeConversion() throws Exception {
        final Object obj = Util.parseYAML("a", readYAML("yaml/simple-types.yaml"));
        final Map<String, Object> config = (Map<String, Object>)obj;
        final Map<String, Object> properties = (Map<String, Object>)config.get("config");

        assertTrue(Util.convert(properties.get("string.array"), "Collection<String>") instanceof Collection<?>);
        assertTrue(((Collection<String>)Util.convert(properties.get("string.array"), "Collection<String>")).iterator().next() instanceof String);

        assertTrue(Util.convert(properties.get("number.array"), "Collection<Integer>") instanceof Collection<?>);
        assertTrue(((Collection<Integer>)Util.convert(properties.get("number.array"), "Collection<Integer>")).iterator().next() instanceof Integer);

        assertTrue(Util.convert(properties.get("number.array"), "Collection<Long>") instanceof Collection<?>);
        assertTrue(((Collection<Long>)Util.convert(properties.get("number.array"), "Collection<Long>")).iterator().next() instanceof Long);

        assertTrue(Util.convert(properties.get("float.array"), "Collection<Float>") instanceof Collection<?>);
        assertTrue(((Collection<Float>)Util.convert(properties.get("float.array"), "Collection<Float>")).iterator().next() instanceof Float);

        assertTrue(Util.convert(properties.get("float.array"), "Collection<Double>") instanceof Collection<?>);
        assertTrue(((Collection<Double>)Util.convert(properties.get("float.array"), "Collection<Double>")).iterator().next() instanceof Double);

        assertTrue(Util.convert(properties.get("number.array"), "Collection<Short>") instanceof Collection<?>);
        assertTrue(((Collection<Short>)Util.convert(properties.get("number.array"), "Collection<Short>")).iterator().next() instanceof Short);

        assertTrue(Util.convert(properties.get("number.array"), "Collection<Byte>") instanceof Collection<?>);
        assertTrue(((Collection<Byte>)Util.convert(properties.get("number.array"), "Collection<Byte>")).iterator().next() instanceof Byte);

        assertTrue(Util.convert(properties.get("string.array"), "Collection<Character>") instanceof Collection<?>);
        assertTrue(((Collection<Character>)Util.convert(properties.get("string.array"), "Collection<Character>")).iterator().next() instanceof Character);

        assertTrue(Util.convert(properties.get("boolean.array"), "Collection<Boolean>") instanceof Collection<?>);
        assertTrue(((Collection<Boolean>)Util.convert(properties.get("boolean.array"), "Collection<Boolean>")).iterator().next() instanceof Boolean);
    }
}
