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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import org.apache.felix.framework.capabilityset.SimpleFilter;
import org.apache.felix.framework.util.ImmutableMap;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.ManifestParser;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Resource;

public class BundleCapabilityImpl implements BundleCapability
{
    public static final String VERSION_ATTR = "version";

    private final BundleRevision m_revision;
    private final String m_namespace;
    private final Map<String, String> m_dirs;
    private final Map<String, Object> m_attrs;
    private final List<String> m_uses;
    private final List<List<String>> m_includeFilter;
    private final List<List<String>> m_excludeFilter;
    private final Set<String> m_mandatory;

    public BundleCapabilityImpl(BundleRevision revision, String namespace,
        Map<String, String> dirs, Map<String, Object> attrs)
    {
        m_namespace = namespace;
        m_revision = revision;
        m_dirs = ImmutableMap.newInstance(dirs);
        m_attrs = ImmutableMap.newInstance(attrs);

        // Find all export directives: uses, mandatory, include, and exclude.

        List<String> uses = Collections.EMPTY_LIST;
        String value = m_dirs.get(Constants.USES_DIRECTIVE);
        if (value != null)
        {
            // Parse these uses directive.
            StringTokenizer tok = new StringTokenizer(value, ",");
            uses = new ArrayList(tok.countTokens());
            while (tok.hasMoreTokens())
            {
                uses.add(tok.nextToken().trim());
            }
        }
        m_uses = uses;

        value = m_dirs.get(Constants.INCLUDE_DIRECTIVE);
        if (value != null)
        {
            List<String> filters = ManifestParser.parseDelimitedString(value, ",");
            m_includeFilter = new ArrayList<List<String>>(filters.size());
            for (int filterIdx = 0; filterIdx < filters.size(); filterIdx++)
            {
                List<String> substrings = SimpleFilter.parseSubstring(filters.get(filterIdx));
                m_includeFilter.add(substrings);
            }
        }
        else
        {
            m_includeFilter = null;
        }

        value = m_dirs.get(Constants.EXCLUDE_DIRECTIVE);
        if (value != null)
        {
            List<String> filters = ManifestParser.parseDelimitedString(value, ",");
            m_excludeFilter = new ArrayList<List<String>>(filters.size());
            for (int filterIdx = 0; filterIdx < filters.size(); filterIdx++)
            {
                List<String> substrings = SimpleFilter.parseSubstring(filters.get(filterIdx));
                m_excludeFilter.add(substrings);
            }
        }
        else
        {
            m_excludeFilter = null;
        }

        Set<String> mandatory = Collections.EMPTY_SET;
        value = m_dirs.get(Constants.MANDATORY_DIRECTIVE);
        if (value != null)
        {
            List<String> names = ManifestParser.parseDelimitedString(value, ",");
            mandatory = new HashSet<String>(names.size());
            for (String name : names)
            {
                // If attribute exists, then record it as mandatory.
                if (m_attrs.containsKey(name))
                {
                    mandatory.add(name);
                }
                // Otherwise, report an error.
                else
                {
                    throw new IllegalArgumentException(
                        "Mandatory attribute '" + name + "' does not exist.");
                }
            }
        }
        m_mandatory = mandatory;
    }

    public BundleRevision getResource()
    {
        return m_revision;
    }

    public BundleRevision getRevision()
    {
        return m_revision;
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

    public boolean isAttributeMandatory(String name)
    {
        return !m_mandatory.isEmpty() && m_mandatory.contains(name);
    }

    public List<String> getUses()
    {
        return m_uses;
    }

    public boolean isIncluded(String name)
    {
        if ((m_includeFilter == null) && (m_excludeFilter == null))
        {
            return true;
        }

        // Get the class name portion of the target class.
        String className = Util.getClassName(name);

        // If there are no include filters then all classes are included
        // by default, otherwise try to find one match.
        boolean included = (m_includeFilter == null);
        for (int i = 0;
            (!included) && (m_includeFilter != null) && (i < m_includeFilter.size());
            i++)
        {
            included = SimpleFilter.compareSubstring(m_includeFilter.get(i), className);
        }

        // If there are no exclude filters then no classes are excluded
        // by default, otherwise try to find one match.
        boolean excluded = false;
        for (int i = 0;
            (!excluded) && (m_excludeFilter != null) && (i < m_excludeFilter.size());
            i++)
        {
            excluded = SimpleFilter.compareSubstring(m_excludeFilter.get(i), className);
        }
        return included && !excluded;
    }

    @Override
    public String toString()
    {
        if (m_revision == null)
        {
            return m_attrs.toString();
        }
        return "[" + m_revision + "] " + m_namespace + "; " + m_attrs;
    }
}