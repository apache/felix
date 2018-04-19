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
package org.apache.felix.utils.capabilities;

import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

import java.util.Collections;
import java.util.Map;

/**
 * Implementation of the OSGi Capability interface.
 */
public class CapabilityImpl implements Capability
{
    private final String namespace;
    private final Map<String, Object> attributes;
    private final Map<String, String> directives;
    private volatile Resource resource;

    /**
     * Create a capability.
     *
     * @param ns The namespace of the capability.
     * @param attrs The attributes of the capability.
     * @param dirs The directives of the capability.
     */
    public CapabilityImpl(String ns, Map<String, Object> attrs, Map<String, String> dirs)
    {
        this(ns, attrs, dirs, null);
    }

    /**
     * Create a capability.
     *
     * @param ns The namespace of the capability.
     * @param attrs The attributes of the capability.
     * @param dirs The directives of the capability.
     * @param res The resource associated with the capability.
     */
    public CapabilityImpl(String ns, Map<String, Object> attrs, Map<String, String> dirs, Resource res)
    {
        namespace = ns;
        attributes = Collections.unmodifiableMap(attrs);
        directives = Collections.unmodifiableMap(dirs);
        resource = res;
    }

    /**
     * Returns the namespace of this capability.
     *
     * @return The namespace of this capability.
     */
    public String getNamespace()
    {
        return namespace;
    }

    /**
     * Returns the attributes of this capability.
     *
     * @return An unmodifiable map of attribute names to attribute values for
     *         this capability, or an empty map if this capability has no
     *         attributes.
     */
    public Map<String, Object> getAttributes()
    {
        return attributes;
    }

    /**
     * Returns the directives of this capability.
     *
     * @return An unmodifiable map of directive names to directive values for
     *         this capability, or an empty map if this capability has no
     *         directives.
     */
    public Map<String, String> getDirectives()
    {
        return directives;
    }

    /**
     * Returns the resource declaring this capability.
     *
     * @return The resource declaring this capability.
     */
    public Resource getResource()
    {
        return resource;
    }

    /**
     * Sets the resource associated with this capability.
     *
     * @param res The resource.
     */
    public void setResource(Resource res)
    {
        resource = res;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((attributes == null) ? 0 : attributes.hashCode());
        result = prime * result + ((directives == null) ? 0 : directives.hashCode());
        result = prime * result + ((namespace == null) ? 0 : namespace.hashCode());
        result = prime * result + ((resource == null) ? 0 : resource.hashCode());
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
        CapabilityImpl other = (CapabilityImpl) obj;
        if (attributes == null) {
            if (other.attributes != null)
                return false;
        } else if (!attributes.equals(other.attributes))
            return false;
        if (directives == null) {
            if (other.directives != null)
                return false;
        } else if (!directives.equals(other.directives))
            return false;
        if (namespace == null) {
            if (other.namespace != null)
                return false;
        } else if (!namespace.equals(other.namespace))
            return false;
        if (resource == null) {
            if (other.resource != null)
                return false;
        } else if (!resource.equals(other.resource))
            return false;
        return true;
    }
}
