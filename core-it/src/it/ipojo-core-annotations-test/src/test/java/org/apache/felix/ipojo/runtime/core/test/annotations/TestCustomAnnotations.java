package org.apache.felix.ipojo.runtime.core.test.annotations;

import org.apache.felix.ipojo.metadata.Element;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Checks the support of the custom annotation handlinig.
 */
public class TestCustomAnnotations extends Common {


    @Test
    public void testThatCustomAnnotationAreCorrectlyAdded() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.CustomAnnotationWithEnum");
        Element[] ann = meta.getElements("IPOJOFoo", "foo.ipojo");
        assertNotNull("Annotation exists ", ann);
    }

    @Test
    public void testThatCustomAnnotationAreSupportingEnums() {
        Element meta = ipojoHelper.getMetadata(testedBundle,  "org.apache.felix.ipojo.runtime.core.test.components.CustomAnnotationWithEnum");
        Element[] ann = meta.getElements("IPOJOFoo", "foo.ipojo");
        assertNotNull("Annotation exists ", ann);
        Element element = ann[0];
        // Simple value
        assertEquals("RED", element.getAttribute("rgb"));
        // Array (FELIX-3508).
        assertEquals("{BLUE,RED}", element.getAttribute("colors"));
    }


}

