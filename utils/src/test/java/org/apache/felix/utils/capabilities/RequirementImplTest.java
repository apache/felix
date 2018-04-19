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
package org.apache.felix.utils.capabilities;

import junit.framework.TestCase;

import org.mockito.Mockito;
import org.osgi.resource.Resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RequirementImplTest extends TestCase {
    public void testRequirement() {
        Map<String, Object> attrs = Collections.<String,Object>singletonMap("foo", "bar");
        Map<String, String> dirs = Collections.emptyMap();
        RequirementImpl r = new RequirementImpl("org.foo.bar", attrs, dirs);

        assertEquals("org.foo.bar", r.getNamespace());
        assertEquals(attrs, r.getAttributes());
        assertEquals(dirs, r.getDirectives());
        assertNull(r.getResource());
    }

    public void testRequirementEqualsHashcode() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put("ding", "dong");
        attrs.put("la", "la");
        Map<String, String> dirs = Collections.singletonMap("a", "b");
        Resource res = Mockito.mock(Resource.class);
        RequirementImpl r1 = new RequirementImpl("org.foo.bar", attrs, dirs, res);
        assertEquals(res, r1.getResource());

        RequirementImpl r2 = new RequirementImpl("org.foo.bar", attrs, dirs);
        r2.setResource(res);
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());

        RequirementImpl r3 = new RequirementImpl("org.foo.bar2", attrs, dirs, res);
        assertFalse(r1.equals(r3));
        assertFalse(r1.hashCode() == r3.hashCode());
    }

    public void testRequirementFilter() {
        RequirementImpl r = new RequirementImpl("lala", "(x=y)");
        assertEquals("lala", r.getNamespace());
        assertEquals(0, r.getAttributes().size());
        assertEquals(Collections.singletonMap("filter", "(x=y)"), r.getDirectives());

        RequirementImpl r2 = new RequirementImpl("lala", null);
        assertEquals("lala", r2.getNamespace());
        assertEquals(0, r2.getAttributes().size());
        assertEquals(0, r2.getDirectives().size());
    }
}
