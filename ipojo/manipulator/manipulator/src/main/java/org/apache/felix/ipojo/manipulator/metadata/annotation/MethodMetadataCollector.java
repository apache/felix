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
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MethodMetadataCollector extends MethodVisitor {

    /**
     * Binding's registry.
     */
    private BindingRegistry registry;

    /**
     * The workbench currently in use.
     */
    private ComponentWorkbench workbench;

    /**
     * Visited field.
     */
    private MethodNode node;

    public MethodMetadataCollector(ComponentWorkbench workbench, MethodNode node, Reporter reporter) {
        super(Opcodes.ASM5);
        this.workbench = workbench;
        this.node = node;
        this.registry = workbench.getBindingRegistry();
    }

    /**
     * Visit method annotations.
     *
     * @param desc    : annotation name.
     * @param visible : is the annotation visible at runtime.
     * @return the visitor paring the visited annotation.
     * @see org.objectweb.asm.MethodVisitor#visitAnnotation(java.lang.String, boolean)
     */
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        // Return the visitor to be executed (may be null)
        return registry.selection(workbench)
                .method(this, node)
                .annotatedWith(desc)
                .get();

    }

    /**
     * Visit a parameter annotation.
     *
     * @see org.objectweb.asm.MethodVisitor#visitParameterAnnotation(int, java.lang.String, boolean)
     */
    public AnnotationVisitor visitParameterAnnotation(int index,
                                                      String desc,
                                                      boolean visible) {
        // Only process annotations on constructor
        if (node.name.equals("<init>")) {

            // Return the visitor to be executed (may be null)
            return registry.selection(workbench)
                    .parameter(this, node, index)
                    .annotatedWith(desc)
                    .get();

        }
        return super.visitParameterAnnotation(index, desc, visible);
    }


}
