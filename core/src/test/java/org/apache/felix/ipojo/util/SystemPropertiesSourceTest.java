/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.util;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test the system property context source.
 */
public class SystemPropertiesSourceTest {

    @After
    public void tearDown() {
        System.clearProperty("__property");
    }

    @Test
    public void getProperties() {
        System.setProperty("__property", "__value");
        SystemPropertiesSource cs = new SystemPropertiesSource();
        assertEquals(cs.getContext().get("__property"), "__value");
    }

    @Test
    public void getProperty() {
        System.setProperty("__property", "__value");
        SystemPropertiesSource cs = new SystemPropertiesSource();
        assertEquals(cs.getProperty("__property"), "__value");
    }

    @Test
    public void getMissingProperty() {
        SystemPropertiesSource cs = new SystemPropertiesSource();
        assertNull(cs.getProperty("__property"));
    }
}
