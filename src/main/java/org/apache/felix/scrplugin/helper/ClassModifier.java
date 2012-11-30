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
package org.apache.felix.scrplugin.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.felix.scrplugin.Log;
import org.apache.felix.scrplugin.SCRDescriptorException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

/**
 * Helper class for injecting/generating accessor methods for
 * unary references.
 */
public abstract class ClassModifier {

    /**
     * Add bind/unbind methods
     * @param className       The class name in which the methods are injected
     * @param referenceName   Name of the reference
     * @param fieldName       Name of the field
     * @param typeName        Name of the type
     * @param createBind      Name of the bind method or null
     * @param createUnbind    Name of the unbind method or null
     * @param outputDirectory Output directory where the class file is stored
     * @throws SCRDescriptorException
     */
    public static void addMethods(final String className,
                           final String referenceName,
                           final String fieldName,
                           final String typeName,
                           final boolean createBind,
                           final boolean createUnbind,
                           final ClassLoader classLoader,
                           final String outputDirectory,
                           final Log logger)
    throws SCRDescriptorException {
        // now do byte code manipulation
        final String fileName = outputDirectory + File.separatorChar +  className.replace('.', File.separatorChar) + ".class";
        final ClassNode cn = new ClassNode();
        try {
            final ClassReader reader = new ClassReader(new FileInputStream(fileName));
            reader.accept(cn, 0);

            // For target Java7 and above use: ClassWriter.COMPUTE_MAXS  | ClassWriter.COMPUTE_FRAMES
            final int mask = (cn.version > 50 ? ClassWriter.COMPUTE_MAXS  | ClassWriter.COMPUTE_FRAMES : 0);
            final ClassWriter writer = new ClassWriter(mask) {

                @Override
                protected String getCommonSuperClass(final String type1, final String type2) {
                    Class<?> c, d;
                    try {
                        c = classLoader.loadClass(type1.replace('/', '.'));
                        d = classLoader.loadClass(type2.replace('/', '.'));
                    } catch (final Exception e) {
                        throw new RuntimeException(e.toString(), e);
                    }
                    if (c.isAssignableFrom(d)) {
                        return type1;
                    }
                    if (d.isAssignableFrom(c)) {
                        return type2;
                    }
                    if (c.isInterface() || d.isInterface()) {
                        return "java/lang/Object";
                    }
                    do {
                        c = c.getSuperclass();
                    } while (!c.isAssignableFrom(d));
                    return c.getName().replace('.', '/');
                }

            };

            cn.accept(writer);
            if ( createBind ) {
                logger.debug("Adding bind " + className + " " + fieldName);

                createMethod(writer, className, referenceName, fieldName, typeName, true);
            }
            if ( createUnbind ) {
                logger.debug("Adding unbind " + className + " " + fieldName);

                createMethod(writer, className, referenceName, fieldName, typeName, false);
            }

            final FileOutputStream fos = new FileOutputStream(fileName);
            fos.write(writer.toByteArray());
            fos.close();
        } catch (final Exception e) {
            throw new SCRDescriptorException("Unable to add methods to " + className, typeName, e);
        }
    }

    private static void createMethod(final ClassWriter cw, final String className, final String referenceName, final String fieldName, final String typeName, final boolean bind) {
        final org.objectweb.asm.Type type = org.objectweb.asm.Type.getType("L" + typeName.replace('.', '/') + ";");
        final String methodName = (bind ? "" : "un") + "bind" + referenceName.substring(0, 1).toUpperCase() + referenceName.substring(1);
        final MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PROTECTED, methodName, "(" + type.toString() + ")V", null, null);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        if ( bind ) {
            mv.visitVarInsn(type.getOpcode(Opcodes.ILOAD), 1);
            mv.visitFieldInsn(Opcodes.PUTFIELD, className.replace('.', '/'), fieldName, type.toString());
        } else {
            mv.visitFieldInsn(Opcodes.GETFIELD, className.replace('.', '/'), fieldName, type.toString());
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            final Label jmpLabel = new Label();
            mv.visitJumpInsn(Opcodes.IF_ACMPNE, jmpLabel);
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitInsn(Opcodes.ACONST_NULL);
            mv.visitFieldInsn(Opcodes.PUTFIELD, className.replace('.', '/'), fieldName, type.toString());
            mv.visitLabel(jmpLabel);
        }
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(2, 2);
    }
}
