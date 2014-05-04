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

package org.apache.felix.ipojo.manipulator.metadata.annotation.registry;

import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.metadata.annotation.ComponentWorkbench;
import org.apache.felix.ipojo.manipulator.spi.BindingContext;
import org.apache.felix.ipojo.manipulator.util.ChainedAnnotationVisitor;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Selection} is used to select a subset of all supported {@link AnnotationVisitor}.
 * It's a query DSL.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Selection {

    private BindingRegistry registry;
    private ComponentWorkbench workbench;
    private Reporter reporter;
    private FieldNode field;
    private MethodNode method;
    private ClassNode clazz;
    private int index = BindingContext.NO_INDEX;
    private String annotation;
    private ElementType elementType = null;
    private Object visitor;

    public Selection(BindingRegistry registry, ComponentWorkbench workbench, Reporter reporter) {
        this.registry = registry;
        this.workbench = workbench;
        this.reporter = reporter;
    }

    public Selection field(FieldVisitor visitor, FieldNode node) {
        this.visitor = visitor;
        this.field = node;
        this.elementType = ElementType.FIELD;
        return this;
    }

    public Selection method(MethodVisitor visitor, MethodNode node) {
        this.visitor = visitor;
        this.method = node;
        this.elementType = ElementType.METHOD;
        return this;
    }

    public Selection type(ClassVisitor visitor, ClassNode node) {
        this.visitor = visitor;
        this.clazz = node;
        this.elementType = ElementType.TYPE;
        return this;
    }

    public Selection parameter(MethodVisitor visitor, MethodNode node, int index) {
        this.visitor = visitor;
        this.index = index;
        this.method = node;
        this.elementType = ElementType.PARAMETER;
        return this;
    }

    public Selection annotatedWith(String desc) {
        this.annotation = desc;
        return this;
    }

    public AnnotationVisitor get() {
        List<AnnotationVisitor> visitors = list();

        if (visitors.isEmpty()) {
            return null;
        }

        if (visitors.size() == 1) {
            return visitors.get(0);
        }

        ChainedAnnotationVisitor chained = new ChainedAnnotationVisitor();
        chained.getVisitors().addAll(visitors);
        return chained;
    }

    private List<AnnotationVisitor> list() {
        BindingContext context;

        if (elementType == ElementType.FIELD) {
            context = new BindingContext(workbench, reporter, Type.getType(annotation), field,
                    elementType, index, visitor);
        } else if (elementType == ElementType.TYPE) {
            context = new BindingContext(workbench, reporter, Type.getType(annotation), clazz,
                    elementType, index, visitor);
        } else {
            // Parameter of method.
            context = new BindingContext(workbench, reporter, Type.getType(annotation), method,
                    elementType, index, visitor);
        }


        List<Binding> predicates = registry.getBindings(annotation);

        List<AnnotationVisitor> visitors = new ArrayList<AnnotationVisitor>();
        if (predicates != null && !predicates.isEmpty()) {
            collectMatchingVisitors(predicates, context, visitors);
        }
        return visitors;
    }


    private void collectMatchingVisitors(List<Binding> bindings, BindingContext context, List<AnnotationVisitor> visitors) {
        for (Binding binding : bindings) {
            if (binding.getPredicate().matches(context)) {
                AnnotationVisitor visitor = binding.getFactory().newAnnotationVisitor(context);
                if (visitor != null) {
                    visitors.add(visitor);
                }
            }
        }
    }

}
