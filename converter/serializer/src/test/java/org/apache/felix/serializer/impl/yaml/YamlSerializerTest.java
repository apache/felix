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
package org.apache.felix.serializer.impl.yaml;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.converter.Converter;
import org.osgi.service.converter.StandardConverter;

import static org.junit.Assert.assertEquals;

public class YamlSerializerTest {
    private Converter converter;

    @Before
    public void setUp() {
        converter = new StandardConverter();
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

        YamlSerializerImpl yamlCodec = new YamlSerializerImpl();
        String yaml = yamlCodec.serialize(m).toString();

        assertEquals("1: 11\n" +
                "ab: 'cd'\n" +
                "true: \n" +
                "  x: true\n" +
                "  y:", yaml);

        @SuppressWarnings("rawtypes")
        Map m2 = yamlCodec.deserialize(Map.class).from(yaml);
        // m2 is not exactly equal to m, as the keys are all strings now, this is unavoidable with JSON
        assertEquals(m.size(), m2.size());
        assertEquals(converter.convert(m.get(1)).to(int.class),
                converter.convert(m2.get(1)).to(int.class));
        assertEquals(m.get("ab"), m2.get("ab"));
        assertEquals(m.get(true), m2.get(true));
    }
}
