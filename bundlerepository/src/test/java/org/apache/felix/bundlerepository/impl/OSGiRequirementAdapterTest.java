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

import org.osgi.resource.Requirement;

public class OSGiRequirementAdapterTest extends TestCase
{
    public void testDirectives()
    {
        Map<String, Object> attrs = new HashMap<String, Object>();
        Map<String, String> dirs = new HashMap<String, String>();
        dirs.put("cardinality", "multiple");
        dirs.put("filter", "(osgi.wiring.package=y)");
        dirs.put("foo", "bar");
        dirs.put("resolution", "optional");
        dirs.put("test", "test");

        Requirement req = new OSGiRequirementImpl("osgi.wiring.package", attrs, dirs);
        OSGiRequirementAdapter adapter = new OSGiRequirementAdapter(req);

        assertEquals("(package=y)", adapter.getFilter());
        assertTrue(adapter.isMultiple());
        assertTrue(adapter.isOptional());
        assertEquals("package", adapter.getName());

        Map<String, String> expected = new HashMap<String, String>();
        expected.put("foo", "bar");
        expected.put("test", "test");
        assertEquals(expected, adapter.getDirectives());
    }
}
