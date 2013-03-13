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

import org.apache.felix.ipojo.manipulator.util.Strings;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Component Info.
 * Represent a component type to be manipulated or already manipulated.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ManipulationUnit {

    private Element m_componentMetadata;

    private String m_resourcePath;

    private String m_className;

    /**
     * Constructor.
     * @param resourcePath class name
     * @param meta component type metadata
     */
    public ManipulationUnit(String resourcePath, Element meta) {
        m_resourcePath = resourcePath;
        m_componentMetadata = meta;
        m_className = Strings.asClassName(resourcePath);
    }

    /**
     * @return Component Type metadata.
     */
    public Element getComponentMetadata() {
        return m_componentMetadata;
    }

    /**
     * @return Resource path
     */
    public String getResourcePath() {
        return m_resourcePath;
    }

    /**
     * @return Fully qualified class name
     */
    public String getClassName() {
        return m_className;
    }

}
