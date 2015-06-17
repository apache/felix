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
package org.apache.felix.resolver.test.util;

import java.util.HashMap;
import java.util.Map;

import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

public class GenericRequirement implements Requirement
{
    private final Resource m_resource;
    private final String m_namespace;
    private final Map<String, String> m_dirs;
    private final Map<String, Object> m_attrs;

    public GenericRequirement(Resource resource, String namespace)
    {
        m_resource = resource;
        m_namespace = namespace.intern();
        m_dirs = new HashMap<String, String>();
        m_attrs = new HashMap<String, Object>();
    }

    public GenericRequirement(Resource resource, String namespace, Map<String, String> directives, Map<String, Object> attributes) {
        this(resource, namespace);
        if (directives != null) {
            for (Map.Entry<String, String> entry : directives.entrySet()) {
                addDirective(entry.getKey(), entry.getValue());
            }
        }
        if (attributes != null) {
            for (Map.Entry<String, Object> entry : attributes.entrySet()) {
                addAttribute(entry.getKey(), entry.getValue());
            }
        }
    }

    public String getNamespace()
    {
        return m_namespace;
    }

    public void addDirective(String name, String value)
    {
        m_dirs.put(name.intern(), value);
    }

    public Map<String, String> getDirectives()
    {
        return m_dirs;
    }

    public void addAttribute(String name, Object value)
    {
        m_attrs.put(name.intern(), value);
    }

    public Map<String, Object> getAttributes()
    {
        return m_attrs;
    }

    public Resource getResource()
    {
        return m_resource;
    }

    @Override
    public String toString()
    {
        return getNamespace() + "; "
            + getDirectives();
    }
}