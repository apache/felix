package org.apache.felix.scrplugin.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import org.apache.felix.scrplugin.SCRDescriptorException;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public class ClassModifier {

    public static void addMethods(final String className,
                           final String referenceName,
                           final String fieldName,
                           final String typeName,
                           final boolean createBind,
                           final boolean createUnbind,
                           final String outputDirectory)
    throws SCRDescriptorException {
        // now do byte code manipulation
        final String fileName = outputDirectory + File.separatorChar +  className.replace('.', File.separatorChar) + ".class";
        final ClassNode cn = new ClassNode();
        try {
            final ClassReader reader = new ClassReader(new FileInputStream(fileName));
            reader.accept(cn, 0);

            final ClassWriter writer = new ClassWriter(0);

            // remove existing implementation von previous builds
            final ClassAdapter adapter = new ClassAdapter(writer) {

                protected final String bindMethodName = "bind" + referenceName.substring(0, 1).toUpperCase() + referenceName.substring(1);
                protected final String unbindMethodName = "unbind" + referenceName.substring(0, 1).toUpperCase() + referenceName.substring(1);
                protected final String description = "(L" + typeName.replace('.', '/') + ";)V";

                /**
                 * @see org.objectweb.asm.ClassAdapter#visitMethod(int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
                 */
                public MethodVisitor visitMethod(final int access,
                                final String name,
                                final String desc,
                                final String signature,
                                final String[] exceptions) {
                    if ( createBind && name.equals(bindMethodName) && description.equals(desc) ) {
                        return null;
                    }
                    if ( createUnbind && name.equals(unbindMethodName)  && description.equals(desc) ) {
                        return null;
                    }
                    return super.visitMethod(access, name, desc, signature, exceptions);
                }

            };

            cn.accept(adapter);
            if ( createBind ) {
                createMethod(writer, className, referenceName, fieldName, typeName, true);
            }
            if ( createUnbind ) {
                createMethod(writer, className, referenceName, fieldName, typeName, false);
            }

            final FileOutputStream fos = new FileOutputStream(fileName);
            fos.write(writer.toByteArray());
            fos.close();
        } catch (final Exception e) {
            throw new SCRDescriptorException("Unable to add methods to " + className, typeName, e);
        }
    }

    private static void createMethod(final ClassWriter cw, final String className, final String referenceName, final String fieldName, final String typeName, boolean bind) {
        final org.objectweb.asm.Type type = org.objectweb.asm.Type.getType("L" + typeName.replace('.', '/') + ";");
        final String methodName = (bind ? "" : "un") + "bind" + referenceName.substring(0, 1).toUpperCase() + referenceName.substring(1);
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PROTECTED, methodName, "(" + type.toString() + ")V", null, null);
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
