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
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.generic.FieldGenericVisitor;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.generic.MethodGenericVisitor;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.generic.ParameterGenericVisitor;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.generic.TypeGenericVisitor;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.util.Elements;
import org.apache.felix.ipojo.manipulator.spi.AnnotationVisitorFactory;
import org.apache.felix.ipojo.manipulator.spi.BindingContext;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.regex.Pattern;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.felix.ipojo.manipulator.spi.helper.Predicates.alwaysTrue;

/**
 * User: guillaume
 * Date: 11/07/13
 * Time: 16:09
 */
public class LegacyGenericBindingRegistry extends CompletableBindingRegistry {
    public static final Pattern CUSTOM_HANDLER_PATTERN = Pattern.compile("(.*\\.ipojo\\..*)|(.*\\.handler\\..*)");

    public LegacyGenericBindingRegistry(final BindingRegistry delegate, final Reporter reporter) {
        super(delegate, reporter);
    }

    @Override
    protected List<Binding> createBindings(final Type type) {
        if (CUSTOM_HANDLER_PATTERN.matcher(type.getClassName()).matches()) {
            Binding binding = new Binding();
            binding.setAnnotationType(type);
            binding.setPredicate(alwaysTrue());
            binding.setFactory(new AnnotationVisitorFactory() {
                // Need to build a new Element instance for each created visitor
                public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
                    if (context.getClassNode() != null) {
                        return new TypeGenericVisitor(context.getWorkbench(),
                                Elements.buildElement(type));
                    } else if (context.getFieldNode() != null) {
                        return new FieldGenericVisitor(context.getWorkbench(),
                                Elements.buildElement(type),
                                context.getFieldNode());

                    } else if ((context.getMethodNode() != null) &&
                            (context.getParameterIndex() == BindingContext.NO_INDEX)) {
                        return new MethodGenericVisitor(context.getWorkbench(),
                                Elements.buildElement(type),
                                context.getMethodNode());
                    } else {
                        // last case: method parameter annotation
                        return new ParameterGenericVisitor(context.getWorkbench(),
                                Elements.buildElement(type),
                                context.getMethodNode(),
                                context.getParameterIndex());
                    }
                }

                @Override
                public String toString() {
                    return "LegacyGenericVisitorFactory";
                }
            });

            // Return the produced generic binding
            return singletonList(binding);
        }

        return emptyList();
    }
}
