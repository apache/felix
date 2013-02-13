package org.apache.felix.ipojo.runtime.core;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.handlers.configuration.ConfigurationHandlerDescription;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Dictionary;
import java.util.Hashtable;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

public class TestArchitecture extends Common {

    /**
     * Instance where the ManagedServicePID is provided by the component type.
     */
    ComponentInstance instance1;
    /**
     * Instance where the ManagedServicePID is provided by the instance.
     */
    ComponentInstance instance2;

    /**
     * Instance without configuration.
     */
    ComponentInstance instance3;

    @Before
    public void setUp() {
        String type = "CONFIG-FooProviderType-4";
        Dictionary<String, String> p = new Hashtable<String, String>();
        p.put("instance.name", "instance");
        p.put("foo", "foo");
        p.put("bar", "2");
        p.put("baz", "baz");
        instance1 = ipojoHelper.createComponentInstance(type, p);
        assertEquals("instance1 created", ComponentInstance.VALID, instance1.getState());

        type = "CONFIG-FooProviderType-3";
        Dictionary<String, String> p1 = new Hashtable<String, String>();
        p1.put("instance.name", "instance-2");
        p1.put("foo", "foo");
        p1.put("bar", "2");
        p1.put("baz", "baz");
        p1.put("managed.service.pid", "instance");
        instance2 = ipojoHelper.createComponentInstance(type, p1);

    }

    @After
    public void tearDown() {
        instance1.dispose();
        instance2.dispose();

        instance1 = null;
        instance2 = null;
    }

    @Test
    public void testArchitectureForInstance1() {

        Architecture arch = osgiHelper.getServiceObject(Architecture.class,
                "(architecture.instance=instance)");
        assertNotNull(arch);

        // Test on String representation.
        String desc = arch.getInstanceDescription().getDescription().toString();
        assertTrue(desc.contains("managed.service.pid=\"FooProvider-3\""));

        // Test on handler description
        ConfigurationHandlerDescription hd = (ConfigurationHandlerDescription) arch.getInstanceDescription().getHandlerDescription("org.apache.felix.ipojo:properties");
        assertNotNull(hd);

        assertEquals(2, hd.getProperties().length);
        assertEquals("FooProvider-3", hd.getManagedServicePid());

    }

    @Test
    public void testArchitectureForInstance2() {
        Architecture arch = osgiHelper.getServiceObject(Architecture.class, "(architecture.instance=instance-2)");
        assertNotNull(arch);

        // Test on String representation.
        String desc = arch.getInstanceDescription().getDescription().toString();
        assertTrue(desc.contains("managed.service.pid=\"instance\""));

        // Test on handler description
        ConfigurationHandlerDescription hd = (ConfigurationHandlerDescription) arch.getInstanceDescription().getHandlerDescription("org.apache.felix.ipojo:properties");
        assertNotNull(hd);

        assertEquals(2, hd.getProperties().length);
        assertEquals("instance", hd.getManagedServicePid());

    }


}
