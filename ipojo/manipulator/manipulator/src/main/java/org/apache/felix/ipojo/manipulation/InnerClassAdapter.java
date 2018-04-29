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
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.tree.LocalVariableNode;

<<<<<<< HEAD
import java.util.Iterator;
=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adapts a inner class in order to allow accessing outer class fields.
 * A manipulated inner class has access to the managed field of the outer class.
 *
 * Only non-static inner classes are manipulated, others are not.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
<<<<<<< HEAD
public class InnerClassAdapter extends ClassAdapter implements Opcodes {
=======
public class InnerClassAdapter extends ClassVisitor implements Opcodes {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    /**
     * The manipulator having manipulated the outer class.
     * We add method descriptions to this manipulator.
     */
    private final Manipulator m_manipulator;

    /**
     * The name of the inner class. This name is only define in the outer class.
     */
    private final String m_name;

    /**
     * The ismple name of the class.
     */
    private final String m_simpleName;

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
     * @param name      the inner class name (internal name)
<<<<<<< HEAD
     * @param arg0       parent class visitor
     * @param outerClassName outer class (implementation class)
     * @param manipulator the manipulator having manipulated the outer class.
     */
    public InnerClassAdapter(String name, ClassVisitor arg0, String outerClassName,
                             Manipulator manipulator) {
        super(arg0);
=======
     * @param visitor       parent class visitor
     * @param outerClassName outer class (implementation class)
     * @param manipulator the manipulator having manipulated the outer class.
     */
    public InnerClassAdapter(String name, ClassVisitor visitor, String outerClassName,
                             Manipulator manipulator) {
        super(Opcodes.ASM5, visitor);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        m_name = name;
        m_simpleName = m_name.substring(m_name.indexOf("$") + 1);
        m_outer = outerClassName;
        m_manipulator = manipulator;
        m_fields = manipulator.getFields().keySet();
    }

<<<<<<< HEAD
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        // If version = 1.7, use 1.6 if the ipojo.downgrade.classes system property is either
        // not set of set to true.
        int theVersion = version;
        String downgrade = System.getProperty("ipojo.downgrade.classes");
        if ((downgrade == null  || "true".equals(downgrade))  && version == Opcodes.V1_7) {
            theVersion = Opcodes.V1_6;
        }
        super.visit(theVersion, access, name, signature, superName, interfaces);
    }

=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
     * @see org.objectweb.asm.ClassAdapter#visitMethod(int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
=======
     * @see org.objectweb.asm.ClassVisitor#visitMethod(int, java.lang.String, java.lang.String, java.lang.String,
     * java.lang.String[])
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
     */
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        // Do nothing on static methods, should not happen in non-static inner classes.
        if ((access & ACC_STATIC) == ACC_STATIC) {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }

        // Do nothing on native methods
        if ((access & ACC_NATIVE) == ACC_NATIVE) {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }


        // Do not re-manipulate.
        if (! m_manipulator.isAlreadyManipulated()) {

            if (name.equals("<init>")) {
                // We change the field access from the constructor, but we don't generate the wrapper.
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                return new MethodCodeAdapter(mv, m_outer, access, name, desc, m_fields);
            }

            // For all non constructor methods

            MethodDescriptor md = getMethodDescriptor(name, desc);
            if (md == null) {
                generateMethodWrapper(access, name, desc, signature, exceptions, null, null,
                        null);
            } else {
                generateMethodWrapper(access, name, desc, signature, exceptions,
                        md.getArgumentLocalVariables(),
                        md.getAnnotations(), md.getParameterAnnotations());
            }

            // The new name is the method name prefixed by the PREFIX.
<<<<<<< HEAD
            MethodVisitor mv = super.visitMethod(access, MethodCreator.PREFIX + name, desc, signature,
                    exceptions);
            return new MethodCodeAdapter(mv, m_outer, access,  MethodCreator.PREFIX + name, desc, m_fields);
=======
            MethodVisitor mv = super.visitMethod(ACC_PRIVATE, ClassManipulator.PREFIX + name, desc, signature,
                    exceptions);
            return new MethodCodeAdapter(mv, m_outer, ACC_PRIVATE,  ClassManipulator.PREFIX + name, desc, m_fields);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        } else {
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
    }

