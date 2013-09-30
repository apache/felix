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
package org.apache.felix.ipojo.parser;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.metadata.Element;

import java.util.*;

/**
 * Manipulation Metadata allows getting information about the implementation class
 * without using reflection such as implemented interfaces, super class,
 * methods and fields.
 * This method allows getting object to register {@link org.apache.felix.ipojo.FieldInterceptor} and
 * {@link org.apache.felix.ipojo.MethodInterceptor}.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class PojoMetadata {

    /**
     * The list of implemented interfaces.
     */
    private String[] m_interfaces = new String[0];

    /**
     * The list of fields.
     */
    private FieldMetadata[] m_fields = new FieldMetadata[0];

    /**
     * The list of methods.
     */
    private List<MethodMetadata> m_methods = new ArrayList<MethodMetadata>();

    /**
     * The Super class (if <code>null</code> for {@link Object}).
     */
    private String m_super;

    /**
     * The manipulated class name.
     */
    private String m_className;

    /**
     * The inner classes and their methods.
     */
    private Map<String, List<MethodMetadata>> m_innerClasses = new HashMap<String, List<MethodMetadata>>();


    /**
     * Creates Pojo metadata.
     * Manipulation Metadata object are created from component type metadata by
     * parsing manipulation metadata.
     * @param metadata the component type metadata
     * @throws ConfigurationException if the manipulation metadata cannot be found
     */
    public PojoMetadata(Element metadata) throws ConfigurationException {
        Element[] elems = metadata.getElements("manipulation", "");
        if (elems == null) {
            throw new ConfigurationException("The component " + metadata.getAttribute("classname") + " has no manipulation metadata");
        }
        Element manip = elems[0];
        m_className = manip.getAttribute("classname");
        m_super = manip.getAttribute("super");

        Element[] fields = manip.getElements("field");
        if (fields != null) {
            for (Element field : fields) {
                addField(new FieldMetadata(field));
            }
        }

        Element[] methods = manip.getElements("method");
        if (methods != null) {
            for (Element method : methods) {
                m_methods.add(new MethodMetadata(method));
            }
        }

        Element[] itfs = manip.getElements("interface");
        if (itfs != null) {
            for (Element itf : itfs) {
                addInterface(itf.getAttribute("name"));
            }
        }

        Element[] inners = manip.getElements("inner");
        if (inners != null) {
            for (Element inner : inners) {
                String name = inner.getAttribute("name");
                List<MethodMetadata> list = m_innerClasses.get(name);
                if (list == null) {
                    list = new ArrayList<MethodMetadata>();
                    m_innerClasses.put(name, list);
                }
                methods = inner.getElements("method");
                if (methods != null) {
                    for (Element m : methods) {
                        list.add(new MethodMetadata(m));
                    }
                }
            }
        }
    }

    public MethodMetadata[] getMethods() { return m_methods.toArray(new MethodMetadata[m_methods.size()]); }

    public FieldMetadata[] getFields() { return m_fields; }

    public String[] getInterfaces() { return m_interfaces; }

    public String getClassName() { return m_className; }

    /**
     * Gets the inner classes from the manipulated class
     * @return the list of the inner class names.
     */
    public String[] getInnerClasses() {
        Set<String> classes = m_innerClasses.keySet();
        return classes.toArray(new String[classes.size()]);
    }

    /**
     * Gets the methods from the given inner class.
     * @param inner the inner class name
     * @return the list of method, empty if none.
     */
    public MethodMetadata[] getMethodsFromInnerClass(String inner) {
        List<MethodMetadata> methods = m_innerClasses.get(inner);
        if (inner != null) {
            return methods.toArray(new MethodMetadata[methods.size()]);
        } else {
            return new MethodMetadata[0];
        }
    }

    /**
     * Gets the field metadata for the given name.
     * @param name : the name of the field
     * @return the corresponding field metadata or <code>null</code> if not found
     */
    public FieldMetadata getField(String name) {
        for (int i = 0; i < m_fields.length; i++) {
            if (m_fields[i].getFieldName().equalsIgnoreCase(name)) { return m_fields[i]; }
        }
        return null;
    }

    /**
     * Gets the field metadata for the given name and type.
     * @param name : the name of the field
     * @param type : the type of the field
     * @return the corresponding field metadata or <code>null</code> if not found
     */
    public FieldMetadata getField(String name, String type) {
        for (int i = 0; i < m_fields.length; i++) {
            if (m_fields[i].getFieldName().equalsIgnoreCase(name) && m_fields[i].getFieldType().equalsIgnoreCase(type)) { return m_fields[i]; }
        }
        return null;
    }

    /**
     * Checks if the given interface name is implemented.
     * This methods checks on interface directly implemented
     * by the implementation class.
     * @param itf the interface to check.
     * @return <code>true</code> if the implementation class implements
     * the given interface.
     */
    public boolean isInterfaceImplemented(String itf) {
        for (int i = 0; i < m_interfaces.length; i++) {
            if (m_interfaces[i].equals(itf)) { return true; }
        }
        return false;
    }

    /**
     * Gets the MethodMetadata corresponding to the method
     * (contained in the implementation class) with
     * the given name.
     * If several methods match, the first one is returned.
     * @param name the name of the method to find.
     * @return the method metadata object or <code>null</code> if not found
     */
    public MethodMetadata getMethod(String name) {
        for (MethodMetadata metadata : m_methods) {
            if (metadata.getMethodName().equalsIgnoreCase(name)) { return metadata; }
        }
        return null;
    }

    /**
     * Gets the MethodMetadata list corresponding to the method
     * (contained in the implementation class) to given name.
     * All methods contained in the implementation class matching
     * with the name are in the returned list.
     * @param name the name of the method to look for.
     * @return the Method Metadata array or an empty array if not found
     */
    public MethodMetadata[] getMethods(String name) {
        List<MethodMetadata> list = new ArrayList<MethodMetadata>();
        for (MethodMetadata metadata : m_methods) {
            if (metadata.getMethodName().equalsIgnoreCase(name)) {
                list.add(metadata);
            }
        }
        return list.toArray(new MethodMetadata[list.size()]);
    }

    /**
     * Gets the MethodMetadata list corresponding to the constructors
     * (contained in the implementation class).
     * @return the Method Metadata array or an empty array if not found
     */
    public MethodMetadata[] getConstructors() {
        return getMethods("$init");
    }

    /**
     * Gets the MethodMetadata corresponding to the method
     * (contained in the implementation class) to given name
     * and argument types.
     * @param name the name of the method to look for.
     * @param types the array of the argument types of the method
     * @return the Method Metadata or <code>null</code> if not found
     */
    public MethodMetadata getMethod(String name, String[] types) {
        for (MethodMetadata metadata : m_methods) {
            if (metadata.getMethodName().equalsIgnoreCase(name) && metadata.getMethodArguments().length == types.length) {
                int argIndex = 0;
                for (; argIndex < types.length; argIndex++) {
                    if (! types[argIndex].equals(metadata.getMethodArguments()[argIndex])) {
                        break;
                    }
                }
                if (argIndex == types.length) { return metadata; } // No mismatch detected.
            }
        }
        return null;
    }

    /**
     * Gets the constructor corresponding to the given argument types.
     * @param types the argument types
     * @return the matching constructor or <code>null</code> if not found.
     */
    public MethodMetadata getConstructor(String[] types) {
    	return getMethod("$init", types); // Constructors are named $init in the manipulation metadata
    }

     /**
     * Adds a field to the list.
     * This method is used during the creation of the {@link PojoMetadata}
     * object.
     * @param field the Field Metadata to add.
     */
    private void addField(FieldMetadata field) {
        if (m_fields.length > 0) {
            FieldMetadata[] newInstances = new FieldMetadata[m_fields.length + 1];
            System.arraycopy(m_fields, 0, newInstances, 0, m_fields.length);
            newInstances[m_fields.length] = field;
            m_fields = newInstances;
        } else {
            m_fields = new FieldMetadata[] { field };
        }
    }

    /**
     * Adds the interface to the list.
     * This method is used during the creation of the {@link PojoMetadata}
     * object.
     * @param itf the interface name to add.
     */
    private void addInterface(String itf) {
        if (m_interfaces.length > 0) {
            String[] newInstances = new String[m_interfaces.length + 1];
            System.arraycopy(m_interfaces, 0, newInstances, 0, m_interfaces.length);
            newInstances[m_interfaces.length] = itf;
            m_interfaces = newInstances;
        } else {
            m_interfaces = new String[] { itf };
        }
    }

    public String getSuperClass() {
        return m_super;
    }

}
