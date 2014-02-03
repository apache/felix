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
package org.apache.felix.bundlerepository.impl;

import java.util.Collections;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Resource;

class OSGiCapabilityImpl implements Capability
{
    private final String namespace;
    private final Map<String, Object> attributes;
    private final Map<String, String> directives;
    private Resource resource;

    OSGiCapabilityImpl(String ns, Map<String, Object> attrs, Map<String, String> dirs)
    {
        this(ns, attrs, dirs, null);
    }

    OSGiCapabilityImpl(String ns, Map<String, Object> attrs, Map<String, String> dirs, Resource res)
    {
        namespace = ns;
        attributes = Collections.unmodifiableMap(attrs);
        directives = Collections.unmodifiableMap(dirs);
        resource = res;
    }

    public String getNamespace()
    {
        return namespace;
    }

    public Map<String, Object> getAttributes()
    {
        return attributes;
    }

    public Map<String, String> getDirectives()
    {
        return directives;
    }

    public Resource getResource()
    {
        return resource;
    }

    void setResource(Resource res)
    {
        resource = res;
    }
}
