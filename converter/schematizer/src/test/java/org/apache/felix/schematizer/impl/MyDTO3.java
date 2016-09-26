/*
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

import java.util.List;

import org.osgi.dto.DTO;

public class MyDTO3<T> extends DTO {
    public enum Count { ONE, TWO, THREE }

    public Count count;

    public String ping;

    public long pong;

    public List<T> embedded;
}

