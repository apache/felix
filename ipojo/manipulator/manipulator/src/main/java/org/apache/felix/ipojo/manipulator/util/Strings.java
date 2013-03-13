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

package org.apache.felix.ipojo.manipulator.util;


/**
 * {@code Strings} is a utility class that helps to manipulate String.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Strings {

    /**
     * Utility class: no public constructor
     */
    private Strings() {}

    /**
     * Transform a FQN of a class (format {@literal org.objectweb.asm.Visitor}) into
     * a normalized resource name ({@literal org/objectweb/asm/Visitor.class}).
     * @param classname FQN of a class to be transformed
     * @return resource name
     */
    public static String asResourcePath(String classname) {
        return classname.replace('.', '/').concat(".class");
    }

    /**
     * Transform a normalized resource path ({@literal org/objectweb/asm/Visitor.class}) into
     * a fully qualified class name (format {@literal org.objectweb.asm.Visitor}).
     * @param path normalized resource path to be transformed
     * @return class name
     */
    public static String asClassName(String path) {
        String transformed = path.replace('/', '.');
        return transformed.substring(0, transformed.length() - ".class".length());
    }
}
