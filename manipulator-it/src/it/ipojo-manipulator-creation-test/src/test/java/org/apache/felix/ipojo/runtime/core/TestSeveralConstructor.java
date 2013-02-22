package org.apache.felix.ipojo.runtime.core;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestSeveralConstructor extends Common {

    private ComponentInstance ci, ci2, ci3;

    @Before
    public void setUp() {
        ci = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.components.SeveralConstructors");
        ci2 = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.components.NoEmptyConstructor");
        ci3 = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.components.NoEmptyConstructorWithParentClass");

    }

    @Test
    public void testSeveralConstructor() {
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), ci.getInstanceName());
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);
        assertTrue("Check assignation", cs.check());
        String name = (String) cs.getProps().get("name");
        assertEquals("Check message", "hello world", name);
        //assertNull("Check message", name);
    }

    @Test
    public void testNoEmptyConstructor() {
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), ci2.getInstanceName());
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);
        assertFalse("Check assignation", cs.check());
        String name = (String) cs.getProps().get("name");
        assertEquals("Check message", "NULL", name);
    }

    @Test
    public void testNoEmptyConstructorWithAParentClass() {
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), ci3.getInstanceName());
        CheckService cs = (CheckService) osgiHelper.getServiceObject(ref);
        assertTrue("Check assignation", cs.check()); // super set name to "hello"
        String name = (String) cs.getProps().get("name");
        assertEquals("Check message", "hello", name);
    }

}
