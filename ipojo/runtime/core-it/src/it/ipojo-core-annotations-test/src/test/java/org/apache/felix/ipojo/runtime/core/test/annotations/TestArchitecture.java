package org.apache.felix.ipojo.runtime.core.test.annotations;

import org.apache.felix.ipojo.metadata.Element;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestArchitecture extends Common {

    @Test
    public void testArch() {
        Element meta = ipojoHelper.getMetadata(testedBundle, "org.apache.felix.ipojo.runtime.core.test.components" +
                ".Arch");
        String arch = meta.getAttribute("architecture");
        assertNotNull("Architecture exists ", arch);
        assertEquals("Architecture value", "true", arch);
    }

    @Test
    public void testNoArch() {
        Element meta = ipojoHelper.getMetadata(testedBundle, "org.apache.felix.ipojo.runtime.core.test.components" +
                ".NoArch");
        String arch = meta.getAttribute("architecture");
        assertNotNull("Architecture exists ", arch);
        assertEquals("Architecture value", "false", arch);
    }


}

