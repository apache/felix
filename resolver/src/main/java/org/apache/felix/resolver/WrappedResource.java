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

<<<<<<< HEAD
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
=======
import java.util.*;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.HostNamespace;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;

class WrappedResource implements Resource
{
    private final Resource m_host;
    private final List<Resource> m_fragments;
<<<<<<< HEAD
    private List<Capability> m_cachedCapabilities = null;
    private List<Requirement> m_cachedRequirements = null;
=======
    private final List<Capability> m_cachedCapabilities;
    private final List<Requirement> m_cachedRequirements;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    public WrappedResource(Resource host, List<Resource> fragments)
    {
        m_host = host;
        m_fragments = fragments;
<<<<<<< HEAD
=======
 
        // Wrap host capabilities.
        List<Capability> caps = new ArrayList<Capability>();
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
                    caps.add(new WrappedCapability(this,  cap));
                }
            }
        }
        m_cachedCapabilities = Collections.unmodifiableList(caps);

        // Wrap host requirements.
        List<Requirement> reqs = new ArrayList<Requirement>();
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
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
=======
        if (namespace != null) {
            List<Capability> filtered = new ArrayList<Capability>();
            for (Capability capability : m_cachedCapabilities) {
                if (namespace.equals(capability.getNamespace())) {
                    filtered.add(capability);
                }
            }
            return Collections.unmodifiableList(filtered);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        return m_cachedCapabilities;
    }

    public List<Requirement> getRequirements(String namespace)
    {
<<<<<<< HEAD
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
=======
        if (namespace != null) {
            List<Requirement> filtered = new ArrayList<Requirement>();
            for (Requirement requirement : m_cachedRequirements) {
                if (namespace.equals(requirement.getNamespace())) {
                    filtered.add(requirement);
                }
            }
            return Collections.unmodifiableList(filtered);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        return m_cachedRequirements;
    }

    public String toString()
    {
        return m_host.toString();
    }
}