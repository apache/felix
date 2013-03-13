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
import org.apache.felix.ipojo.manipulator.ManipulationVisitor;
import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.ResourceStore;
import org.apache.felix.ipojo.metadata.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Write manipulation result in the backend (store).
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ManipulatedResourcesWriter implements ManipulationVisitor {

    private ResourceStore m_resourceStore;
    private Reporter m_reporter;
    private List<ManipulatedResultWriter> m_writers;

    public ManipulatedResourcesWriter() {
        m_writers = new ArrayList<ManipulatedResultWriter>();
    }

    public void setResourceStore(ResourceStore resourceStore) {
        m_resourceStore = resourceStore;
    }

    public void setReporter(Reporter reporter) {
        m_reporter = reporter;
    }

    public ManipulationResultVisitor visitManipulationResult(Element metadata) {
        m_resourceStore.writeMetadata(metadata);
        ManipulatedResultWriter writer = new ManipulatedResultWriter(metadata);
        m_writers.add(writer);
        return writer;
    }

    public void visitMetadata(Element metadata) {
        m_resourceStore.writeMetadata(metadata);
    }

    public void visitEnd() {

        try {
            m_resourceStore.open();
            for (ManipulatedResultWriter writer : m_writers) {
                for (Map.Entry<String, byte[]> entry : writer.getResources().entrySet()) {
                    m_resourceStore.write(entry.getKey(), entry.getValue());
                }
            }
            m_resourceStore.close();
        } catch (IOException e) {
            m_reporter.error("Cannot store manipulation result: " + e.getMessage());
        }
    }
}
