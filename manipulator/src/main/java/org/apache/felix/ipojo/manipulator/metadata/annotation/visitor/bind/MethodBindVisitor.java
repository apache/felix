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

import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.metadata.annotation.ComponentWorkbench;
import org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.util.Names;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.tree.MethodNode;

import static org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.util.Names.computeEffectiveMethodName;

/**
 * Parse @Bind & @Unbind annotations on methods.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MethodBindVisitor extends AbstractBindVisitor {

    /**
     * Method name.
     */
    private MethodNode m_node;

    /**
     * Error reporter.
     */
    private Reporter m_reporter;

    public MethodBindVisitor(ComponentWorkbench workbench, Action action, MethodNode method, Reporter reporter) {
        super(workbench, action);
        this.m_node = method;
        this.m_reporter = reporter;
    }

    /**
     * End of the visit.
     * Create or append the requirement info to a created or already existing "requires" element.
     *
     * @see org.objectweb.asm.commons.EmptyVisitor#visitEnd()
     */
    public void visitEnd() {
        if (m_id == null) {
            String identifier = Names.getMethodIdentifier(m_node);
            if (identifier != null) {
                m_id = identifier;
            } else {
                if (m_specification != null) {
                    m_id = m_specification;
                } else {
                    m_reporter.error("Cannot determine the requires identifier for the (%s) %s method: %s",
                                     computeEffectiveMethodName(m_node.name),
                                     action.name(),
                                     "Either 'id' attribute is missing or method name do not follow the bind/set/add/modified " +
                                     "naming pattern, or no specification (service interface) can be found in method signature " +
                                     "or specified in annotation. Dependency will be ignored (would cause an Exception at runtime)");
                    return;
                }
            }
        }

        Element requires = getRequiresElement();

        Element callback = new Element("callback", "");
        callback.addAttribute(new Attribute("method", computeEffectiveMethodName(m_node.name)));
        callback.addAttribute(new Attribute("type", action.name().toLowerCase()));
        requires.addElement(callback);

        workbench.getIds().put(m_id, requires);
        workbench.getElements().put(requires, null);
    }
}
