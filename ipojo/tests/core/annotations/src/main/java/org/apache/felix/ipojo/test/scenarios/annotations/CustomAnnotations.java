package org.apache.felix.ipojo.test.scenarios.annotations;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Checks the support of the custom annotation handlinig.
 */
public class CustomAnnotations extends OSGiTestCase {
    
    private IPOJOHelper helper;
    
    public void setUp() {
        helper = new IPOJOHelper(this);
    }
    
    public void testThatCustomAnnotationAreCorrectlyAdded() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.CustomAnnotationWithEnum");
        Element[] ann = meta.getElements("IPOJOFoo", "foo.ipojo");
        assertNotNull("Annotation exists ", ann);
    }

    public void testThatCustomAnnotationAreSupportingEnums() {
        Element meta = helper.getMetadata("org.apache.felix.ipojo.test.scenarios.component.CustomAnnotationWithEnum");
        Element[] ann = meta.getElements("IPOJOFoo", "foo.ipojo");
        assertNotNull("Annotation exists ", ann);
        Element element = ann[0];
        // Simple value
        assertEquals("RED", element.getAttribute("rgb"));
        // Array (FELIX-3508).
        assertEquals("{BLUE,RED}", element.getAttribute("colors"));
    }

    
    

}

