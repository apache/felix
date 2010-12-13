package org.apache.felix.ipojo.test.scenarios.service.dependency.inner;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.test.scenarios.component.inner.C1;
import org.apache.felix.ipojo.test.scenarios.component.inner.C2;
import org.apache.felix.ipojo.test.scenarios.component.inner.C3;

public class InnerProxyMixTest extends OSGiTestCase {

	public static String C1 = C1.class.getName();
	public static String C2 = C2.class.getName();
	public static String C3 = C3.class.getName();

	IPOJOHelper ipojo;
	private ComponentInstance instancec1;
	private ComponentInstance instancec2;
	private ComponentInstance instancec3;

	public void setUp() {
		ipojo = new IPOJOHelper(this);
		// Create the instances
		instancec1 = ipojo.createComponentInstance(C1);
		instancec2 = ipojo.createComponentInstance(C2);
		instancec3 = ipojo.createComponentInstance(C3);
	}

	public void tearDown() {
		ipojo.dispose();
	}

	public void testMix() {
		// Check that everything is OK
		assertEquals(ComponentInstance.VALID, instancec1.getState());
		assertEquals(ComponentInstance.VALID, instancec2.getState());
		assertEquals(ComponentInstance.VALID, instancec3.getState());

		// Call C3
		C3 svc = (C3) getServiceObject(C3, null);
		assertNotNull(svc);
		assertEquals("called", svc.getFilter().authenticate());

		// So far, all right

		//We stop c1 and c2.
		instancec1.stop();
		instancec2.stop();

		assertEquals(ComponentInstance.INVALID, instancec3.getState()); // C2 dependency invalid

		instancec1.start();
		instancec2.start();

		// Check that everything is OK
		assertEquals(ComponentInstance.VALID, instancec1.getState());
		assertEquals(ComponentInstance.VALID, instancec2.getState());
		assertEquals(ComponentInstance.VALID, instancec3.getState());

		// Call C3
		svc = (C3) getServiceObject(C3, null);
		assertNotNull(svc);
		assertEquals("called", svc.getFilter().authenticate());
	}

}
