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

package org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.stereotype;

import org.apache.felix.ipojo.manipulator.metadata.annotation.model.AnnotationType;
import org.apache.felix.ipojo.manipulator.metadata.annotation.model.Playback;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * User: guillaume
 * Date: 30/05/13
 * Time: 18:55
 */
public class ParameterStereotypeVisitor extends AnnotationVisitor {

    private final MethodVisitor m_delegate;
    private final int index;
    private final AnnotationType m_annotationType;

    public ParameterStereotypeVisitor(final MethodVisitor delegate, final int index, AnnotationType annotationType) {
        super(Opcodes.ASM5);
        this.m_delegate = delegate;
        this.index = index;
        m_annotationType = annotationType;
    }

    @Override
    public void visitEnd() {
        // Replay stereotype annotations
        for (Playback playback : m_annotationType.getPlaybacks()) {
            playback.accept(m_delegate, index);
        }
    }
}
