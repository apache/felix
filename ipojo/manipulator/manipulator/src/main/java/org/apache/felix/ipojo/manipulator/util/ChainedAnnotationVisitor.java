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

package org.apache.felix.ipojo.manipulator.util;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

/**
* User: guillaume
* Date: 10/07/13
* Time: 16:43
*/
public class ChainedAnnotationVisitor extends AnnotationVisitor {

    private List<AnnotationVisitor> m_visitors = new ArrayList<AnnotationVisitor>();

    public ChainedAnnotationVisitor() {
        super(Opcodes.ASM5);
    }

    public List<AnnotationVisitor> getVisitors() {
        return m_visitors;
    }

    public void visit(final String name, final Object value) {
        for (AnnotationVisitor visitor : m_visitors) {
            visitor.visit(name, value);
        }
    }

    public void visitEnum(final String name, final String desc, final String value) {
        for (AnnotationVisitor visitor : m_visitors) {
            visitor.visitEnum(name, desc, value);
        }
    }

    public AnnotationVisitor visitAnnotation(final String name, final String desc) {
        ChainedAnnotationVisitor chain = null;
        for (AnnotationVisitor visitor : m_visitors) {
            AnnotationVisitor child = visitor.visitAnnotation(name, desc);
            if (child != null) {
                if (chain == null) {
                    chain = new ChainedAnnotationVisitor();
                }
                chain.getVisitors().add(child);
            }

        }
        return chain;
    }

    public AnnotationVisitor visitArray(final String name) {
        ChainedAnnotationVisitor chain = null;
        for (AnnotationVisitor visitor : m_visitors) {
            AnnotationVisitor child = visitor.visitArray(name);
            if (child != null) {
                if (chain == null) {
                    chain = new ChainedAnnotationVisitor();
                }
                chain.getVisitors().add(child);
            }

        }
        return chain;
    }

    public void visitEnd() {
        for (AnnotationVisitor visitor : m_visitors) {
            visitor.visitEnd();
        }
    }
}
