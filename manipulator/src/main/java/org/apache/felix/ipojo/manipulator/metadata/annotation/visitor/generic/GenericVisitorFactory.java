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

package org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.generic;

import org.apache.felix.ipojo.manipulator.spi.AnnotationVisitorFactory;
import org.apache.felix.ipojo.manipulator.spi.BindingContext;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

/**
* User: guillaume
* Date: 11/07/13
* Time: 14:41
*/
public class GenericVisitorFactory implements AnnotationVisitorFactory {
    private final String m_name;
    private final String m_namespace;

    public GenericVisitorFactory(final String name, final String namespace) {
        m_name = name;
        m_namespace = namespace;
    }

    // Need to build a new Element instance for each created visitor
    public AnnotationVisitor newAnnotationVisitor(BindingContext context) {
        System.out.println("Create annotation visitor for " + context.getNode());
        if (context.getNode() instanceof ClassNode) {
            return new TypeGenericVisitor(context.getWorkbench(),
                                          new Element(m_name, m_namespace));
        } else if (context.getNode() instanceof FieldNode) {
            return new FieldGenericVisitor(context.getWorkbench(),
                                           new Element(m_name, m_namespace),
                                           (FieldNode) context.getNode());

        } else if ((context.getNode() instanceof MethodNode) &&
                (context.getParameterIndex() == BindingContext.NO_INDEX)) {
            return new MethodGenericVisitor(context.getWorkbench(),
                                            new Element(m_name, m_namespace),
                                            (MethodNode) context.getNode());
        } else {
            // last case: method parameter annotation
            return new ParameterGenericVisitor(context.getWorkbench(),
                                               new Element(m_name, m_namespace),
                                               (MethodNode) context.getNode(),
                                               context.getParameterIndex());
        }
    }

    @Override
    public String toString() {
        return "GenericVisitorFactory";
    }

}