    private String getMethodFlagName(String name, String desc) {
<<<<<<< HEAD
        return MethodCreator.METHOD_FLAG_PREFIX + getMethodId(name, desc);
=======
        return ClassManipulator.METHOD_FLAG_PREFIX + getMethodId(name, desc);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    private String getMethodId(String name, String desc) {
        StringBuilder id = new StringBuilder(m_simpleName);
        id.append("___"); // Separator
        id.append(name);

        Type[] args = Type.getArgumentTypes(desc);
        for (Type type : args) {
            String arg = type.getClassName();
            if (arg.endsWith("[]")) {
                // We have to replace all []
                String acc = "";
                while (arg.endsWith("[]")) {
                    arg = arg.substring(0, arg.length() - 2);
                    acc += "__";
                }
                id.append("$").append(arg.replace('.', '_')).append(acc);
            } else {
                id.append("$").append(arg.replace('.', '_'));
            }
        }
        return id.toString();
    }

    /**
     * Generate the method header of a POJO method.
     * This method header encapsulate the POJO method call to
     * signal entry exit and error to the container.
     *
     * The instance manager and flag are accessed using method calls.
     * @param access : access flag.
     * @param name : method name.
     * @param desc : method descriptor.
     * @param signature : method signature.
     * @param exceptions : declared exceptions.
     * @param localVariables : the local variable nodes.
     * @param annotations : the annotations to move to this method.
     * @param paramAnnotations : the parameter annotations to move to this method.
     */
    private void generateMethodWrapper(int access, String name, String desc, String signature, String[] exceptions,
                                       List<LocalVariableNode> localVariables, List<ClassChecker.AnnotationDescriptor> annotations,
                                       Map<Integer, List<ClassChecker.AnnotationDescriptor>> paramAnnotations) {
        GeneratorAdapter mv = new GeneratorAdapter(cv.visitMethod(access, name, desc, signature, exceptions), access, name, desc);

        // If we have variables, we wraps the code within labels. The `lifetime` of the variables are bound to those
        // two variables.
        boolean hasArgumentLabels = localVariables != null && !localVariables.isEmpty();
        Label start = null;
        if (hasArgumentLabels) {
            start = new Label();
            mv.visitLabel(start);
        }

        mv.visitCode();

        Type returnType = Type.getReturnType(desc);

        // Compute result and exception stack location
        int result = -1;
<<<<<<< HEAD
        int exception = -1;
=======
        int exception;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        //int arguments = mv.newLocal(Type.getType((new Object[0]).getClass()));

        if (returnType.getSort() != Type.VOID) {
            // The method returns something
            result = mv.newLocal(returnType);
            exception = mv.newLocal(Type.getType(Throwable.class));
        } else {
            exception = mv.newLocal(Type.getType(Throwable.class));
        }

        Label l0 = new Label();
        Label l1 = new Label();
        Label l2 = new Label();

        mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Throwable");

        // Access the flag from the outer class
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_name, "this$0", "L" + m_outer + ";");
        mv.visitFieldInsn(GETFIELD, m_outer, getMethodFlagName(name, desc), "Z");
        mv.visitJumpInsn(IFNE, l0);

        mv.visitVarInsn(ALOAD, 0);
        mv.loadArgs();
<<<<<<< HEAD
        mv.visitMethodInsn(INVOKESPECIAL, m_name, MethodCreator.PREFIX + name, desc);
=======
        mv.visitMethodInsn(INVOKESPECIAL, m_name, ClassManipulator.PREFIX + name, desc, false);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        mv.visitInsn(returnType.getOpcode(IRETURN));

        // end of the non intercepted method invocation.

        mv.visitLabel(l0);

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_name, "this$0", "L" + m_outer + ";");
<<<<<<< HEAD
        mv.visitFieldInsn(GETFIELD, m_outer, MethodCreator.IM_FIELD, "Lorg/apache/felix/ipojo/InstanceManager;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn(getMethodId(name, desc));
        mv.loadArgArray();
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", MethodCreator.ENTRY,
                "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
=======
        mv.visitFieldInsn(GETFIELD, m_outer, ClassManipulator.IM_FIELD, "Lorg/apache/felix/ipojo/InstanceManager;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn(getMethodId(name, desc));
        mv.loadArgArray();
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", ClassManipulator.ENTRY,
                "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V", false);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        mv.visitVarInsn(ALOAD, 0);

        // Do not allow argument modification : just reload arguments.
        mv.loadArgs();
<<<<<<< HEAD
        mv.visitMethodInsn(INVOKESPECIAL, m_name, MethodCreator.PREFIX + name, desc);
=======
        mv.visitMethodInsn(INVOKESPECIAL, m_name, ClassManipulator.PREFIX + name, desc, false);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        if (returnType.getSort() != Type.VOID) {
            mv.visitVarInsn(returnType.getOpcode(ISTORE), result);
        }

        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_name, "this$0", "L" + m_outer + ";");
<<<<<<< HEAD
        mv.visitFieldInsn(GETFIELD, m_outer, MethodCreator.IM_FIELD, "Lorg/apache/felix/ipojo/InstanceManager;");
=======
        mv.visitFieldInsn(GETFIELD, m_outer, ClassManipulator.IM_FIELD, "Lorg/apache/felix/ipojo/InstanceManager;");
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn(getMethodId(name, desc));
        if (returnType.getSort() != Type.VOID) {
            mv.visitVarInsn(returnType.getOpcode(ILOAD), result);
            mv.box(returnType);
        } else {
            mv.visitInsn(ACONST_NULL);
        }
<<<<<<< HEAD
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", MethodCreator.EXIT, "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V");
=======
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager",
                ClassManipulator.EXIT, "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Object;)V", false);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        mv.visitLabel(l1);
        Label l7 = new Label();
        mv.visitJumpInsn(GOTO, l7);
        mv.visitLabel(l2);

        mv.visitVarInsn(ASTORE, exception);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, m_name, "this$0", "L" + m_outer + ";");
