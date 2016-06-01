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
package org.apache.felix.scr.impl;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import junit.framework.TestCase;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

/**
 * Ensure that the helper / manager / metadata packages can actually be used
 * without any other packages.
 */
public class PackageIsolationTest extends TestCase {

    public void testIsolation() throws Exception {
        final List<String> packages = Arrays.asList(
                "org/apache/felix/scr/component",
                "org/apache/felix/scr/impl/helper",
                "org/apache/felix/scr/impl/manager",
                "org/apache/felix/scr/impl/metadata");


        StringBuilder regexp = new StringBuilder();
        regexp.append("target/classes/(");
        boolean first = true;
        for (String pkg : packages) {
            if (!first) {
                regexp.append("|");
            }
            regexp.append(pkg);
            first = false;
        }
        regexp.append(")/[A-Za-z]+\\.class");
        Pattern pattern = Pattern.compile(regexp.toString());

        DependencyVisitor v = new DependencyVisitor();
        check(new File("target/classes"), pattern, v);

        for (String pkg : v.getPackages()) {
            if (pkg.startsWith("org/apache/felix/scr")
                    && !packages.contains(pkg)) {
                fail("Unexpected dependency on package " + pkg + " found");
            }
        }
    }

    static void check(File file, Pattern regexp, ClassVisitor v) throws Exception {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                if (regexp.matcher(child.toString()).matches()) {
                    new ClassReader(new FileInputStream(child)).accept(v, 0);
                }
                check(child, regexp, v);
            }
        }
    }

    public static class DependencyVisitor extends ClassVisitor {
        Set<String> packages = new HashSet<String>();

        Map<String, Map<String, Integer>> groups = new HashMap<String, Map<String, Integer>>();

        Map<String, Integer> current;

        public Map<String, Map<String, Integer>> getGlobals() {
            return groups;
        }

        public Set<String> getPackages() {
            return packages;
        }

        public DependencyVisitor() {
            super(Opcodes.ASM5);
        }

        // ClassVisitor

        @Override
        public void visit(final int version, final int access, final String name,
                          final String signature, final String superName,
                          final String[] interfaces) {
            String p = getGroupKey(name);
            current = groups.get(p);
            if (current == null) {
                current = new HashMap<String, Integer>();
                groups.put(p, current);
            }

            if (signature == null) {
                if (superName != null) {
                    addInternalName(superName);
                }
                addInternalNames(interfaces);
            } else {
                addSignature(signature);
            }
        }

        @Override
        public AnnotationVisitor visitAnnotation(final String desc,
                                                 final boolean visible) {
            addDesc(desc);
            return new AnnotationDependencyVisitor();
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(final int typeRef,
                                                     final TypePath typePath, final String desc, final boolean visible) {
            addDesc(desc);
            return new AnnotationDependencyVisitor();
        }

        @Override
        public FieldVisitor visitField(final int access, final String name,
                                       final String desc, final String signature, final Object value) {
            if (signature == null) {
                addDesc(desc);
            } else {
                addTypeSignature(signature);
            }
            if (value instanceof Type) {
                addType((Type) value);
            }
            return new FieldDependencyVisitor();
        }

        @Override
        public MethodVisitor visitMethod(final int access, final String name,
                                         final String desc, final String signature, final String[] exceptions) {
            if (signature == null) {
                addMethodDesc(desc);
            } else {
                addSignature(signature);
            }
            addInternalNames(exceptions);
            return new MethodDependencyVisitor();
        }

        class AnnotationDependencyVisitor extends AnnotationVisitor {

            public AnnotationDependencyVisitor() {
                super(Opcodes.ASM5);
            }

            @Override
            public void visit(final String name, final Object value) {
                if (value instanceof Type) {
                    addType((Type) value);
                }
            }

            @Override
            public void visitEnum(final String name, final String desc,
                                  final String value) {
                addDesc(desc);
            }

            @Override
            public AnnotationVisitor visitAnnotation(final String name,
                                                     final String desc) {
                addDesc(desc);
                return this;
            }

            @Override
            public AnnotationVisitor visitArray(final String name) {
                return this;
            }
        }

        class FieldDependencyVisitor extends FieldVisitor {

            public FieldDependencyVisitor() {
                super(Opcodes.ASM5);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                addDesc(desc);
                return new AnnotationDependencyVisitor();
            }

            @Override
            public AnnotationVisitor visitTypeAnnotation(final int typeRef,
                                                         final TypePath typePath, final String desc,
                                                         final boolean visible) {
                addDesc(desc);
                return new AnnotationDependencyVisitor();
            }
        }

        class MethodDependencyVisitor extends MethodVisitor {

            public MethodDependencyVisitor() {
                super(Opcodes.ASM5);
            }

            @Override
            public AnnotationVisitor visitAnnotationDefault() {
                return new AnnotationDependencyVisitor();
            }

            @Override
            public AnnotationVisitor visitAnnotation(final String desc,
                                                     final boolean visible) {
                addDesc(desc);
                return new AnnotationDependencyVisitor();
            }

            @Override
            public AnnotationVisitor visitTypeAnnotation(final int typeRef,
                                                         final TypePath typePath, final String desc,
                                                         final boolean visible) {
                addDesc(desc);
                return new AnnotationDependencyVisitor();
            }

            @Override
            public AnnotationVisitor visitParameterAnnotation(final int parameter,
                                                              final String desc, final boolean visible) {
                addDesc(desc);
                return new AnnotationDependencyVisitor();
            }

            @Override
            public void visitTypeInsn(final int opcode, final String type) {
                addType(Type.getObjectType(type));
            }

            @Override
            public void visitFieldInsn(final int opcode, final String owner,
                                       final String name, final String desc) {
                addInternalName(owner);
                addDesc(desc);
            }

            @Override
            public void visitMethodInsn(final int opcode, final String owner,
                                        final String name, final String desc, final boolean itf) {
                addInternalName(owner);
                addMethodDesc(desc);
            }

            @Override
            public void visitInvokeDynamicInsn(String name, String desc,
                                               Handle bsm, Object... bsmArgs) {
                addMethodDesc(desc);
                addConstant(bsm);
                for (int i = 0; i < bsmArgs.length; i++) {
                    addConstant(bsmArgs[i]);
                }
            }

            @Override
            public void visitLdcInsn(final Object cst) {
                addConstant(cst);
            }

            @Override
            public void visitMultiANewArrayInsn(final String desc, final int dims) {
                addDesc(desc);
            }

            @Override
            public AnnotationVisitor visitInsnAnnotation(int typeRef,
                                                         TypePath typePath, String desc, boolean visible) {
                addDesc(desc);
                return new AnnotationDependencyVisitor();
            }

            @Override
            public void visitLocalVariable(final String name, final String desc,
                                           final String signature, final Label start, final Label end,
                                           final int index) {
                addTypeSignature(signature);
            }

            @Override
            public AnnotationVisitor visitLocalVariableAnnotation(int typeRef,
                                                                  TypePath typePath, Label[] start, Label[] end, int[] index,
                                                                  String desc, boolean visible) {
                addDesc(desc);
                return new AnnotationDependencyVisitor();
            }

            @Override
            public void visitTryCatchBlock(final Label start, final Label end,
                                           final Label handler, final String type) {
                if (type != null) {
                    addInternalName(type);
                }
            }

            @Override
            public AnnotationVisitor visitTryCatchAnnotation(int typeRef,
                                                             TypePath typePath, String desc, boolean visible) {
                addDesc(desc);
                return new AnnotationDependencyVisitor();
            }
        }

        class SignatureDependencyVisitor extends SignatureVisitor {

            String signatureClassName;

            public SignatureDependencyVisitor() {
                super(Opcodes.ASM5);
            }

            @Override
            public void visitClassType(final String name) {
                signatureClassName = name;
                addInternalName(name);
            }

            @Override
            public void visitInnerClassType(final String name) {
                signatureClassName = signatureClassName + "$" + name;
                addInternalName(signatureClassName);
            }
        }

        // ---------------------------------------------

        private String getGroupKey(String name) {
            int n = name.lastIndexOf('/');
            if (n > -1) {
                name = name.substring(0, n);
            }
            packages.add(name);
            return name;
        }

        private void addName(final String name) {
            if (name == null) {
                return;
            }
            String p = getGroupKey(name);
            if (current.containsKey(p)) {
                current.put(p, current.get(p) + 1);
            } else {
                current.put(p, 1);
            }
        }

        void addInternalName(final String name) {
            addType(Type.getObjectType(name));
        }

        private void addInternalNames(final String[] names) {
            for (int i = 0; names != null && i < names.length; i++) {
                addInternalName(names[i]);
            }
        }

        void addDesc(final String desc) {
            addType(Type.getType(desc));
        }

        void addMethodDesc(final String desc) {
            addType(Type.getReturnType(desc));
            Type[] types = Type.getArgumentTypes(desc);
            for (int i = 0; i < types.length; i++) {
                addType(types[i]);
            }
        }

        void addType(final Type t) {
            switch (t.getSort()) {
                case Type.ARRAY:
                    addType(t.getElementType());
                    break;
                case Type.OBJECT:
                    addName(t.getInternalName());
                    break;
                case Type.METHOD:
                    addMethodDesc(t.getDescriptor());
                    break;
            }
        }

        private void addSignature(final String signature) {
            if (signature != null) {
                new SignatureReader(signature)
                        .accept(new SignatureDependencyVisitor());
            }
        }

        void addTypeSignature(final String signature) {
            if (signature != null) {
                new SignatureReader(signature)
                        .acceptType(new SignatureDependencyVisitor());
            }
        }

        void addConstant(final Object cst) {
            if (cst instanceof Type) {
                addType((Type) cst);
            } else if (cst instanceof Handle) {
                Handle h = (Handle) cst;
                addInternalName(h.getOwner());
                addMethodDesc(h.getDesc());
            }
        }
    }

}
