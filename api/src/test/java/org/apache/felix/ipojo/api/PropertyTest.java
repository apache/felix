package org.apache.felix.ipojo.api;

import junit.framework.TestCase;

import org.apache.felix.ipojo.metadata.Element;

/**
 * Test the {@link Property} methods.
 */
public class PropertyTest extends TestCase {

	public void testConstructorParameter() {
		Property prop = new Property().setConstructorParameter(1);
		Element elem = prop.getElement();
		assertEquals("1", elem.getAttribute("constructor-parameter"));
	}

	public void testField() {
		Property prop = new Property().setField("field");
		Element elem = prop.getElement();
		assertEquals(null, elem.getAttribute("constructor-parameter"));
		assertEquals("field", elem.getAttribute("field"));
	}

	public void testMandatoryPropertyWithValue() {
		Property prop = new Property().setField("field")
			.setMandatory(true)
			.setName("prop")
			.setValue("foo")
			.setImmutable(true);
		Element elem = prop.getElement();
		assertEquals(null, elem.getAttribute("constructor-parameter"));
		assertEquals("field", elem.getAttribute("field"));
		assertEquals("prop", elem.getAttribute("name"));
		assertEquals("foo", elem.getAttribute("value"));
		assertEquals("true", elem.getAttribute("mandatory"));
		assertEquals("true", elem.getAttribute("immutable"));
	}


}
