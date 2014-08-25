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

package org.apache.felix.ipojo.manipulator.metadata.annotation.module;

import org.apache.felix.ipojo.annotations.*;
import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.metadata.annotation.ComponentWorkbench;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.*;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.bind.Action;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.bind.MethodBindVisitor;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.bind.ParameterBindVisitor;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.generic.GenericVisitorFactory;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.util.Elements;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.util.Names;
import org.apache.felix.ipojo.manipulator.spi.AbsBindingModule;
import org.apache.felix.ipojo.manipulator.spi.AnnotationVisitorFactory;
import org.apache.felix.ipojo.manipulator.spi.BindingContext;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.tree.MethodNode;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.lang.annotation.ElementType;

import static org.apache.felix.ipojo.manipulator.spi.helper.Predicates.on;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DefaultBindingModule extends AbsBindingModule {

    /**
     * Configure all the iPOJO's default annotation's bindings.
     */
    public void configure() {

        // Class level annotations
        // --------------------------------
        bind(Component.class)
                .to(new AnnotationVisitorFactory() {
                    public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                        return new ComponentVisitor(context.getWorkbench(), context.getReporter());
                    }
                });

        bind(Handler.class)
                .to(new AnnotationVisitorFactory() {
                    public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                        return new HandlerVisitor(context.getWorkbench(), context.getReporter());
                    }
                });

        bind(Provides.class)
                .to(new AnnotationVisitorFactory() {
                    public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                        return new ProvidesVisitor(context.getWorkbench());
                    }
                });


        bind(HandlerDeclaration.class)
                .to(new AnnotationVisitorFactory() {
                    public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                        Reporter reporter = context.getReporter();
                        return new HandlerDeclarationVisitor(context.getWorkbench(), getFreshDocumentBuilder(reporter), reporter);
                    }
                });

        bind(Instantiate.class)
                .to(new AnnotationVisitorFactory() {
                    public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                        return new InstantiateVisitor(context.getWorkbench());
                    }
                });

        // Field level annotations
        // --------------------------------
        bind(Requires.class)
                .when(on(ElementType.FIELD))
                .to(new AnnotationVisitorFactory() {
                    public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                        return new RequiresVisitor(context.getWorkbench(), context.getFieldNode().name);
                    }
                })
                .when(on(ElementType.PARAMETER))
                .to(new AnnotationVisitorFactory() {
                    public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                        return new ParameterBindVisitor(context.getWorkbench(), Action.BIND, context.getParameterIndex());
                    }
                });

        bind(Controller.class)
                .to(new AnnotationVisitorFactory() {
                    public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                        return new ControllerVisitor(context.getWorkbench(), context.getFieldNode().name);
                    }
                });

        bind(ServiceProperty.class)
                .to(new AnnotationVisitorFactory() {
                    public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                        String name = context.getFieldNode().name;
                        ComponentWorkbench workbench = context.getWorkbench();

                        if (!workbench.getIds().containsKey("provides")) {
                            // The provides annotation is already computed.
                            context.getReporter().warn("The component does not provide services, skip ServiceProperty for {}", name);
                            return null;
                        } else {
                            // Get the provides element
                            Element provides = workbench.getIds().get("provides");
                            return new FieldPropertyVisitor(name, provides);
                        }

                    }
                });

        bind(ServiceController.class)
                .to(new AnnotationVisitorFactory() {
                    public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                        String name = context.getFieldNode().name;
                        ComponentWorkbench workbench = context.getWorkbench();

                        if (!workbench.getIds().containsKey("provides")) { // The provides annotation is already computed.
                            context.getReporter().warn("The component does not provide services, skip @ServiceController for {}", name);
                            return null;
                        } else {
                            // Get the provides element
                            Element provides = workbench.getIds().get("provides");
                            return new ServiceControllerVisitor(name, provides);
                        }

                    }
                });

        bind(Property.class)
                .when(on(ElementType.FIELD))
                .to(new AnnotationVisitorFactory() {
                    public AnnotationVisitor newAnnotationVisitor(BindingContext context) {

                        ComponentWorkbench workbench = context.getWorkbench();
                        Element properties = Elements.getPropertiesElement(workbench);
                        String name = context.getFieldNode().name;
                        return new FieldPropertyVisitor(name, properties);
                    }

                })
                .when(on(ElementType.METHOD))
                .to(new AnnotationVisitorFactory() {
                    public AnnotationVisitor newAnnotationVisitor(BindingContext context) {

                        ComponentWorkbench workbench = context.getWorkbench();
                        // @Property on method parameter
                        Element properties = Elements.getPropertiesElement(workbench);
                        String name = context.getMethodNode().name;
                        return new MethodPropertyVisitor(properties, name);
                    }
                })
                .when(on(ElementType.PARAMETER))
                .to(new AnnotationVisitorFactory() {
                    public AnnotationVisitor newAnnotationVisitor(BindingContext context) {

                        ComponentWorkbench workbench = context.getWorkbench();
                        // @Property on method parameter
                        Element properties = Elements.getPropertiesElement(workbench);
                        MethodNode method = context.getMethodNode();
                        return new ParameterPropertyVisitor(properties, method, context.getParameterIndex());
                    }
                });

        bind(Validate.class)
                .to(new AnnotationVisitorFactory() {
                    public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                        MethodNode node = context.getMethodNode();
                        return new LifecycleVisitor(context.getWorkbench(),
                                Names.computeEffectiveMethodName(node.name),
                                LifecycleVisitor.Transition.VALIDATE);
                    }
                });

        bind(Invalidate.class)
                .to(new AnnotationVisitorFactory() {
                    public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                        MethodNode node = context.getMethodNode();
                        return new LifecycleVisitor(context.getWorkbench(),
                                Names.computeEffectiveMethodName(node.name),
                                LifecycleVisitor.Transition.INVALIDATE);
                    }
                });

        bind(Updated.class)
                .to(new AnnotationVisitorFactory() {
                    public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                        MethodNode node = context.getMethodNode();
                        return new UpdatedVisitor(context.getWorkbench(),
                                Names.computeEffectiveMethodName(node.name));
                    }
                });

        bind(Bind.class)
                .to(new AnnotationVisitorFactory() {
                    public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                        MethodNode node = context.getMethodNode();
                        return new MethodBindVisitor(context.getWorkbench(), Action.BIND, node, context.getReporter());
                    }
                });

        bind(Unbind.class)
                .to(new AnnotationVisitorFactory() {
                    public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                        MethodNode node = context.getMethodNode();
                        return new MethodBindVisitor(context.getWorkbench(), Action.UNBIND, node, context.getReporter());
                    }
                });

        bind(Modified.class)
                .to(new AnnotationVisitorFactory() {
                    public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                        MethodNode node = context.getMethodNode();
                        return new MethodBindVisitor(context.getWorkbench(), Action.MODIFIED, node, context.getReporter());
                    }
                });

        bind(PostRegistration.class)
                .to(new AnnotationVisitorFactory() {
                    public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                        MethodNode node = context.getMethodNode();
                        return new PostRegistrationVisitor(context.getWorkbench(), node.name);
                    }
                });

        bind(PostUnregistration.class)
                .to(new AnnotationVisitorFactory() {
                    public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                        MethodNode node = context.getMethodNode();
                        return new PostUnregistrationVisitor(context.getWorkbench(), node.name);
                    }
                });

        bind(Context.class).to(new GenericVisitorFactory("context", ""));
    }

    private DocumentBuilder m_builder;


    /**
     * Creates a 'fresh' document builder.
     * @return a new document builder is not already created, else reset
     * the created one, and return it.
     */
    protected DocumentBuilder getFreshDocumentBuilder(Reporter reporter) {
        if (m_builder == null) {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            try {
                m_builder = factory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                // TODO GSA is this acceptable to throw a RuntimeException here ?
                reporter.warn("Cannot get a fresh DocumentBuilder", e);
            }

            return m_builder;
        }

        // The builder has to be reset
        m_builder.reset();

        return m_builder;
    }
}
