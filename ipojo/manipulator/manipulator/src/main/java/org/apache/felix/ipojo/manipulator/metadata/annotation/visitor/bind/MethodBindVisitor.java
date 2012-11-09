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
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.util.Names;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

import static org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.util.Names.computeEffectiveMethodName;

/**
 * Parse @Bind & @Unbind annotations on methods.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MethodBindVisitor extends AbstractBindVisitor {

    /**
     * Method name.
     */
    private String m_name;

    public MethodBindVisitor(ComponentWorkbench workbench, Action action, String method) {
        super(workbench, action);
        this.m_name = method;
    }

    /**
     * End of the visit.
     * Create or append the requirement info to a created or already existing "requires" element.
     *
     * @see org.objectweb.asm.commons.EmptyVisitor#visitEnd()
     */
    public void visitEnd() {
        if (m_id == null) {
            String identifier = Names.getMethodIdentifier(m_name);
            if (identifier != null) {
                m_id = identifier;
            } else {
                System.err.println("Cannot determine the id of the " + action.name() + " method : " + computeEffectiveMethodName(m_name));
                return;
            }
        }

        Element requires = getRequiresElement();

        Element callback = new Element("callback", "");
        callback.addAttribute(new Attribute("method", computeEffectiveMethodName(m_name)));
        callback.addAttribute(new Attribute("type", action.name().toLowerCase()));
        requires.addElement(callback);

        workbench.getIds().put(m_id, requires);
        workbench.getElements().put(requires, null);
    }
}
