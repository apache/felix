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

package org.apache.felix.ipojo.manipulator.metadata.annotation.visitor;

import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ParameterPropertyVisitor extends MethodPropertyVisitor {

    /**
     * If this is a parameter annotation, the index of the parameter.
     */
    private int m_index = -1;

    private MethodNode node;

    /**
     * Constructor.
     *
     * @param parent : element element.
     * @param method : attached method.
     * @param index  : the parameter index
     */
    public ParameterPropertyVisitor(Element parent, MethodNode method, int index) {
        super(parent, method.name);
        this.node = method;
        m_index = index;
    }

    /**
     * End of the visit.
     * Append the computed element to the element element.
     *
     * @see org.objectweb.asm.AnnotationVisitor#visitEnd()
     */
    public void visitEnd() {
        Element prop = visitEndCommon();
        String type = Type.getArgumentTypes(node.desc)[m_index].getClassName();
        prop.addAttribute(new Attribute("type", type));
        prop.addAttribute(new Attribute("constructor-parameter", Integer.toString(m_index)));

    }
}
