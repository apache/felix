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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/**
 * Manipulates inner class allowing outer class access. The manipulated class
 * has access to managed field of the outer class.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class InnerClassManipulator {

    /**
     * The manipulator having manipulated the component class.
     */
    private final Manipulator m_manipulator;
    private final String m_innerClassName;

    /**
     * Outer class class name.
     */
    private String m_outer;

    /**
     * Component class fields.
     */
    private Set<String> m_fields;

    /**
     * Creates an inner class manipulator.
     * @param outerclassName : class name
     * @param manipulator : fields
     */
    public InnerClassManipulator(String innerClassName, String outerclassName, Manipulator manipulator) {
        m_outer = outerclassName;
        m_innerClassName = innerClassName;
        m_fields = manipulator.getFields().keySet();
        m_manipulator = manipulator;
    }

    /**
     * Manipulate the inner class.
     * @param in input (i.e. original) class
     * @return manipulated class
     * @throws IOException the class cannot be read correctly
     */
    public byte[] manipulate(byte[] in, int version) throws IOException {
        InputStream is1 = new ByteArrayInputStream(in);

        ClassReader cr = new ClassReader(is1);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        InnerClassAdapter adapter = new InnerClassAdapter(m_innerClassName, cw, m_outer, m_fields, m_manipulator);
        if (version >= Opcodes.V1_6) {
            cr.accept(adapter, ClassReader.EXPAND_FRAMES);
        } else {
            cr.accept(adapter, 0);
        }
        is1.close();

        return cw.toByteArray();
    }

}
