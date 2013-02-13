package org.apache.felix.ipojo.runtime.core.test.annotations;

import org.apache.felix.ipojo.metadata.Element;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestAggregateDependency extends Common {

    private Element[] deps;

    @Before
    public void setUp() {
        Element meta = ipojoHelper.getMetadata(testedBundle, "org.apache.felix.ipojo.runtime.core.test.components.AggregateDependency");
        deps = meta.getElements("requires");
    }

    @Test
    public void testCallbackBind() {
        Element dep = getDependencyById(deps, "Bar");
        String opt = dep.getAttribute("aggregate");
        assertEquals("Check aggregate", "true", opt);
    }

    @Test
    public void testCallbackUnbind() {
        Element dep = getDependencyById(deps, "Baz");
        String opt = dep.getAttribute("aggregate");
        assertEquals("Check aggregate", "true", opt);
    }

    @Test
    public void testBindOnly() {
        Element dep = getDependencyById(deps, "bindonly");
        String opt = dep.getAttribute("aggregate");
        assertEquals("Check aggregate", "true", opt);
    }

    @Test
    public void testUnbindOnly() {
        Element dep = getDependencyById(deps, "unbindonly");
        String opt = dep.getAttribute("aggregate");
        assertEquals("Check aggregate", "true", opt);
    }

    private Element getDependencyById(Element[] deps, String name) {
        for (int i = 0; i < deps.length; i++) {
            String na = deps[i].getAttribute("id");
            String field = deps[i].getAttribute("field");
            if (na != null && na.equalsIgnoreCase(name)) {
                return deps[i];
            }
            if (field != null && field.equalsIgnoreCase(name)) {
                return deps[i];
            }
        }
        fail("Dependency  " + name + " not found");
        return null;
    }

}
