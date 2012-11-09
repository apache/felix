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

import org.apache.felix.ipojo.manipulator.metadata.annotation.ComponentWorkbench;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * AnnotationVisitor parsing the @Requires annotation.
 * @see org.apache.felix.ipojo.annotations.Requires
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class RequiresVisitor extends EmptyVisitor implements AnnotationVisitor {


    private ComponentWorkbench workbench;

    /**
     * Dependency field.
     */
    private String m_field;

    /**
     * Dependency filter.
     */
    private String m_filter;

    /**
     * Is the dependency optional ?
     */
    private String m_optional;

    /**
     * Dependency specification.
     */
    private String m_specification;

    /**
     * Dependency id.
     */
    private String m_id;

    /**
     * Binding policy.
     */
    private String m_policy;

    /**
     * Default-Implementation attribute.
     */
    private String m_defaultImplementation;

    /**
     * Enable or Disable Nullable pattern.
     */
    private String m_nullable;

    /**
     * Comparator.
     */
    private String m_comparator;

    /**
     * From attribute.
     */
    private String m_from;

    /**
     * Proxy attribute.
     */
    private String m_proxy;

    /**
     * Constructor.
     * @param name : field name.
     */
    public RequiresVisitor(ComponentWorkbench workbench, String name) {
        this.workbench = workbench;
        this.m_field = name;
    }

    /**
     * Visit one "simple" annotation.
     * @param name : annotation name
     * @param value : annotation value
     * @see org.objectweb.asm.AnnotationVisitor#visit(java.lang.String, java.lang.Object)
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
        if (name.equals("nullable")) {
            m_nullable = value.toString();
            return;
        }
        if (name.equals("policy")) {
            m_policy = value.toString();
            return;
        }
        if (name.equals("defaultimplementation")) {
            Type type = Type.getType(value.toString());
            m_defaultImplementation = type.getClassName();
            return;
        }
        if (name.equals("specification")) {
            m_specification = value.toString();
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
            return;
        }
        if (name.equals("proxy")) {
            m_proxy = value.toString();
        }
    }

    /**
     * End of the annotation.
     * Create a "requires" element
     * @see org.objectweb.asm.AnnotationVisitor#visitEnd()
     */
    public void visitEnd() {
        Element requires = null;
        if (m_id == null) {
            requires = workbench.getIds().get(m_field);
        } else {
            requires = workbench.getIds().get(m_id);
        }

        if (requires == null) {
            requires = new Element("requires", "");
        }

        requires.addAttribute(new Attribute("field", m_field));
        if (m_specification != null) {
            requires.addAttribute(new Attribute("specification", m_specification));
        }
        if (m_filter != null) {
            requires.addAttribute(new Attribute("filter", m_filter));
        }
        if (m_optional != null) {
            requires.addAttribute(new Attribute("optional", m_optional));
        }
        if (m_nullable != null) {
            requires.addAttribute(new Attribute("nullable", m_nullable));
        }
        if (m_defaultImplementation != null) {
            requires.addAttribute(new Attribute("default-implementation", m_defaultImplementation));
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

        if (m_id != null) {
            workbench.getIds().put(m_id, requires);
        } else {
            workbench.getIds().put(m_field, requires);
        }

        workbench.getElements().put(requires, null);
    }
}