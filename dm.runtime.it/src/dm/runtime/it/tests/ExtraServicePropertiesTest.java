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
package dm.runtime.it.tests;

import org.osgi.framework.ServiceRegistration;

import dm.it.Ensure;
import dm.it.TestBase;
import dm.runtime.it.components.ExtraAdapterServiceProperties;
import dm.runtime.it.components.ExtraFactoryServiceProperties;
import dm.runtime.it.components.ExtraServiceProperties;

/**
 * Use case: Verify the a Service may provide its service properties dynamically from its start method.
 */
public class ExtraServicePropertiesTest extends TestBase {
    
    public ExtraServicePropertiesTest() { 
        super(false); /* don't autoclear managers when one test is done */ 
    }

    /**
     * Tests if a Service can provide its service properties from its start method.
     */
    public void testExtraServiceProperties() {
        Ensure e = new Ensure();
        ServiceRegistration sr = register(e, ExtraServiceProperties.ENSURE);
        e.waitForStep(2, 10000);
        sr.unregister();
    }

    /**
     * Tests if a Service instantiated by a Factory can provide its service properties from its start method.
     */
    public void testExtraFactoryServiceProperties() {
        Ensure e = new Ensure();
        ServiceRegistration sr = register(e, ExtraFactoryServiceProperties.ENSURE);
        e.waitForStep(3, 10000);
        sr.unregister();
    }

    /**
     * Tests if an AdapterService can provide its service properties from its start method.
     */
    public void testExtraAdapterServiceProperties() {
        Ensure e = new Ensure();
        ServiceRegistration sr = register(e, ExtraAdapterServiceProperties.ENSURE);
        e.waitForStep(3, 10000);
        sr.unregister();
    }

    /**
     * Tests if an AspectService can provide its service properties from its start method.
     */
    // TODO
    public void testExtraAspectServiceProperties() {
//        Ensure e = new Ensure();
//        ServiceRegistration sr = register(e, ExtraAspectServiceProperties.ENSURE);
//        e.waitForStep(3, 10000);
//        sr.unregister();
    }
}
