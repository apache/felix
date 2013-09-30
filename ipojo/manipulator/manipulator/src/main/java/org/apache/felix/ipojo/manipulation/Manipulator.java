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

import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * iPOJO Byte code Manipulator.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 *
 */
public class Manipulator {
    /**
     * Store the visited fields : [name of the field, type of the field].
     */
    private Map<String, String> m_fields;

    /**
     * Store the interface implemented by the class.
     */
    private List<String> m_interfaces;

    /**
     * Store the methods list.
     */
    private List<MethodDescriptor> m_methods;

    /**
     * Pojo super class.
     */
    private String m_superClass;

    /**
     * List of owned inner classes and internal methods.
     */
    private Map<String, List<MethodDescriptor>> m_inners;

    /**
     * Java byte code version.
     */
    private int m_version;

    /**
     * Was the class already manipulated.
     */
    private boolean m_alreadyManipulated;

    /**
     * The manipulated class name.
     */
    private String m_className;

    /**
     * Manipulate the given byte array.
     * @param origin : original class.
     * @return the manipulated class, if the class is already manipulated, the original class.
     * @throws IOException : if an error occurs during the manipulation.
     */
    public byte[] manipulate(byte[] origin) throws IOException {
        InputStream is1 = new ByteArrayInputStream(origin);

        // First check if the class is already manipulated :
        ClassReader ckReader = new ClassReader(is1);
        ClassChecker ck = new ClassChecker();
        ckReader.accept(ck, ClassReader.SKIP_FRAMES);
        is1.close();

        m_fields = ck.getFields(); // Get visited fields (contains only POJO fields)
        m_className = ck.getClassName();

        // Get interfaces and super class.
        m_interfaces = ck.getInterfaces();
        m_superClass = ck.getSuperClass();

        // Get the methods list
        m_methods = ck.getMethods();

        m_inners = ck.getInnerClassesAndMethods();

        m_version = ck.getClassVersion();

        ClassWriter finalWriter = null;

        m_alreadyManipulated = ck.isAlreadyManipulated();

        if (!m_alreadyManipulated) {
            // Manipulation ->
            // Add the _setComponentManager method
            // Instrument all fields
            InputStream is2 = new ByteArrayInputStream(origin);
            ClassReader cr0 = new ClassReader(is2);
            ClassWriter cw0 = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            //CheckClassAdapter ch = new CheckClassAdapter(cw0);
            MethodCreator process = new MethodCreator(cw0, m_fields, m_methods);
            if (ck.getClassVersion() >= Opcodes.V1_6) {
                cr0.accept(process, ClassReader.EXPAND_FRAMES);
            } else {
                cr0.accept(process, 0);
            }
            is2.close();
            finalWriter = cw0;
        }
        // The file is in the bundle
        if (m_alreadyManipulated) {
            return origin;
        } else {
            return finalWriter.toByteArray();
        }
    }

    /**
     * Checks whether the class was already manipulated.
     * @return {@code true} if the class was already manipulated, {@code false} otherwise
     */
    public boolean isAlreadyManipulated() {
        return m_alreadyManipulated;
    }

    public static String toQualifiedName(String clazz) {
        return clazz.replace("/", ".");
    }

    /**
     * Compute component type manipulation metadata.
     * @return the manipulation metadata of the class.
     */
    public Element getManipulationMetadata() {
        Element elem = new Element("Manipulation", "");

        elem.addAttribute(new Attribute("className", toQualifiedName(m_className)));

        if (m_superClass != null) {
            elem.addAttribute(new Attribute("super", m_superClass));
        }

        for (String m_interface : m_interfaces) {
            Element itf = new Element("Interface", "");
            Attribute att = new Attribute("name", m_interface);
            itf.addAttribute(att);
            elem.addElement(itf);
        }

        for (Map.Entry<String, String> f : m_fields.entrySet()) {
            Element field = new Element("Field", "");
            Attribute attName = new Attribute("name", f.getKey());
            Attribute attType = new Attribute("type", f.getValue());
            field.addAttribute(attName);
            field.addAttribute(attType);
            elem.addElement(field);
        }

        for (MethodDescriptor method : m_methods) {
            elem.addElement(method.getElement());
        }

        for (Map.Entry<String, List<MethodDescriptor>> inner : m_inners.entrySet()) {
            Element element = new Element("Inner", "");
            Attribute name = new Attribute("name", extractInnerClassName(toQualifiedName(inner.getKey())));
            element.addAttribute(name);

            for (MethodDescriptor method : inner.getValue()) {
                element.addElement(method.getElement());
            }
            elem.addElement(element);
        }

        return elem;
    }

    /**
     * Extracts the inner class simple name from the qualified name. It extracts the part after the `$` character.
     * @param clazz the qualified class name
     * @return the simple inner class name
     */
    public static String extractInnerClassName(String clazz) {
        if (!clazz.contains("$")) {
            return clazz;
        } else {
            return clazz.substring(clazz.indexOf("$") +1);
        }
    }

    public Map<String, String> getFields() {
        return m_fields;
    }

    public Collection<String> getInnerClasses() {
        return new ArrayList<String>(m_inners.keySet());
    }

    public int getClassVersion() {
        return m_version;
    }

    /**
     * Adds a method to an inner class.
     * @param name the inner class name
     * @param md the method descriptor to add
     */
    public void addMethodToInnerClass(String name, MethodDescriptor md) {
        List<MethodDescriptor> list = m_inners.get(name);
        if (list == null) {
            list = new ArrayList<MethodDescriptor>();
            m_inners.put(name, list);
        }
        list.add(md);
    }
}
