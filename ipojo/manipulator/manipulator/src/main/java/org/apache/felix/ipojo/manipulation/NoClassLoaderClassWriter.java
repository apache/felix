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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

/**
 * An extension of {@link org.objectweb.asm.ClassWriter} that doe snot trigger class loading to compute the common super
 * class. It generally returns Object.
 */
public class NoClassLoaderClassWriter extends ClassWriter {

    private static final String OBJECT_INTERNAL_NAME = "java/lang/Object";

    public NoClassLoaderClassWriter(ClassReader reader, int flags) {
        super(reader, flags);
    }

    public NoClassLoaderClassWriter(int flags) {
        super(flags);
    }

    /**
     * Implements the common super class lookup to be a bit more permissive. First we check is type1 == type2,
     * because in this case, the lookup is done. Then, if one of the class is Object,
     * returns object. If both checks failed, it returns Object.
     * @param type1 the first class
     * @param type2 the second class
     * @return the common super class
     */
    @Override
    protected final String getCommonSuperClass(String type1, String type2) {
        //If the two are equal then return either
        if (type1.equals(type2)) {
            return type1;
        }

        //If either is Object, then Object must be the answer
        if (type1.equals(OBJECT_INTERNAL_NAME) || type2.equals(OBJECT_INTERNAL_NAME)) {
            return OBJECT_INTERNAL_NAME;
        }

        // We shunt the rest of the process.
        return OBJECT_INTERNAL_NAME;
    }
}

