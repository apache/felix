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

import java.util.List;
import java.util.Map;
import org.apache.felix.framework.wiring.BundleCapabilityImpl;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRevision;

public class HostedCapability extends BundleCapabilityImpl
{
    private final BundleRevision m_host;
    private final BundleCapabilityImpl m_cap;

    public HostedCapability(BundleRevision host, BundleCapabilityImpl cap)
    {
        super(host, cap.getNamespace(), cap.getDirectives(), cap.getAttributes());
        m_host = host;
        m_cap = cap;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final HostedCapability other = (HostedCapability) obj;
        if (m_host != other.m_host && (m_host == null || !m_host.equals(other.m_host)))
        {
            return false;
        }
        if (m_cap != other.m_cap && (m_cap == null || !m_cap.equals(other.m_cap)))
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 37 * hash + (m_host != null ? m_host.hashCode() : 0);
        hash = 37 * hash + (m_cap != null ? m_cap.hashCode() : 0);
        return hash;
    }

    public BundleCapabilityImpl getOriginalCapability()
    {
        return m_cap;
    }

    @Override
    public BundleRevision getRevision()
    {
        return m_host;
    }

    @Override
    public String getNamespace()
    {
        return m_cap.getNamespace();
    }

    @Override
    public Map<String, String> getDirectives()
    {
        return m_cap.getDirectives();
    }

    @Override
    public Map<String, Object> getAttributes()
    {
        return m_cap.getAttributes();
    }

    @Override
    public List<String> getUses()
    {
        return m_cap.getUses();
    }

    @Override
    public String toString()
    {
        if (m_host == null)
        {
            return getAttributes().toString();
        }
        if (getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE))
        {
            return "[" + m_host + "] "
                + getNamespace()
                + "; "
                + getAttributes().get(BundleRevision.PACKAGE_NAMESPACE);
        }
        return "[" + m_host + "] " + getNamespace() + "; " + getAttributes();
    }
}