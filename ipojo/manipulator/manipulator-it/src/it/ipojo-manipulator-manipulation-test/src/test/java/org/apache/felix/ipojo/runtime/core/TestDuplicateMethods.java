package org.apache.felix.ipojo.runtime.core;

import org.apache.felix.ipojo.runtime.core.services.Plop;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestDuplicateMethods extends Common {


    @Test
    public void testDuplicateMethod() {
        ipojoHelper.createComponentInstance("plopimpl", "plop");
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(Plop.class.getName(), "plop");
        assertNotNull("Check plop", ref);
        Plop plop = (Plop) osgiHelper.getServiceObject(ref);
        Object o = plop.getPlop();
        assertEquals("Check result", "plop", o);
        ipojoHelper.dispose();
    }
}
