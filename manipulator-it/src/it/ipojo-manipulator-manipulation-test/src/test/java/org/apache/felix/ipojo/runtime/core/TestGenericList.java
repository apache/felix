package org.apache.felix.ipojo.runtime.core;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.apache.felix.ipojo.runtime.core.services.FooService;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import java.util.List;
import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.*;

public class TestGenericList extends Common {

    ComponentInstance foo1, foo2;
    ComponentInstance checker;

    @Before
    public void setUp() {
        foo1 = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.components.FooServiceImpl", "foo1");
        foo2 = ipojoHelper.createComponentInstance("org.apache.felix.ipojo.runtime.core.components.FooServiceImpl", "foo2");
        checker = ipojoHelper.createComponentInstance("TypedList", "checker");
        foo1.stop();
        foo2.stop();
    }

    @Test
    public void testTypedList() {
        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), checker.getInstanceName());
        CheckService check = (CheckService) osgiHelper.getServiceObject(ref);
        assertNotNull("Checker availability", check);
        // Check without providers
        assertFalse("Empty list", check.check());

        // Start the first provider
        foo1.start();
        assertTrue("List with one element", check.check());
        Properties props = check.getProps();
        List<FooService> list = (List<FooService>) props.get("list");
        assertEquals("Check size - 1", 1, list.size());

        // Start the second provider 
        foo2.start();
        assertTrue("List with two element", check.check());
        props = check.getProps();
        list = (List<FooService>) props.get("list");
        assertEquals("Check size - 2", 2, list.size());

        // Stop the first one
        foo1.stop();
        assertTrue("List with one element (2)", check.check());
        props = check.getProps();
        list = (List<FooService>) props.get("list");
        assertEquals("Check size - 3", 1, list.size());
    }

}
