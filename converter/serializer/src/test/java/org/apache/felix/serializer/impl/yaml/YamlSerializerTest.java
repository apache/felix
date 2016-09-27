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
import org.osgi.service.converter.TypeReference;

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
    public void testYAMLCodec() throws Exception {
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
        // m2 is not exactly equal to m, as the keys are all strings now, this is unavoidable with YAML
        assertEquals(m.size(), m2.size());
        assertEquals(converter.convert(m.get(1)).to(int.class),
                converter.convert(m2.get(1)).to(int.class));
        assertEquals(m.get("ab"), m2.get("ab"));
        assertEquals(m.get(true), m2.get(true));
    }

    @Test
    public void testCodecWithAdapter() {
        Map<String, Foo> m1 = new HashMap<>();
        m1.put("f", new Foo("fofofo"));
        Map<String, Map<String,Foo>> m = new HashMap<>();
        m.put("submap", m1);

        Converter ca = converter.newConverterBuilder().
                rule(Foo.class, String.class, Foo::tsFun, v -> Foo.fsFun(v)).build();

        YamlSerializerImpl yamlCodec = new YamlSerializerImpl();
        String yaml = yamlCodec.serialize(m).with(ca).toString();

        assertEquals("submap: \n" +
                "  f: '<fofofo>'", yaml);

        // And convert back
        Map<String,Map<String,Foo>> m2 = yamlCodec.deserialize(new TypeReference<Map<String,Map<String,Foo>>>(){}).
                with(ca).from(yaml);
        assertEquals(m, m2);
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

        @Override
        public int hashCode() {
            return val.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            if (!(obj instanceof Foo))
                return false;

            Foo f = (Foo) obj;
            return f.val.equals(val);
        }
    }
}
