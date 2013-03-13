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
 * Test the {@link Dependency} methods.
 */
public class DependencyTest extends TestCase {

	public void testConstructorParameter() {
		Dependency dep = new Dependency().setConstructorParameter(1);
		Element elem = dep.getElement();
		assertEquals("1", elem.getAttribute("constructor-parameter"));
	}

	public void testField() {
		Dependency dep = new Dependency().setField("field");
		Element elem = dep.getElement();
		assertEquals(null, elem.getAttribute("constructor-parameter"));
		assertEquals("field", elem.getAttribute("field"));
	}

	public void testAggregate() {
		Dependency dep = new Dependency().setField("field")
			.setAggregate(true);
		Element elem = dep.getElement();
		assertEquals(null, elem.getAttribute("constructor-parameter"));
		assertEquals("field", elem.getAttribute("field"));
		assertEquals("true", elem.getAttribute("aggregate"));
	}

	public void testOptional() {
		Dependency dep = new Dependency().setField("field")
			.setOptional(true);
		Element elem = dep.getElement();
		assertEquals(null, elem.getAttribute("constructor-parameter"));
		assertEquals("field", elem.getAttribute("field"));
		assertEquals("true", elem.getAttribute("optional"));
	}

	public void testNullable() {
		Dependency dep = new Dependency().setField("field").setOptional(true)
				.setNullable(false);
		Element elem = dep.getElement();
		assertEquals(null, elem.getAttribute("constructor-parameter"));
		assertEquals("field", elem.getAttribute("field"));
		assertEquals("true", elem.getAttribute("optional"));
		assertEquals("false", elem.getAttribute("nullable"));

		dep = new Dependency().setField("field").setOptional(true)
				.setNullable(true);
		elem = dep.getElement();
		assertEquals(null, elem.getAttribute("constructor-parameter"));
		assertEquals("field", elem.getAttribute("field"));
		assertEquals("true", elem.getAttribute("optional"));
		assertEquals(null, elem.getAttribute("nullable")); // Default value.
	}

	public void testFilter() {
		Dependency dep = new Dependency().setField("field")
			.setFilter("(my.prop=1)");
		Element elem = dep.getElement();
		assertEquals(null, elem.getAttribute("constructor-parameter"));
		assertEquals("field", elem.getAttribute("field"));
		assertEquals("(my.prop=1)", elem.getAttribute("filter"));
	}

	public void testFrom() {
		Dependency dep = new Dependency().setField("field")
			.setFrom("foo");
		Element elem = dep.getElement();
		assertEquals(null, elem.getAttribute("constructor-parameter"));
		assertEquals("field", elem.getAttribute("field"));
		assertEquals("foo", elem.getAttribute("from"));
	}

	public void testId() {
		Dependency dep = new Dependency().setField("field")
			.setId("foo");
		Element elem = dep.getElement();
		assertEquals(null, elem.getAttribute("constructor-parameter"));
		assertEquals("field", elem.getAttribute("field"));
		assertEquals("foo", elem.getAttribute("id"));
	}

	public void testProxy() {
		Dependency dep = new Dependency().setField("field").setOptional(true)
				.setProxy(false);
		Element elem = dep.getElement();
		assertEquals(null, elem.getAttribute("constructor-parameter"));
		assertEquals("field", elem.getAttribute("field"));
		assertEquals("true", elem.getAttribute("optional"));
		assertEquals("false", elem.getAttribute("proxy"));
	}

	public void testComparator() {
		Dependency dep = new Dependency().setField("field")
			.setComparator("my.Comparator");
		Element elem = dep.getElement();
		assertEquals(null, elem.getAttribute("constructor-parameter"));
		assertEquals("field", elem.getAttribute("field"));
		assertEquals("my.Comparator", elem.getAttribute("comparator"));
	}

	public void testBindingPolicy() {
		Dependency dep = new Dependency().setField("field")
			.setBindingPolicy(Dependency.DYNAMIC_PRIORITY);
		Element elem = dep.getElement();
		assertEquals(null, elem.getAttribute("constructor-parameter"));
		assertEquals("field", elem.getAttribute("field"));
		assertEquals("dynamic-priority", elem.getAttribute("policy"));

		dep = new Dependency().setField("field")
			.setBindingPolicy(Dependency.STATIC);
		elem = dep.getElement();
		assertEquals(null, elem.getAttribute("constructor-parameter"));
		assertEquals("field", elem.getAttribute("field"));
		assertEquals("static", elem.getAttribute("policy"));

		dep = new Dependency().setField("field")
			.setBindingPolicy(Dependency.DYNAMIC);
		elem = dep.getElement();
		assertEquals(null, elem.getAttribute("constructor-parameter"));
		assertEquals("field", elem.getAttribute("field"));
		assertEquals("dynamic", elem.getAttribute("policy"));
	}

}
