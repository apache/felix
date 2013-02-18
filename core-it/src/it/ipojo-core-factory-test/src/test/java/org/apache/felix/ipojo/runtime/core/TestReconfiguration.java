package org.apache.felix.ipojo.runtime.core;

import junit.framework.Assert;
import org.apache.felix.ipojo.*;
import org.apache.felix.ipojo.architecture.Architecture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class TestReconfiguration extends Common {

    private ConfigurationAdmin admin;

    @Before
    public void setUp() {
        admin = osgiHelper.getServiceObject(ConfigurationAdmin.class);
        assertNotNull("Check configuration admin availability", admin);
        try {
            Configuration[] configurations = admin.listConfigurations(
                    "(service.factoryPid=org.apache.felix.ipojo.runtime.core.components.ReconfigurableSimpleType)");
            for (int i = 0; configurations != null && i < configurations.length; i++) {
                configurations[i].delete();
            }
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (InvalidSyntaxException e) {
            fail(e.getMessage());
        }
    }

    @After
    public void tearDown() {
        try {
            Configuration[] configurations = admin.listConfigurations(
                    "(service.factoryPid=org.apache.felix.ipojo.runtime.core.components.ReconfigurableSimpleType)");
            for (int i = 0; configurations != null && i < configurations.length; i++) {
                configurations[i].delete();
            }
        } catch (IOException e) {
            fail(e.getMessage());
        } catch (InvalidSyntaxException e) {
            fail(e.getMessage());
        }
        admin = null;
    }

    @Test
    public void testRevalidationOnReconfiguration() {
        ComponentFactory factory = (ComponentFactory) ipojoHelper.getFactory(
                "org.apache.felix.ipojo.runtime.core.components.ReconfigurableSimpleType");

        // First inject a configuration triggering an exception of the validate method.
        Properties props = new Properties();
        props.put("prop", "KO");
        ComponentInstance ci = null;
        try {
            ci = factory.createComponentInstance(props);
        } catch (UnacceptableConfiguration e) {
            fail(e.getMessage());
        } catch (MissingHandlerException e) {
            fail(e.getMessage());
        } catch (ConfigurationException e) {
            fail(e.getMessage());
        }

        assertNotNull(ci);
        assertEquals("instance invalid", ComponentInstance.STOPPED, ci.getState());

        // Reconfigure
        props = new Properties();
        props.put("prop", "OK");

        ci.reconfigure(props);

        assertNotNull(ci);
        assertEquals("instance valid", ComponentInstance.VALID, ci.getState());
    }

    public static long UPDATE_WAIT_TIME = 2000;

    @Test public void testRevalidationOnReconfigurationUsingConfigAdmin() throws InvalidSyntaxException {
        Configuration configuration = null;
        try {
            configuration = admin.createFactoryConfiguration("org.apache.felix.ipojo.runtime.core.components.ReconfigurableSimpleType", null);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        Dictionary props = configuration.getProperties();
        if (props == null) {
            props = new Properties();
        }
        // First inject a configuration triggering an exception of the validate method.
        props.put("prop", "KO");

        try {
            configuration.update(props);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        String pid = configuration.getPid();

        // Wait for the processing of the first configuration.
        try {
            Thread.sleep(UPDATE_WAIT_TIME);
        } catch (InterruptedException e1) {
            fail(e1.getMessage());
        }

        Assert.assertNull("No architecture", osgiHelper.getServiceReference(Architecture.class.getName(),
                "(architecture.instance=" + pid + ")"));


        // Reconfigure
        props = new Properties();
        props.put("prop", "OK");

        try {
            configuration.update(props);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        pid = configuration.getPid();

        // Wait for the processing of the first configuration.
        try {
            Thread.sleep(UPDATE_WAIT_TIME);
        } catch (InterruptedException e1) {
            fail(e1.getMessage());
        }

        Assert.assertNotNull("architecture", osgiHelper.getServiceReference(Architecture.class.getName(), "(architecture.instance=" + pid + ")"));
        Architecture arch = (Architecture) osgiHelper.getServiceObject( Architecture.class.getName(), "(architecture.instance=" + pid + ")");

        assertEquals("Is valid ?", ComponentInstance.VALID, arch.getInstanceDescription().getState());
    }

    @Test public void testRevalidationOnReconfigurationWithController() {
        ComponentFactory factory = (ComponentFactory) ipojoHelper.getFactory(
                "org.apache.felix.ipojo.runtime.core.components.ReconfigurableSimpleType2");

        // First inject a configuration triggering an exception of the validate method.
        Properties props = new Properties();
        props.put("prop", "KO");
        ComponentInstance ci = null;
        try {
            ci = factory.createComponentInstance(props);
        } catch (UnacceptableConfiguration e) {
            fail(e.getMessage());
        } catch (MissingHandlerException e) {
            fail(e.getMessage());
        } catch (ConfigurationException e) {
            fail(e.getMessage());
        }

        assertNotNull(ci);
        assertEquals("instance invalid", ComponentInstance.INVALID, ci.getState()); // Controller effect.

        // Reconfigure
        props = new Properties();
        props.put("prop", "OK");

        ci.reconfigure(props);

        assertNotNull(ci);
        assertEquals("instance valid", ComponentInstance.VALID, ci.getState());
    }

    @Test public void testRevalidationOnReconfigurationUsingConfigAdminAndController() throws InvalidSyntaxException {
        Configuration configuration = null;
        try {
            configuration = admin.createFactoryConfiguration("org.apache.felix.ipojo.runtime.core.components.ReconfigurableSimpleType2",
                    null);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        Dictionary props = configuration.getProperties();
        if (props == null) {
            props = new Properties();
        }
        // First inject a configuration triggering an exception of the validate method.
        props.put("prop", "KO");

        try {
            configuration.update(props);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        String pid = configuration.getPid();

        // Wait for the processing of the first configuration.
        try {
            Thread.sleep(UPDATE_WAIT_TIME);
        } catch (InterruptedException e1) {
            fail(e1.getMessage());
        }

        // Invalid ... controller effect
        Assert.assertNotNull("architecture", osgiHelper.getServiceReference(Architecture.class.getName(), "(architecture.instance=" + pid + ")"));
        Architecture arch = (Architecture) osgiHelper.getServiceObject( Architecture.class.getName(), "(architecture.instance=" + pid + ")");

        assertEquals("Is invalid ?", ComponentInstance.INVALID, arch.getInstanceDescription().getState());

        // Reconfigure
        props = new Properties();
        props.put("prop", "OK");

        try {
            configuration.update(props);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        pid = configuration.getPid();

        // Wait for the processing of the first configuration.
        try {
            Thread.sleep(UPDATE_WAIT_TIME);
        } catch (InterruptedException e1) {
            fail(e1.getMessage());
        }

        Assert.assertNotNull("architecture", osgiHelper.getServiceReference(Architecture.class.getName(), "(architecture.instance=" + pid + ")"));
        arch = (Architecture) osgiHelper.getServiceObject( Architecture.class.getName(), "(architecture.instance=" + pid + ")");

        assertEquals("Is valid ?", ComponentInstance.VALID, arch.getInstanceDescription().getState());
    }

    @Test public void testRevalidationOnReconfigurationOfTheController() {
        ComponentFactory factory = (ComponentFactory) ipojoHelper.getFactory(
                "org.apache.felix.ipojo.runtime.core.components.ReconfigurableSimpleType3");

        // First inject a configuration triggering an exception of the validate method.
        Properties props = new Properties();
        props.put("controller", "false");
        ComponentInstance ci = null;
        try {
            ci = factory.createComponentInstance(props);
        } catch (UnacceptableConfiguration e) {
            fail(e.getMessage());
        } catch (MissingHandlerException e) {
            fail(e.getMessage());
        } catch (ConfigurationException e) {
            fail(e.getMessage());
        }

        assertNotNull(ci);
        assertEquals("instance invalid", ComponentInstance.INVALID, ci.getState()); // Controller effect.

        // Reconfigure
        props = new Properties();
        props.put("controller", "true");

        ci.reconfigure(props);

        assertNotNull(ci);
        assertEquals("instance valid", ComponentInstance.VALID, ci.getState());
    }

    @Test public void testRevalidationOnReconfigurationUsingConfigAdminOfTheController() throws InvalidSyntaxException {
        Configuration configuration = null;
        try {
            configuration = admin.createFactoryConfiguration("org.apache.felix.ipojo.runtime.core.components.ReconfigurableSimpleType3",
                    null);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        Dictionary props = configuration.getProperties();
        if (props == null) {
            props = new Properties();
        }
        // First inject a configuration triggering an exception of the validate method.
        props.put("controller", "false");

        try {
            configuration.update(props);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        String pid = configuration.getPid();

        // Wait for the processing of the first configuration.
        try {
            Thread.sleep(UPDATE_WAIT_TIME);
        } catch (InterruptedException e1) {
            fail(e1.getMessage());
        }

        // Invalid ... controller effect
        Assert.assertNotNull("architecture", osgiHelper.getServiceReference(Architecture.class.getName(), "(architecture.instance=" + pid + ")"));
        Architecture arch = (Architecture) osgiHelper.getServiceObject( Architecture.class.getName(), "(architecture.instance=" + pid + ")");

        assertEquals("Is invalid ?", ComponentInstance.INVALID, arch.getInstanceDescription().getState());

        // Reconfigure
        props = new Properties();
        props.put("controller", "true");

        try {
            configuration.update(props);
        } catch (IOException e) {
            fail(e.getMessage());
        }

        pid = configuration.getPid();

        // Wait for the processing of the first configuration.
        try {
            Thread.sleep(UPDATE_WAIT_TIME);
        } catch (InterruptedException e1) {
            fail(e1.getMessage());
        }

        Assert.assertNotNull("architecture", osgiHelper.getServiceReference(Architecture.class.getName(), "(architecture.instance=" + pid + ")"));
        arch = (Architecture) osgiHelper.getServiceObject( Architecture.class.getName(), "(architecture.instance=" + pid + ")");

        assertEquals("Is valid ?", ComponentInstance.VALID, arch.getInstanceDescription().getState());
    }
}
