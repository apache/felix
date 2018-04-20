/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.felix.utils.resource;

import org.osgi.framework.Constants;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the OSGi Capability interface.
 */
public class CapabilityImpl extends AbstractCapabilityRequirement implements Capability {

    protected final Set<String> mandatory;

    /**
     * Create a capability.
     * @param res The resource associated with the capability.
     * @param ns The namespace of the capability.
     * @param attrs The attributes of the capability.
     * @param dirs The directives of the capability.
     */
    public CapabilityImpl(Resource res, String ns, Map<String, String> dirs, Map<String, Object> attrs) {
        super(res, ns, dirs, attrs);

        // Handle mandatory directive
        Set<String> mandatory = Collections.emptySet();
        String value = this.directives.get(Constants.MANDATORY_DIRECTIVE);
        if (value != null) {
            List<String> names = ResourceBuilder.parseDelimitedString(value, ",");
            mandatory = new HashSet<>(names.size());
            for (String name : names) {
                // If attribute exists, then record it as mandatory.
                if (this.attributes.containsKey(name)) {
                    mandatory.add(name);
                    // Otherwise, report an error.
                } else {
                    throw new IllegalArgumentException("Mandatory attribute '" + name + "' does not exist.");
                }
            }
        }
        this.mandatory = mandatory;
    }

    /**
     * Create a capability based on an existing capability, providing the resource.
     * The namespace, attributes and directives are copied from the provided capability.
     * @param capability The capability to base the new requirement on.
     * @param resource The resource to be associated with the capability
     */
    public CapabilityImpl(Resource resource, Capability capability) {
        this(resource, capability.getNamespace(), capability.getDirectives(), capability.getAttributes());
    }

    public boolean isAttributeMandatory(String name) {
        return !mandatory.isEmpty() && mandatory.contains(name);
    }
}
