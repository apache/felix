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

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.test.Base;
import org.apache.felix.dm.test.BundleGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * Use case: Verify Aspect Annotations usage.
 */
@RunWith(JUnit4TestRunner.class)
public class AspectAnnotationTest extends AnnotationBase
{
    @Configuration
    public static Option[] configuration()
    {
        return options(
            systemProperty(DMLOG_PROPERTY).value( "true" ),
            provision(
                mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium").version(Base.OSGI_SPEC_VERSION),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager").versionAsInProject(),
                mavenBundle().groupId("org.apache.felix").artifactId("org.apache.felix.dependencymanager.runtime").versionAsInProject()),
            provision(
                new BundleGenerator()
                    .set(Constants.BUNDLE_SYMBOLICNAME, "AspectTest")
                    .set("Export-Package", "org.apache.felix.dm.test.bundle.annotation.sequencer")
                    .set("Private-Package", "org.apache.felix.dm.test.bundle.annotation.aspect")
                    .set("Import-Package", "*")
                    .set("-plugin", "org.apache.felix.dm.annotation.plugin.bnd.AnnotationPlugin")
                    .build()));           
    }

    @Test
    public void testAspectChain(BundleContext context) throws Throwable
    {
        DependencyManager m = new DependencyManager(context);
        // Activate service consumer
        Component scSequencer = makeSequencer(m, "AspectChainTest.ServiceConsumer");
        m.add(scSequencer);
        // Activate service provider
        Component spSequencer = makeSequencer(m, "AspectChainTest.ServiceProvider");
        m.add(spSequencer);
        // Activate service aspect 2
        Component sa2Sequencer = makeSequencer(m, "AspectChainTest.ServiceAspect2");
        m.add(sa2Sequencer);
        // Activate service aspect 3
        Component sa3Sequencer = makeSequencer(m, "AspectChainTest.ServiceAspect3");
        m.add(sa3Sequencer);
        // Activate service aspect 1
        Component sa1Sequencer = makeSequencer(m, "AspectChainTest.ServiceAspect1");
        m.add(sa1Sequencer);

        m_ensure.step();
        m_ensure.waitForStep(6, 10000);

        // Deactivate service provider
        m.remove(spSequencer);
        // Make sure that service aspect 1 has been called in ts removed and stop callbacks 
        m_ensure.waitForStep(8, 10000);
        m_ensure.ensure();
    }    
}
