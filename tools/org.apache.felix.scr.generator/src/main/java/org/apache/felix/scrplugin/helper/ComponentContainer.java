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
package org.apache.felix.scrplugin.helper;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.felix.scrplugin.description.ClassDescription;
import org.apache.felix.scrplugin.description.ComponentDescription;
import org.apache.felix.scrplugin.description.PropertyDescription;
import org.apache.felix.scrplugin.description.ReferenceDescription;
import org.apache.felix.scrplugin.description.ServiceDescription;


/**
 * A <code>ComponentContainer</code> contains all relevant information
 * about a component:
 * - the class descriptor
 * - the component descriptor
 * - the service descriptor
 * - reference descriptors
 * - property descriptors
 */
public class ComponentContainer {

    /** The class description. */
    private final ClassDescription classDescription;

    /** The component description. */
    private final ComponentDescription componentDescription;

    /** The service description (optional). */
    private ServiceDescription serviceDescription;

    /** All references. */
    private final Map<String, ReferenceDescription> allReferences = new LinkedHashMap<String, ReferenceDescription>();

    /** All properties. */
    private final Map<String, PropertyDescription> allProperties = new LinkedHashMap<String, PropertyDescription>();

    /** Metatype container. */
    private MetatypeContainer metatype;

    /**
     * Create a new component container
     * @param classDescription     Class description
     * @param componentDescription Component description
     */
    public ComponentContainer(final ClassDescription classDescription,
                    final ComponentDescription componentDescription) {
        this.classDescription = classDescription;
        this.componentDescription = componentDescription;
    }

    /**
     * Get the class description
     * @return The class description
     */
    public ClassDescription getClassDescription() {
        return this.classDescription;
    }

    /**
     * Get the component description
     * @return The component description
     */
    public ComponentDescription getComponentDescription() {
        return this.componentDescription;
    }

    /**
     * Get all references.
     * The references are put into the map by name.
     * This map can be modified by clients.
     * @return The map of references
     */
    public Map<String, ReferenceDescription> getReferences() {
        return this.allReferences;
    }

    /**
     * Get all properties.
     * The properties are put into the map by name.
     * This map can be modified by clients.
     * @return The map of properties
     */
    public Map<String, PropertyDescription> getProperties() {
        return this.allProperties;
    }

    /**
     * Get the service description.
     * @return The service description or <code>null</code>
     */
    public ServiceDescription getServiceDescription() {
        return serviceDescription;
    }

    /**
     * Set the service description
     * @param serviceDescription The new service description
     */
    public void setServiceDescription(final ServiceDescription serviceDescription) {
        this.serviceDescription = serviceDescription;
    }

    @Override
    public String toString() {
        return "ComponentContainer [classDescription=" + classDescription + ", componentDescription=" + componentDescription
                        + ", serviceDescription=" + serviceDescription + ", allReferences=" + allReferences + ", allProperties="
                        + allProperties + "]";
    }

    public MetatypeContainer getMetatypeContainer() {
        return this.metatype;
    }

    public void setMetatypeContainer(final MetatypeContainer ocd) {
        this.metatype = ocd;
    }
}
