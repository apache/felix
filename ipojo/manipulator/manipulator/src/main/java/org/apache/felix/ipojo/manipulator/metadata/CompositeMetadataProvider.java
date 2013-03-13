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
import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.metadata.Element;

/**
 * A {@code CompositeMetadataProvider} is responsible to detect duplicates
 * component's declaration.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class CompositeMetadataProvider implements MetadataProvider {

    private List<MetadataProvider> m_providers = new ArrayList<MetadataProvider>();
    private Reporter m_reporter;

    public CompositeMetadataProvider(Reporter reporter) {
        m_reporter = reporter;
    }

    public void addMetadataProvider(MetadataProvider provider) {
        m_providers.add(provider);
    }

    public List<Element> getMetadatas() throws IOException {
        List<Element> metadata = new ArrayList<Element>();
        for (MetadataProvider provider : m_providers) {

            List<Element> loaded = provider.getMetadatas();

            // Analyze each newly loaded metadata
            // And find duplicate component definition

            for (Element meta : loaded) {
                if (isInstance(meta)) {
                    // This is an instance, just add it to the list
                    metadata.add(meta);
                } else {
                    // Handler or Component
                    // Finds duplicate (if any)
                    String name = getComponentName(meta);
                    if (name != null) {
                        if (isDuplicate(metadata, name)) {
                            // TODO Try to add more information here, but what ?
                            m_reporter.warn("The component type " + name + " is duplicated.");
                        } else {
                            metadata.add(meta);
                        }
                    } else {
                        // no name, strange, but add it to the list
                        metadata.add(meta);
                    }
                }
            }
        }
        return metadata;
    }

    private boolean isDuplicate(List<Element> elements, String name) {
        for (Element element : elements) {
            if (!isInstance(element) && name.equals(getComponentName(element))) {
                return true;
            }
        }
        return false;
    }

    private String getComponentName(Element element) {
        return element.getAttribute("name");
    }

    private boolean isInstance(Element element) {
        return "instance".equals(element.getName());
    }
}
