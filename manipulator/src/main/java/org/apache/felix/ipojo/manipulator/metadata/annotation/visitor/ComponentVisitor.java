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
 * Parse the @Component annotation.
 * @see org.apache.felix.ipojo.annotations.Component
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ComponentVisitor extends AnnotationVisitor {

    private Reporter reporter;

    /**
     * Element 'properties'.
     */
    private Element m_props = new Element("properties", "");

    private Element component = new Element("component", "");

    private ComponentWorkbench workbench;

    public ComponentVisitor(ComponentWorkbench workbench, Reporter reporter) {
        super(Opcodes.ASM5);
        this.workbench = workbench;
        this.reporter = reporter;
    }

    /**
     * Visit @Component annotation attribute.
     * @param name attribute name
     * @param value attribute value
     * @see org.objectweb.asm.AnnotationVisitor#visit(java.lang.String, java.lang.Object)
     */
    public void visit(String name, Object value) {
        if (name.equals("public_factory")  || name.equals("publicFactory")) {
            // public_factory is deprecated, but must sill be supported
            String factory = value.toString();
            if (factory.equalsIgnoreCase("false")) {
                component.addAttribute(new Attribute("public", "false"));
            } else {
                component.addAttribute(new Attribute("public", "true"));
            }
            return;
        }

        if (name.equals("name")) {
            component.addAttribute(new Attribute("name", value.toString()));
            return;
        }
        if (name.equals("immediate")) {
            component.addAttribute(new Attribute("immediate", value.toString()));
            return;
        }
        if (name.equals("architecture")) {
            component.addAttribute(new Attribute("architecture", value.toString()));
            return;
        }
        if (name.equals("propagation") && (value != null)) {
            if (arePropertiesEmpty()) {
                initComponentProperties();
            }
            m_props.addAttribute(new Attribute("propagation", value.toString()));
            return;
        }
        if (name.equals("managedservice") && (value != null)) {
            if (arePropertiesEmpty()) {
                initComponentProperties();
            }
            m_props.addAttribute(new Attribute("pid", value.toString()));
            return;
        }
        if ((name.equals("factory_method")  || name.equals("factoryMethod")) && (value != null)) {
            // factory_method is deprecated, but must still be supported.
            component.addAttribute(new Attribute("factory-method", value.toString()));
            return;
        }
        if (name.equals("version") && (value != null)) {
            component.addAttribute(new Attribute("version", value.toString()));
        }
    }

    private boolean arePropertiesEmpty() {
        return m_props.getElements().length == 0;
    }

    private void initComponentProperties() {
        workbench.getElements().put(m_props, null);
        workbench.getIds().put("properties", m_props);
    }

    /**
     * End of the visit.
     * Append to the "component" element computed attribute.
     * @see org.objectweb.asm.AnnotationVisitor#visitEnd()
     */
    public void visitEnd() {

        String classname = workbench.getType().getClassName();

        if (component.getAttribute("name") == null) {
            component.addAttribute(new Attribute("name", classname));
        }

        component.addAttribute(new Attribute("classname", classname));

        if (workbench.getRoot() == null) {
            workbench.setRoot(component);
        } else {
            // Error case: 2 component type's annotations (@Component and @Handler for example) on the same class
            reporter.error("Multiple 'component type' annotations on the class '{%s}'.", classname);
            reporter.warn("@Component will be ignored.");
        }
    }
}
