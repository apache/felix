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

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.converter.impl.ConverterImpl;
import org.apache.felix.converter.impl.MyDTO;
import org.apache.felix.converter.impl.MyDTO.Count;
import org.apache.felix.converter.impl.MyEmbeddedDTO;
import org.apache.felix.converter.impl.MyEmbeddedDTO.Alpha;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.converter.Adapter;
import org.osgi.service.converter.Converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JsonCodecTest {
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
    public void testJSONCodec() throws Exception {
        Map<Object, Object> m1 = new HashMap<>();
        m1.put("x", true);
        m1.put("y", null);
        Map<Object, Object> m = new HashMap<>();
        m.put(1, 11L);
        m.put("ab", "cd");
        m.put(true, m1);

        JsonCodecImpl jsonCodec = new JsonCodecImpl();
        String json = jsonCodec.encode(m).toString();

        JSONObject jo = new JSONObject(json);
        assertEquals(11, jo.getInt("1"));
        assertEquals("cd", jo.getString("ab"));
        JSONObject jo2 = jo.getJSONObject("true");
        assertEquals(true, jo2.getBoolean("x"));
        assertTrue(jo2.isNull("y"));

        @SuppressWarnings("rawtypes")
        Map m2 = jsonCodec.decode(Map.class).from(json);
        // m2 is not exactly equal to m, as the keys are all strings now, this is unavoidable with JSON
        assertEquals(m.size(), m2.size());
        assertEquals(m.get(1), m2.get("1"));
        assertEquals(m.get("ab"), m2.get("ab"));
        assertEquals(m.get(true), m2.get("true"));
    }

    @Test
    public void testCodecWithAdapter() throws JSONException {
        Map<String, Foo> m1 = new HashMap<>();
        m1.put("f", new Foo("fofofo"));
        Map<String, Object> m = new HashMap<>();
        m.put("submap", m1);

        Adapter ca = converter.getAdapter();
        ca.rule(Foo.class, String.class, Foo::tsFun, v -> Foo.fsFun(v));

        JsonCodecImpl jsonCodec = new JsonCodecImpl();
        String json = jsonCodec.with(ca).encode(m).toString();

        JSONObject jo = new JSONObject(json);
        assertEquals(1, jo.length());
        JSONObject jo1 = jo.getJSONObject("submap");
        assertEquals("<fofofo>", jo1.getString("f"));

        // TODO convert back into a Map<String, Foo> via TypeReference
    }

    @Test
    public void testDTO() {
        MyDTO dto = new MyDTO();
        dto.count = Count.ONE;
        dto.ping = "'";
        dto.pong = Long.MIN_VALUE;

        MyEmbeddedDTO embedded = new MyEmbeddedDTO();
        embedded.alpha = Alpha.B;
        embedded.marco = "jo !";
        embedded.polo = 327;
        dto.embedded = embedded;

        JsonCodecImpl jsonCodec = new JsonCodecImpl();
        String json = jsonCodec.encode(dto).toString();
        assertEquals(
            "{\"ping\":\"'\",\"count\":\"ONE\",\"pong\":-9223372036854775808,"
            + "\"embedded\":{\"polo\":327,\"alpha\":\"B\",\"marco\":\"jo !\"}}",
            json);

        MyDTO dto2 = jsonCodec.decode(MyDTO.class).from(json);
        assertEquals(Count.ONE, dto2.count);
        assertEquals("'", dto2.ping);
        assertEquals(Long.MIN_VALUE, dto2.pong);
        MyEmbeddedDTO embedded2 = dto2.embedded;
        assertEquals(Alpha.B, embedded2.alpha);
        assertEquals("jo !", embedded2.marco);
        assertEquals(327, embedded2.polo);
    }

    static class Foo {
        private final String val;

        public Foo(String s) {
            val = s;
        }

        public String tsFun() {
            return "<" + val + ">";
        }

        public static Foo fsFun(String s) {
            return new Foo(s.substring(1, s.length() - 1));
        }
    }
}
