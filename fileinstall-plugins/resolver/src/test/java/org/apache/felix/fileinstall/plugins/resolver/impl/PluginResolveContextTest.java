/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.apache.felix.fileinstall.plugins.resolver.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

@SuppressWarnings("serial")
public class PluginResolveContextTest {

    @Test
    public void testMatchRequirementsEffectiveResolve() throws Exception {
        assertTrue("effective should match: req=default, cap=default",
                PluginResolveContext.match(createRequirement("foo", "(foo=bar)", null),
                        createCapability("foo", null, new HashMap<String, Object>() {
                            {
                                put("foo", "bar");
                            }
                        })));
        assertTrue("effective should match: req=default, cap=resolve",
                PluginResolveContext.match(createRequirement("foo", "(foo=bar)", null),
                        createCapability("foo", "resolve", new HashMap<String, Object>() {
                            {
                                put("foo", "bar");
                            }
                        })));
        assertTrue("effective should match: req=resolve, cap=default",
                PluginResolveContext.match(createRequirement("foo", "(foo=bar)", "resolve"),
                        createCapability("foo", null, new HashMap<String, Object>() {
                            {
                                put("foo", "bar");
                            }
                        })));
        assertTrue("effective should match: req=resolve, cap=resolve",
                PluginResolveContext.match(createRequirement("foo", "(foo=bar)", "resolve"),
                        createCapability("foo", "resolve", new HashMap<String, Object>() {
                            {
                                put("foo", "bar");
                            }
                        })));
    }

    @Test
    public void testEffectiveResolveCapsMatchAnyRequirement() throws Exception {
        // Caps with effective=resolve (or default) match requirements with any
        // effective=...
        assertTrue("effective should match: req=blah, cap=default",
                PluginResolveContext.match(createRequirement("foo", "(foo=bar)", "blah"),
                        createCapability("foo", null, new HashMap<String, Object>() {
                            {
                                put("foo", "bar");
                            }
                        })));
        assertTrue("effective should match: req=blah, cap=resolve",
                PluginResolveContext.match(createRequirement("foo", "(foo=bar)", "blah"),
                        createCapability("foo", "resolve", new HashMap<String, Object>() {
                            {
                                put("foo", "bar");
                            }
                        })));
    }

    @Test
    public void testEffectiveNonResolveCapsMatchRequirementWithSame() throws Exception {
        // Caps with effective=!resolve ONLY match reqs with the same effective
        assertTrue("effective should match: req=blah, cap=blah",
                PluginResolveContext.match(createRequirement("foo", "(foo=bar)", "blah"),
                        createCapability("foo", "blah", new HashMap<String, Object>() {
                            {
                                put("foo", "bar");
                            }
                        })));
        assertFalse("effective should NOT match: req=default, cap=blah",
                PluginResolveContext.match(createRequirement("foo", "(foo=bar)", null),
                        createCapability("foo", "blah", new HashMap<String, Object>() {
                            {
                                put("foo", "bar");
                            }
                        })));
        assertFalse("effective should NOT match: req=default, cap=blah",
                PluginResolveContext.match(createRequirement("foo", "(foo=bar)", "resolve"),
                        createCapability("foo", "blah", new HashMap<String, Object>() {
                            {
                                put("foo", "bar");
                            }
                        })));
        assertFalse("effective should NOT match: req=wibble, cap=blah",
                PluginResolveContext.match(createRequirement("foo", "(foo=bar)", "wibble"),
                        createCapability("foo", "blah", new HashMap<String, Object>() {
                            {
                                put("foo", "bar");
                            }
                        })));
    }

    static RequirementImpl createRequirement(String ns, String filter, String effective) {
        Map<String, String> directives = new HashMap<>();
        if (filter != null) {
            directives.put("filter", "(foo=bar)");
        }
        if (effective != null) {
            directives.put("effective", effective);
        }

        return new RequirementImpl(ns, directives, Collections.<String, Object>emptyMap(), null);
    }

    static CapabilityImpl createCapability(String ns, String effective, Map<String, Object> attrs) {
        Map<String, String> directives = new HashMap<>();
        if (effective != null) {
            directives.put("effective", effective);
        }

        return new CapabilityImpl(ns, directives, attrs, null);
    }

}
