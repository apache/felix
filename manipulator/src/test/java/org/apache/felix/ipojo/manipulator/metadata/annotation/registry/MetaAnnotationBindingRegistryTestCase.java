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

package org.apache.felix.ipojo.manipulator.metadata.annotation.registry;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.HandlerBinding;
import org.apache.felix.ipojo.annotations.Ignore;
import org.apache.felix.ipojo.annotations.Stereotype;
import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.ResourceStore;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.generic.GenericVisitorFactory;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.ignore.NullBinding;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.ignore.NullVisitorFactory;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.stereotype.StereotypeVisitorFactory;
import org.apache.felix.ipojo.manipulator.spi.AnnotationVisitorFactory;
import org.apache.felix.ipojo.manipulator.util.Streams;
import org.apache.felix.ipojo.manipulator.util.Strings;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.objectweb.asm.Type;

import junit.framework.TestCase;

/**
 * User: guillaume
 * Date: 12/07/13
 * Time: 12:13
 */
public class MetaAnnotationBindingRegistryTestCase extends TestCase {

    public static final String DESCRIPTOR = "[Lunknown;";
    public static final Type TYPE = Type.getType(DESCRIPTOR);

    @Mock
    private BindingRegistry delegate;

    @Mock
    private ResourceStore store;

    @Mock
    private Reporter reporter;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(delegate.getBindings(DESCRIPTOR)).thenReturn(Collections.<Binding>emptyList());
    }

    public void testAnnotationTypeResourceNotFound() throws Exception {
        when(store.read(anyString()))
                .thenThrow(IOException.class);
        MetaAnnotationBindingRegistry registry = new MetaAnnotationBindingRegistry(delegate, reporter, store);
        assertTrue(registry.getBindings(DESCRIPTOR).isEmpty());
    }

    public void testClassicalAnnotation() throws Exception {
        when(store.read(anyString()))
                .thenReturn(from(Classical.class));
        MetaAnnotationBindingRegistry registry = new MetaAnnotationBindingRegistry(delegate, reporter, store);
        assertTrue(registry.getBindings(Type.getDescriptor(Classical.class)).isEmpty());
    }

    public void testStereotypeAnnotation() throws Exception {
        when(store.read(anyString()))
                .thenReturn(from(Stereotyped.class));
        MetaAnnotationBindingRegistry registry = new MetaAnnotationBindingRegistry(delegate, reporter, store);
        Binding binding = registry.getBindings(Type.getDescriptor(Stereotyped.class)).get(0);
        assertTrue(binding.getFactory() instanceof StereotypeVisitorFactory);
    }

    public void testHandlerBindingAnnotation() throws Exception {
        when(store.read(anyString()))
                .thenReturn(from(Bound.class));
        MetaAnnotationBindingRegistry registry = new MetaAnnotationBindingRegistry(delegate, reporter, store);
        Binding binding = registry.getBindings(Type.getDescriptor(Bound.class)).get(0);
        assertTrue(binding.getFactory() instanceof GenericVisitorFactory);
    }

    public void testIgnoreAnnotation() throws Exception {
        when(store.read(anyString()))
                .thenReturn(from(Ignored.class));
        MetaAnnotationBindingRegistry registry = new MetaAnnotationBindingRegistry(delegate, reporter, store);
        Binding binding = registry.getBindings(Type.getDescriptor(Ignored.class)).get(0);
        assertTrue(binding instanceof NullBinding);
    }

    public void testBothStereotypeAndBindingAnnotation() throws Exception {
        when(store.read(anyString()))
                .thenReturn(from(StereotypedBinding.class));
        MetaAnnotationBindingRegistry registry = new MetaAnnotationBindingRegistry(delegate, reporter, store);
        List<Binding> bindings = registry.getBindings(Type.getDescriptor(StereotypedBinding.class));
        assertEquals(2, bindings.size());
        assertHasVisitorFactories(bindings, StereotypeVisitorFactory.class, GenericVisitorFactory.class);
    }

    public void testIgnoredWithStereotypeAnnotation() throws Exception {
        when(store.read(anyString()))
                .thenReturn(from(StereotypedIgnoredBinding.class));
        MetaAnnotationBindingRegistry registry = new MetaAnnotationBindingRegistry(delegate, reporter, store);
        List<Binding> bindings = registry.getBindings(Type.getDescriptor(StereotypedIgnoredBinding.class));
        assertEquals(1, bindings.size());
        assertHasVisitorFactories(bindings, NullVisitorFactory.class);
    }

    private void assertHasVisitorFactories(final List<Binding> bindings,
                                           final Class<? extends AnnotationVisitorFactory>... factories) {
        List<Class<? extends AnnotationVisitorFactory>> existing = new ArrayList<Class<? extends AnnotationVisitorFactory>>();
        for (Binding binding : bindings) {
            existing.add(binding.getFactory().getClass());
        }

        for (Class<? extends AnnotationVisitorFactory> factory : factories) {
            if (!existing.remove(factory)) {
                fail("Bindings do not contains expected AnnotationVisitorFactory type:  " + factory.getName());
            }
        }
    }

    private byte[] from(Class<?> type) throws IOException {
        ClassLoader loader = type.getClassLoader();
        InputStream is = loader.getResourceAsStream(Strings.asResourcePath(type.getName()));
        return Streams.readBytes(is);
    }

    private static @interface Classical {}

    @Component
    @Stereotype
    private static @interface Stereotyped {}

    @HandlerBinding
    private static @interface Bound {}

    @Ignore
    private static @interface Ignored {}

    @Component
    @Stereotype
    @HandlerBinding
    private static @interface StereotypedBinding {}

    @Component
    @Stereotype
    @HandlerBinding
    @Ignore
    private static @interface StereotypedIgnoredBinding {}

}
