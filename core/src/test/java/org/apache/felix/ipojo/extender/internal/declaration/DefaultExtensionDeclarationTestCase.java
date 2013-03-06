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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Hashtable;

import org.apache.felix.ipojo.extender.ExtensionDeclaration;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;

import junit.framework.TestCase;

/**
 * Checks the behavior of the devault extension declaration.
 */
public class DefaultExtensionDeclarationTestCase extends TestCase {

    @Mock
    private BundleContext m_bundleContext;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testRegistration() throws Exception {
        m_bundleContext = mock(BundleContext.class);
        DefaultExtensionDeclaration declaration = new DefaultExtensionDeclaration(m_bundleContext, null, "component");

        // Before start, declaration is not bound
        assertFalse(declaration.getStatus().isBound());
        declaration.start();

        // After start, declaration is bound
        assertTrue(declaration.getStatus().isBound());

        // Verify service registration
        ArgumentCaptor<Hashtable> argument = ArgumentCaptor.forClass(Hashtable.class);
        verify(m_bundleContext).registerService(eq(ExtensionDeclaration.class.getName()), eq(declaration), argument.capture());
        assertEquals(argument.getValue().get(ExtensionDeclaration.EXTENSION_NAME_PROPERTY), "component");

    }
}
