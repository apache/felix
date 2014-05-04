/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.extender.internal.processor;

import org.apache.felix.ipojo.configuration.Configuration;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Class visitor detecting @Configuration annotation.
 * <p/>
 * After the visit, two state variables are set:
 * {@literal isConfiguration} is set to true if the class contained the @Configuration annotation
 * {@literal parent} is set to the parent class if and only if it's not a java.* class (which don't contain the
 * Configuration annotation) and {@literal isConfiguration} is set to false
 */
public class ConfigurationAnnotationScanner extends ClassVisitor implements Opcodes {

    private static final String CONFIGURATION_ANNOTATION_DESCRIPTOR = Type.getType(Configuration.class)
            .getDescriptor();
    private boolean m_isConfiguration = false;
    private String m_super;

    public ConfigurationAnnotationScanner() {
        super(Opcodes.ASM5);
    }


    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        m_super = superName;  // This is the internal class name.
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean vis) {
        if (desc.equals(CONFIGURATION_ANNOTATION_DESCRIPTOR)) {
            m_isConfiguration = true;
        }
        return null;
    }

    public boolean isConfiguration() {
        return m_isConfiguration;
    }

    public String getParent() {
        if (m_super == null || m_super.startsWith("java/") || m_isConfiguration) {
            return null;
        }
        return m_super.replace("/", ".");
    }
}
