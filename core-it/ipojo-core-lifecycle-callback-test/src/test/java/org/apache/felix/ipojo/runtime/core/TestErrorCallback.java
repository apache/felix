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
package org.apache.felix.ipojo.runtime.core;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.runtime.core.services.CheckService;
import org.junit.After;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import java.util.Properties;

import static org.junit.Assert.assertNull;

/**
 * Test the fix for FELIX-1965.
 * When a validate callback throws an exception, the service is still provided.
 */
public class TestErrorCallback extends Common {
    
    ComponentInstance instance; // Instance under test

    @After
    public void tearDown() {
        if (instance != null) {
            instance.dispose();
            instance= null;
        }
    }
    
    @Test
    public void testErrorInValidateCallback() {
        Properties p2 = new Properties();
        p2.put("instance.name","error");
        instance = ipojoHelper.createComponentInstance("LFCB-CallbackWithError", p2);
        
        // The service should not be provided as the start method has thrown an exception
        ServiceReference ref = osgiHelper.getServiceReference(CheckService.class.getName(),
                "(instance.name=" + instance.getInstanceName() + ")");
        assertNull(ref);
    }
        

}
