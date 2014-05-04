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

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

/**
 * Record annotation to be replayed later.
 */
public class AnnotationRecorder extends AnnotationVisitor implements Replay {

    private List<Replay> m_replays = new ArrayList<Replay>();

    public AnnotationRecorder() {
        super(Opcodes.ASM5);
    }

    public void visit(final String name, final Object value) {
        m_replays.add(new Visit(name, value));
    }

    public void visitEnum(final String name, final String desc, final String value) {
        m_replays.add(new VisitEnum(name, desc, value));
    }

    public AnnotationVisitor visitAnnotation(final String name, final String desc) {
        AnnotationRecorder sub = new AnnotationRecorder();
        m_replays.add(new VisitAnnotation(name, desc, sub));
        return sub;
    }

    public AnnotationVisitor visitArray(final String name) {
        AnnotationRecorder sub = new AnnotationRecorder();
        m_replays.add(new VisitArray(name, sub));
        return sub;
    }

    public void visitEnd() {
        m_replays.add(new VisitEnd());
    }

    public void accept(final AnnotationVisitor visitor) {
        for (Replay replay : m_replays) {
            replay.accept(visitor);
        }
    }
}
