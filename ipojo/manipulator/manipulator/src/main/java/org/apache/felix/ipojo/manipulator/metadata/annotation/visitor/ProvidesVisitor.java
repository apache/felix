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
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * Parse the @Provides annotation.
 * @see org.apache.felix.ipojo.annotations.Provides
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ProvidesVisitor extends EmptyVisitor implements AnnotationVisitor {
=======
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Parse the @Provides annotation.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 * @see org.apache.felix.ipojo.annotations.Provides
 */
public class ProvidesVisitor extends AnnotationVisitor {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    private ComponentWorkbench workbench;

    /**
     * Provides element.
     */
    private Element m_prov = new Element("provides", "");

    public ProvidesVisitor(ComponentWorkbench workbench) {
<<<<<<< HEAD
=======
        super(Opcodes.ASM5);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        this.workbench = workbench;
    }

    /**
     * Visit @provides annotation attributes.
<<<<<<< HEAD
     * @param name : annotation attribute name
     * @param value : annotation attribute value
     * @see org.objectweb.asm.commons.EmptyVisitor#visit(java.lang.String, java.lang.Object)
=======
     *
     * @param name  : annotation attribute name
     * @param value : annotation attribute value
     * @see org.objectweb.asm.AnnotationVisitor#visit(java.lang.String, java.lang.Object)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
     */
    public void visit(String name, Object value) {
        if (name.equals("factory")) { // Should be deprecated
            m_prov.addAttribute(new Attribute("factory", value.toString()));
        }
        if (name.equals("strategy")) {
            m_prov.addAttribute(new Attribute("strategy", value.toString()));
        }
    }

    /**
     * Visit specifications array.
<<<<<<< HEAD
     * @param name : attribute name
     * @return a visitor visiting each element of the array.
     * @see org.objectweb.asm.commons.EmptyVisitor#visitArray(java.lang.String)
=======
     *
     * @param name : attribute name
     * @return a visitor visiting each element of the array.
     * @see org.objectweb.asm.AnnotationVisitor#visitArray(java.lang.String)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
     */
    public AnnotationVisitor visitArray(String name) {
        if (name.equals("specifications")) {
            return new InterfaceArrayVisitor();
        } else if (name.equals("properties")) {
            // Create a new simple visitor to visit the nested ServiceProperty annotations
            // Collected properties are collected in m_prov
<<<<<<< HEAD
            return new EmptyVisitor() {
=======
            return new AnnotationVisitor(Opcodes.ASM5) {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                public AnnotationVisitor visitAnnotation(String ignored, String desc) {
                    return new FieldPropertyVisitor(m_prov);
                }
            };
        } else {
            return null;
        }
    }

    /**
     * End of the visit.
     * Append to the element element the computed "provides" element.
<<<<<<< HEAD
     * @see org.objectweb.asm.commons.EmptyVisitor#visitEnd()
=======
     *
     * @see org.objectweb.asm.AnnotationVisitor#visitEnd()
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
     */
    public void visitEnd() {
        workbench.getIds().put("provides", m_prov);
        workbench.getElements().put(m_prov, null);
    }

<<<<<<< HEAD
    private class InterfaceArrayVisitor extends EmptyVisitor {
=======
    private class InterfaceArrayVisitor extends AnnotationVisitor {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        /**
         * List of parsed interface.
         */
        private String m_itfs;

<<<<<<< HEAD
        /**
         * Visit one element of the array.
         * @param arg0 : null
         * @param arg1 : element value.
         * @see org.objectweb.asm.commons.EmptyVisitor#visit(java.lang.String, java.lang.Object)
=======

        public InterfaceArrayVisitor() {
            super(Opcodes.ASM5);
        }

        /**
         * Visit one element of the array.
         *
         * @param arg0 : null
         * @param arg1 : element value.
         * @see org.objectweb.asm.AnnotationVisitor#visit(java.lang.String, java.lang.Object)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
         */
        public void visit(String arg0, Object arg1) {
            if (m_itfs == null) {
                m_itfs = "{" + ((Type) arg1).getClassName();
            } else {
                m_itfs += "," + ((Type) arg1).getClassName();
            }
        }

        /**
         * End of the array visit.
         * Add the attribute to 'provides' element.
<<<<<<< HEAD
         * @see org.objectweb.asm.commons.EmptyVisitor#visitEnd()
=======
         *
         * @see org.objectweb.asm.AnnotationVisitor#visitEnd()
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
         */
        public void visitEnd() {
            m_prov.addAttribute(new Attribute("specifications", m_itfs + "}"));
        }

    }


}
