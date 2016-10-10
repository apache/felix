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
package org.apache.felix.prefs;

import static org.junit.Assert.assertEquals;

import java.nio.file.Files;

import org.apache.felix.prefs.impl.DataFileBackingStoreImpl;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class PreferencesImplTest {

    @Test public void testAddRemoveAdd()
    throws Exception {
        final BackingStore store = new DataFileBackingStoreImpl(null, Files.createTempDirectory("prefs").toFile());
        final PreferencesImpl prefs = new PreferencesImpl(new PreferencesDescription(5L, null),
                new BackingStoreManager() {

            public BackingStore getStore() throws BackingStoreException {
                return store;
            }
        });
        Preferences firstA = prefs.node("A");
        firstA.node("1");
        firstA.node("2");
        assertEquals(1, prefs.childrenNames().length);
        assertEquals(2, firstA.childrenNames().length);

        firstA.removeNode();
        prefs.flush();

        assertEquals(0, prefs.childrenNames().length);

        firstA = prefs.node("A");
        assertEquals(1, prefs.childrenNames().length);
        assertEquals(0, firstA.childrenNames().length);
        firstA.node("1");
        firstA.node("2");
        assertEquals(2, firstA.childrenNames().length);
    }
}
