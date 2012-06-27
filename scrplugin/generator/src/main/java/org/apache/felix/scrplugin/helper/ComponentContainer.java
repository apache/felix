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
 *
 */
public class ComponentContainer {

    private final ClassDescription classDescription;

    private final ComponentDescription componentDescription;

    private ServiceDescription serviceDescription;

    private final Map<String, ReferenceDescription> allReferences = new LinkedHashMap<String, ReferenceDescription>();
    private final Map<String, PropertyDescription> allProperties = new LinkedHashMap<String, PropertyDescription>();

    public ComponentContainer(final ClassDescription classDescription,
                    final ComponentDescription componentDescription) {
        this.classDescription = classDescription;
        this.componentDescription = componentDescription;
    }

    public ClassDescription getClassDescription() {
        return this.classDescription;
    }

    public ComponentDescription getComponentDescription() {
        return this.componentDescription;
    }

    public Map<String, ReferenceDescription> getReferences() {
        return this.allReferences;
    }

    public Map<String, PropertyDescription> getProperties() {
        return this.allProperties;
    }

    public ServiceDescription getServiceDescription() {
        return serviceDescription;
    }

    public void setServiceDescription(ServiceDescription serviceDescription) {
        this.serviceDescription = serviceDescription;
    }
}
