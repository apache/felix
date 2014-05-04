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
import org.objectweb.asm.Type;

/**
 * Parses a @ServiceController annotation.
 * @see org.apache.felix.ipojo.annotations.ServiceController
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceControllerVisitor extends AnnotationVisitor {

    /**
     * Parent element.
     */
    private Element provides;

    /**
     * Provides element.
     */
    private Element controller = new Element("controller", "");

    /**
     * Constructor.
     * @param provides : element element.
     * @param field : field name.
     */
    public ServiceControllerVisitor(String field, Element provides) {
        super(Opcodes.ASM5);
        this.provides = provides;
        controller.addAttribute(new Attribute("field", field));
    }

    /**
     * Visit one "simple" annotation.
     * @param name : annotation name
     * @param value : annotation value
     * @see org.objectweb.asm.AnnotationVisitor#visit(java.lang.String, java.lang.Object)
     */
    public void visit(String name, Object value) {
        if (name.equals("value")) {
            controller.addAttribute(new Attribute("value", value.toString()));
            return;
        }
        if (name.equals("specification")) {
            String spec = ((Type) value).getClassName();
            controller.addAttribute(new Attribute("specification", spec));
        }
    }

    /**
     * End of the annotation.
     * Create a "controller" element
     * @see org.objectweb.asm.AnnotationVisitor#visitEnd()
     */
    public void visitEnd() {
        provides.addElement(controller);
    }
}
