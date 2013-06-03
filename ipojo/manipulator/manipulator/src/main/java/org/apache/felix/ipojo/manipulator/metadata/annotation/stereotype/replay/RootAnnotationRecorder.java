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

package org.apache.felix.ipojo.manipulator.metadata.annotation.stereotype.replay;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * User: guillaume
 * Date: 30/05/13
 * Time: 19:20
 */
public class RootAnnotationRecorder extends AnnotationRecorder {
    private final String m_desc;
    private final boolean m_visible;

    public RootAnnotationRecorder(final String desc, final boolean visible) {
        m_desc = desc;
        m_visible = visible;
    }

    public void accept(final FieldVisitor visitor) {
        AnnotationVisitor av = visitor.visitAnnotation(m_desc, m_visible);
        if (av != null) {
            accept(av);
        }
    }

    public void accept(final ClassVisitor visitor) {
        AnnotationVisitor av = visitor.visitAnnotation(m_desc, m_visible);
        if (av != null) {
            accept(av);
        }
    }

    public void accept(final MethodVisitor visitor) {
        AnnotationVisitor av = visitor.visitAnnotation(m_desc, m_visible);
        if (av != null) {
            accept(av);
        }
    }

    public void accept(final MethodVisitor visitor, int index) {
        AnnotationVisitor av = visitor.visitParameterAnnotation(index, m_desc, m_visible);
        if (av != null) {
            accept(av);
        }
    }
}
