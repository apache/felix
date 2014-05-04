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
import org.objectweb.asm.Opcodes;

import static org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.util.Names.computeEffectiveMethodName;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MethodPropertyVisitor extends AnnotationVisitor {

    /**
     * Parent element.
     */
    private Element m_parent;

    /**
     * Attached method.
     */
    private String m_method;

    /**
     * Property name.
     */
    private String m_name;

    /**
     * Property id.
     */
    private String m_id;

    /**
     * Property value.
     */
    private String m_value;

    /**
     * Property mandatory aspect.
     */
    private String m_mandatory;

    /**
     * Property immutable aspect.
     */
    private String m_immutable;

    /**
     * Constructor.
     *
     * @param parent : element element.
     * @param method : attached method.
     */
    public MethodPropertyVisitor(Element parent, String method) {
        super(Opcodes.ASM5);
        m_parent = parent;
        m_method = method;
    }

    /**
     * Visit annotation attributes.
     *
     * @param name : annotation name
     * @param value : annotation value
     * @see org.objectweb.asm.AnnotationVisitor#visit(java.lang.String, java.lang.Object)
     */
    public void visit(String name, Object value) {
        if (name.equals("name")) {
            m_name = value.toString();
            return;
        }
        if (name.equals("value")) {
            m_value = value.toString();
            return;
        }
        if (name.equals("mandatory")) {
            m_mandatory = value.toString();
            return;
        }
        if (name.equals("immutable")) {
            m_immutable = value.toString();
            return;
        }
        if (name.equals("id")) {
            m_id = value.toString();
        }
    }

    /**
     * End of the visit.
     * Append the computed element to the element element.
     *
     * @see org.objectweb.asm.AnnotationVisitor#visitEnd()
     */
    public void visitEnd() {
        Element prop = visitEndCommon();

        prop.addAttribute(new Attribute("method", m_method));

    }

    protected Element visitEndCommon() {
        m_method = computeEffectiveMethodName(m_method);

        // If neither name nor id is provided, try to extract the name
        if (m_name == null && m_id == null && m_method.startsWith("set")) {
            m_name = m_method.substring("set".length());
            m_id = m_name;
            // Else align the two values
        } else if (m_name != null && m_id == null) {
            m_id = m_name;
        } else if (m_id != null && m_name == null) {
            m_name = m_id;
        }

        Element prop = getPropertyElement();

        if (m_value != null) {
            prop.addAttribute(new Attribute("value", m_value));
        }
        if (m_mandatory != null) {
            prop.addAttribute(new Attribute("mandatory", m_mandatory));
        }
        if (m_immutable != null) {
            prop.addAttribute(new Attribute("immutable", m_immutable));
        }

        return prop;
    }

    private Element getPropertyElement() {

        // Gather all the <property> Elements
        Element[] props = m_parent.getElements("property");
        Element prop = null;
        for (int i = 0; props != null && prop == null && i < props.length; i++) {

            // Get the first one with the good name
            String name = props[i].getAttribute("name");
            if (name != null && name.equals(m_name)) {
                prop = props[i];
            }
        }

        // Create the Element if not present
        if (prop == null) {
            prop = new Element("property", "");
            m_parent.addElement(prop);
            if (m_name != null) {
                prop.addAttribute(new Attribute("name", m_name));
            }
        }

        return prop;
    }
}
