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

package org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.generic;

import org.apache.felix.ipojo.manipulator.metadata.annotation.ComponentWorkbench;
import org.apache.felix.ipojo.metadata.Element;

import java.lang.annotation.ElementType;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class RootGenericVisitor extends GenericVisitor {
    private ComponentWorkbench workbench;

    /**
     * Id attribute (if found)
     * else use the annotation package name.
     */
    private String m_id;

    /**
     * Parent attribute (if found)
     * else use the annotation package name.
     */
    private String m_parent;

    /**
     * Describes the structure that supports the traversed annotation.
     */
    private ElementType type;

    public RootGenericVisitor(ComponentWorkbench workbench, Element element, ElementType type) {
        super(element);
        this.workbench = workbench;
        this.type = type;
    }

    /**
     * Visit a 'simple' annotation attribute.
     * This method is used for primitive arrays too.
     * @param name : attribute name
     * @param value : attribute value
     * @see org.objectweb.asm.commons.EmptyVisitor#visit(String, Object)
     */
    public void visit(String name, Object value) {
        super.visit(name, value);

        if (name.equals("id")) {
            m_id = value.toString();
        } else if (name.equals("parent")) {
            m_parent = value.toString();
        }
    }



    /**
     * End of the visit.
     * All attribute were visited, we can update collectors data.
     * @see org.objectweb.asm.commons.EmptyVisitor#visitEnd()
     */
    public void visitEnd() {

        if (m_id != null) {
            // An ID has been provided as annotation attribute
            // Register our element under that ID
            workbench.getIds().put(m_id, element);
        } else {
            // No ID provided, generate a new one from the element's namespace (aka handler's namespace)
            m_id = element.getNameSpace();
            if (m_id != null  && !workbench.getIds().containsKey(m_id) && isClassType()) {
                // No Elements were already registered under that namespace
                workbench.getIds().put(m_id, element);
            } else {
                // ID already registered by another annotation
                if (m_parent == null) {
                    // If no parent specified, place this element under the 'class level' Element (default)
                    m_parent = element.getNameSpace();
                } // Otherwise, place this element under the specified Element (contribution)
            }
        }

        workbench.getElements().put(element, m_parent);

    }

    private boolean isClassType() {
        return ElementType.TYPE.equals(type);
    }

}
