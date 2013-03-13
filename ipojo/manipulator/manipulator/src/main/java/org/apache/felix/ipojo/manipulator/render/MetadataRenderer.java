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

package org.apache.felix.ipojo.manipulator.render;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

/**
 * A {@code MetadataRenderer} renders a given {@link Element} into a String.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MetadataRenderer {

    private List<MetadataFilter> m_filters = new ArrayList<MetadataFilter>();

    public MetadataRenderer() {
        // By default, filter metadata coming from prior manipulation.
        this.addMetadataFilter(new ManipulatedMetadataFilter());
    }

    /**
     * Add a metadata filter
     * @param filter added filter
     */
    public void addMetadataFilter(MetadataFilter filter) {
        m_filters.add(filter);
    }

    /**
     * Generate manipulation metadata.
     * @param element rendered element.
     * @return given manipulation metadata + manipulation metadata of the given element.
     */
    public String render(Element element) {
        StringBuilder builder = new StringBuilder();
        renderElement(element, builder);
        return builder.toString();
    }

    private void renderElement(Element element, StringBuilder builder) {

        // If the element is already here, do not re-add the element.
        if(!isFiltered(element)) {

            // Print the beginning of the element
            startElement(element, builder);

            // Render all attributes
            for (Attribute attribute : element.getAttributes()) {
                renderAttribute(attribute, builder);
            }

            // Render child elements
            for (Element child : element.getElements()) {
                renderElement(child, builder);
            }

            // Print the end of the element
            endElement(builder);
        }


    }

    private void startElement(Element element, StringBuilder builder) {
        // Default namespace is empty
        String namespace = "";
        if (element.getNameSpace() != null) {
            namespace = element.getNameSpace() + ":";
        }

        builder.append(namespace)
                .append(element.getName())
                .append(" { ");
    }

    private void endElement(StringBuilder builder) {
        builder.append("}");
    }

    private void renderAttribute(Attribute current, StringBuilder builder) {

        // Default namespace is empty
        String namespace = "";
        if (current.getNameSpace() != null) {
            namespace = current.getNameSpace() + ":";
        }

        // Render the attribute
        builder.append("$")
                .append(namespace)
                .append(current.getName())
                .append("=")
                .append(quoted(current.getValue()))
                .append(" ");
    }

    private String quoted(String value) {
        return "\"" + value + "\"";
    }

    /**
     * Checks if the given element is an iPOJO generated element from a prior treatment
     * @param element Element to be tested
     * @return <code>true</code> if the given element was already injected by iPOJO
     */
    private boolean isFiltered(final Element element) {

        // Iterates over all the filters and return the first positive answer (if any)
        for (MetadataFilter filter : m_filters) {
            if (filter.accept(element)) {
                return true;
            }
        }

        return false;
    }

}
