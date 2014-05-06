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

import org.apache.felix.ipojo.manipulation.Manipulator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@code ManipulationEngine} is responsible to drive the component's
 * classes manipulation.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ManipulationEngine {

    /**
     * The classloader given to the manipulator to load classes.
     */
    private final ClassLoader m_classLoader;
    /**
     * List of component types.
     */
    private List<ManipulationUnit> m_manipulationUnits = new ArrayList<ManipulationUnit>();

    /**
     * Error reporting.
     */
    private Reporter m_reporter;

    /**
     * Bytecode store.
     */
    private ResourceStore m_store;

    /**
     * The visitor handling output result.
     */
    private ManipulationVisitor m_manipulationVisitor;

    public ManipulationEngine(ClassLoader classLoader) {
        m_classLoader = classLoader;
    }

    /**
     * Add information related to a discovered component that will be manipulated.
     * @param component additional component
     */
    public void addManipulationUnit(ManipulationUnit component) {
        m_manipulationUnits.add(component);
    }

    public void setManipulationVisitor(ManipulationVisitor manipulationVisitor) {
        m_manipulationVisitor = manipulationVisitor;
    }

    /**
     * @param reporter Feedback reporter.
     */
    public void setReporter(Reporter reporter) {
        m_reporter = reporter;
    }

    /**
     * Provides the bytecode store that allows to retrieve bytecode of the
     * component's related resources (inner classes for example).
     * @param store Helps to locate bytecode for classes.
     */
    public void setResourceStore(ResourceStore store) {
        m_store = store;
    }

    /**
     * Manipulates classes of all the given component's.
     */
    public void generate() {

        // Iterates over the list of discovered components
        // Note that this list includes components from metadata.xml AND from annotations

        for (ManipulationUnit info : m_manipulationUnits) {

            byte[] bytecode;
            try {
                bytecode = m_store.read(info.getResourcePath());
            } catch (IOException e) {
                m_reporter.error("Cannot find bytecode for class '" + info.getClassName() + "': no bytecode found.");
                return;
            }

            // Is the visitor interested in this component ?
            ManipulationResultVisitor result = m_manipulationVisitor.visitManipulationResult(info.getComponentMetadata());

            if (result != null) {
                // Should always be the case

                // Manipulation preparation
                Manipulator manipulator = new Manipulator(m_classLoader);
                try {
                    manipulator.prepare(bytecode);
                } catch (IOException e) {
                    m_reporter.error("Cannot analyze the class " + info.getClassName() + " : " + e.getMessage());
                    return;
                }

                // Inner class preparation
                for (String inner : manipulator.getInnerClasses()) {
                    // Get the bytecode and start manipulation
                    String resourcePath = inner + ".class";
                    String outerClassInternalName = info.getClassName().replace('.', '/');
                    byte[] innerClassBytecode;
                    try {
                        innerClassBytecode = m_store.read(resourcePath);
                        manipulator.prepareInnerClass(inner, innerClassBytecode);
                    } catch (IOException e) {
                        m_reporter.error("Cannot find or analyze inner class '" + resourcePath + "'");
                        return;
                    }
                }

                // Now manipulate the classes.
                try {
                    byte[] out = manipulator.manipulate(bytecode);
                    // Call the visitor
                    result.visitManipulatedResource(info.getResourcePath(), out);
                } catch (IOException e) {
                    m_reporter.error("Cannot manipulate the class " + info.getClassName() + " : " + e.getMessage());
                    return;
                }

                // Visit inner classes
                for (String inner : manipulator.getInnerClasses()) {
                    // Get the bytecode and start manipulation
                    String resourcePath = inner + ".class";
                    String outerClassInternalName = info.getClassName().replace('.', '/');
                    byte[] innerClassBytecode;
                    try {
                        innerClassBytecode = m_store.read(resourcePath);
                    } catch (IOException e) {
                        m_reporter.error("Cannot find inner class '" + resourcePath + "'");
                        return;
                    }

                    // Manipulate inner class
                    // Notice that (for performance reason) re-use the class version information
                    // discovered in the main class instead of re-parsing the inner class to find
                    // its own class version
                    try {
                        byte[] manipulated = manipulator.manipulateInnerClass(inner, innerClassBytecode);
                        // Propagate manipulated resource
                        result.visitManipulatedResource(resourcePath, manipulated);
                    } catch (IOException e) {
                        m_reporter.error("Cannot manipulate inner class '" + resourcePath + "'");
                        return;
                    }
                }

                // Compute manipulation metadata
                result.visitClassStructure(manipulator.getManipulationMetadata());

                // All resources have been manipulated for this component
                result.visitEnd();
            }
        }
    }
}
