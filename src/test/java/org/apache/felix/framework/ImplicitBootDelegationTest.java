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
package org.apache.felix.framework;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.junit.Assert;
import org.junit.Assume;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;

import junit.framework.Test;
import junit.framework.TestCase;

public class ImplicitBootDelegationTest extends TestCase {
	
	public void testDoesBootdelegateForClassloaderClassload() throws Exception{
		withFelixDo(new ThrowingConsumer<Felix>() {
			@Override
			public void accept(Felix felix) throws Exception {
				BundleImpl bundle = (BundleImpl) felix.getBundleContext().installBundle(createBundle(
						ImplicitBootDelegationTestActivator.class).toURI().toURL().toString());
				
				bundle.start();
				
				Runnable testClass = felix.getBundleContext().getService(
						felix.getBundleContext().getServiceReference(Runnable.class));
				
				Assert.assertEquals(TestClass.class,  
						testClass.getClass().getClassLoader().loadClass(TestClass.class.getName()));
			}
		});
	}
	
	public void testDoesNotBootdelegateForClassloadFromInsideBundle() throws Exception{
		withFelixDo(new ThrowingConsumer<Felix>() {
			@Override
			public void accept(Felix felix) throws Exception {
				BundleImpl bundle = (BundleImpl) felix.getBundleContext().installBundle(createBundle(
						ImplicitBootDelegationTestActivator.class).toURI().toURL().toString());
				
				bundle.start();
				
				Runnable testClass = felix.getBundleContext().getService(
						felix.getBundleContext().getServiceReference(Runnable.class));
				
				try
				{
					testClass.run();
					Assert.fail("Expected to not be able to load an implicit bootdelegated class from inside the bundle");
				} catch (NoClassDefFoundError ex) {
					
				}
			}
		});
	}
	
	public void testDoesNotBootdelegateForBundleClassload() throws Exception {
		withFelixDo(new ThrowingConsumer<Felix>() {
			@Override
			public void accept(Felix felix) throws Exception {
				BundleImpl bundle = (BundleImpl) felix.getBundleContext().installBundle(createBundle(
						ImplicitBootDelegationTestActivator.class).toURI().toURL().toString());
				try
				{
					bundle.loadClass(TestClass.class.getName());
					Assert.fail("Expected to not be able to bundle.loadClass an implicit bootdelegated class");
				}
				catch (ClassNotFoundException ex) {
					
				}
			}
		});
	}
	
	public void testDoesNotBootdelegateForServiceAssignability() throws Exception {
		withFelixDo(new ThrowingConsumer<Felix>() {
			@Override
			public void accept(Felix felix) throws Exception {
				BundleImpl provider = (BundleImpl) felix.getBundleContext().installBundle(createBundle(
						ProvidesActivator.class, TestClass.class).toURI().toURL().toString());
				
				provider.start();
				
				Assert.assertNotNull(felix.getBundleContext().getAllServiceReferences(TestClass.class.getName(), null));
				
				BundleImpl requirer = (BundleImpl) felix.getBundleContext().installBundle(createBundle(
						RequireActivator.class).toURI().toURL().toString());
				
				requirer.start();
				
				Runnable requirerActivtor = felix.getBundleContext().getService(
						felix.getBundleContext().getServiceReference(Runnable.class));
				
				Assume.assumeTrue(requirerActivtor.getClass().getClassLoader().loadClass(TestClass.class.getName()) 
						== TestClass.class);
				
				requirerActivtor.run();
				
				Object service = requirer.getBundleContext().getService(
						requirer.getBundleContext().getServiceReference(TestClass.class.getName()));
				
				assertNotNull(service);
				assertTrue(!(service instanceof TestClass));
				assertTrue(service.getClass().getName().equals(TestClass.class.getName()));
			}
		});
	}
	
	public static class RequireActivator implements BundleActivator, Runnable {
		private volatile BundleContext context;
		@Override
		public void start(BundleContext context) throws Exception {
			this.context = context;
			context.registerService(Runnable.class, this, null);
		}

		@Override
		public void stop(BundleContext context) throws Exception {
			// TODO Auto-generated method stub
			
		}
		
