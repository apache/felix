package org.apache.felix.ipojo.test.scenarios.factories;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class ReconfigurationTest extends OSGiTestCase {

	 private ConfigurationAdmin admin;

	 public void setUp() {
	        admin = (ConfigurationAdmin) Utils.getServiceObject(getContext(), ConfigurationAdmin.class.getName(), null);
	        assertNotNull("Check configuration admin availability", admin);
	        try {
	            Configuration[] configurations = admin.listConfigurations(
	            		"(service.factoryPid=org.apache.felix.ipojo.test.scenarios.component.ReconfigurableSimpleType)");
	            for (int i = 0; configurations != null && i < configurations.length; i++) {
	                configurations[i].delete();
	            }
	        } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        } catch (InvalidSyntaxException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	    }

	    public void tearDown() {
	        try {
	            Configuration[] configurations = admin.listConfigurations(
	            		"(service.factoryPid=org.apache.felix.ipojo.test.scenarios.component.ReconfigurableSimpleType)");
	            for (int i = 0; configurations != null && i < configurations.length; i++) {
	                configurations[i].delete();
	            }
	        } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        } catch (InvalidSyntaxException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	        admin = null;


	    }

	public void testRevalidationOnReconfiguration() {
		ComponentFactory factory = (ComponentFactory) Utils.getFactoryByName(getContext(),
				"org.apache.felix.ipojo.test.scenarios.component.ReconfigurableSimpleType");

		// First inject a configuration triggering an exception of the validate method.
		Properties props = new Properties();
		props.put("prop", "KO");
		ComponentInstance ci = null;
		try {
			ci = factory.createComponentInstance(props);
		} catch (UnacceptableConfiguration e) {
			e.printStackTrace();
		} catch (MissingHandlerException e) {
			e.printStackTrace();
		} catch (ConfigurationException e) {
			e.printStackTrace();
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

	public void testRevalidationOnReconfigurationUsingConfigAdmin() throws InvalidSyntaxException {
		Configuration configuration = null;
        try {
            configuration = admin.createFactoryConfiguration("org.apache.felix.ipojo.test.scenarios.component.ReconfigurableSimpleType", null);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        Dictionary props = configuration.getProperties();
        if(props == null) {
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

        Assert.assertNull("No architecture", getContext().getServiceReferences(Architecture.class.getName(), "(architecture.instance="+pid+")"));


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

        Assert.assertNotNull("architecture", getContext().getServiceReferences(Architecture.class.getName(), "(architecture.instance="+pid+")"));
        Architecture arch = (Architecture) Utils.getServiceObject(getContext(), Architecture.class.getName(), "(architecture.instance="+pid+")");

        Assert.assertEquals("Is valid ?", ComponentInstance.VALID, arch.getInstanceDescription().getState());
	}

	public void testRevalidationOnReconfigurationWithController() {
		ComponentFactory factory = (ComponentFactory) Utils.getFactoryByName(getContext(),
				"org.apache.felix.ipojo.test.scenarios.component.ReconfigurableSimpleType2");

		// First inject a configuration triggering an exception of the validate method.
		Properties props = new Properties();
		props.put("prop", "KO");
		ComponentInstance ci = null;
		try {
			ci = factory.createComponentInstance(props);
		} catch (UnacceptableConfiguration e) {
			e.printStackTrace();
		} catch (MissingHandlerException e) {
			e.printStackTrace();
		} catch (ConfigurationException e) {
			e.printStackTrace();
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

	public void testRevalidationOnReconfigurationUsingConfigAdminAndController() throws InvalidSyntaxException {
		Configuration configuration = null;
        try {
            configuration = admin.createFactoryConfiguration("org.apache.felix.ipojo.test.scenarios.component.ReconfigurableSimpleType2",
            		null);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        Dictionary props = configuration.getProperties();
        if(props == null) {
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
        Assert.assertNotNull("architecture", getContext().getServiceReferences(Architecture.class.getName(), "(architecture.instance="+pid+")"));
        Architecture arch = (Architecture) Utils.getServiceObject(getContext(), Architecture.class.getName(), "(architecture.instance="+pid+")");

        Assert.assertEquals("Is invalid ?", ComponentInstance.INVALID, arch.getInstanceDescription().getState());

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

        Assert.assertNotNull("architecture", getContext().getServiceReferences(Architecture.class.getName(), "(architecture.instance="+pid+")"));
        arch = (Architecture) Utils.getServiceObject(getContext(), Architecture.class.getName(), "(architecture.instance="+pid+")");

        Assert.assertEquals("Is valid ?", ComponentInstance.VALID, arch.getInstanceDescription().getState());
	}

	public void testRevalidationOnReconfigurationOfTheController() {
		ComponentFactory factory = (ComponentFactory) Utils.getFactoryByName(getContext(),
				"org.apache.felix.ipojo.test.scenarios.component.ReconfigurableSimpleType3");

		// First inject a configuration triggering an exception of the validate method.
		Properties props = new Properties();
		props.put("controller", "false");
		ComponentInstance ci = null;
		try {
			ci = factory.createComponentInstance(props);
		} catch (UnacceptableConfiguration e) {
			e.printStackTrace();
		} catch (MissingHandlerException e) {
			e.printStackTrace();
		} catch (ConfigurationException e) {
			e.printStackTrace();
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

	public void testRevalidationOnReconfigurationUsingConfigAdminOfTheController() throws InvalidSyntaxException {
		Configuration configuration = null;
        try {
            configuration = admin.createFactoryConfiguration("org.apache.felix.ipojo.test.scenarios.component.ReconfigurableSimpleType3",
            		null);
        } catch (IOException e) {
            fail(e.getMessage());
        }
        Dictionary props = configuration.getProperties();
        if(props == null) {
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
        Assert.assertNotNull("architecture", getContext().getServiceReferences(Architecture.class.getName(), "(architecture.instance="+pid+")"));
        Architecture arch = (Architecture) Utils.getServiceObject(getContext(), Architecture.class.getName(), "(architecture.instance="+pid+")");

        Assert.assertEquals("Is invalid ?", ComponentInstance.INVALID, arch.getInstanceDescription().getState());

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

        Assert.assertNotNull("architecture", getContext().getServiceReferences(Architecture.class.getName(), "(architecture.instance="+pid+")"));
        arch = (Architecture) Utils.getServiceObject(getContext(), Architecture.class.getName(), "(architecture.instance="+pid+")");

        Assert.assertEquals("Is valid ?", ComponentInstance.VALID, arch.getInstanceDescription().getState());
	}
}
