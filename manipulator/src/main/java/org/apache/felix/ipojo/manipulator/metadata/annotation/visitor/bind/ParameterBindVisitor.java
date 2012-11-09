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

package org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.bind;

import org.apache.felix.ipojo.manipulator.metadata.annotation.ComponentWorkbench;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Parse @Bind & @Unbind annotations on method's parameters.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ParameterBindVisitor extends AbstractBindVisitor {

    /**
     * For annotation parameter,
     * the parameter index.
     */
    private int m_index;

    public ParameterBindVisitor(ComponentWorkbench workbench, Action action, int index) {
        super(workbench, action);
        m_index = index;
    }

    /**
     * End of the visit.
     * Create or append the requirement info to a created or already existing "requires" element.
     *
     * @see org.objectweb.asm.commons.EmptyVisitor#visitEnd()
     */
    public void visitEnd() {
        if (m_id == null) {
            m_id = Integer.toString(m_index);
        }

        Element requires = getRequiresElement();

        // Specific for parameters
        requires.addAttribute(new Attribute("constructor-parameter", Integer.toString(m_index)));

        workbench.getIds().put(m_id, requires);
        workbench.getElements().put(requires, null);
    }

}
