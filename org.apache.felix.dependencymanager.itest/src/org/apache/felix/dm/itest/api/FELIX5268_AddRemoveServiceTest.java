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
package org.apache.felix.dm.itest.api;

import java.util.stream.Stream;

import org.apache.felix.dm.itest.bundle2.AddRemoveService;
import org.apache.felix.dm.itest.util.TestBase;
import org.junit.Assert;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

/**
 * FELIX_XXX: When a bundle activator is invoked because the bundle is starting, then if the
 * activator adds/removes a DM service providing component, then the service should be also removed from the
 * OSGI service registry.
 */
public class FELIX5268_AddRemoveServiceTest extends TestBase {
	
	public void testStartStop() throws BundleException {
		// Lookup our test bundle which adds/removes a "AddRemoveService" component.
		
		Bundle testBundle = Stream.of(context.getBundles())
			.filter(b -> b.getSymbolicName().equals("org.apache.felix.dependencymanager.itest.bundle2"))
			.findFirst()
			.orElseThrow(() -> new RuntimeException("cound not find bundle2 test."));
		
		// Stop it
		testBundle.stop();
		
		// Restart it
		testBundle.start();
		
		// At this point, then AddRemoveService should not be registered in the OSGI service registry.
		ServiceReference ref = context.getServiceReference(AddRemoveService.class.getName());
		Assert.assertNull(ref);

		// We don't need this bundle anymore, stop it.
		testBundle.stop();
	}
}
