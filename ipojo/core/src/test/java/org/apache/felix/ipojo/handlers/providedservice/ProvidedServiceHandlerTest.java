package org.apache.felix.ipojo.handlers.providedservice;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.handlers.dependency.DependencyHandler;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.PojoMetadata;
import org.apache.felix.ipojo.test.MockBundle;
import org.apache.felix.ipojo.util.Logger;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;

public class ProvidedServiceHandlerTest extends TestCase {

    BundleContext context;
    ComponentFactory factory;
    InstanceManager im;
    ComponentTypeDescription desc;
    ProvidedServiceHandler handler;

    public void setUp() {
        context = (BundleContext) Mockito.mock(BundleContext.class);
        Mockito.when(context.getProperty(DependencyHandler.PROXY_TYPE_PROPERTY)).thenReturn(null);
        Mockito.when(context.getProperty(Logger.IPOJO_LOG_LEVEL_PROP)).thenReturn(null);
        Mockito.when(context.getBundle()).thenReturn(new MockBundle(this.getClass().getClassLoader()));

        factory = (ComponentFactory) Mockito.mock(ComponentFactory.class);
        Mockito.when(factory.getBundleClassLoader()).thenReturn(ProvidedServiceHandler.class.getClassLoader());
        Mockito.when(factory.getLogger()).thenReturn(new Logger(context, "TEST", Logger.INFO));

        im = (InstanceManager) Mockito.mock(InstanceManager.class);
        Mockito.when(im.getContext()).thenReturn(context);
        Mockito.when(im.getFactory()).thenReturn(factory);

        desc = (ComponentTypeDescription) Mockito.mock(ComponentTypeDescription.class);
        Mockito.when(desc.getFactory()).thenReturn(factory);
        Mockito.when(desc.getBundleContext()).thenReturn(context);

        handler = new ProvidedServiceHandler();
        handler.setFactory(factory);
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

}
