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

import org.apache.felix.ipojo.manipulator.MetadataProvider;
import org.apache.felix.ipojo.manipulator.spi.ModuleProvider;
import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.ResourceStore;
import org.apache.felix.ipojo.manipulator.ResourceVisitor;
import org.apache.felix.ipojo.manipulator.metadata.annotation.ClassMetadataCollector;
import org.apache.felix.ipojo.manipulator.metadata.annotation.registry.BindingRegistry;
import org.apache.felix.ipojo.manipulator.spi.provider.CoreModuleProvider;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.ClassReader;

import static org.apache.felix.ipojo.manipulator.metadata.annotation.visitor.util.Bindings.newBindingRegistry;

/**
 * A {@code AnnotationMetadataProvider} loads iPOJO metadata from bytecode of classes.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AnnotationMetadataProvider implements MetadataProvider {

    private ResourceStore m_store;

    private Reporter m_reporter;
    private BindingRegistry m_registry;

    public AnnotationMetadataProvider(final ResourceStore store,
                                      final Reporter reporter) {
        this(store, new CoreModuleProvider(), reporter);
    }

    public AnnotationMetadataProvider(final ResourceStore store,
                                      final ModuleProvider provider,
                                      final Reporter reporter) {
        this(store, newBindingRegistry(reporter, store, provider), reporter);
    }

    public AnnotationMetadataProvider(final ResourceStore store,
                                      final BindingRegistry registry,
                                      final Reporter reporter) {
        this.m_store = store;
        this.m_registry = registry;
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
                        m_reporter.warn("Cannot read content of %s", name);
                    }

                    // We check the array size to avoid manipulating empty files
                    // produced by incremental compilers (like Eclipse Compiler)
                    if (data != null && data.length > 0) {
                        computeAnnotations(name, data, metadata);
                    } else {
                        m_reporter.error("Cannot compute annotations from %s : Empty file", name);
                    }
                }
            }
        });

        return metadata;
    }

    /**
     * Parse the content of the class to detect annotated classes.
     * @param name Resource's name
     * @param bytecode the class' content to inspect.
     * @param metadata list of metadata to be filled
     */
    private void computeAnnotations(String name, byte[] bytecode, List<Element> metadata) {

        ClassReader cr = new ClassReader(bytecode);
        ClassMetadataCollector collector = new ClassMetadataCollector(m_registry, m_reporter);
        cr.accept(collector, 0);

        if (collector.getComponentMetadata() != null) {
            metadata.add(collector.getComponentMetadata());

            // Instantiate ?
            Element instance = collector.getInstanceMetadata();
            if (instance != null) {
                m_reporter.trace("Declaring an instance of %s", instance.getAttribute("component"));
                metadata.add(instance);
            }
        }
    }


}
