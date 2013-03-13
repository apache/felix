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

package org.apache.felix.ipojo.manipulator;

import org.apache.felix.ipojo.metadata.Element;

/**
 * Visit manipulation results.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ManipulationResultVisitor {

    /**
     * Called once per visitor with the class' structure discovered during manipulation.
     * @param structure Component's structure (discovered during manipulation, not the data from metadata.xml)
     */
    void visitClassStructure(Element structure);

    /**
     * Accept a manipulated resource (main component class or inner classes).
     * @param type type name
     * @param resource manipulated bytecode
     */
    void visitManipulatedResource(String type, byte[] resource);

    /**
     * Called when all resources from this manipulation result have been processed.
     */
    void visitEnd();
}
