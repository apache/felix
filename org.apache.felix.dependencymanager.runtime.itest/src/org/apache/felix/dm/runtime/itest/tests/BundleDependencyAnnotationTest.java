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
import org.apache.felix.dm.runtime.itest.components.BundleDependencyAnnotation;
import org.osgi.framework.ServiceRegistration;

/**
 * Use case: Verify Bundle Dependency annotations usage.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class BundleDependencyAnnotationTest extends TestBase {
    
    /**
     * Tests a simple Consumer, which has a BundleDependency over the dependency manager bundle.
     * TODO: this test is not currently working.
     */
    public void testBundleDependencyAnnotation() {
        Ensure e = new Ensure();
        ServiceRegistration sr = register(e, BundleDependencyAnnotation.ENSURE_CONSUMER);
        e.waitForStep(2, 10000);
        stopBundle(BundleDependencyAnnotation.METATYPE_BSN);
        e.waitForStep(4, 10000);
        sr.unregister();
        startBundle(BundleDependencyAnnotation.METATYPE_BSN);
    }

    /**
     * Tests a Bundle Adapter, which adapts the dependency manager bundle to a "ServiceInterface" service.
     * @throws Throwable 
     */
    public void testBundleAdapterServiceAnnotation() throws Throwable {
        Ensure e = new Ensure();
        ServiceRegistration sr = register(e, BundleDependencyAnnotation.ENSURE_ADAPTER);
        e.waitForStep(3, 10000);
        e.ensure();
        sr.unregister();
    }
}
