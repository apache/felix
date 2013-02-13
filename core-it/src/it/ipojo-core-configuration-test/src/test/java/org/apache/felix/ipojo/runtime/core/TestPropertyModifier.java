package org.apache.felix.ipojo.runtime.core;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.apache.felix.ipojo.runtime.core.services.FooService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.ow2.chameleon.testing.helpers.IPOJOHelper;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

import javax.inject.Inject;
import java.util.Properties;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.options;


public class TestPropertyModifier extends Common {

    @Test
    public void testPropertyModifier() {

        ComponentInstance ci = null;
        Factory factory = ipojoHelper.getFactory("org.apache.felix.ipojo.runtime.core.components.PropertyModifier");
        Properties props = new Properties();
        props.put("cls", new String[]{FooService.class.getName()});
        try {
            ci = factory.createComponentInstance(props);
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        ServiceReference ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), ci.getInstanceName());
        assertNotNull("Check ref", ref);

        // Check the service property
        // Not exposed here:
        assertNull("Classes -0", ref.getProperty("classes"));

        CheckService check = (CheckService) osgiHelper.getServiceObject(ref);
        assertTrue(check.check());

        // Property exposed now.
        ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), ci.getInstanceName());
        Class[] str = (Class[]) ref.getProperty("classes");
        assertEquals("Classes size", 1, str.length);
        assertEquals("Classes[0]", FooService.class.getName(), str[0].getName());

        Properties p = check.getProps();
        Class[] str2 = (Class[]) p.get("classes");
        assertEquals("Classes size -2", 1, str2.length);
        assertEquals("Classes[0] -2", FooService.class.getName(), str2[0].getName());

        Properties props2 = new Properties();
        props2.put("cls", new String[]{FooService.class.getName(), CheckService.class.getName()});
        try {
            ci.reconfigure(props2);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        // Check the service property
        ref = ipojoHelper.getServiceReferenceByName(CheckService.class.getName(), ci.getInstanceName());
        assertNotNull("Check ref", ref);
        str = (Class[]) ref.getProperty("classes");
        assertEquals("Classes size -3", 2, str.length);
        assertEquals("Classes[0] -3", FooService.class.getName(), str[0].getName());
        assertEquals("Classes[1] -3", CheckService.class.getName(), str[1].getName());


        check = (CheckService) osgiHelper.getServiceObject(ref);
        p = check.getProps();
        str2 = (Class[]) p.get("classes");
        assertEquals("Classes size -4", 2, str2.length);
        assertEquals("Classes[0] -4", FooService.class.getName(), str2[0].getName());
        assertEquals("Classes[1] -4", CheckService.class.getName(), str2[1].getName());

        ci.dispose();
    }

}
