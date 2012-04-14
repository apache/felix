package org.apache.felix.ipojo.handlers.dependency;

import java.awt.Window;
import java.util.List;

import javax.swing.Action;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.test.MockBundle;
import org.apache.felix.ipojo.util.Logger;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class SmartProxyTest extends TestCase {

    public void setUp() {
    }


    /**
     * Check that we don't create smart proxies for concrete and abstract classes.
     */
    public void testCannotProxyAbstractAndConcreteClasses() {
        Bundle bundle = new MockBundle(Dependency.class.getClassLoader());

        BundleContext context = (BundleContext) Mockito.mock(BundleContext.class);
        Mockito.when(context.getProperty(DependencyHandler.PROXY_TYPE_PROPERTY)).thenReturn(null);
        Mockito.when(context.getProperty(Logger.IPOJO_LOG_LEVEL_PROP)).thenReturn(null);
        Mockito.when(context.getBundle()).thenReturn(bundle);

        ComponentFactory factory = (ComponentFactory) Mockito.mock(ComponentFactory.class);
        Mockito.when(factory.getBundleClassLoader()).thenReturn(Dependency.class.getClassLoader());

        InstanceManager im = (InstanceManager) Mockito.mock(InstanceManager.class);
        Mockito.when(im.getContext()).thenReturn(context);
        Mockito.when(im.getFactory()).thenReturn(factory);

        DependencyHandler handler = (DependencyHandler) Mockito.mock(DependencyHandler.class);
        Mockito.when(handler.getInstanceManager()).thenReturn(im);
        Logger logger = new Logger(context, "test", Logger.INFO);


        Mockito.when(handler.getLogger()).thenReturn(logger);

        // Try with java.List
        Dependency dependency = new Dependency(handler, "a_field", Window.class, null, false, false, false,
                true, "dep", context, Dependency.DYNAMIC_BINDING_POLICY, null, null);
        dependency.start();

        // No service
        Assert.assertNull(dependency.onGet(new Object(), "a_field", null));

        dependency.stop();

        // Try with javax.swing.Action
        dependency = new Dependency(handler, "a_field", Object.class, null, false, false, false,
                true, "dep", context, Dependency.DYNAMIC_BINDING_POLICY, null, null);
        dependency.start();
        // OK
        Assert.assertNull(dependency.onGet(new Object(), "a_field", null));
    }

    /**
     * Tests if we can proxies classes from java.* package.
     * Indeed, a recent JVM bug fix introduces a bug:
     * <code>
     * [ERROR] test : Cannot create the proxy object
     * java.lang.SecurityException: Prohibited package name: java.awt
     * </code>
     */
    public void testProxiesOfJavaClasses() {
        Bundle bundle = new MockBundle(Dependency.class.getClassLoader());

        BundleContext context = (BundleContext) Mockito.mock(BundleContext.class);
        Mockito.when(context.getProperty(DependencyHandler.PROXY_TYPE_PROPERTY)).thenReturn(null);
        Mockito.when(context.getProperty(Logger.IPOJO_LOG_LEVEL_PROP)).thenReturn(null);
        Mockito.when(context.getBundle()).thenReturn(bundle);

        ComponentFactory factory = (ComponentFactory) Mockito.mock(ComponentFactory.class);
        Mockito.when(factory.getBundleClassLoader()).thenReturn(Dependency.class.getClassLoader());

        InstanceManager im = (InstanceManager) Mockito.mock(InstanceManager.class);
        Mockito.when(im.getContext()).thenReturn(context);
        Mockito.when(im.getFactory()).thenReturn(factory);

        DependencyHandler handler = (DependencyHandler) Mockito.mock(DependencyHandler.class);
        Mockito.when(handler.getInstanceManager()).thenReturn(im);
        Logger logger = new Logger(context, "test", Logger.INFO);


        Mockito.when(handler.getLogger()).thenReturn(logger);

        // Try with java.List
        Dependency dependency = new Dependency(handler, "a_field", List.class, null, false, false, false,
                true, "dep", context, Dependency.DYNAMIC_BINDING_POLICY, null, null);
        dependency.start();

        // OK
        Assert.assertNotNull(dependency.onGet(new Object(), "a_field", null));
        Assert.assertTrue(dependency.onGet(new Object(), "a_field", null) instanceof List);

        dependency.stop();

        // Try with javax.swing.Action
        dependency = new Dependency(handler, "a_field", Action.class, null, false, false, false,
                true, "dep", context, Dependency.DYNAMIC_BINDING_POLICY, null, null);
        dependency.start();
        // OK
        Assert.assertNotNull(dependency.onGet(new Object(), "a_field", null));
        Assert.assertTrue(dependency.onGet(new Object(), "a_field", null) instanceof Action);
    }

}
