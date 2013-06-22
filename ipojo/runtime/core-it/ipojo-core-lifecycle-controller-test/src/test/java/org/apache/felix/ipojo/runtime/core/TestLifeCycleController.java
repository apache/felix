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
import org.junit.Ignore;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import java.util.Properties;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class TestLifeCycleController extends Common {

    private ComponentInstance under;

    private Factory factory;

    @Before
    public void setUp() {
        factory = ipojoHelper.getFactory("LFC-Test");
    }

    @Test
    public void testOne() {
        Properties props = new Properties();
        props.put("conf", "foo");
        props.put("instance.name", "under1");
        under = ipojoHelper.createComponentInstance("LFC-Test", props);

        // The conf is correct, the PS must be provided
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), "under1");
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
        ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), "under1");
        assertNull("Check service availability -2", ref);

        // Reconfigure the instance with a valid configuration
        props.put("conf", "foo"); // Bar is a bad conf
        try {
            factory.reconfigure(props);
        } catch (Exception e) {
            fail("The reconfiguration is not unacceptable and seems unacceptable (2) : " + props);
        }

        ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), "under1");
        assertNotNull("Check service availability -3", ref);
        cs = (CheckService) osgiHelper.getServiceObject(ref);
        assertTrue("Check state 2", cs.check());
        bc.ungetService(ref);

        under.dispose();
    }

    /**
     * This test must be removed as it is not compliant with osgiHelper. It unregisters a service during the creation of the
     * service instance, so the returned object is null.
     */
    @Test
    @Ignore("This test must be removed as it is not compliant with osgiHelper. It unregisters a service during the creation" +
            " of the service instance, so the returned object is null.")
    public void testTwo() {
        Properties props = new Properties();
        props.put("conf", "bar");
        props.put("instance.name", "under2");
        under = ipojoHelper.createComponentInstance("LFC-Test", props);

        // The conf is incorrect, but the test can appears only when the object is created : the PS must be provided
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), "under2");
        assertNotNull("Check service availability -1", ref);

        System.out.println("CS received : " + osgiHelper.getServiceObject(ref));
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);
        assertNotNull("Assert CS not null", cs);
        try {
            assertFalse("Check state (false)", cs.check());
        } catch (Throwable e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        // As soon as the instance is created, the service has to disappear :
        ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), "under2");
        assertNull("Check service availability -2", ref);

        // Reconfigure the instance with a correct configuration
        props.put("conf", "foo");
        try {
            factory.reconfigure(props);
        } catch (Exception e) {
            fail("The reconfiguration is not unacceptable and seems unacceptable : " + props);
        }

        ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), "under2");
        assertNotNull("Check service availability -3", ref);
        cs = (CheckService) osgiHelper.getServiceObject(ref);
        assertTrue("Check state ", cs.check());
        bc.ungetService(ref);

        under.dispose();
    }

}
