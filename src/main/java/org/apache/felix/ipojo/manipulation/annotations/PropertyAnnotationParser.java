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
package org.apache.felix.ipojo.manipulation.annotations;

import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * Parses a Property or ServiceProperty annotation.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
class PropertyAnnotationParser extends EmptyVisitor implements AnnotationVisitor {

    /**
     * Parent element element.
     */
    private Element m_parent;

    /**
     * Field name.
     */
    private String m_field;

    /**
     * Property name.
     */
    private String m_name;

    /**
     * Property value.
     */
    private String m_value;

    /**
     * Property mandatory aspect.
     */
    private String m_mandatory;

    /**
     * Property type.
     */
    private String m_type;


    /**
     * Constructor.
     * @param parent : parent element.
     * @param field : field name.
     */
    PropertyAnnotationParser(String field, Element parent) {
        m_parent = parent;
        m_field = field;
    }

    /**
     * Constructor without field
     * @param parent : parent element..
     */
    PropertyAnnotationParser(Element parent) {
        m_parent = parent;
        m_field = null;
    }

    /**
     * Visit one "simple" annotation.
     * @param arg0 : annotation name
     * @param arg1 : annotation value
     * @see org.objectweb.asm.AnnotationVisitor#visit(java.lang.String, java.lang.Object)
     */
    public void visit(String arg0, Object arg1) {
        if (arg0.equals("name")) {
            m_name = arg1.toString();
            return;
        }
        if (arg0.equals("value")) {
            m_value = arg1.toString();
            return;
        }
        if (arg0.equals("mandatory")) {
            m_mandatory = arg1.toString();
            return;
        }
        if (arg0.equals("type")) {
        	m_type = arg1.toString();
        	return;
        }
    }

    /**
     * End of the annotation.
     * Create a "property" element
     * @see org.objectweb.asm.AnnotationVisitor#visitEnd()
     */
    public void visitEnd() {
        if (m_field != null  && m_name == null) {
            m_name = m_field;
        }


        Element[] props = m_parent.getElements("Property");
        Element prop = null;
        for (int i = 0; prop == null && props != null && i < props.length; i++) {
            String name = props[i].getAttribute("name");
            if (name != null && name.equals(m_name)) {
                prop = props[i];
            }
        }

        if (prop == null) {
            prop = new Element("property", "");
            m_parent.addElement(prop);
            if (m_name != null) {
                prop.addAttribute(new Attribute("name", m_name));
            }
        }

        if (m_field != null) {
        	prop.addAttribute(new Attribute("field", m_field));
        }
        if (m_type != null) {
        	prop.addAttribute(new Attribute("type", m_type));
        }

        if (m_value != null) {
            prop.addAttribute(new Attribute("value", m_value));
        }
        if (m_mandatory != null) {
            prop.addAttribute(new Attribute("mandatory", m_mandatory));
        }

    }
}