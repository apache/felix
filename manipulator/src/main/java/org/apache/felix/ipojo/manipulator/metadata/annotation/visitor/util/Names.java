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

import org.apache.felix.ipojo.manipulation.MethodCreator;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Names {

    /**
     * Computes the real method name. This method is useful when the annotation is collected on an manipulated method
     * (prefixed by <code>__M_</code>). This method just removes the prefix if found.
     * @param name the collected method name
     * @return the effective method name, can be the collected method name if the method name does not start with
     * the prefix.
     */
    public static String computeEffectiveMethodName(String name) {
        if (name != null && name.startsWith(MethodCreator.PREFIX)) {
            return name.substring(MethodCreator.PREFIX.length());
        } else {
            return name;
        }
    }

    /**
     * Extract an identifier from the given method name.
     * It removes some pre-defined prefixes ({@literal bind}, {@literal unbind},
     * {@literal set}, {@literal unset}, {@literal modified}).
     * @param method method's name
     * @return the method's identifier
     */
    public static String getMethodIdentifier(final String method) {

        String effectiveName = computeEffectiveMethodName(method);

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
