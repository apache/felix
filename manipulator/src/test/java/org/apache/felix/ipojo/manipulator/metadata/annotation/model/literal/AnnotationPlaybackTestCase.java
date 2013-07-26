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

package org.apache.felix.ipojo.manipulator.metadata.annotation.model.literal;

import java.lang.annotation.Annotation;

import org.apache.felix.ipojo.manipulator.metadata.annotation.model.literal.AnnotationPlayback;
import org.apache.felix.ipojo.manipulator.metadata.annotation.model.literal.types.AnnotationAnnotation;
import org.apache.felix.ipojo.manipulator.metadata.annotation.model.literal.types.ArrayAnnotation;
import org.apache.felix.ipojo.manipulator.metadata.annotation.model.literal.types.EnumAnnotation;
import org.apache.felix.ipojo.manipulator.metadata.annotation.model.literal.types.InnerAnnotation;
import org.apache.felix.ipojo.manipulator.metadata.annotation.model.literal.types.Mode;
import org.apache.felix.ipojo.manipulator.metadata.annotation.model.literal.types.SimpleTypes;
import org.apache.felix.ipojo.manipulator.metadata.annotation.model.literal.types.Support;
import org.mockito.Mock;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.MockitoAnnotations;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Type;

import junit.framework.TestCase;

/**
 * User: guillaume
 * Date: 08/07/13
 * Time: 17:25
 */

public class AnnotationPlaybackTestCase extends TestCase {

    @Mock
    private ClassVisitor visitor;

    @Mock
    private AnnotationVisitor annotationVisitor;

    @Mock
    private AnnotationVisitor arrayVisitor;

    @Mock
    private AnnotationVisitor innerAnnotationVisitor;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testSimpleValuesPlayback() throws Exception {

        when(visitor.visitAnnotation(anyString(), anyBoolean())).thenReturn(annotationVisitor);

        AnnotationPlayback playback = new AnnotationPlayback(find(SimpleTypes.class));
        playback.accept(visitor);

        verify(visitor).visitAnnotation(Type.getType(SimpleTypes.class).getDescriptor(),
                                        true);
        verify(annotationVisitor).visitEnd();

        // Verify simple mono-valued attributes
        verify(annotationVisitor).visit("aByte", Byte.valueOf((byte) 42));
        verify(annotationVisitor).visit("aBoolean", Boolean.TRUE);
        verify(annotationVisitor).visit("anInt", Integer.valueOf(42));
        verify(annotationVisitor).visit("aLong", Long.valueOf(42));
        verify(annotationVisitor).visit("aShort", Short.valueOf((short) 42));
        verify(annotationVisitor).visit("aFloat", Float.valueOf(42));
        verify(annotationVisitor).visit("aDouble", Double.valueOf(42));
        verify(annotationVisitor).visit("aChar", Character.valueOf('a'));

        // Verify String & Class
        verify(annotationVisitor).visit("aString", "42");
        verify(annotationVisitor).visit("aClass", Type.getType(String.class));

        // Verify array os simple types
        verify(annotationVisitor).visit("arrayOfByte", new byte[] {(byte) 42});
        verify(annotationVisitor).visit("arrayOfBoolean", new boolean[] {true, true});
        verify(annotationVisitor).visit("arrayOfInt", new int[] {42});
        verify(annotationVisitor).visit("arrayOfLong", new long[] {42});
        verify(annotationVisitor).visit("arrayOfShort", new short[] {42});
        verify(annotationVisitor).visit("arrayOfFloat", new float[] {42});
        verify(annotationVisitor).visit("arrayOfDouble", new double[] {});
        verify(annotationVisitor).visit("arrayOfChar", new char[] {'a', 'b', 'c'});

    }

