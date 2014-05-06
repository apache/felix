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

import junit.framework.TestCase;
import org.apache.felix.ipojo.manipulator.util.Streams;
import org.objectweb.asm.*;
import org.osgi.framework.BundleContext;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClassCheckerTestCase extends TestCase {

    public void testIsAlreadyManipulatedWithNotManipulatedResource() throws Exception {
        ClassChecker checker = check(resource("test/SimplePojo.class"));
        assertFalse(checker.isAlreadyManipulated());
    }

    public void testIsAlreadyManipulatedWithManipulatedResource() throws Exception {
        ClassChecker checker = check(manipulate(resource("test/SimplePojo.class")));
        assertTrue(checker.isAlreadyManipulated());
    }

    public void testMetadataForAlreadyManipulatedClassAreCleaned() throws Exception {
        ClassChecker checker = check(manipulate(resource("test/AnnotatedComponent.class")));

        // Check implemented interfaces
        List<String> interfaces = checker.getInterfaces();
        assertTrue(interfaces.isEmpty());

        // Check super class
        assertNull(checker.getSuperClass());

        // Check inner classes
        Collection<String> inner = checker.getInnerClasses();
        assertTrue(inner.isEmpty());

        // Ensure fields are correctly filtered
        Map<String, String> fields = checker.getFields();
        assertEquals(1, fields.size());
        assertEquals("java.lang.String", fields.get("prop"));

        // Ensure methods are also correctly filtered
        List<MethodDescriptor> descriptors = checker.getMethods();
        assertEquals(2, descriptors.size());

        // AnnotatedComponent(BundleContext)
        MethodDescriptor constructor = searchMethod("$init", descriptors);
        assertNotNull(constructor);
        Type[] arguments = Type.getArgumentTypes(constructor.getDescriptor());
        assertEquals(1, arguments.length);
        assertEquals(Type.getType(BundleContext.class), arguments[0]);

        // @FakeAnnotation
        // AnnotatedComponent.annotatedMethod():Void
        MethodDescriptor method = searchMethod("annotatedMethod", descriptors);
        assertNotNull(method);
        assertEquals("()V", method.getDescriptor()); // return void + no params
        assertAnnotationIsAlone(method, "Ltest/FakeAnnotation;");


    }

    private void assertAnnotationIsAlone(MethodDescriptor method, String desc) {
        List<ClassChecker.AnnotationDescriptor> annotations = method.getAnnotations();

        assertEquals(1, annotations.size());
        ClassChecker.AnnotationDescriptor annotationDescriptor = annotations.get(0);
        MethodVisitor mv = mock(MethodVisitor.class);
        when(mv.visitAnnotation(desc, true)).thenReturn(new AnnotationVisitor(Opcodes.ASM5) {});
        annotationDescriptor.visitAnnotation(mv);
    }

    private MethodDescriptor searchMethod(String methodName, List<MethodDescriptor> descriptors) {
        for (MethodDescriptor descriptor : descriptors) {
            if (methodName.equals(descriptor.getName())) {
                return descriptor;
            }
        }

        return null;
    }

    private byte[] manipulate(byte[] input) throws Exception {
        Manipulator manipulator = new Manipulator(this.getClass().getClassLoader());
        manipulator.prepare(input);
        return manipulator.manipulate(input);
    }

    private byte[] resource(String name) throws Exception {
        return ManipulatorTest.getBytesFromFile(new File("target/test-classes/" + name));
    }

    private ClassChecker check(byte[] resource) throws Exception {
        ClassChecker checker = new ClassChecker();
        ByteArrayInputStream is = new ByteArrayInputStream(resource);
        try {
            ClassReader classReader = new ClassReader(is);
            classReader.accept(checker, ClassReader.SKIP_FRAMES);
        } finally {
            Streams.close(is);
        }
        return checker;
    }
}
