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

package org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.manipulation.ClassManipulator;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Names {

    /**
     * Excluded types when searching for a specification interface in method's arguments.
     */
    private static List<Type> EXCLUSIONS = new ArrayList<Type>();

    static {
        EXCLUSIONS.add(Type.getType(Map.class));
        EXCLUSIONS.add(Type.getType(Dictionary.class));
        EXCLUSIONS.add(Type.getType("Lorg/osgi/framework/ServiceReference;"));
    }

    /**
     * Computes the real method name. This method is useful when the annotation is collected on an manipulated method
     * (prefixed by <code>__M_</code>). This method just removes the prefix if found.
     * @param name the collected method name
     * @return the effective method name, can be the collected method name if the method name does not start with
     * the prefix.
     */
    public static String computeEffectiveMethodName(String name) {
        if (name != null && name.startsWith(ClassManipulator.PREFIX)) {
            return name.substring(ClassManipulator.PREFIX.length());
        } else {
            return name;
        }
    }

    /**
     * Extract an identifier from the given method name.
     * It removes some pre-defined prefixes ({@literal bind}, {@literal unbind},
     * {@literal set}, {@literal unset}, {@literal modified}).
     *
     * @param method method's name
     * @return the method's identifier
     */
    public static String getMethodIdentifier(final MethodNode method) {

        String effectiveName = computeEffectiveMethodName(method.name);

        if (effectiveName.startsWith("bind")) {
            return effectiveName.substring("bind".length());
        }

        if (effectiveName.startsWith("set")) {
            return effectiveName.substring("set".length());
        }

        if (effectiveName.startsWith("unbind")) {
            return effectiveName.substring("unbind".length());
        }

        if (effectiveName.startsWith("unset")) {
            return effectiveName.substring("unset".length());
        }

        if (effectiveName.startsWith("modified")) {
            return effectiveName.substring("modified".length());
        }

        if (effectiveName.startsWith("add")) {
            return effectiveName.substring("add".length());
        }

        if (effectiveName.startsWith("remove")) {
            return effectiveName.substring("remove".length());
        }

        // Try to discover the specification's type from method's parameters' type
        Type[] arguments = Type.getArgumentTypes(method.desc);
        return findSpecification(Arrays.asList(arguments));

    }

    /**
     * Find the first type that was not excluded and consider it as the specification
     * @param types method parameter's type
     * @return the first non-excluded type or {@literal null} if no specification can be found
     */
    private static String findSpecification(final List<Type> types) {

        // Find first non-excluded specification
        // May return null if no specification is provided (likely a user error)
        for (Type type : types) {
            if (!EXCLUSIONS.contains(type)) {
                return type.getClassName();
            }
        }
        return null;
    }

    /**
     * Check if the given annotation descriptor is an iPOJO custom annotation.
     * A valid iPOJO custom annotation must contains 'ipojo' or 'handler' in its qualified name.
     * @param desc annotation descriptor
     * @return {@literal true} if the given descriptor is an iPOJO custom annotation
     */
    public static boolean isCustomAnnotation(final String desc) {
        String lowerCase = desc.toLowerCase();
        return lowerCase.contains("ipojo") || lowerCase.contains("handler");
    }
}
