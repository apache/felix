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
import java.util.Collections;
import java.util.Iterator;

import org.junit.Test;

public class ConfigListTest {

    @Test public void testReadWrite() throws Exception {
        final ConfigList list = new ConfigList();

        final Config c1 = new Config("a", Collections.singleton("e1"),
                null, 10, 0, ConfigPolicy.DEFAULT);
        final Config c2 = new Config("a", Collections.singleton("e1"),
                null, 10, 50, ConfigPolicy.DEFAULT);
        list.add(c1);
        list.add(c2);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try ( final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(list);
        }

        try ( final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            final ConfigList l = (ConfigList) ois.readObject();

            assertEquals(2, l.size());
        }
    }

    @Test public void testRanking() {
        final ConfigList list = new ConfigList();
        final Config c1 = new Config("a", Collections.emptySet(), null, 1,  0, ConfigPolicy.DEFAULT);
        final Config c2 = new Config("a", Collections.emptySet(), null, 1, 10, ConfigPolicy.DEFAULT);
        final Config c3 = new Config("a", Collections.emptySet(), null, 1,  0, ConfigPolicy.DEFAULT);
        final Config c4 = new Config("a", Collections.emptySet(), null, 1, 50, ConfigPolicy.DEFAULT);
        final Config c5 = new Config("a", Collections.emptySet(), null, 1, 20, ConfigPolicy.DEFAULT);
        final Config c6 = new Config("a", Collections.emptySet(), null, 1, 10, ConfigPolicy.DEFAULT);

        list.add(c1);
        list.add(c2);
        list.add(c3);
        list.add(c4);
        list.add(c5);
        list.add(c6);

        assertEquals(6, list.size());
        final Iterator<Config> iter = list.iterator();
        assertEquals(c4, iter.next());
        assertEquals(c5, iter.next());
        assertEquals(c2, iter.next());
        assertEquals(c6, iter.next());
        assertEquals(c1, iter.next());
        assertEquals(c3, iter.next());
    }

    @Test public void testDifferentBundleIds() {
        final ConfigList list = new ConfigList();
        final Config c1 = new Config("a", Collections.emptySet(), null, 2, 10, ConfigPolicy.DEFAULT);
        final Config c2 = new Config("a", Collections.emptySet(), null, 1, 10, ConfigPolicy.DEFAULT);

        list.add(c1);
        list.add(c2);

        assertEquals(2, list.size());
        final Iterator<Config> iter = list.iterator();
        assertEquals(c2, iter.next());
        assertEquals(c1, iter.next());
    }
}
