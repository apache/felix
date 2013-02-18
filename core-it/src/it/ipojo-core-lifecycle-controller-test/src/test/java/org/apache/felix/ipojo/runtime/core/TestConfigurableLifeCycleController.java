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

import org.apache.felix.ipojo.*;
import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import java.util.Properties;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class TestConfigurableLifeCycleController extends Common {

    private ComponentInstance under;

    private Factory factory;


    @Before
    public void setUp() {
        factory = ipojoHelper.getFactory("LFC-Test-Configurable");
    }

    @Test
    public void testValidThenInvalid() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        Properties props = new Properties();
        props.put("instance.name", "under1");
        props.put("state", "true");
        under = factory.createComponentInstance(props);

        // The conf is correct, the PS must be provided
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), "under1");
        assertNotNull("Check service availability -1", ref);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);
        assertTrue("Check state 1", cs.check());
        bc.ungetService(ref);

        // Reconfigure the instance
        props.put("state", "false"); // Bar is a bad conf
        try {
            factory.reconfigure(props);
        } catch (Exception e) {
            fail("The reconfiguration is not unacceptable and seems unacceptable : " + props);
        }

        // The instance should now be invalid
        ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), "under1");
        assertNull("Check service availability -2", ref);

        // Reconfigure the instance with a valid configuration
        props.put("state", "true");
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

    @Test
    public void testInValidThenValid() throws Exception {
        Properties props = new Properties();
        props.put("instance.name", "under1");
        props.put("state", "false");
        under = factory.createComponentInstance(props);

        // The instance should now be invalid
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), "under1");
        assertNull("Check service availability -2", ref);

        // Reconfigure the instance
        props.put("state", "true"); // Bar is a bad conf
        try {
            factory.reconfigure(props);
        } catch (Exception e) {
            fail("The reconfiguration is not unacceptable and seems unacceptable : " + props);
        }

//        Object[] objects = Utils.getServiceObjects(context, Architecture.class.getName(), null);
//        for (int i = 0; i < objects.length; i++) {
//        	Architecture a = (Architecture) objects[i];
//        	System.out.println(a.getInstanceDescription().getDescription());
//        }

        ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), "under1");
        assertNotNull("Check service availability -1", ref);
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);
        assertTrue("Check state 1", cs.check());
        bc.ungetService(ref);


        // Reconfigure the instance
        props.put("state", "false"); // Bar is a bad conf
        try {
            factory.reconfigure(props);
        } catch (Exception e) {
            fail("The reconfiguration is not unacceptable and seems unacceptable : " + props);
        }

        // The instance should now be invalid
        ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), "under1");
        assertNull("Check service availability -2", ref);

        // Reconfigure the instance with a valid configuration
        props.put("state", "true");
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

}
