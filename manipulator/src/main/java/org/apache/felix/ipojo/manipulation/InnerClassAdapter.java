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

import org.objectweb.asm.*;

import java.util.Set;

/**
 * Adapts a inner class in order to allow accessing outer class fields.
 * A manipulated inner class has access to the managed field of the outer class.
 *
 * Only non-static inner classes are manipulated, others are not.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class InnerClassAdapter extends ClassAdapter implements Opcodes {

    private final Manipulator m_manipulator;
    private final String m_name;
    /**
     * Implementation class name.
     */
    private String m_outer;
    /**
     * List of fields of the implementation class.
     */
    private Set<String> m_fields;

    /**
     * Creates the inner class adapter.
     *
     * @param name      the inner class name
     * @param arg0       parent class visitor
     * @param outerClass outer class (implementation class)
     * @param fields     fields of the implementation class
     * @param manipulator the manipulator having manipulated the outer class.
     */
    public InnerClassAdapter(String name, ClassVisitor arg0, String outerClass, Set<String> fields,
                             Manipulator manipulator) {
        super(arg0);
        m_name = name;
        m_outer = outerClass;
        m_fields = fields;
        m_manipulator = manipulator;
    }

    /**
     * Visits a method.
     * This methods create a code visitor manipulating outer class field accesses.
     *
     * @param access     method visibility
     * @param name       method name
     * @param desc       method descriptor
     * @param signature  method signature
     * @param exceptions list of exceptions thrown by the method
     * @return a code adapter manipulating field accesses
     * @see org.objectweb.asm.ClassAdapter#visitMethod(int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
     */
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {

        final MethodDescriptor md = new MethodDescriptor(name, desc, (access & ACC_STATIC) == ACC_STATIC);
        m_manipulator.addMethodToInnerClass(m_name, md);

        // Do nothing on static methods, should not happen in non-static inner classes.
        if ((access & ACC_STATIC) == ACC_STATIC) {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }

        // Do nothing on native methods
        if ((access & ACC_NATIVE) == ACC_NATIVE) {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }

        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
        if (! m_manipulator.isAlreadyManipulated()) {
            // Do not re-manipulate.
            return new MethodCodeAdapter(mv, m_outer, access, name, desc, m_fields);
        } else {
            return mv;
        }
    }


}