    public void testEnumValuesPlayback() throws Exception {

        when(visitor.visitAnnotation(anyString(), anyBoolean())).thenReturn(annotationVisitor);

        AnnotationPlayback playback = new AnnotationPlayback(find(EnumAnnotation.class));
        playback.accept(visitor);

        verify(visitor).visitAnnotation(Type.getType(EnumAnnotation.class).getDescriptor(),
                                        true);
        verify(annotationVisitor).visitEnd();

        String desc = Type.getType(Mode.class).getDescriptor();
        verify(annotationVisitor).visitEnum("noDefault", desc, "IN");
        verify(annotationVisitor).visitEnum("withDefault", desc, "IN");
        verify(annotationVisitor).visitEnum("withDefaultOverridden", desc, "IN");
    }

    public void testArrayValuesPlayback() throws Exception {

        AnnotationVisitor arrayOfString = mock(AnnotationVisitor.class);
        AnnotationVisitor arrayOfEnum = mock(AnnotationVisitor.class);
        AnnotationVisitor arrayOfClass = mock(AnnotationVisitor.class);
        AnnotationVisitor arrayOfAnnotation = mock(AnnotationVisitor.class);
        AnnotationVisitor emptyArray = mock(AnnotationVisitor.class);

        when(visitor.visitAnnotation(anyString(), anyBoolean())).thenReturn(annotationVisitor);
        when(annotationVisitor.visitArray("arrayOfString")).thenReturn(arrayOfString);
        when(annotationVisitor.visitArray("arrayOfEnum")).thenReturn(arrayOfEnum);
        when(annotationVisitor.visitArray("arrayOfClass")).thenReturn(arrayOfClass);
        when(annotationVisitor.visitArray("arrayOfAnnotation")).thenReturn(arrayOfAnnotation);
        when(annotationVisitor.visitArray("emptyArray")).thenReturn(emptyArray);

        AnnotationPlayback playback = new AnnotationPlayback(find(ArrayAnnotation.class));
        playback.accept(visitor);

        verify(visitor).visitAnnotation(Type.getType(ArrayAnnotation.class).getDescriptor(),
                                        true);
        verify(annotationVisitor).visitEnd();

        String desc = Type.getType(Mode.class).getDescriptor();

        verify(arrayOfString).visit(null, "42");
        verify(arrayOfString).visit(null, "43");
        verify(arrayOfString).visitEnd();

        verify(arrayOfEnum).visitEnum(null, desc, "IN");
        verify(arrayOfEnum).visitEnd();

        verify(arrayOfClass).visit(null, Type.getType(Object.class));
        verify(arrayOfClass).visitEnd();

        verify(arrayOfAnnotation).visitAnnotation(null, Type.getType(InnerAnnotation.class).getDescriptor());
        verify(arrayOfAnnotation).visitEnd();

        verify(emptyArray).visitEnd();

    }


    public void testAnnotationValuesPlayback() throws Exception {

        when(visitor.visitAnnotation(anyString(), anyBoolean())).thenReturn(annotationVisitor);
        when(annotationVisitor.visitAnnotation("value", Type.getType(InnerAnnotation.class).getDescriptor()))
                .thenReturn(innerAnnotationVisitor);
        when(innerAnnotationVisitor.visitArray("arrayOfClass"))
                .thenReturn(arrayVisitor);

        AnnotationPlayback playback = new AnnotationPlayback(find(AnnotationAnnotation.class));
        playback.accept(visitor);

        verify(visitor).visitAnnotation(Type.getType(AnnotationAnnotation.class).getDescriptor(),
                                        true);
        verify(annotationVisitor).visitEnd();

        String desc = Type.getType(Mode.class).getDescriptor();
        verify(innerAnnotationVisitor).visit("aString", "42");
        verify(innerAnnotationVisitor).visitEnum("modeEnum", desc, "IN");
        verify(innerAnnotationVisitor).visitEnd();

        verify(arrayVisitor).visit(null, Type.getType(Object.class));
        verify(arrayVisitor).visitEnd();

    }
    private Annotation find(final Class<? extends Annotation> type) {
        return Support.class.getAnnotation(type);
    }

}
