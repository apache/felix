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

package org.apache.felix.ipojo.manipulator.metadata.annotation;

import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.metadata.annotation.registry.BindingRegistry;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ClassMetadataCollector extends ClassVisitor {

    /**
     * Binding's registry.
     */
    private BindingRegistry registry;

    /**
     * Output information.
     */
    private Reporter reporter;

    /**
     * Workbench where produced Elements will be merged and hierarchically organized.
     */
    private ComponentWorkbench workbench;

    /**
     * Class currently being analyzed.
     */
    private ClassNode node;

    private Element componentMetadata;

    private Element instanceMetadata;

    public ClassMetadataCollector(BindingRegistry registry, Reporter reporter) {
        super(Opcodes.ASM5);
        this.registry = registry;
        this.reporter = reporter;
        node = new ClassNode();
    }

    /**
     * Build metadata. May be {@literal null} if no "component type" was found.
     *
     * @return Build metadata. May be {@literal null} if no "component type" was found.
     */
    public Element getComponentMetadata() {
        return componentMetadata;
    }

    /**
     * Build instance metadata. May be {@literal null} if no "component type" was found.
     *
     * @return Build metadata. May be {@literal null} if no "component type" was found.
     */
    public Element getInstanceMetadata() {
        return instanceMetadata;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        node.visit(version, access, name, signature, superName, interfaces);
        workbench = new ComponentWorkbench(registry, node);
    }

    /**
     * Visit class annotations.
     * This method detects @component and @provides annotations.
     *
     * @param desc    : annotation descriptor.
     * @param visible : is the annotation visible at runtime.
     * @return the annotation visitor.
     * @see org.objectweb.asm.ClassVisitor#visitAnnotation(java.lang.String, boolean)
     */
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        //TODO we should find a better way to do this.
        // Cannot retrieve the class object as @Configuration is in iPOJO runtime.
        if (Type.getType(desc).getClassName().equals("org.apache.felix.ipojo.configuration.Configuration")) {
            workbench.ignore(true);
            return null;
        }

        // Return the visitor to be executed (may be null)
        return registry.selection(workbench)
                .type(this, node)
                .annotatedWith(desc)
                .get();

    }

    /**
     * Visit a field.
     * Call the field collector visitor.
     *
     * @param access    : field access.
     * @param name      : field name
     * @param desc      : field descriptor
     * @param signature : field signature
     * @param value     : field value (static field only)
     * @return the field visitor.
     * @see org.objectweb.asm.ClassVisitor#visitField(int, java.lang.String, java.lang.String, java.lang.String,
     * java.lang.Object)
     */
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        return new FieldMetadataCollector(workbench, new FieldNode(access, name, desc, signature, value));
    }

    /**
     * Visit a method.
     * Call the method collector visitor.
     *
     * @param access     : method access
     * @param name       : method name
     * @param desc       : method descriptor
     * @param signature  : method signature
     * @param exceptions : method exceptions
     * @return the Method Visitor.
     * @see org.objectweb.asm.ClassVisitor#visitMethod(int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
     */
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new MethodMetadataCollector(workbench, new MethodNode(access, name, desc, signature, exceptions), reporter);
    }

    /**
     * End of the visit : compute final elements.
     *
     * @see org.objectweb.asm.ClassVisitor#visitEnd()
     */
    @Override
    public void visitEnd() {
        // Only process real class (no annotations, no interfaces)
        if (!(is(Opcodes.ACC_ANNOTATION) || is(Opcodes.ACC_INTERFACE) || is(Opcodes.ACC_ABSTRACT))) {
            if (workbench.getRoot() == null) {
                // No 'top-level' element has been contributed

                if (workbench.ignore()) {
                    // Ignore this class.
                    return;
                }

                if (!workbench.getElements().isEmpty()) {
                    // There are other annotation's contribution on this type (additional handler declaration/configuration)
                    // That means that there is a missing 'component type' annotation

                    reporter.warn("Class %s has not been marked as a component type (no @Component, @Handler, " +
                                    "...). It will be ignored by the iPOJO manipulator.",
                            workbench.getType().getClassName()
                    );
                    return;
                } // else: no root and no elements
                return;
            }

            componentMetadata = workbench.build();
            instanceMetadata = workbench.getInstance();

            // If we have an instance declared and the component metadata has a name, we update the component's attribute
            // of the instance (https://issues.apache.org/jira/browse/FELIX-4052).
            if (componentMetadata != null && componentMetadata.containsAttribute("name") && instanceMetadata != null) {
                // Update the component attribute
                instanceMetadata.addAttribute(new Attribute("component", componentMetadata.getAttribute("name")));
            }

        }
    }

    private boolean is(int flags) {
        return (node.access & flags) == flags;
    }

}
