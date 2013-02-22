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

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;

/**
 * Check the manipulation of primitive type (boxed and unboxed).
 * The targeted implementation contains numbers in the package and class name.
 */
public class TestPrimitiveTypesWithNumberInNames extends Common {

    PrimitiveManipulationTestService prim;

    @Before
    public void setUp() {
        ComponentInstance instance =
                ipojoHelper.createComponentInstance("ManipulationPrimitives-PrimitiveManipulationTesterA");
        assertTrue("check instance state", instance.getState() == ComponentInstance.VALID);
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(PrimitiveManipulationTestService.class.getName(),
                instance.getInstanceName());
        assertNotNull("Check prim availability", ref);
        prim = (PrimitiveManipulationTestService) osgiHelper.getServiceObject(ref);
    }

    @After
    public void tearDown() {
        prim = null;
    }

    @Test
    public void testByte() {
        assertEquals("Check - 1", prim.getByte(), 1);
        prim.setByte((byte) 2);
        assertEquals("Check - 2", prim.getByte(), 2);
    }

    @Test
    public void testShort() {
        assertEquals("Check - 1", prim.getShort(), 1);
        prim.setShort((short) 2);
        assertEquals("Check - 2", prim.getShort(), 2);
    }

    @Test
    public void testInt() {
        assertEquals("Check - 1", prim.getInt(), 1);
        prim.setInt((int) 2);
        assertEquals("Check - 2", prim.getInt(), 2);
    }

    @Test
    public void testLong() {
        assertEquals("Check - 1", prim.getLong(), 1);
        prim.setLong((long) 2);
        assertEquals("Check - 2", prim.getLong(), 2);
    }

    @Test
    public void testFloat() {
        assertEquals("Check - 1", prim.getFloat(), 1.1f);
        prim.setFloat(2.2f);
        assertEquals("Check - 2", prim.getFloat(), 2.2f);
    }

    @Test
    public void testDouble() {
        assertEquals("Check - 1", prim.getDouble(), 1.1);
        prim.setDouble(2.2);
        assertEquals("Check - 2", prim.getDouble(), 2.2);
    }

    @Test
    public void testBoolean() {
        assertFalse("Check - 1", prim.getBoolean());
        prim.setBoolean(true);
        assertTrue("Check - 2", prim.getBoolean());
    }

    @Test
    public void testChar() {
        assertEquals("Check - 1", prim.getChar(), 'a');
        prim.setChar('b');
        assertEquals("Check - 2", prim.getChar(), 'b');
    }

}
