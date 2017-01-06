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

import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.Hashtable;

import org.junit.Test;

public class ConfigurationImplTest {

    @Test public void testEqualsWithArrays() {
        final Dictionary<String, Object> props1 = new Hashtable<String, Object>();
        props1.put("array", new long[] {1,2});

        final Dictionary<String, Object> props2 = new Hashtable<String, Object>();
        props2.put("array", new long[] {1,2});

        assertTrue(ConfigurationImpl.equals(props1, props2));

        props2.put("array", new Long[] {1L,2L});
        assertTrue(ConfigurationImpl.equals(props1, props2));
    }
}
