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

import java.util.*;

import org.apache.felix.ipojo.manipulation.ClassChecker.AnnotationDescriptor;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LocalVariableNode;

/**
 * Method Descriptor describe a method.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MethodDescriptor {
    /**
     * Method name.
     */
    private final String m_name;

    /**
     * Returned type.
     */
    private final String m_returnType;

    /**
     * Argument types.
     */
    private final String[] m_arguments;

    /**
     * The descriptor of the method.
     */
    private final String m_desc;


    /**
     * The list of {@link AnnotationDescriptor} attached to this
     * method.
     */
    private List<AnnotationDescriptor> m_annotations;

    /**
     * The association argument (number) - {@link AnnotationDescriptor}.
     */
    private Map<Integer, List<AnnotationDescriptor>> m_parameterAnnotations = new HashMap<Integer, List<AnnotationDescriptor>>();

    /**
     * The arguments variables.
     */
    private List<LocalVariableNode> m_argLocalVariables;

    /**
     * The stack size to keep of the arguments.
     */
    private final int m_argsVarLength;

    /**
     * Flag indicating is the described method is static.
     */
    private final boolean m_isStatic;

    /**
     * The local variables by index.
     * This map is used to detect the argument names.
     */
    private LinkedHashMap<Integer, LocalVariableNode> m_locals = new LinkedHashMap<Integer, LocalVariableNode>();

    /**
     * Constructor.
     * @param name : name of the method.
     * @param desc : descriptor of the method.
     * @param isStatic : is the method static
     */
    public MethodDescriptor(String name, String desc, boolean isStatic) {
        m_name = name;
        m_desc = desc;
        m_isStatic = isStatic;
        Type ret = Type.getReturnType(desc);
        Type[] args = Type.getArgumentTypes(desc);

        m_returnType = getType(ret);
        m_arguments = new String[args.length];
        int argsVarLength = args.length;
        if (!m_isStatic) {
            argsVarLength++;
        }
        for (int i = 0; i < args.length; i++) {
            String type = getType(args[i]);
            m_arguments[i] = type;
            if ("long".equals(type) || "double".equals(type)) {
                argsVarLength++;
            }
        }
        m_argsVarLength = argsVarLength;
    }

    /**
     * Add an annotation to the current method.
     * @param ann annotation to add
     */
    public void addAnnotation(AnnotationDescriptor ann) {
        if (m_annotations == null) {
            m_annotations = new ArrayList<AnnotationDescriptor>();
        }
        m_annotations.add(ann);
    }

    /**
     * Add an annotation to the current method.
     * @param ann annotation to add
     */
    public void addParameterAnnotation(int id, AnnotationDescriptor ann) {
        List<AnnotationDescriptor> list = m_parameterAnnotations.get(new Integer(id));
        if (list == null) {
            list = new ArrayList<AnnotationDescriptor>();
            m_parameterAnnotations.put(new Integer(id), list);
        }
        list.add(ann);
    }

    public List<AnnotationDescriptor> getAnnotations() {
        return m_annotations;
    }

    public Map<Integer, List<AnnotationDescriptor>> getParameterAnnotations() {
        return m_parameterAnnotations;
    }

    public String getDescriptor() {
        return m_desc;
    }

    /**
     * Compute method manipulation metadata.
     * @return the element containing metadata about this method.
     */
    public Element getElement() {
        Element method = new Element("method", "");
        method.addAttribute(new Attribute("name", m_name));

        // Add return
        if (!m_returnType.equals("void")) {
            method.addAttribute(new Attribute("return", m_returnType));
        }

        // Add arguments
        if (m_arguments.length > 0) {
            StringBuilder args = new StringBuilder("{");
            StringBuilder names = new StringBuilder("{");
            args.append(m_arguments[0]);
            if (m_locals.containsKey(1)) {
                names.append(m_locals.get(1).name); // index +1 as the 0 is this
            }
            for (int i = 1; i < m_arguments.length; i++) {
                args.append(",").append(m_arguments[i]);
                if (m_locals.containsKey(i +1)) {
                    names.append(",").append(m_locals.get(i +1).name);
                }
            }
            args.append("}");
            names.append("}");

            method.addAttribute(new Attribute("arguments", args.toString()));
            method.addAttribute(new Attribute("names", names.toString()));
        }

        return method;
    }

    /**
     * Get the iPOJO internal type for the given type.
     * @param type : type.
     * @return the iPOJO internal type.
     */
    private String getType(Type type) {
        switch (type.getSort()) {
            case Type.ARRAY:
                // Append brackets.
                String brackets = "";
                for (int i = 0; i < type.getDimensions(); i++) {
                    brackets += "[]";
                }
                Type elemType = type.getElementType();
                return getType(elemType) + brackets;
            case Type.BOOLEAN:
                return "boolean";
            case Type.BYTE:
                return "byte";
            case Type.CHAR:
                return "char";
            case Type.DOUBLE:
                return "double";
            case Type.FLOAT:
                return "float";
            case Type.INT:
                return "int";
            case Type.LONG:
                return "long";
            case Type.OBJECT:
                return type.getClassName();
            case Type.SHORT:
                return "short";
            case Type.VOID:
                return "void";
            default:
                return "unknown";
        }
    }

    public String getName() {
        return m_name;
    }

    public void addLocalVariable(String name, String desc, String signature, int index) {
        m_locals.put(index, new LocalVariableNode(name, desc, signature, null, null, index));
        if (index >= m_argsVarLength) {
            // keep only argument-related local variables definitions (others relate to code which isn't in this method) 
            return;
        }
        if (m_argLocalVariables == null) {
            m_argLocalVariables = new ArrayList<LocalVariableNode>();
        }
        m_argLocalVariables.add(new LocalVariableNode(name, desc, signature, null, null, index));
    }

    public void end() {
        if (m_argLocalVariables != null && m_argLocalVariables.size() > 1) {
            // sort them by index, even if from experience, argument-related variables (and only those) are already sorted
            Collections.sort(m_argLocalVariables, new Comparator<LocalVariableNode>(){
                public int compare(LocalVariableNode o1, LocalVariableNode o2) {
                    return o1.index - o2.index;
                }
            });
        }
    }

    public List<LocalVariableNode> getArgumentLocalVariables() {
        return m_argLocalVariables;
    }

    public LinkedHashMap<Integer, LocalVariableNode> getLocals() {
        return m_locals;
    }
}
