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
package org.apache.felix.ipojo.runtime.core.instantiator;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.runtime.core.Common;
import org.apache.felix.ipojo.runtime.core.services.BarService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.*;

public class TestOptionalMultipleInstantiation extends Common {

    private ComponentFactory bar2Factory;
    private ComponentInstance empty;

    @Before
    public void setUp() {
        bar2Factory = (ComponentFactory) ipojoHelper.getFactory("composite.bar.4");
        assertNotNull("Check bar2factory availability", bar2Factory);

        Factory fact = ipojoHelper.getFactory("composite.empty");
        Properties props = new Properties();
        props.put("instance.name", "empty");
        try {
            empty = fact.createComponentInstance(props);
        } catch (Exception e) {
            fail("Cannot create the empty composite : " + e.getMessage());
        }
    }

    @After
    public void tearDown() {
        empty.dispose();
        empty = null;
    }

    @Test
    public void testCreation() {
        Properties props = new Properties();
        props.put("instance.name", "under");
        ComponentInstance under = null;
        try {
            under = bar2Factory.createComponentInstance(props);
        } catch (Exception e) {
            fail("Cannot instantiate under : " + e.getMessage());
        }
        assertTrue("Check instance validity", under.getState() == ComponentInstance.VALID);
        under.dispose();
    }

    @Test
    public void testServiceAvailability() {
        Properties props = new Properties();
        props.put("instance.name", "under");
        ComponentInstance under = null;
        try {
            under = bar2Factory.createComponentInstance(props);
        } catch (Exception e) {
            fail("Cannot instantiate under : " + e.getMessage());
        }
        assertTrue("Check instance validity", under.getState() == ComponentInstance.VALID);
        ServiceContext sc = getServiceContext(under);
        assertNotNull("Check service availability", sc.getServiceReference(BarService.class.getName()));
        int count = ipojoHelper.getServiceReferences(sc, BarService.class.getName(), null).length;
        assertEquals("Check service provider number : " + count, count, 3);

        under.dispose();
    }

    @Test
    public void testCreationLevel2() {
        ServiceContext sc = getServiceContext(empty);
        Properties props = new Properties();
        props.put("instance.name", "under");
        ComponentInstance under = null;
        try {
            under = bar2Factory.createComponentInstance(props, sc);
        } catch (Exception e) {
            fail("Cannot instantiate under : " + e.getMessage());
        }
        assertTrue("Check instance validity", under.getState() == ComponentInstance.VALID);
        under.dispose();
    }

    @Test
    public void testServiceAvailabilityLevel2() {
        ServiceContext sc = getServiceContext(empty);
        Properties props = new Properties();
        props.put("instance.name", "under");
        ComponentInstance under = null;
        try {
            under = bar2Factory.createComponentInstance(props, sc);
        } catch (Exception e) {
            fail("Cannot instantiate under : " + e.getMessage());
        }
        assertTrue("Check instance validity", under.getState() == ComponentInstance.VALID);
        ServiceContext sc2 = getServiceContext(under);
        assertNotNull("Check service availability", sc2.getServiceReference(BarService.class.getName()));
        assertEquals("Check service provider number", ipojoHelper.getServiceReferences(sc2,
                BarService.class.getName(), null).length, 3);

        under.dispose();
    }

    @Test
    public void testFactoryManagement() {
        Properties props = new Properties();
        props.put("instance.name", "under");
        ComponentInstance under = null;
        try {
            under = bar2Factory.createComponentInstance(props);
        } catch (Exception e) {
            fail("Cannot instantiate under : " + e.getMessage());
        }
        ServiceContext sc = getServiceContext(under);
        assertTrue("Check instance validity - 1", under.getState() == ComponentInstance.VALID);

        ComponentFactory fact1 = (ComponentFactory) ipojoHelper.getFactory("COMPO-FooBarProviderType-1");
        ComponentFactory fact2 = (ComponentFactory) ipojoHelper.getFactory("COMPO-FooBarProviderType-2");
        ComponentFactory fact3 = (ComponentFactory) ipojoHelper.getFactory("COMPO-FooBarProviderType-3");

        fact1.stop();
        assertTrue("Check instance validity - 2", under.getState() == ComponentInstance.VALID);
        assertEquals("Check service provider number", ipojoHelper.getServiceReferences(sc,
                BarService.class.getName(), null).length, 2);

        fact2.stop();
        assertTrue("Check instance validity - 3", under.getState() == ComponentInstance.VALID);
        assertEquals("Check service provider number", ipojoHelper.getServiceReferences(sc,
                BarService.class.getName(), null).length, 1);

        fact3.stop();
        assertTrue("Check instance validity - 4", under.getState() == ComponentInstance.VALID);
        assertEquals("Check service provider number", ipojoHelper.getServiceReferences(sc,
                BarService.class.getName(), null).length, 0);

        fact1.start();
        assertEquals("Check service provider number", ipojoHelper.getServiceReferences(sc,
                BarService.class.getName(), null).length, 1);
        assertTrue("Check instance validity - 5", under.getState() == ComponentInstance.VALID);

        under.dispose();
        fact2.start();
        fact3.start();
    }

    @Test
    public void testFactoryManagementLevel2() {
        ServiceContext sc = getServiceContext(empty);
        Properties props = new Properties();
        props.put("instance.name", "under");
        ComponentInstance under = null;
        try {
            under = bar2Factory.createComponentInstance(props, sc);
        } catch (Exception e) {
            fail("Cannot instantiate under : " + e.getMessage());
        }
        assertTrue("Check instance validity - 1", under.getState() == ComponentInstance.VALID);
        ServiceContext sc2 = getServiceContext(under);

        ComponentFactory fact1 = (ComponentFactory) ipojoHelper.getFactory("COMPO-FooBarProviderType-1");
        ComponentFactory fact2 = (ComponentFactory) ipojoHelper.getFactory("COMPO-FooBarProviderType-2");
        ComponentFactory fact3 = (ComponentFactory) ipojoHelper.getFactory("COMPO-FooBarProviderType-3");

        fact1.stop();
        assertTrue("Check instance validity - 2", under.getState() == ComponentInstance.VALID);
        assertEquals("Check service provider number", ipojoHelper.getServiceReferences(sc2,
                BarService.class.getName(), null).length, 2);

        fact2.stop();
        assertTrue("Check instance validity - 3", under.getState() == ComponentInstance.VALID);
        assertEquals("Check service provider number", ipojoHelper.getServiceReferences(sc2,
                BarService.class.getName(), null).length, 1);

        fact3.stop();
        assertTrue("Check instance validity - 4", under.getState() == ComponentInstance.VALID);
        assertEquals("Check service provider number", ipojoHelper.getServiceReferences(sc2,
                BarService.class.getName(), null).length, 0);

        fact1.start();
        assertTrue("Check instance validity - 5", under.getState() == ComponentInstance.VALID);
        assertEquals("Check service provider number", ipojoHelper.getServiceReferences(sc2,
                BarService.class.getName(), null).length, 1);

        under.dispose();
        fact2.start();
        fact3.start();
    }

}
