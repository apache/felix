package org.apache.felix.ipojo.runtime.core.test.annotations;

import org.apache.felix.ipojo.metadata.Element;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestLifecycleController extends Common {

    @Test
    public void testLFC() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.Lifecycle");
        Element[] ctrls = meta.getElements("controller");
        assertNotNull("Controller exists ", ctrls);
        Element ctrl = ctrls[0];
        assertNotNull("Field", ctrl.getAttribute("field"));
        assertEquals("Field", "lfc", ctrl.getAttribute("field"));
    }


}

