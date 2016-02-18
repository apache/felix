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
package org.apache.felix.dm.lambda.itest;

import static org.apache.felix.dm.lambda.DependencyManagerActivator.component;

import org.apache.felix.dm.DependencyManager;
import org.junit.Assert;
import org.osgi.framework.BundleContext;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceDependencyThroughCallbackInstanceTest extends TestBase {
    public void testServiceWithCallbacksAndOneDependency() {
        invokeTest(context, 1);
    }
    
    public void testServiceWithCallbacksAndThreeDependencies() {
        invokeTest(context, 3);
    }

    private void invokeTest(BundleContext context, int numberOfServices) {
        DependencyManager m = getDM();
        // create a number of services
		for (int i = 0; i < numberOfServices; i++) {
			final int num = i;
			component(m, comp -> comp
					.provides(Service.class).impl(new Service() {
						public String toString() {
							return "A" + num;
						}}));
		}

		// create a service with dependency which will be invoked on a callback instance
		CallbackInstance instance = new CallbackInstance();
		component(m, comp -> comp
				.impl(new SimpleService() {})
				.withSvc(Service.class, srv -> srv.add(instance::added).remove(instance::removed)));
		
		Assert.assertEquals(numberOfServices, instance.getCount());
		m.clear();
    }
    
    public static interface Service {
    }
    
    public static interface SimpleService {
    }
    
    public static class CallbackInstance {
    	int m_count = 0;

    	void added(Service service) {
    	    System.out.println("added " + service);
    		m_count++;
    	}
    	
    	void removed(Service service) {
    	}	
    	
    	int getCount() {
    		return m_count;
    	}
    }
}
