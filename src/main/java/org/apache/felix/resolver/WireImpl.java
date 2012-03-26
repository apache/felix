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
}