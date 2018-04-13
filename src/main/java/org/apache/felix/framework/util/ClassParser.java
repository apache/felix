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
package org.apache.felix.framework.util;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class is based on code developed at https://github.com/bndtools/bnd
 */
public class ClassParser
{
    Map<String, TypeRef> typeRefCache = new HashMap<String, TypeRef>();
    Map<String, Descriptor> descriptorCache = new HashMap<String, Descriptor>();
    Map<String, PackageRef> packageCache = new HashMap<String, PackageRef>();

    // MUST BE BEFORE PRIMITIVES, THEY USE THE DEFAULT PACKAGE!!
    final static PackageRef DEFAULT_PACKAGE = new PackageRef();
    final static PackageRef PRIMITIVE_PACKAGE = new PackageRef();

    final static TypeRef VOID = new ConcreteRef("V", "void", PRIMITIVE_PACKAGE);
    final static TypeRef BOOLEAN = new ConcreteRef("Z", "boolean", PRIMITIVE_PACKAGE);
    final static TypeRef BYTE = new ConcreteRef("B", "byte", PRIMITIVE_PACKAGE);
    final static TypeRef CHAR = new ConcreteRef("C", "char", PRIMITIVE_PACKAGE);
    final static TypeRef SHORT = new ConcreteRef("S", "short", PRIMITIVE_PACKAGE);
    final static TypeRef INTEGER = new ConcreteRef("I", "int", PRIMITIVE_PACKAGE);
    final static TypeRef LONG = new ConcreteRef("J", "long", PRIMITIVE_PACKAGE);
    final static TypeRef DOUBLE = new ConcreteRef("D", "double", PRIMITIVE_PACKAGE);
    final static TypeRef FLOAT = new ConcreteRef("F", "float", PRIMITIVE_PACKAGE);


    {
        packageCache.put("", DEFAULT_PACKAGE);
    }

    private interface TypeRef extends Comparable<TypeRef>
    {
        String getBinary();

        String getFQN();

        String getPath();

        boolean isPrimitive();

        TypeRef getClassRef();

        PackageRef getPackageRef();

        String getShortName();

        String getSourcePath();

        String getDottedOnly();

    }

    private static class PackageRef implements Comparable<PackageRef>
    {
        final String binaryName;
        final String fqn;

        PackageRef(String binaryName)
        {
            this.binaryName = fqnToBinary(binaryName);
            this.fqn = binaryToFQN(binaryName);
        }

        PackageRef()
        {
            this.binaryName = "";
            this.fqn = ".";
        }

        public String getFQN()
        {
            return fqn;
        }

        @Override
        public String toString()
        {
            return fqn;
        }

        boolean isPrimitivePackage()
        {
            return this == PRIMITIVE_PACKAGE;
        }

        @Override
        public int compareTo(PackageRef other)
        {
            return fqn.compareTo(other.fqn);
        }

        @Override
        public boolean equals(Object o)
        {
            assert o instanceof PackageRef;
            return o == this;
        }

        @Override
        public int hashCode()
        {
            return super.hashCode();
        }


    }

    // We "intern" the
    private static class ConcreteRef implements TypeRef
    {
        final String binaryName;
        final String fqn;
        final boolean primitive;
        final PackageRef packageRef;

        ConcreteRef(PackageRef packageRef, String binaryName)
        {
            this.binaryName = binaryName;
            this.fqn = binaryToFQN(binaryName);
            this.primitive = false;
            this.packageRef = packageRef;
        }

        ConcreteRef(String binaryName, String fqn, PackageRef pref)
        {
            this.binaryName = binaryName;
            this.fqn = fqn;
            this.primitive = true;
            this.packageRef = pref;
        }

        @Override
        public String getBinary()
        {
            return binaryName;
        }

        @Override
        public String getPath()
        {
            return binaryName + ".class";
        }

        @Override
        public String getSourcePath()
        {
            return binaryName + ".java";
        }

        @Override
        public String getFQN()
        {
            return fqn;
        }

        @Override
        public String getDottedOnly()
        {
            return fqn.replace('$', '.');
        }

        @Override
        public boolean isPrimitive()
        {
            return primitive;
        }

        @Override
        public TypeRef getClassRef()
        {
            return this;
        }

        @Override
        public PackageRef getPackageRef()
        {
            return packageRef;
        }

        @Override
        public String getShortName()
        {
            int n = binaryName.lastIndexOf('/');
            return binaryName.substring(n + 1);
        }

        @Override
        public String toString()
        {
            return fqn;
        }

        @Override
        public boolean equals(Object other)
        {
            assert other instanceof TypeRef;
            return this == other;
        }

        @Override
        public int compareTo(TypeRef other)
        {
            if (this == other)
            {
                return 0;
            }
            return fqn.compareTo(other.getFQN());
        }

        @Override
        public int hashCode()
        {
            return super.hashCode();
        }

    }

    private static class ArrayRef implements TypeRef
    {
        final TypeRef component;

        ArrayRef(TypeRef component)
        {
            this.component = component;
        }

        @Override
        public String getBinary()
        {
            return "[" + component.getBinary();
        }

        @Override
        public String getFQN()
        {
            return component.getFQN() + "[]";
        }

        @Override
        public String getPath()
        {
            return component.getPath();
        }

        @Override
        public String getSourcePath()
        {
            return component.getSourcePath();
        }

        @Override
        public boolean isPrimitive()
        {
            return false;
        }

        @Override
        public TypeRef getClassRef()
        {
            return component.getClassRef();
        }

        @Override
        public boolean equals(Object other)
        {
            if (other == null || other.getClass() != getClass())
            {
                return false;
            }

            return component.equals(((ArrayRef) other).component);
        }

        @Override
        public PackageRef getPackageRef()
        {
            return component.getPackageRef();
        }

        @Override
        public String getShortName()
        {
            return component.getShortName() + "[]";
        }

        @Override
        public String toString()
        {
            return component.toString() + "[]";
        }

        @Override
        public String getDottedOnly()
        {
            return component.getDottedOnly();
        }

        @Override
        public int compareTo(TypeRef other)
        {
            if (this == other)
            {
                return 0;
            }

            return getFQN().compareTo(other.getFQN());
        }

        @Override
        public int hashCode()
        {
            return super.hashCode();
        }
    }

    private TypeRef getTypeRef(String binaryClassName)
    {
        TypeRef ref = typeRefCache.get(binaryClassName);
        if (ref != null)
        {
            return ref;
        }

        if (binaryClassName.startsWith("["))
        {
            ref = getTypeRef(binaryClassName.substring(1));
            ref = new ArrayRef(ref);
        }
        else
        {
            if (binaryClassName.length() == 1)
            {
                switch (binaryClassName.charAt(0))
                {
                    case 'V':
                        return VOID;
                    case 'B':
                        return BYTE;
                    case 'C':
                        return CHAR;
                    case 'I':
                        return INTEGER;
                    case 'S':
                        return SHORT;
                    case 'D':
                        return DOUBLE;
                    case 'F':
                        return FLOAT;
                    case 'J':
                        return LONG;
                    case 'Z':
                        return BOOLEAN;
                }
                // falls trough for other 1 letter class names
            }
            if (binaryClassName.startsWith("L") && binaryClassName.endsWith(";"))
            {
                binaryClassName = binaryClassName.substring(1, binaryClassName.length() - 1);
            }
            ref = typeRefCache.get(binaryClassName);
            if (ref != null)
            {
                return ref;
            }

            PackageRef pref;
            int n = binaryClassName.lastIndexOf('/');
            if (n < 0)
            {
                pref = DEFAULT_PACKAGE;
            }
            else
            {
                pref = getPackageRef(binaryClassName.substring(0, n));
            }

            ref = new ConcreteRef(pref, binaryClassName);
        }

        typeRefCache.put(binaryClassName, ref);
        return ref;
    }

    private PackageRef getPackageRef(String binaryPackName)
    {
        if (binaryPackName.indexOf('.') >= 0)
        {
            binaryPackName = binaryPackName.replace('.', '/');
        }
        PackageRef ref = packageCache.get(binaryPackName);
        if (ref != null)
        {
            return ref;
        }

        ref = new PackageRef(binaryPackName);
        packageCache.put(binaryPackName, ref);
        return ref;
    }

    private Descriptor getDescriptor(String descriptor)
    {
        Descriptor d = descriptorCache.get(descriptor);
        if (d != null)
        {
            return d;
        }
        d = new Descriptor(descriptor);
        descriptorCache.put(descriptor, d);
        return d;
    }

    private class Descriptor
    {
        final TypeRef type;
        final TypeRef[] prototype;
        final String descriptor;

        Descriptor(String descriptor)
        {
            this.descriptor = descriptor;
            int index = 0;
            List<TypeRef> types = new ArrayList<TypeRef>();
            if (descriptor.charAt(index) == '(')
            {
                index++;
                while (descriptor.charAt(index) != ')')
                {
                    index = parse(types, descriptor, index);
                }
                index++; // skip )
                prototype = types.toArray(new TypeRef[0]);
                types.clear();
            }
            else
            {
                prototype = null;
            }

            index = parse(types, descriptor, index);
            type = types.get(0);
        }

