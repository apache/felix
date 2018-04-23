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
package org.apache.felix.utils.resource;

import junit.framework.TestCase;

import org.osgi.resource.Capability;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CapabilitySetTest extends TestCase {
    public void testMatchesRequirement() {
        RequirementImpl r = new RequirementImpl(null, "foo",
                Collections.singletonMap("filter", "(a=b)"), null);

        Capability c1 = new CapabilityImpl(null, "foo", null,
                Collections.<String, Object>singletonMap("a", "b"));
        Capability c2 = new CapabilityImpl(null, "foox", null,
                Collections.<String, Object>singletonMap("a", "b"));
        Capability c3 = new CapabilityImpl(null, "bar", null,
                Collections.<String, Object>singletonMap("a", "b"));
        Capability c4 = new CapabilityImpl(null, "foo", null,
                Collections.<String, Object>singletonMap("a", "c"));
        assertTrue(CapabilitySet.matches(c1, r));
        assertFalse(CapabilitySet.matches(c2, r));
        assertFalse(CapabilitySet.matches(c3, r));
        assertFalse(CapabilitySet.matches(c4, r));

        RequirementImpl r2 = new RequirementImpl(null, "foo", null, null);
        assertTrue(CapabilitySet.matches(c1, r2));
        assertFalse(CapabilitySet.matches(c2, r2));
        assertFalse(CapabilitySet.matches(c3, r2));
        assertTrue(CapabilitySet.matches(c4, r2));

        Map<String, Object> m = new HashMap<>();
        m.put("a", "b");
        m.put("c", "d");
        Capability c5 = new CapabilityImpl(null, "foo", null, m);
        assertTrue(CapabilitySet.matches(c5, r));
    }
}
