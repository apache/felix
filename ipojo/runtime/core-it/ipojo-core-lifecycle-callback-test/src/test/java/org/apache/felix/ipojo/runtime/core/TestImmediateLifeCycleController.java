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
package org.apache.felix.ipojo.runtime.core;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import java.util.Properties;

import static org.junit.Assert.*;

public class TestImmediateLifeCycleController extends Common {

    private ComponentInstance under;
    private Factory factory;

    @Before
    public void setUp() {
        factory = ipojoHelper.getFactory("LFC-Test-Immediate");
    }

    @Test
    public void testOne() {
        Properties props = new Properties();
        props.put("conf", "foo");
        props.put("instance.name", "under");
        under = ipojoHelper.createComponentInstance("LFC-Test-Immediate", props);

        // The conf is correct, the PS must be provided
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), "under");
        assertNotNull("Check service availability -1", ref);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);
        assertTrue("Check state 1", cs.check());
        bc.ungetService(ref);

        // Reconfigure the instance with a bad configuration
        props.put("conf", "bar"); // Bar is a bad conf
        try {
            factory.reconfigure(props);
        } catch (Exception e) {
            fail("The reconfiguration is not unacceptable and seems unacceptable : " + props);
        }

        // The instance should now be invalid 
        ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), "under");
        assertNull("Check service availability -2", ref);

        // Reconfigure the instance with a valid configuration
        props.put("conf", "foo"); // Bar is a bad conf
        try {
            factory.reconfigure(props);
        } catch (Exception e) {
            fail("The reconfiguration is not unacceptable and seems unacceptable (2) : " + props);
        }

        ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), "under");
        assertNotNull("Check service availability -3", ref);
        cs = (CheckService) osgiHelper.getServiceObject(ref);
        assertTrue("Check state 2", cs.check());
        bc.ungetService(ref);
        under.dispose();
    }

    @Test
    public void testTwo() {
        Properties props = new Properties();
        props.put("conf", "bar");
        props.put("instance.name", "under");
        under = ipojoHelper.createComponentInstance("LFC-Test-Immediate", props);

        assertEquals("check under state", under.getState(), ComponentInstance.INVALID);

        // The conf is incorrect, the PS must not be provided
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), "under");
        assertNull("Check service availability -1", ref);

        // Reconfigure the instance with a correct configuration
        props.put("conf", "foo");
        try {
            factory.reconfigure(props);
        } catch (Exception e) {
            fail("The reconfiguration is not unacceptable and seems unacceptable : " + props);
        }

        ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), "under");
        assertNotNull("Check service availability -2", ref);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);
        assertTrue("Check state ", cs.check());
        bc.ungetService(ref);
        under.dispose();
    }


}
