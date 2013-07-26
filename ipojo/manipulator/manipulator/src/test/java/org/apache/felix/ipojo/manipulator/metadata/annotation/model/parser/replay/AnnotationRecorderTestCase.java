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

package org.apache.felix.ipojo.manipulator.metadata.annotation.model.parser.replay;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.objectweb.asm.AnnotationVisitor;

import junit.framework.TestCase;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * User: guillaume
 * Date: 30/05/13
 * Time: 22:04
 */
public class AnnotationRecorderTestCase extends TestCase {

    @Mock
    private AnnotationVisitor visitor;

    @Override
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    public void testVisitRecord() throws Exception {
        AnnotationRecorder recorder = new AnnotationRecorder();
        recorder.visit("attribute", "a value");
        recorder.accept(visitor);
        verify(visitor).visit("attribute", "a value");
    }

    public void testVisitEndRecord() throws Exception {
        AnnotationRecorder recorder = new AnnotationRecorder();
        recorder.visitEnd();
        recorder.accept(visitor);
        verify(visitor).visitEnd();
    }

    public void testVisitEnumRecord() throws Exception {
        AnnotationRecorder recorder = new AnnotationRecorder();
        recorder.visitEnum("name", "type-desc", "A");
        recorder.accept(visitor);
        verify(visitor).visitEnum("name", "type-desc", "A");
    }

    public void testVisitAnnotationRecord() throws Exception {

        when(visitor.visitAnnotation("name", "type-desc")).thenReturn(visitor);

        AnnotationRecorder recorder = new AnnotationRecorder();
        AnnotationVisitor sub = recorder.visitAnnotation("name", "type-desc");
        sub.visit("name2", "value2");
        recorder.accept(visitor);

        verify(visitor).visitAnnotation("name", "type-desc");
        verify(visitor).visit("name2", "value2");
    }

    public void testVisitArrayRecord() throws Exception {

        when(visitor.visitArray("name")).thenReturn(visitor);

        AnnotationRecorder recorder = new AnnotationRecorder();
        AnnotationVisitor sub = recorder.visitArray("name");
        sub.visit("name2", "value2");
        recorder.accept(visitor);

        verify(visitor).visitArray("name");
        verify(visitor).visit("name2", "value2");
    }
}
