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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.objectweb.asm.Type;

import junit.framework.TestCase;

/**
 * User: guillaume
 * Date: 11/07/13
 * Time: 17:07
 */
public class LegacyGenericBindingRegistryTestCase extends TestCase {

    public static final String AN_IGNORED_ANNOTATION = "[Lan.ignored.Annotation;";
    public static final String RECOGNISED_IPOJO_ANNOTATION = "[La.recognised.ipojo.Annotation;";
    public static final String RECOGNISED_HANDLER_ANNOTATION = "[La.recognised.handler.Annotation;";

    @Mock
    private BindingRegistry delegate;

    @Captor
    ArgumentCaptor<List<Binding>> capture;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testNonRecognisedTypeAreIgnored() throws Exception {
        when(delegate.getBindings(AN_IGNORED_ANNOTATION)).thenReturn(Collections.<Binding>emptyList());

        LegacyGenericBindingRegistry registry = new LegacyGenericBindingRegistry(delegate, null);

        List<Binding> bindings = registry.getBindings(AN_IGNORED_ANNOTATION);
        assertTrue(bindings.isEmpty());

        verify(delegate).addBindings(capture.capture());
        assertTrue(capture.getValue().isEmpty());
    }

    public void testIPojoRecognisedTypeAreSupported() throws Exception {
        when(delegate.getBindings(RECOGNISED_IPOJO_ANNOTATION)).thenReturn(Collections.<Binding>emptyList());

        LegacyGenericBindingRegistry registry = new LegacyGenericBindingRegistry(delegate, null);

        List<Binding> bindings = registry.getBindings(RECOGNISED_IPOJO_ANNOTATION);
        assertEquals(1, bindings.size());

        verify(delegate).addBindings(capture.capture());
        Binding one = capture.getValue().get(0);
        assertEquals(Type.getType(RECOGNISED_IPOJO_ANNOTATION), one.getAnnotationType());
    }

    public void testHandlerRecognisedTypeAreSupported() throws Exception {
        when(delegate.getBindings(RECOGNISED_HANDLER_ANNOTATION)).thenReturn(Collections.<Binding>emptyList());

        LegacyGenericBindingRegistry registry = new LegacyGenericBindingRegistry(delegate, null);

        List<Binding> bindings = registry.getBindings(RECOGNISED_HANDLER_ANNOTATION);
        assertEquals(1, bindings.size());

        verify(delegate).addBindings(capture.capture());
        Binding one = capture.getValue().get(0);
        assertEquals(Type.getType(RECOGNISED_HANDLER_ANNOTATION), one.getAnnotationType());
    }
}
