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
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import static junit.framework.Assert.assertEquals;
import static org.fest.assertions.Assertions.assertThat;

/**
 * Checks the inner class manipulation
 */
public class InnerClassAdapterTest {

    public static String baseClassDirectory = "target/test-classes/";

    private static ManipulatedClassLoader manipulate(String className, Manipulator manipulator) throws IOException {

        byte[] clazz = manipulator.manipulate(ManipulatorTest.getBytesFromFile(new File
                (baseClassDirectory + className.replace(".", "/") + ".class")));
        ManipulatedClassLoader classloader = new ManipulatedClassLoader(className, clazz);

        // Manipulate all inner classes
        for (String s : manipulator.getInnerClasses()) {
            String outerClassInternalName = className.replace(".", "/");
            byte[] innerClassBytecode = ManipulatorTest.getBytesFromFile(new File(baseClassDirectory + s + "" +
                    ".class"));
            String innerClassName = s.replace("/", ".");
            InnerClassManipulator innerManipulator = new InnerClassManipulator(s, outerClassInternalName,
                    manipulator);
            byte[] manipulated = innerManipulator.manipulate(innerClassBytecode, manipulator.getClassVersion());
            classloader.addInnerClass(innerClassName, manipulated);
        }

        return classloader;
    }

    @Test
    public void testManipulatingTheInner() throws Exception {
        Manipulator manipulator = new Manipulator();
        String className = "test.PojoWithInner";
        byte[] origin =  ManipulatorTest.getBytesFromFile(new File(baseClassDirectory + className.replace(".",
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
        for (int i = 0; i < csts.length; i++) {
            System.out.println(Arrays.asList(csts[i].getParameterTypes()));
            if (csts[i].getParameterTypes().length == 1  &&
                    csts[i].getParameterTypes()[0].equals(InstanceManager.class)) {
                found = true;
                cst = csts[i];
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
        Object pojo = cst.newInstance(new Object[] {im});
        Assert.assertNotNull(pojo);
        Assert.assertTrue(pojo instanceof Pojo);
        Method method = cl.getMethod("doSomething", new Class[0]);
        Assert.assertTrue(((Boolean) method.invoke(pojo, new Object[0])).booleanValue());

    }


    @Test
    public void testInnerClasses() throws IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Manipulator manipulator = new Manipulator();
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
        Object pojo = constructor.newInstance(new Object[] {im});
        Assert.assertNotNull(pojo);
        Assert.assertTrue(pojo instanceof Pojo);
        Method method = clazz.getMethod("doSomething", new Class[0]);
        String result = (String) method.invoke(pojo);
        assertEquals(result, "foofoofoofoo");
    }

    @Test
    public void testRemanipulationOfInnerClasses() throws IOException, ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException {
        Manipulator manipulator = new Manipulator();
        String className = "test.inner.ComponentWithInnerClasses";

        // Two manipulation of the outer class.
        byte[] bytecode = manipulator.manipulate(ManipulatorTest.getBytesFromFile(new File
                (baseClassDirectory + className.replace(".", "/") + ".class")));
        bytecode = manipulator.manipulate(bytecode);

        ManipulatedClassLoader classloader = new ManipulatedClassLoader(className, bytecode);

        // Manipulate all inner classes
        for (String s : manipulator.getInnerClasses()) {
            String outerClassInternalName = className.replace(".", "/");
            byte[] innerClassBytecode = ManipulatorTest.getBytesFromFile(new File(baseClassDirectory + s + "" +
                    ".class"));
            String innerClassName = s.replace("/", ".");
            InnerClassManipulator innerManipulator = new InnerClassManipulator(s, outerClassInternalName,
                    manipulator);
            // Two manipulation of all inner classes.
            byte[] manipulated = innerManipulator.manipulate(innerClassBytecode, manipulator.getClassVersion());
            manipulated = innerManipulator.manipulate(manipulated, manipulator.getClassVersion());
            classloader.addInnerClass(innerClassName, manipulated);
        }

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
        Object pojo = constructor.newInstance(new Object[] {im});
        Assert.assertNotNull(pojo);
        Assert.assertTrue(pojo instanceof Pojo);
        Method method = clazz.getMethod("doSomething", new Class[0]);
        String result = (String) method.invoke(pojo);
        assertEquals(result, "foofoofoofoo");
    }

    @Test
    public void testThatManipulationMetadataContainsTheInnerClasses() throws IOException, ClassNotFoundException,
            NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Manipulator manipulator = new Manipulator();
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

}
