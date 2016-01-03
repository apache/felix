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

import org.apache.felix.dm.itest.bundle.HelloWorld;
import org.apache.felix.dm.itest.util.TestBase;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FELIX5153_StopBeforeUngetServiceTest extends TestBase {
    public void testCallStopAfterUngetService() throws Throwable {
        Bundle testBundle = null;
        
        try {
    	    ServiceReference[] refs = context.getServiceReferences(HelloWorld.class.getName(), "(dm=dm4)");
    	    HelloWorld service = (HelloWorld) context.getService(refs[0]);
    	    System.out.println(service.sayIt("DM4"));

    		for (Bundle b : context.getBundles()) {
    			if (b.getSymbolicName().equals("org.apache.felix.dependencymanager.itest.bundle")) {
    				testBundle = b;
    				// Stop the test bundle. In this test bundle, we have a service factory component, and its unget method
    				// is expected to be called *before* its stop() method.
    				b.stop();
    				break;
    			}
    		}
    	} 
    	
    	catch (Throwable t) {
    		error("test failed", t);
    	}
    	
    	finally {
    	    if (testBundle != null) {
    	        testBundle.start(); // restart the test bundle
    	    }
    	}
    }
}
