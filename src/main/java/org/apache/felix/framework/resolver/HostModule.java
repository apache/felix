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
package org.apache.felix.framework.resolver;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import org.apache.felix.framework.util.manifestparser.R4Library;
import org.apache.felix.framework.wiring.BundleCapabilityImpl;
import org.apache.felix.framework.wiring.BundleRequirementImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

class HostModule implements Module
{
    private final Module m_host;
    private final List<Module> m_fragments;
    private List<BundleCapabilityImpl> m_cachedCapabilities = null;
    private List<BundleRequirementImpl> m_cachedRequirements = null;

    public HostModule(Module module, List<Module> fragments)
    {
        m_host = module;
        m_fragments = fragments;
    }

    public Module getHost()
    {
        return m_host;
    }

    public List<Module> getFragments()
    {
        return m_fragments;
    }

    public String getId()
    {
        return m_host.getId();
    }

    public List<BundleCapabilityImpl> getDeclaredCapabilities()
    {
        if (m_cachedCapabilities == null)
        {
            List<BundleCapabilityImpl> caps = new ArrayList<BundleCapabilityImpl>();

            // Wrap host capabilities.
            for (BundleCapabilityImpl cap : m_host.getDeclaredCapabilities())
            {
                caps.add(new HostedCapability(this, cap));
            }

            // Wrap fragment capabilities.
            if (m_fragments != null)
            {
                for (Module fragment : m_fragments)
                {
                    for (BundleCapabilityImpl cap : fragment.getDeclaredCapabilities())
                    {
                        if (cap.getNamespace().equals(BundleCapabilityImpl.PACKAGE_NAMESPACE))
                        {
                            caps.add(new HostedCapability(this, cap));
                        }
                    }
                }
            }
            m_cachedCapabilities = Collections.unmodifiableList(caps);
        }
        return m_cachedCapabilities;
    }

    public List<BundleCapabilityImpl> getResolvedCapabilities()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<BundleRequirementImpl> getDeclaredRequirements()
    {
        if (m_cachedRequirements == null)
        {
            List<BundleRequirementImpl> reqs = new ArrayList<BundleRequirementImpl>();

            // Wrap host requirements.
            for (BundleRequirementImpl req : m_host.getDeclaredRequirements())
            {
                reqs.add(new HostedRequirement(this, req));
            }

            // Wrap fragment requirements.
            if (m_fragments != null)
            {
                for (Module fragment : m_fragments)
                {
                    for (BundleRequirementImpl req : fragment.getDeclaredRequirements())
                    {
                        if (req.getNamespace().equals(BundleCapabilityImpl.PACKAGE_NAMESPACE)
                            || req.getNamespace().equals(BundleCapabilityImpl.MODULE_NAMESPACE))
                        {
                            reqs.add(new HostedRequirement(this, req));
                        }
                    }
                }
            }
            m_cachedRequirements = Collections.unmodifiableList(reqs);
        }
        return m_cachedRequirements;
    }

    public List<BundleRequirementImpl> getResolvedRequirements()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String toString()
    {
        return m_host.getId();
    }

    public Map getHeaders()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isExtension()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getSymbolicName()
    {
        return m_host.getSymbolicName();
    }

    public Version getVersion()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<BundleRequirementImpl> getDeclaredDynamicRequirements()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<BundleRequirementImpl> getResolvedDynamicRequirements()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public List<R4Library> getNativeLibraries()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getDeclaredActivationPolicy()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Bundle getBundle()
    {
        return m_host.getBundle();
    }

    public List<Wire> getWires()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isResolved()
    {
        return false;
    }

    public Object getSecurityContext()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isRemovalPending()
    {
        return m_host.isRemovalPending();
    }

    public Content getContent()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Class getClassByDelegation(String name) throws ClassNotFoundException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public URL getResourceByDelegation(String name)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Enumeration getResourcesByDelegation(String name)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public URL getEntry(String name)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean hasInputStream(int index, String urlPath) throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public InputStream getInputStream(int index, String urlPath) throws IOException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public URL getLocalURL(int index, String urlPath)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}