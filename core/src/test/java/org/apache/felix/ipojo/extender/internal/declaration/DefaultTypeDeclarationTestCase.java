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

import junit.framework.TestCase;
import org.apache.felix.ipojo.IPojoFactory;
import org.apache.felix.ipojo.extender.ExtensionDeclaration;
import org.apache.felix.ipojo.extender.TypeDeclaration;
import org.apache.felix.ipojo.extender.builder.FactoryBuilder;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Checks the behavior of {@link TypeDeclaration}.
 */
public class DefaultTypeDeclarationTestCase extends TestCase {

    @Mock
    private BundleContext m_bundleContext;
    @Mock
    private Filter filter;
    @Mock
    private ServiceReference extensionReference;
    @Mock
    private ExtensionDeclaration m_extension;
    @Mock
    private FactoryBuilder m_builder;
    @Mock
    private IPojoFactory factory;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testRegistration() throws Exception {
        when(m_bundleContext.createFilter(anyString())).thenReturn(filter);

        DefaultTypeDeclaration declaration = new DefaultTypeDeclaration(m_bundleContext, element("component", "component.Hello"));
        declaration.start();

        // Declaration is not bound
        assertFalse(declaration.getStatus().isBound());

        // Verify service registration
        verify(m_bundleContext).registerService(TypeDeclaration.class.getName(), declaration, null);

    }

    private Element element(String type, String name) {
        Element root = new Element(type, null);
        root.addAttribute(new Attribute("name", name));
        return root;
    }
}
