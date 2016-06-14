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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class YamlSerializationTest {
    @Test
    public void testComplexMapSerialization() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sKey", "a string");
        m.put("iKey", 42);
        m.put("bKey",  true);
        m.put("noKey", null);
        m.put("simpleArray", new int[] {1,2,3});

        Map<String, Object> m1 = new HashMap<>();
        m1.put("a", 1L);
        m1.put("b", "hello");
        m.put("simpleObject", m1);

        String expected = "";
        assertEquals(expected, new YamlCodecImpl().encode(m).toString());
    }
}
