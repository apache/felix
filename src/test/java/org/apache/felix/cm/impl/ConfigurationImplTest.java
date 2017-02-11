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
package org.apache.felix.cm.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConfigurationImplTest {

    @Test public void testEqualsWithArrays() {
        final Dictionary<String, Object> props1 = new Hashtable<String, Object>();
        props1.put("array", new long[] {1,2});

        final Dictionary<String, Object> props2 = new Hashtable<String, Object>();
        props2.put("array", new long[] {1,2});

        assertTrue(ConfigurationImpl.equals(props1, props2));

        props2.put("array", new Long[] {1L,2L});
        assertTrue(ConfigurationImpl.equals(props1, props2));

        final Dictionary<String, Object> props3 = new Hashtable<String, Object>();
        props3.put("array", new long[] {1,2,3});
        assertFalse(ConfigurationImpl.equals(props1, props3));

        final Dictionary<String, Object> props4 = new Hashtable<String, Object>();
        props3.put("array", new long[] {1});
        assertFalse(ConfigurationImpl.equals(props1, props4));
    }

    @Test public void testEqualsForNull() {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        assertFalse(ConfigurationImpl.equals(props, null));
        assertFalse(ConfigurationImpl.equals(null, props));
        assertTrue(ConfigurationImpl.equals(null, null));

    }
}
