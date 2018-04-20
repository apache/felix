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
package org.apache.felix.utils.capabilities;

import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import java.util.Collections;
import java.util.Map;

/**
 * Implementation of the OSGi Requirement interface.
 */
public class RequirementImpl extends AbstractCapabilityRequirement implements Requirement {
    /**
     * Create a requirement that is not associated with a resource.
     * @param res The resource associated with the requirement.
     * @param ns The namespace of the requirement.
     * @param attrs The attributes of the requirement.
     * @param dirs The directives of the requirement.
     */
    public RequirementImpl(String ns, Map<String, Object> attrs, Map<String, String> dirs) {
        this(ns, attrs, dirs, null);
    }

    /**
     * Create a requirement.
     * @param ns The namespace of the requirement.
     * @param attrs The attributes of the requirement.
     * @param dirs The directives of the requirement.
     * @param res The resource associated with the requirement.
     */
    public RequirementImpl(String ns, Map<String, Object> attrs, Map<String, String> dirs, Resource res) {
        super(ns, attrs, dirs, res);
    }

    /**
      * Create a requirement with a namespace and a filter.
      *
      * This is a convenience method that creates a requirement with
      * an empty attributes map and a single 'filter' directive.
      * @param ns The namespace for the requirement.
      * @param filter The filter.
      */
     public RequirementImpl(String ns, String filter)
     {
         this(ns, Collections.<String, Object>emptyMap(),
             filter == null ? Collections.<String, String> emptyMap() :
             Collections.singletonMap(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter));
     }

    /**
     * Create a requirement based on an existing requirement, providing the resource.
     * The namespace, attributes and directives are copied from the provided requirement.
     * @param requirement The requirement to base the new requirement on.
     * @param resource The resource to be associated with the requirement
     */
    public RequirementImpl(Resource resource, Requirement requirement) {
        this(requirement.getNamespace(), requirement.getAttributes(), requirement.getDirectives(), resource);
    }
}
