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
* Time: 17:23
*/
public class VisitEnum implements Replay {
    private final String m_name;
    private final String m_desc;
    private final String m_value;

    public VisitEnum(final String name, final String desc, final String value) {m_name = name;
        m_desc = desc;
        m_value = value;
    }

    public void accept(final AnnotationVisitor visitor) {
        visitor.visitEnum(m_name, m_desc, m_value);
    }
}
