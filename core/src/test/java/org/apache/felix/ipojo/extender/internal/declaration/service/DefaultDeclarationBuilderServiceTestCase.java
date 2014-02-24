/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.extender.internal.declaration.service;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;

import java.util.Dictionary;

import org.apache.felix.ipojo.extender.InstanceBuilder;
import org.apache.felix.ipojo.extender.DeclarationHandle;
import org.apache.felix.ipojo.extender.ExtensionDeclaration;
import org.apache.felix.ipojo.extender.InstanceDeclaration;
import org.apache.felix.ipojo.extender.TypeDeclaration;
import org.apache.felix.ipojo.extender.builder.FactoryBuilder;
import org.apache.felix.ipojo.metadata.Element;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;

import junit.framework.TestCase;

/**
 * User: guillaume
 * Date: 10/02/2014
 * Time: 15:41
 */
public class DefaultDeclarationBuilderServiceTestCase extends TestCase {

    @Mock
    private BundleContext m_bundleContext;

    @Mock
    private FactoryBuilder m_factoryBuilder;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testNewInstance() throws Exception {
        DefaultDeclarationBuilderService service = new DefaultDeclarationBuilderService(m_bundleContext);
        assertNotNull(service.newInstance("type.of.component"));
        assertNotNull(service.newInstance("type.of.component", "instance.name"));
        InstanceBuilder builder = service.newInstance("type.of.component", "instance.name", "component.version");
        assertNotNull(builder);
        DeclarationHandle instance = builder.build();
        instance.publish();
        verify(m_bundleContext).registerService(
                eq(InstanceDeclaration.class.getName()),
                anyObject(),
                any(Dictionary.class));
    }

    public void testNewExtension() throws Exception {
        DefaultDeclarationBuilderService service = new DefaultDeclarationBuilderService(m_bundleContext);
        DeclarationHandle extension = service.newExtension("test", m_factoryBuilder);
        assertNotNull(extension);
        extension.publish();
        verify(m_bundleContext).registerService(
                eq(ExtensionDeclaration.class.getName()),
                anyObject(),
                any(Dictionary.class));
    }

    public void testNewType() throws Exception {
        DefaultDeclarationBuilderService service = new DefaultDeclarationBuilderService(m_bundleContext);
        DeclarationHandle type = service.newType(new Element("component", null));
        assertNotNull(type);
        type.publish();
        verify(m_bundleContext).registerService(
                eq(TypeDeclaration.class.getName()),
                anyObject(),
                isNull(Dictionary.class));
    }
}
