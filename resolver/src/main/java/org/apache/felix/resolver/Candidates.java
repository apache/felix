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

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.resolver.ResolverImpl.PermutationType;
import org.apache.felix.resolver.ResolverImpl.ResolveSession;
import org.apache.felix.resolver.util.*;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.*;
import org.osgi.resource.*;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;

class Candidates
{
    static class PopulateResult {
        boolean success;
        ResolutionError error;
        List<Requirement> remaining;
        Map<Requirement, List<Capability>> candidates;

        @Override
        public String toString() {
            return success ? "true" : error != null ? error.getMessage() : "???";
        }
    }

    private final ResolveSession m_session;
    // Maps a capability to requirements that match it.
    private final OpenHashMapSet<Capability, Requirement> m_dependentMap;
    // Maps a requirement to the capability it matches.
    private final OpenHashMapList m_candidateMap;
    // Maps a bundle revision to its associated wrapped revision; this only happens
    // when a revision being resolved has fragments to attach to it.
    private final Map<Resource, WrappedResource> m_allWrappedHosts;
    // Map used when populating candidates to hold intermediate and final results.
    private final OpenHashMap<Resource, PopulateResult> m_populateResultCache;

    private final Map<Capability, Requirement> m_subtitutableMap;

    private final OpenHashMapSet<Requirement, Capability> m_delta;
    private final AtomicBoolean m_candidateSelectorsUnmodifiable;

    /**
     * Private copy constructor used by the copy() method.
     */
    private Candidates(
        ResolveSession session,
        AtomicBoolean candidateSelectorsUnmodifiable,
        OpenHashMapSet<Capability, Requirement> dependentMap,
        OpenHashMapList candidateMap,
        Map<Resource, WrappedResource> wrappedHosts,
        OpenHashMap<Resource, PopulateResult> populateResultCache,
        Map<Capability, Requirement> substitutableMap,
        OpenHashMapSet<Requirement, Capability> delta)
    {
        m_session = session;
        m_candidateSelectorsUnmodifiable = candidateSelectorsUnmodifiable;
        m_dependentMap = dependentMap;
        m_candidateMap = candidateMap;
        m_allWrappedHosts = wrappedHosts;
        m_populateResultCache = populateResultCache;
        m_subtitutableMap = substitutableMap;
        m_delta = delta;
    }

    /**
     * Constructs an empty Candidates object.
     */
    public Candidates(ResolveSession session)
    {
        m_session = session;
        m_candidateSelectorsUnmodifiable = new AtomicBoolean(false);
        m_dependentMap = new OpenHashMapSet<Capability, Requirement>();
        m_candidateMap = new OpenHashMapList();
        m_allWrappedHosts = new HashMap<Resource, WrappedResource>();
        m_populateResultCache = new OpenHashMap<Resource, PopulateResult>();
        m_subtitutableMap = new OpenHashMap<Capability, Requirement>();
        m_delta = new OpenHashMapSet<Requirement, Capability>(3);
    }

    public int getNbResources()
    {
        return m_populateResultCache.size();
    }

    public Map<Resource, Resource> getRootHosts()
    {
        Map<Resource, Resource> hosts = new LinkedHashMap<Resource, Resource>();
        for (Resource res : m_session.getMandatoryResources())
        {
            addHost(res, hosts);
        }

        for (Resource res : m_session.getOptionalResources())
        {
            if (isPopulated(res)) {
                addHost(res, hosts);
            }
        }

        return hosts;
    }

    private void addHost(Resource res, Map<Resource, Resource> hosts) {
        if (res instanceof WrappedResource)
        {
            res = ((WrappedResource) res).getDeclaredResource();
        }
        if (!Util.isFragment(res))
        {
            hosts.put(res, getWrappedHost(res));
        } else {
            Requirement hostReq = res.getRequirements(HostNamespace.HOST_NAMESPACE).get(0);
            Capability hostCap = getFirstCandidate(hostReq);
            // If the resource is an already resolved fragment and can not
            // be attached to new hosts, there will be no matching host,
            // so ignore this resource
            if (hostCap != null) {
                res = getWrappedHost(hostCap.getResource());
                if (res instanceof WrappedResource) {
                    hosts.put(((WrappedResource) res).getDeclaredResource(), res);
                }
            }
        }
    }

