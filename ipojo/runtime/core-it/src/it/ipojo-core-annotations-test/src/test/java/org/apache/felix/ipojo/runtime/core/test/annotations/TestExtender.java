package org.apache.felix.ipojo.runtime.core.test.annotations;

import org.apache.felix.ipojo.metadata.Element;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestExtender extends Common {

    String type = "org.apache.felix.ipojo.runtime.core.test.components.extender.Extender";
    String namespace = "org.apache.felix.ipojo.extender";


    @Test
    public void testMetadata() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  type);
        assertNotNull("Check meta", meta);
        Element[] ext = meta.getElements("extender", namespace);
        assertEquals("Check size", 1, ext.length);
        String extension = ext[0].getAttribute("extension");
        String onArr = ext[0].getAttribute("onArrival");
        String onDep = ext[0].getAttribute("onDeparture");

        assertEquals("Check extension", "foo", extension);
        assertEquals("Check onArrival", "onArrival", onArr);
        assertEquals("Check onDeparture", "onDeparture", onDep);
    }

}
