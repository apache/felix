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

import java.util.Dictionary;

import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.extender.InstanceBuilder;
import org.apache.felix.ipojo.extender.DeclarationHandle;
import org.apache.felix.ipojo.extender.InstanceDeclaration;
import org.apache.felix.ipojo.extender.internal.declaration.DefaultInstanceDeclaration;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;

import junit.framework.TestCase;

/**
 * User: guillaume
 * Date: 13/02/2014
 * Time: 10:32
 */
public class DefaultInstanceBuilderTestCase extends TestCase {

    @Mock
    private BundleContext m_bundleContext;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testNoConfiguration() throws Exception {
        InstanceBuilder builder = new DefaultInstanceBuilder(m_bundleContext, "type");
        DeclarationHandle handle = builder.build();
        InstanceDeclaration did = (InstanceDeclaration) handle;

        assertEquals("type", did.getComponentName());
        assertEquals(InstanceDeclaration.UNNAMED_INSTANCE, did.getInstanceName());
        assertNull(did.getComponentVersion());

        Dictionary<String,Object> configuration = did.getConfiguration();
        assertTrue(configuration.isEmpty());
    }

    public void testNameConfiguration() throws Exception {
        InstanceBuilder builder = new DefaultInstanceBuilder(m_bundleContext, "type").name("John");

        DeclarationHandle handle = builder.build();
        InstanceDeclaration did = (InstanceDeclaration) handle;

        assertEquals("type", did.getComponentName());
        assertEquals("John", did.getInstanceName());
        assertNull(did.getComponentVersion());

        Dictionary<String,Object> configuration = did.getConfiguration();
        assertEquals("John", configuration.get(Factory.INSTANCE_NAME_PROPERTY));
    }

    public void testVersionConfiguration() throws Exception {
        InstanceBuilder builder = new DefaultInstanceBuilder(m_bundleContext, "type").name("John").version("1.0");

        DeclarationHandle handle = builder.build();
        InstanceDeclaration did = (InstanceDeclaration) handle;

        assertEquals("type", did.getComponentName());
        assertEquals("John", did.getInstanceName());
        assertEquals("1.0", did.getComponentVersion());

        Dictionary<String,Object> configuration = did.getConfiguration();
        assertEquals("John", configuration.get(Factory.INSTANCE_NAME_PROPERTY));
        assertEquals("1.0", configuration.get(Factory.FACTORY_VERSION_PROPERTY));
    }

    public void testBuilderReUseProvidesDifferentInstances() throws Exception {
        InstanceBuilder builder = new DefaultInstanceBuilder(m_bundleContext, "type");
        assertNotSame(builder.build(), builder.build());
    }


    public void testDeclarationIsNotAutomaticallyStarted() throws Exception {
        InstanceBuilder builder = new DefaultInstanceBuilder(m_bundleContext, "type");
        DeclarationHandle handle = builder.build();
        DefaultInstanceDeclaration did = (DefaultInstanceDeclaration) handle;

        assertFalse(did.isRegistered());
    }

    public void testDeepConfiguration() throws Exception {
        InstanceBuilder builder = new DefaultInstanceBuilder(m_bundleContext, "type");
        assertNotNull(builder.configure());
    }
}

