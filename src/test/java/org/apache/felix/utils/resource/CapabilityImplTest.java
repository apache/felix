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

import org.mockito.Mockito;
import org.osgi.resource.Resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotEquals;

public class CapabilityImplTest extends TestCase {

    public void testCapability() {
        Map<String, Object> attrs = Collections.<String,Object>singletonMap("foo", "bar");
        Map<String, String> dirs = Collections.emptyMap();
        CapabilityImpl c = new CapabilityImpl(Mockito.mock(Resource.class), "org.foo.bar", dirs, attrs);

        assertEquals("org.foo.bar", c.getNamespace());
        assertEquals(attrs, c.getAttributes());
        assertEquals(dirs, c.getDirectives());
        assertNotNull(c.getResource());
    }

    public void testCapabilityEqualsHashcode() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("ding", "dong");
        attrs.put("la", "la");
        Map<String, String> dirs = Collections.singletonMap("a", "b");
        Resource res = Mockito.mock(Resource.class);
        CapabilityImpl c1 = new CapabilityImpl(res, "org.foo.bar", dirs, attrs);
        assertEquals(res, c1.getResource());

        CapabilityImpl c2 = new CapabilityImpl(res, "org.foo.bar", dirs, attrs);
        assertEquals(c1.toString(), c2.toString());

        CapabilityImpl c3 = new CapabilityImpl(res, "org.foo.bar2", dirs, attrs);
        assertNotEquals(c1.toString(), c3.toString());
    }

    public void testCopyCapability() {
        Resource res = Mockito.mock(Resource.class);
        CapabilityImpl c = new CapabilityImpl(res, "x.y.z",
                Collections.<String, String>singletonMap("x", "y"),
                Collections.<String, Object>singletonMap("a", 123));

        Resource res2 = Mockito.mock(Resource.class);
        CapabilityImpl c2 = new CapabilityImpl(res2, c);
        assertNotEquals("Should not be equal, the resources are different", c, c2);

        CapabilityImpl c3 = new CapabilityImpl(res, c);
        assertEquals(c.toString(), c3.toString());
    }
}
