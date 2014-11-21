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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.framework.BundleWiringImpl.BundleClassLoader;
import org.apache.felix.framework.BundleWiringImpl.BundleClassLoaderJava5;
import org.apache.felix.framework.cache.Content;

import org.junit.Test;

import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;

public class BundleWiringImplTest 
{

	private BundleWiringImpl bundleWiring;
	
	private StatefulResolver mockResolver;
	
	private BundleRevisionImpl mockRevisionImpl;
	
	private BundleImpl mockBundle;
	
	public void initializeSimpleBundleWiring() throws Exception
	{
		
		mockResolver = mock(StatefulResolver.class);
		mockRevisionImpl = mock(BundleRevisionImpl.class);
		mockBundle = mock(BundleImpl.class);
		
		Logger logger = new Logger();
		Map configMap = new HashMap();
		List<BundleRevision> fragments = new ArrayList<BundleRevision>();
		List<BundleWire> wires = new ArrayList<BundleWire>();
		Map<String, BundleRevision> importedPkgs = new HashMap<String, BundleRevision>();
		Map<String, List<BundleRevision>> requiredPkgs =
				new HashMap<String, List<BundleRevision>>();
		
		when(mockRevisionImpl.getBundle()).thenReturn(mockBundle);
		when(mockBundle.getBundleId()).thenReturn(Long.valueOf(1));
		
		bundleWiring = new BundleWiringImpl(logger, configMap, mockResolver, mockRevisionImpl,
				fragments, wires, importedPkgs, requiredPkgs);
	}

	@Test
	public void testBundleClassLoader() throws Exception
	{
		bundleWiring = mock(BundleWiringImpl.class);
		BundleClassLoader bundleClassLoader = createBundleClassLoader(BundleClassLoader.class, bundleWiring);
		assertNotNull(bundleClassLoader);
	}
	
	@Test
	public void testBundleClassLoaderJava5() throws Exception
	{
		bundleWiring = mock(BundleWiringImpl.class);
		BundleClassLoader bundleClassLoader = createBundleClassLoader(BundleClassLoaderJava5.class, bundleWiring);
		assertNotNull(bundleClassLoader);
	}
	
	@Test
	public void testFindClassNonExistant() throws Exception
	{
		initializeSimpleBundleWiring();
		
		BundleClassLoader bundleClassLoader = createBundleClassLoader(BundleClassLoaderJava5.class, bundleWiring);
		assertNotNull(bundleClassLoader);
		Class foundClass = null;
		try {
			foundClass = bundleClassLoader.findClass("org.apache.felix.test.NonExistant");
		} 
		catch (ClassNotFoundException e) 
		{
			fail("Class should not throw exception");
		}
		assertNull("Nonexistant Class Should be null", foundClass);
	}
	
	@Test
	public void testFindClassExistant() throws Exception
	{
		Felix mockFramework = mock(Felix.class);
		Content mockContent = mock(Content.class);
		Class testClass = TestClass.class;
		String testClassName = testClass.getName();
		String testClassAsPath = testClassName.replace('.', '/') + ".class";
		InputStream testClassResourceStream = 
				testClass.getClassLoader().getResourceAsStream(testClassAsPath);
		
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int curByte;
		while((curByte = testClassResourceStream.read()) != -1)
		{
			baos.write(curByte);
		}
		byte[] testClassBytes = baos.toByteArray();
		
		List<Content> contentPath = new ArrayList<Content>();
		contentPath.add(mockContent);
		initializeSimpleBundleWiring();
		
		when(mockBundle.getFramework()).thenReturn(mockFramework);
		when(mockFramework.getBootPackages()).thenReturn(new String[0]);
		
		when(mockRevisionImpl.getContentPath()).thenReturn(contentPath);
		when(mockContent.getEntryAsBytes(testClassAsPath)).thenReturn(testClassBytes);
		
		BundleClassLoader bundleClassLoader = createBundleClassLoader(BundleClassLoaderJava5.class, bundleWiring);
		assertNotNull(bundleClassLoader);
		Class foundClass = null;
		try {
			
			foundClass = bundleClassLoader.findClass(TestClass.class.getName());
		} 
		catch (ClassNotFoundException e) 
		{
			fail("Class should not throw exception");
		}
		assertNotNull("Class Should be found in this classloader", foundClass);
	}
	
	
	private BundleClassLoader createBundleClassLoader(Class bundleClassLoaderClass, BundleWiringImpl bundleWiring) throws Exception
	{
		Logger logger = new Logger();
		Constructor ctor = BundleRevisionImpl.getSecureAction()
                .getConstructor(bundleClassLoaderClass, new Class[] { BundleWiringImpl.class, ClassLoader.class, Logger.class });
		BundleClassLoader bundleClassLoader = (BundleClassLoader)
                BundleRevisionImpl.getSecureAction().invoke(ctor,
                new Object[] { bundleWiring, this.getClass().getClassLoader(), logger });
        return bundleClassLoader;
	}
	
	class TestClass{
		
	}
}
