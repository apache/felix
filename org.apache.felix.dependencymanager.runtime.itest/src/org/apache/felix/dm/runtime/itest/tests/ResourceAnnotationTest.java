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
package org.apache.felix.dm.runtime.itest.tests;

import org.apache.felix.dm.itest.util.Ensure;
import org.apache.felix.dm.itest.util.TestBase;
import org.apache.felix.dm.runtime.itest.components.ResourceAnnotation;
import org.osgi.framework.ServiceRegistration;

/**
 * Use case: Verify Bundle Dependency annotations usage.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ResourceAnnotationTest extends TestBase {
    
    /**
     * Tests a simple ResourceConsumer
     * @param context
     */
    public void testResourceAnnotation() {
        Ensure e = new Ensure();
        ServiceRegistration sr = register(e, ResourceAnnotation.ENSURE_RESOURCE);
        ServiceRegistration sr2 = register(e, ResourceAnnotation.ENSURE_PROVIDER);
        sr.unregister();
        sr2.unregister();
        e.waitForStep(1, 10000);
    }

    /**
     * Tests a simple ResourceConsumer using a class field for resource injection
     */
    public void testResourceAnnotationAutoConfig() {
        Ensure e = new Ensure();
        ServiceRegistration sr = register(e, ResourceAnnotation.ENSURE_FIELD);
        ServiceRegistration sr2 = register(e, ResourceAnnotation.ENSURE_PROVIDER);
        sr.unregister();
        sr2.unregister();
        e.waitForStep(1, 10000);
    }

    /**
      * Tests a ResourceAdapter
      * @param context
      */
    public void testResourceAdapterAnnotation() throws Throwable {
        Ensure e = new Ensure();
        ServiceRegistration sr = register(e, ResourceAnnotation.ENSURE_ADAPTER);
        ServiceRegistration sr2 = register(e, ResourceAnnotation.ENSURE_PROVIDER);
        sr.unregister();
        sr2.unregister();
        e.waitForStep(2, 10000);
        e.ensure();
    }
}
