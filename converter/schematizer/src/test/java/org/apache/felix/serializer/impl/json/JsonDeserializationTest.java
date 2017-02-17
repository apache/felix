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

import java.util.ArrayList;
import java.util.Optional;

import org.apache.felix.schematizer.Schema;
import org.apache.felix.schematizer.SchematizingConverter;
import org.apache.felix.schematizer.impl.SchematizerImpl;
import org.apache.felix.serializer.impl.json.MyDTO.Count;
import org.apache.felix.serializer.impl.json.MyEmbeddedDTO.Alpha;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.util.converter.TypeReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JsonDeserializationTest {
    private SchematizingConverter converter;

    @Before
    public void setUp() {
        converter = new SchematizingConverter();
    }

    @After
    public void tearDown() {
        converter = null;
    }

    @Test
    public void testDeserialization() {
        MyEmbeddedDTO embedded = new MyEmbeddedDTO();
        embedded.marco = "mmmm";
        embedded.polo = 24;
        embedded.alpha = Alpha.B;

        MyDTO dto = new MyDTO();
        dto.ping = "pppping";
        dto.pong = 42;
        dto.count = Count.TWO;
        dto.embedded = embedded;

        // TODO
        Optional<Schema> opt = new SchematizerImpl()
            .rule("MyDTO", new TypeReference<MyDTO>(){})
            .rule("MyDTO", "/embedded", new TypeReference<MyEmbeddedDTO>(){})
            .get("MyDTO");

        assertTrue(opt.isPresent());

        Schema s = opt.get();

        String serialized = new JsonSerializerImpl().serialize(dto).with(converter.withSchema(s)).toString();
        MyDTO result = new JsonSerializerImpl()
                .deserialize(MyDTO.class)
                .with(converter.withSchema(s))
                .from(serialized);

        assertEquals(dto.ping, result.ping);
        assertEquals(dto.pong, result.pong);
        assertEquals(dto.count, result.count);
        assertEquals(dto.embedded.marco, result.embedded.marco);
        assertEquals(dto.embedded.polo, result.embedded.polo);
        assertEquals(dto.embedded.alpha, result.embedded.alpha);
    }

    @Test
    public void testDeserializationWithCollection() {
        MyEmbeddedDTO2<String> embedded = new MyEmbeddedDTO2<>();
        embedded.value = "one million dollars";

        MyDTO2<MyEmbeddedDTO2<String>> dto = new MyDTO2<>();
        dto.ping = "pppping";
        dto.pong = 42;
        dto.embedded = new ArrayList<>();
        dto.embedded.add( embedded );

        String serialized = new JsonSerializerImpl().serialize(dto).toString();

        Optional<Schema> opt = new SchematizerImpl()
                .rule("MyDTO", new TypeReference<MyDTO2<MyEmbeddedDTO2<String>>>(){})
                .rule("MyDTO", "/embedded", new TypeReference<MyEmbeddedDTO2<String>>(){})
                .rule("MyDTO", "/embedded/value", String.class)
                .get("MyDTO");

        assertTrue(opt.isPresent());

        Schema s = opt.get();
        MyDTO2<MyEmbeddedDTO2<String>> result =
                new JsonSerializerImpl()
                .deserialize(new TypeReference<MyDTO2<MyEmbeddedDTO2<String>>>(){})
                .with(converter.withSchema(s))
                .from(serialized);

        assertEquals(dto.ping, result.ping);
        assertEquals(dto.pong, result.pong);
        assertEquals(dto.embedded.get(0).value, result.embedded.get(0).value);
    }
}
