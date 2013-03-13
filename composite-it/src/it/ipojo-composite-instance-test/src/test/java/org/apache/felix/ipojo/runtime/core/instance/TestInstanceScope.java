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

package org.apache.felix.ipojo.runtime.core.instance;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.composite.CompositeFactory;
import org.apache.felix.ipojo.runtime.core.Common;
import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.apache.felix.ipojo.runtime.core.services.Service;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import java.util.Properties;

import static org.junit.Assert.*;

public class TestInstanceScope extends Common {

    CompositeFactory factory;
    ComponentInstance instance;

    @Before
    public void setUp() {
        factory = (CompositeFactory) ipojoHelper.getFactory("SCOPE-scope");
        assertNotNull("Factory", factory);
        try {
            instance = factory.createComponentInstance(null);
        } catch (Exception e) {
            fail("Fail instantiation : " + e.getMessage());
        }


    }

    @After
    public void tearDown() {
        instance.dispose();
        instance = null;
    }

    @Test
    public void testScope() {
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(Architecture.class.getName(), instance.getInstanceName());
        assertNotNull("Check architecture availability", ref);
        Architecture arch = (Architecture) getContext().getService(ref);
        assertTrue("Validity", arch.getInstanceDescription().getState() == ComponentInstance.VALID);

        // Get internal service
        ServiceContext sc = getServiceContext(instance);
        ServiceReference ref2 = ipojoHelper.getServiceReference(sc, CheckService.class.getName(), null);
        assertNotNull("Check CheckService availability", ref2);
        CheckService svc = (CheckService) sc.getService(ref2);
        Properties props = svc.getProps();
        assertEquals("Check props - 1", 1, ((Integer) props.get("1")).intValue());
        assertEquals("Check props - 2", 2, ((Integer) props.get("2")).intValue());
        assertEquals("Check props - 3", 3, ((Integer) props.get("3")).intValue());

    }

    @Test
    public void testGlobalUnavailability() {
        ServiceReference ref2 = osgiHelper.getServiceReference(Service.class.getName(), null);
        assertNull("Check Service unavailability", ref2);
    }

    @Test
    public void testScopeUnvailability() {
        CompositeFactory factory2 = (CompositeFactory) ipojoHelper.getFactory("SCOPE-badscope");
        assertNotNull("Factory", factory2);
        ComponentInstance instance2 = null;
        try {
            instance2 = factory2.createComponentInstance(null);
        } catch (Exception e) {
            fail("Fail instantiation : " + e.getMessage());
        }
        //System.out.println(instance2.getInstanceDescription().getDescription());

        assertEquals("Check invalidity", ComponentInstance.INVALID, instance2.getState());
        instance2.dispose();

    }


}
