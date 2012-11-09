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

package org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.util;

import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.metadata.annotation.registry.Binding;
import org.apache.felix.ipojo.manipulator.metadata.annotation.registry.BindingRegistry;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.generic.FieldGenericVisitor;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.generic.MethodGenericVisitor;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.generic.ParameterGenericVisitor;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.generic.TypeGenericVisitor;
import org.apache.felix.ipojo.manipulator.spi.AnnotationVisitorFactory;
import org.apache.felix.ipojo.manipulator.spi.BindingContext;
import org.apache.felix.ipojo.manipulator.spi.Module;
import org.apache.felix.ipojo.manipulator.spi.Predicate;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ServiceLoader;
import java.util.regex.Pattern;

import static org.apache.felix.ipojo.manipulator.spi.helper.Predicates.and;
import static org.apache.felix.ipojo.manipulator.spi.helper.Predicates.on;
import static org.apache.felix.ipojo.manipulator.spi.helper.Predicates.pattern;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Bindings {

    private static List<Binding> DEFAULT_BINDINGS;
    static {
        DEFAULT_BINDINGS = new ArrayList<Binding>();
        DEFAULT_BINDINGS.add(newGenericTypeBinding());
        DEFAULT_BINDINGS.add(newGenericFieldBinding());
        DEFAULT_BINDINGS.add(newGenericMethodBinding());
        DEFAULT_BINDINGS.add(newGenericParameterBinding());
        DEFAULT_BINDINGS = Collections.unmodifiableList(DEFAULT_BINDINGS);
    }

    private static Binding newGenericTypeBinding() {
        Binding binding = new Binding();
        // ElementType is TYPE
        // Annotation descriptor is matching generic pattern
        binding.setPredicate(
                and(
                        on(ElementType.TYPE),
                        customAnnotationPattern()
                ));
        binding.setFactory(new AnnotationVisitorFactory() {
            public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                Element element = Elements.buildElement(context.getAnnotationType());
                return new TypeGenericVisitor(context.getWorkbench(), element);
            }
        });
        return binding;
    }

    private static Predicate customAnnotationPattern() {
        return pattern("(.*\\.ipojo\\..*)|(.*\\.handler\\..*)").matches();
    }

    private static Binding newGenericFieldBinding() {
        Binding binding = new Binding();
        // ElementType is FIELD
        // Annotation descriptor is matching generic pattern
        binding.setPredicate(
                and(
                        on(ElementType.FIELD),
                        customAnnotationPattern()
                ));
        binding.setFactory(new AnnotationVisitorFactory() {
            public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                Element element = Elements.buildElement(context.getAnnotationType());
                return new FieldGenericVisitor(context.getWorkbench(), element, (FieldNode) context.getNode());
            }
        });
        return binding;
    }

    private static Binding newGenericMethodBinding() {
        Binding binding = new Binding();
        // ElementType is METHOD
        // Annotation descriptor is matching generic pattern
        binding.setPredicate(
                and(
                        on(ElementType.METHOD),
                        customAnnotationPattern()
                ));
        binding.setFactory(new AnnotationVisitorFactory() {
            public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                Element element = Elements.buildElement(context.getAnnotationType());
                return new MethodGenericVisitor(context.getWorkbench(), element, (MethodNode) context.getNode());
            }
        });
        return binding;
    }

    private static Binding newGenericParameterBinding() {
        Binding binding = new Binding();
        // ElementType is METHOD
        // Annotation descriptor is matching generic pattern
        binding.setPredicate(
                and(
                        on(ElementType.PARAMETER),
                        customAnnotationPattern()
                ));
        binding.setFactory(new AnnotationVisitorFactory() {
            public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                Element element = Elements.buildElement(context.getAnnotationType());
                return new ParameterGenericVisitor(context.getWorkbench(), element, (MethodNode) context.getNode(), context.getParameterIndex());
            }
        });
        return binding;
    }


    public static BindingRegistry newBindingRegistry(Reporter reporter) {

        BindingRegistry registry = new BindingRegistry(reporter);
        ServiceLoader<Module> loader = ServiceLoader.load(Module.class, classloader());

        // Build each Module and add its contributed Bindings in the registry
        for (Module module : loader) {
            module.configure();
            registry.addBindings(module);
        }

        // Do not forget the default Bindings
        registry.getDefaultBindings().addAll(DEFAULT_BINDINGS);

        return registry;
    }

    public static List<Binding> getDefaultBindings() {
        return DEFAULT_BINDINGS;
    }

    private static ClassLoader classloader() {
        return Bindings.class.getClassLoader();
    }
}