		public void run() {
			Object service = context.getService(context.getServiceReference(
					"org.apache.felix.framework.ImplicitBootDelegationTest$TestClass"));
			
			if (service == null)
			{
				throw new IllegalStateException("Expected service to be available from inside bundle");
			}
			
			ClassLoader loader = service.getClass().getClassLoader();
			if (!(service.getClass().getClassLoader() instanceof BundleReference))
			{
				throw new IllegalStateException("Expected service to be loaded from bundle");
			}
			try
			{
				getClass().getClassLoader().loadClass(service.getClass().getName());
				throw new IllegalStateException("Expected to be unable to load service class");
			}
			catch (ClassNotFoundException ex) {
				
			}
			
			try {
				TestClass test = new TestClass();
				throw new IllegalStateException("Expected to be unable to create object of type TestClass");
				
			} catch (NoClassDefFoundError ex) {
				
			}
		}
	}
	
	public static class ProvidesActivator implements BundleActivator {

		@Override
		public void start(BundleContext context) throws Exception {
			context.registerService(TestClass.class, new TestClass(), null);
		}

		@Override
		public void stop(BundleContext context) throws Exception {
			// TODO Auto-generated method stub
			
		}
		
	}

	public static class TestClass {
	}
	
	private void withFelixDo(ThrowingConsumer<Felix> consumer) throws Exception {
		File cacheDir = File.createTempFile("felix-cache", ".dir");
		try
		{
			Felix felix = getFramework(cacheDir);
			try
			{
				felix.init();
				felix.start();
				consumer.accept(felix);
			}
			finally 
			{
				felix.stop();
				felix.waitForStop(1000);
			}
		}
		finally
		{
			delete(cacheDir);
		}
	}
	
	@FunctionalInterface
	private static interface ThrowingConsumer<T> {
		public void accept(T t) throws Exception;
	}
	
	
	public static class ImplicitBootDelegationTestActivator implements BundleActivator, Runnable {

		@Override
		public void start(BundleContext context) throws Exception {
			context.registerService(Runnable.class, this, null);
		}

		@Override
		public void stop(BundleContext context) throws Exception {
		}
		
		public void run() 
		{
			new TestClass();
		}
	}
	
	private Felix getFramework(File cacheDir) {
		Map params = new HashMap();
        params.put(Constants.FRAMEWORK_SYSTEMPACKAGES,
            "org.osgi.framework; version=1.4.0,"
            + "org.osgi.service.packageadmin; version=1.2.0,"
            + "org.osgi.service.startlevel; version=1.1.0,"
            + "org.osgi.util.tracker; version=1.3.3,"
            + "org.osgi.service.url; version=1.0.0");
        cacheDir.delete();
        cacheDir.mkdirs();
        String cache = cacheDir.getPath();
        params.put("felix.cache.profiledir", cache);
        params.put("felix.cache.dir", cache);
        params.put(Constants.FRAMEWORK_STORAGE, cache);
        
        return new Felix(params);
	}
	
	private static File createBundle(Class activator, Class...classes) throws IOException
	{
		String mf = "Bundle-SymbolicName: " + activator.getName() +"\n"
                + "Bundle-Version: 1.0.0\n"
                + "Bundle-ManifestVersion: 2\n"
                + "Import-Package: org.osgi.framework\n"
                + "Manifest-Version: 1.0\n"
                + "Bundle-Activator: " + activator.getName() + "\n\n";
		
		Class[] classesCombined;
		
		if (classes.length > 0) {
			List<Class> list = new ArrayList<Class>(Arrays.asList(classes));
			list.add(activator);
			classesCombined = list.toArray(new Class[0]);
		}
		else
		{
			classesCombined = new Class[]{activator};
		}
		return createBundle(mf,classesCombined);
        
	}
	
	private static File createBundle(String manifest, Class... classes) throws IOException
    {
        File f = File.createTempFile("felix-bundle", ".jar");
        f.deleteOnExit();

        Manifest mf = new Manifest(new ByteArrayInputStream(manifest.getBytes("utf-8")));
        JarOutputStream os = new JarOutputStream(new FileOutputStream(f), mf);

        for (Class clazz : classes)
        {
            String path = clazz.getName().replace('.', '/') + ".class";
            os.putNextEntry(new ZipEntry(path));

            InputStream is = clazz.getClassLoader().getResourceAsStream(path);
            byte[] buffer = new byte[8 * 1024];
            for (int i = is.read(buffer); i != -1; i = is.read(buffer))
            {
                os.write(buffer, 0, i);
            }
            is.close();
            os.closeEntry();
        }
        os.close();
        return f;
    }
	
    private static void delete(File file) throws IOException
    {
        if (file.isDirectory())
        {
            for (File child : file.listFiles())
            {
                delete(child);
            }
        }
        file.delete();
    }
}

	
	