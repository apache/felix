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
import org.apache.felix.dm.runtime.itest.components.AdapterServiceTestWithPublisher;
import org.apache.felix.dm.runtime.itest.components.BundleAdapterServiceTestWithPublisher;
import org.apache.felix.dm.runtime.itest.components.ComponentFactoryServiceTestWthPublisher;
import org.apache.felix.dm.runtime.itest.components.FactoryConfigurationAdapterServiceTestWithPublisher;
import org.apache.felix.dm.runtime.itest.components.FactoryServiceTestWthPublisher;
import org.apache.felix.dm.runtime.itest.components.ResourceAdapterServiceTestWithPublisher;
import org.apache.felix.dm.runtime.itest.components.ServiceTestWthPublisher;
import org.osgi.framework.ServiceRegistration;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class PublisherAnnotationTest extends TestBase {
    
    /**
     * A Service that just registers/unregisters its service, using the @ServiceLifecycle annotation.
     */
    public void testServiceWithPublisher() {
        Ensure e = new Ensure();
        ServiceRegistration sr = register(e, ServiceTestWthPublisher.ENSURE);
        e.waitForStep(4, 10000);
        sr.unregister();
    }

    /**
     * A Service instantiated from a FactorySet, and which registers/unregisters its service,
     * using the @ServiceLifecycle annotation.
     */
    public void testFactoryServiceWithPublisher() {
        Ensure e = new Ensure();
        ServiceRegistration sr = register(e, FactoryServiceTestWthPublisher.ENSURE);
        e.waitForStep(5, 10000);
        sr.unregister();
    }

    /**
     * A Service instantiated from a DM ComponentFactory, and which registers/unregisters its service,
     * using the @ServiceLifecycle annotation.
     */
    public void testComponentFactoryServiceWithPublisher() {
        Ensure e = new Ensure();
        ServiceRegistration sr = register(e, ComponentFactoryServiceTestWthPublisher.ENSURE);
        e.waitForStep(5, 10000);
        sr.unregister();
    }

    /**
     * Test an AdapterService which provides its interface using a @ServiceLifecycle.
     */
    public void testAdapterServiceWithPublisher() {
        Ensure e = new Ensure();
        ServiceRegistration sr = register(e, AdapterServiceTestWithPublisher.ENSURE);
        e.waitForStep(6, 10000);
        sr.unregister();
    }

    /**
     * Test a BundleAdapterService which provides its interface using a @ServiceLifecycle.
     */
    public void testBundleAdapterServiceWithPublisher() {
        Ensure e = new Ensure();
        ServiceRegistration sr = register(e, BundleAdapterServiceTestWithPublisher.ENSURE);
        e.waitForStep(5, 10000);
        sr.unregister();
    }

    /**
     * Test a ResourceAdapterService which provides its interface using a @ServiceLifecycle.
     */
    public void TestResourceAdapterServiceWithPublisher() {
        Ensure e = new Ensure();
        ServiceRegistration sr = register(e, ResourceAdapterServiceTestWithPublisher.ENSURE);
        e.waitForStep(5, 10000);
        sr.unregister();
    }

    /**
     * Test a FactoryConfigurationAdapterService which provides its interface using a @ServiceLifecycle.
     */
    public void testFactoryAdapterServiceWithPublisher() {
        Ensure e = new Ensure();
        ServiceRegistration sr = register(e, FactoryConfigurationAdapterServiceTestWithPublisher.ENSURE);
        e.waitForStep(5, 10000);
        sr.unregister();
    }
}
