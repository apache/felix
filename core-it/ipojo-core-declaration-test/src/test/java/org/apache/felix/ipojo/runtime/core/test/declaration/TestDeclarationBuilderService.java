/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.runtime.core.test.declaration;

import org.apache.felix.ipojo.IPojoFactory;
import org.apache.felix.ipojo.extender.DeclarationBuilderService;
import org.apache.felix.ipojo.extender.DeclarationHandle;
import org.apache.felix.ipojo.extender.ExtensionDeclaration;
import org.apache.felix.ipojo.extender.builder.FactoryBuilder;
import org.apache.felix.ipojo.extender.builder.FactoryBuilderException;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.runtime.core.test.services.HelloService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import static java.lang.String.format;
import static junit.framework.Assert.*;

public class TestDeclarationBuilderService extends Common {

    private DeclarationBuilderService builder;
    private DeclarationHandle handle;

    @Before
    public void setUp() {
        builder = osgiHelper.getServiceObject(DeclarationBuilderService.class);
    }

    @After
    public void tearDown() {
        if (handle != null) {
            handle.retract();
        }
    }

    @Test
    public void testAnonymousInstanceCreation() {
        handle = builder.newInstance("org.apache.felix.ipojo.runtime.core.test.components.FrenchHelloService")
                .build();

        // When a  service is registered, all events are fired synchronously, we
        // can safely test the declaration binding
        assertFalse(handle.getStatus().isBound());
        handle.publish();

        // This test has been already ssen as failed.
        HelloService hs = osgiHelper.waitForService(HelloService.class, "(factory.name=org.apache.felix.ipojo.runtime" +
                ".core.test.components.FrenchHelloService)", 1000);
        assertNotNull(hs);
        System.out.println("Status: " + handle.getStatus().isBound());

        assertTrue(handle.getStatus().isBound());
        handle.retract();
        assertFalse(handle.getStatus().isBound());
    }

    @Test
    public void testNamedInstanceCreation() {
        handle = builder.newInstance("org.apache.felix.ipojo.runtime.core.test.components.FrenchHelloService")
                .name("bonjour-service")
                .build();

        handle.publish();
        assertTrue(ipojoHelper.isServiceAvailableByName(HelloService.class.getName(), "bonjour-service"));
    }

    @Test
    public void testConfiguredInstanceCreation() {
        handle = builder.newInstance("org.apache.felix.ipojo.runtime.core.test.components.FrenchHelloService")
                .name("bonjour-service")
                .configure()
                .property("message", "Salut")
                .build();

        handle.publish();
        assertTrue(ipojoHelper.isServiceAvailableByName(HelloService.class.getName(), "bonjour-service"));

        HelloService service = osgiHelper.getServiceObject(HelloService.class, format("(instance.name=%s)", "bonjour-service"));
        assertEquals(service.hello("Guillaume"), "Salut Guillaume");
    }

    @Test
    public void testVersionedTypeInstanceCreation() {
        handle = builder.newInstance("hello-service")
                .version("2.0")
                .name("hello2")
                .build();

        handle.publish();

        String filter = format("(instance.name=%s)", "hello2");
        osgiHelper.waitForService(HelloService.class, filter, 1000);
        HelloService service = osgiHelper.getServiceObject(HelloService.class, filter);
        assertEquals(service.hello("Guillaume"), "Hello2 Guillaume");
    }

    @Test
    public void testExtensionCreation() {
        handle = builder.newExtension("test", new EmptyFactoryBuilder());

        handle.publish();

        osgiHelper.waitForService(ExtensionDeclaration.class, null, 1000);
    }

    @Test
    public void testTypeCreation() throws Exception {

        handle = builder.newType(germanComponent());
        handle.publish();

        DeclarationHandle instance = builder.newInstance("german-service")
                .name("german-hello")
                .build();
        instance.publish();

        System.out.println(instance.getStatus().getMessage());

        String filter = format("(instance.name=%s)", "german-hello");
        osgiHelper.waitForService(HelloService.class, filter, 1000);
        HelloService service = osgiHelper.getServiceObject(HelloService.class, filter);
        assertEquals(service.hello("Guillaume"), "Hallo Guillaume");

        instance.retract();

    }

    /*
    @Test
    public void testTypeCreationFromAPI() throws Exception {
        PrimitiveComponentType type = new PrimitiveComponentType()
                .setClassName("org.apache.felix.ipojo.runtime.core.test.components.GermanHelloService")
                .setComponentTypeName("german-service")
                .addService(new Service());

        Element description = type.getFactory().getComponentMetadata();
        handle = builder.newType(description);
        handle.publish();

        DeclarationHandle instance = builder.newInstance("german-service")
                .name("german-hello")
                .build();
        instance.publish();

        String filter = format("(instance.name=%s)", "german-hello");
        osgiHelper.waitForService(HelloService.class, filter, 1000);
        HelloService service = osgiHelper.getServiceObject(HelloService.class, filter);
        assertEquals(service.hello("Guillaume"), "Hallo Guillaume");

        instance.retract();

    }
    */

    private Element germanComponent() {
        Element component = new Element("component", null);
        component.addAttribute(new Attribute("name", "german-service"));
        component.addAttribute(new Attribute("classname", "org.apache.felix.ipojo.runtime.core.test.components.GermanHelloService"));
        component.addElement(new Element("provides", null));
        component.addElement(manipulation());
        return component;
    }

    private Element manipulation() {
        Element manipulation = new Element("manipulation", null);
        manipulation.addAttribute(new Attribute("classname", "org.apache.felix.ipojo.runtime.core.test.components.GermanHelloService"));
        manipulation.addAttribute(new Attribute("super", "java.lang.Object"));

        Element itf = new Element("interface", null);
        itf.addAttribute(new Attribute("name", "org.apache.felix.ipojo.runtime.core.test.services.HelloService"));
        manipulation.addElement(itf);

        Element method = new Element("method", null);
        method.addAttribute(new Attribute("name", "hello"));
        method.addAttribute(new Attribute("return", "java.lang.String"));
        method.addAttribute(new Attribute("arguments", "{java.lang.String}"));
        method.addAttribute(new Attribute("names", "{name}"));
        manipulation.addElement(method);

        return manipulation;
    }

    private static class EmptyFactoryBuilder implements FactoryBuilder {
        @Override
        public IPojoFactory build(final BundleContext bundleContext, final Element metadata) throws FactoryBuilderException {
            return null;
        }
    }
}
