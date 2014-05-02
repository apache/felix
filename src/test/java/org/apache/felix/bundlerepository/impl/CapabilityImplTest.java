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
package org.apache.felix.bundlerepository.impl;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class CapabilityImplTest extends TestCase
{
    public void testDirectives()
    {
        CapabilityImpl c = new CapabilityImpl();

        assertEquals(0, c.getDirectives().size());
        c.addDirective("x", "y");
        assertEquals(1, c.getDirectives().size());
        assertEquals("y", c.getDirectives().get("x"));

        c.addDirective("x", "z");
        assertEquals(1, c.getDirectives().size());
        assertEquals("z", c.getDirectives().get("x"));

        c.addDirective("Y", "A b C");

        Map<String, String> expected = new HashMap<String, String>();
        expected.put("x", "z");
        expected.put("Y", "A b C");
        assertEquals(expected, c.getDirectives());
    }
}
