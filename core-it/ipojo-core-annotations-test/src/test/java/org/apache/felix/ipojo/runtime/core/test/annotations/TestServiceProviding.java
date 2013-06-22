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

import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ParseUtils;
import org.apache.felix.ipojo.runtime.core.test.services.CheckService;
import org.apache.felix.ipojo.runtime.core.test.services.FooService;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;

import java.util.List;

import static junit.framework.Assert.*;

public class TestServiceProviding extends Common {

    @Test
    public void testProvidesSimple() {
        Element meta = IPOJOHelper.getMetadata(getTestBundle(), "org.apache.felix.ipojo.runtime.core.test.components.ProvidesSimple");
        Element[] provs = meta.getElements("provides");
        assertNotNull("Provides exists ", provs);
    }

    @Test
    public void testProvidesDouble() {
        Element meta = IPOJOHelper.getMetadata(getTestBundle(), "org.apache.felix.ipojo.runtime.core.test.components.ProvidesDouble");
        Element[] provs = meta.getElements("provides");
        assertNotNull("Provides exists ", provs);
    }

    @Test
    public void testProvidesTriple() {
        Element meta = IPOJOHelper.getMetadata(getTestBundle(), "org.apache.felix.ipojo.runtime.core.test.components.ProvidesTriple");
        Element[] provs = meta.getElements("provides");
        assertNotNull("Provides exists ", provs);
        Element prov = provs[0];
        String itfs = prov.getAttribute("specifications");
        List list = ParseUtils.parseArraysAsList(itfs);
        assertTrue("Provides CS ", list.contains(CheckService.class.getName()));
    }

    @Test
    public void testProvidesQuatro() {
        Element meta = IPOJOHelper.getMetadata(getTestBundle(), "org.apache.felix.ipojo.runtime.core.test.components.ProvidesQuatro");
        Element[] provs = meta.getElements("provides");
        assertNotNull("Provides exists ", provs);
        Element prov = provs[0];
        String itfs = prov.getAttribute("specifications");
        List list = ParseUtils.parseArraysAsList(itfs);
        assertTrue("Provides CS ", list.contains(CheckService.class.getName()));
        assertTrue("Provides Foo ", list.contains(FooService.class.getName()));
    }

    @Test
    public void testProperties() {
        Element meta = IPOJOHelper.getMetadata(getTestBundle(), "org.apache.felix.ipojo.runtime.core.test.components.ProvidesProperties");
        Element[] provs = meta.getElements("provides");
        assertNotNull("Provides exists ", provs);
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
        assertEquals("Check mandatory value", "true", bar.getAttribute("mandatory"));
        //Boo
        Element boo = getPropertyByName(props, "boo");
        assertEquals("Check boo field", "boo", boo.getAttribute("field"));
        //Baa
        Element baa = getPropertyByName(props, "baa");
        assertEquals("Check baa field", "m_baa", baa.getAttribute("field"));
        assertEquals("Check baa name", "baa", baa.getAttribute("name"));

        //Bar
        Element baz = getPropertyByName(props, "baz");
        assertEquals("Check baz field", "m_baz", baz.getAttribute("field"));
        assertEquals("Check baz name", "baz", baz.getAttribute("name"));
    }

    @Test
    public void testStaticProperties() {
        Element meta = IPOJOHelper.getMetadata(getTestBundle(), "org.apache.felix.ipojo.runtime.core.test.components.ProvidesStaticProperties");
        Element[] provs = meta.getElements("provides");
        assertNotNull("Provides exists ", provs);
        Element prov = provs[0];
        Element[] props = prov.getElements("property");
        assertEquals("Number of properties", props.length, 9);
        //Prop1
        Element foo = getPropertyByName(props, "prop1");
        assertNull(foo.getAttribute("field"));
        assertEquals("prop1", foo.getAttribute("value"));

        //Prop2
        Element prop2 = getPropertyByName(props, "prop2");
        assertNull(prop2.getAttribute("field"));
        assertNull(prop2.getAttribute("value"));

        // Props
        Element prop = getPropertyByName(props, "props");
        assertNull(prop.getAttribute("field"));
        assertEquals("{prop1, prop2}", prop.getAttribute("value"));

        // Mandatory
        Element mandatory = getPropertyByName(props, "mandatory1");
        assertNull(mandatory.getAttribute("field"));
        assertNull(mandatory.getAttribute("value"));
        assertEquals("true", mandatory.getAttribute("mandatory"));

        //Bar
        Element bar = getPropertyByName(props, "bar");
        assertEquals("Check bar field", "bar", bar.getAttribute("field"));
        assertEquals("Check bar value", "4", bar.getAttribute("value"));
        assertEquals("Check mandatory value", "true", bar.getAttribute("mandatory"));
        //Boo
        Element boo = getPropertyByName(props, "boo");
        assertEquals("Check boo field", "boo", boo.getAttribute("field"));
        //Baa
        Element baa = getPropertyByName(props, "baa");
        assertEquals("Check baa field", "m_baa", baa.getAttribute("field"));
        assertEquals("Check baa name", "baa", baa.getAttribute("name"));

        //Bar
        Element baz = getPropertyByName(props, "baz");
        assertEquals("Check baz field", "m_baz", baz.getAttribute("field"));
        assertEquals("Check baz name", "baz", baz.getAttribute("name"));
    }

    @Test
    public void testServiceController() {
        Element meta = IPOJOHelper.getMetadata(getTestBundle(), "org.apache.felix.ipojo.runtime.core.test.components.PSServiceController");
        Element[] provs = meta.getElements("provides");
        assertNotNull("Provides exists ", provs);
        System.out.println(provs[0].toString());
        assertNotNull(provs[0].getElements("controller"));
        assertEquals(1, provs[0].getElements("controller").length);
        assertEquals("false", provs[0].getElements("controller")[0].getAttribute("value"));
    }

    @Test
    public void testServiceControllerWithSpecification() {
        Element meta = IPOJOHelper.getMetadata(getTestBundle(), "org.apache.felix.ipojo.runtime.core.test.components.PSServiceControllerSpec");
        Element[] provs = meta.getElements("provides");
        assertNotNull("Provides exists ", provs);
        System.out.println(provs[0].toString());
        assertNotNull(provs[0].getElements("controller"));
        assertEquals(2, provs[0].getElements("controller").length);
        assertEquals("false", provs[0].getElements("controller")[0].getAttribute("value"));
        assertEquals(FooService.class.getName(), provs[0].getElements("controller")[0].getAttribute("specification"));
    }

    /**
     * Checks that declared static properties.
     * It used 'org.apache.felix.ipojo.runtime.core.test.components.components.ComponentWithProperties'
     * This test is related to : https://issues.apache.org/jira/browse/FELIX-4053
     */
    @Test
    public void testPublishedStaticProperties() {
        ServiceReference reference = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(),
                "instanceWithProperties");
        assertNotNull(reference);
    }

    private Element getPropertyByName(Element[] props, String name) {
        for (Element prop : props) {
            String na = prop.getAttribute("name");
            String field = prop.getAttribute("field");
            if (na != null && na.equalsIgnoreCase(name)) {
                return prop;
            }
            if (field != null && field.equalsIgnoreCase(name)) {
                return prop;
            }
        }
        fail("Property  " + name + " not found");
        return null;
    }


}
