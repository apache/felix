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
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;

public class TestDependency extends Common {


    @Test
    public void testDependencyDeclaration() {
        Element meta = ipojoHelper.getMetadata(getTestBundle(),  "org.apache.felix.ipojo.runtime.core.test.components.Dependency");
        Element[] deps = meta.getElements("requires");

        // Check fs
        Element dep = getDependencyById(deps, "fs");
        String field = dep.getAttribute("field");
        String id = dep.getAttribute("id");
        String bind = getBind(dep);
        String unbind = getUnbind(dep);
        assertNotNull("Check fs field", field);
        assertEquals("Check fs field", "fs", field);
        assertNull("Check fs bind", bind);
        assertNull("Check fs unbind", unbind);
        assertNull("Check fs id", id);

        // Check bar
        dep = getDependencyById(deps, "Bar");
        field = dep.getAttribute("field");
        id = dep.getAttribute("id");
        bind = getBind(dep);
        unbind = getUnbind(dep);
        assertNull("Check bar field", field);
        assertEquals("Check bar bind", "bindBar", bind);
        assertEquals("Check bar unbind", "unbindBar", unbind);
        assertEquals("Check bar id", "Bar", id);

        // Check baz
        dep = getDependencyById(deps, "Baz");
        field = dep.getAttribute("field");
        id = dep.getAttribute("id");
        bind = getBind(dep);
        unbind = getUnbind(dep);
        assertNull("Check baz field", field);
        assertEquals("Check baz bind", "bindBaz", bind);
        assertEquals("Check baz unbind", "unbindBaz", unbind);
        assertEquals("Check baz id", "Baz", id);

        // Check fs2
        dep = getDependencyById(deps, "fs2");
        field = dep.getAttribute("field");
        id = dep.getAttribute("id");
        bind = getBind(dep);
        unbind = getUnbind(dep);
        assertNotNull("Check fs2 field", field);
        assertEquals("Check fs2 field", "fs2", field);
        assertEquals("Check fs2 bind", "bindFS2", bind);
        assertEquals("Check fs2 unbind", "unbindFS2", unbind);

        // Check fs2inv
        dep = getDependencyById(deps, "fs2inv");
        field = dep.getAttribute("field");
        id = dep.getAttribute("id");
        bind = getBind(dep);
        unbind = getUnbind(dep);
        assertNotNull("Check fs2inv field", field);
        assertEquals("Check fs2 field", "fs2inv", field);
        assertEquals("Check fs2 bind", "bindFS2Inv", bind);
        assertEquals("Check fs2 unbind", "unbindFS2Inv", unbind);
        assertEquals("Check fs2 id", "inv", id);

        // Check mod
        dep = getDependencyById(deps, "mod");
        id = dep.getAttribute("id");
        bind = getBind(dep);
        unbind = getUnbind(dep);
        String mod = getModified(dep);
        assertEquals("Check mod bind", "bindMod", bind);
        assertEquals("Check mod unbind", "unbindMod", unbind);
        assertEquals("Check mod modified", "modifiedMod", mod);
        assertEquals("Check mod id", "mod", id);

        // Check not proxied
        dep = getDependencyById(deps, "notproxied");
        assertEquals("Check not proxied", "false", dep.getAttribute("proxy"));
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

    private String getBind(Element dep) {
        Element[] elem = dep.getElements("callback");
        for (int i = 0; elem != null && i < elem.length; i++) {
            if (elem[i].getAttribute("type").equalsIgnoreCase("bind")) {
                return elem[i].getAttribute("method");
            }
        }
        return null;
    }

    private String getUnbind(Element dep) {
        Element[] elem = dep.getElements("callback");
        for (int i = 0; elem != null && i < elem.length; i++) {
            if (elem[i].getAttribute("type").equalsIgnoreCase("unbind")) {
                return elem[i].getAttribute("method");
            }
        }
        return null;
    }

    private String getModified(Element dep) {
        Element[] elem = dep.getElements("callback");
        for (int i = 0; elem != null && i < elem.length; i++) {
            if (elem[i].getAttribute("type").equalsIgnoreCase("modified")) {
                return elem[i].getAttribute("method");
            }
        }
        return null;
    }

}
