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
package org.apache.felix.framework;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.felix.framework.StatefulResolver.ResolverHookRecord;
import org.apache.felix.framework.resolver.CandidateComparator;
import org.apache.felix.resolver.FelixResolveContext;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;

/**
 *
 * @author rickhall
 */
public class ResolveContextImpl extends ResolveContext implements FelixResolveContext
{
    private final StatefulResolver m_state;
    private final Map<Resource, Wiring> m_wirings;
    private final ResolverHookRecord m_resolverHookrecord;
    private final Collection<BundleRevision> m_mandatory;
    private final Collection<BundleRevision> m_optional;
    private final Collection<BundleRevision> m_ondemand;

    ResolveContextImpl(
        StatefulResolver state, Map<Resource, Wiring> wirings,
        ResolverHookRecord resolverHookRecord, Collection<BundleRevision> mandatory,
        Collection<BundleRevision> optional, Collection<BundleRevision> ondemand)
    {
        m_state = state;
        m_wirings = wirings;
        m_resolverHookrecord = resolverHookRecord;
        m_mandatory = mandatory;
        m_optional = optional;
        m_ondemand = ondemand;
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

    public Collection<Resource> getOndemandResources(Resource host)
    {
        return new ArrayList<Resource>(m_ondemand);
    }

    @Override
    public List<Capability> findProviders(Requirement br)
    {
        if (!(br instanceof BundleRequirement))
            throw new IllegalStateException("Expected a BundleRequirement");

        List<BundleCapability> result = m_state.findProvidersInternal(
            m_resolverHookrecord, br, true, true);

        // Casting the result to a List of Capability.
        // TODO Can we do this without the strang double-cast?
        @SuppressWarnings("unchecked")
        List<Capability> caps =
            (List<Capability>) (List<? extends Capability>) result;
        return caps;
    }

    @Override
    public int insertHostedCapability(List<Capability> caps, HostedCapability hc)
    {
        int idx = Collections.binarySearch(caps, hc, new CandidateComparator());
        if (idx < 0)
        {
            idx = Math.abs(idx + 1);
        }
        caps.add(idx, hc);
        return idx;
    }

    @Override
    public boolean isEffective(Requirement br)
    {
        return m_state.isEffective(br);
    }

    @Override
    public Map<Resource, Wiring> getWirings()
    {
        return m_wirings;
    }
}