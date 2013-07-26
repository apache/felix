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

import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.ignore.NullBinding;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.objectweb.asm.Type;

import junit.framework.TestCase;

/**
 * User: guillaume
 * Date: 12/07/13
 * Time: 10:27
 */
public class CompletableBindingRegistryTestCase extends TestCase {

    public static final String DESCRIPTOR = "[unknown;";

    @Mock
    private BindingRegistry delegate;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testUnknownDescriptorTriggerCallback() throws Exception {
        EmptyCompletableBindingRegistry registry = new EmptyCompletableBindingRegistry(delegate);
        when(delegate.getBindings(DESCRIPTOR)).thenReturn(Collections.<Binding>emptyList());

        registry.getBindings(DESCRIPTOR);

        assertNotNull(registry.type);
    }

    public void testKnownDescriptorDoNotTriggerCallback() throws Exception {
        EmptyCompletableBindingRegistry registry = new EmptyCompletableBindingRegistry(delegate);
        when(delegate.getBindings(DESCRIPTOR)).thenReturn(Collections.<Binding>singletonList(new NullBinding(Type.getType(DESCRIPTOR))));

        registry.getBindings(DESCRIPTOR);

        assertNull(registry.type);
    }

    private static class EmptyCompletableBindingRegistry extends CompletableBindingRegistry {

        public Type type = null;

        public EmptyCompletableBindingRegistry(final BindingRegistry delegate) {
            super(delegate, null);
        }

        @Override
        protected List<Binding> createBindings(final Type type) {
            this.type = type;
            return Collections.emptyList();
        }
    }
}
