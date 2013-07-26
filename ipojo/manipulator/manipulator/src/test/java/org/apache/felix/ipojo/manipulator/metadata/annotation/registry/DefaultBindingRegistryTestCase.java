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
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.spi.AnnotationVisitorFactory;
import org.apache.felix.ipojo.manipulator.spi.Predicate;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.objectweb.asm.Type;

import java.util.Collections;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: guillaume
 * Date: 10/11/12
 * Time: 10:31 AM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultBindingRegistryTestCase extends TestCase {

    public static final Type PROVIDES_TYPE = Type.getType(Provides.class);
    private BindingRegistry registry;

    @Mock
    private Reporter reporter;
    @Mock
    private AnnotationVisitorFactory factory;
    @Mock
    private Predicate predicate;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        registry = new DefaultBindingRegistry(reporter);
    }

    public void testBindingAddition() throws Exception {
        registry.addBindings(Collections.singletonList(binding()));

        List<Binding> predicates = registry.getBindings(Type.getType(Provides.class).getDescriptor());

        assertEquals(1, predicates.size());
        Binding found = predicates.get(0);
        assertNotNull(found);
        assertEquals(predicate, found.getPredicate());
        assertEquals(factory, found.getFactory());
    }

    public void testGetBindingsWhenEmpty() throws Exception {
        assertTrue(registry.getBindings(PROVIDES_TYPE.getDescriptor()).isEmpty());
    }

    private Binding binding() {
        Binding binding = new Binding();
        binding.setAnnotationType(PROVIDES_TYPE);
        binding.setFactory(factory);
        binding.setPredicate(predicate);
        return binding;
    }
}
