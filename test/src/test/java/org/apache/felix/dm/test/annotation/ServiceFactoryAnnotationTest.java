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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.test.Base;
import org.apache.felix.dm.test.BundleGenerator;
import org.apache.felix.dm.test.bundle.annotation.factory.MyService;
import org.apache.felix.dm.test.bundle.annotation.factory.MyServiceInterface;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

@RunWith(JUnit4TestRunner.class)
public class ServiceFactoryAnnotationTest extends AnnotationBase {
    @Configuration
    public static Option[] configuration() {
        return options(
                systemProperty(DMLOG_PROPERTY).value("true"),
                provision(
                        mavenBundle().groupId("org.osgi").artifactId("org.osgi.compendium")
                                .version(Base.OSGI_SPEC_VERSION),
                        mavenBundle().groupId("org.apache.felix")
                                .artifactId("org.apache.felix.dependencymanager").versionAsInProject(),
                        mavenBundle().groupId("org.apache.felix")
                                .artifactId("org.apache.felix.dependencymanager.runtime")
                                .versionAsInProject()),
                provision(new BundleGenerator()
                        .set(Constants.BUNDLE_SYMBOLICNAME, "ServiceFactoryAnnotationTest")
                        .set("Export-Package", "org.apache.felix.dm.test.bundle.annotation.sequencer")
                        .set("Private-Package", "org.apache.felix.dm.test.bundle.annotation.factory")
                        .set("Import-Package", "*")
                        .set("-plugin", "org.apache.felix.dm.annotation.plugin.bnd.AnnotationPlugin").build()));
    }

    @Test
    public void testServiceFactory(BundleContext context) {
        DependencyManager m = new DependencyManager(context);

        // We provide ourself as a "Sequencer" service to the annotated bundles.
        m.add(m.createComponent().setImplementation(ServiceFactoryAnnotationTest.this)
                .setInterface(Sequencer.class.getName(), null));

        // Wait for the factory.
        m.add(m.createComponent()
                .setImplementation(this)
                .add(m.createServiceDependency()
                        .setService(Set.class, "(" + Component.FACTORY_NAME + "=MyServiceFactory)")
                        .setRequired(true).setCallbacks("bindFactory", null)));

        // Check if the test.annotation components have been initialized orderly
        m_ensure.waitForStep(10, 5000);
    }

    void bindFactory(Set factory) {
        // create a service instance with this configuration
        Hashtable conf = new Hashtable();
        conf.put("instance.id", "instance");
        conf.put(".private.param", "private");
        Assert.assertTrue(factory.add(conf));        
        m_ensure.waitForStep(4, 5000);

        // update the service instance
        conf.put("instance.modified", "true");
        Assert.assertFalse(factory.add(conf));
        m_ensure.waitForStep(7, 5000);

        // remove instance
        Assert.assertTrue(factory.remove(conf));
    }
}