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
package org.apache.felix.serializer.impl.json;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JsonParserTest {
    @Test
    public void testJsonSimple() {
        String json = "{\"hi\": \"ho\", \"ha\": true}";
        JsonParser jp = new JsonParser(json);
        Map<String, Object> m = jp.getParsed();
        assertEquals(2, m.size());
        assertEquals("ho", m.get("hi"));
        assertTrue((Boolean) m.get("ha"));
    }

    @Test
    public void testJsonWithNewline() {
        String json = ""
                + "{\n"
                + "  \"hi\": \"ho\",\n"
                + "  \"ha\": [\n"
                + "    \"one\",\n"
                + "    \"two\",\n"
                + "    \"three\"\n"
                + "  ]\n"
                + "}\n"
                + "\n";
        JsonParser jp = new JsonParser(json);
        Map<String, Object> m = jp.getParsed();
        assertEquals(2, m.size());
        assertEquals("ho", m.get("hi"));
        assertEquals(3, ((List<?>)m.get("ha")).size());
    }

    @Test
    public void testJsonWithCRLF() {
        String json = ""
                + "{\r\n"
                + "  \"hi\": \"ho\",\r\n"
                + "  \"ha\": [\r\n"
                + "    \"one\",\r\n"
                + "    \"two\",\r\n"
                + "    \"three\"\r\n"
                + "  ]\r\n"
                + "}\r\n"
                + "\r\n";
        JsonParser jp = new JsonParser(json);
        Map<String, Object> m = jp.getParsed();
        assertEquals(2, m.size());
        assertEquals("ho", m.get("hi"));
        assertEquals(3, ((List<?>)m.get("ha")).size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testJsonComplex() {
        String json = "{\"a\": [1,2,3,4,5], \"b\": {\"x\": 12, \"y\": 42, \"z\": {\"test test\": \"hello hello\"}}, \"ddd\": 12.34}";
        JsonParser jp = new JsonParser(json);
        Map<String, Object> m = jp.getParsed();
        assertEquals(3, m.size());
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), m.get("a"));
        Map<String, Object> mb = (Map<String, Object>) m.get("b");
        assertEquals(3, mb.size());
        assertEquals(12L, mb.get("x"));
        assertEquals(42L, mb.get("y"));
        Map<String, Object> mz = (Map<String, Object>) mb.get("z");
        assertEquals(1, mz.size());
        assertEquals("hello hello", mz.get("test test"));
        assertEquals(12.34d, ((Double) m.get("ddd")).doubleValue(), 0.0001d);
    }

    @Test
    public void testJsonArray() {
        String json = "{\"abc\": [\"x\", \"y\", \"z\"]}";
        JsonParser jp = new JsonParser(json);
        Map<String, Object> m = jp.getParsed();
        assertEquals(1, m.size());
        assertEquals(Arrays.asList("x", "y", "z"), m.get("abc"));
    }

    @Test
    public void testEmptyJsonArray() {
        String json = "{\"abc\": {\"def\": []}}";
        JsonParser jp = new JsonParser(json);
        Map<String, Object> m = jp.getParsed();
        assertEquals(1, m.size());
        Map<String, Object> result = new HashMap<>();
        result.put("def", Collections.emptyList());
        assertEquals(result, m.get("abc"));
    }
}