        int parse(List<TypeRef> types, String descriptor, int index)
        {
            char c;
            StringBuilder sb = new StringBuilder();
            while ((c = descriptor.charAt(index++)) == '[')
            {
                sb.append('[');
            }

            switch (c)
            {
                case 'L':
                    while ((c = descriptor.charAt(index++)) != ';')
                    {
                        // TODO
                        sb.append(c);
                    }
                    break;

                case 'V':
                case 'B':
                case 'C':
                case 'I':
                case 'S':
                case 'D':
                case 'F':
                case 'J':
                case 'Z':
                    sb.append(c);
                    break;

                default:
                    throw new IllegalArgumentException(
                        "Invalid type in descriptor: " + c + " from " + descriptor + "[" + index + "]");
            }
            types.add(getTypeRef(sb.toString()));
            return index;
        }

        @Override
        public boolean equals(Object other)
        {
            if (other == null || other.getClass() != getClass())
            {
                return false;
            }

            return Arrays.equals(prototype, ((Descriptor) other).prototype) && type == ((Descriptor) other).type;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = prime + type.hashCode();
            result = prime * result + ((prototype == null) ? 0 : Arrays.hashCode(prototype));
            return result;
        }

        @Override
        public String toString()
        {
            return descriptor;
        }
    }

    private static String binaryToFQN(String binary)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, l = binary.length(); i < l; i++)
        {
            char c = binary.charAt(i);

            if (c == '/')
            {
                sb.append('.');
            }
            else
            {
                sb.append(c);
            }
        }
        String result = sb.toString();
        assert result.length() > 0;
        return result;
    }

    private static String fqnToBinary(String binary)
    {
        return binary.replace('.', '/');
    }


    TypeRef getTypeRefFromFQN(String fqn)
    {
        if (fqn.equals("boolean"))
        {
            return BOOLEAN;
        }

        if (fqn.equals("byte"))
        {
            return BOOLEAN;
        }

        if (fqn.equals("char"))
        {
            return CHAR;
        }

        if (fqn.equals("short"))
        {
            return SHORT;
        }

        if (fqn.equals("int"))
        {
            return INTEGER;
        }

        if (fqn.equals("long"))
        {
            return LONG;
        }

        if (fqn.equals("float"))
        {
            return FLOAT;
        }

        if (fqn.equals("double"))
        {
            return DOUBLE;
        }

        return getTypeRef(fqnToBinary(fqn));
    }


    public Set<String> parseClassFileUses(String path, InputStream in) throws Exception
    {
        DataInputStream din = new DataInputStream(in);
        try
        {
            return new Clazz(this, path).parseClassFileData(din);
        }
        finally
        {
            din.close();
        }
    }

    private static class Clazz
    {

        class ClassConstant
        {
            int cname;
            boolean referred;

            ClassConstant(int class_index)
            {
                this.cname = class_index;
            }

            public String getName()
            {
                return (String) pool[cname];
            }

            @Override
            public String toString()
            {
                return "ClassConstant[" + getName() + "]";
            }
        }


        enum CONSTANT
        {
            Zero(0),
            Utf8,
            Two,
            Integer(4),
            Float(4),
            Long(8),
            Double(8),
            Class(2),
            String(2),
            Fieldref(4),
            Methodref(4),
            InterfaceMethodref(4),
            NameAndType(4),
            Thirteen,
            Fourteen,
            MethodHandle(3),
            MethodType(2),
            Seventeen,
            InvokeDynamic(4),
            Module(2),
            Package(2);
            private final int skip;

            CONSTANT(int skip)
            {
                this.skip = skip;
            }

            CONSTANT()
            {
                this.skip = -1;
            }

            int skip()
            {
                return skip;
            }
        }

        final static int ACC_MODULE = 0x8000;

        static protected class Assoc
        {
            Assoc(CONSTANT tag, int a, int b)
            {
                this.tag = tag;
                this.a = a;
                this.b = b;
            }

            CONSTANT tag;
            int a;
            int b;

            @Override
            public String toString()
            {
                return "Assoc[" + tag + ", " + a + "," + b + "]";
            }
        }

        public abstract class Def
        {

            final int access;

            public Def(int access)
            {
                this.access = access;
            }

        }

        public class FieldDef extends Def
        {
            final String name;
            final Descriptor descriptor;
            String signature;
            Object constant;


            public FieldDef(int access, String name, String descriptor)
            {
                super(access);
                this.name = name;
                this.descriptor = Clazz.this.classParser.getDescriptor(descriptor);
            }


            @Override
            public String toString()
            {
                return name;
            }
        }

        public class MethodDef extends FieldDef
        {
            public MethodDef(int access, String method, String descriptor)
            {
                super(access, method, descriptor);
            }
        }

        boolean hasDefaultConstructor;

        int depth = 0;

        TypeRef className;
        Object pool[];
        int intPool[];
        Set<String> imports = new HashSet<String>();
        String path;
        int minor = 0;
        int major = 0;
        int accessx = 0;
        int forName = 0;
        int class$ = 0;
        TypeRef[] interfaces;
        TypeRef zuper;
        FieldDef last = null;
        final ClassParser classParser;
        String classSignature;

        private boolean detectLdc;

        public Clazz(ClassParser classParser, String path)
        {
            this.path = path;
            this.classParser = classParser;
        }

        Set<String> parseClassFileData(DataInput in) throws Exception
        {

            ++depth;

            boolean crawl = false; // Crawl the byte code if we have a
            // collector
            int magic = in.readInt();
            if (magic != 0xCAFEBABE)
            {
                throw new IOException("Not a valid class file (no CAFEBABE header)");
            }

            minor = in.readUnsignedShort(); // minor version
            major = in.readUnsignedShort(); // major version
            int count = in.readUnsignedShort();
            pool = new Object[count];
            intPool = new int[count];

            CONSTANT[] tags = CONSTANT.values();
            process:
            for (int poolIndex = 1; poolIndex < count; poolIndex++)
            {
                int tagValue = in.readUnsignedByte();
                if (tagValue >= tags.length)
                {
                    throw new IOException("Unrecognized constant pool tag value " + tagValue);
                }
                CONSTANT tag = tags[tagValue];
                switch (tag)
                {
                    case Zero:
                        break process;
                    case Utf8:
                        constantUtf8(in, poolIndex);
                        break;
                    case Integer:
                        constantInteger(in, poolIndex);
                        break;
                    case Float:
                        constantFloat(in, poolIndex);
                        break;
                    // For some insane optimization reason,
                    // the long and double entries take two slots in the
                    // constant pool. See 4.4.5
                    case Long:
                        constantLong(in, poolIndex);
                        poolIndex++;
                        break;
                    case Double:
                        constantDouble(in, poolIndex);
                        poolIndex++;
                        break;
                    case Class:
                        constantClass(in, poolIndex);
                        break;
                    case String:
                        constantString(in, poolIndex);
                        break;
                    case Fieldref:
                    case Methodref:
                    case InterfaceMethodref:
                        ref(in, poolIndex);
                        break;
                    case NameAndType:
                        nameAndType(in, poolIndex, tag);
                        break;
                    case MethodHandle:
                        methodHandle(in, poolIndex, tag);
                        break;
                    case MethodType:
                        methodType(in, poolIndex, tag);
                        break;
                    case InvokeDynamic:
                        invokeDynamic(in, poolIndex, tag);
                        break;
                    default:
                        int skip = tag.skip();
                        if (skip == -1)
                        {
                            throw new IOException("Invalid tag " + tag);
                        }
                        in.skipBytes(skip);
                        break;
                }
            }

            pool(pool, intPool);

            // All name& type and class constant records contain classParser we must
            // treat
            // as references, though not API
            for (Object o : pool)
            {
                if (o == null)
                {
                    continue;
                }

                if (o instanceof Assoc)
                {
                    Assoc assoc = (Assoc) o;
                    switch (assoc.tag)
                    {
                        case Fieldref:
                        case Methodref:
                        case InterfaceMethodref:
                            classConstRef(assoc.a);
                            break;

                        case NameAndType:
                        case MethodType:
                            referTo(assoc.b, 0); // Descriptor
                            break;
                        default:
                            break;
                    }
                }
            }

            //
            // There is a bug in J8 compiler that leaves an
            // orphan class constant. So when we have a CC that
            // is not referenced by fieldrefs, method refs, or other
            // refs then we need to crawl the byte code.
            //
            for (Object o : pool)
            {
                if (o instanceof ClassConstant)
                {
                    ClassConstant cc = (ClassConstant) o;
                    if (cc.referred == false)
                    {
                        detectLdc = true;
                    }
                }
            }

            /*
             * Parse after the constant pool, code thanks to Hans Christian
             * Falkenberg
             */

            accessx = in.readUnsignedShort(); // access

            int this_class = in.readUnsignedShort();
            className = classParser.getTypeRef((String) pool[intPool[this_class]]);
            if (!isModule())
            {
                referTo(className, Modifier.PUBLIC);
            }

            int super_class = in.readUnsignedShort();
            String superName = (String) pool[intPool[super_class]];
            if (superName != null)
            {
                zuper = classParser.getTypeRef(superName);
            }

            if (zuper != null)
            {
                referTo(zuper, accessx);
            }

            int interfacesCount = in.readUnsignedShort();
            if (interfacesCount > 0)
            {
                interfaces = new TypeRef[interfacesCount];
                for (int i = 0; i < interfacesCount; i++)
                {
                    interfaces[i] = classParser.getTypeRef((String) pool[intPool[in.readUnsignedShort()]]);
                    referTo(interfaces[i], accessx);
                }
            }

            int fieldsCount = in.readUnsignedShort();
            for (int i = 0; i < fieldsCount; i++)
            {
                int access_flags = in.readUnsignedShort(); // skip access flags
                int name_index = in.readUnsignedShort();
                int descriptor_index = in.readUnsignedShort();

                // Java prior to 1.5 used a weird
                // static variable to hold the com.X.class
                // result construct. If it did not find it
                // it would create a variable class$com$X
                // that would be used to hold the class
                // object gotten with Class.forName ...
                // Stupidly, they did not actively use the
                // class name for the field type, so bnd
                // would not see a reference. We detect
                // this case and add an artificial descriptor
                String name = pool[name_index].toString(); // name_index
                if (name.startsWith("class$") || name.startsWith("$class$"))
                {
                    crawl = true;
                }

                referTo(descriptor_index, access_flags);
                doAttributes(in, ElementType.FIELD, false, access_flags);
            }

            //
            // Check if we have to crawl the code to find
            // the ldc(_w) <string constant> invokestatic Class.forName
            // if so, calculate the method ref index so we
            // can do this efficiently
            //
            if (crawl)
            {
                forName = findMethodReference("java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
                class$ = findMethodReference(className.getBinary(), "class$", "(Ljava/lang/String;)Ljava/lang/Class;");
            }
            else if (major == 48)
            {
                forName = findMethodReference("java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
                if (forName > 0)
                {
                    crawl = true;
                    class$ = findMethodReference(className.getBinary(), "class$",
                        "(Ljava/lang/String;)Ljava/lang/Class;");
                }
            }

            // There are some serious changes in the
            // class file format. So we do not do any crawling
            // it has also become less important
            // however, jDK8 has a bug that leaves an orphan ClassConstnat
            // so if we have those, we need to also crawl the byte codes.
            // if (major >= JAVA.OpenJDK7.major)

            crawl |= detectLdc;

            //
            // Handle the methods
            //
            int methodCount = in.readUnsignedShort();
            for (int i = 0; i < methodCount; i++)
            {
                int access_flags = in.readUnsignedShort();
                int name_index = in.readUnsignedShort();
                int descriptor_index = in.readUnsignedShort();
                String name = pool[name_index].toString();
                String descriptor = pool[descriptor_index].toString();
                MethodDef mdef = null;
                referTo(descriptor_index, access_flags);

                if ("<init>".equals(name))
                {
                    if (Modifier.isPublic(access_flags) && "()V".equals(descriptor))
                    {
                        hasDefaultConstructor = true;
                    }
                    doAttributes(in, ElementType.CONSTRUCTOR, crawl, access_flags);
                }
                else
                {
                    doAttributes(in, ElementType.METHOD, crawl, access_flags);
                }
            }
            last = null;

            doAttributes(in, ElementType.TYPE, false, accessx);

            //
            // Parse all the classParser we found
            //
            reset();
            return imports;
        }

        private void constantFloat(DataInput in, int poolIndex) throws IOException
        {
            in.skipBytes(4);
        }

        private void constantInteger(DataInput in, int poolIndex) throws IOException
        {
            intPool[poolIndex] = in.readInt();
            pool[poolIndex] = intPool[poolIndex];
        }

        private void pool(@SuppressWarnings("unused") Object[] pool, @SuppressWarnings("unused") int[] intPool)
        {
        }

        private void nameAndType(DataInput in, int poolIndex, CONSTANT tag) throws IOException
        {
            int name_index = in.readUnsignedShort();
            int descriptor_index = in.readUnsignedShort();
            pool[poolIndex] = new Assoc(tag, name_index, descriptor_index);
        }

        private void methodType(DataInput in, int poolIndex, CONSTANT tag) throws IOException
        {
            int descriptor_index = in.readUnsignedShort();
            pool[poolIndex] = new Assoc(tag, 0, descriptor_index);
        }

        private void methodHandle(DataInput in, int poolIndex, CONSTANT tag) throws IOException
        {
            int reference_kind = in.readUnsignedByte();
            int reference_index = in.readUnsignedShort();
            pool[poolIndex] = new Assoc(tag, reference_kind, reference_index);
        }

        private void invokeDynamic(DataInput in, int poolIndex, CONSTANT tag) throws IOException
        {
            int bootstrap_method_attr_index = in.readUnsignedShort();
            int name_and_type_index = in.readUnsignedShort();
            pool[poolIndex] = new Assoc(tag, bootstrap_method_attr_index, name_and_type_index);
        }

        private void ref(DataInput in, int poolIndex) throws IOException
        {
            int class_index = in.readUnsignedShort();
            int name_and_type_index = in.readUnsignedShort();
            pool[poolIndex] = new Assoc(Clazz.CONSTANT.Methodref, class_index, name_and_type_index);
        }

        private void constantString(DataInput in, int poolIndex) throws IOException
        {
            int string_index = in.readUnsignedShort();
            intPool[poolIndex] = string_index;
        }

        private void constantClass(DataInput in, int poolIndex) throws IOException
        {
            int class_index = in.readUnsignedShort();
            intPool[poolIndex] = class_index;
            ClassConstant c = new ClassConstant(class_index);
            pool[poolIndex] = c;
        }

        private void constantDouble(DataInput in, int poolIndex) throws IOException
        {
            in.skipBytes(8);
        }

        private void constantLong(DataInput in, int poolIndex) throws IOException
        {
            in.skipBytes(8);
        }

        private void constantUtf8(DataInput in, int poolIndex) throws IOException
        {
            // CONSTANT_Utf8

            String name = in.readUTF();
            pool[poolIndex] = name;
        }

        private int findMethodReference(String clazz, String methodname, String descriptor)
        {
            for (int i = 1; i < pool.length; i++)
            {
                if (pool[i] instanceof Assoc)
                {
                    Assoc methodref = (Assoc) pool[i];
                    if (methodref.tag == CONSTANT.Methodref)
                    {
                        // Method ref
                        int class_index = methodref.a;
                        int class_name_index = intPool[class_index];
                        if (clazz.equals(pool[class_name_index]))
                        {
                            int name_and_type_index = methodref.b;
                            Assoc name_and_type = (Assoc) pool[name_and_type_index];
                            if (name_and_type.tag == CONSTANT.NameAndType)
                            {
                                // Name and Type
                                int name_index = name_and_type.a;
                                int type_index = name_and_type.b;
                                if (methodname.equals(pool[name_index]))
                                {
                                    if (descriptor.equals(pool[type_index]))
                                    {
                                        return i;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return -1;
        }

        private void doAttributes(DataInput in, ElementType member, boolean crawl, int access_flags) throws Exception
        {
            int attributesCount = in.readUnsignedShort();
            for (int j = 0; j < attributesCount; j++)
            {
                // skip name CONSTANT_Utf8 pointer
                doAttribute(in, member, crawl, access_flags);
            }
        }

        private static long getUnsignedInt(int x)
        {
            return x & 0x00000000ffffffffL;
        }

        private static int getUnsingedByte(byte b)
        {
            return b & 0xFF;
        }

        private static int getUnsingedShort(short s)
        {
            return s & 0xFFFF;
        }

        private void doAttribute(DataInput in, ElementType member, boolean crawl, int access_flags) throws Exception
        {
            final int attribute_name_index = in.readUnsignedShort();
            final String attributeName = (String) pool[attribute_name_index];
            final long attribute_length = getUnsignedInt(in.readInt());
            if (attributeName.equals("Deprecated"))
            {
            }
            else if (attributeName.equals("RuntimeVisibleAnnotations"))
            {
                doAnnotations(in, member, RetentionPolicy.RUNTIME, access_flags);

            }
            else if (attributeName.equals("RuntimeInvisibleAnnotations"))
            {
                doAnnotations(in, member, RetentionPolicy.CLASS, access_flags);

            }
            else if (attributeName.equals("RuntimeVisibleParameterAnnotations"))
            {
                doParameterAnnotations(in, member, RetentionPolicy.RUNTIME, access_flags);

            }
            else if (attributeName.equals("RuntimeInvisibleParameterAnnotations"))
            {
                doParameterAnnotations(in, member, RetentionPolicy.CLASS, access_flags);

            }
            else if (attributeName.equals("RuntimeVisibleTypeAnnotations"))
            {
                doTypeAnnotations(in, member, RetentionPolicy.RUNTIME, access_flags);

            }
            else if (attributeName.equals("RuntimeInvisibleTypeAnnotations"))
            {
                doTypeAnnotations(in, member, RetentionPolicy.CLASS, access_flags);

            }
            else if (attributeName.equals("InnerClasses"))
            {
                doInnerClasses(in);

            }
            else if (attributeName.equals("EnclosingMethod"))
            {
                doEnclosingMethod(in);

            }
            else if (attributeName.equals("SourceFile"))
            {
                doSourceFile(in);

            }
            else if (attributeName.equals("Code"))
            {
                doCode(in, crawl);

            }
            else if (attributeName.equals("Signature"))
            {
                doSignature(in, member, access_flags);

            }
            else if (attributeName.equals("ConstantValue"))
            {
                doConstantValue(in);

            }
            else if (attributeName.equals("AnnotationDefault"))
            {
                doElementValue(in, member, RetentionPolicy.RUNTIME, access_flags);
            }
            else if (attributeName.equals("Exceptions"))
            {
                doExceptions(in, access_flags);

            }
            else if (attributeName.equals("BootstrapMethods"))
            {
                doBootstrapMethods(in);

            }
            else if (attributeName.equals("StackMapTable"))
            {
                doStackMapTable(in);

            }
            else
            {
                if (attribute_length > 0x7FFFFFFF)
                {
                    throw new IllegalArgumentException("Attribute > 2Gb");
                }
                in.skipBytes((int) attribute_length);

            }
        }

        private void doEnclosingMethod(DataInput in) throws IOException
        {
            int cIndex = in.readUnsignedShort();
            int mIndex = in.readUnsignedShort();
            classConstRef(cIndex);
        }

        private void doInnerClasses(DataInput in) throws Exception
        {
            int number_of_classes = in.readUnsignedShort();
            for (int i = 0; i < number_of_classes; i++)
            {
                int inner_class_info_index = in.readUnsignedShort();
                int outer_class_info_index = in.readUnsignedShort();
                int inner_name_index = in.readUnsignedShort();
                int inner_class_access_flags = in.readUnsignedShort();
            }
        }

        void doSignature(DataInput in, ElementType member, int access_flags) throws IOException
        {
            int signature_index = in.readUnsignedShort();
            String signature = (String) pool[signature_index];
            try
            {

                parseDescriptor(signature, access_flags);
                if (last != null)
                {
                    last.signature = signature;
                }

                if (member == ElementType.TYPE)
                {
                    classSignature = signature;
                }

            }
            catch (Exception e)
            {
                throw new RuntimeException("Signature failed for " + signature, e);
            }
        }

        void doConstantValue(DataInput in) throws IOException
        {
            int constantValue_index = in.readUnsignedShort();
        }

        void doExceptions(DataInput in, int access_flags) throws IOException
        {
            int exception_count = in.readUnsignedShort();
            for (int i = 0; i < exception_count; i++)
            {
                int index = in.readUnsignedShort();
                ClassConstant cc = (ClassConstant) pool[index];
                TypeRef clazz = classParser.getTypeRef(cc.getName());
                referTo(clazz, access_flags);
            }
        }

        private void doCode(DataInput in, boolean crawl) throws Exception
        {
            /* int max_stack = */
            in.readUnsignedShort();
            /* int max_locals = */
            in.readUnsignedShort();
            int code_length = in.readInt();
            byte code[] = new byte[code_length];
            in.readFully(code, 0, code_length);
            if (crawl)
            {
                crawl(code);
            }
            int exception_table_length = in.readUnsignedShort();
            for (int i = 0; i < exception_table_length; i++)
            {
                int start_pc = in.readUnsignedShort();
                int end_pc = in.readUnsignedShort();
                int handler_pc = in.readUnsignedShort();
                int catch_type = in.readUnsignedShort();
                classConstRef(catch_type);
            }
            doAttributes(in, ElementType.METHOD, false, 0);
        }

        private void crawl(byte[] code)
        {
            ByteBuffer bb = ByteBuffer.wrap(code);
            int lastReference = -1;

            while (bb.remaining() > 0)
            {
                int instruction = getUnsingedByte(bb.get());
                switch (instruction)
                {
                    case ldc:
                        lastReference = getUnsingedByte(bb.get());
                        classConstRef(lastReference);
                        break;

                    case ldc_w:
                        lastReference = getUnsingedShort(bb.getShort());
                        classConstRef(lastReference);
                        break;

                    case anewarray:
                    case checkcast:
                    case instanceof_:
                    case new_:
                    {
                        int cref = getUnsingedShort(bb.getShort());
                        classConstRef(cref);
                        lastReference = -1;
                        break;
                    }

                    case multianewarray:
                    {
                        int cref = getUnsingedShort(bb.getShort());
                        classConstRef(cref);
                        bb.get();
                        lastReference = -1;
                        break;
                    }

                    case invokespecial:
                    {
                        int mref = getUnsingedShort(bb.getShort());
                        break;
                    }

                    case invokevirtual:
                    {
                        int mref = getUnsingedShort(bb.getShort());
                        break;
                    }

                    case invokeinterface:
                    {
                        int mref = getUnsingedShort(bb.getShort());
                        bb.get(); // read past the 'count' operand
                        bb.get(); // read past the reserved space for future operand
                        break;
                    }

                    case invokestatic:
                    {
                        int methodref = getUnsingedShort(bb.getShort());

                        if ((methodref == forName || methodref == class$) && lastReference != -1
                            && pool[intPool[lastReference]] instanceof String)
                        {
                            String fqn = (String) pool[intPool[lastReference]];
                            if (!fqn.equals("class") && fqn.indexOf('.') > 0)
                            {
                                TypeRef clazz = classParser.getTypeRefFromFQN(fqn);
                                referTo(clazz, 0);
                            }
                            lastReference = -1;
                        }
                        break;
                    }

                    /*
                     * 3/5: opcode, indexbyte1, indexbyte2 or iinc, indexbyte1,
                     * indexbyte2, countbyte1, countbyte2
                     */
                    case wide:
                        int opcode = getUnsingedByte(bb.get());
                        bb.getShort(); // at least 3 bytes
                        if (opcode == iinc)
                        {
                            bb.getShort();
                        }
                        break;

                    case tableswitch:
                        // Skip to place divisible by 4
                        while ((bb.position() & 0x3) != 0)
                        {
                            bb.get();
                        }
                        /* int deflt = */
                        bb.getInt();
                        int low = bb.getInt();
                        int high = bb.getInt();
                        bb.position(bb.position() + (high - low + 1) * 4);
                        lastReference = -1;
                        break;

                    case lookupswitch:
                        // Skip to place divisible by 4
                        while ((bb.position() & 0x3) != 0)
                        {
                            int n = bb.get();
                            assert n == 0; // x
                        }
                        /* deflt = */
                        int deflt = bb.getInt();
                        int npairs = bb.getInt();
                        bb.position(bb.position() + npairs * 8);
                        lastReference = -1;
                        break;

                    default:
                        lastReference = -1;
                        bb.position(bb.position() + OFFSETS[instruction]);
                }
            }
        }

        private void doSourceFile(DataInput in) throws IOException
        {
            int sourcefile_index = in.readUnsignedShort();
        }

        private void doParameterAnnotations(DataInput in, ElementType member, RetentionPolicy policy, int access_flags)
            throws Exception
        {
            int num_parameters = in.readUnsignedByte();
            for (int p = 0; p < num_parameters; p++)
            {
                doAnnotations(in, member, policy, access_flags);
            }
        }

        private void doTypeAnnotations(DataInput in, ElementType member, RetentionPolicy policy, int access_flags)
            throws Exception
        {
            int num_annotations = in.readUnsignedShort();
            for (int p = 0; p < num_annotations; p++)
            {

                // type_annotation {
                // u1 target_type;
                // union {
                // type_parameter_target;
                // supertype_target;
                // type_parameter_bound_target;
                // empty_target;
                // method_formal_parameter_target;
                // throws_target;
                // localvar_target;
                // catch_target;
                // offset_target;
                // type_argument_target;
                // } target_info;
                // type_path target_path;
                // u2 type_index;
                // u2 num_element_value_pairs;
                // { u2 element_name_index;
                // element_value value;
                // } element_value_pairs[num_element_value_pairs];
                // }

                // Table 4.7.20-A. Interpretation of target_type values (Part 1)

                int target_type = in.readUnsignedByte();
                switch (target_type)
                {
                    case 0x00: // type parameter declaration of generic class or
                        // interface
                    case 0x01: // type parameter declaration of generic method or
                        // constructor
                        //
                        // type_parameter_target {
                        // u1 type_parameter_index;
                        // }
                        in.skipBytes(1);
                        break;

                    case 0x10: // type in extends clause of class or interface
                        // declaration (including the direct superclass of
                        // an anonymous class declaration), or in implements
                        // clause of interface declaration
                        // supertype_target {
                        // u2 supertype_index;
                        // }

                        in.skipBytes(2);
                        break;

                    case 0x11: // type in bound of type parameter declaration of
                        // generic class or interface
                    case 0x12: // type in bound of type parameter declaration of
                        // generic method or constructor
                        // type_parameter_bound_target {
                        // u1 type_parameter_index;
                        // u1 bound_index;
                        // }
                        in.skipBytes(2);
                        break;

                    case 0x13: // type in field declaration
                    case 0x14: // return type of method, or type of newly
                        // constructed object
                    case 0x15: // receiver type of method or constructor
                        break;

                    case 0x16: // type in formal parameter declaration of method,
                        // constructor, or lambda expression
                        // formal_parameter_target {
                        // u1 formal_parameter_index;
                        // }
                        in.skipBytes(1);
                        break;

                    case 0x17: // type in throws clause of method or constructor
                        // throws_target {
                        // u2 throws_type_index;
                        // }
                        in.skipBytes(2);
                        break;

                    case 0x40: // type in local variable declaration
                    case 0x41: // type in resource variable declaration
                        // localvar_target {
                        // u2 table_length;
                        // { u2 start_pc;
                        // u2 length;
                        // u2 index;
                        // } table[table_length];
                        // }
                        int table_length = in.readUnsignedShort();
                        in.skipBytes(table_length * 6);
                        break;

                    case 0x42: // type in exception parameter declaration
                        // catch_target {
                        // u2 exception_table_index;
                        // }
                        in.skipBytes(2);
                        break;

                    case 0x43: // type in instanceof expression
                    case 0x44: // type in new expression
                    case 0x45: // type in method reference expression using ::new
                    case 0x46: // type in method reference expression using
                        // ::Identifier
                        // offset_target {
                        // u2 offset;
                        // }
                        in.skipBytes(2);
                        break;

                    case 0x47: // type in cast expression
                    case 0x48: // type argument for generic constructor in new
                        // expression or explicit constructor invocation
                        // statement

                    case 0x49: // type argument for generic method in method
                        // invocation expression
                    case 0x4A: // type argument for generic constructor in method
                        // reference expression using ::new
                    case 0x4B: // type argument for generic method in method
                        // reference expression using ::Identifier
                        // type_argument_target {
                        // u2 offset;
                        // u1 type_argument_index;
                        // }
                        in.skipBytes(3);
                        break;

                }

                // The value of the target_path item denotes precisely which part of
                // the type indicated by target_info is annotated. The format of the
                // type_path structure is specified in §4.7.20.2.
                //
                // type_path {
                // u1 path_length;
                // { u1 type_path_kind;
                // u1 type_argument_index;
                // } path[path_length];
                // }

                int path_length = in.readUnsignedByte();
                in.skipBytes(path_length * 2);

                //
                // Rest is identical to the normal annotations
                doAnnotation(in, member, policy, access_flags);
            }
        }

        private void doAnnotations(DataInput in, ElementType member, RetentionPolicy policy, int access_flags)
            throws Exception
        {
            int num_annotations = in.readUnsignedShort(); // # of annotations
            for (int a = 0; a < num_annotations; a++)
            {
                doAnnotation(in, member, policy, access_flags);
            }
        }

        // annotation {
        // u2 type_index;
        // u2 num_element_value_pairs; {
        // u2 element_name_index;
        // element_value value;
        // }
        // element_value_pairs[num_element_value_pairs];
        // }

        private void doAnnotation(DataInput in, ElementType member, RetentionPolicy policy, int access_flags) throws IOException
        {
            int type_index = in.readUnsignedShort();

            String typeName = (String) pool[type_index];
            if (typeName != null)
            {
                if (policy == RetentionPolicy.RUNTIME)
                {
                    referTo(type_index, 0);
                }
            }
            int num_element_value_pairs = in.readUnsignedShort();

            for (int v = 0; v < num_element_value_pairs; v++)
            {
                in.readUnsignedShort();
                doElementValue(in, member, policy, access_flags);
            }
        }

        private Object doElementValue(DataInput in, ElementType member, RetentionPolicy policy, int access_flags) throws IOException
        {
            char tag = (char) in.readUnsignedByte();
            switch (tag)
            {
                case 'B': // Byte
                case 'C': // Character
                case 'I': // Integer
                case 'S': // Short
                    int const_value_index = in.readUnsignedShort();
                    return intPool[const_value_index];

                case 'D': // Double
                case 'F': // Float
                case 's': // String
                case 'J': // Long
                    const_value_index = in.readUnsignedShort();
                    return pool[const_value_index];

                case 'Z': // Boolean
                    const_value_index = in.readUnsignedShort();
                    return pool[const_value_index] == null || pool[const_value_index].equals(0) ? false : true;

                case 'e': // enum constant
                    int type_name_index = in.readUnsignedShort();
                    if (policy == RetentionPolicy.RUNTIME)
                    {
                        referTo(type_name_index, 0);
                    }
                    int const_name_index = in.readUnsignedShort();
                    return pool[const_name_index];

                case 'c': // Class
                    int class_info_index = in.readUnsignedShort();
                    TypeRef name = classParser.getTypeRef((String) pool[class_info_index]);
                    if (policy == RetentionPolicy.RUNTIME)
                    {
                        referTo(class_info_index, 0);
                    }
                    return name;

                case '@': // Annotation type
                    doAnnotation(in, member, policy, access_flags);

                case '[': // Array
                    int num_values = in.readUnsignedShort();
                    Object[] result = new Object[num_values];
                    for (int i = 0; i < num_values; i++)
                    {
                        result[i] = doElementValue(in, member, policy, access_flags);
                    }
                    return result;

                default:
                    throw new IllegalArgumentException("Invalid value for Annotation ElementValue tag " + tag);
            }
        }

        /*
         * We don't currently process BootstrapMethods. We walk the data structure
         * to consume the attribute.
         */
        private void doBootstrapMethods(DataInput in) throws IOException
        {
            final int num_bootstrap_methods = in.readUnsignedShort();
            for (int v = 0; v < num_bootstrap_methods; v++)
            {
                final int bootstrap_method_ref = in.readUnsignedShort();
                final int num_bootstrap_arguments = in.readUnsignedShort();
                for (int a = 0; a < num_bootstrap_arguments; a++)
                {
                    final int bootstrap_argument = in.readUnsignedShort();
                }
            }
        }

        /*
         * The verifier can require access to types only referenced in StackMapTable
         * attributes.
         */
        private void doStackMapTable(DataInput in) throws IOException
        {
            final int number_of_entries = in.readUnsignedShort();
            for (int v = 0; v < number_of_entries; v++)
            {
                final int frame_type = in.readUnsignedByte();
                if (frame_type <= 63)
                { // same_frame
                    // nothing else to do
                }
                else if (frame_type <= 127)
                { // same_locals_1_stack_item_frame
                    verification_type_info(in);
                }
                else if (frame_type <= 246)
                { // RESERVED
                    // nothing else to do
                }
                else if (frame_type <= 247)
                { // same_locals_1_stack_item_frame_extended
                    final int offset_delta = in.readUnsignedShort();
                    verification_type_info(in);
                }
                else if (frame_type <= 250)
                { // chop_frame
                    final int offset_delta = in.readUnsignedShort();
                }
                else if (frame_type <= 251)
                { // same_frame_extended
                    final int offset_delta = in.readUnsignedShort();
                }
                else if (frame_type <= 254)
                { // append_frame
                    final int offset_delta = in.readUnsignedShort();
                    final int number_of_locals = frame_type - 251;
                    for (int n = 0; n < number_of_locals; n++)
                    {
                        verification_type_info(in);
                    }
                }
                else if (frame_type <= 255)
                { // full_frame
                    final int offset_delta = in.readUnsignedShort();
                    final int number_of_locals = in.readUnsignedShort();
                    for (int n = 0; n < number_of_locals; n++)
                    {
                        verification_type_info(in);
                    }
                    final int number_of_stack_items = in.readUnsignedShort();
                    for (int n = 0; n < number_of_stack_items; n++)
                    {
                        verification_type_info(in);
                    }
                }
            }
        }

        private void verification_type_info(DataInput in) throws IOException
        {
            final int tag = in.readUnsignedByte();
            switch (tag)
            {
                case 7:// Object_variable_info
                    final int cpool_index = in.readUnsignedShort();
                    classConstRef(cpool_index);
                    break;
                case 8:// ITEM_Uninitialized
                    final int offset = in.readUnsignedShort();
                    break;
            }
        }

        void referTo(TypeRef typeRef, int modifiers)
        {
            if (typeRef.isPrimitive())
            {
                return;
            }

            PackageRef packageRef = typeRef.getPackageRef();
            if (packageRef.isPrimitivePackage())
            {
                return;
            }

            imports.add(packageRef.getFQN());
        }

        void referTo(int index, int modifiers)
        {
            String descriptor = (String) pool[index];
            parseDescriptor(descriptor, modifiers);
        }

        /*
         * This method parses a descriptor and adds the package of the descriptor to
         * the referenced packages. The syntax of the descriptor is:
         *
         * <pre>
         * descriptor ::= ( '(' reference * ')' )? reference reference ::= 'L'
         * classname ( '&lt;' references '&gt;' )? ';' | 'B' | 'Z' | ... | '+' | '-'
         * | '['
         * </pre>
         *
         * This methods uses heavy recursion to parse the descriptor and a roving
         * pointer to limit the creation of string objects.
         *
         * @param descriptor The to be parsed descriptor
         * @param modifiers
         */

        public void parseDescriptor(String descriptor, int modifiers)
        {
            // Some classParser are weird, they start with a generic
            // declaration that contains ':', not sure what they mean ...
            int rover = 0;
            if (descriptor.charAt(0) == '<')
            {
                rover = parseFormalTypeParameters(descriptor, rover, modifiers);
            }

            if (descriptor.charAt(rover) == '(')
            {
                rover = parseReferences(descriptor, rover + 1, ')', modifiers);
                rover++;
            }
            parseReferences(descriptor, rover, (char) 0, modifiers);
        }

        /*
         * Parse a sequence of references. A sequence ends with a given character or
         * when the string ends.
         *
         * @param descriptor The whole descriptor.
         * @param rover The index in the descriptor
         * @param delimiter The end character or 0
         * @return the last index processed, one character after the delimeter
         */
        int parseReferences(String descriptor, int rover, char delimiter, int modifiers)
        {
            int r = rover;
            while (r < descriptor.length() && descriptor.charAt(r) != delimiter)
            {
                r = parseReference(descriptor, r, modifiers);
            }
            return r;
        }

        /*
         * Parse a single reference. This can be a single character or an object
         * reference when it starts with 'L'.
         *
         * @param descriptor The descriptor
         * @param rover The place to start
         * @return The return index after the reference
         */
        int parseReference(String descriptor, int rover, int modifiers)
        {
            int r = rover;
            char c = descriptor.charAt(r);
            while (c == '[')
            {
                c = descriptor.charAt(++r);
            }

            if (c == '<')
            {
                r = parseReferences(descriptor, r + 1, '>', modifiers);
            }
            else if (c == 'T')
            {
                // Type variable name
                r++;
                while (descriptor.charAt(r) != ';')
                {
                    r++;
                }
            }
            else if (c == 'L')
            {
                StringBuilder sb = new StringBuilder();
                r++;
                while ((c = descriptor.charAt(r)) != ';')
                {
                    if (c == '<')
                    {
                        r = parseReferences(descriptor, r + 1, '>', modifiers);
                    }
                    else
                    {
                        sb.append(c);
                    }
                    r++;
                }
                TypeRef ref = classParser.getTypeRef(sb.toString());

                referTo(ref, modifiers);
            }
            else
            {
                if ("+-*BCDFIJSZV".indexOf(c) < 0)
                {
                    ;// System.err.println("Should not skip: " + c);
                }
            }

            // this skips a lot of characters
            // [, *, +, -, B, etc.

            return r + 1;
        }

        /*
         * FormalTypeParameters
         *
         * @param descriptor
         * @param index
         */
        private int parseFormalTypeParameters(String descriptor, int index, int modifiers)
        {
            index++;
            while (descriptor.charAt(index) != '>')
            {
                // Skip IDENTIFIER
                index = descriptor.indexOf(':', index) + 1;
                if (index == 0)
                {
                    throw new IllegalArgumentException("Expected ClassBound or InterfaceBounds: " + descriptor);
                }

                // ClassBound? InterfaceBounds
                char c = descriptor.charAt(index);

                if (c != ':')
                {
                    // ClassBound?
                    index = parseReference(descriptor, index, modifiers);
                    c = descriptor.charAt(index);
                }

                // InterfaceBounds*
                while (c == ':')
                {
                    index++;
                    index = parseReference(descriptor, index, modifiers);
                    c = descriptor.charAt(index);
                } // for each interface

            } // for each formal parameter
            return index + 1; // skip >
        }

        public Set<String> getReferred()
        {
            return imports;
        }

        /*
         * .class construct for different compilers sun 1.1 Detect static variable
         * class$com$acme$MyClass 1.2 " 1.3 " 1.4 " 1.5 ldc_w (class) 1.6 " eclipse
         * 1.1 class$0, ldc (string), invokestatic Class.forName 1.2 " 1.3 " 1.5 ldc
         * (class) 1.6 " 1.5 and later is not an issue, sun pre 1.5 is easy to
         * detect the static variable that decodes the class name. For eclipse, the
         * class$0 gives away we have a reference encoded in a string.
         * compilerversions/compilerversions.jar contains test versions of all
         * versions/compilers.
         */

        public void reset()
        {
            if (--depth == 0)
            {
                pool = null;
                intPool = null;
            }
        }

        @Override
        public String toString()
        {
            if (className != null)
            {
                return className.getFQN();
            }
            return super.toString();
        }


        public boolean isModule()
        {
            return (ACC_MODULE & accessx) != 0;
        }

        private void classConstRef(int lastReference)
        {
            Object o = pool[lastReference];
            if (o == null)
            {
                return;
            }

            if (o instanceof ClassConstant)
            {
                ClassConstant cc = (ClassConstant) o;
                if (cc.referred)
                {
                    return;
                }
                cc.referred = true;
                String name = cc.getName();
                if (name != null)
                {
                    TypeRef tr = classParser.getTypeRef(name);
                    referTo(tr, 0);
                }
            }

        }


        // the stack
        final static short bipush = 0x10;            // byte ? value
        // pushes a
        // byte
        // onto the stack as an integer
        // value
        final static short sipush = 0x11;            // byte1, byte2 ?
        // value
        // pushes a
        // signed integer (byte1 << 8 +
        // byte2) onto the stack
        final static short ldc = 0x12;            // index ? value
        // pushes
        // a
        // constant #index from a
        // constant pool (String, int,
        // float or class type) onto the
        // stack
        final static short ldc_w = 0x13;            // indexbyte1,
        // indexbyte2 ?
        // value pushes a constant
        // #index from a constant pool
        // (String, int, float or class
        // type) onto the stack (wide
        // index is constructed as
        // indexbyte1 << 8 + indexbyte2)
        final static short ldc2_w = 0x14;            // indexbyte1,
        // indexbyte2 ?
        // value pushes a constant
        // #index from a constant pool
        // (double or long) onto the
        // stack (wide index is
        // constructed as indexbyte1 <<
        // 8 + indexbyte2)
        final static short iload = 0x15;            // index ? value
        // loads
        // an int
        // value from a variable #index
        final static short lload = 0x16;            // index ? value
        // load a
        // long
        // value from a local variable
        // #index
        final static short fload = 0x17;            // index ? value
        // loads a
        // float
        // value from a local variable
        // #index
        final static short dload = 0x18;            // index ? value
        // loads a
        // double
        // value from a local variable
        // #index
        final static short aload = 0x19;            // index ? objectref
        // loads a
        // reference onto the stack from
        // short from array
        final static short istore = 0x36;            // index value ?
        // store
        // int value
        // into variable #index
        final static short lstore = 0x37;            // index value ?
        // store a
        // long
        // value in a local variable
        // #index
        final static short fstore = 0x38;            // index value ?
        // stores
        // a float
        // value into a local variable
        // #index
        final static short dstore = 0x39;            // index value ?
        // stores
        // a double
        // longs
        final static short iinc = 0x84;            // index, const [No
        // change]
        // increment local variable
        // compares two doubles
        final static short ifeq = 0x99;            // branchbyte1,
        // branchbyte2
        // a long from an array
        final static short astore = 0x3a;            // index objectref ?
        // stores a
        // reference into a local
        // double to a long
        final static short ifne = 0x9a;            // branchbyte1,
        // branchbyte2
        // value ? if value is not 0,
        // branch to instruction at
        // branchoffset (signed short
        // constructed from unsigned
        // bytes branchbyte1 << 8 +
        // branchbyte2)
        final static short iflt = 0x9b;            // branchbyte1,
        // branchbyte2
        // value ? if value is less than
        // 0, branch to instruction at
        // branchoffset (signed short
        // constructed from unsigned
        // bytes branchbyte1 << 8 +
        // branchbyte2)
        final static short ifge = 0x9c;            // branchbyte1,
        // branchbyte2
        // value ? if value is greater
        // than or equal to 0, branch to
        // instruction at branchoffset
        // (signed short constructed
        // from unsigned bytes
        // branchbyte1 << 8 +
        // branchbyte2)
        final static short ifgt = 0x9d;            // branchbyte1,
        // branchbyte2
        // value ? if value is greater
        // than 0, branch to instruction
        // at branchoffset (signed short
        // constructed from unsigned
        // bytes branchbyte1 << 8 +
        // branchbyte2)
        final static short ifle = 0x9e;            // branchbyte1,
        // branchbyte2
        // value ? if value is less than
        // or equal to 0, branch to
        // instruction at branchoffset
        // (signed short constructed
        // from unsigned bytes
        // branchbyte1 << 8 +
        // branchbyte2)
        final static short if_icmpeq = 0x9f;            // branchbyte1,
        // branchbyte2
        // value1, value2 ? if ints are
        // equal, branch to instruction
        // at branchoffset (signed short
        // constructed from unsigned
        // bytes branchbyte1 << 8 +
        // branchbyte2)
        final static short if_icmpne = 0xa0;            // branchbyte1,
        // branchbyte2
        // value1, value2 ? if ints are
        // not equal, branch to
        // instruction at branchoffset
        // (signed short constructed
        // from unsigned bytes
        // branchbyte1 << 8 +
        // branchbyte2)
        final static short if_icmplt = 0xa1;            // branchbyte1,
        // branchbyte2
        // value1, value2 ? if value1 is
        // less than value2, branch to
        // instruction at branchoffset
        // (signed short constructed
        // from unsigned bytes
        // branchbyte1 << 8 +
        // branchbyte2)
        final static short if_icmpge = 0xa2;            // branchbyte1,
        // branchbyte2
        // value1, value2 ? if value1 is
        // greater than or equal to
        // value2, branch to instruction
        // at branchoffset (signed short
        // constructed from unsigned
        // bytes branchbyte1 << 8 +
        // branchbyte2)
        final static short if_icmpgt = 0xa3;            // branchbyte1,
        // branchbyte2
        // value1, value2 ? if value1 is
        // greater than value2, branch
        // to instruction at
        // branchoffset (signed short
        // constructed from unsigned
        // bytes branchbyte1 << 8 +
        // branchbyte2)
        final static short if_icmple = 0xa4;            // branchbyte1,
        // branchbyte2
        // value1, value2 ? if value1 is
        // less than or equal to value2,
        // branch to instruction at
        // branchoffset (signed short
        // constructed from unsigned
        // bytes branchbyte1 << 8 +
        // branchbyte2)
        final static short if_acmpeq = 0xa5;            // branchbyte1,
        // branchbyte2
        // value1, value2 ? if
        // references are equal, branch
        // to instruction at
        // branchoffset (signed short
        // constructed from unsigned
        // bytes branchbyte1 << 8 +
        // branchbyte2)
        final static short if_acmpne = 0xa6;            // branchbyte1,
        // branchbyte2
        // value1, value2 ? if
        // references are not equal,
        // branch to instruction at
        // branchoffset (signed short
        // constructed from unsigned
        // bytes branchbyte1 << 8 +
        // branchbyte2)
        final static short goto_ = 0xa7;            // branchbyte1,
        // branchbyte2 [no
        // change] goes to another
        // instruction at branchoffset
        // (signed short constructed
        // from unsigned bytes
        // branchbyte1 << 8 +
        // branchbyte2)
        final static short jsr = 0xa8;            // branchbyte1,
        // branchbyte2 ?
        // address jump to subroutine at
        // branchoffset (signed short
        // constructed from unsigned
        // bytes branchbyte1 << 8 +
        // branchbyte2) and place the
        // return address on the stack
        final static short ret = 0xa9;            // index [No change]
        // continue
        // execution from address taken
        // from a local variable #index
        // (the asymmetry with jsr is
        // intentional)
        final static short tableswitch = 0xaa;            // [0-3 bytes
        // padding],
        // defaultbyte1, defaultbyte2,
        // defaultbyte3, defaultbyte4,
        // lowbyte1, lowbyte2, lowbyte3,
        // lowbyte4, highbyte1,
        // highbyte2, highbyte3,
        // highbyte4, jump offsets...
        // index ? continue execution
        // from an address in the table
        // at offset index
        final static short lookupswitch = 0xab;            // <0-3 bytes
        // padding>,
        // defaultbyte1, defaultbyte2,
        // from
        // method
        final static short getstatic = 0xb2;            // index1, index2 ?
        // value gets a
        // static field value of a
        // class, where the field is
        // identified by field reference
        // in the constant pool index
        // (index1 << 8 + index2)
        final static short putstatic = 0xb3;            // indexbyte1,
        // indexbyte2 value
        // ? set static field to value
        // in a class, where the field
        // is identified by a field
        // reference index in constant
        // pool (indexbyte1 << 8 +
        // indexbyte2)
        final static short getfield = 0xb4;            // index1, index2
        // objectref ?
        // value gets a field value of
        // an object objectref, where
        // the field is identified by
        // field reference in the
        // constant pool index (index1
        // << 8 + index2)
        final static short putfield = 0xb5;            // indexbyte1,
        // indexbyte2
        // objectref, value ? set field
        // to value in an object
        // objectref, where the field is
        // identified by a field
        // reference index in constant
        // pool (indexbyte1 << 8 +
        // indexbyte2)
        final static short invokevirtual = 0xb6;            // indexbyte1,
        // indexbyte2
        // objectref, [arg1, arg2, ...]
        // ? invoke virtual method on
        // object objectref, where the
        // method is identified by
        // method reference index in
        // constant pool (indexbyte1 <<
        // 8 + indexbyte2)
        final static short invokespecial = 0xb7;            // indexbyte1,
        // indexbyte2
        // objectref, [arg1, arg2, ...]
        // ? invoke instance method on
        // object objectref, where the
        // method is identified by
        // method reference index in
        // constant pool (indexbyte1 <<
        // 8 + indexbyte2)
        final static short invokestatic = 0xb8;            // indexbyte1,
        // indexbyte2 [arg1,
        // arg2, ...] ? invoke a static
        // method, where the method is
        // identified by method
        // reference index in constant
        // pool (indexbyte1 << 8 +
        // indexbyte2)
        final static short invokeinterface = 0xb9;            // indexbyte1,
        // indexbyte2,
        // count, 0 objectref, [arg1,
        // arg2, ...] ? invokes an
        // interface method on object
        // objectref, where the
        // interface method is
        // identified by method
        // reference index in constant
        // pool (indexbyte1 << 8 +
        // indexbyte2)
        final static short invokedynamic = 0xba;            // introduced in J7

        final static short new_ = 0xbb;            // indexbyte1,
        // indexbyte2 ?
        // objectref creates new object
        // of type identified by class
        // reference in constant pool
        // index (indexbyte1 << 8 +
        // indexbyte2)
        final static short newarray = 0xbc;            // atype count ?
        // arrayref
        // creates new array with count
        // elements of primitive type
        // identified by atype
        final static short anewarray = 0xbd;            // indexbyte1,
        // indexbyte2 count
        // objectref throws an error or
        // exception (notice that the
        // rest of the stack is cleared,
        // leaving only a reference to
        // the Throwable)
        final static short checkcast = 0xc0;            // indexbyte1,
        // indexbyte2
        // objectref ? objectref checks
        // whether an objectref is of a
        // certain type, the class
        // reference of which is in the
        // constant pool at index
        // (indexbyte1 << 8 +
        // indexbyte2)
        final static short instanceof_ = 0xc1;            // indexbyte1,
        // indexbyte2
        // object ("release the lock" -
        // end of synchronized()
        // section)
        final static short wide = 0xc4;            // opcode,
        // indexbyte1,
        // indexbyte2
        final static short multianewarray = 0xc5;            // indexbyte1,
        // indexbyte2,
        // dimensions count1,
        // [count2,...] ? arrayref
        // create a new array of
        // dimensions dimensions with
        // elements of type identified
        // by class reference in
        // constant pool index
        // (indexbyte1 << 8 +
        // indexbyte2); the sizes of
        // each dimension is identified
        // by count1, [count2, etc]
        final static short ifnull = 0xc6;            // branchbyte1,
        // branchbyte2
        // value ? if value is null,
        // branch to instruction at
        // branchoffset (signed short
        // constructed from unsigned
        // bytes branchbyte1 << 8 +
        // branchbyte2)
        final static short ifnonnull = 0xc7;            // branchbyte1,
        // branchbyte2
        // value ? if value is not null,
        // branch to instruction at
        // branchoffset (signed short
        // constructed from unsigned
        // bytes branchbyte1 << 8 +
        // branchbyte2)
        final static short goto_w = 0xc8;            // branchbyte1,
        // branchbyte2,
        // branchbyte3, branchbyte4 [no
        // change] goes to another
        // instruction at branchoffset
        // (signed int constructed from
        // unsigned bytes branchbyte1 <<
        // 24 + branchbyte2 << 16 +
        // branchbyte3 << 8 +
        // branchbyte4)
        final static short jsr_w = 0xc9;            // branchbyte1,
        // branchbyte2,


        final static byte OFFSETS[] = new byte[256];

        static
        {
            OFFSETS[bipush] = 1; // byte ? value pushes a byte onto the
            // stack as an integer value
            OFFSETS[sipush] = 2; // byte1, byte2 ? value pushes a signed
            // integer (byte1 << 8 + byte2) onto the
            // stack
            OFFSETS[ldc] = 1; // index ? value pushes a constant
            // #index from a constant pool (String,
            // int, float or class type) onto the
            // stack
            OFFSETS[ldc_w] = 2; // indexbyte1, indexbyte2 ? value pushes
            // a constant #index from a constant
            // pool (String, int, float or class
            // type) onto the stack (wide index is
            // constructed as indexbyte1 << 8 +
            // indexbyte2)
            OFFSETS[ldc2_w] = 2; // indexbyte1, indexbyte2 ? value pushes
            // a constant #index from a constant
            // pool (double or long) onto the stack
            // (wide index is constructed as
            // indexbyte1 << 8 + indexbyte2)
            OFFSETS[iload] = 1; // index ? value loads an int value from
            // a variable #index
            OFFSETS[lload] = 1; // index ? value load a long value from
            // a local variable #index
            OFFSETS[fload] = 1; // index ? value loads a float value
            // from a local variable #index
            OFFSETS[dload] = 1; // index ? value loads a double value
            // from a local variable #index
            OFFSETS[aload] = 1; // index ? objectref loads a reference
            // onto the stack from a local variable
            // #index
            OFFSETS[istore] = 1; // index value ? store int value into
            // variable #index
            OFFSETS[lstore] = 1; // index value ? store a long value in a
            // local variable #index
            OFFSETS[fstore] = 1; // index value ? stores a float value
            // into a local variable #index
            OFFSETS[dstore] = 1; // index value ? stores a double value
            // into a local variable #index
            OFFSETS[iinc] = 2; // index, const [No change] increment
            // local variable #index by signed byte
            // const
            OFFSETS[ifeq] = 2; // branchbyte1, branchbyte2 value ? if
            // value is 0, branch to instruction at
            // branchoffset (signed short
            // constructed from unsigned bytes
            // branchbyte1 << 8 + branchbyte2)
            OFFSETS[astore] = 1; // index objectref ? stores a reference
            // into a local variable #index
            OFFSETS[ifne] = 2; // branchbyte1, branchbyte2 value ? if
            // value is not 0, branch to instruction
            // at branchoffset (signed short
            // constructed from unsigned bytes
            // branchbyte1 << 8 + branchbyte2)
            OFFSETS[iflt] = 2; // branchbyte1, branchbyte2 value ? if
            // value is less than 0, branch to
            // instruction at branchoffset (signed
            // short constructed from unsigned bytes
            // branchbyte1 << 8 + branchbyte2)
            OFFSETS[ifge] = 2; // branchbyte1, branchbyte2 value ? if
            // value is greater than or equal to 0,
            // branch to instruction at branchoffset
            // (signed short constructed from
            // unsigned bytes branchbyte1 << 8 +
            // branchbyte2)
            OFFSETS[ifgt] = 2; // branchbyte1, branchbyte2 value ? if
            // value is greater than 0, branch to
            // instruction at branchoffset (signed
            // short constructed from unsigned bytes
            // branchbyte1 << 8 + branchbyte2)
            OFFSETS[ifle] = 2; // branchbyte1, branchbyte2 value ? if
            // value is less than or equal to 0,
            // branch to instruction at branchoffset
            // (signed short constructed from
            // unsigned bytes branchbyte1 << 8 +
            // branchbyte2)
            OFFSETS[if_icmpeq] = 2; // branchbyte1, branchbyte2 value1,
            // value2 ? if ints are equal,
            // branch to instruction at
            // branchoffset (signed short
            // constructed from unsigned bytes
            // branchbyte1 << 8 + branchbyte2)
            OFFSETS[if_icmpne] = 2; // branchbyte1, branchbyte2 value1,
            // value2 ? if ints are not equal,
            // branch to instruction at
            // branchoffset (signed short
            // constructed from unsigned bytes
            // branchbyte1 << 8 + branchbyte2)
            OFFSETS[if_icmplt] = 2; // branchbyte1, branchbyte2 value1,
            // value2 ? if value1 is less than
            // value2, branch to instruction at
            // branchoffset (signed short
            // constructed from unsigned bytes
            // branchbyte1 << 8 + branchbyte2)
            OFFSETS[if_icmpge] = 2; // branchbyte1, branchbyte2 value1,
            // value2 ? if value1 is greater
            // than or equal to value2, branch
            // to instruction at branchoffset
            // (signed short constructed from
            // unsigned bytes branchbyte1 << 8 +
            // branchbyte2)
            OFFSETS[if_icmpgt] = 2; // branchbyte1, branchbyte2 value1,
            // value2 ? if value1 is greater
            // than value2, branch to
            // instruction at branchoffset
            // (signed short constructed from
            // unsigned bytes branchbyte1 << 8 +
            // branchbyte2)
            OFFSETS[if_icmple] = 2; // branchbyte1, branchbyte2 value1,
            // value2 ? if value1 is less than
            // or equal to value2, branch to
            // instruction at branchoffset
            // (signed short constructed from
            // unsigned bytes branchbyte1 << 8 +
            // branchbyte2)
            OFFSETS[if_acmpeq] = 2; // branchbyte1, branchbyte2 value1,
            // value2 ? if references are equal,
            // branch to instruction at
            // branchoffset (signed short
            // constructed from unsigned bytes
            // branchbyte1 << 8 + branchbyte2)
            OFFSETS[if_acmpne] = 2; // branchbyte1, branchbyte2 value1,
            // value2 ? if references are not
            // equal, branch to instruction at
            // branchoffset (signed short
            // constructed from unsigned bytes
            // branchbyte1 << 8 + branchbyte2)
            OFFSETS[goto_] = 2; // branchbyte1, branchbyte2 [no change]
            // goes to another instruction at
            // branchoffset (signed short
            // constructed from unsigned bytes
            // branchbyte1 << 8 + branchbyte2)
            OFFSETS[jsr] = 2; // branchbyte1, branchbyte2 ? address
            // jump to subroutine at branchoffset
            // (signed short constructed from
            // unsigned bytes branchbyte1 << 8 +
            // branchbyte2) and place the return
            // address on the stack
            OFFSETS[ret] = 1; // index [No change] continue execution
            // from address taken from a local
            // variable #index (the asymmetry with
            // jsr is intentional)
            OFFSETS[tableswitch] = -1; // [0-3 bytes padding],
            // defaultbyte1, defaultbyte2,
            // defaultbyte3, defaultbyte4,
            // lowbyte1, lowbyte2, lowbyte3,
            // lowbyte4, highbyte1,
            // highbyte2, highbyte3,
            // highbyte4, jump offsets...
            // index ? continue execution
            // from an address in the table
            // at offset index
            OFFSETS[lookupswitch] = -1; // <0-3 bytes padding>,
            // defaultbyte1, defaultbyte2,
            // defaultbyte3, defaultbyte4,
            // npairs1, npairs2, npairs3,
            // npairs4, match-offset
            // pairs... key ? a target
            // address is looked up from a
            // table using a key and
            // execution continues from the
            // instruction at that address
            OFFSETS[getstatic] = 2; // index1, index2 ? value gets a
            // static field value of a class,
            // where the field is identified by
            // field reference in the constant
            // pool index (index1 << 8 + index2)
            OFFSETS[putstatic] = 2; // indexbyte1, indexbyte2 value ?
            // set static field to value in a
            // class, where the field is
            // identified by a field reference
            // index in constant pool
            // (indexbyte1 << 8 + indexbyte2)
            OFFSETS[getfield] = 2; // index1, index2 objectref ? value
            // gets a field value of an object
            // objectref, where the field is
            // identified by field reference in
            // the constant pool index (index1
            // << 8 + index2)
            OFFSETS[putfield] = 2; // indexbyte1, indexbyte2 objectref,
            // value ? set field to value in an
            // object objectref, where the field
            // is identified by a field
            // reference index in constant pool
            // (indexbyte1 << 8 + indexbyte2)
            OFFSETS[invokevirtual] = 2; // indexbyte1, indexbyte2
            // objectref, [arg1, arg2, ...]
            // ? invoke virtual method on
            // object objectref, where the
            // method is identified by
            // method reference index in
            // constant pool (indexbyte1 <<
            // 8 + indexbyte2)
            OFFSETS[invokespecial] = 2; // indexbyte1, indexbyte2
            // objectref, [arg1, arg2, ...]
            // ? invoke instance method on
            // object objectref, where the
            // method is identified by
            // method reference index in
            // constant pool (indexbyte1 <<
            // 8 + indexbyte2)
            OFFSETS[invokestatic] = 2; // indexbyte1, indexbyte2 [arg1,
            // arg2, ...] ? invoke a static
            // method, where the method is
            // identified by method
            // reference index in constant
            // pool (indexbyte1 << 8 +
            // indexbyte2)
            OFFSETS[invokeinterface] = 2; // indexbyte1, indexbyte2,
            // count, 0 objectref,
            // [arg1, arg2, ...] ?
            // invokes an interface
            // method on object
            // objectref, where the
            // interface method is
            // identified by method
            // reference index in
            // constant pool (indexbyte1
            // << 8 + indexbyte2)

            OFFSETS[invokedynamic] = 4; // 4: indexbyte1, indexbyte2, 0, 0

            OFFSETS[new_] = 2; // indexbyte1, indexbyte2 ? objectref
            // creates new object of type identified
            // by class reference in constant pool
            // index (indexbyte1 << 8 + indexbyte2)
            OFFSETS[newarray] = 1; // atype count ? arrayref creates
            // new array with count elements of
            // primitive type identified by
            // atype
            OFFSETS[anewarray] = 2; // indexbyte1, indexbyte2 count ?
            // arrayref creates a new array of
            // references of length count and
            // component type identified by the
            // class reference index (indexbyte1
            // << 8 + indexbyte2) in the
            // constant pool
            OFFSETS[checkcast] = 2; // indexbyte1, indexbyte2 objectref
            // ? objectref checks whether an
            // objectref is of a certain type,
            // the class reference of which is
            // in the constant pool at index
            // (indexbyte1 << 8 + indexbyte2)
            OFFSETS[instanceof_] = 2; // indexbyte1, indexbyte2 objectref
            // ? result determines if an object
            // objectref is of a given type,
            // identified by class reference
            // index in constant pool
            // (indexbyte1 << 8 + indexbyte2)
            OFFSETS[wide] = 3; // opcode, indexbyte1, indexbyte2
            OFFSETS[multianewarray] = 3; // indexbyte1, indexbyte2,
            // dimensions count1,
            // [count2,...] ? arrayref
            // create a new array of
            // dimensions dimensions with
            // elements of type identified
            // by class reference in
            // constant pool index
            // (indexbyte1 << 8 +
            // indexbyte2); the sizes of
            // each dimension is identified
            // by count1, [count2, etc]
            OFFSETS[ifnull] = 2; // branchbyte1, branchbyte2 value ? if
            // value is null, branch to instruction
            // at branchoffset (signed short
            // constructed from unsigned bytes
            // branchbyte1 << 8 + branchbyte2)
            OFFSETS[ifnonnull] = 2; // branchbyte1, branchbyte2 value ?
            // if value is not null, branch to
            // instruction at branchoffset
            // (signed short constructed from
            // unsigned bytes branchbyte1 << 8 +
            // branchbyte2)
            OFFSETS[goto_w] = 4; // branchbyte1, branchbyte2,
            // branchbyte3, branchbyte4 [no change]
            // goes to another instruction at
            // branchoffset (signed int constructed
            // from unsigned bytes branchbyte1 << 24
            // + branchbyte2 << 16 + branchbyte3 <<
            // 8 + branchbyte4)
            OFFSETS[jsr_w] = 4; // branchbyte1, branchbyte2,
            // branchbyte3, branchbyte4 ? address
            // jump to subroutine at branchoffset
            // (signed int constructed from unsigned
            // bytes branchbyte1 << 24 + branchbyte2
            // << 16 + branchbyte3 << 8 +
            // branchbyte4) and place the return
            // address on the stack
        }
    }
}
