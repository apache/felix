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
package org.apache.felix.converter.impl.json;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.converter.impl.json.JsonCodecImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JsonSerializationTest {
    @Test
    public void testComplexMapSerialization() {
        Map<String, Object> m = new HashMap<>();
        m.put("sKey", "a string");
        m.put("iKey", 42);
        m.put("bKey",  true);
        m.put("noKey", null);
        m.put("simpleArray", new int[] {1,2,3});

        Map<String, Object> m1 = new HashMap<>();
        m1.put("a", 1L);
        m1.put("b", "hello");
        m.put("simpleObject", m1);

        String expected = "{\"bKey\":true,"
                + "\"simpleArray\":[1,2,3],"
                + "\"iKey\":42,"
                + "\"sKey\":\"a string\","
                + "\"simpleObject\":{\"a\":1,\"b\":\"hello\"},"
                + "\"noKey\":null}";
        assertEquals(expected, new JsonCodecImpl().encode(m).toString());
    }

    @Test
    public void testComplexMapSerialization2() {
        Map<String, Object> m2 = new HashMap<>();
        m2.put("yes", Boolean.TRUE);
        m2.put("no", Collections.singletonMap("maybe", false));

        Map<String, Object> cm = new HashMap<>();
        cm.put("list", Arrays.asList(
                Collections.singletonMap("x", "y"),
                Collections.singletonMap("x", "b")));
        cm.put("embedded", m2);

        String expected = "{\"list\":[{\"x\":\"y\"},{\"x\":\"b\"}],"
                + "\"embedded\":"
                + "{\"no\":{\"maybe\":false},\"yes\":true}}";
        assertEquals(expected, new JsonCodecImpl().encode(cm).toString());
    }
}
