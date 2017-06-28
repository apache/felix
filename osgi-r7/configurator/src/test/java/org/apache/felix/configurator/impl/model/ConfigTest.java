/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.configurator.impl.model;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Test;

public class ConfigTest {

    @Test public void testReadWrite() throws Exception {
        final Dictionary<String, Object> props = new Hashtable<>();
        props.put("x", "1");
        props.put("y", 1L);

        final Config cfg = new Config("a",
                props, 10, 50, ConfigPolicy.DEFAULT);
        cfg.setIndex(70);
        cfg.setState(ConfigState.UNINSTALL);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try ( final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(cfg);
        }

        try ( final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            final Config c = (Config) ois.readObject();

            assertEquals("a", c.getPid());

            assertEquals(10, c.getBundleId());
            assertEquals(50, c.getRanking());
            assertEquals(70, c.getIndex());
            assertEquals(ConfigState.UNINSTALL, c.getState());
            assertEquals(ConfigPolicy.DEFAULT, c.getPolicy());

            assertEquals(2, c.getProperties().size());
            assertEquals("1", c.getProperties().get("x"));
            assertEquals(1L, c.getProperties().get("y"));
        }
    }
}
