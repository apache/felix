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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;

public class ResolveContextImpl extends ResolveContext
{
    private final Map<Resource, Wiring> m_wirings;
    private final Map<Requirement, List<Capability>> m_candMap;
    private final Collection<Resource> m_mandatory;
    private final Collection<Resource> m_optional;

    public ResolveContextImpl(
        Map<Resource, Wiring> wirings, Map<Requirement, List<Capability>> candMap,
        Collection<Resource> mandatory, Collection<Resource> optional)
    {
        m_wirings = wirings;
        m_candMap = candMap;
        m_mandatory = mandatory;
        m_optional = optional;
    }

    @Override
    public Collection<Resource> getMandatoryResources()
    {
        return new ArrayList<Resource>(m_mandatory);
    }

    @Override
    public Collection<Resource> getOptionalResources()
    {
        return new ArrayList<Resource>(m_optional);
    }

    @Override
    public List<Capability> findProviders(Requirement r)
    {
        List<Capability> cs = m_candMap.get(r);
        if (cs != null) {
            return new ArrayList<Capability>(cs);
        } else {
            return new ArrayList<Capability>();
        }
    }

    @Override
    public int insertHostedCapability(List<Capability> capabilities, HostedCapability hostedCapability)
    {
        int idx = 0;
        capabilities.add(idx, hostedCapability);
        return idx;
    }

    @Override
    public boolean isEffective(Requirement requirement)
    {
        return true;
    }

    @Override
    public Map<Resource, Wiring> getWirings()
    {
        return m_wirings;
    }
}