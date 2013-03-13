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

package org.apache.felix.ipojo.manipulator.spi;

import junit.framework.TestCase;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.manipulator.metadata.annotation.registry.Binding;

import java.lang.annotation.ElementType;
import java.util.Iterator;

import static org.apache.felix.ipojo.manipulator.spi.helper.Predicates.on;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;

/**
 * Created with IntelliJ IDEA.
 * User: guillaume
 * Date: 10/10/12
 * Time: 10:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class AbsBindingModuleTestCase extends TestCase {

    public void testSimpleBinding() throws Exception {
        final AnnotationVisitorFactory factory = mock(AnnotationVisitorFactory.class);
        AbsBindingModule module = new AbsBindingModule() {
            public void configure() {
                bind(Provides.class).to(factory);
            }
        };
        module.configure();

        Iterator<Binding> i = module.iterator();
        Binding one = i.next();
        assertNotNull(one);
        assertEquals(Provides.class, one.getAnnotationType());
        assertEquals(factory, one.getFactory());

        // Only 1 Binding
        assertFalse(i.hasNext());
    }

    public void testTwoBindings() throws Exception {
        final AnnotationVisitorFactory factory = mock(AnnotationVisitorFactory.class);
        AbsBindingModule module = new AbsBindingModule() {
            public void configure() {
                bind(Provides.class).to(factory);
                bind(Requires.class).to(factory);
            }
        };
        module.configure();

        Iterator<Binding> i = module.iterator();
        Binding one = i.next();
        assertNotNull(one);
        assertEquals(Provides.class, one.getAnnotationType());
        assertEquals(factory, one.getFactory());

        // Second Binding
        Binding two = i.next();
        assertNotNull(two);
        assertEquals(Requires.class, two.getAnnotationType());
        assertEquals(factory, two.getFactory());
    }

    public void testTwoBindingsForSameAnnotation() throws Exception {
        final AnnotationVisitorFactory factory = mock(AnnotationVisitorFactory.class);
        final AnnotationVisitorFactory factory2 = mock(AnnotationVisitorFactory.class);
        AbsBindingModule module = new AbsBindingModule() {
            public void configure() {
                bind(Provides.class).to(factory);
                bind(Provides.class).to(factory2);
            }
        };
        module.configure();

        Iterator<Binding> i = module.iterator();
        Binding one = i.next();
        assertNotNull(one);
        assertEquals(Provides.class, one.getAnnotationType());
        assertEquals(factory, one.getFactory());

        // Second Binding
        Binding two = i.next();
        assertNotNull(two);
        assertEquals(Provides.class, two.getAnnotationType());
        assertEquals(factory2, two.getFactory());
    }

    public void testConditionalBinding() throws Exception {
        final AnnotationVisitorFactory factory = mock(AnnotationVisitorFactory.class);
        AbsBindingModule module = new AbsBindingModule() {
            public void configure() {
                bind(Provides.class)
                        .when(on(ElementType.FIELD))
                        .to(factory);
            }
        };
        module.configure();

        Iterator<Binding> i = module.iterator();
        Binding one = i.next();
        assertNotNull(one);
        assertEquals(Provides.class, one.getAnnotationType());
        assertEquals(factory, one.getFactory());

        // Only 1 Binding
        assertFalse(i.hasNext());
    }

    public void testConditionalBindings() throws Exception {
        final AnnotationVisitorFactory factory = mock(AnnotationVisitorFactory.class);
        final AnnotationVisitorFactory factory2 = mock(AnnotationVisitorFactory.class);
        AbsBindingModule module = new AbsBindingModule() {
            public void configure() {
                bind(Provides.class)
                        .when(on(ElementType.FIELD))
                        .to(factory)
                .when(on(ElementType.PARAMETER))
                .to(factory2);
            }
        };
        module.configure();

        Iterator<Binding> i = module.iterator();
        Binding one = i.next();
        assertNotNull(one);
        assertEquals(Provides.class, one.getAnnotationType());
        assertEquals(factory, one.getFactory());

        // Second Binding
        Binding two = i.next();
        assertNotNull(two);
        assertEquals(Provides.class, two.getAnnotationType());
        assertEquals(factory2, two.getFactory());
    }


}
