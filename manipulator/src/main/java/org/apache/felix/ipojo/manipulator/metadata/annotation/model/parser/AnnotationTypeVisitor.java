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

package org.apache.felix.ipojo.manipulator.metadata.annotation.model.parser;

import org.apache.felix.ipojo.manipulator.metadata.annotation.model.AnnotationType;
import org.apache.felix.ipojo.manipulator.metadata.annotation.model.parser.replay.AnnotationVisitorPlayback;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 *
 */
public class AnnotationTypeVisitor extends ClassVisitor {

    private AnnotationType annotationType;

    public AnnotationTypeVisitor() {
        super(Opcodes.ASM5);
    }

    @Override
    public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
        annotationType = new AnnotationType(Type.getObjectType(name));
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
        // Build annotations of this annotation type
        AnnotationVisitorPlayback playback = new AnnotationVisitorPlayback(desc, visible);
        annotationType.getPlaybacks().add(playback);
        return playback;
    }

    public AnnotationType getAnnotationType() {
        return annotationType;
    }

    // Note: if we override visitMethod here, we could get the annotation's default values.
}
