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

public class RequirementImplTest extends TestCase {
    public void testRequirement() {
        Map<String, Object> attrs = Collections.<String,Object>singletonMap("foo", "bar");
        Map<String, String> dirs = Collections.emptyMap();
        RequirementImpl r = new RequirementImpl(Mockito.mock(Resource.class),"org.foo.bar", dirs, attrs);

        assertEquals("org.foo.bar", r.getNamespace());
        assertEquals(attrs, r.getAttributes());
        assertEquals(dirs, r.getDirectives());
        assertNotNull(r.getResource());
    }

    public void testRequirementEqualsHashcode() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("ding", "dong");
        attrs.put("la", "la");
        Map<String, String> dirs = Collections.singletonMap("a", "b");
        Resource res = Mockito.mock(Resource.class);
        RequirementImpl r1 = new RequirementImpl(res, "org.foo.bar", dirs, attrs);
        assertEquals(res, r1.getResource());

        RequirementImpl r2 = new RequirementImpl(res, "org.foo.bar", dirs, attrs);
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());

        RequirementImpl r3 = new RequirementImpl(res, "org.foo.bar2", dirs, attrs);
        assertNotEquals(r1, r3);
        assertNotEquals(r1.hashCode(), r3.hashCode());
    }

    public void testRequirementFilter() {
        RequirementImpl r = new RequirementImpl(Mockito.mock(Resource.class), "lala", "(x=y)");
        assertEquals("lala", r.getNamespace());
        assertEquals(0, r.getAttributes().size());
        assertEquals(Collections.singletonMap("filter", "(x=y)"), r.getDirectives());

        RequirementImpl r2 = new RequirementImpl(Mockito.mock(Resource.class), "lala", null);
        assertEquals("lala", r2.getNamespace());
        assertEquals(0, r2.getAttributes().size());
        assertEquals(0, r2.getDirectives().size());
    }

    public void testCopyRequirement() {
        Resource res1 = Mockito.mock(Resource.class);

        RequirementImpl r = new RequirementImpl(res1,
                "x.y.z",
                Collections.<String, String>singletonMap("x", "y"),
                Collections.<String, Object>singletonMap("a", 123));

        Resource res2 = Mockito.mock(Resource.class);
        RequirementImpl r2 = new RequirementImpl(res2, r);
        assertNotEquals("Should not be equal, the resources are different", r, r2);

        RequirementImpl r3 = new RequirementImpl(res1, r);
        assertEquals(r, r3);
    }
}
