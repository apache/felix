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
import org.apache.felix.ipojo.manipulator.ResourceStore;
import org.apache.felix.ipojo.manipulator.metadata.annotation.registry.AnnotationRegistry;
import org.apache.felix.ipojo.manipulator.metadata.annotation.stereotype.StereotypeParser;
import org.apache.felix.ipojo.manipulator.metadata.annotation.registry.Binding;
import org.apache.felix.ipojo.manipulator.metadata.annotation.registry.BindingRegistry;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.stereotype.FieldStereotypeVisitor;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.stereotype.MethodStereotypeVisitor;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.stereotype.ParameterStereotypeVisitor;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.stereotype.TypeStereotypeVisitor;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.generic.FieldGenericVisitor;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.generic.MethodGenericVisitor;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.generic.ParameterGenericVisitor;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.generic.TypeGenericVisitor;
import org.apache.felix.ipojo.manipulator.spi.AnnotationVisitorFactory;
import org.apache.felix.ipojo.manipulator.spi.BindingContext;
import org.apache.felix.ipojo.manipulator.spi.Module;
import org.apache.felix.ipojo.manipulator.spi.Predicate;
import org.apache.felix.ipojo.manipulator.util.Strings;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import static org.apache.felix.ipojo.manipulator.spi.helper.Predicates.and;
import static org.apache.felix.ipojo.manipulator.spi.helper.Predicates.on;
import static org.apache.felix.ipojo.manipulator.spi.helper.Predicates.or;
import static org.apache.felix.ipojo.manipulator.spi.helper.Predicates.pattern;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Bindings {

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


    public static BindingRegistry newBindingRegistry(Reporter reporter, ResourceStore store) {

        AnnotationRegistry ar = new AnnotationRegistry();
        BindingRegistry registry = new BindingRegistry(reporter);
        ServiceLoader<Module> loader = ServiceLoader.load(Module.class, classloader());

        // Build each Module and add its contributed Bindings in the registry
        for (Module module : loader) {
            module.configure();
            registry.addBindings(module);
        }

        // Do not forget the default Bindings
        registry.getDefaultBindings().addAll(newDefaultBindings(store, ar));

        return registry;
    }

    public static List<Binding> newDefaultBindings(final ResourceStore store, final AnnotationRegistry ar) {
        List<Binding> bindings = new ArrayList<Binding>();

        // Register Stereotype binding support first
        // That allows iPOJO to provide its own stereotyped annotations
        bindings.add(newStereotypeTypeBinding(store, ar));
        bindings.add(newStereotypeFieldBinding(store, ar));
        bindings.add(newStereotypeMethodBinding(store, ar));
        bindings.add(newStereotypeParameterBinding(store, ar));

        // Then register the generic bindings
        bindings.add(newGenericTypeBinding());
        bindings.add(newGenericFieldBinding());
        bindings.add(newGenericMethodBinding());
        bindings.add(newGenericParameterBinding());
        return bindings;

    }

    private static Binding newStereotypeParameterBinding(final ResourceStore store, final AnnotationRegistry registry) {
        Binding binding = new Binding();
        binding.setPredicate(
                and(
                        on(ElementType.PARAMETER),
                        stereotype(store, registry)
                ));
        binding.setFactory(new AnnotationVisitorFactory() {
            public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                return new ParameterStereotypeVisitor((MethodVisitor) context.getVisitor(),
                                                      context.getParameterIndex(),
                                                      registry.getRecorders(context.getAnnotationType()));
            }
        });
        return binding;
    }

    private static Binding newStereotypeMethodBinding(final ResourceStore store, final AnnotationRegistry registry) {
        Binding binding = new Binding();
        binding.setPredicate(
                and(
                        on(ElementType.METHOD),
                        stereotype(store, registry)
                ));
        binding.setFactory(new AnnotationVisitorFactory() {
            public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                return new MethodStereotypeVisitor((MethodVisitor) context.getVisitor(),
                                                  registry.getRecorders(context.getAnnotationType()));
            }
        });
        return binding;
    }

    private static Binding newStereotypeFieldBinding(final ResourceStore store, final AnnotationRegistry registry) {
        Binding binding = new Binding();
        binding.setPredicate(
                and(
                        on(ElementType.FIELD),
                        stereotype(store, registry)
                ));
        binding.setFactory(new AnnotationVisitorFactory() {
            public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                return new FieldStereotypeVisitor((FieldVisitor) context.getVisitor(),
                                                  registry.getRecorders(context.getAnnotationType()));
            }
        });
        return binding;
    }

    private static Binding newStereotypeTypeBinding(final ResourceStore store, final AnnotationRegistry registry) {
        Binding binding = new Binding();
        binding.setPredicate(
                and(
                        on(ElementType.TYPE),
                        stereotype(store, registry)
                ));
        binding.setFactory(new AnnotationVisitorFactory() {
            public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                return new TypeStereotypeVisitor((ClassVisitor) context.getVisitor(),
                                                 registry.getRecorders(context.getAnnotationType()));
            }
        });
        return binding;
    }

    private static Predicate stereotype(final ResourceStore store, final AnnotationRegistry registry) {
        return new Predicate() {
            public boolean matches(final BindingContext context) {

                Type type = context.getAnnotationType();
                if (registry.isUnknown(type)) {
                    // The given annotation type was never parsed before
                    try {

                        // Try to read the annotation's byte code
                        byte[] bytes = store.read(Strings.asResourcePath(context.getAnnotationType().getClassName()));
                        if (bytes != null) {
                            StereotypeParser parser = new StereotypeParser();
                            parser.read(bytes);
                            if (parser.isStereotype()) {
                                registry.addStereotype(type, parser.getRecorders());
                                return true;
                            } else {
                                registry.addUnbound(type);
                                return false;
                            }
                        }
                        registry.addUnbound(type);
                        return false;
                    } catch (IOException e) {
                        // Cannot load the byte code, assume it's not a stereotype
                        // TODO print a warning ?
                        registry.addUnbound(type);
                        return false;
                    }
                }
                return registry.isStereotype(type);
            }
        };
    }

    private static ClassLoader classloader() {
        return Bindings.class.getClassLoader();
    }
}
