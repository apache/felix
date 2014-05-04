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

import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.metadata.annotation.ComponentWorkbench;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Parses the @Handler annotation.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 * @see org.apache.felix.ipojo.annotations.Handler
 */
public class HandlerVisitor extends AnnotationVisitor {

    private Element handler = new Element("handler", "");

    private ComponentWorkbench workbench;

    private Reporter reporter;

    public HandlerVisitor(ComponentWorkbench workbench, Reporter reporter) {
        super(Opcodes.ASM5);
        this.workbench = workbench;
        this.reporter = reporter;
    }

    /**
     * Visit @Handler annotation attributes.
     *
     * @param name  : annotation attribute name
     * @param value : annotation attribute value
     * @see org.objectweb.asm.AnnotationVisitor#visit(java.lang.String, java.lang.Object)
     */
    public void visit(String name, Object value) {
        if (name.equals("name")) {
            handler.addAttribute(new Attribute("name", value.toString()));
            return;
        }
        if (name.equals("namespace")) {
            handler.addAttribute(new Attribute("namespace", value.toString()));
            return;
        }
        if (name.equals("level")) {
            handler.addAttribute(new Attribute("level", value.toString()));
            return;
        }
        if (name.equals("architecture")) {
            handler.addAttribute(new Attribute("architecture", value.toString()));
        }
    }


    /**
     * End of the visit.
     * Append to the "component" element computed attribute.
     *
     * @see org.objectweb.asm.AnnotationVisitor#visitEnd()
     */
    public void visitEnd() {

        String classname = workbench.getType().getClassName();
        handler.addAttribute(new Attribute("classname", classname));

        if (workbench.getRoot() == null) {
            workbench.setRoot(handler);
        } else {
            // Error case: 2 component type's annotations (@Component and @Handler for example) on the same class
            reporter.error("Multiple 'component type' annotations on the class '{%s}'.", classname);
            reporter.warn("@Handler will be ignored.");
        }
    }
}
