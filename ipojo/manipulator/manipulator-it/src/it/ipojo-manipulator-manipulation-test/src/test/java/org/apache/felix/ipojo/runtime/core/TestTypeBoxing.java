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
import org.apache.felix.ipojo.runtime.core.services.PrimitiveManipulationTestService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;
import org.ow2.chameleon.testing.helpers.BaseTest;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestTypeBoxing extends BaseTest {

    ComponentInstance instance; // Instance under test

    PrimitiveManipulationTestService prim;

    ServiceReference prim_ref;

    @Before
    public void setUp() {
        Properties p1 = new Properties();
        p1.put("instance.name", "primitives");
        instance = ipojoHelper.createComponentInstance("ManipulationPrimitives5-PrimitiveManipulationTester", p1);
        assertTrue("check instance state", instance.getState() == ComponentInstance.VALID);
        prim_ref = ipojoHelper.getServiceReferenceByName(PrimitiveManipulationTestService.class.getName(), instance.getInstanceName());
        assertNotNull("Check prim availability", prim_ref);
        prim = (PrimitiveManipulationTestService) osgiHelper.getServiceObject(prim_ref);
    }

    @After
    public void tearDown() {
        prim = null;
    }


    @Test
    public void testLongFromObject() {
        assertEquals("Check - 1", prim.getLong(), 1);
        Long l = new Long(2);
        prim.setLong(l);
        assertEquals("Check - 2", prim.getLong(), 2);
    }

    @Test
    public void testLongFromObject2() {
        assertEquals("Check - 1", prim.getLong(), 1);
        Long l = new Long(2);
        prim.setLong(l, "ss");
        assertEquals("Check - 2", prim.getLong(), 2);
    }

    @Override
    protected List<String> getExtraExports() {
        return Arrays.asList("org.apache.felix.ipojo.runtime.core.components");
    }

}
