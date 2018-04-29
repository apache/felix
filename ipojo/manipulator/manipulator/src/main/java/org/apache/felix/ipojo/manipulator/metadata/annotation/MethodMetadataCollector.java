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
<<<<<<< HEAD
import org.objectweb.asm.commons.EmptyVisitor;
=======
import org.objectweb.asm.Opcodes;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import org.objectweb.asm.tree.MethodNode;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
<<<<<<< HEAD
public class MethodMetadataCollector extends EmptyVisitor implements MethodVisitor {
=======
public class MethodMetadataCollector extends MethodVisitor {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    /**
     * Binding's registry.
     */
    private BindingRegistry registry;

    /**
<<<<<<< HEAD
     * Output informations.
     */
    private Reporter reporter;

    /**
=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
     * The workbench currently in use.
     */
    private ComponentWorkbench workbench;

    /**
     * Visited field.
     */
    private MethodNode node;

    public MethodMetadataCollector(ComponentWorkbench workbench, MethodNode node, Reporter reporter) {
<<<<<<< HEAD
        this.workbench = workbench;
        this.reporter = reporter;
=======
        super(Opcodes.ASM5);
        this.workbench = workbench;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        this.node = node;
        this.registry = workbench.getBindingRegistry();
    }

    /**
     * Visit method annotations.
     *
<<<<<<< HEAD
     * @param desc : annotation name.
     * @param visible : is the annotation visible at runtime.
     * @return the visitor paring the visited annotation.
     * @see org.objectweb.asm.commons.EmptyVisitor#visitAnnotation(java.lang.String, boolean)
     */
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {

=======
     * @param desc    : annotation name.
     * @param visible : is the annotation visible at runtime.
     * @return the visitor paring the visited annotation.
     * @see org.objectweb.asm.MethodVisitor#visitAnnotation(java.lang.String, boolean)
     */
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        // Return the visitor to be executed (may be null)
        return registry.selection(workbench)
                .method(this, node)
                .annotatedWith(desc)
                .get();

    }

    /**
     * Visit a parameter annotation.
     *
<<<<<<< HEAD
     * @see org.objectweb.asm.commons.EmptyVisitor#visitParameterAnnotation(int, java.lang.String, boolean)
=======
     * @see org.objectweb.asm.MethodVisitor#visitParameterAnnotation(int, java.lang.String, boolean)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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


<<<<<<< HEAD

=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
}
