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

package org.apache.felix.ipojo.extender.internal.declaration;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import java.util.Hashtable;

import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.extender.InstanceDeclaration;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;

import junit.framework.TestCase;

/**
 * Checks the behavior of the default instance declaration.
 */
public class DefaultInstanceDeclarationTestCase extends TestCase {

    @Mock
    private BundleContext m_bundleContext;

    @Captor
    private ArgumentCaptor<Hashtable<String, Object>> argument;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testRegistrationWithEmptyConfiguration() throws Exception {
        DefaultInstanceDeclaration declaration = new DefaultInstanceDeclaration(m_bundleContext, "component.Hello");
        declaration.start();

        // Declaration is not bound
        assertFalse(declaration.getStatus().isBound());

        // Verify service registration
        verify(m_bundleContext).registerService(eq(InstanceDeclaration.class.getName()), eq(declaration), argument.capture());
        assertEquals(argument.getValue().get(InstanceDeclaration.COMPONENT_NAME_PROPERTY), "component.Hello");
        assertNull(argument.getValue().get(InstanceDeclaration.COMPONENT_VERSION_PROPERTY));

    }

    public void testRegistrationWithVersionedConfiguration() throws Exception {
        Hashtable<String, Object> configuration = new Hashtable<String, Object>();
        configuration.put(Factory.FACTORY_VERSION_PROPERTY, "1.0.0");
        DefaultInstanceDeclaration declaration = new DefaultInstanceDeclaration(m_bundleContext, "component.Hello", configuration);

        declaration.start();

        // Declaration is not bound
        assertFalse(declaration.getStatus().isBound());

        // Verify service registration
        verify(m_bundleContext).registerService(eq(InstanceDeclaration.class.getName()), eq(declaration), argument.capture());
        assertEquals(argument.getValue().get(InstanceDeclaration.COMPONENT_NAME_PROPERTY), "component.Hello");
        assertEquals(argument.getValue().get(InstanceDeclaration.COMPONENT_VERSION_PROPERTY), "1.0.0");

    }
}
