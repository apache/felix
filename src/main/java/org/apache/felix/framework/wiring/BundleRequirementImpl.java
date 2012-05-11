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
package org.apache.felix.framework.wiring;

import java.util.Collections;
import java.util.Map;
import org.apache.felix.framework.capabilityset.CapabilitySet;
import org.apache.felix.framework.capabilityset.SimpleFilter;
import org.apache.felix.framework.util.ImmutableMap;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Resource;

public class BundleRequirementImpl implements BundleRequirement
{
    private final BundleRevision m_revision;
    private final String m_namespace;
    private final SimpleFilter m_filter;
    private final boolean m_optional;
    private final Map<String, String> m_dirs;
    private final Map<String, Object> m_attrs;

    public BundleRequirementImpl(
        BundleRevision revision, String namespace,
        Map<String, String> dirs, Map<String, Object> attrs, SimpleFilter filter)
    {
        m_revision = revision;
        m_namespace = namespace;
        m_dirs = ImmutableMap.newInstance(dirs);
        m_attrs = ImmutableMap.newInstance(attrs);
        m_filter = filter;

        // Find resolution import directives.
        boolean optional = false;
        if (m_dirs.containsKey(Constants.RESOLUTION_DIRECTIVE)
            && m_dirs.get(Constants.RESOLUTION_DIRECTIVE).equals(Constants.RESOLUTION_OPTIONAL))
        {
            optional = true;
        }
        m_optional = optional;
    }

    public BundleRequirementImpl(
        BundleRevision revision, String namespace,
        Map<String, String> dirs, Map<String, Object> attrs)
    {
        this(revision, namespace, dirs, Collections.EMPTY_MAP, SimpleFilter.convert(attrs));
    }

    public String getNamespace()
    {
        return m_namespace;
    }

    public Map<String, String> getDirectives()
    {
        return m_dirs;
    }

    public Map<String, Object> getAttributes()
    {
        return m_attrs;
    }

    public BundleRevision getResource()
    {
        return m_revision;
    }

    public BundleRevision getRevision()
    {
        return m_revision;
    }

    public boolean matches(BundleCapability cap)
    {
        return CapabilitySet.matches((BundleCapabilityImpl) cap, getFilter());
    }

    public boolean isOptional()
    {
        return m_optional;
    }

    public SimpleFilter getFilter()
    {
        return m_filter;
    }

    @Override
    public String toString()
    {
        return "[" + m_revision + "] " + m_namespace + "; " + getFilter().toString();
    }
}