<<<<<<< HEAD
        mv.visitFieldInsn(GETFIELD, m_outer, MethodCreator.IM_FIELD, "Lorg/apache/felix/ipojo/InstanceManager;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn(getMethodId(name, desc));
        mv.visitVarInsn(ALOAD, exception);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", MethodCreator.ERROR, "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Throwable;)V");
=======
        mv.visitFieldInsn(GETFIELD, m_outer, ClassManipulator.IM_FIELD, "Lorg/apache/felix/ipojo/InstanceManager;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn(getMethodId(name, desc));
        mv.visitVarInsn(ALOAD, exception);
        mv.visitMethodInsn(INVOKEVIRTUAL, "org/apache/felix/ipojo/InstanceManager", ClassManipulator.ERROR,
                "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/Throwable;)V", false);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        mv.visitVarInsn(ALOAD, exception);
        mv.visitInsn(ATHROW);

        mv.visitLabel(l7);
        if (returnType.getSort() != Type.VOID) {
            mv.visitVarInsn(returnType.getOpcode(ILOAD), result);
        }
        mv.visitInsn(returnType.getOpcode(IRETURN));

        // If we had arguments, we mark the end of the lifetime.
        Label end = null;
        if (hasArgumentLabels) {
            end = new Label();
            mv.visitLabel(end);
        }

        // Move annotations
        if (annotations != null) {
<<<<<<< HEAD
            for (int i = 0; i < annotations.size(); i++) {
                ClassChecker.AnnotationDescriptor ad = annotations.get(i);
=======
            for (ClassChecker.AnnotationDescriptor ad : annotations) {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                ad.visitAnnotation(mv);
            }
        }

        // Move parameter annotations
        if (paramAnnotations != null  && ! paramAnnotations.isEmpty()) {
<<<<<<< HEAD
            Iterator<Integer> ids = paramAnnotations.keySet().iterator();
            while(ids.hasNext()) {
                Integer id = ids.next();
                List<ClassChecker.AnnotationDescriptor> ads = paramAnnotations.get(id);
                for (int i = 0; i < ads.size(); i++) {
                    ClassChecker.AnnotationDescriptor ad = ads.get(i);
=======
            for (Integer id : paramAnnotations.keySet()) {
                List<ClassChecker.AnnotationDescriptor> ads = paramAnnotations.get(id);
                for (ClassChecker.AnnotationDescriptor ad : ads) {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                    ad.visitParameterAnnotation(id, mv);
                }
            }
        }

        // Write the arguments name.
        if (hasArgumentLabels) {
            for (LocalVariableNode var : localVariables) {
                mv.visitLocalVariable(var.name, var.desc, var.signature, start, end, var.index);
            }
        }

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * Gets the method descriptor for the specified name and descriptor.
     * The method descriptor is looked inside the
<<<<<<< HEAD
     * {@link MethodCreator#m_visitedMethods}
=======
     * {@link ClassManipulator#m_visitedMethods}
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
     * @param name the name of the method
     * @param desc the descriptor of the method
     * @return the method descriptor or <code>null</code> if not found.
     */
    private MethodDescriptor getMethodDescriptor(String name, String desc) {
        for (MethodDescriptor md : m_manipulator.getMethodsFromInnerClass(m_name)) {
            if (md.getName().equals(name) && md.getDescriptor().equals(desc)) {
                return md;
            }
        }
        return null;
    }

}
