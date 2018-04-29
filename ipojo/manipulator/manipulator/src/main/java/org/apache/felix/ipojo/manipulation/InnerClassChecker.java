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

package org.apache.felix.ipojo.manipulation;

<<<<<<< HEAD
import org.objectweb.asm.*;
import org.objectweb.asm.commons.EmptyVisitor;

import java.util.*;
=======
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

/**
 * Analyze an inner class.
 * This visit collects the methods from the inner class.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
<<<<<<< HEAD
public class InnerClassChecker extends EmptyVisitor implements ClassVisitor, Opcodes {
=======
public class InnerClassChecker extends ClassVisitor implements Opcodes {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    private final String m_name;
    private final Manipulator m_manipulator;

    public InnerClassChecker(String name, Manipulator manipulator) {
<<<<<<< HEAD
=======
        super(Opcodes.ASM5);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        m_name = name;
        m_manipulator = manipulator;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        // Do not collect static and native method.
<<<<<<< HEAD

=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        if ((access & ACC_STATIC) == ACC_STATIC) {
            return null;
        }

        if ((access & ACC_NATIVE) == ACC_NATIVE) {
            return null;
        }

        // Don't add generated methods, and constructors
        if (!ClassChecker.isGeneratedMethod(name, desc)  && ! name.endsWith("<init>")) {
            final MethodDescriptor md = new MethodDescriptor(name, desc, (access & ACC_STATIC) == ACC_STATIC);
            m_manipulator.addMethodToInnerClass(m_name, md);
        }

        return null;
    }
}
