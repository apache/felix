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

import org.apache.felix.utils.collections.StringArrayMap;
import org.osgi.framework.Version;
import org.osgi.resource.Resource;

import java.util.Map;
import java.util.Objects;

abstract class AbstractCapabilityRequirement {

    /** The resource. Required. */
    protected final Resource resource;

    /** The namespace. Required. */
    protected final String namespace;

    /** Optional attributes. Never null. */
    protected final Map<String, String> directives;

    /** Optional attributes. Never null. */
    protected final Map<String, Object> attributes;

    AbstractCapabilityRequirement(final Resource res, final String ns, final Map<String, String> dirs, final Map<String, Object> attrs) {
        resource = res;
        namespace = Objects.requireNonNull(ns, "Namespace must not be null.");
        directives = StringArrayMap.reduceMemory(dirs);
        attributes = StringArrayMap.reduceMemory(attrs);
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

    @Override
    public String toString() {
        return ResourceUtils.toString(getResource(), getNamespace(), getAttributes(), getDirectives());
    }

}
