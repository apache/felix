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

package org.apache.felix.ipojo.manipulator.visitor;

import org.apache.felix.ipojo.manipulator.ManipulationResultVisitor;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Empty ManipulationResultVisitor visitor.
 * It is useful to extends this class to keep a working visitor chain.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ManipulationResultAdapter implements ManipulationResultVisitor {

    private ManipulationResultVisitor m_delegate;

    public ManipulationResultAdapter(ManipulationResultVisitor delegate) {
        m_delegate = delegate;
    }

    public void visitClassStructure(Element structure) {
        m_delegate.visitClassStructure(structure);
    }

    public void visitManipulatedResource(String type, byte[] resource) {
        m_delegate.visitManipulatedResource(type, resource);
    }

    public void visitEnd() {
        m_delegate.visitEnd();
    }
}
