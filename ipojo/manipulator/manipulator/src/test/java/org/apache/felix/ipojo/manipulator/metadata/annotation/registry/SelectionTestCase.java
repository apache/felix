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

import junit.framework.TestCase;
import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.ResourceStore;
import org.apache.felix.ipojo.manipulator.spi.AbsBindingModule;
import org.apache.felix.ipojo.manipulator.spi.AnnotationVisitorFactory;
import org.apache.felix.ipojo.manipulator.spi.BindingContext;
import org.hamcrest.Matcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: guillaume
 * Date: 10/11/12
 * Time: 10:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class SelectionTestCase extends TestCase {
    private BindingRegistry registry;

    @Mock
    private Reporter reporter;
    @Mock
    private AnnotationVisitorFactory factory;
    @Mock
    private AnnotationVisitor visitor;
    @Mock
    private ResourceStore store;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        registry = new DefaultBindingRegistry(reporter);
        when(factory.newAnnotationVisitor(any(BindingContext.class)))
                .thenReturn(visitor);
        // Simulate a resource not found exception
        when(store.read(anyString()))
                .thenThrow(new IOException());
    }

    public void testSelectionOnClassNodeOnly() throws Exception {

        AbsBindingModule module = new MonoBindingModule(OnTypeOnly.class);
        module.load();
        registry.addBindings(module);

        // Verifications
        assertClassSelection(OnTypeOnly.class, equalTo(visitor));
        assertFieldSelection(OnTypeOnly.class, nullValue());
        assertMethodSelection(OnTypeOnly.class, nullValue());
        assertParameterSelection(OnTypeOnly.class, nullValue());
    }

    public void testSelectionOnFieldNodeOnly() throws Exception {

        AbsBindingModule module = new MonoBindingModule(OnFieldOnly.class);
        module.load();
        registry.addBindings(module);

        // Verifications
        assertClassSelection(OnFieldOnly.class, nullValue());
        assertFieldSelection(OnFieldOnly.class, equalTo(visitor));
        assertMethodSelection(OnFieldOnly.class, nullValue());
        assertParameterSelection(OnFieldOnly.class, nullValue());

    }

    public void testSelectionOnMethodNodeOnly() throws Exception {

        AbsBindingModule module = new MonoBindingModule(OnMethodOnly.class);
        module.load();
        registry.addBindings(module);

        // Verifications
        assertClassSelection(OnMethodOnly.class, nullValue());
        assertFieldSelection(OnMethodOnly.class, nullValue());
        assertMethodSelection(OnMethodOnly.class, equalTo(visitor));
        assertParameterSelection(OnMethodOnly.class, nullValue());

    }

    public void testSelectionOnMethodParameterOnly() throws Exception {

        AbsBindingModule module = new MonoBindingModule(OnParameterOnly.class);
        module.load();
        registry.addBindings(module);

        // Verifications
        assertClassSelection(OnParameterOnly.class, nullValue());
        assertFieldSelection(OnParameterOnly.class, nullValue());
        assertMethodSelection(OnParameterOnly.class, nullValue());
        assertParameterSelection(OnParameterOnly.class, equalTo(visitor));

    }

    public void testSelectionOBothMethodAndParameter() throws Exception {

        AbsBindingModule module = new MonoBindingModule(OnBothMethodAndParameter.class);
        module.load();
        registry.addBindings(module);

        // Verifications
        assertClassSelection(OnBothMethodAndParameter.class, nullValue());
        assertFieldSelection(OnBothMethodAndParameter.class, nullValue());
        assertMethodSelection(OnBothMethodAndParameter.class, equalTo(visitor));
        assertParameterSelection(OnBothMethodAndParameter.class, equalTo(visitor));

    }

    private void assertClassSelection(Class<? extends Annotation> type, Matcher matcher) {
        Selection selection = new Selection(registry, null, reporter);
        selection.type(null, classNode());
        selection.annotatedWith(descriptor(type));

        assertTrue(matcher.matches(selection.get()));
    }

    private void assertFieldSelection(Class<? extends Annotation> type, Matcher matcher) {
        Selection selection = new Selection(registry, null, reporter);
        selection.field(null, fieldNode());
        selection.annotatedWith(descriptor(type));

        assertTrue(matcher.matches(selection.get()));
    }

    private void assertMethodSelection(Class<? extends Annotation> type, Matcher matcher) {
        Selection selection = new Selection(registry, null, reporter);
        selection.method(null, methodNode());
        selection.annotatedWith(descriptor(type));

        assertTrue(matcher.matches(selection.get()));
    }

    private void assertParameterSelection(Class<? extends Annotation> type, Matcher matcher) {
        Selection selection = new Selection(registry, null, reporter);
        selection.parameter(null, methodNode(), 0);
        selection.annotatedWith(descriptor(type));

        assertTrue(matcher.matches(selection.get()));
    }

    private MethodNode methodNode() {
        return new MethodNode(0, "method", "(java/lang/String)V", null, null);
    }

    private ClassNode classNode() {
        ClassNode node = new ClassNode();
        node.visit(0, 0, "my/Component", null, "java/lang/Object", null);
        return node;
    }

    public void testSelectionWithEmptyRegistry() throws Exception {
        Selection selection = new Selection(registry, null, reporter);

        selection.field(null, fieldNode())
                .annotatedWith(descriptor(OnTypeOnly.class));

        assertNull(selection.get());
    }

    private String descriptor(Class<? extends Annotation> type) {
        return Type.getType(type).getDescriptor();
    }

    private FieldNode fieldNode() {
        return new FieldNode(0,
                             "field",
                             Type.getType(Object.class).getDescriptor(),
                             null,
                             null);
    }

    @Target(ElementType.TYPE)
    private @interface OnTypeOnly {}

    @Target(ElementType.FIELD)
    private @interface OnFieldOnly {}

    @Target(ElementType.METHOD)
    private @interface OnMethodOnly {}

    @Target(ElementType.PARAMETER)
    private @interface OnParameterOnly {}

    @Target({ElementType.PARAMETER, ElementType.METHOD})
    private @interface OnBothMethodAndParameter {}


    private class MonoBindingModule extends AbsBindingModule {
        private Class<? extends Annotation> type;

        public MonoBindingModule(Class<? extends Annotation> aClass) {
            this.type = aClass;
        }

        public void configure() {
            bind(type).to(factory);
        }
    }
}
