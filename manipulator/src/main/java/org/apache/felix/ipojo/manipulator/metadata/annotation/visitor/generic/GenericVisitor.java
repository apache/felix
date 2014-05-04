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

import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.util.Elements;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.reflect.Array;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class GenericVisitor extends AnnotationVisitor {
    protected Element element;

    public GenericVisitor(Element element) {
        super(Opcodes.ASM5);
        this.element = element;
    }

    /**
     * Visit a 'simple' annotation attribute.
     * This method is used for primitive arrays too.
     *
     * @param name  : attribute name
     * @param value : attribute value
     * @see org.objectweb.asm.AnnotationVisitor#visit(String, Object)
     */
    public void visit(String name, Object value) {
        if (value.getClass().isArray()) {
            // Primitive arrays case
            String v = null;
            int index = Array.getLength(value);
            for (int i = 0; i < index; i++) {
                if (v == null) {
                    v = "{" + Array.get(value, i);
                } else {
                    v += "," + Array.get(value, i);
                }
            }
            v += "}";
            element.addAttribute(new Attribute(name, v));
            return;
        }

        // Attributes are added as normal attributes
        if (!(value instanceof Type)) {
            element.addAttribute(new Attribute(name, value.toString()));
        } else {
            // Attributes of type class need a special handling
            element.addAttribute(new Attribute(name, ((Type) value).getClassName()));
        }

    }

    /**
     * Visit a sub-annotation.
     *
     * @param name       : attribute name.
     * @param descriptor : annotation description
     * @return an annotation visitor which will visit the given annotation
     * @see org.objectweb.asm.AnnotationVisitor#visitAnnotation(String, String)
     */
    public AnnotationVisitor visitAnnotation(String name, String descriptor) {
        // Sub annotations are mapped to sub-elements
        Element sub = Elements.buildElement(Type.getType(descriptor));
        element.addElement(sub);
        return new GenericVisitor(sub);
    }

    /**
     * Visit an array attribute.
     *
     * @param name : attribute name
     * @return a visitor which will visit each element of the array
     * @see org.objectweb.asm.AnnotationVisitor#visitArray(String)
     */
    public AnnotationVisitor visitArray(String name) {
        return new SubArrayVisitor(element, name);
    }

    /**
     * Visits an enumeration attribute.
     *
     * @param name  the attribute name
     * @param desc  the enumeration descriptor
     * @param value the attribute value
     * @see org.objectweb.asm.AnnotationVisitor#visitEnum(String, String, String)
     */
    public void visitEnum(String name, String desc, String value) {
        element.addAttribute(new Attribute(name, value));
    }
}
