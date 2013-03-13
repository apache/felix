/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
