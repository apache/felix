package org.apache.felix.ipojo.test.scenarios.factories;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.util.Utils;

public class ReconfigurationTest extends OSGiTestCase {


	public void testRevalidationOnREconfiguration() {
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



}
