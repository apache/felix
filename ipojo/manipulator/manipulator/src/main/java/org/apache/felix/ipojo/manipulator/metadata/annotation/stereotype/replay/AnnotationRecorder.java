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

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;

/**
* User: guillaume
* Date: 30/05/13
* Time: 17:22
*/
public class AnnotationRecorder implements AnnotationVisitor, Replay {

    private List<Replay> record = new ArrayList<Replay>();

    public void visit(final String name, final Object value) {
        record.add(new Visit(name, value));
    }

    public void visitEnum(final String name, final String desc, final String value) {
        record.add(new VisitEnum(name, desc, value));
    }

    public AnnotationVisitor visitAnnotation(final String name, final String desc) {
        AnnotationRecorder sub = new AnnotationRecorder();
        record.add(new VisitAnnotation(name, desc, sub));
        return sub;
    }

    public AnnotationVisitor visitArray(final String name) {
        AnnotationRecorder sub = new AnnotationRecorder();
        record.add(new VisitArray(name, sub));
        return sub;
    }

    public void visitEnd() {
        record.add(new VisitEnd());
    }

    public void accept(final AnnotationVisitor visitor) {
        for (Replay replay : record) {
            replay.accept(visitor);
        }
    }
}
