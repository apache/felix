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

import org.junit.Test;

public class BundleStateTest {

    @Test public void testReadWrite() throws Exception {
        final BundleState state = new BundleState();

        final Config c1 = new Config("a", Collections.emptySet(), null, 1,  0, ConfigPolicy.DEFAULT);
        final Config c2 = new Config("b", Collections.emptySet(), null, 1, 10, ConfigPolicy.DEFAULT);

        state.add(c1);
        state.add(c2);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try ( final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(state);
        }

        try ( final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            final BundleState s = (BundleState) ois.readObject();

            assertEquals(1, s.getConfigurations("a").size());
            assertEquals(1, s.getConfigurations("b").size());
        }
    }

    @Test public void testDifferentPids() {
        final BundleState state = new BundleState();
        final Config c1 = new Config("a", Collections.emptySet(), null, 1,  0, ConfigPolicy.DEFAULT);
        final Config c2 = new Config("b", Collections.emptySet(), null, 1, 10, ConfigPolicy.DEFAULT);

        state.add(c1);
        state.add(c2);

        assertEquals(1, state.getConfigurations("a").size());
        assertEquals(1, state.getConfigurations("b").size());
    }
}
