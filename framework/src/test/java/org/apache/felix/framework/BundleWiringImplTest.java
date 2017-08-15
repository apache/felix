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

import org.apache.felix.framework.BundleWiringImpl.BundleClassLoader;
import org.apache.felix.framework.cache.Content;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.weaving.WeavingException;
import org.osgi.framework.hooks.weaving.WeavingHook;
import org.osgi.framework.hooks.weaving.WovenClass;
import org.osgi.framework.hooks.weaving.WovenClassListener;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BundleWiringImplTest
{

    private BundleWiringImpl bundleWiring;

    private StatefulResolver mockResolver;

    private BundleRevisionImpl mockRevisionImpl;

    private BundleImpl mockBundle;

    @SuppressWarnings("rawtypes")
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
        Map<String, List<BundleRevision>> requiredPkgs = new HashMap<String, List<BundleRevision>>();

        when(mockRevisionImpl.getBundle()).thenReturn(mockBundle);
        when(mockBundle.getBundleId()).thenReturn(Long.valueOf(1));

        bundleWiring = new BundleWiringImpl(logger, configMap, mockResolver,
                mockRevisionImpl, fragments, wires, importedPkgs, requiredPkgs);
    }

    @Test
    public void testBundleClassLoader() throws Exception
    {
        bundleWiring = mock(BundleWiringImpl.class);
        BundleClassLoader bundleClassLoader = createBundleClassLoader(
                BundleClassLoader.class, bundleWiring);
        assertNotNull(bundleClassLoader);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testFindClassNonExistant() throws Exception
    {
        initializeSimpleBundleWiring();

        BundleClassLoader bundleClassLoader = createBundleClassLoader(
                BundleClassLoader.class, bundleWiring);
        assertNotNull(bundleClassLoader);
        Class foundClass = null;
        try
        {
            foundClass = bundleClassLoader
                    .findClass("org.apache.felix.test.NonExistant");
        } catch (ClassNotFoundException e)
        {
            fail("Class should not throw exception");
        }
        assertNull("Nonexistant Class Should be null", foundClass);
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testFindClassExistant() throws Exception
    {
        Felix mockFramework = mock(Felix.class);
        HookRegistry hReg = mock(HookRegistry.class);
        Mockito.when(mockFramework.getHookRegistry()).thenReturn(hReg);
        Content mockContent = mock(Content.class);
        Class testClass = TestClass.class;
        String testClassName = testClass.getName();
        String testClassAsPath = testClassName.replace('.', '/') + ".class";
        byte[] testClassBytes = createTestClassBytes(testClass, testClassAsPath);

        List<Content> contentPath = new ArrayList<Content>();
        contentPath.add(mockContent);
        initializeSimpleBundleWiring();

        when(mockBundle.getFramework()).thenReturn(mockFramework);
        when(mockFramework.getBootPackages()).thenReturn(new String[0]);

        when(mockRevisionImpl.getContentPath()).thenReturn(contentPath);
        when(mockContent.getEntryAsBytes(testClassAsPath)).thenReturn(
                testClassBytes);

        BundleClassLoader bundleClassLoader = createBundleClassLoader(
                BundleClassLoader.class, bundleWiring);
        assertNotNull(bundleClassLoader);
        Class foundClass = null;
        try
        {

            foundClass = bundleClassLoader.findClass(TestClass.class.getName());
        } catch (ClassNotFoundException e)
        {
            fail("Class should not throw exception");
        }
        assertNotNull("Class Should be found in this classloader", foundClass);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testFindClassWeave() throws Exception
    {
        Felix mockFramework = mock(Felix.class);
        Content mockContent = mock(Content.class);
        ServiceReference<WeavingHook> mockServiceReferenceWeavingHook = mock(ServiceReference.class);
        ServiceReference<WovenClassListener> mockServiceReferenceWovenClassListener = mock(ServiceReference.class);

        Set<ServiceReference<WeavingHook>> hooks = new HashSet<ServiceReference<WeavingHook>>();
        hooks.add(mockServiceReferenceWeavingHook);

        DummyWovenClassListener dummyWovenClassListener = new DummyWovenClassListener();

        Set<ServiceReference<WovenClassListener>> listeners = new HashSet<ServiceReference<WovenClassListener>>();
        listeners.add(mockServiceReferenceWovenClassListener);

        Class testClass = TestClass.class;
        String testClassName = testClass.getName();
        String testClassAsPath = testClassName.replace('.', '/') + ".class";
        byte[] testClassBytes = createTestClassBytes(testClass, testClassAsPath);

        List<Content> contentPath = new ArrayList<Content>();
        contentPath.add(mockContent);
        initializeSimpleBundleWiring();

        when(mockBundle.getFramework()).thenReturn(mockFramework);
        when(mockFramework.getBootPackages()).thenReturn(new String[0]);

        when(mockRevisionImpl.getContentPath()).thenReturn(contentPath);
        when(mockContent.getEntryAsBytes(testClassAsPath)).thenReturn(
                testClassBytes);

        HookRegistry hReg = mock(HookRegistry.class);
        when(hReg.getHooks(WeavingHook.class)).thenReturn(hooks);
        when(mockFramework.getHookRegistry()).thenReturn(hReg);
        when(
                mockFramework.getService(mockFramework,
                        mockServiceReferenceWeavingHook, false)).thenReturn(
                                new GoodDummyWovenHook());

        when(hReg.getHooks(WovenClassListener.class)).thenReturn(
                listeners);
        when(
                mockFramework.getService(mockFramework,
                        mockServiceReferenceWovenClassListener, false))
        .thenReturn(dummyWovenClassListener);

        BundleClassLoader bundleClassLoader = createBundleClassLoader(
                BundleClassLoader.class, bundleWiring);
        assertNotNull(bundleClassLoader);
        Class foundClass = null;
        try
        {

            foundClass = bundleClassLoader.findClass(TestClass.class.getName());
        } catch (ClassNotFoundException e)
        {
            fail("Class should not throw exception");
        }
        assertNotNull("Class Should be found in this classloader", foundClass);
        assertEquals("Weaving should have added a field", 1,
                foundClass.getFields().length);
        assertEquals("There should be 2 state changes fired by the weaving", 2,
                dummyWovenClassListener.stateList.size());
        assertEquals("The first state change should transform the class",
                (Object)WovenClass.TRANSFORMED,
                dummyWovenClassListener.stateList.get(0));
        assertEquals("The second state change should define the class",
                (Object)WovenClass.DEFINED, dummyWovenClassListener.stateList.get(1));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testFindClassBadWeave() throws Exception
    {
        Felix mockFramework = mock(Felix.class);
        Content mockContent = mock(Content.class);
        ServiceReference<WeavingHook> mockServiceReferenceWeavingHook = mock(ServiceReference.class);
        ServiceReference<WovenClassListener> mockServiceReferenceWovenClassListener = mock(ServiceReference.class);

        Set<ServiceReference<WeavingHook>> hooks = new HashSet<ServiceReference<WeavingHook>>();
        hooks.add(mockServiceReferenceWeavingHook);

        DummyWovenClassListener dummyWovenClassListener = new DummyWovenClassListener();

        Set<ServiceReference<WovenClassListener>> listeners = new HashSet<ServiceReference<WovenClassListener>>();
        listeners.add(mockServiceReferenceWovenClassListener);

        Class testClass = TestClass.class;
        String testClassName = testClass.getName();
        String testClassAsPath = testClassName.replace('.', '/') + ".class";
        byte[] testClassBytes = createTestClassBytes(testClass, testClassAsPath);

        List<Content> contentPath = new ArrayList<Content>();
        contentPath.add(mockContent);
        initializeSimpleBundleWiring();

        when(mockBundle.getFramework()).thenReturn(mockFramework);
        when(mockFramework.getBootPackages()).thenReturn(new String[0]);

        when(mockRevisionImpl.getContentPath()).thenReturn(contentPath);
        when(mockContent.getEntryAsBytes(testClassAsPath)).thenReturn(
                testClassBytes);

        HookRegistry hReg = mock(HookRegistry.class);
        when(hReg.getHooks(WeavingHook.class)).thenReturn(hooks);
        when(mockFramework.getHookRegistry()).thenReturn(hReg);
        when(
                mockFramework.getService(mockFramework,
                        mockServiceReferenceWeavingHook, false)).thenReturn(
                                new BadDummyWovenHook());

        when(hReg.getHooks(WovenClassListener.class)).thenReturn(
                listeners);
        when(
                mockFramework.getService(mockFramework,
                        mockServiceReferenceWovenClassListener, false))
        .thenReturn(dummyWovenClassListener);

        BundleClassLoader bundleClassLoader = createBundleClassLoader(
                BundleClassLoader.class, bundleWiring);
        assertNotNull(bundleClassLoader);

        try
        {

            bundleClassLoader.findClass(TestClass.class.getName());
            fail("Class should throw exception");
        } catch (Error e)
        {
            // This is expected
        }

        assertEquals("There should be 1 state changes fired by the weaving", 1,
                dummyWovenClassListener.stateList.size());
        assertEquals(
                "The only state change should be a failed transform on the class",
                (Object)WovenClass.TRANSFORMING_FAILED,
                dummyWovenClassListener.stateList.get(0));

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void testFindClassWeaveDefineError() throws Exception
    {
        Felix mockFramework = mock(Felix.class);
        Content mockContent = mock(Content.class);
        ServiceReference<WeavingHook> mockServiceReferenceWeavingHook = mock(ServiceReference.class);
        ServiceReference<WovenClassListener> mockServiceReferenceWovenClassListener = mock(ServiceReference.class);

        Set<ServiceReference<WeavingHook>> hooks = new HashSet<ServiceReference<WeavingHook>>();
        hooks.add(mockServiceReferenceWeavingHook);

        DummyWovenClassListener dummyWovenClassListener = new DummyWovenClassListener();

        Set<ServiceReference<WovenClassListener>> listeners = new HashSet<ServiceReference<WovenClassListener>>();
        listeners.add(mockServiceReferenceWovenClassListener);

        Class testClass = TestClass.class;
        String testClassName = testClass.getName();
        String testClassAsPath = testClassName.replace('.', '/') + ".class";
        byte[] testClassBytes = createTestClassBytes(testClass, testClassAsPath);

        List<Content> contentPath = new ArrayList<Content>();
        contentPath.add(mockContent);
        initializeSimpleBundleWiring();

        when(mockBundle.getFramework()).thenReturn(mockFramework);
        when(mockFramework.getBootPackages()).thenReturn(new String[0]);

        when(mockRevisionImpl.getContentPath()).thenReturn(contentPath);
        when(mockContent.getEntryAsBytes(testClassAsPath)).thenReturn(
                testClassBytes);

        HookRegistry hReg = mock(HookRegistry.class);
        when(hReg.getHooks(WeavingHook.class)).thenReturn(hooks);
        when(mockFramework.getHookRegistry()).thenReturn(hReg);
        when(
                mockFramework.getService(mockFramework,
                        mockServiceReferenceWeavingHook, false)).thenReturn(
                                new BadDefineWovenHook());

        when(hReg.getHooks(WovenClassListener.class)).thenReturn(
                listeners);
        when(
                mockFramework.getService(mockFramework,
                        mockServiceReferenceWovenClassListener, false))
        .thenReturn(dummyWovenClassListener);

        BundleClassLoader bundleClassLoader = createBundleClassLoader(
                BundleClassLoader.class, bundleWiring);
        assertNotNull(bundleClassLoader);
        try
        {

            bundleClassLoader.findClass(TestClass.class.getName());
            fail("Class should throw exception");
        } catch (Throwable e)
        {

        }
        assertEquals("There should be 2 state changes fired by the weaving", 2,
                dummyWovenClassListener.stateList.size());
        assertEquals("The first state change should transform the class",
                (Object)WovenClass.TRANSFORMED,
                dummyWovenClassListener.stateList.get(0));
        assertEquals("The second state change failed the define on the class",
                (Object)WovenClass.DEFINE_FAILED,
                dummyWovenClassListener.stateList.get(1));
    }

    private ConcurrentHashMap<String, ClassLoader> getAccessorCache(BundleWiringImpl wiring) throws NoSuchFieldException, IllegalAccessException {
        Field m_accessorLookupCache = BundleWiringImpl.class.getDeclaredField("m_accessorLookupCache");
        m_accessorLookupCache.setAccessible(true);
        return (ConcurrentHashMap<String, ClassLoader>) m_accessorLookupCache.get(wiring);
    }

    @Test
    public void testFirstGeneratedAccessorSkipClassloading() throws Exception
    {

        String classToBeLoaded = "sun.reflect.GeneratedMethodAccessor21";

        Felix mockFramework = mock(Felix.class);
        when(mockFramework.getBootPackages()).thenReturn(new String[0]);

        initializeSimpleBundleWiring();

        when(bundleWiring.getBundle().getFramework()).thenReturn(mockFramework);

        BundleClassLoader bundleClassLoader = createBundleClassLoader(
                BundleClassLoader.class, bundleWiring);
        assertNotNull(bundleClassLoader);

        try {
            bundleClassLoader.loadClass(classToBeLoaded, true);
            fail();
        } catch (ClassNotFoundException cnf) {
            //this is expected

            //make sure boot delegation was done before CNF was thrown
            verify(mockFramework).getBootPackages();

            //make sure the class is added to the skip class cache
            assertEquals(getAccessorCache(bundleWiring).get(classToBeLoaded), BundleWiringImpl.CNFE_CLASS_LOADER);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @SuppressWarnings("rawtypes")
    public void initializeBundleWiringWithImportsAndRequired(Map<String, BundleRevision> importedPkgs, Map<String, List<BundleRevision>> requiredPkgs) throws Exception
    {

        mockResolver = mock(StatefulResolver.class);
        mockRevisionImpl = mock(BundleRevisionImpl.class);
        mockBundle = mock(BundleImpl.class);

        Logger logger = new Logger();
        Map configMap = new HashMap();
        List<BundleRevision> fragments = new ArrayList<BundleRevision>();
        List<BundleWire> wires = new ArrayList<BundleWire>();

        when(mockRevisionImpl.getBundle()).thenReturn(mockBundle);
        when(mockBundle.getBundleId()).thenReturn(Long.valueOf(1));

        bundleWiring = new BundleWiringImpl(logger, configMap, mockResolver,
                mockRevisionImpl, fragments, wires, importedPkgs, requiredPkgs);
    }

    @Test
    public void testAccessorFirstLoadFailed() throws Exception
    {

        String classToBeLoaded = "sun.reflect.GeneratedMethodAccessor21";

        Felix mockFramework = mock(Felix.class);
        when(mockFramework.getBootPackages()).thenReturn(new String[0]);

        Map<String, BundleRevision> importedPkgs = mock(Map.class);
        Map<String, List<BundleRevision>> requiredPkgs = mock(Map.class);

        initializeBundleWiringWithImportsAndRequired(importedPkgs, requiredPkgs);

        when(bundleWiring.getBundle().getFramework()).thenReturn(mockFramework);

        BundleClassLoader bundleClassLoader = createBundleClassLoader(
                BundleClassLoader.class, bundleWiring);
        assertNotNull(bundleClassLoader);

        try {
            bundleClassLoader.loadClass(classToBeLoaded, true);
            fail();
        } catch (ClassNotFoundException cnf) {
            //this is expected

            //make sure boot delegation was done before CNF was thrown
            verify(mockFramework).getBootPackages();

            //make sure imported and required pkgs are searched
            verify(importedPkgs).values();
            verify(requiredPkgs).values();

            //make sure the class is added to the skip class cache
            assertEquals(getAccessorCache(bundleWiring).get(classToBeLoaded), BundleWiringImpl.CNFE_CLASS_LOADER);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testAccessorSubsequentLoadFailed() throws Exception
    {

        String classToBeLoaded = "sun.reflect.GeneratedMethodAccessor21";

        Felix mockFramework = mock(Felix.class);
        when(mockFramework.getBootPackages()).thenReturn(new String[0]);

        Map<String, BundleRevision> importedPkgs = mock(Map.class);
        Map<String, List<BundleRevision>> requiredPkgs = mock(Map.class);

        initializeBundleWiringWithImportsAndRequired(importedPkgs, requiredPkgs);

        when(bundleWiring.getBundle().getFramework()).thenReturn(mockFramework);

        BundleClassLoader bundleClassLoader = createBundleClassLoader(
                BundleClassLoader.class, bundleWiring);
        assertNotNull(bundleClassLoader);

        //first attempt to populate the cache
        try {
            bundleClassLoader.loadClass(classToBeLoaded, true);
            fail();
        } catch (ClassNotFoundException cnf) {
            //this is expected
        }

        //now test that the subsequent class load throws CNF with out boot delegation and import/required packages
        try {

            importedPkgs = mock(Map.class);
            requiredPkgs = mock(Map.class);
            initializeBundleWiringWithImportsAndRequired(importedPkgs, requiredPkgs);
            mockFramework = mock(Felix.class);
            when(mockFramework.getBootPackages()).thenReturn(new String[0]);
            when(bundleWiring.getBundle().getFramework()).thenReturn(mockFramework);
            bundleClassLoader.loadClass(classToBeLoaded, true);
            fail();
        } catch (ClassNotFoundException cnf) {
            //this is expected

            //make sure boot delegation was not used
            verify(mockFramework, never()).getBootPackages();

            //make sure boot import and required packages were not searched
            verify(importedPkgs, never()).values();
            verify(requiredPkgs, never()).values();

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    private BundleRevision getBundleRevision(String classToBeLoaded, BundleClassLoader pkgBundleClassLoader, Object value) throws ClassNotFoundException {
        BundleRevision bundleRevision = mock(BundleRevision.class);
        BundleWiring pkgBundleWiring = mock(BundleWiring.class);
        when(pkgBundleClassLoader.findLoadedClassInternal(classToBeLoaded)).thenAnswer(createAnswer(value));
        when(pkgBundleClassLoader.loadClass(classToBeLoaded)).thenAnswer(createAnswer(value));

        when(pkgBundleWiring.getClassLoader()).thenReturn(pkgBundleClassLoader);
        when(bundleRevision.getWiring()).thenReturn(pkgBundleWiring);
        return bundleRevision;
    }

    @Test
    public void testAccessorLoadImportPackage() throws Exception
    {

        String classToBeLoaded = "sun.reflect.GeneratedMethodAccessor21";

        Felix mockFramework = mock(Felix.class);
        when(mockFramework.getBootPackages()).thenReturn(new String[0]);

        Map<String, BundleRevision> importedPkgs = mock(Map.class);
        BundleClassLoader foundClassLoader = mock(BundleClassLoader.class);
        BundleClassLoader notFoundClassLoader = mock(BundleClassLoader.class);
        BundleRevision bundleRevision1 = getBundleRevision(classToBeLoaded, foundClassLoader, String.class);
        BundleRevision bundleRevision2 = getBundleRevision(classToBeLoaded, notFoundClassLoader, null);
        Map<String, BundleRevision> importedPkgsActual = new HashMap<String, BundleRevision>();
        importedPkgsActual.put("sun.reflect1", bundleRevision1);
        importedPkgsActual.put("sun.reflect2", bundleRevision2);
        when(importedPkgs.values()).thenReturn(importedPkgsActual.values());
        Map<String, List<BundleRevision>> requiredPkgs = mock(Map.class);

        initializeBundleWiringWithImportsAndRequired(importedPkgs, requiredPkgs);

        when(bundleWiring.getBundle().getFramework()).thenReturn(mockFramework);

        BundleClassLoader bundleClassLoader = createBundleClassLoader(
                BundleClassLoader.class, bundleWiring);
        assertNotNull(bundleClassLoader);

        //call class load to populate the cache
        try {
            Object result = bundleClassLoader.loadClass(classToBeLoaded, true);
            assertNotNull(result);
            assertTrue(getAccessorCache(bundleWiring).containsKey(classToBeLoaded));
            assertEquals(getAccessorCache(bundleWiring).get(classToBeLoaded), foundClassLoader);
            verify(foundClassLoader, times(1)).findLoadedClassInternal(classToBeLoaded);
            verify(notFoundClassLoader, never()).findLoadedClassInternal(classToBeLoaded);
        } catch (Exception e) {
            fail();
        }

        //now make sure subsequent class load happens from cached revision
        Object result = bundleClassLoader.loadClass(classToBeLoaded, true);
        assertNotNull(result);
        //makes sure the look up cache is accessed and the class is loaded from cached revision
        verify(foundClassLoader, times(1)).findLoadedClassInternal(classToBeLoaded);
        verify(foundClassLoader, times(1)).loadClass(classToBeLoaded);
        verify(notFoundClassLoader, never()).findLoadedClassInternal(classToBeLoaded);
    }

    private static <T> Answer<T> createAnswer(final T value) {
        Answer<T> dummy = new Answer<T>() {
            @Override
            public T answer(InvocationOnMock invocation) throws Throwable {
                return value;
            }
        };
        return dummy;
    }

    @Test
    public void testAccessorBootDelegate() throws Exception
    {

        String classToBeLoaded = "sun.reflect.GeneratedMethodAccessor21";

        Felix mockFramework = mock(Felix.class);
        when(mockFramework.getBootPackages()).thenReturn(new String[0]);

        Map<String, BundleRevision> importedPkgs = mock(Map.class);
        BundleRevision bundleRevision1 = mock(BundleRevision.class);
        Map<String, BundleRevision> importedPkgsActual = new HashMap<String, BundleRevision>();
        importedPkgsActual.put("sun.reflect1", bundleRevision1);
        when(importedPkgs.values()).thenReturn(importedPkgsActual.values());
        Map<String, List<BundleRevision>> requiredPkgs = mock(Map.class);

        ClassLoader bootDelegateClassLoader = mock(ClassLoader.class);

        when(bootDelegateClassLoader.loadClass(classToBeLoaded)).thenAnswer(createAnswer(String.class));

        initializeBundleWiringWithImportsAndRequired(importedPkgs, requiredPkgs);

        when(bundleWiring.getBundle().getFramework()).thenReturn(mockFramework);

        Field field = bundleWiring.getClass().getDeclaredField("m_bootClassLoader");
        field.setAccessible(true);
        field.set(bundleWiring, bootDelegateClassLoader);

        BundleClassLoader bundleClassLoader = createBundleClassLoader(
                BundleClassLoader.class, bundleWiring);
        assertNotNull(bundleClassLoader);

        try {
            Object result = bundleClassLoader.loadClass(classToBeLoaded, true);
            assertNotNull(result);
            verify(importedPkgs, never()).values();
            verify(requiredPkgs, never()).values();
            assertTrue(getAccessorCache(bundleWiring).containsKey(classToBeLoaded));
            assertTrue(getAccessorCache(bundleWiring).get(classToBeLoaded) == bootDelegateClassLoader);
        } catch (Exception e) {
            fail();
        }

        //now make sure subsequent class loading happens from boot delegation
        Object result = bundleClassLoader.loadClass(classToBeLoaded, true);
        assertNotNull(result);
        //makes sure the look up cache is accessed and the class is loaded via boot delegation
        verify(importedPkgs, never()).values();
        verify(requiredPkgs, never()).values();
    }

    @SuppressWarnings("rawtypes")
    private byte[] createTestClassBytes(Class testClass, String testClassAsPath)
            throws IOException
    {
        InputStream testClassResourceStream = testClass.getClassLoader()
                .getResourceAsStream(testClassAsPath);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int curByte;
        while ((curByte = testClassResourceStream.read()) != -1)
        {
            baos.write(curByte);
        }
        byte[] testClassBytes = baos.toByteArray();
        return testClassBytes;
    }

    @SuppressWarnings("rawtypes")
    private BundleClassLoader createBundleClassLoader(
            Class bundleClassLoaderClass, BundleWiringImpl bundleWiring)
                    throws Exception
    {
        Logger logger = new Logger();
        Constructor ctor = BundleRevisionImpl.getSecureAction().getConstructor(
                bundleClassLoaderClass,
                new Class[] { BundleWiringImpl.class, ClassLoader.class,
                        Logger.class });
        BundleClassLoader bundleClassLoader = (BundleClassLoader) BundleRevisionImpl
                .getSecureAction().invoke(
                        ctor,
                        new Object[] { bundleWiring,
                                this.getClass().getClassLoader(), logger });
        return bundleClassLoader;
    }

    class TestClass
    {
        // An empty test class to weave.
    }

    class GoodDummyWovenHook implements WeavingHook
    {
        // Adds the awesomePublicField to a class
        @Override
        @SuppressWarnings("unchecked")
        public void weave(WovenClass wovenClass)
        {
            byte[] wovenClassBytes = wovenClass.getBytes();
            ClassNode classNode = new ClassNode();
            ClassReader reader = new ClassReader(wovenClassBytes);
            reader.accept(classNode, 0);
            classNode.fields.add(new FieldNode(Opcodes.ACC_PUBLIC,
                    "awesomePublicField", "Ljava/lang/String;", null, null));
            ClassWriter writer = new ClassWriter(reader, Opcodes.ASM4);
            classNode.accept(writer);
            wovenClass.setBytes(writer.toByteArray());
        }
    }

    class BadDefineWovenHook implements WeavingHook
    {
        // Adds the awesomePublicField twice to the class. This is bad java.
        @Override
        @SuppressWarnings("unchecked")
        public void weave(WovenClass wovenClass)
        {
            byte[] wovenClassBytes = wovenClass.getBytes();
            ClassNode classNode = new ClassNode();
            ClassReader reader = new ClassReader(wovenClassBytes);
            reader.accept(classNode, 0);
            classNode.fields.add(new FieldNode(Opcodes.ACC_PUBLIC,
                    "awesomePublicField", "Ljava/lang/String;", null, null));
            classNode.fields.add(new FieldNode(Opcodes.ACC_PUBLIC,
                    "awesomePublicField", "Ljava/lang/String;", null, null));
            ClassWriter writer = new ClassWriter(reader, Opcodes.ASM4);
            classNode.accept(writer);
            wovenClass.setBytes(writer.toByteArray());
        }
    }

    class BadDummyWovenHook implements WeavingHook
    {
        // Just Blow up
        @Override
        public void weave(WovenClass wovenClass)
        {
            throw new WeavingException("Bad Weaver!");
        }
    }

    class DummyWovenClassListener implements WovenClassListener
    {
        public List<Integer> stateList = new ArrayList<Integer>();

        @Override
        public void modified(WovenClass wovenClass)
        {
            stateList.add(wovenClass.getState());
        }
    }
}
