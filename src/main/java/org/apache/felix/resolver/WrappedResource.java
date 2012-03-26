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
package org.apache.felix.resolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

class WrappedResource implements Resource
{
    private final Resource m_host;
    private final List<Resource> m_fragments;
    private List<Capability> m_cachedCapabilities = null;
    private List<Requirement> m_cachedRequirements = null;

    public WrappedResource(Resource host, List<Resource> fragments)
    {
        m_host = host;
        m_fragments = fragments;
    }

    public Resource getDeclaredResource()
    {
        return m_host;
    }

    public List<Resource> getFragments()
    {
        return m_fragments;
    }

    public List<Capability> getCapabilities(String namespace)
    {
        if (m_cachedCapabilities == null)
        {
            List<Capability> caps = new ArrayList<Capability>();

            // Wrap host capabilities.
            for (Capability cap : m_host.getCapabilities(null))
            {
                caps.add(new WrappedCapability(this, cap));
            }

            // Wrap fragment capabilities.
            if (m_fragments != null)
            {
                for (Resource fragment : m_fragments)
                {
                    for (Capability cap : fragment.getCapabilities(null))
                    {
                        // Filter out identity capabilities, since they
                        // are not part of the fragment payload.
                        if (!cap.getNamespace()
                            .equals(IdentityNamespace.IDENTITY_NAMESPACE))
                        {
                            caps.add(new WrappedCapability(this,  cap));
                        }
                    }
                }
            }
            m_cachedCapabilities = Collections.unmodifiableList(caps);
        }
        return m_cachedCapabilities;
    }

    public List<Requirement> getRequirements(String namespace)
    {
        if (m_cachedRequirements == null)
        {
            List<Requirement> reqs = new ArrayList<Requirement>();

            // Wrap host requirements.
            for (Requirement req : m_host.getRequirements(null))
            {
                reqs.add(new WrappedRequirement(this, req));
            }

            // Wrap fragment requirements.
            if (m_fragments != null)
            {
                for (Resource fragment : m_fragments)
                {
                    for (Requirement req : fragment.getRequirements(null))
                    {
                        // Filter out host and execution environment requirements,
                        // since they are not part of the fragment payload.
                        if (!req.getNamespace().equals(HostNamespace.HOST_NAMESPACE)
                            && !req.getNamespace().equals(
                                ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE))
                        {
                            reqs.add(new WrappedRequirement(this, req));
                        }
                    }
                }
            }
            m_cachedRequirements = Collections.unmodifiableList(reqs);
        }
        return m_cachedRequirements;
    }

    public String toString()
    {
        return m_host.toString();
    }
}