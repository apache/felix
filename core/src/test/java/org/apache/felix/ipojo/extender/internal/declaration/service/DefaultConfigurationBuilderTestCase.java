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

import org.apache.felix.ipojo.extender.InstanceDeclaration;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.BundleContext;

import junit.framework.TestCase;

/**
 * User: guillaume
 * Date: 13/02/2014
 * Time: 11:33
 */
public class DefaultConfigurationBuilderTestCase extends TestCase {

    @Mock
    private BundleContext m_bundleContext;
    private DefaultInstanceBuilder parent;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        parent = new DefaultInstanceBuilder(m_bundleContext, "type");
    }

    public void testPropertyAddition() throws Exception {
        DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder(parent);
        builder.property("a", "b");
        InstanceDeclaration declaration = (InstanceDeclaration) builder.build();
        assertEquals(declaration.getConfiguration().get("a"), "b");
    }

    public void testPropertyAdditionReUse() throws Exception {
        DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder(parent);
        builder.property("a", "b");

        // First built instance
        InstanceDeclaration declaration = (InstanceDeclaration) builder.build();
        assertEquals(declaration.getConfiguration().get("a"), "b");

        // Second built instance
        builder.property("c", "d");
        InstanceDeclaration declaration2 = (InstanceDeclaration) builder.build();
        assertEquals(declaration2.getConfiguration().get("a"), "b");
        assertEquals(declaration2.getConfiguration().get("c"), "d");

        // Verify that first instance is not modified
        assertNull(declaration.getConfiguration().get("c"));

    }

    public void testPropertyRemoval() throws Exception {
        DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder(parent);
        builder.property("a", "b");

        InstanceDeclaration declaration = (InstanceDeclaration) builder.build();
        assertEquals(declaration.getConfiguration().get("a"), "b");

        builder.remove("a");

        InstanceDeclaration declaration2 = (InstanceDeclaration) builder.build();
        assertNull(declaration2.getConfiguration().get("a"));

    }

    public void testClear() throws Exception {
        DefaultConfigurationBuilder builder = new DefaultConfigurationBuilder(parent);
        builder.property("a", "b");
        builder.property("c", "d");

        InstanceDeclaration declaration = (InstanceDeclaration) builder.build();
        assertEquals(declaration.getConfiguration().get("a"), "b");

        builder.clear();

        InstanceDeclaration declaration2 = (InstanceDeclaration) builder.build();
        assertTrue(declaration2.getConfiguration().isEmpty());

    }
}
