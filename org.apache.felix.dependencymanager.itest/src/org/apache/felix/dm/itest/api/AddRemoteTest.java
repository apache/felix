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

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.itest.util.TestBase;
import org.junit.Assert;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * This test checks that a component service provider is not in the registry anymore whe nthe corresponding DM
 * component is removed from DependencyManager.
 */
public class AddRemoteTest extends TestBase {
	
	public void testAddRemove() throws InvalidSyntaxException, InterruptedException {
		DependencyManager m = getDM();
		
		String testBSN = super.context.getBundle().getSymbolicName();
		
		// Create a service providing component
		Component addRemove =
			m.createComponent().setInterface(Object.class.getName(), null).setImplementation(Object.class);
		
		// register it.
		m.add(addRemove);

		// check if the service is really in the osgi service registry.
		ServiceReference[] refs = super.context.getServiceReferences(Object.class.getName(), null);
        Assert.assertNotNull(refs);        
        Stream.of(refs)
        	.filter(ref -> ref.getBundle().getSymbolicName().equals(testBSN))
        	.findFirst()
        	.orElseThrow(() -> new RuntimeException("service provider not found from registry"));        

        // unregister the service provider
		m.remove(addRemove);	   		

		// Now, ensure the service provider is not present in the service registry anymore.
		refs = super.context.getServiceReferences(Object.class.getName(), null);
		if (refs != null) {
	        Stream.of(refs)
	        	.filter(ref -> ref.getBundle().getSymbolicName().equals(testBSN))
	        	.findFirst()
	        	.ifPresent(ref -> Assert.fail("found old service from registry: " + ref));
		}
		
		clearComponents();
	}
	
}
