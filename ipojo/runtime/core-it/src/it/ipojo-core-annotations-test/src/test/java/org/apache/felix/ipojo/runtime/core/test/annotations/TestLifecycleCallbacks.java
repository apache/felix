package org.apache.felix.ipojo.runtime.core.test.annotations;

import org.apache.felix.ipojo.metadata.Element;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class TestLifecycleCallbacks extends Common {


    @Test
    public void testCallbacks() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.Lifecycle");
        Element[] cbs = meta.getElements("callback");
        assertNotNull("Callbacks exists ", cbs);
        assertEquals("Callbacks count ", 2, cbs.length);

        Element elem = getCallbackByMethodName(cbs, "start");
        assertEquals("Check start method", "start", elem.getAttribute("method"));
        assertEquals("Check start transition", "validate", elem.getAttribute("transition"));

        elem = getCallbackByMethodName(cbs, "stop");
        assertEquals("Check stop method", "stop", elem.getAttribute("method"));
        assertEquals("Check stop transition", "invalidate", elem.getAttribute("transition"));
    }

    @Test
    public void testImmediate() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.Immediate");
        assertNotNull("Immediate attribute", meta.getAttribute("immediate"));
        assertEquals("Immediate attribute value", "true", meta.getAttribute("immediate"));
    }

    @Test
    public void testNoImmediate() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.NoImmediate");
        assertNotNull("Immediate attribute", meta.getAttribute("immediate"));
        assertEquals("Immediate attribute value", "false", meta.getAttribute("immediate"));
    }

    private Element getCallbackByMethodName(Element[] cbs, String method) {
        for (int i = 0; i < cbs.length; i++) {
            String met = cbs[i].getAttribute("method");
            if (met != null && met.equalsIgnoreCase(method)) {
                return cbs[i];
            }
        }
        fail("Cannot found the callback with the method " + method);
        return null;
    }


}
