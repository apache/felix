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

import org.osgi.resource.Resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

abstract class AbstractCapabilityRequirement {

    /** The namespace. Required. */
    private final String namespace;

    /** Optional resource. */
    private volatile Resource resource;

    /** Optional attributes. Never null. */
    private final Map<String, Object> attributes;

    /** Optional attributes. Never null. */
    private final Map<String, String> directives;

    AbstractCapabilityRequirement(final String ns, final Map<String, Object> attrs, final Map<String, String> dirs, final Resource res) {
        if ( ns == null ) {
            throw new IllegalArgumentException("Namespace must not be null.");
        }
        namespace = ns;
        attributes = attrs == null
                ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new HashMap<String, Object>(attrs));
        directives = dirs == null
                ? Collections.<String,String>emptyMap()
                : Collections.unmodifiableMap(new HashMap<String,String>(dirs));
                resource = res;
    }

    /**
     * Return the namespace.
     * @return The namespace. This is never @{code null}.
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Return the attributes.
     * @return The attributes, might be empty.
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * Return the directives.
     * @return The directives, might be empty.
     */
    public Map<String, String> getDirectives() {
        return directives;
    }

    /**
     * Return the resource.
     * @return The resource or @{code null}.
     */
    public Resource getResource() {
        return resource;
    }

    /**
     * Set the resource associated with this requirement.
     *
     * @param res The resource.
     */
    public void setResource(Resource res) {
        resource = res;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + attributes.hashCode();
        result = prime * result + directives.hashCode();
        result = prime * result + namespace.hashCode();

        if (resource != null)
            result = prime * result + resource.hashCode();

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractCapabilityRequirement other = (AbstractCapabilityRequirement) obj;
        if (!namespace.equals(other.namespace))
            return false;
        if (!attributes.equals(other.attributes))
            return false;
        if (!directives.equals(other.directives))
            return false;
        if (resource == null) {
            return other.resource == null;
        } else {
            return resource.equals(other.resource);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [resource=" + resource + ", namespace=" + namespace + ", attributes=" + attributes
                + ", directives=" + directives + "]";
    }
}
