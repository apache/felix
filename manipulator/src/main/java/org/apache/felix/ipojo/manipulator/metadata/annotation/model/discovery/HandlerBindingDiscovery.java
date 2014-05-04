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

package org.apache.felix.ipojo.manipulator.metadata.annotation.model.discovery;

import org.apache.felix.ipojo.annotations.HandlerBinding;
import org.apache.felix.ipojo.manipulator.metadata.annotation.model.AnnotationDiscovery;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * The annotation visitor responsible for parsing the {@link org.apache.felix.ipojo.annotations.HandlerBinding}
 * annotation.
 */
public class HandlerBindingDiscovery extends AnnotationVisitor implements AnnotationDiscovery {

    public static final String HANDLER_BINDING_DESCRIPTOR = Type.getType(HandlerBinding.class).getDescriptor();

    private boolean m_handlerBinding = false;
    private String m_value = null;
    private String m_namespace = null;

    /**
     * Constructs a new {@link org.objectweb.asm.AnnotationVisitor}.
     */
    public HandlerBindingDiscovery() {
        super(Opcodes.ASM5);
    }

    public AnnotationVisitor visitAnnotation(final String desc) {
        if (HANDLER_BINDING_DESCRIPTOR.equals(desc)) {
            m_handlerBinding = true;
            return this;
        }
        return null;
    }

    @Override
    public void visit(final String name, final Object value) {
        if ("value".equals(name)) {
            m_value = (String) value;
        }
        if ("namespace".equals(name)) {
            m_namespace = (String) value;
        }
    }

    public boolean isHandlerBinding() {
        return m_handlerBinding;
    }

    public String getValue() {
        return m_value;
    }

    public String getNamespace() {
        return m_namespace;
    }
}
