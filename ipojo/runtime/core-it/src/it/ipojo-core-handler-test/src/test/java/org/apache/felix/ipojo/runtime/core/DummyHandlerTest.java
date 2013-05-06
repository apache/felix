/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.runtime.core;

import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.apache.felix.ipojo.*;
import org.apache.felix.ipojo.runtime.core.components.DummyImpl;
import org.apache.felix.ipojo.runtime.core.handlers.DummyHandler;
import org.apache.felix.ipojo.runtime.core.services.Dummy;
import org.apache.felix.ipojo.runtime.core.services.User;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.ow2.chameleon.testing.helpers.TimeUtils;
import org.ow2.chameleon.testing.tinybundles.ipojo.IPOJOStrategy;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;


@ExamReactorStrategy(PerMethod.class)
public class DummyHandlerTest extends Common {

    private static final String DUMMY_TEST_FACTORY = "dummy.test";
    /*
     * Number of mock object by test.
     */
    private static final int NB_MOCK = 10;

    @Configuration
    public Option[] config() throws IOException {
        Option[] options = super.config();

        // Build handler bundle
        File handlerJar = new File("target/bundles/handler.jar");
        FileUtils.copyInputStreamToFile(
                TinyBundles.bundle()
                        .add(DummyHandler.class)
                        .set(org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME, "Dummy.Handler")
                        .build(IPOJOStrategy.withiPOJO(new File("src/main/resources/dummy-handler.xml"))),
                handlerJar);

        // Build consumer bundle
        File dummyJar = new File("target/bundles/dummy.jar");
        FileUtils.copyInputStreamToFile(
                TinyBundles.bundle()
                        .add(DummyImpl.class)
                        .set(org.osgi.framework.Constants.BUNDLE_SYMBOLICNAME, "Dummy.Bundle")
                        .build(IPOJOStrategy.withiPOJO(new File("src/main/resources/dummy-component.xml"))),
                dummyJar);


        return OptionUtils.combine(options,
                streamBundle(TinyBundles.bundle()
                        .add(Dummy.class)
                        .add(User.class)
                        .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.runtime.core.services")
                        .set(Constants.BUNDLE_SYMBOLICNAME, "service")
                        .build(withBnd())
                ),
                bundle(handlerJar.toURI().toURL().toExternalForm()),
                bundle(dummyJar.toURI().toURL().toExternalForm())
        );
    }

    /**
     * Basic Test, in order to know if the instance is correctly create.
     */
    @Test
    public void testDummyTestInstance() {
        ComponentInstance instance;

        // Get the factory
        Factory factory = Tools.getValidFactory(osgiHelper, DUMMY_TEST_FACTORY);
        Assert.assertNotNull(factory);

        // Create an instance
        try {
            instance = factory.createComponentInstance(null);
        } catch (UnacceptableConfiguration e) {
            throw new AssertionError(e);
        } catch (MissingHandlerException e) {
            throw new AssertionError(e);
        } catch (ConfigurationException e) {
            throw new AssertionError(e);
        }

        // Must be valid now
        Assert.assertEquals(instance.getState(), ComponentInstance.VALID);

        // Stop the instance
        instance.stop();
        Assert.assertEquals(instance.getState(), ComponentInstance.STOPPED);

        // Start the instance
        instance.start();
        Assert.assertEquals(instance.getState(), ComponentInstance.VALID);
    }

    /**
     * Test if the bind and unbind methods are called when the bind service are registered after the instance creation
     */
    @Test
    public void testDummyTestBindAfterStart() {
        // Get the factory
        Factory factory = Tools.getValidFactory(osgiHelper, DUMMY_TEST_FACTORY);
        assertNotNull(factory);

        // Create an instance, it will be disposed by the helper
        ComponentInstance instance = ipojoHelper.createComponentInstance(DUMMY_TEST_FACTORY);
        assertTrue(instance.getState() == ComponentInstance.VALID);

        Map<MyUser, ServiceRegistration> registrations = new HashMap<MyUser, ServiceRegistration>();

        for (int i = 0; i < NB_MOCK; i++) {
            MyUser service = new MyUser();
            ServiceRegistration sr = bc.registerService(User.class.getName(), service, null);
            registrations.put(service, sr);
        }

        TimeUtils.grace(200);
        assertEquals(osgiHelper.getServiceReferences(User.class, null).length, NB_MOCK);

        //verify that the bind method of the handler has been called
        for (MyUser user : registrations.keySet()) {
            assertTrue(user.name);
        }

        //verify that the unbind has been called
        for (MyUser user : registrations.keySet()) {
            registrations.get(user).unregister();
            assertTrue(user.type);
        }
    }

    /**
     * Test if the bind and unbind methods when the bind services are registered before the instance creation
     */
    @Test
    public void testDummyTestBindBeforeStart() {
        ComponentInstance instance = null;

        Map<MyUser, ServiceRegistration> registrations = new HashMap<MyUser, ServiceRegistration>();

        for (int i = 0; i < NB_MOCK; i++) {
            MyUser service = new MyUser();
            ServiceRegistration sr = bc.registerService(User.class.getName(), service, null);
            registrations.put(service, sr);
        }

        // Get the factory
        Factory factory = Tools.getValidFactory(osgiHelper, DUMMY_TEST_FACTORY);
        assertNotNull(factory);

        // The instance will be disposed by the helper
        ipojoHelper.createComponentInstance(DUMMY_TEST_FACTORY);

        //verify that the bind method of the handler has been called
        for (MyUser user : registrations.keySet()) {
            assertTrue(user.name);
        }

        //verify that the unbind has been called
        for (MyUser user : registrations.keySet()) {
            registrations.get(user).unregister();
            assertTrue(user.type);
        }
    }

    private class MyUser implements User {

        boolean name;
        boolean type;

        @Override
        public String getName() {
            name = true;
            return "name";
        }

        @Override
        public int getType() {
            type = true;
            return 1;
        }
    }
}
