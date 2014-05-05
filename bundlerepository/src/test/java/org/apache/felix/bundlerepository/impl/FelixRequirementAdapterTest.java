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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class FelixRequirementAdapterTest extends TestCase
{
    public void testDirectiveTranslation()
    {
        assertFilter("(foo=bar)", "(foo=bar)");
        assertFilter("(package=x.y.z)", "(osgi.wiring.package=x.y.z)");
        // TODO should this be symbolicname?
        assertFilter("( bundle = abc  )", "(osgi.wiring.bundle= abc  )");
        assertFilter("(service=xyz)", "(osgi.service=xyz)");
        assertFilter("(|(bundle=x)(&(bundle=y)(fragment=z)))",
                "(|(osgi.wiring.bundle=x)(&(osgi.wiring.bundle=y)(osgi.wiring.host=z)))");
    }

    private void assertFilter(String obr, String osgi)
    {
        Resource resource = new OSGiResourceImpl(
            Collections.<Capability>emptyList(),
            Collections.<Requirement>emptyList());

        RequirementImpl requirement = new RequirementImpl();
        requirement.setFilter(obr);
        assertEquals(osgi, new FelixRequirementAdapter(requirement, resource).getDirectives().get("filter"));
    }

    public void testOtherDirectives()
    {
        Resource resource = new OSGiResourceImpl(
            Collections.<Capability>emptyList(),
            Collections.<Requirement>emptyList());

        RequirementImpl requirement = new RequirementImpl();
        requirement.setFilter("(a=b)");
        Map<String, String> other = new HashMap<String, String>();
        other.put("xyz", "abc");
        requirement.setDirectives(other);

        FelixRequirementAdapter adapter = new FelixRequirementAdapter(requirement, resource);

        Map<String, String> expected = new HashMap<String, String>();
        expected.put("filter", "(a=b)");
        expected.put("xyz", "abc");
        assertEquals(expected, adapter.getDirectives());
    }
}
