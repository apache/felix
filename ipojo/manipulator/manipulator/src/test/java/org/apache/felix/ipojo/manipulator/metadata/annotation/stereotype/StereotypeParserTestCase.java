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

package org.apache.felix.ipojo.manipulator.metadata.annotation.stereotype;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.net.URL;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Stereotype;
import org.apache.felix.ipojo.manipulator.metadata.annotation.stereotype.replay.RootAnnotationRecorder;
import org.apache.felix.ipojo.manipulator.util.Streams;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

import junit.framework.TestCase;

/**
 * User: guillaume
 * Date: 30/05/13
 * Time: 23:51
 */
public class StereotypeParserTestCase extends TestCase {
    public static final String COMPONENT_DESC = Type.getType(Component.class).getDescriptor();
    public static final String INSTANTIATE_DESC = Type.getType(Instantiate.class).getDescriptor();
    @Mock
    private AnnotationVisitor av;

    @Mock
    private ClassVisitor cv;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testStereotypeParsing() throws Exception {
        StereotypeParser parser = new StereotypeParser();
        parser.read(resource(IsStereotype.class));
        assertTrue(parser.isStereotype());
    }

    public void testStereotypeParsingWithAnnotations() throws Exception {

        when(cv.visitAnnotation(COMPONENT_DESC, false))
                .thenReturn(av);
        when(cv.visitAnnotation(INSTANTIATE_DESC, false))
                .thenReturn(av);

        StereotypeParser parser = new StereotypeParser();
        parser.read(resource(ComponentInstanceStereotype.class));
        assertTrue(parser.isStereotype());
        assertEquals(2, parser.getRecorders().size());

        RootAnnotationRecorder rec1 = parser.getRecorders().get(0);
        rec1.accept(cv);
        RootAnnotationRecorder rec2 = parser.getRecorders().get(1);
        rec2.accept(cv);

        InOrder order = inOrder(cv, av);
        order.verify(cv).visitAnnotation(COMPONENT_DESC, false);
        order.verify(av).visit("immediate", Boolean.TRUE);
        order.verify(av).visitEnd();
        order.verify(cv).visitAnnotation(INSTANTIATE_DESC, false);
        order.verify(av).visitEnd();
    }

    public void testNoStereotypeParsing() throws Exception {
        StereotypeParser parser = new StereotypeParser();
        parser.read(resource(NoStereotype.class));
        assertFalse(parser.isStereotype());
    }

    private byte[] resource(final Class<?> aClass) throws Exception {
        String name = aClass.getDeclaringClass().getSimpleName() +
                "$" + aClass.getSimpleName()
                + ".class";
        URL url = aClass.getResource(name);
        return Streams.readBytes(url.openStream());
    }

    @Stereotype
    public static @interface IsStereotype {}

    public static @interface NoStereotype {}

    @Component(immediate = true)
    @Instantiate
    @Stereotype
    public static @interface ComponentInstanceStereotype {}

}
