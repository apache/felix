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

package org.apache.felix.ipojo.manipulator.metadata.annotation.model.parser;

import org.apache.felix.ipojo.manipulator.metadata.annotation.model.AnnotationType;
import org.objectweb.asm.ClassReader;

/**
 * User: guillaume
 * Date: 01/07/13
 * Time: 15:49
 */
public class AnnotationParser {
    public AnnotationType read(byte[] resource) {
        ClassReader reader = new ClassReader(resource);
        AnnotationTypeVisitor visitor = new AnnotationTypeVisitor();
        reader.accept(visitor, ClassReader.SKIP_CODE);
        return visitor.getAnnotationType();
    }

}
