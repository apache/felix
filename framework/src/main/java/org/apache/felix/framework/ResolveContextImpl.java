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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.framework.StatefulResolver.ResolverHookRecord;
import org.apache.felix.framework.resolver.CandidateComparator;
import org.apache.felix.resolver.FelixResolveContext;
import org.apache.felix.resolver.ResolverImpl;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.resource.Wiring;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;

/**
 * 
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
        // TODO Can we do this without the strange double-cast?
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

	@Override
	public Collection<Wire> getSubstitutionWires(Wiring wiring) {
		// TODO: this is calculating information that probably has been calculated 
		// already or at least could be calculated quicker taking into account the
		// current state. We need to revisit this.
		Set<String> exportNames = new HashSet<String>();
        for (Capability cap : wiring.getResource().getCapabilities(null))
        {
            if (PackageNamespace.PACKAGE_NAMESPACE.equals(cap.getNamespace()))
            {
                exportNames.add(
                    (String) cap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
            }
        }
        // Add fragment exports
        for (Wire wire : wiring.getProvidedResourceWires(null))
        {
            if (HostNamespace.HOST_NAMESPACE.equals(wire.getCapability().getNamespace()))
            {
                for (Capability cap : wire.getRequirement().getResource().getCapabilities(
                    null))
                {
                    if (PackageNamespace.PACKAGE_NAMESPACE.equals(cap.getNamespace()))
                    {
                        exportNames.add((String) cap.getAttributes().get(
                            PackageNamespace.PACKAGE_NAMESPACE));
                    }
                }
            }
        }
        Collection<Wire> substitutionWires = new ArrayList<Wire>();
        for (Wire wire : wiring.getRequiredResourceWires(null))
        {
            if (PackageNamespace.PACKAGE_NAMESPACE.equals(
                wire.getCapability().getNamespace()))
            {
                if (exportNames.contains(wire.getCapability().getAttributes().get(
                    PackageNamespace.PACKAGE_NAMESPACE)))
                {
                    substitutionWires.add(wire);
                }
            }
        }
        return substitutionWires;
	}
}