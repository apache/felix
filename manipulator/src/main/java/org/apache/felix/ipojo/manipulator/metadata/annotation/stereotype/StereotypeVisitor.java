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

import org.apache.felix.ipojo.annotations.Stereotype;
import org.apache.felix.ipojo.manipulator.metadata.annotation.stereotype.replay.RootAnnotationRecorder;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.EmptyVisitor;

/**
* User: guillaume
* Date: 30/05/13
* Time: 17:09
*/
public class StereotypeVisitor extends EmptyVisitor {

    public static final Type MARKER_TYPE = Type.getType(Stereotype.class);

    private StereotypeParser m_definition;

    public StereotypeVisitor(final StereotypeParser definition) {
        m_definition = definition;
    }

    @Override
    public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
        Type annotationType = Type.getType(desc);
        if (MARKER_TYPE.equals(annotationType)) {
            m_definition.setStereotype(true);
            return null;
        }
        RootAnnotationRecorder visitor = new RootAnnotationRecorder(desc, visible);
        m_definition.getRecorders().add(visitor);
        return visitor;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String name, final String desc, final String signature, final String[] exceptions) {
        return null;
    }

    @Override
    public FieldVisitor visitField(final int access, final String name, final String desc, final String signature, final Object value) {
        return null;
    }

}
