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
package org.apache.felix.ipojo.handlers.providedservice;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.felix.ipojo.*;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.handlers.dependency.DependencyHandler;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.PojoMetadata;
import org.apache.felix.ipojo.test.MockBundle;
import org.apache.felix.ipojo.util.Logger;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Dictionary;
import java.util.Properties;

public class ProvidedServiceHandlerTest extends TestCase {

    BundleContext context;
    ComponentFactory factory;
    InstanceManager im;
    ComponentTypeDescription desc;
    ProvidedServiceHandler handler;
    Logger logger;

    public void setUp() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        context = (BundleContext) Mockito.mock(BundleContext.class);
        Mockito.when(context.getProperty(DependencyHandler.PROXY_TYPE_PROPERTY)).thenReturn(null);
        Mockito.when(context.getProperty(Logger.IPOJO_LOG_LEVEL_PROP)).thenReturn(null);
        Mockito.when(context.getBundle()).thenReturn(new MockBundle(this.getClass().getClassLoader()));

        factory = (ComponentFactory) Mockito.mock(ComponentFactory.class);
        Mockito.when(factory.getBundleClassLoader()).thenReturn(ProvidedServiceHandler.class.getClassLoader());
        logger = Mockito.spy(new Logger(context, "TEST", Logger.INFO));
        Mockito.when(factory.getLogger()).thenReturn(logger);

        im = (InstanceManager) Mockito.mock(InstanceManager.class);
        Mockito.when(im.getContext()).thenReturn(context);
        Mockito.when(im.getGlobalContext()).thenReturn(context);
        Mockito.when(im.getFactory()).thenReturn(factory);
        Mockito.when(im.getInstanceName()).thenReturn("an.instance");

        desc = (ComponentTypeDescription) Mockito.mock(ComponentTypeDescription.class);
        Mockito.when(desc.getFactory()).thenReturn(factory);
        Mockito.when(desc.getBundleContext()).thenReturn(context);

        handler = new ProvidedServiceHandler();
        handler.setFactory(factory);

        // Attach the handler
        Method method = PrimitiveHandler.class.getDeclaredMethod("attach", new Class[]{ComponentInstance.class});
        method.setAccessible(true);
        method.invoke(handler, new Object[]{im});

    }

    public void testServiceDetectionNoInterface() throws ConfigurationException {
        String classname = "org.apache.felix.ipojo.handlers.providedservice.ComponentTest";

        Element metadata = new Element("component", "");
        Element manipulation = new Element("manipulation", "");
        metadata.addAttribute(new Attribute("classname", classname));
        metadata.addElement(new Element("provides", ""));
        metadata.addElement(manipulation);
        manipulation.addAttribute(new Attribute("classname", classname));

        Mockito.when(factory.getPojoMetadata()).thenReturn(new PojoMetadata(metadata));
        Mockito.when(factory.getClassName()).thenReturn(classname);

        handler.initializeComponentFactory(desc, metadata);

        //Expected behavior: the implementation classname
        Assert.assertEquals("{org.apache.felix.ipojo.handlers.providedservice.ComponentTest}",
                metadata.getElements("provides")[0].getAttribute("specifications"));
    }

    public void testServiceDetectionSuperClass() throws ConfigurationException {
        String classname = "org.apache.felix.ipojo.handlers.providedservice.ComponentTestWithSuperClass";

        Element metadata = new Element("component", "");
        Element manipulation = new Element("manipulation", "");
        metadata.addAttribute(new Attribute("classname", classname));
        Element provides = new Element("provides", "");
        provides.addAttribute(new Attribute("specifications", "java.beans.SimpleBeanInfo"));
        metadata.addElement(provides);
        metadata.addElement(manipulation);
        manipulation.addAttribute(new Attribute("classname", classname));
        manipulation.addAttribute(new Attribute("super", "java.beans.SimpleBeanInfo"));
        Mockito.when(factory.getPojoMetadata()).thenReturn(new PojoMetadata(metadata));
        Mockito.when(factory.getClassName()).thenReturn(classname);

        handler.initializeComponentFactory(desc, metadata);

        System.out.println(metadata);

    }

    public void testServiceDetectionImplementationClass() throws ConfigurationException {
        String classname = "org.apache.felix.ipojo.handlers.providedservice.ComponentTestWithSuperClass";

        Element metadata = new Element("component", "");
        Element manipulation = new Element("manipulation", "");
        metadata.addAttribute(new Attribute("classname", classname));
        Element provides = new Element("provides", "");
        provides.addAttribute(new Attribute("specifications", classname));
        metadata.addElement(provides);
        metadata.addElement(manipulation);
        manipulation.addAttribute(new Attribute("classname", classname));
        manipulation.addAttribute(new Attribute("super", "java.beans.SimpleBeanInfo"));
        Mockito.when(factory.getPojoMetadata()).thenReturn(new PojoMetadata(metadata));
        Mockito.when(factory.getClassName()).thenReturn(classname);

        handler.initializeComponentFactory(desc, metadata);

        System.out.println(metadata);

    }

    public void testServiceDetectionSuperSuperClass() throws ConfigurationException {
        String classname = "org.apache.felix.ipojo.handlers.providedservice.ComponentTestWithAnotherSuperClass";

        Element metadata = new Element("component", "");
        Element manipulation = new Element("manipulation", "");
        metadata.addAttribute(new Attribute("classname", classname));
        Element provides = new Element("provides", "");
        provides.addAttribute(new Attribute("specifications", "java.beans.FeatureDescriptor"));
        metadata.addElement(provides);
        metadata.addElement(manipulation);
        manipulation.addAttribute(new Attribute("classname", classname));
        manipulation.addAttribute(new Attribute("super", "java.beans.MethodDescriptor"));

        Mockito.when(factory.getPojoMetadata()).thenReturn(new PojoMetadata(metadata));
        Mockito.when(factory.getClassName()).thenReturn(classname);

        handler.initializeComponentFactory(desc, metadata);

        System.out.println(metadata);

    }

    public void testWhenRequiresFilterIsPropagated() throws Exception {
        Dictionary dictionary = new Properties();
        Dictionary requiresfilter = new Properties();
        requiresfilter.put("id1", "(filter1)");
        dictionary.put("requires.filter", requiresfilter);


        ProvidedService providedService = new ProvidedService(handler, new String[] {Runnable.class.getName()}, ProvidedService.SINGLETON_STRATEGY, null, new Properties());
        Assert.assertEquals(2, providedService.getProperties().length); // instance.name, service.pid
        providedService.addProperties(dictionary);

        Assert.assertEquals(2 + 1, providedService.getProperties().length);
    }

}
