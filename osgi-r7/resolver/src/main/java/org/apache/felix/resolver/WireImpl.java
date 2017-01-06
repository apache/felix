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

import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;

class WireImpl implements Wire
{
    private final Resource m_requirer;
    private final Requirement m_req;
    private final Resource m_provider;
    private final Capability m_cap;

    public WireImpl(
        Resource requirer, Requirement req,
        Resource provider, Capability cap)
    {
        m_requirer = requirer;
        m_req = req;
        m_provider = provider;
        m_cap = cap;
    }

    public Resource getRequirer()
    {
        return m_requirer;
    }

    public Requirement getRequirement()
    {
        return m_req;
    }

    public Resource getProvider()
    {
        return m_provider;
    }

    public Capability getCapability()
    {
        return m_cap;
    }

    @Override
    public String toString()
    {
        return m_req
            + " -> "
            + "[" + m_provider + "]";
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (!(obj instanceof Wire))
        {
            return false;
        }
        final Wire other = (Wire) obj;
        if (this.m_requirer != other.getRequirer()
            && (this.m_requirer == null || !this.m_requirer.equals(other.getRequirer())))
        {
            return false;
        }
        if (this.m_req != other.getRequirement()
            && (this.m_req == null || !this.m_req.equals(other.getRequirement())))
        {
            return false;
        }
        if (this.m_provider != other.getProvider()
            && (this.m_provider == null || !this.m_provider.equals(other.getProvider())))
        {
            return false;
        }
        if (this.m_cap != other.getCapability()
            && (this.m_cap == null || !this.m_cap.equals(other.getCapability())))
        {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 29 * hash + (this.m_requirer != null ? this.m_requirer.hashCode() : 0);
        hash = 29 * hash + (this.m_req != null ? this.m_req.hashCode() : 0);
        hash = 29 * hash + (this.m_provider != null ? this.m_provider.hashCode() : 0);
        hash = 29 * hash + (this.m_cap != null ? this.m_cap.hashCode() : 0);
        return hash;
    }
}