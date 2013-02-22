package org.apache.felix.ipojo.runtime.core;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.runtime.core.services.PrimitiveManipulationTestService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestTypeBoxing extends Common {

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

}
