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
package org.apache.felix.ipojo.test.scenarios.eh;

import java.util.List;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.component.CheckServiceHandlerDescription;
import org.apache.felix.ipojo.test.scenarios.util.Utils;

public class AutoHandlerTest extends OSGiTestCase {

	private static final String ORG_APACHE_FELIX_IPOJO_HANDLER_AUTO_PRIMITIVE = "org.apache.felix.ipojo.handler.auto.primitive";

	ComponentInstance instance;

	ComponentFactory factory;

	public void setUp() {
		factory = (ComponentFactory) Utils.getFactoryByName(getContext(), "HANDLER-HandlerTesterWO");
		System.setProperty(ORG_APACHE_FELIX_IPOJO_HANDLER_AUTO_PRIMITIVE, "");
	}

	public void tearDown() {
		if (instance != null) {
			instance.dispose();
		}
		instance = null;

		System.setProperty(ORG_APACHE_FELIX_IPOJO_HANDLER_AUTO_PRIMITIVE, "");

	}

	public void testRequiredHandlerList() {
		System.setProperty(ORG_APACHE_FELIX_IPOJO_HANDLER_AUTO_PRIMITIVE, "");

		factory.stop();
		factory.restart();
		factory.start();

		List list = factory.getRequiredHandlers();
		assertFalse(list.contains("org.apache.felix.ipojo.test.handler.checkservice:check"));

		String v = "org.apache.felix.ipojo.test.handler.checkservice:check";
		System.setProperty(ORG_APACHE_FELIX_IPOJO_HANDLER_AUTO_PRIMITIVE, v);

		factory.stop();
		factory.restart();
		factory.start();

		list = factory.getRequiredHandlers();
		assertTrue(list.contains("org.apache.felix.ipojo.test.handler.checkservice:check"));

		System.setProperty(ORG_APACHE_FELIX_IPOJO_HANDLER_AUTO_PRIMITIVE, "");

	}

	public void testInstanceCreation() throws Exception {
		String v = "org.apache.felix.ipojo.test.handler.checkservice:check";
		System.setProperty(ORG_APACHE_FELIX_IPOJO_HANDLER_AUTO_PRIMITIVE, v);

		factory.stop();
		factory.restart();
		factory.start();

		instance = factory.createComponentInstance(new Properties());
		assertEquals(ComponentInstance.VALID, instance.getState());

		HandlerDescription hd = instance.getInstanceDescription().getHandlerDescription(v);
		assertNotNull(hd);
		assertTrue(hd instanceof CheckServiceHandlerDescription);

		System.setProperty(ORG_APACHE_FELIX_IPOJO_HANDLER_AUTO_PRIMITIVE, "");

		factory.stop();
		factory.restart();
		factory.start();
	}
}
