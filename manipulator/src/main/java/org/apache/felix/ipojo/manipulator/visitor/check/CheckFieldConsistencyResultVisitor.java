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

package org.apache.felix.ipojo.manipulator.visitor.check;

import org.apache.felix.ipojo.manipulator.ManipulationResultVisitor;
import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.util.Metadatas;
import org.apache.felix.ipojo.manipulator.visitor.ManipulationResultAdapter;
import org.apache.felix.ipojo.metadata.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * This visitor checks that field referenced in the metadata are present in the bytecode.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class CheckFieldConsistencyResultVisitor extends ManipulationResultAdapter {

    /**
     * Component's metadata.
     */
    private Element m_metadata;

    /**
     * Reporter for errors.
     */
    private Reporter m_reporter;

    public CheckFieldConsistencyResultVisitor(ManipulationResultVisitor visitor) {
        super(visitor);
    }

    public void setMetadata(Element metadata) {
        this.m_metadata = metadata;
    }

    public void setReporter(Reporter reporter) {
        this.m_reporter = reporter;
    }

    public void visitClassStructure(Element structure) {

        List<String> fieldsInStructure = new ArrayList<String>();
        collectStructuralFields(fieldsInStructure, structure);

        List<String> fieldsInMetadata = new ArrayList<String>();
        Metadatas.findFields(fieldsInMetadata, m_metadata);

        checkReferredFieldsAreInStructure(fieldsInMetadata, fieldsInStructure);

        // Do this at the end because the writer insert the manipulation
        // metadata inside the component Element
        // So to avoid duplicate find, we need to execute this code at the end
        super.visitClassStructure(structure);

    }

    private void collectStructuralFields(List<String> fieldsInStructure, Element structure) {
        Element[] fields = structure.getElements("field");
        if (fields != null) {
            for (Element field : fields) {
                fieldsInStructure.add(field.getAttribute("name"));
            }
        }
    }

    /**
     * Detects missing fields.
     * If a referenced field does not exist in the class
     * the method throws an error breaking the build process.
     * @param fieldsInMetadata
     * @param fieldsInStructure
     */
    private void checkReferredFieldsAreInStructure(List<String> fieldsInMetadata, List<String> fieldsInStructure) {
        // Then, try to find each referred field in the given field map
        for (String fieldName : fieldsInMetadata) {
            if (!fieldsInStructure.contains(fieldName)) {
                m_reporter.error("The field " + fieldName + " is referenced in the "
                        + "metadata but does not exist in the " + Metadatas.getComponentType(m_metadata)
                        + " class");
            }
        }
    }

}
