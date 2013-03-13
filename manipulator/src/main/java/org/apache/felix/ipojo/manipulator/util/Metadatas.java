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

package org.apache.felix.ipojo.manipulator.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.ipojo.metadata.Element;

/**
 * {@code Streams} is a utility class that helps to manipulate streams.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Metadatas {

    /**
     * Utility class: no public constructor
     */
    private Metadatas() {}

    /**
     * Return the {@literal classname} attribute value.
     * @param meta metadata to be explored
     * @return the {@literal classname} attribute value or {@literal null} if
     *         the attribute does not exists.
     */
    public static String getComponentType(Element meta) {
        return meta.getAttribute("classname");
    }

    /**
     * Looks for 'field' attribute in the given metadata.
     * @param fields discovered fields (accumulator)
     * @param metadata metadata to inspect
     */
    public static void findFields(List<String> fields, Element metadata) {
        String field = metadata.getAttribute("field");
        if (field != null && !fields.contains(field)) {
            fields.add(field);
        }
        for (Element element : metadata.getElements()) {
            findFields(fields, element);
        }
    }

    /**
     * Get packages referenced by component.
     * @param metadata Element base for the search
     * @return the Set of referenced packages.
     */
    public static Set<String> findReferredPackages(Element metadata) {

        Set<String> packages = new HashSet<String>();
        Set<String> specifications = findAttributes(metadata, "specification");

        // Extract the package name from each specification (aka interface)
        for (String specification : specifications) {
            String name = getPackageName(specification);
            if (name != null) {
                packages.add(name);
            }
        }

        return packages;
    }

    private static String getPackageName(String specification) {
        int last = specification.lastIndexOf('.');
        if (last != -1) {
            return specification.substring(0, last);
        }
        return null;
    }

    /**
     * Find all the values of the specified attribute in the given element.
     * @param metadata Element to be traversed
     * @param attributeName Search attribute name
     * @return Set of attribute values (no duplicate).
     */
    public static Set<String> findAttributes(Element metadata, String attributeName) {
        Set<String> referred = new HashSet<String>();

        // Search in the given element
        if (metadata.containsAttribute(attributeName)) {
            referred.add(metadata.getAttribute(attributeName));
        }

        // Search in children
        for (Element elem : metadata.getElements()) {
            Set<String> found = findAttributes(elem, attributeName);
            referred.addAll(found);
        }

        // Return all found values
        return referred;
    }



}
