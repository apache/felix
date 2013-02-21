package org.apache.felix.ipojo.runtime.core.test.dependencies.inner;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.runtime.core.test.components.inner.C1;
import org.apache.felix.ipojo.runtime.core.test.components.inner.C2;
import org.apache.felix.ipojo.runtime.core.test.components.inner.C3;
import org.apache.felix.ipojo.runtime.core.test.dependencies.Common;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestInnerProxyMix extends Common {

    public static String C1 = "org.apache.felix.ipojo.runtime.core.test.components.inner.C1";
    public static String C2 = "org.apache.felix.ipojo.runtime.core.test.components.inner.C2";
    public static String C3 = "org.apache.felix.ipojo.runtime.core.test.components.inner.C3";

    private ComponentInstance instancec1;
    private ComponentInstance instancec2;
    private ComponentInstance instancec3;

    @Before
    public void setUp() {
        // Create the instances
        instancec1 = ipojoHelper.createComponentInstance(C1);
        instancec2 = ipojoHelper.createComponentInstance(C2);
        instancec3 = ipojoHelper.createComponentInstance(C3);
    }

    @Test
    public void testMix() {
        // Check that everything is OK
        assertEquals(ComponentInstance.VALID, instancec1.getState());
        assertEquals(ComponentInstance.VALID, instancec2.getState());
        assertEquals(ComponentInstance.VALID, instancec3.getState());

        // Call C3
        C3 svc = (C3) osgiHelper.getServiceObject(C3, null);
        assertNotNull(svc);
        assertEquals("called", svc.getFilter().authenticate());

        // So far, all right

        //We stop c1 and c2.
        instancec1.stop();
        instancec2.stop();

        assertEquals(ComponentInstance.INVALID, instancec3.getState()); // C2 dependency invalid

        instancec1.start();
        instancec2.start();

        // Check that everything is OK
        assertEquals(ComponentInstance.VALID, instancec1.getState());
        assertEquals(ComponentInstance.VALID, instancec2.getState());
        assertEquals(ComponentInstance.VALID, instancec3.getState());

        // Call C3
        svc = (C3) osgiHelper.getServiceObject(C3, null);
        assertNotNull(svc);
        assertEquals("called", svc.getFilter().authenticate());
    }

}
