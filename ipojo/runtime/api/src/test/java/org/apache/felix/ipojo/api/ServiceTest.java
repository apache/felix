package org.apache.felix.ipojo.api;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.metadata.Element;

import junit.framework.TestCase;

/**
 * Test the {@link Service} methods.
 */
public class ServiceTest extends TestCase {

	public void testServiceController() {
		Service svc = new Service().setServiceController("m_controller", true);

		Element elem = svc.getElement();
		assertTrue(elem.getElements("controller").length == 1);
		Element controller = elem.getElements("controller")[0];
		assertEquals("m_controller", controller.getAttribute("field"));
		assertEquals("true", controller.getAttribute("value"));

		svc = new Service().setServiceController("m_controller", false);
		elem = svc.getElement();
		assertTrue(elem.getElements("controller").length == 1);
		controller = elem.getElements("controller")[0];
		assertEquals("m_controller", controller.getAttribute("field"));
		assertEquals("false", controller.getAttribute("value"));
	}

	public void testRegistrationCallbacks() {
		Service svc = new Service()
			.setPostRegistrationCallback("registration")
			.setPostUnregistrationCallback("unregistration");

		Element elem = svc.getElement();
		assertEquals("registration", elem.getAttribute("post-registration"));
		assertEquals("unregistration", elem.getAttribute("post-unregistration"));
	}

	public void testSpecificationWithOneService() {
		Service svc = new Service()
			.setSpecification("org.foo.acme.MyService");

		Element elem = svc.getElement();
		assertEquals("org.foo.acme.MyService", elem.getAttribute("specifications"));
	}

	public void testSpecificationWithTwoServices() {
		List spec = new ArrayList();
		spec.add("org.foo.acme.MyService");
		spec.add("org.foo.acme.MyService2");
		Service svc = new Service()
			.setSpecifications(spec);

		Element elem = svc.getElement();
		assertEquals("{org.foo.acme.MyService,org.foo.acme.MyService2}", elem.getAttribute("specifications"));
	}


}
