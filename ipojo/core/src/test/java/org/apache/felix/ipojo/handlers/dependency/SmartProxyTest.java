package org.apache.felix.ipojo.handlers.dependency;

import java.awt.Window;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;

import javax.swing.Action;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.util.Logger;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

public class SmartProxyTest extends TestCase {

	public void setUp() {
	}


	/**
	 * Check that we don't create smart proxies for concrete and abstract classes.
	 */
	public void testCannotProxyAbstractAndContreteClasses() {
		Bundle bundle = new BundleImplementingLoading();

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
		Bundle bundle = new BundleImplementingLoading();

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

	private class BundleImplementingLoading implements Bundle {
		public int getState() {
			return 0;
		}

		public void start() throws BundleException {
		}

		public void stop() throws BundleException {
		}

		public void update() throws BundleException {
		}

		public void update(InputStream in) throws BundleException {
		}

		public void uninstall() throws BundleException {
		}

		public Dictionary getHeaders() {
			return null;
		}

		public long getBundleId() {
			return 0;
		}

		public String getLocation() {
			return null;
		}

		public ServiceReference[] getRegisteredServices() {
			return null;
		}

		public ServiceReference[] getServicesInUse() {
			return null;
		}

		public boolean hasPermission(Object permission) {
			return false;
		}

		public URL getResource(String name) {
			return null;
		}

		public Dictionary getHeaders(String locale) {
			return null;
		}

		public String getSymbolicName() {
			return null;
		}

		public Class loadClass(String name) throws ClassNotFoundException {
			return Dependency.class.getClassLoader().loadClass(name);
		}

		public Enumeration getResources(String name) throws IOException {
			return null;
		}

		public Enumeration getEntryPaths(String path) {
			return null;
		}

		public URL getEntry(String name) {
			return null;
		}

		public long getLastModified() {
			return 0;
		}

		public Enumeration findEntries(String path, String filePattern,
				boolean recurse) {
			return null;
		}
	}

}
