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
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 * @see org.apache.felix.ipojo.annotations.PostRegistration
 */
public class PostRegistrationVisitor extends AnnotationVisitor {

    private ComponentWorkbench workbench;
    private String name;

    public PostRegistrationVisitor(ComponentWorkbench workbench, String name) {
        super(Opcodes.ASM5);
        this.workbench = workbench;
        this.name = name;
    }

    @Override
    public void visitEnd() {
        Element provides;
        if (workbench.getIds().containsKey("provides")) {
            provides = workbench.getIds().get("provides");
            provides.addAttribute(new Attribute("post-registration", name));
        }
        // Else ignore annotation ...
    }
}
