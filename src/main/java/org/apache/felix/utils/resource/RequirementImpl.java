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
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

import java.util.Collections;
import java.util.Map;

/**
 * Implementation of the OSGi Requirement interface.
 */
public class RequirementImpl extends AbstractCapabilityRequirement implements Requirement {

    private final SimpleFilter filter;
    private final boolean optional;

    /**
     * Create a requirement.
     * @param res The resource associated with the requirement.
     * @param ns The namespace of the requirement.
     * @param attrs The attributes of the requirement.
     * @param dirs The directives of the requirement.
     */
    public RequirementImpl(Resource res, String ns, Map<String, String> dirs, Map<String, Object> attrs) {
        this(res, ns, dirs, attrs, null);
    }

    /**
      * Create a requirement with a namespace and a filter.
      *
      * This is a convenience method that creates a requirement with
      * an empty attributes map and a single 'filter' directive.
     * @param res The resource associated with the requirement.
      * @param ns The namespace for the requirement.
      * @param filter The filter.
      */
     public RequirementImpl(Resource res, String ns, String filter)
     {
         this(res, ns,
             filter == null ? Collections.<String, String>emptyMap() :
             Collections.singletonMap(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter),
             null);
     }

    /**
     * Create a requirement based on an existing requirement, providing the resource.
     * The namespace, attributes and directives are copied from the provided requirement.
     * @param requirement The requirement to base the new requirement on.
     * @param resource The resource to be associated with the requirement
     */
    public RequirementImpl(Resource resource, Requirement requirement) {
        this(resource, requirement.getNamespace(), requirement.getDirectives(), requirement.getAttributes());
    }

    public RequirementImpl(Resource resource, String path, Map<String, String> dirs, Map<String, Object> attrs, SimpleFilter sf) {
        super(resource, path, dirs, attrs);
        this.filter = sf != null ? sf : SimpleFilter.convert(attributes);
        // Find resolution import directives.
        this.optional = Constants.RESOLUTION_OPTIONAL.equals(directives.get(Constants.RESOLUTION_DIRECTIVE));
    }

    public boolean matches(Capability cap) {
        return CapabilitySet.matches(cap, getFilter());
    }

    public boolean isOptional() {
        return optional;
    }

    public SimpleFilter getFilter() {
        return filter;
    }

    /**
     * Utility method to check wether a requirment is optional. This method works with any
     * object implementing the requirement interface.
     *
     * @param requirement A requirement
     * @return {@code true} if the requirement it optional, {@code false} otherwise.
     */
    public static boolean isOptional(Requirement requirement) {
        if (requirement instanceof RequirementImpl) {
            return ((RequirementImpl) requirement).isOptional();
        }

        return Constants.RESOLUTION_OPTIONAL.equals(requirement.getDirectives().get(Constants.RESOLUTION_DIRECTIVE));
    }
}
