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
package org.apache.felix.dm.test.annotation;

import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.provision;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.util.Hashtable;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.test.BundleGenerator;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * Use case: Verify the @ServiceLifecycle annotation, which allows a component to activate/deactivate itself programatically.
 */
@RunWith(JUnit4TestRunner.class)
public class PublisherAnnotationTest extends AnnotationBase
{
    @Configuration
    public static Option[] configuration()
    {
        return options(
            systemProperty(DMLOG_PROPERTY).value( "true" ),
            provision(
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").version("4.1.0"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager").versionAsInProject(),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.configadmin").version("1.2.4"),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager.runtime").versionAsInProject()),
            provision(
                new BundleGenerator()
                    .set(Constants.BUNDLE_SYMBOLICNAME, "PublisherAnnotationsTest")
                    .set("Export-Package", "org.apache.felix.dm.test.bundle.annotation.sequencer")
                    .set("Private-Package", "org.apache.felix.dm.test.bundle.annotation.publisher")
                    .set("Import-Package", "*")
                    .set("-plugin", "org.apache.felix.dm.annotation.plugin.bnd.AnnotationPlugin")
                    .build()));            
    }
    
    /**
     * A Service that just registers/unregisters its service, using the @ServiceLifecycle annotation.
     */
    @Test
    public void testServiceWithPublisher(BundleContext context)
    {
        DependencyManager m = new DependencyManager(context);
        registerSequencer(m, "ServiceTestWthPublisher"); 
        m_ensure.waitForStep(4, 10000);
    }
    
    /**
     * A Service instantiated from a FactorySet, and which registers/unregisters its service,
     * using the @ServiceLifecycle annotation.
     */
    @Test
    public void testFactoryServiceWithPublisher(BundleContext context)
    {
        DependencyManager m = new DependencyManager(context);
        registerSequencer(m, "FactoryServiceTestWthPublisher"); 
        m_ensure.waitForStep(5, 10000);
    }

    /**
     * Test an AdapterService which provides its interface using a @ServiceLifecycle.
     */
    @Test
    public void testAdapterServiceWithPublisher(BundleContext context)
    {
        DependencyManager m = new DependencyManager(context);
        registerSequencer(m, "AdapterServiceTestWithPublisher"); 
        m_ensure.waitForStep(6, 10000);
    }

    /**
     * Test a BundleAdapterService which provides its interface using a @ServiceLifecycle.
     */
    @Test
    public void testBundleAdapterServiceWithPublisher(BundleContext context)
    {
        DependencyManager m = new DependencyManager(context);
        registerSequencer(m, "BundleAdapterServiceTestWithPublisher"); 
        m_ensure.waitForStep(5, 10000);
    }

    /**
     * Test a ResourceAdapterService which provides its interface using a @ServiceLifecycle.
     */
    @Test
    public void TestResourceAdapterServiceWithPublisher(BundleContext context)
    {
        DependencyManager m = new DependencyManager(context);
        registerSequencer(m, "ResourceAdapterServiceTestWithPublisher"); 
        m_ensure.waitForStep(5, 10000);
    }

    /**
     * Test a FactoryConfigurationAdapterService which provides its interface using a @ServiceLifecycle.
     */
    @Test
    public void testFactoryAdapterServiceWithPublisher(BundleContext context)
    {
        DependencyManager m = new DependencyManager(context);
        registerSequencer(m, "FactoryConfigurationAdapterServiceTestWithPublisher"); 
        m_ensure.waitForStep(5, 10000);
    }
}
