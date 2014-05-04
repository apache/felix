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

package org.apache.felix.ipojo.manipulator.metadata.annotation.visitor;

import org.apache.felix.ipojo.manipulator.metadata.annotation.ComponentWorkbench;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Parse @Validate and @Invalidate annotations.
 * @see org.apache.felix.ipojo.annotations.Validate
 * @see org.apache.felix.ipojo.annotations.Invalidate
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class LifecycleVisitor extends AnnotationVisitor {

    public static enum Transition {
        VALIDATE, INVALIDATE
    }

    private ComponentWorkbench workbench;
    private String name;
    private Transition transition;

    public LifecycleVisitor(ComponentWorkbench workbench, String name, Transition transition) {
        super(Opcodes.ASM5);
        this.workbench = workbench;
        this.name = name;
        this.transition = transition;
    }

    @Override
    public void visitEnd() {
        Element cb = new Element("callback", "");
        cb.addAttribute(new Attribute("transition", transition.name().toLowerCase()));
        cb.addAttribute(new Attribute("method", name));
        workbench.getElements().put(cb, null);
    }
}
