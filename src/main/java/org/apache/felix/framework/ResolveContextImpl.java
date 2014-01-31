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
import org.apache.felix.framework.resolver.HostedCapability;
import org.apache.felix.framework.resolver.ResolveContext;
import org.apache.felix.framework.resolver.ResolveException;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;

/**
 *
 * @author rickhall
 */
public class ResolveContextImpl extends ResolveContext
{
    private final StatefulResolver m_state;
    private final Map<BundleRevision, BundleWiring> m_wirings;
    private final ResolverHookRecord m_resolverHookrecord;
    private final Collection<BundleRevision> m_mandatory;
    private final Collection<BundleRevision> m_optional;
    private final Collection<BundleRevision> m_ondemand;

    ResolveContextImpl(
        StatefulResolver state, Map<BundleRevision, BundleWiring> wirings,
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
    public Collection<BundleRevision> getMandatoryRevisions()
    {
        return new ArrayList<BundleRevision>(m_mandatory);
    }

    @Override
    public Collection<BundleRevision> getOptionalRevisions()
    {
        return new ArrayList<BundleRevision>(m_optional);
    }

    public Collection<BundleRevision> getOndemandRevisions()
    {
        return new ArrayList<BundleRevision>(m_ondemand);
    }

    public List<BundleCapability> findProviders(BundleRequirement br, boolean obeyMandatory)
    {
        return m_state.findProvidersInternal(m_resolverHookrecord, br, obeyMandatory);
    }

    public int insertHostedCapability(List<BundleCapability> caps, HostedCapability hc)
    {
        int idx = Collections.binarySearch(caps, hc, new CandidateComparator());
        if (idx < 0)
        {
            idx = Math.abs(idx + 1);
        }
        caps.add(idx, hc);
        return idx;
    }

    public boolean isEffective(BundleRequirement br)
    {
        return m_state.isEffective(br);
    }

    public Map<BundleRevision, BundleWiring> getWirings()
    {
        return m_wirings;
    }

    public void checkNativeLibraries(BundleRevision rev) throws ResolveException
    {
        m_state.checkNativeLibraries(rev);
    }
}