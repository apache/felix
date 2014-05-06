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

package org.apache.felix.ipojo.manipulator.spi;

import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.metadata.annotation.ComponentWorkbench;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.annotation.ElementType;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class BindingContext {

    public static final int NO_INDEX = -1;

    /**
     *
     */
    private ComponentWorkbench workbench;
    private FieldNode field;
    private MethodNode method;
    private ClassNode clazz;
    private ElementType elementType;
    private int parameterIndex;
    private Reporter reporter;
    private Type annotationType;
    private Object visitor;


    public BindingContext(final ComponentWorkbench workbench,
                          final Reporter reporter,
                          final Type annotationType,
                          final FieldNode node,
                          final ElementType elementType,
                          final int parameterIndex,
                          final Object visitor) {
        this(workbench, reporter, annotationType, elementType, parameterIndex, visitor);
        this.field = node;
    }

    public BindingContext(final ComponentWorkbench workbench,
                          final Reporter reporter,
                          final Type annotationType,
                          final MethodNode node,
                          final ElementType elementType,
                          final int parameterIndex,
                          final Object visitor) {
        this(workbench, reporter, annotationType, elementType, parameterIndex, visitor);
        this.method = node;
    }

    public BindingContext(final ComponentWorkbench workbench,
                          final Reporter reporter,
                          final Type annotationType,
                          final ClassNode node,
                          final ElementType elementType,
                          final int parameterIndex,
                          final Object visitor) {
        this(workbench, reporter, annotationType, elementType, parameterIndex, visitor);
        this.clazz = node;
    }

    private BindingContext(final ComponentWorkbench workbench,
                           final Reporter reporter,
                           final Type annotationType,
                           final ElementType elementType,
                           final int parameterIndex,
                           final Object visitor) {
        this.workbench = workbench;
        this.reporter = reporter;
        this.annotationType = annotationType;
        this.elementType = elementType;
        this.parameterIndex = parameterIndex;
        this.visitor = visitor;
    }

    public ComponentWorkbench getWorkbench() {
        return workbench;
    }

    public FieldNode getFieldNode() {
        return field;
    }

    public MethodNode getMethodNode() {
        return method;
    }

    public ClassNode getClassNode() {
        return clazz;
    }

    public ElementType getElementType() {
        return elementType;
    }

    public int getParameterIndex() {
        return parameterIndex;
    }

    public Reporter getReporter() {
        return reporter;
    }

    public Type getAnnotationType() {
        return annotationType;
    }

    public Object getVisitor() {
        return visitor;
    }

    /**
     * This method is just to support the compatibility with the previous version. It returns an ASM node of the
     * member on which the annotation is 'attached'. The type of the node depends on this element. It can be a {@link
     * org.objectweb.asm.tree.FieldNode}, a {@link org.objectweb.asm.tree.MethodNode} or a
     * {@link org.objectweb.asm.tree.ClassNode}. It actually checks the current node and returns the first set ones
     * in this order: field, method, class.
     * <p/>
     * The type of the returned object can be determined using the {@link #getElementType} method. However it is
     * recommended to use one of the following method: {@link #getFieldNode()}, {@link #getMethodNode()}  or
     * {@link #getClassNode()}.
     *
     * @return the node.
     * @deprecated
     */
    public Object getNode() {
        if (field != null) {
            return field;
        }
        if (method != null) {
            return method;
        }
        if (clazz != null) {
            return clazz;
        }
        // No node ?
        return null;
    }
}
