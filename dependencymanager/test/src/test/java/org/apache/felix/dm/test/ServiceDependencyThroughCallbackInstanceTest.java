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
package org.apache.felix.dm.test;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import junit.framework.Assert;

import org.apache.felix.dm.DependencyManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;

@RunWith(JUnit4TestRunner.class)
public class ServiceDependencyThroughCallbackInstanceTest {
    @Configuration
    public static Option[] configuration() {
        return options(
            provision(
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").version(Base.OSGI_SPEC_VERSION),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager").versionAsInProject()
            )
        );
    }    
    
    @Test
    public void testServiceWithCallbacksAndOneDependency(BundleContext context) {
        invokeTest(context, 1);
    }
    
    @Test
    public void testServiceWithCallbacksAndThreeDependencies(BundleContext context) {
        invokeTest(context, 3);
    }

    private void invokeTest(BundleContext context, int numberOfServices) {
        DependencyManager m = new DependencyManager(context);
        // create a number of services
		for (int i = 0; i < numberOfServices; i++) {
			final int num = i;
			m.add(m.createComponent()
			    .setInterface(Service.class.getName(), null)
			    .setImplementation(new Service() {
			        public String toString() {
			            return "A" + num;
			            }
			        }
			    )
		    );
		}

		// create a service with dependency which will be invoked on a callback instance
		CallbackInstance instance = new CallbackInstance();
		m.add(m.createComponent()
		    .setImplementation(new SimpleService() {})
		    .add(m.createServiceDependency()
				.setService(Service.class)
				.setCallbacks(instance, "added", "removed")
				.setRequired(true)
			)
		);
		
		Assert.assertEquals(numberOfServices, instance.getCount());
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
