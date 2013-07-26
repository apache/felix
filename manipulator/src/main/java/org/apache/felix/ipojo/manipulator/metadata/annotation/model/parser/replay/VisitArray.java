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

/**
* User: guillaume
* Date: 30/05/13
* Time: 17:24
*/
public class VisitArray implements Replay {
    private final String m_name;
    private final AnnotationRecorder m_sub;

    public VisitArray(final String name, final AnnotationRecorder sub) {
        m_name = name;
        m_sub = sub;
    }

    public void accept(final AnnotationVisitor visitor) {
        AnnotationVisitor child = visitor.visitArray(m_name);
        if (child != null) {
            m_sub.accept(child);
        }
    }
}