    /**
     * Returns the delta which is the differences in the candidates from the
     * original Candidates permutation.
     * @return the delta
     */
    public Object getDelta()
    {
        return m_delta;
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    public void populate(Collection<Resource> resources)
    {
        ResolveContext rc = m_session.getContext();
        Set<Resource> toRemove = new HashSet<Resource>();
        LinkedList<Resource> toPopulate = new LinkedList<Resource>(resources);
        while (!toPopulate.isEmpty())
        {
            Resource resource = toPopulate.getFirst();
            // Get cached result
            PopulateResult result = m_populateResultCache.get(resource);
            if (result == null)
            {
                result = new PopulateResult();
                result.candidates = new OpenHashMap<Requirement, List<Capability>>();
                result.remaining = new ArrayList<Requirement>(resource.getRequirements(null));
                m_populateResultCache.put(resource, result);
            }
            if (result.success || result.error != null)
            {
                toPopulate.removeFirst();
                continue;
            }
            if (result.remaining.isEmpty())
            {
                toPopulate.removeFirst();
                result.success = true;
                addCandidates(result.candidates);
                result.candidates = null;
                result.remaining = null;
                if ((rc instanceof FelixResolveContext) && !Util.isFragment(resource))
                {
                    Collection<Resource> ondemandFragments = ((FelixResolveContext) rc).getOndemandResources(resource);
                    for (Resource fragment : ondemandFragments)
                    {
                        if (m_session.isValidOnDemandResource(fragment))
                        {
                            // This resource is a valid on demand resource;
                            // populate it now, consider it optional
                            toPopulate.addFirst(fragment);
                        }
                    }
                }
                continue;
            }
            // We have a requirement to process
            Requirement requirement = result.remaining.remove(0);
            if (!isEffective(requirement))
            {
                continue;
            }
            List<Capability> candidates = rc.findProviders(requirement);
            LinkedList<Resource> newToPopulate = new LinkedList<Resource>();
            ResolutionError thrown = processCandidates(newToPopulate, requirement, candidates);
             if (candidates.isEmpty() && !Util.isOptional(requirement))
            {
                if (Util.isFragment(resource) && rc.getWirings().containsKey(resource))
                {
                    // This is a fragment that is already resolved and there is no unresolved hosts to attach it to.
                    result.success = true;
                }
                else
                {
                    result.error = new MissingRequirementError(requirement, thrown);
                    toRemove.add(resource);
                }
                toPopulate.removeFirst();
            }
            else
            {
                if (!candidates.isEmpty())
                {
                    result.candidates.put(requirement, candidates);
                }
                if (!newToPopulate.isEmpty())
                {
                    toPopulate.addAll(0, newToPopulate);
                }
            }
        }

        while (!toRemove.isEmpty())
        {
            Iterator<Resource> iterator = toRemove.iterator();
            Resource resource = iterator.next();
            iterator.remove();
            remove(resource, toRemove);
        }
    }

    private boolean isEffective(Requirement req) {
        if (!m_session.getContext().isEffective(req)) {
            return false;
        }
        String res = req.getDirectives().get(PackageNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE);
        return !PackageNamespace.RESOLUTION_DYNAMIC.equals(res);
    }

    private boolean isMandatory(ResolveContext rc, Requirement requirement) {
        // The requirement is optional
        if (Util.isOptional(requirement)) {
            return false;
        }
        // This is a fragment that is already resolved and there is no unresolved hosts to attach it to
        Resource resource = requirement.getResource();
        if (Util.isFragment(resource) && rc.getWirings().containsKey(resource)) {
            return false;
        }
        return true;
    }

    private void populateSubstitutables()
    {
        for (Map.Entry<Resource, PopulateResult> populated : m_populateResultCache.fast())
        {
            if (populated.getValue().success)
            {
                populateSubstitutables(populated.getKey());
            }
        }
    }

    private void populateSubstitutables(Resource resource)
    {
        // Collect the package names exported
        OpenHashMap<String, List<Capability>> exportNames = new OpenHashMap<String, List<Capability>>() {
            @Override
            protected List<Capability> compute(String s) {
                return new ArrayList<Capability>(1);
            }
        };
        for (Capability packageExport : resource.getCapabilities(null))
        {
            if (!PackageNamespace.PACKAGE_NAMESPACE.equals(packageExport.getNamespace()))
            {
                continue;
            }
            String packageName = (String) packageExport.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
            List<Capability> caps = exportNames.getOrCompute(packageName);
            caps.add(packageExport);
        }
        if (exportNames.isEmpty())
        {
            return;
        }
        // Check if any requirements substitute one of the exported packages
        for (Requirement req : resource.getRequirements(null))
        {
            if (!PackageNamespace.PACKAGE_NAMESPACE.equals(req.getNamespace()))
            {
                continue;
            }
            CandidateSelector substitutes = m_candidateMap.get(req);
            if (substitutes != null && !substitutes.isEmpty())
            {
                String packageName = (String) substitutes.getCurrentCandidate().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
                List<Capability> exportedPackages = exportNames.get(packageName);
                if (exportedPackages != null)
                {
                    // The package is exported;
                    // Check if the requirement only has the bundle's own export as candidates
                    if (!exportedPackages.containsAll(substitutes.getRemainingCandidates()))
                    {
                        for (Capability exportedPackage : exportedPackages)
                        {
                            m_subtitutableMap.put(exportedPackage, req);
                        }
                    }
                }
            }
        }
    }

    private static final int UNPROCESSED = 0;
    private static final int PROCESSING = 1;
    private static final int SUBSTITUTED = 2;
    private static final int EXPORTED = 3;

    ResolutionError checkSubstitutes()
    {
        OpenHashMap<Capability, Integer> substituteStatuses = new OpenHashMap<Capability, Integer>(m_subtitutableMap.size());
        for (Capability substitutable : m_subtitutableMap.keySet())
        {
            // initialize with unprocessed
            substituteStatuses.put(substitutable, UNPROCESSED);
        }
        // note we are iterating over the original unmodified map by design
        for (Capability substitutable : m_subtitutableMap.keySet())
        {
            isSubstituted(substitutable, substituteStatuses);
        }

        // Remove any substituted exports from candidates
        for (Map.Entry<Capability, Integer> substituteStatus : substituteStatuses.fast())
        {
            // add a permutation that imports a different candidate for the substituted if possible
            Requirement substitutedReq = m_subtitutableMap.get(substituteStatus.getKey());
            if (substitutedReq != null)
            {
                m_session.permutateIfNeeded(PermutationType.SUBSTITUTE, substitutedReq, this);
            }
            Set<Requirement> dependents = m_dependentMap.get(substituteStatus.getKey());
            if (dependents != null)
            {
                for (Requirement dependent : dependents)
                {
                    CandidateSelector candidates = m_candidateMap.get(dependent);
                    if (candidates != null)
                    {
                        candidates:
                        while (!candidates.isEmpty())
                        {
                            Capability candidate = candidates.getCurrentCandidate();
                            Integer candidateStatus = substituteStatuses.get(candidate);
                            if (candidateStatus == null)
                            {
                                candidateStatus = EXPORTED;
                            }
                            switch (candidateStatus)
                            {
                                case EXPORTED:
                                    // non-substituted candidate hit before the substituted one; do not continue
                                    break candidates;
                                case SUBSTITUTED:
                                default:
                                    // Need to remove any substituted that comes before an exported candidate
                                    candidates.removeCurrentCandidate();
                                    // continue to next candidate
                                    break;
                            }
                        }
                        if (candidates.isEmpty())
                        {
                            if (Util.isOptional(dependent))
                            {
                                m_candidateMap.remove(dependent);
                            }
                            else
                            {
                                return new MissingRequirementError(dependent);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isSubstituted(Capability substitutableCap, Map<Capability, Integer> substituteStatuses)
    {
        Integer substituteState = substituteStatuses.get(substitutableCap);
        if (substituteState == null)
        {
            return false;
        }

        switch (substituteState)
        {
            case PROCESSING:
                // found a cycle mark the initiator as not substituted
                substituteStatuses.put(substitutableCap, EXPORTED);
                return false;
            case SUBSTITUTED:
                return true;
            case EXPORTED:
                return false;
            default:
                break;
        }

        Requirement substitutableReq = m_subtitutableMap.get(substitutableCap);
        if (substitutableReq == null)
        {
            // this should never happen.
            return false;
        }
        // mark as processing to detect cycles
        substituteStatuses.put(substitutableCap, PROCESSING);
        // discover possible substitutes
        CandidateSelector substitutes = m_candidateMap.get(substitutableReq);
        if (substitutes != null)
        {
            for (Capability substituteCandidate : substitutes.getRemainingCandidates())
            {
                if (substituteCandidate.getResource().equals(substitutableCap.getResource()))
                {
                    substituteStatuses.put(substitutableCap, EXPORTED);
                    return false;
                }
                if (!isSubstituted(substituteCandidate, substituteStatuses))
                {
                    // The resource's exported package is substituted for this permutation.
                    substituteStatuses.put(substitutableCap, SUBSTITUTED);
                    return true;
                }
            }
        }
        // if we get here then the export is not substituted
        substituteStatuses.put(substitutableCap, EXPORTED);
        return false;
    }

    public ResolutionError populateDynamic()
    {

        // Process the candidates, removing any candidates that
        // cannot resolve.
        // TODO: verify the two following statements
        LinkedList<Resource> toPopulate = new LinkedList<Resource>();
        ResolutionError rethrow = processCandidates(toPopulate, m_session.getDynamicRequirement(), m_session.getDynamicCandidates());

        // Add the dynamic imports candidates.
        // Make sure this is done after the call to processCandidates since we want to ensure
        // fragment candidates are properly hosted before adding the candidates list which makes a copy
        addCandidates(m_session.getDynamicRequirement(), m_session.getDynamicCandidates());

        populate(toPopulate);

        CandidateSelector caps = m_candidateMap.get(m_session.getDynamicRequirement());
        if (caps != null)
        {
            m_session.getDynamicCandidates().retainAll(caps.getRemainingCandidates());
        }
        else
        {
            m_session.getDynamicCandidates().clear();
        }

        if (m_session.getDynamicCandidates().isEmpty())
        {
            if (rethrow == null)
            {
                rethrow = new DynamicImportFailed(m_session.getDynamicRequirement());
            }
            return rethrow;
        }

        PopulateResult result = new PopulateResult();
        result.success = true;
        m_populateResultCache.put(m_session.getDynamicHost(), result);
        return null;
    }

    private ResolutionError processCandidates(
        LinkedList<Resource> toPopulate,
        Requirement req,
        List<Capability> candidates)
    {
        ResolveContext rc = m_session.getContext();
        // Get satisfying candidates and populate their candidates if necessary.
        ResolutionError rethrow = null;
        Set<Capability> fragmentCands = null;
        for (Iterator<Capability> itCandCap = candidates.iterator();
            itCandCap.hasNext();)
        {
            Capability candCap = itCandCap.next();

            boolean isFragment = Util.isFragment(candCap.getResource());

            // If the capability is from a fragment, then record it
            // because we have to insert associated host capabilities
            // if the fragment is already attached to any hosts.
            if (isFragment)
            {
                if (fragmentCands == null)
                {
                    fragmentCands = new HashSet<Capability>();
                }
                fragmentCands.add(candCap);
            }

            // Do a sanity check incase the resolve context tries to attach
            // a fragment to an already resolved host capability
            if (HostNamespace.HOST_NAMESPACE.equals(req.getNamespace())) {
                if (rc.getWirings().containsKey(candCap.getResource())) {
                    itCandCap.remove();
                    continue;
                }
            }
            // If the candidate revision is a fragment, then always attempt
            // to populate candidates for its dependency, since it must be
            // attached to a host to be used. Otherwise, if the candidate
            // revision is not already resolved and is not the current version
            // we are trying to populate, then populate the candidates for
            // its dependencies as well.
            // NOTE: Technically, we don't have to check to see if the
            // candidate revision is equal to the current revision, but this
            // saves us from recursing and also simplifies exceptions messages
            // since we effectively chain exception messages for each level
            // of recursion; thus, any avoided recursion results in fewer
            // exceptions to chain when an error does occur.
            if ((isFragment || !rc.getWirings().containsKey(candCap.getResource()))
                && !candCap.getResource().equals(req.getResource()))
            {
                PopulateResult result = m_populateResultCache.get(candCap.getResource());
                if (result != null)
                {
                    if (result.error != null)
                    {
                        if (rethrow == null)
                        {
                            rethrow = result.error;
                        }
                        // Remove the candidate since we weren't able to
                        // populate its candidates.
                        itCandCap.remove();
                    }
                    else if (!result.success)
                    {
                        toPopulate.add(candCap.getResource());
                    }
                }
                else
                {
                    toPopulate.add(candCap.getResource());
                }
            }
        }

        // If any of the candidates for the requirement were from a fragment,
        // then also insert synthesized hosted capabilities for any other host
        // to which the fragment is attached since they are all effectively
        // unique capabilities.
        if (fragmentCands != null)
        {
            for (Capability fragCand : fragmentCands)
            {
                String fragCandName = fragCand.getNamespace();
                if (IdentityNamespace.IDENTITY_NAMESPACE.equals(fragCandName))
                {
                    // no need to wrap identity namespace ever
                    continue;
                }
                // Only necessary for resolved fragments.
                Wiring wiring = rc.getWirings().get(fragCand.getResource());
                if (wiring != null)
                {
                    // Fragments only have host wire, so each wire represents
                    // an attached host.
                    for (Wire wire : wiring.getRequiredResourceWires(HostNamespace.HOST_NAMESPACE))
                    {
                        // If the capability is a package, then make sure the
                        // host actually provides it in its resolved capabilities,
                        // since it may be a substitutable export.
                        if (!fragCandName.equals(PackageNamespace.PACKAGE_NAMESPACE)
                            || rc.getWirings().get(wire.getProvider())
                            .getResourceCapabilities(null).contains(fragCand))
                        {
                            // Note that we can just add this as a candidate
                            // directly, since we know it is already resolved.
                            // NOTE: We are synthesizing a hosted capability here,
                            // but we are not using a ShadowList like we do when
                            // we synthesizing capabilities for unresolved hosts.
                            // It is not necessary to use the ShadowList here since
                            // the host is resolved, because in that case we can
                            // calculate the proper package space by traversing
                            // the wiring. In the unresolved case, this isn't possible
                            // so we need to use the ShadowList so we can keep
                            // a reference to a synthesized resource with attached
                            // fragments so we can correctly calculate its package
                            // space.
                            // Must remove the fragment candidate because we must
                            // only use hosted capabilities for package namespace
                            candidates.remove(fragCand);
                            rc.insertHostedCapability(
                                candidates,
                                new WrappedCapability(
                                    wire.getCapability().getResource(),
                                    fragCand));
                        }
                    }
                }
            }
        }

        return rethrow;
    }

    public boolean isPopulated(Resource resource)
    {
        PopulateResult value = m_populateResultCache.get(resource);
        return (value != null && value.success);
    }

    public ResolutionError getResolutionError(Resource resource)
    {
        PopulateResult value = m_populateResultCache.get(resource);
        return value != null ? value.error : null;
    }

    /**
     * Adds a requirement and its matching candidates to the internal data
     * structure. This method assumes it owns the data being passed in and does
     * not make a copy. It takes the data and processes, such as calculating
     * which requirements depend on which capabilities and recording any
     * fragments it finds for future merging.
     *
     * @param req the requirement to add.
     * @param candidates the candidates matching the requirement.
     */
    private void addCandidates(Requirement req, List<Capability> candidates)
    {
        // Record the candidates.
        m_candidateMap.put(req, new CandidateSelector(candidates, m_candidateSelectorsUnmodifiable));
        for (Capability cap : candidates)
        {
            m_dependentMap.getOrCompute(cap).add(req);
        }
    }

    /**
     * Adds requirements and candidates in bulk. The outer map is not retained
     * by this method, but the inner data structures are, so they should not be
     * further modified by the caller.
     *
     * @param candidates the bulk requirements and candidates to add.
     */
    private void addCandidates(Map<Requirement, List<Capability>> candidates)
    {
        for (Map.Entry<Requirement, List<Capability>> entry : candidates.entrySet())
        {
            addCandidates(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Returns the wrapped resource associated with the given resource. If the
     * resource was not wrapped, then the resource itself is returned. This is
     * really only needed to determine if the root resources of the resolve have
     * been wrapped.
     *
     * @param r the resource whose wrapper is desired.
     * @return the wrapper resource or the resource itself if it was not
     * wrapped.
     */
    public Resource getWrappedHost(Resource r)
    {
        Resource wrapped = m_allWrappedHosts.get(r);
        return (wrapped == null) ? r : wrapped;
    }

    /**
     * Gets the candidates associated with a given requirement.
     *
     * @param req the requirement whose candidates are desired.
     * @return the matching candidates or null.
     */
    public List<Capability> getCandidates(Requirement req)
    {
        CandidateSelector candidates = m_candidateMap.get(req);
        if (candidates != null)
        {
            return candidates.getRemainingCandidates();
        }
        return null;
    }

    public Capability getFirstCandidate(Requirement req)
    {
        CandidateSelector candidates = m_candidateMap.get(req);
        if (candidates != null && !candidates.isEmpty())
        {
            return candidates.getCurrentCandidate();
        }
        return null;
    }

    public void removeFirstCandidate(Requirement req)
    {
        CandidateSelector candidates = m_candidateMap.get(req);
        // Remove the conflicting candidate.
        Capability cap = candidates.removeCurrentCandidate();
        if (candidates.isEmpty())
        {
            m_candidateMap.remove(req);
        }
        // Update the delta with the removed capability
        CopyOnWriteSet<Capability> capPath = m_delta.getOrCompute(req);
        capPath.add(cap);
    }

    public CandidateSelector clearMultipleCardinalityCandidates(Requirement req, Collection<Capability> caps)
    {
        // this is a special case where we need to completely replace the CandidateSelector
    	// this method should never be called from normal Candidates permutations
        CandidateSelector candidates = m_candidateMap.get(req);
        List<Capability> remaining = new ArrayList<Capability>(candidates.getRemainingCandidates());
        remaining.removeAll(caps);
        candidates = new CandidateSelector(remaining, m_candidateSelectorsUnmodifiable);
        m_candidateMap.put(req, candidates);
        return candidates;
    }

    /**
     * Merges fragments into their hosts. It does this by wrapping all host
     * modules and attaching their selected fragments, removing all unselected
     * fragment modules, and replacing all occurrences of the original fragments
     * in the internal data structures with the wrapped host modules instead.
     * Thus, fragment capabilities and requirements are merged into the
     * appropriate host and the candidates for the fragment now become
     * candidates for the host. Likewise, any module depending on a fragment now
     * depend on the host. Note that this process is sort of like
     * multiplication, since one fragment that can attach to two hosts
     * effectively gets multiplied across the two hosts. So, any modules being
     * satisfied by the fragment will end up having the two hosts as potential
     * candidates, rather than the single fragment.
     *
     * @return  ResolutionError if the removal of any unselected fragments
     * result in the root module being unable to resolve.
     */
    public ResolutionError prepare()
    {
        // Maps a host capability to a map containing its potential fragments;
        // the fragment map maps a fragment symbolic name to a map that maps
        // a version to a list of fragments requirements matching that symbolic
        // name and version.
        Map<Capability, Map<String, Map<Version, List<Requirement>>>> hostFragments =
            getHostFragments();

        // This method performs the following steps:
        // 1. Select the fragments to attach to a given host.
        // 2. Wrap hosts and attach fragments.
        // 3. Remove any unselected fragments. This is necessary because
        //    other revisions may depend on the capabilities of unselected
        //    fragments, so we need to remove the unselected fragments and
        //    any revisions that depends on them, which could ultimately cause
        //    the entire resolve to fail.
        // 4. Replace all fragments with any host it was merged into
        //    (effectively multiplying it).
        //    * This includes setting candidates for attached fragment
        //      requirements as well as replacing fragment capabilities
        //      with host's attached fragment capabilities.
        // Steps 1 and 2
        List<WrappedResource> hostResources = new ArrayList<WrappedResource>();
        List<Resource> unselectedFragments = new ArrayList<Resource>();
        for (Entry<Capability, Map<String, Map<Version, List<Requirement>>>> hostEntry : hostFragments.entrySet())
        {
            // Step 1
            Capability hostCap = hostEntry.getKey();
            Map<String, Map<Version, List<Requirement>>> fragments =
                hostEntry.getValue();
            List<Resource> selectedFragments = new ArrayList<Resource>();
            for (Entry<String, Map<Version, List<Requirement>>> fragEntry
                : fragments.entrySet())
            {
                boolean isFirst = true;
                for (Entry<Version, List<Requirement>> versionEntry
                    : fragEntry.getValue().entrySet())
                {
                    for (Requirement hostReq : versionEntry.getValue())
                    {
                        // Selecting the first fragment in each entry, which
                        // is equivalent to selecting the highest version of
                        // each fragment with a given symbolic name.
                        if (isFirst)
                        {
                            selectedFragments.add(hostReq.getResource());
                            isFirst = false;
                        }
                        // For any fragment that wasn't selected, remove the
                        // current host as a potential host for it and remove it
                        // as a dependent on the host. If there are no more
                        // potential hosts for the fragment, then mark it as
                        // unselected for later removal.
                        else
                        {
                            m_dependentMap.get(hostCap).remove(hostReq);
                            CandidateSelector hosts = removeCandidate(hostReq, hostCap);
                            if (hosts.isEmpty())
                            {
                                unselectedFragments.add(hostReq.getResource());
                            }
                        }
                    }
                }
            }

            // Step 2
            WrappedResource wrappedHost =
                new WrappedResource(hostCap.getResource(), selectedFragments);
            hostResources.add(wrappedHost);
            m_allWrappedHosts.put(hostCap.getResource(), wrappedHost);
        }

        // Step 3
        for (Resource fragment : unselectedFragments)
        {
            removeResource(fragment, new FragmentNotSelectedError(fragment));
        }

        // Step 4
        for (WrappedResource hostResource : hostResources)
        {
            // Replaces capabilities from fragments with the capabilities
            // from the merged host.
            for (Capability c : hostResource.getCapabilities(null))
            {
                // Don't replace the host capability, since the fragment will
                // really be attached to the original host, not the wrapper.
                if (!c.getNamespace().equals(HostNamespace.HOST_NAMESPACE))
                {
                    Capability origCap = ((HostedCapability) c).getDeclaredCapability();
                    // Note that you might think we could remove the original cap
                    // from the dependent map, but you can't since it may come from
                    // a fragment that is attached to multiple hosts, so each host
                    // will need to make their own copy.
                    CopyOnWriteSet<Requirement> dependents = m_dependentMap.get(origCap);
                    if (dependents != null)
                    {
                        dependents = new CopyOnWriteSet<Requirement>(dependents);
                        m_dependentMap.put(c, dependents);
                        for (Requirement r : dependents)
                        {
                            // We have synthesized hosted capabilities for all
                            // fragments that have been attached to hosts by
                            // wrapping the host bundle and their attached
                            // fragments. We need to use the ResolveContext to
                            // determine the proper priority order for hosted
                            // capabilities since the order may depend on the
                            // declaring host/fragment combination. However,
                            // internally we completely wrap the host revision
                            // and make all capabilities/requirements point back
                            // to the wrapped host not the declaring host. The
                            // ResolveContext expects HostedCapabilities to point
                            // to the declaring revision, so we need two separate
                            // candidate lists: one for the ResolveContext with
                            // HostedCapabilities pointing back to the declaring
                            // host and one for the resolver with HostedCapabilities
                            // pointing back to the wrapped host. We ask the
                            // ResolveContext to insert its appropriate HostedCapability
                            // into its list, then we mirror the insert into a
                            // shadow list with the resolver's HostedCapability.
                            // We only need to ask the ResolveContext to find
                            // the insert position for fragment caps since these
                            // were synthesized and we don't know their priority.
                            // However, in the resolver's candidate list we need
                            // to replace all caps with the wrapped caps, no
                            // matter if they come from the host or fragment,
                            // since we are completing replacing the declaring
                            // host and fragments with the wrapped host.
                            CandidateSelector cands = m_candidateMap.get(r);
                            ShadowList shadow;
                            if (!(cands instanceof ShadowList))
                            {
                                shadow = ShadowList.createShadowList(cands);
                                m_candidateMap.put(r, shadow);
                                cands = shadow;
                            }
                            else
                            {
                                shadow = (ShadowList) cands;
                            }

                            // If the original capability is from a fragment, then
                            // ask the ResolveContext to insert it and update the
                            // shadow copy of the list accordingly.
                            if (!origCap.getResource().equals(hostResource.getDeclaredResource()))
                            {
                                shadow.insertHostedCapability(
                                        m_session.getContext(),
                                        (HostedCapability) c,
                                        new SimpleHostedCapability(
                                                hostResource.getDeclaredResource(),
                                                origCap));
                            }
                            // If the original capability is from the host, then
                            // we just need to replace it in the shadow list.
                            else
                            {
                                shadow.replace(origCap, c);
                            }
                        }
                    }
                }
            }

            // Copy candidates for fragment requirements to the host.
            for (Requirement r : hostResource.getRequirements(null))
            {
                Requirement origReq = ((WrappedRequirement) r).getDeclaredRequirement();
                CandidateSelector cands = m_candidateMap.get(origReq);
                if (cands != null)
                {
                    m_candidateMap.put(r, cands.copy());
                    for (Capability cand : cands.getRemainingCandidates())
                    {
                        Set<Requirement> dependents = m_dependentMap.get(cand);
                        dependents.remove(origReq);
                        dependents.add(r);
                    }
                }
            }
        }

        // Lastly, verify that all mandatory revisions are still
        // populated, since some might have become unresolved after
        // selecting fragments/singletons.
        for (Resource resource : m_session.getMandatoryResources())
        {
            if (!isPopulated(resource))
            {
                return getResolutionError(resource);
            }
        }

        populateSubstitutables();

        m_candidateMap.trim();
        m_dependentMap.trim();

        // mark the selectors as unmodifiable now
        m_candidateSelectorsUnmodifiable.set(true);
        return null;
    }

    // Maps a host capability to a map containing its potential fragments;
    // the fragment map maps a fragment symbolic name to a map that maps
    // a version to a list of fragments requirements matching that symbolic
    // name and version.
    private Map<Capability, Map<String, Map<Version, List<Requirement>>>> getHostFragments()
    {
        Map<Capability, Map<String, Map<Version, List<Requirement>>>> hostFragments =
            new HashMap<Capability, Map<String, Map<Version, List<Requirement>>>>();
        for (Entry<Requirement, CandidateSelector> entry : m_candidateMap.fast())
        {
            Requirement req = entry.getKey();
            CandidateSelector caps = entry.getValue();
            for (Capability cap : caps.getRemainingCandidates())
            {
                // Keep track of hosts and associated fragments.
                if (req.getNamespace().equals(HostNamespace.HOST_NAMESPACE))
                {
                    String resSymName = Util.getSymbolicName(req.getResource());
                    Version resVersion = Util.getVersion(req.getResource());

                    Map<String, Map<Version, List<Requirement>>> fragments = hostFragments.get(cap);
                    if (fragments == null)
                    {
                        fragments = new HashMap<String, Map<Version, List<Requirement>>>();
                        hostFragments.put(cap, fragments);
                    }
                    Map<Version, List<Requirement>> fragmentVersions = fragments.get(resSymName);
                    if (fragmentVersions == null)
                    {
                        fragmentVersions =
                            new TreeMap<Version, List<Requirement>>(Collections.reverseOrder());
                        fragments.put(resSymName, fragmentVersions);
                    }
                    List<Requirement> actual = fragmentVersions.get(resVersion);
                    if (actual == null)
                    {
                        actual = new ArrayList<Requirement>();
                        if (resVersion == null)
                            resVersion = new Version(0, 0, 0);
                        fragmentVersions.put(resVersion, actual);
                    }
                    actual.add(req);
                }
            }
        }

        return hostFragments;
    }

    /**
     * Removes a module from the internal data structures if it wasn't selected
     * as a fragment or a singleton. This process may cause other modules to
     * become unresolved if they depended on the module's capabilities and there
     * is no other candidate.
     *
     * @param resource the module to remove.
     * @param ex the resolution error
     */
    private void removeResource(Resource resource, ResolutionError ex)
    {
        // Add removal reason to result cache.
        PopulateResult result = m_populateResultCache.get(resource);
        result.success = false;
        result.error = ex;
        // Remove from dependents.
        Set<Resource> unresolvedResources = new HashSet<Resource>();
        remove(resource, unresolvedResources);
        // Remove dependents that failed as a result of removing revision.
        while (!unresolvedResources.isEmpty())
        {
            Iterator<Resource> it = unresolvedResources.iterator();
            resource = it.next();
            it.remove();
            remove(resource, unresolvedResources);
        }
    }

    /**
     * Removes the specified module from the internal data structures, which
     * involves removing its requirements and its capabilities. This may cause
     * other modules to become unresolved as a result.
     *
     * @param resource the module to remove.
     * @param unresolvedResources a list to containing any additional modules
     * that that became unresolved as a result of removing this module and will
     * also need to be removed.
     */
    private void remove(Resource resource, Set<Resource> unresolvedResources)
    {
        for (Requirement r : resource.getRequirements(null))
        {
            remove(r);
        }

        for (Capability c : resource.getCapabilities(null))
        {
            remove(c, unresolvedResources);
        }
    }

    /**
     * Removes a requirement from the internal data structures.
     *
     * @param req the requirement to remove.
     */
    private void remove(Requirement req)
    {
        CandidateSelector candidates = m_candidateMap.remove(req);
        if (candidates != null)
        {
            for (Capability cap : candidates.getRemainingCandidates())
            {
                Set<Requirement> dependents = m_dependentMap.get(cap);
                if (dependents != null)
                {
                    dependents.remove(req);
                }
            }
        }
    }

    /**
     * Removes a capability from the internal data structures. This may cause
     * other modules to become unresolved as a result.
     *
     * @param c the capability to remove.
     * @param unresolvedResources a list to containing any additional modules
     * that that became unresolved as a result of removing this module and will
     * also need to be removed.
     */
    private void remove(Capability c, Set<Resource> unresolvedResources)
    {
        Set<Requirement> dependents = m_dependentMap.remove(c);
        if (dependents != null)
        {
            for (Requirement r : dependents)
            {
                CandidateSelector candidates = removeCandidate(r, c);
                if (candidates.isEmpty())
                {
                    m_candidateMap.remove(r);
                    if (!Util.isOptional(r))
                    {
                        PopulateResult result = m_populateResultCache.get(r.getResource());
                        if (result != null)
                        {
                            result.success = false;
                            result.error =
                                    new MissingRequirementError(r, m_populateResultCache.get(c.getResource()).error);
                        }
                        unresolvedResources.add(r.getResource());
                    }
                }
            }
        }
    }

    private CandidateSelector removeCandidate(Requirement req, Capability cap) {
        CandidateSelector candidates = m_candidateMap.get(req);
        candidates.remove(cap);
        return candidates;
    }

    /**
     * Creates a copy of the Candidates object. This is used for creating
     * permutations when package space conflicts are discovered.
     *
     * @return copy of this Candidates object.
     */
    public Candidates copy()
    {
        return new Candidates(
                m_session,
                m_candidateSelectorsUnmodifiable,
                m_dependentMap,
                m_candidateMap.deepClone(),
                m_allWrappedHosts,
                m_populateResultCache,
                m_subtitutableMap,
                m_delta.deepClone());
    }

    public void dump(ResolveContext rc)
    {
        // Create set of all revisions from requirements.
        Set<Resource> resources = new CopyOnWriteSet<Resource>();
        for (Entry<Requirement, CandidateSelector> entry
            : m_candidateMap.entrySet())
        {
            resources.add(entry.getKey().getResource());
        }
        // Now dump the revisions.
        System.out.println("=== BEGIN CANDIDATE MAP ===");
        for (Resource resource : resources)
        {
            Wiring wiring = rc.getWirings().get(resource);
            System.out.println("  " + resource
                + " (" + ((wiring != null) ? "RESOLVED)" : "UNRESOLVED)"));
            List<Requirement> reqs = (wiring != null)
                ? wiring.getResourceRequirements(null)
                : resource.getRequirements(null);
            for (Requirement req : reqs)
            {
                CandidateSelector candidates = m_candidateMap.get(req);
                if ((candidates != null) && (!candidates.isEmpty()))
                {
                    System.out.println("    " + req + ": " + candidates);
                }
            }
            reqs = (wiring != null)
                ? Util.getDynamicRequirements(wiring.getResourceRequirements(null))
                : Util.getDynamicRequirements(resource.getRequirements(null));
            for (Requirement req : reqs)
            {
                CandidateSelector candidates = m_candidateMap.get(req);
                if ((candidates != null) && (!candidates.isEmpty()))
                {
                    System.out.println("    " + req + ": " + candidates);
                }
            }
        }
        System.out.println("=== END CANDIDATE MAP ===");
    }

    public Candidates permutate(Requirement req)
    {
        if (!Util.isMultiple(req) && canRemoveCandidate(req))
        {
            Candidates perm = copy();
            perm.removeFirstCandidate(req);
            return perm;
        }
        return null;
    }

    public boolean canRemoveCandidate(Requirement req)
    {
        CandidateSelector candidates = m_candidateMap.get(req);
        return ((candidates != null) && (candidates.getRemainingCandidateCount() > 1 || Util.isOptional(req)));
    }

    static class DynamicImportFailed extends ResolutionError {

        private final Requirement requirement;

        public DynamicImportFailed(Requirement requirement) {
            this.requirement = requirement;
        }

        public String getMessage() {
            return "Dynamic import failed.";
        }

        public Collection<Requirement> getUnresolvedRequirements() {
            return Collections.singleton(requirement);
        }

    }

    static class FragmentNotSelectedError extends ResolutionError {

        private final Resource resource;

        public FragmentNotSelectedError(Resource resource) {
            this.resource = resource;
        }

        public String getMessage() {
            return "Fragment was not selected for attachment: " + resource;
        }

    }

    static class MissingRequirementError extends ResolutionError {

        private final Requirement requirement;
        private final ResolutionError cause;

        public MissingRequirementError(Requirement requirement) {
            this(requirement, null);
        }

        public MissingRequirementError(Requirement requirement, ResolutionError cause) {
            this.requirement = requirement;
            this.cause = cause;
        }

        public String getMessage() {
            String msg = "Unable to resolve " + requirement.getResource()
                    + ": missing requirement " + requirement;
            if (cause != null)
            {
                msg = msg + " [caused by: " + cause.getMessage() + "]";
            }
            return msg;
        }

        public Collection<Requirement> getUnresolvedRequirements() {
            return Collections.singleton(requirement);
        }

    }

}
