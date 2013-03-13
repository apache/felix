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

package org.apache.felix.ipojo.manipulator.visitor.writer;

import org.apache.felix.ipojo.manipulator.ManipulationResultVisitor;
import org.apache.felix.ipojo.metadata.Element;

import java.util.HashMap;
import java.util.Map;

/**
 * Gather manipulated bytecode.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ManipulatedResultWriter implements ManipulationResultVisitor {

    private Map<String, byte[]> m_resources;

    private Element m_component;

    public ManipulatedResultWriter(Element component) {
        m_component = component;
        m_resources = new HashMap<String, byte[]>();
    }

    public void visitClassStructure(Element structure) {
        // Insert the manipulation structure in the component's metadata
        m_component.addElement(structure);
    }

    public void visitManipulatedResource(String type, byte[] resource) {
        m_resources.put(type, resource);
    }

    public void visitEnd() {
        // nothing to do
    }

    public Map<String, byte[]> getResources() {
        return m_resources;
    }
}
