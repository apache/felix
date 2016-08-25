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
package org.apache.felix.converter.impl.yaml;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class YamlSerializationTest {
    @Test
    @SuppressWarnings("unchecked")
    public void testComplexMapSerialization() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sKey", "a string");
        m.put("iKey", 42);
        m.put("bKey",  true);
        m.put("noKey", null);
        m.put("simpleArray", new int[] {1,2,3});

        Map<String, Object> m1 = new LinkedHashMap<>();
        m1.put("a", 1L);
        m1.put("b", "hello");
        m.put("simpleObject", m1);

        String expected = "sKey: 'a string'\n" +
                "iKey: 42\n" +
                "bKey: true\n" +
                "noKey: \n" +
                "simpleArray: \n" +
                "  - 1\n" +
                "  - 2\n" +
                "  - 3\n" +
                "simpleObject: \n" +
                "  a: 1\n" +
                "  b: 'hello'";
        assertEquals(expected, new YamlCodecImpl().encode(m).toString().trim());

        Map<String, Object> dm = new YamlCodecImpl().decode(Map.class).from(expected);
        Map<String, Object> expected2 = new LinkedHashMap<>();
        expected2.put("sKey", "a string");
        expected2.put("iKey", 42);
        expected2.put("bKey",  true);
        expected2.put("noKey", null);
        expected2.put("simpleArray", Arrays.asList(1,2,3));

        Map<String, Object> m2 = new LinkedHashMap<>();
        m2.put("a", 1);
        m2.put("b", "hello");
        expected2.put("simpleObject", m2);
        assertEquals(expected2, dm);
    }

    @Test
    public void testComplexMapSerialization2() {
        Map<String, Object> m2 = new LinkedHashMap<>();
        m2.put("yes", Boolean.TRUE);
        m2.put("no", Collections.singletonMap("maybe", false));

        Map<String, Object> cm = new LinkedHashMap<>();
        cm.put("list", Arrays.asList(
                Collections.singletonMap("x", "y"),
                Collections.singletonMap("x", "b")));
        cm.put("embedded", m2);

        String expected = "list: \n" +
                "  - \n" +
                "    x: 'y'\n" +
                "  - \n" +
                "    x: 'b'\n" +
                "embedded: \n" +
                "  yes: true\n" +
                "  no: \n" +
                "    maybe: false";
        assertEquals(expected, new YamlCodecImpl().encode(cm).toString().trim());
    }
}
