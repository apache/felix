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
<<<<<<< HEAD
import org.objectweb.asm.commons.EmptyVisitor;
=======
import org.objectweb.asm.Opcodes;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

/**
 * Parses the @Controller annotation.
 * @see org.apache.felix.ipojo.annotations.Controller
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
<<<<<<< HEAD
public class ControllerVisitor extends EmptyVisitor implements AnnotationVisitor {
=======
public class ControllerVisitor extends AnnotationVisitor {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    private ComponentWorkbench workbench;

    private String field;

    public ControllerVisitor(ComponentWorkbench workbench, String field) {
<<<<<<< HEAD
=======
        super(Opcodes.ASM5);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        this.workbench = workbench;
        this.field = field;
    }

    /**
     * Visit @Handler annotation attributes.
<<<<<<< HEAD
     * @see org.objectweb.asm.commons.EmptyVisitor#visit(String, Object)
=======
     * @see org.objectweb.asm.AnnotationVisitor#visit(String, Object)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
     */
    public void visitEnd() {
        Element controller = new Element("controller", "");
        controller.addAttribute(new Attribute("field", field));
        workbench.getElements().put(controller, null);
    }
}
