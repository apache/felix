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

package org.apache.felix.ipojo.manipulation;

import junit.framework.Assert;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.Pojo;
import org.apache.felix.ipojo.metadata.Element;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Callable;

import static junit.framework.Assert.assertEquals;
import static org.fest.assertions.Assertions.assertThat;

/**
 * Checks the inner class manipulation
 */
public class InnerClassAdapterTest {

    public static String baseClassDirectory = "target/test-classes/";

    public static ManipulatedClassLoader manipulate(String className, Manipulator manipulator) throws IOException {
        final File mainClassFile = new File(baseClassDirectory + className.replace(".", "/") + ".class");
        byte[] bytecode = ManipulatorTest.getBytesFromFile(
                mainClassFile);

        // Preparation.
        try {
            manipulator.prepare(bytecode);
        } catch (IOException e) {
            Assert.fail("Cannot read " + className);
        }

        // Inner class preparation
        for (String inner : manipulator.getInnerClasses()) {
            // Get the bytecode and start manipulation
            String resourcePath = inner + ".class";
            byte[] innerClassBytecode;
            try {
                innerClassBytecode = ManipulatorTest.getBytesFromFile(new File(baseClassDirectory + resourcePath));
                manipulator.prepareInnerClass(inner, innerClassBytecode);
            } catch (IOException e) {
                Assert.fail("Cannot find or analyze inner class '" + resourcePath + "'");
            }
        }

        // Now manipulate the classes.
        byte[] out = new byte[0];
        try {
            out = manipulator.manipulate(bytecode);
        } catch (IOException e) {
            Assert.fail("Cannot manipulate the class " + className + " : " + e.getMessage());
        }

        ManipulatedClassLoader classloader = new ManipulatedClassLoader(className, out);

        // Visit inner classes
        for (String inner : manipulator.getInnerClasses()) {
            // Get the bytecode and start manipulation
            String resourcePath = inner + ".class";
            byte[] innerClassBytecode;
            try {
                innerClassBytecode = ManipulatorTest.getBytesFromFile(new File(baseClassDirectory + resourcePath));
                byte[] manipulated = manipulator.manipulateInnerClass(inner, innerClassBytecode);
                classloader.addInnerClass(inner.replace("/", "."), manipulated);
            } catch (IOException e) {
                Assert.fail("Cannot find inner class '" + resourcePath + "'");
            }
        }

        // Lookup for all the other inner classes (not manipulated)
        File[] files = mainClassFile.getParentFile().listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(mainClassFile.getName().substring(0, mainClassFile.getName().length() -
                        ".class".length()) + "$");
            }
        });

        for (File f : files) {
            String name = className + f.getName().substring(f.getName().indexOf("$"));
            name = name.substring(0, name.length() - ".class".length());
            byte[] innerClassBytecode = ManipulatorTest.getBytesFromFile(f);
            classloader.addInnerClassIfNotAlreadyDefined(name, innerClassBytecode);
        }

        return classloader;
    }

    public static ManipulatedClassLoader manipulate(String className, Manipulator manipulator,
                                                    ManipulatedClassLoader initial) throws IOException {
        byte[] bytecode = initial.get(className);
        final File mainClassFile = new File(baseClassDirectory + className.replace(".", "/") + ".class");
        String mainClass = className;

        // Preparation.
        try {
            manipulator.prepare(bytecode);
        } catch (IOException e) {
            Assert.fail("Cannot read " + className);
        }

        // Inner class preparation
        for (String inner : manipulator.getInnerClasses()) {
            // Get the bytecode and start manipulation
            String resourcePath = inner + ".class";
            byte[] innerClassBytecode;
            try {
                innerClassBytecode = initial.get(inner.replace("/", "."));
                manipulator.prepareInnerClass(inner, innerClassBytecode);
            } catch (IOException e) {
                Assert.fail("Cannot find or analyze inner class '" + resourcePath + "'");
            }
        }

        // Now manipulate the classes.
        byte[] out = new byte[0];
        try {
            out = manipulator.manipulate(bytecode);
        } catch (IOException e) {
            Assert.fail("Cannot manipulate the class " + className + " : " + e.getMessage());
        }

        ManipulatedClassLoader classloader = new ManipulatedClassLoader(className, out);

        // Visit inner classes
        for (String inner : manipulator.getInnerClasses()) {
            // Get the bytecode and start manipulation
            String resourcePath = inner + ".class";
            byte[] innerClassBytecode;
            try {
                innerClassBytecode = initial.get(inner.replace("/", "."));
                byte[] manipulated = manipulator.manipulateInnerClass(inner, innerClassBytecode);
                classloader.addInnerClass(inner.replace("/", "."), manipulated);
            } catch (IOException e) {
                Assert.fail("Cannot find inner class '" + resourcePath + "'");
            }
        }

        // Lookup for all the other inner classes (not manipulated)
        File[] files = mainClassFile.getParentFile().listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(mainClassFile.getName().substring(0, mainClassFile.getName().length() -
                        ".class".length()) + "$");
            }
        });


        for (File f : files) {
            String name = className + f.getName().substring(f.getName().indexOf("$"));
            name = name.substring(0, name.length() - ".class".length());
            byte[] innerClassBytecode = ManipulatorTest.getBytesFromFile(f);
            classloader.addInnerClassIfNotAlreadyDefined(name, innerClassBytecode);
        }

        return classloader;
    }

    private static Element getInnerClassMetadataByName(Element[] inners, String name) {
        for (Element element : inners) {
            if (name.equals(element.getAttribute("name"))) {
                return element;
            }
        }
        return null;
    }

    private static Element getMethodByName(Element[] methods, String name) {
        for (Element element : methods) {
            if (name.equals(element.getAttribute("name"))) {
                return element;
            }
        }
        return null;
    }

    @Test
    public void testManipulatingTheInner() throws Exception {
        Manipulator manipulator = new Manipulator(this.getClass().getClassLoader());
        String className = "test.PojoWithInner";
        byte[] origin = ManipulatorTest.getBytesFromFile(new File(baseClassDirectory + className.replace(".",
                "/") + ".class"));

        ManipulatedClassLoader classloader = manipulate(className, manipulator);

        Class cl = classloader.findClass(className);
        Assert.assertNotNull(cl);
        Assert.assertNotNull(manipulator.getManipulationMetadata());
        Assert.assertFalse(manipulator.getInnerClasses().isEmpty());

        System.out.println(manipulator.getManipulationMetadata());

        // The manipulation add stuff to the class.
        Assert.assertTrue(classloader.get(className).length > origin.length);


        boolean found = false;
        Constructor cst = null;
        Constructor[] csts = cl.getDeclaredConstructors();
        for (Constructor cst2 : csts) {
            System.out.println(Arrays.asList(cst2.getParameterTypes()));
            if (cst2.getParameterTypes().length == 1 &&
                    cst2.getParameterTypes()[0].equals(InstanceManager.class)) {
                found = true;
                cst = cst2;
            }
        }
        Assert.assertTrue(found);

        // We still have the empty constructor
        found = false;
        csts = cl.getDeclaredConstructors();
        for (Constructor cst1 : csts) {
            System.out.println(Arrays.asList(cst1.getParameterTypes()));
            if (cst1.getParameterTypes().length == 0) {
                found = true;
            }
        }
        Assert.assertTrue(found);

        // Check the POJO interface
        Assert.assertTrue(Arrays.asList(cl.getInterfaces()).contains(Pojo.class));

        InstanceManager im = Mockito.mock(InstanceManager.class);
        cst.setAccessible(true);
        Object pojo = cst.newInstance(new Object[]{im});
        Assert.assertNotNull(pojo);
        Assert.assertTrue(pojo instanceof Pojo);
        Method method = cl.getMethod("doSomething", new Class[0]);
        Assert.assertTrue(((Boolean) method.invoke(pojo, new Object[0])).booleanValue());

    }

    @Test
    public void testInnerClasses() throws IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Manipulator manipulator = new Manipulator(this.getClass().getClassLoader());
        String className = "test.inner.ComponentWithInnerClasses";
        ManipulatedClassLoader classloader = manipulate(className, manipulator);


        Class clazz = classloader.findClass(className);
        Assert.assertNotNull(clazz);
        Assert.assertNotNull(manipulator.getManipulationMetadata());
        Assert.assertFalse(manipulator.getInnerClasses().isEmpty());
        // We should have found only 2 inner classes.
        assertThat(manipulator.getInnerClasses().size()).isEqualTo(3);

        // Check that all inner classes are manipulated.
        InstanceManager im = Mockito.mock(InstanceManager.class);
        Constructor constructor = clazz.getDeclaredConstructor(InstanceManager.class);
        constructor.setAccessible(true);
        Object pojo = constructor.newInstance(im);
        Assert.assertNotNull(pojo);
        Assert.assertTrue(pojo instanceof Pojo);
        Method method = clazz.getMethod("doSomething", new Class[0]);
        String result = (String) method.invoke(pojo);
        assertEquals(result, "foofoofoofoo");
    }

    @Test
    public void testDoubleManipulation() throws IOException, ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        Manipulator manipulator = new Manipulator(this.getClass().getClassLoader());
        String className = "test.inner.ComponentWithInnerClasses";
        ManipulatedClassLoader classloader = manipulate(className, manipulator);

        manipulator = new Manipulator(this.getClass().getClassLoader());
        classloader = manipulate(className, manipulator, classloader);

        Class clazz = classloader.findClass(className);
        Assert.assertNotNull(clazz);
        Assert.assertNotNull(manipulator.getManipulationMetadata());
        Assert.assertFalse(manipulator.getInnerClasses().isEmpty());
        // We should have found only 2 inner classes.
        assertThat(manipulator.getInnerClasses().size()).isEqualTo(3);

        // Check that all inner classes are manipulated.
        InstanceManager im = Mockito.mock(InstanceManager.class);
        Constructor constructor = clazz.getDeclaredConstructor(InstanceManager.class);
        constructor.setAccessible(true);
        Object pojo = constructor.newInstance(im);
        Assert.assertNotNull(pojo);
        Assert.assertTrue(pojo instanceof Pojo);
        Method method = clazz.getMethod("doSomething", new Class[0]);
        String result = (String) method.invoke(pojo);
        assertEquals(result, "foofoofoofoo");
    }

    @Test
    public void testThatManipulationMetadataContainsTheInnerClasses() throws IOException, ClassNotFoundException,
            NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Manipulator manipulator = new Manipulator(this.getClass().getClassLoader());
        String className = "test.inner.ComponentWithInnerClasses";
        manipulate(className, manipulator);

        assertThat(manipulator.getInnerClasses().size()).isEqualTo(3);

        Element manipulation = manipulator.getManipulationMetadata();
        System.out.println(manipulation);
        Element[] inners = manipulation.getElements("inner");
        assertThat(inners.length).isEqualTo(3);

        Element inner = getInnerClassMetadataByName(inners, "MyInnerWithANativeMethod");
        assertThat(inner).isNotNull();
        assertThat(getMethodByName(inner.getElements("method"), "foo")).isNotNull();

        inner = getInnerClassMetadataByName(inners, "MyInnerClass");
        assertThat(inner).isNotNull();
        assertThat(getMethodByName(inner.getElements("method"), "foo")).isNotNull();

        inner = getInnerClassMetadataByName(inners, "1");
        assertThat(inner).isNotNull();
        assertThat(getMethodByName(inner.getElements("method"), "compute")).isNotNull();
    }

    @Test
    public void testThatTheClassContainsTheFlagsForTheInnerMethods() throws IOException, ClassNotFoundException,
            NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException {
        Manipulator manipulator = new Manipulator(this.getClass().getClassLoader());
        String className = "test.inner.ComponentWithInnerClasses";
        ManipulatedClassLoader classLoader = manipulate(className, manipulator);

        Class clazz = classLoader.findClass(className);

        String flag = "__M" + "MyInnerWithANativeMethod" + "___" + "foo";
        assertThat(clazz.getDeclaredField(flag)).isNotNull();

        flag = "__M" + "MyInnerClass" + "___" + "foo";
        assertThat(clazz.getDeclaredField(flag)).isNotNull();

        flag = "__M" + "1" + "___" + "compute" + "$java_lang_String";
        assertThat(clazz.getDeclaredField(flag)).isNotNull();
    }

    @Test
    public void testThatStaticInnerClassesAreNotManipulated() throws Exception {
        Manipulator manipulator = new Manipulator(this.getClass().getClassLoader());
        String className = "test.inner.ComponentWithInnerClasses";
        ManipulatedClassLoader classLoader = manipulate(className, manipulator);

        Class clazz = classLoader.findClass(className);
        Class inner = findInnerClass(clazz.getClasses(), "MyStaticInnerClass");
        assertThat(inner).isNotNull();
        Method bar = inner.getMethod("bar");
        Object o = inner.newInstance();
        bar.setAccessible(true);
        assertThat(bar).isNotNull();
        assertThat((String) bar.invoke(o)).isEqualTo("bar");
    }

    @Test
    public void testThatAnonymousClassDeclaredInStaticFieldsAreNotManipulated() throws Exception {
        Manipulator manipulator = new Manipulator(this.getClass().getClassLoader());
        String className = "test.inner.ComponentWithInnerClasses";
        ManipulatedClassLoader classLoader = manipulate(className, manipulator);

        Class clazz = classLoader.findClass(className);
        Method method = clazz.getMethod("call");
        assertThat(method).isNotNull();
        assertThat(method.invoke(null)).isEqualTo(1);
    }

    private Class findInnerClass(Class[] classes, String name) {
        for (Class clazz : classes) {
            if (clazz.getName().contains(name)) {
                return clazz;
            }
        }
        return null;
    }

}
