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

import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

class OSGiRequirementImpl implements Requirement
{
    private final String namespace;
    private final Map<String, Object> attributes;
    private final Map<String, String> directives;

    OSGiRequirementImpl(String ns, String filter)
    {
        this(ns, Collections.<String, Object>emptyMap(),
            filter == null ? Collections.<String, String> emptyMap() :
            Collections.singletonMap(Namespace.REQUIREMENT_FILTER_DIRECTIVE, filter));
    }

    OSGiRequirementImpl(String ns, Map<String, Object> attrs, Map<String, String> dirs)
    {
        namespace = ns;
        attributes = attrs;
        directives = dirs;
    }

    public String getNamespace()
    {
        return namespace;
    }

    public Map<String, Object> getAttributes()
    {
        return Collections.unmodifiableMap(attributes);
    }

    public Map<String, String> getDirectives()
    {
        return Collections.unmodifiableMap(directives);
    }

    public Resource getResource()
    {
        return null;
    }
}
