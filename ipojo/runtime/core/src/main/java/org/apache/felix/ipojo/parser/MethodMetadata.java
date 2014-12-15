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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.Type;

/**
 * A Method Metadata represents a method from the implementation class.
 * This class allows getting information about a method : name, arguments, return type...
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MethodMetadata {

    /**
     * Empty Constructor Method Id.
     */
    public static final String EMPTY_CONSTRUCTOR_ID = "$init";

    /**
     * Bundle Context Constructor Method Id.
     */
    public static final String BC_CONSTRUCTOR_ID = "$init$org_osgi_framework_BundleContext";

    /**
     * Constructor Prefix.
     */
    public static final String CONSTRUCTOR_PREFIX = "$init";


    /**
     * The name of the method.
     */
    private final String m_name;

    /**
     * The argument type array.
     */
    private final String[] m_arguments;

    /**
     * The returned type.
     */
    private final String m_return;

    /**
     * The argument names if there were contained in the manifest.
     * @since 1.11.0
     */
    private final String[] m_names;

    /**
     * Creates a Method Metadata.
     * @param metadata the method manipulation element.
     */
    public MethodMetadata(Element metadata) {
        m_name = metadata.getAttribute("name");
        String arg = metadata.getAttribute("arguments");
        String names = metadata.getAttribute("names");
        String result = metadata.getAttribute("return");
        if (arg != null) {
            m_arguments = ParseUtils.parseArrays(arg);
        } else {
            m_arguments = new String[0];
        }
        if (names != null) {
            m_names = ParseUtils.parseArrays(names);
        } else {
            m_names = new String[0];
        }

        if (result != null) {
            m_return = result;
        } else {
            m_return = "void";
        }
    }

    public String getMethodName() {
        return m_name;
    }

    public String[] getMethodArguments() {
        return m_arguments;
    }

    public String[] getMethodArgumentNames() {
        return m_names;
    }

    /**
     * Gets the method arguments.
     * The keys are the argument names, while the values are the argument type.
     * @return the map of argument
     * @since 1.10.2
     */
    public Map<String, String> getArguments() {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        for (int i = 0; i < m_names.length; i++) {
            map.put(m_names[i], m_arguments[i]);
        }
        return map;
    }

    public String getMethodReturn() {
        return m_return;
    }

    /**
     * Gets the method unique identifier. For internal usage only.
     * A method identifier is a unique string that can be a java field
     * that identify the method.
     * @return the method identifier.
     */
    public String getMethodIdentifier() {
        StringBuffer identifier = new StringBuffer(m_name);
        for (int i = 0; i < m_arguments.length; i++) {
            String arg = m_arguments[i];
            if (arg.endsWith("[]")) {
                // We have to replace all []
                String acc = "";
                while (arg.endsWith("[]")) {
                    arg = arg.substring(0, arg.length() - 2);
                    acc += "__";
                }
                identifier.append("$" + arg.replace('.', '_') + acc);
            } else {
                identifier.append("$" + arg.replace('.', '_'));
            }
        }
        return identifier.toString();
    }

    /**
     * Computes the method id for the given Method object.
     * @param method the Method object.
     * @return the method id.
     */
    public static String computeMethodId(Method method) {
        StringBuffer identifier = new StringBuffer(method.getName());
        Class[] args = method.getParameterTypes();
        for (int i = 0; i < args.length; i++) {
            identifier.append('$'); // Argument separator.
            if (args[i].isArray()) {
                String acc = "__";
                if (args[i].getComponentType().isPrimitive()) {
                    // Primitive array
                    identifier.append(FieldMetadata.getPrimitiveTypeByClass(args[i].getComponentType()));
                } else if (args[i].getComponentType().isArray()) {
                    // Multi-directional array.
                    Class current = args[i].getComponentType();
                    while (current.isArray()) {
                        acc += "__";
                        current = current.getComponentType();
                    }
                    if (current.isPrimitive()) {
                        acc = FieldMetadata.getPrimitiveTypeByClass(current) + acc;
                    } else {
                        acc = current.getName().replace('.', '_') + acc;
                    }
                } else {
                    // Object array
                    identifier.append(args[i].getComponentType().getName().replace('.', '_')); // Replace '.' by '_'
                }
                identifier.append(acc); // Add __ (array)
            } else {
                if (args[i].isPrimitive()) {
                    // Primitive type
                    identifier.append(FieldMetadata.getPrimitiveTypeByClass(args[i]));
                } else {
                    // Object type
                    identifier.append(args[i].getName().replace('.', '_')); // Replace '.' by '_'
                }
            }
        }
        return identifier.toString();
    }

    /**
     * Computes the method id for the given Constructor object.
     * @param method the Method object.
     * @return the method id.
     */
    public static String computeMethodId(Constructor method) {
        StringBuffer identifier = new StringBuffer("$init");
        Class[] args = method.getParameterTypes();
        for (int i = 0; i < args.length; i++) {
            // If the first argument is the InstanceManager skip it
            if (i == 0  && InstanceManager.class.equals(args[i])) {
                // Skip it.
                continue;
            }

            identifier.append('$'); // Argument separator.
            if (args[i].isArray()) {
                String acc = "__";
                if (args[i].getComponentType().isPrimitive()) {
                    // Primitive array
                    identifier.append(FieldMetadata.getPrimitiveTypeByClass(args[i].getComponentType()));
                } else if (args[i].getComponentType().isArray()) {
                    // Multi-directional array.
                    Class current = args[i].getComponentType();
                    while (current.isArray()) {
                        acc += "__";
                        current = current.getComponentType();
                    }
                } else {
                    // Object array
                    identifier.append(args[i].getComponentType().getName().replace('.', '_')); // Replace '.' by '_'
                }
                identifier.append(acc); // Add __ (array)
            } else {
                if (args[i].isPrimitive()) {
                    // Primitive type
                    identifier.append(FieldMetadata.getPrimitiveTypeByClass(args[i]));
                } else {
                    // Object type
                    identifier.append(args[i].getName().replace('.', '_')); // Replace '.' by '_'
                }
            }
        }
        return identifier.toString();
    }
}
