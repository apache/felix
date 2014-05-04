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

package org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.bind;

import org.apache.felix.ipojo.manipulator.metadata.annotation.ComponentWorkbench;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.RequiresVisitor;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class AbstractBindVisitor extends AnnotationVisitor {

    protected ComponentWorkbench workbench;
    protected Action action;

    public AbstractBindVisitor(ComponentWorkbench workbench, Action action) {
        super(Opcodes.ASM5);
        this.workbench = workbench;
        this.action = action;
    }

    /**
     * Requirement filter.
     */
    protected String m_filter;
    /**
     * Is the requirement optional?
     */
    protected String m_optional;
    /**
     * Is the requirement aggregate?
     */
    protected String m_aggregate;
    /**
     * Required specification.
     */
    protected String m_specification;
    /**
     * Requirement id.
     */
    protected String m_id;
    /**
     * Binding policy.
     */
    protected String m_policy;
    /**
     * Comparator.
     */
    protected String m_comparator;
    /**
     * From attribute.
     */
    protected String m_from;

    /**
     * proxy attribute.
     */
    protected String m_proxy;

    /**
     * Visit annotation's attributes.
     *
     * @param name  : annotation name
     * @param value : annotation value
     * @see org.objectweb.asm.AnnotationVisitor#visit(String, Object)
     */
    public void visit(String name, Object value) {
        if (name.equals("filter")) {
            m_filter = value.toString();
            return;
        }
        if (name.equals("optional")) {
            m_optional = value.toString();
            return;
        }
        if (name.equals("aggregate")) {
            m_aggregate = value.toString();
            return;
        }
        if (name.equals("specification")) {
            // Detect whether it's an internal class name.
            if (value.toString().startsWith("L") && value.toString().endsWith(";")) {
                Type type = Type.getType(value.toString());
                m_specification = type.getClassName();
            } else {
                m_specification = value.toString();
            }
            return;
        }
        // Still used by component using the old version of the annotations.
        if (name.equals("policy")) {
            m_policy = RequiresVisitor.getPolicy(value.toString());
            return;
        }
        if (name.equals("id")) {
            m_id = value.toString();
            return;
        }
        if (name.equals("comparator")) {
            Type type = Type.getType(value.toString());
            m_comparator = type.getClassName();
            return;
        }
        if (name.equals("from")) {
            m_from = value.toString();
        }
        if (name.equals("proxy")) {
            m_proxy = value.toString();
        }

    }

    @Override
    public void visitEnum(String name, String desc, String value) {
        if (name.equals("policy")) {
            m_policy = RequiresVisitor.getPolicy(value);
        }
    }

    public void visitEnd() {
    }

    protected Element getRequiresElement() {

        // Check if it is a full-determined requirement
        Element requires = workbench.getIds().get(m_id);
        if (requires == null) {
            // Add the complete requires
            requires = createRequiresElement();
        } else {
            if (!completeExistingRequires(requires))
                return null;
        }

        return requires;
    }

    protected boolean completeExistingRequires(Element requires) {
        return
                completeAttribute(requires, "specification", m_specification)
                        && completeAttribute(requires, "optional", m_optional)
                        && completeAttribute(requires, "aggregate", m_aggregate)
                        && completeAttribute(requires, "filter", m_filter)
                        && completeAttribute(requires, "policy", m_policy)
                        && completeAttribute(requires, "comparator", m_comparator)
                        && completeAttribute(requires, "from", m_from)
                        && completeAttribute(requires, "proxy", m_proxy);
    }

    private boolean completeAttribute(Element requires, String name, String value) {
        // If we have a value
        if (value != null) {

            String old = requires.getAttribute(name);
            if (old == null) {
                // If the old value was not set, just set the new value
                requires.addAttribute(new Attribute(name, value));
            } else if (!value.equals(old)) {
                //
                System.err.println("The '" + name + "' attribute has changed: " + old + " -> " + value);
                return false;
            } // Otherwise, the old and new value are equals
        }
        return true;
    }

    protected Element createRequiresElement() {
        Element requires;
        requires = new Element("requires", "");
        if (m_specification != null) {
            requires.addAttribute(new Attribute("specification", m_specification));
        }
        if (m_aggregate != null) {
            requires.addAttribute(new Attribute("aggregate", m_aggregate));
        }
        if (m_filter != null) {
            requires.addAttribute(new Attribute("filter", m_filter));
        }
        if (m_optional != null) {
            requires.addAttribute(new Attribute("optional", m_optional));
        }
        if (m_policy != null) {
            requires.addAttribute(new Attribute("policy", m_policy));
        }
        if (m_id != null) {
            requires.addAttribute(new Attribute("id", m_id));
        }
        if (m_comparator != null) {
            requires.addAttribute(new Attribute("comparator", m_comparator));
        }
        if (m_from != null) {
            requires.addAttribute(new Attribute("from", m_from));
        }
        if (m_proxy != null) {
            requires.addAttribute(new Attribute("proxy", m_proxy));
        }
        return requires;
    }


}
