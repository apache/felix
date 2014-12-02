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

package org.apache.felix.ipojo.manipulator.metadata.annotation;

import org.apache.felix.ipojo.manipulator.metadata.annotation.registry.BindingRegistry;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ComponentWorkbench {

    /**
     * Root element (usually <component /> or <handler/>).
     * Maybe null until set.
     */
    private Element root;

    /**
     * Instance element (may be {@literal null}).
     */
    private Element instance;

    /**
     * Map of [element ids, element].
     * This map is used to easily get an already created element.
     */
    private Map<String, Element> m_ids = new TreeMap<String, Element>();

    /**
     * Map of [element, referto].
     * This map is used to recreate the element hierarchy.
     * Stored element are added under referred element.
     */
    private Map<Element, String> m_elements = new LinkedHashMap<Element, String>();

    private Type type;

    private BindingRegistry bindingRegistry;

    private ClassNode classNode;

    /**
     * A flag indicating if the class needs to be ignored.
     */
    private boolean toIgnore;

    public ComponentWorkbench(BindingRegistry bindingRegistry, ClassNode node) {
        this.bindingRegistry = bindingRegistry;
        this.classNode = node;
        this.type = Type.getObjectType(node.name);
    }

    public Type getType() {
        return type;
    }

    public BindingRegistry getBindingRegistry() {
        return bindingRegistry;
    }

    public ClassNode getClassNode() {
        return classNode;
    }

    /**
     * The identified root Element. May be null if at the visit time, the root as not been identified.
     *
     * @return the root Element. or {@literal null} if not defined at the execution time.
     */
    public Element getRoot() {
        return root;
    }

    public void setRoot(Element root) {
        // TODO check if root already assigned
        this.root = root;
    }

    public Element getInstance() {
        return instance;
    }

    public void setInstance(Element instance) {
        this.instance = instance;
    }

    public Map<String, Element> getIds() {
        return m_ids;
    }

    public Map<Element, String> getElements() {
        return m_elements;
    }

    public Element build() {

        if (root == null) {
            // No 'top level' component Element has been registered
            return null;
        }

        // Iterates on all contributed Elements
        for (Element current : m_elements.keySet()) {

            // If the traversed Element has a reference to another Element,
            // it has to be moved inside that referenced Element
            // This is useful for contributing data to other handlers

            String refId = m_elements.get(current);
            if (refId == null) {
                // No reference provided, just add it a a direct child
                root.addElement(current);
            } else {

                // Get the referenced Element (if any)
                Element ref = m_ids.get(refId);
                if (ref == null) {
                    // Add to the root Element
                    root.addElement(current);
                } else {
                    // Add as child of the referenced Element
                    ref.addElement(current);
                }
            }
        }

        // Clear
        m_ids.clear();
        m_elements.clear();

        return root;

    }

    /**
     * Checks whether this class must be ignored.
     *
     * @return {@code true} if the class is ignored.
     */
    public boolean ignore() {
        return toIgnore;
    }

    /**
     * Sets the 'ignore' aspect of the current class.
     *
     * @param ignore whether or not the class must be ignored.
     */
    public void ignore(boolean ignore) {
        this.toIgnore = ignore;
    }
}
