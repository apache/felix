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
package org.apache.felix.schematizer.impl;

import java.util.Map;
import java.util.Optional;

import org.apache.felix.schematizer.Node;
import org.apache.felix.schematizer.Schema;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.util.converter.Converter;
import org.osgi.util.converter.StandardConverter;
import org.osgi.util.converter.TypeReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SchematizerServiceTest {
    private SchematizerImpl schematizer;
    @SuppressWarnings( "unused" )
    private Converter converter;

    @Before
    public void setUp() {
        schematizer = new SchematizerImpl();
        converter = new StandardConverter();
    }

    @After
    public void tearDown() {
        schematizer = null;
    }

    @Test
    public void testSchematizeDTO() {
        Optional<Schema> opt = schematizer
                .rule("MyDTO", new TypeReference<MyDTO>(){})
                .rule("MyDTO", "/embedded", new TypeReference<MyEmbeddedDTO>(){})
                .get("MyDTO");

        assertTrue(opt.isPresent());
        Schema s = opt.get();
        assertNotNull(s);
        Node root = s.rootNode();
        assertNodeEquals("", "/", false, MyDTO.class, root);
        assertEquals(4, root.children().size());
        Node pingNode = root.children().get("/ping");
        assertNodeEquals("ping", "/ping", false, String.class, pingNode);
        Node pongNode = root.children().get("/pong");
        assertNodeEquals("pong", "/pong", false, Long.class, pongNode);
        Node countNode = root.children().get("/count");
        assertNodeEquals("count", "/count", false, MyDTO.Count.class, countNode);
        Node embeddedNode = root.children().get("/embedded");
        assertEquals(3, embeddedNode.children().size());
        assertNodeEquals("embedded", "/embedded", false, MyEmbeddedDTO.class, embeddedNode);
        Node marcoNode = embeddedNode.children().get("/marco");
        assertNodeEquals("marco", "/embedded/marco", false, String.class, marcoNode);
        Node poloNode = embeddedNode.children().get("/polo");
        assertNodeEquals("polo", "/embedded/polo", false, Long.class, poloNode);
        Node alphaNode = embeddedNode.children().get("/alpha");
        assertNodeEquals("alpha", "/embedded/alpha", false, MyEmbeddedDTO.Alpha.class, alphaNode);

        Node sRoot = s.nodeAtPath("/").get();
        assertNodeEquals("", "/", false, MyDTO.class, sRoot);
        Node sPingNode = s.nodeAtPath("/ping").get();
        assertNodeEquals("ping", "/ping", false, String.class, sPingNode);
        Node sPongNode = s.nodeAtPath("/pong").get();
        assertNodeEquals("pong", "/pong", false, Long.class, sPongNode);
        Node sCountNode = s.nodeAtPath("/count").get();
        assertNodeEquals("count", "/count", false, MyDTO.Count.class, sCountNode);
        Node sEmbeddedNode = s.nodeAtPath("/embedded").get();
        assertNodeEquals("embedded", "/embedded", false, MyEmbeddedDTO.class, sEmbeddedNode);
        Node sMarcoNode = s.nodeAtPath("/embedded/marco").get();
        assertNodeEquals("marco", "/embedded/marco", false, String.class, sMarcoNode);
        Node sPoloNode = s.nodeAtPath("/embedded/polo").get();
        assertNodeEquals("polo", "/embedded/polo", false, Long.class, sPoloNode);
        Node sAlphaNode = s.nodeAtPath("/embedded/alpha").get();
        assertNodeEquals("alpha", "/embedded/alpha", false, MyEmbeddedDTO.Alpha.class, sAlphaNode);
    }

    @Test
    public void testSchematizeDTOWithColletion() {
        Optional<Schema> opt = schematizer
                .rule("MyDTO", new TypeReference<MyDTO3<MyEmbeddedDTO2<String>>>(){})
                .rule("MyDTO", "/embedded", new TypeReference<MyEmbeddedDTO2<String>>(){})
                .rule("MyDTO", "/embedded/value", String.class)
                .get("MyDTO");

        assertTrue(opt.isPresent());
        Schema s = opt.get();
        assertNotNull(s);
        Node root = s.rootNode();
        assertNodeEquals("", "/", false, new TypeReference<MyDTO3<MyEmbeddedDTO2<String>>>(){}.getType(), root);
        assertEquals(4, root.children().size());
        Node pingNode = root.children().get("/ping");
        assertNodeEquals("ping", "/ping", false, String.class, pingNode);
        Node pongNode = root.children().get("/pong");
        assertNodeEquals("pong", "/pong", false, Long.class, pongNode);
        Node countNode = root.children().get("/count");
        assertNodeEquals("count", "/count", false, MyDTO3.Count.class, countNode);
        Node embeddedNode = root.children().get("/embedded");
        assertEquals(1, embeddedNode.children().size());
        assertNodeEquals("embedded", "/embedded", true, new TypeReference<MyEmbeddedDTO2<String>>(){}.getType(), embeddedNode);
        Node valueNode = embeddedNode.children().get("/value");
        assertNodeEquals("value", "/embedded/value", false, String.class, valueNode);

        Node sRoot = s.nodeAtPath("/").get();
        assertNodeEquals("", "/", false, new TypeReference<MyDTO3<MyEmbeddedDTO2<String>>>(){}.getType(), sRoot);
        Node sPingNode = s.nodeAtPath("/ping").get();
        assertNodeEquals("ping", "/ping", false, String.class, sPingNode);
        Node sPongNode = s.nodeAtPath("/pong").get();
        assertNodeEquals("pong", "/pong", false, Long.class, sPongNode);
        Node sCountNode = s.nodeAtPath("/count").get();
        assertNodeEquals("count", "/count", false, MyDTO3.Count.class, sCountNode);
        Node sEmbeddedNode = s.nodeAtPath("/embedded").get();
        assertNodeEquals("embedded", "/embedded", true, new TypeReference<MyEmbeddedDTO2<String>>(){}.getType(), sEmbeddedNode);
        Node sValueNode = s.nodeAtPath("/embedded/value").get();
        assertNodeEquals("value", "/embedded/value", false, String.class, sValueNode);
    }

    @Test
    public void testSchematizeToMap() {
        Optional<Schema> opt = schematizer
                .rule("MyDTO", new TypeReference<MyDTO>(){})
                .rule("MyDTO", "/embedded", new TypeReference<MyEmbeddedDTO>(){})
                .get("MyDTO");

        assertTrue(opt.isPresent());
        Schema s = opt.get();
        Map<String, Node.DTO> map = s.toMap();
        testMapValues(map);
    }

    private void testMapValues(Map<String, Node.DTO> map) {
        assertNotNull(map);
        assertEquals(1, map.size());
        Node.DTO root = map.get("/");
        assertEquals(4, root.children.size());
        assertNodeDTOEquals("", "/", false, MyDTO.class, root);
        Node.DTO pingNode = root.children.get("ping");
        assertNodeDTOEquals("ping", "/ping", false, String.class, pingNode);
        Node.DTO pongNode = root.children.get("pong");
        assertNodeDTOEquals("pong", "/pong", false, Long.class, pongNode);
        Node.DTO countNode = root.children.get("count");
        assertNodeDTOEquals("count", "/count", false, MyDTO.Count.class, countNode);
        Node.DTO embeddedNode = root.children.get("embedded");
        assertEquals(3, embeddedNode.children.size());
        assertNodeDTOEquals("embedded", "/embedded", false, MyEmbeddedDTO.class, embeddedNode);
        Node.DTO marcoNode = embeddedNode.children.get("marco");
        assertNodeDTOEquals("marco", "/embedded/marco", false, String.class, marcoNode);
        Node.DTO poloNode = embeddedNode.children.get("polo");
        assertNodeDTOEquals("polo", "/embedded/polo", false, Long.class, poloNode);
        Node.DTO alphaNode = embeddedNode.children.get("alpha");
        assertNodeDTOEquals("alpha", "/embedded/alpha", false, MyEmbeddedDTO.Alpha.class, alphaNode);
    }

    @Test
    public void testSchemaFromMap() {
        Optional<Schema> opt1 = schematizer
                .rule("MyDTO", new TypeReference<MyDTO>(){})
                .rule("MyDTO", "/embedded", new TypeReference<MyEmbeddedDTO>(){})
                .get("MyDTO");

        assertTrue(opt1.isPresent());
        Schema s1 = opt1.get();
        Map<String, Node.DTO> map = s1.toMap();

        Optional<Schema> opt2 = schematizer.from("MyDTO", map);
        assertTrue(opt1.isPresent());
        Schema s2 = opt2.get();
        testSchema(s2);
    }

    private void testSchema(Schema s) {
        // Assume that the map is serialized, then deserialized "as is".
        assertNotNull(s);
        Node root = s.rootNode();
        assertEquals(4, root.children().size());
        assertNodeEquals("", "/", false, MyDTO.class, root);
        Node pingNode = root.children().get("/ping");
        assertNodeEquals("ping", "/ping", false, String.class, pingNode);
        Node pongNode = root.children().get("/pong");
        assertNodeEquals("pong", "/pong", false, Long.class, pongNode);
        Node countNode = root.children().get("/count");
        assertNodeEquals("count", "/count", false, MyDTO.Count.class, countNode);
        Node embeddedNode = root.children().get("/embedded");
        assertEquals(3, embeddedNode.children().size());
        assertNodeEquals("embedded", "/embedded", false, MyEmbeddedDTO.class, embeddedNode);
        Node marcoNode = embeddedNode.children().get("/marco");
        assertNodeEquals("marco", "/embedded/marco", false, String.class, marcoNode);
        Node poloNode = embeddedNode.children().get("/polo");
        assertNodeEquals("polo", "/embedded/polo", false, Long.class, poloNode);
        Node alphaNode = embeddedNode.children().get("/alpha");
        assertNodeEquals("alpha", "/embedded/alpha", false, MyEmbeddedDTO.Alpha.class, alphaNode);

        Node sRoot = s.nodeAtPath("/").get();
        assertNodeEquals("", "/", false, MyDTO.class, sRoot);
        Node sPingNode = s.nodeAtPath("/ping").get();
        assertNodeEquals("ping", "/ping", false, String.class, sPingNode);
        Node sPongNode = s.nodeAtPath("/pong").get();
        assertNodeEquals("pong", "/pong", false, Long.class, sPongNode);
        Node sCountNode = s.nodeAtPath("/count").get();
        assertNodeEquals("count", "/count", false, MyDTO.Count.class, sCountNode);
        Node sEmbeddedNode = s.nodeAtPath("/embedded").get();
        assertNodeEquals("embedded", "/embedded", false, MyEmbeddedDTO.class, sEmbeddedNode);
        Node sMarcoNode = s.nodeAtPath("/embedded/marco").get();
        assertNodeEquals("marco", "/embedded/marco", false, String.class, sMarcoNode);
        Node sPoloNode = s.nodeAtPath("/embedded/polo").get();
        assertNodeEquals("polo", "/embedded/polo", false, Long.class, sPoloNode);
        Node sAlphaNode = s.nodeAtPath("/embedded/alpha").get();
        assertNodeEquals("alpha", "/embedded/alpha", false, MyEmbeddedDTO.Alpha.class, sAlphaNode);
    }

    @Test
    public void testVisitor() {
        Optional<Schema> opt = schematizer
                .rule("MyDTO", new TypeReference<MyDTO>(){})
                .rule("MyDTO", "/embedded", new TypeReference<MyEmbeddedDTO>(){})
                .get("MyDTO");

        assertTrue(opt.isPresent());
        Schema s = opt.get();

        StringBuilder sb = new StringBuilder();
        s.visit( n -> sb.append("::").append(n.name()));
        assertEquals("::::count::embedded::alpha::marco::polo::ping::pong", sb.toString());
    }

    private void assertNodeEquals(String name, String path, boolean isCollection, Object type, Node node) {
        assertNotNull(node);
        assertEquals(name, node.name());
        assertEquals(path, node.absolutePath());
        assertEquals(isCollection, node.isCollection());
        assertEquals(type, node.type());
    }

    private void assertNodeDTOEquals(String name, String path, boolean isCollection, Class<?> type, Node.DTO node) {
        assertNotNull(node);
        assertEquals(name, node.name);
        assertEquals(path, node.path);
        assertEquals(isCollection, node.isCollection);
        assertEquals(type.getName(), node.type);
    }
}
