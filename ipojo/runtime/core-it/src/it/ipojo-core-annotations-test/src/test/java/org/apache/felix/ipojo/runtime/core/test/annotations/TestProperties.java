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

import static junit.framework.Assert.*;

public class TestProperties extends Common {

    @Test
    public void testProperties() {
        Element meta = ipojoHelper.getMetadata(getTestBundle(),  "org.apache.felix.ipojo.runtime.core.test.components.Properties");
        Element[] provs = meta.getElements("properties");
        assertNotNull("Properties exists ", provs);
        Element prov = provs[0];
        Element[] props = prov.getElements("property");
        assertEquals("Number of properties", props.length, 5);
        //Foo
        Element foo = getPropertyByName(props, "foo");
        assertEquals("Check foo field", "m_foo", foo.getAttribute("field"));
        assertEquals("Check foo name", "foo", foo.getAttribute("name"));
        //Bar
        Element bar = getPropertyByName(props, "bar");
        assertEquals("Check bar field", "bar", bar.getAttribute("field"));
        assertEquals("Check bar value", "4", bar.getAttribute("value"));
        //Boo
        Element boo = getPropertyByName(props, "boo");
        assertEquals("Check boo field", "boo", boo.getAttribute("field"));
        assertEquals("Check boo method", "setboo", boo.getAttribute("method"));
        //Baa
        Element baa = getPropertyByName(props, "baa");
        assertEquals("Check baa field", "m_baa", baa.getAttribute("field"));
        assertEquals("Check baa name", "baa", baa.getAttribute("name"));
        assertEquals("Check baa method", "setbaa", baa.getAttribute("method"));
        assertEquals("Check mandatory", "true", baa.getAttribute("mandatory"));


        //Bar
        Element baz = getPropertyByName(props, "baz");
        assertEquals("Check baz method", "setbaz", baz.getAttribute("method"));
        assertEquals("Check baz name", "baz", baz.getAttribute("name"));
    }

    @Test
    public void testAbsentPropagation() {
        Element meta = ipojoHelper.getMetadata(getTestBundle(),  "org.apache.felix.ipojo.runtime.core.test.components.Properties");
        Element[] provs = meta.getElements("properties");
        assertNotNull("Properties exists ", provs);
        Element prov = provs[0];
        String att = prov.getAttribute("propagation");
        assertNull("Propagation", att);
    }

    @Test
    public void testPropagation() {
        Element meta = ipojoHelper.getMetadata(getTestBundle(),  "org.apache.felix.ipojo.runtime.core.test.components.Propagation");
        Element[] provs = meta.getElements("properties");
        assertNotNull("Properties exists ", provs);
        Element prov = provs[0];
        String att = prov.getAttribute("propagation");
        assertNotNull("Propagation", att);
        assertEquals("Propagation value", "true", att);
    }

    @Test
    public void testNoPropagation() {
        Element meta = ipojoHelper.getMetadata(getTestBundle(),  "org.apache.felix.ipojo.runtime.core.test.components.NoPropagation");
        Element[] provs = meta.getElements("properties");
        assertNotNull("Properties exists ", provs);
        Element prov = provs[0];
        String att = prov.getAttribute("propagation");
        assertNotNull("Propagation", att);
        assertEquals("Propagation value", "false", att);
    }

    @Test
    public void testPID() {
        Element meta = ipojoHelper.getMetadata(getTestBundle(),  "org.apache.felix.ipojo.runtime.core.test.components.ManagedServicePID");
        Element[] provs = meta.getElements("properties");
        assertNotNull("Properties exists ", provs);
        Element prov = provs[0];
        String att = prov.getAttribute("pid");
        assertNotNull("PID", att);
        assertEquals("PID Value", "MyPID", att);
    }

    @Test
    public void testAbsentPID() {
        Element meta = ipojoHelper.getMetadata(getTestBundle(),  "org.apache.felix.ipojo.runtime.core.test.components.Properties");
        Element[] provs = meta.getElements("properties");
        assertNotNull("Properties exists ", provs);
        Element prov = provs[0];
        String att = prov.getAttribute("pid");
        assertNull("PID", att);
    }

    @Test
    public void testPropagationAndPID() {
        Element meta = ipojoHelper.getMetadata(getTestBundle(),  "org.apache.felix.ipojo.runtime.core.test.components.PropagationandPID");
        Element[] provs = meta.getElements("properties");
        assertNotNull("Properties exists ", provs);
        Element prov = provs[0];
        String att = prov.getAttribute("pid");
        assertNotNull("PID", att);
        assertEquals("PID Value", "MyPID", att);
        att = prov.getAttribute("propagation");
        assertNotNull("Propagation", att);
        assertEquals("Propagation value", "true", att);
    }

    @Test
    public void testPIDAndPropagation() {
        Element meta = ipojoHelper.getMetadata(getTestBundle(),  "org.apache.felix.ipojo.runtime.core.test.components.PIDandPropagation");
        Element[] provs = meta.getElements("properties");
        assertNotNull("Properties exists ", provs);
        Element prov = provs[0];
        String att = prov.getAttribute("pid");
        assertNotNull("PID", att);
        assertEquals("PID Value", "MyPID", att);
        att = prov.getAttribute("propagation");
        assertNotNull("Propagation", att);
        assertEquals("Propagation value", "true", att);
    }

    @Test
    public void testUpdatedAndPID() {
        Element meta = ipojoHelper.getMetadata(getTestBundle(),  "org.apache.felix.ipojo.runtime.core.test.components.UpdatedWithManagedService");
        Element[] provs = meta.getElements("properties");
        assertNotNull("Properties exists ", provs);
        Element prov = provs[0];
        String att = prov.getAttribute("pid");
        assertNotNull("PID", att);
        assertEquals("PID Value", "MyPID", att);

        att = prov.getAttribute("updated");
        assertNotNull("att", att);
        assertEquals("Updated Value", "after", att);
    }

    @Test
    public void testUpdatedAndProperties() {
        Element meta = ipojoHelper.getMetadata(getTestBundle(),  "org.apache.felix.ipojo.runtime.core.test.components.UpdatedWithProperties");
        Element[] provs = meta.getElements("properties");
        assertNotNull("Properties exists ", provs);
        Element prov = provs[0];
        String att = prov.getAttribute("pid");
        assertNull("PID", att);

        att = prov.getAttribute("updated");
        assertNotNull("att", att);
        assertEquals("Updated Value", "after", att);
    }

    private Element getPropertyByName(Element[] props, String name) {
        for (int i = 0; i < props.length; i++) {
            String na = props[i].getAttribute("name");
            String field = props[i].getAttribute("field");
            if (na != null && na.equalsIgnoreCase(name)) {
                return props[i];
            }
            if (field != null && field.equalsIgnoreCase(name)) {
                return props[i];
            }
        }
        fail("Property  " + name + " not found");
        return null;
    }


}
