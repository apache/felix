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
package org.apache.felix.ipojo.manipulator.metadata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.manipulation.annotations.MetadataCollector;
import org.apache.felix.ipojo.manipulator.MetadataProvider;
import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.ResourceStore;
import org.apache.felix.ipojo.manipulator.ResourceVisitor;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.ClassReader;

/**
 * A {@code AnnotationMetadataProvider} loads iPOJO metadata from bytecode of classes.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AnnotationMetadataProvider implements MetadataProvider {

    private ResourceStore m_store;

    private Reporter m_reporter;

    public AnnotationMetadataProvider(final ResourceStore store,
                                      final Reporter reporter) {
        this.m_store = store;
        this.m_reporter = reporter;
    }

    public List<Element> getMetadatas() throws IOException {

        final List<Element> metadata = new ArrayList<Element>();


        m_store.accept(new ResourceVisitor() {
            public void visit(String name) {
                if (name.endsWith(".class")) {

                    // Read file's content
                    byte[] data = null;
                    try {
                        data = m_store.read(name);
                    } catch (IOException e) {
                        m_reporter.warn("Cannot read content of " + name);
                    }

                    // We check the array size to avoid manipulating empty files
                    // produced by incremental compilers (like Eclipse Compiler)
                    if (data != null && data.length > 0) {
                        computeAnnotations(data, metadata);
                    } else {
                        m_reporter.error("Cannot compute annotations from " + name + " : Empty file");
                    }
                }
            }
        });

        return metadata;
    }

    /**
     * Parse the content of the class to detect annotated classes.
     * @param bytecode the class to inspect.
     * @param metadata
     */
    private void computeAnnotations(byte[] bytecode, List<Element> metadata) {

        ClassReader cr = new ClassReader(bytecode);
        MetadataCollector collector = new MetadataCollector();
        cr.accept(collector, 0);

        if (collector.isIgnoredBecauseOfMissingComponent()) {
            // No @Component, just skip.
            //warn("Annotation processing ignored in " + collector.getClassName() + " - @Component missing");
        } else if (collector.isComponentType()) {

            // if no metadata or empty one, create a new array.
            metadata.add(collector.getComponentTypeDeclaration());

            // Instantiate ?
            if (collector.getInstanceDeclaration() != null) {
                //warn("Declaring an empty instance of " + elem.getAttribute("classname"));
                metadata.add(collector.getInstanceDeclaration());
            }
        }
    }


}
