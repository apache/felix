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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.framework.BundleWiringImpl.BundleClassLoader;
import org.apache.felix.framework.cache.Content;
import org.junit.Test;
import org.mockito.Mockito;
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
