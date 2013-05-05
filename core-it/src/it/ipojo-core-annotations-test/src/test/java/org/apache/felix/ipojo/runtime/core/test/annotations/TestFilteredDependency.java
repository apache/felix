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

package org.apache.felix.ipojo.runtime.core.test.annotations;

import org.apache.felix.ipojo.metadata.Element;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class TestFilteredDependency extends Common {

    private Element[] deps;
    private Element[] froms;


    @Before
    public void setUp() {
        Element meta = ipojoHelper.getMetadata(getTestBundle(),  "org.apache.felix.ipojo.runtime.core.test.components.FilteredDependency");
        deps = meta.getElements("requires");

        Element meta2 = ipojoHelper.getMetadata(getTestBundle(),  "org.apache.felix.ipojo.runtime.core.test.components.FromDependency");
        froms = meta2.getElements("requires");
    }

    @Test
    public void testField() {
        Element dep = getDependencyById(deps, "fs");
        String opt = dep.getAttribute("filter");
        assertEquals("Check filter", "(foo=bar)", opt);
    }

    @Test
    public void testCallbackBind() {
        Element dep = getDependencyById(deps, "Bar");
        String opt = dep.getAttribute("filter");
        assertEquals("Check filter", "(foo=bar)", opt);
    }

    @Test
    public void testCallbackUnbind() {
        Element dep = getDependencyById(deps, "Baz");
        String opt = dep.getAttribute("filter");
        assertEquals("Check filter", "(foo=bar)", opt);
    }

    @Test
    public void testBoth() {
        Element dep = getDependencyById(deps, "inv");
        String opt = dep.getAttribute("filter");
        assertEquals("Check filter", "(foo=bar)", opt);
    }

    @Test
    public void testBindOnly() {
        Element dep = getDependencyById(deps, "bindonly");
        String opt = dep.getAttribute("filter");
        assertEquals("Check filter", "(foo=bar)", opt);
    }

    @Test
    public void testUnbindOnly() {
        Element dep = getDependencyById(deps, "unbindonly");
        String opt = dep.getAttribute("filter");
        assertEquals("Check filter", "(foo=bar)", opt);
    }

    @Test
    public void testFromField() {
        Element dep = getDependencyById(froms, "fs");
        String from = dep.getAttribute("from");
        assertEquals("Check from", "X", from);
    }

    @Test
    public void testFromBind() {
        Element dep = getDependencyById(froms, "fs2");
        String from = dep.getAttribute("from");
        assertEquals("Check from", "X", from);
    }

    @Test
    public void testFromUnbind() {
        Element dep = getDependencyById(froms, "inv");
        String from = dep.getAttribute("from");
        assertEquals("Check from", "X", from);
    }

    @Test
    public void testNoFrom() {
        Element dep = getDependencyById(froms, "Bar");
        String from = dep.getAttribute("from");
        assertNull("Check from", from);
    }


    private Element getDependencyById(Element[] deps, String name) {
        for (int i = 0; i < deps.length; i++) {
            String na = deps[i].getAttribute("id");
            String field = deps[i].getAttribute("field");
            if (na != null && na.equalsIgnoreCase(name)) {
                return deps[i];
            }
            if (field != null && field.equalsIgnoreCase(name)) {
                return deps[i];
            }
        }
        fail("Dependency  " + name + " not found");
        return null;
    }


}
