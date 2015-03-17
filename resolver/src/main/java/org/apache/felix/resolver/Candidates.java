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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.felix.resolver.util.CopyOnWriteSet;
import org.apache.felix.resolver.util.CopyOnWriteList;
import org.apache.felix.resolver.util.OpenHashMap;
import org.apache.felix.resolver.util.OpenHashMapList;
import org.apache.felix.resolver.util.OpenHashMapSet;
import org.apache.felix.resolver.util.ShadowList;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.resource.Wiring;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.ResolveContext;

class Candidates
{
    public static final int MANDATORY = 0;
    public static final int OPTIONAL = 1;

    private final Set<Resource> m_mandatoryResources;
    // Maps a capability to requirements that match it.
    private final OpenHashMapSet<Capability, Requirement> m_dependentMap;
    // Maps a requirement to the capability it matches.
    private final OpenHashMapList<Requirement, Capability> m_candidateMap;
    // Maps a bundle revision to its associated wrapped revision; this only happens
    // when a revision being resolved has fragments to attach to it.
    private final Map<Resource, WrappedResource> m_allWrappedHosts;
    // Map used when populating candidates to hold intermediate and final results.
    private final Map<Resource, Object> m_populateResultCache;

    // Flag to signal if fragments are present in the candidate map.
    private boolean m_fragmentsPresent = false;

    private final Map<Resource, Boolean> m_validOnDemandResources;

    private final Map<Capability, Requirement> m_subtitutableMap;

    private final OpenHashMapSet<Requirement, Capability> m_path;

    /**
     * Private copy constructor used by the copy() method.
     */
    private Candidates(
        Set<Resource> mandatoryResources,
        OpenHashMapSet<Capability, Requirement> dependentMap,
        OpenHashMapList<Requirement, Capability> candidateMap,
        Map<Resource, WrappedResource> wrappedHosts, Map<Resource, Object> populateResultCache,
        boolean fragmentsPresent,
        Map<Resource, Boolean> onDemandResources,
        Map<Capability, Requirement> substitutableMap,
        OpenHashMapSet<Requirement, Capability> path)
    {
        m_mandatoryResources = mandatoryResources;
        m_dependentMap = dependentMap;
        m_candidateMap = candidateMap;
        m_allWrappedHosts = wrappedHosts;
        m_populateResultCache = populateResultCache;
        m_fragmentsPresent = fragmentsPresent;
        m_validOnDemandResources = onDemandResources;
        m_subtitutableMap = substitutableMap;
        m_path = path;
    }

    /**
     * Constructs an empty Candidates object.
     */
    public Candidates(Map<Resource, Boolean> validOnDemandResources)
    {
        m_mandatoryResources = new HashSet<Resource>();
        m_dependentMap = new OpenHashMapSet<Capability, Requirement>();
        m_candidateMap = new OpenHashMapList<Requirement, Capability>();
        m_allWrappedHosts = new HashMap<Resource, WrappedResource>();
        m_populateResultCache = new LinkedHashMap<Resource, Object>();
        m_validOnDemandResources = validOnDemandResources;
        m_subtitutableMap = new LinkedHashMap<Capability, Requirement>();
        m_path = new OpenHashMapSet<Requirement, Capability>(3);
    }

    public Object getPath() {
        return m_path;
    }

    /**
     * Populates candidates for the specified revision. How a revision is
     * resolved depends on its resolution type as follows:
     * <ul>
     * <li><tt>MANDATORY</tt> - must resolve and failure to do so throws an
     * exception.</li>
     * <li><tt>OPTIONAL</tt> - attempt to resolve, but no exception is thrown if
     * the resolve fails.</li>
     * <li><tt>ON_DEMAND</tt> - only resolve on demand; this only applies to
     * fragments and will only resolve a fragment if its host is already
     * selected as a candidate.</li>
     * </ul>
     *
     * @param rc the resolve context used for populating the candidates.
     * @param resource the resource whose candidates should be populated.
     * @param resolution indicates the resolution type.
     */
    public final void populate(
        ResolveContext rc, Resource resource, int resolution) throws ResolutionException
    {
        // Get the current result cache value, to make sure the revision
        // hasn't already been populated.
        Object cacheValue = m_populateResultCache.get(resource);
        // Has been unsuccessfully populated.
        if (cacheValue instanceof ResolutionException)
        {
            return;
        }
        // Has been successfully populated.
        else if (cacheValue instanceof Boolean)
        {
            return;
        }

        // We will always attempt to populate fragments, since this is necessary
        // for ondemand attaching of fragment. However, we'll only attempt to
        // populate optional non-fragment revisions if they aren't already
        // resolved.
        boolean isFragment = Util.isFragment(resource);
        if (!isFragment && rc.getWirings().containsKey(resource))
        {
            return;
        }

        if (resolution == MANDATORY)
        {
            m_mandatoryResources.add(resource);
        }
        try
        {
            // Try to populate candidates for the optional revision.
            populateResource(rc, resource);
        }
        catch (ResolutionException ex)
        {
            // Only throw an exception if resolution is mandatory.
            if (resolution == MANDATORY)
            {
                throw ex;
            }
        }
    }

    /**
     * Populates candidates for the specified revision.
     *
     * @param rc the resolver state used for populating the candidates.
     * @param resource the revision whose candidates should be populated.
     */
// TODO: FELIX3 - Modify to not be recursive.
    @SuppressWarnings("unchecked")
    private void populateResource(ResolveContext rc, Resource resource) throws ResolutionException
    {
        // Determine if we've already calculated this revision's candidates.
        // The result cache will have one of three values:
        //   1. A resolve exception if we've already attempted to populate the
        //      revision's candidates but were unsuccessful.
        //   2. Boolean.TRUE indicating we've already attempted to populate the
        //      revision's candidates and were successful.
        //   3. An array containing the cycle count, current map of candidates
        //      for already processed requirements, and a list of remaining
        //      requirements whose candidates still need to be calculated.
        // For case 1, rethrow the exception. For case 2, simply return immediately.
        // For case 3, this means we have a cycle so we should continue to populate
        // the candidates where we left off and not record any results globally
        // until we've popped completely out of the cycle.

        // Keeps track of the number of times we've reentered this method
        // for the current revision.
        Integer cycleCount = null;

        // Keeps track of the candidates we've already calculated for the
        // current revision's requirements.
        Map<Requirement, List<Capability>> localCandidateMap = null;

        // Keeps track of the current revision's requirements for which we
        // haven't yet found candidates.
        List<Requirement> remainingReqs = null;

        // Get the cache value for the current revision.
        Object cacheValue = m_populateResultCache.get(resource);

        // This is case 1.
        if (cacheValue instanceof ResolutionException)
        {
            throw (ResolutionException) cacheValue;
        }
        // This is case 2.
        else if (cacheValue instanceof Boolean)
        {
            return;
        }
        // This is case 3.
        else if (cacheValue != null)
        {
            // Increment and get the cycle count.
            cycleCount = (Integer) (((Object[]) cacheValue)[0] = (Integer) ((Object[]) cacheValue)[0] + 1);
            // Get the already populated candidates.
            localCandidateMap = (Map) ((Object[]) cacheValue)[1];
            // Get the remaining requirements.
            remainingReqs = (List) ((Object[]) cacheValue)[2];
        }

        // If there is no cache value for the current revision, then this is
        // the first time we are attempting to populate its candidates, so
        // do some one-time checks and initialization.
        if ((remainingReqs == null) && (localCandidateMap == null))
        {
            // Record cycle count.
            cycleCount = 0;

            // Create a local map for populating candidates first, just in case
            // the revision is not resolvable.
            localCandidateMap = new HashMap<Requirement, List<Capability>>();

            // Create a modifiable list of the revision's requirements.
            remainingReqs = new ArrayList<Requirement>(resource.getRequirements(null));

            // Add these value to the result cache so we know we are
            // in the middle of populating candidates for the current
            // revision.
            m_populateResultCache.put(resource,
                cacheValue = new Object[] { cycleCount, localCandidateMap, remainingReqs });
        }

        // If we have requirements remaining, then find candidates for them.
        while (!remainingReqs.isEmpty())
        {
            Requirement req = remainingReqs.remove(0);

            // Ignore non-effective and dynamic requirements.
            String resolution = req.getDirectives()
                .get(PackageNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE);
            if (!rc.isEffective(req)
                || ((resolution != null)
                && resolution.equals(PackageNamespace.RESOLUTION_DYNAMIC)))
            {
                continue;
            }

            // Process the candidates, removing any candidates that
            // cannot resolve.
            List<Capability> candidates = rc.findProviders(req);
            ResolutionException rethrow = processCandidates(rc, resource, candidates);

            // First, due to cycles, makes sure we haven't already failed in
            // a deeper recursion.
            Object result = m_populateResultCache.get(resource);
            if (result instanceof ResolutionException)
            {
                throw (ResolutionException) result;
            }
            // Next, if are no candidates remaining and the requirement is not
            // not optional, then record and throw a resolve exception.
            else if (candidates.isEmpty() && !Util.isOptional(req))
            {
                if (Util.isFragment(resource) && rc.getWirings().containsKey(resource))
                {
                    // This is a fragment that is already resolved and there is no unresolved hosts to attach it to.
                    m_populateResultCache.put(resource, Boolean.TRUE);
                    return;
                }
                String msg = "Unable to resolve " + resource
                    + ": missing requirement " + req;
                if (rethrow != null)
                {
                    msg = msg + " [caused by: " + rethrow.getMessage() + "]";
                }
                rethrow = new ResolutionException(msg, null, Collections.singleton(req));
                m_populateResultCache.put(resource, rethrow);
                throw rethrow;
            }
            // Otherwise, if we actually have candidates for the requirement, then
            // add them to the local candidate map.
            else if (candidates.size() > 0)
            {
                localCandidateMap.put(req, candidates);
            }
        }

        // If we are exiting from a cycle then decrement
        // cycle counter, otherwise record the result.
        if (cycleCount > 0)
        {
            ((Object[]) cacheValue)[0] = cycleCount - 1;
        }
        else if (cycleCount == 0)
        {
            // Record that the revision was successfully populated.
            m_populateResultCache.put(resource, Boolean.TRUE);
            // FELIX-4825: Verify candidate map in case of cycles and optional requirements
            for (Iterator<Map.Entry<Requirement, List<Capability>>> it = localCandidateMap.entrySet().iterator(); it.hasNext();)
            {
                Map.Entry<Requirement, List<Capability>> entry = it.next();
                for (Iterator<Capability> it2 = entry.getValue().iterator(); it2.hasNext();)
                {
                    if (m_populateResultCache.get(it2.next().getResource()) instanceof ResolutionException)
                    {
                        it2.remove();
                    }
                }
                if (entry.getValue().isEmpty())
                {
                    it.remove();
                }
            }
            // Merge local candidate map into global candidate map.
            if (localCandidateMap.size() > 0)
            {
                add(localCandidateMap);
            }
            if ((rc instanceof FelixResolveContext) && !Util.isFragment(resource))
            {
                Collection<Resource> ondemandFragments = ((FelixResolveContext) rc).getOndemandResources(resource);
                for (Resource fragment : ondemandFragments)
                {
                    Boolean valid = m_validOnDemandResources.get(fragment);
                    if (valid == null)
                    {
                        // Mark this resource as a valid on demand resource
                        m_validOnDemandResources.put(fragment, Boolean.TRUE);
                        valid = Boolean.TRUE;
                    }
                    if (valid)
                    {
                        // This resource is a valid on demand resource;
                        // populate it now, consider it optional
                        populate(rc, fragment, OPTIONAL);
                    }
                }
            }
        }
    }

    private void populateSubstitutables()
    {
        for (Map.Entry<Resource, Object> populated : m_populateResultCache.entrySet())
        {
            if (populated.getValue() instanceof Boolean)
            {
                populateSubstitutables(populated.getKey());
            }
        }
    }

    private void populateSubstitutables(Resource resource)
    {
        // Collect the package names exported
        List<Capability> packageExports = resource.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
        if (packageExports.isEmpty())
        {
            return;
        }
        List<Requirement> packageImports = resource.getRequirements(PackageNamespace.PACKAGE_NAMESPACE);
        if (packageImports.isEmpty())
        {
            return;
        }
        Map<String, List<Capability>> exportNames = new LinkedHashMap<String, List<Capability>>();
        for (Capability packageExport : packageExports)
        {
            String packageName = (String) packageExport.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
            List<Capability> caps = exportNames.get(packageName);
            if (caps == null)
            {
                caps = new ArrayList<Capability>(1);
                exportNames.put(packageName, caps);
            }
            caps.add(packageExport);
        }
        // Check if any requirements substitute one of the exported packages
        for (Requirement req : packageImports)
        {
            List<Capability> substitutes = m_candidateMap.get(req);
            if (substitutes != null && !substitutes.isEmpty())
            {
                String packageName = (String) substitutes.iterator().next().getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
                List<Capability> exportedPackages = exportNames.get(packageName);
                if (exportedPackages != null)
                {
                    // The package is exported;
                    // Check if the requirement only has the bundle's own export as candidates
                    substitutes = new ArrayList<Capability>(substitutes);
                    for (Capability exportedPackage : exportedPackages)
                    {
                        substitutes.remove(exportedPackage);
                    }
                    if (!substitutes.isEmpty())
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

    void checkSubstitutes(List<Candidates> importPermutations) throws ResolutionException
    {
        Map<Capability, Integer> substituteStatuses = new LinkedHashMap<Capability, Integer>(m_subtitutableMap.size());
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
        for (Map.Entry<Capability, Integer> substituteStatus : substituteStatuses.entrySet())
        {
            if (substituteStatus.getValue() == SUBSTITUTED)
            {
                if (m_dependentMap.isEmpty())
                {
                    // make sure the dependents are populated
                    populateDependents();
                }
            }
            // add a permutation that imports a different candidate for the substituted if possible
            Requirement substitutedReq = m_subtitutableMap.get(substituteStatus.getKey());
            if (substitutedReq != null)
            {
                permutateIfNeeded(substitutedReq, importPermutations);
            }
            Set<Requirement> dependents = m_dependentMap.get(substituteStatus.getKey());
            if (dependents != null)
            {
                for (Requirement dependent : dependents)
                {
                    List<Capability> candidates = m_candidateMap.get(dependent);
                    if (candidates != null)
                    {
                        candidates:
                        for (Iterator<Capability> iCandidates = candidates.iterator(); iCandidates.hasNext();)
                        {
                            Capability candidate = iCandidates.next();
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
                                    iCandidates.remove();
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
                                String msg = "Unable to resolve " + dependent.getResource()
                                        + ": missing requirement " + dependent;
                                throw new ResolutionException(msg, null, Collections.singleton(dependent));
                            }
                        }
                    }
                }
            }
        }
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
        List<Capability> substitutes = m_candidateMap.get(substitutableReq);
        if (substitutes != null)
        {
            for (Capability substituteCandidate : substitutes)
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

    public void populateDynamic(
        ResolveContext rc, Resource resource,
        Requirement req, List<Capability> candidates) throws ResolutionException
    {
        // Record the revision associated with the dynamic require
        // as a mandatory revision.
        m_mandatoryResources.add(resource);

        // Add the dynamic imports candidates.
        add(req, candidates);

        // Process the candidates, removing any candidates that
        // cannot resolve.
        ResolutionException rethrow = processCandidates(rc, resource, candidates);

        if (candidates.isEmpty())
        {
            if (rethrow == null)
            {
                rethrow = new ResolutionException(
                    "Dynamic import failed.", null, Collections.singleton(req));
            }
            throw rethrow;
        }

        m_populateResultCache.put(resource, Boolean.TRUE);
    }

    /**
     * This method performs common processing on the given set of candidates.
     * Specifically, it removes any candidates which cannot resolve and it
     * synthesizes candidates for any candidates coming from any attached
     * fragments, since fragment capabilities only appear once, but technically
     * each host represents a unique capability.
     *
     * @param rc the resolver state.
     * @param resource the revision being resolved.
     * @param candidates the candidates to process.
     * @return a resolve exception to be re-thrown, if any, or null.
     */
    private ResolutionException processCandidates(
        ResolveContext rc,
        Resource resource,
        List<Capability> candidates)
    {
        // Get satisfying candidates and populate their candidates if necessary.
        ResolutionException rethrow = null;
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
                && !candCap.getResource().equals(resource))
            {
                try
                {
                    populateResource(rc, candCap.getResource());
                }
                catch (ResolutionException ex)
                {
                    if (rethrow == null)
                    {
                        rethrow = ex;
                    }
                    // Remove the candidate since we weren't able to
                    // populate its candidates.
                    itCandCap.remove();
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
        Object value = m_populateResultCache.get(resource);
        return ((value != null) && (value instanceof Boolean));
    }

    public ResolutionException getResolveException(Resource resource)
    {
        Object value = m_populateResultCache.get(resource);
        return ((value != null) && (value instanceof ResolutionException))
            ? (ResolutionException) value : null;
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
    private void add(Requirement req, List<Capability> candidates)
    {
        if (req.getNamespace().equals(HostNamespace.HOST_NAMESPACE))
        {
            m_fragmentsPresent = true;
        }

        // Record the candidates.
        m_candidateMap.put(req, new CopyOnWriteList<Capability>(candidates));
    }

    /**
     * Adds requirements and candidates in bulk. The outer map is not retained
     * by this method, but the inner data structures are, so they should not be
     * further modified by the caller.
     *
     * @param candidates the bulk requirements and candidates to add.
     */
    private void add(Map<Requirement, List<Capability>> candidates)
    {
        for (Entry<Requirement, List<Capability>> entry : candidates.entrySet())
        {
            add(entry.getKey(), entry.getValue());
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
        List<Capability> candidates = m_candidateMap.get(req);
        if (candidates != null)
        {
            return Collections.unmodifiableList(candidates);
        }
        return null;
    }

    public Capability getFirstCandidate(Requirement req)
    {
        List<Capability> candidates = m_candidateMap.get(req);
        if (candidates != null && !candidates.isEmpty())
        {
            return m_candidateMap.get(req).get(0);
        }
        return null;
    }

    public void removeFirstCandidate(Requirement req)
    {
        List<Capability> candidates = m_candidateMap.get(req);
        // Remove the conflicting candidate.
        Capability cap = candidates.remove(0);
        if (candidates.isEmpty())
        {
            m_candidateMap.remove(req);
        }
        // Update resolution path
        CopyOnWriteSet<Capability> capPath = m_path.get(req);
        if (capPath == null) {
            capPath = new CopyOnWriteSet<Capability>();
            m_path.put(req, capPath);
        }
        capPath.add(cap);
    }

    public List<Capability> clearCandidates(Requirement req, Collection<Capability> caps)
    {
        List<Capability> l = m_candidateMap.get(req);
        l.removeAll(caps);
        // Update resolution path
        CopyOnWriteSet<Capability> capPath = m_path.get(req);
        if (capPath == null) {
            capPath = new CopyOnWriteSet<Capability>();
            m_path.put(req, capPath);
        }
        capPath.addAll(caps);
        return l;
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
     * @throws org.osgi.service.resolver.ResolutionException if the removal of any unselected fragments
     * result in the root module being unable to resolve.
     */
    public void prepare(ResolveContext rc) throws ResolutionException
    {
        // Maps a host capability to a map containing its potential fragments;
        // the fragment map maps a fragment symbolic name to a map that maps
        // a version to a list of fragments requirements matching that symbolic
        // name and version.
        Map<Capability, Map<String, Map<Version, List<Requirement>>>> hostFragments = Collections.emptyMap();
        if (m_fragmentsPresent)
        {
            hostFragments = populateDependents();
        }

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
                            List<Capability> hosts = m_candidateMap.get(hostReq);
                            hosts.remove(hostCap);
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
            removeResource(fragment,
                new ResolutionException(
                    "Fragment was not selected for attachment: " + fragment));
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
                            List<Capability> cands = m_candidateMap.get(r);
                            if (!(cands instanceof ShadowList))
                            {
                                ShadowList<Capability> shadow = new ShadowList<Capability>(cands);
                                m_candidateMap.put(r, shadow);
                                cands = shadow;
                            }

                            // If the original capability is from a fragment, then
                            // ask the ResolveContext to insert it and update the
                            // shadow copy of the list accordingly.
                            if (!origCap.getResource().equals(hostResource.getDeclaredResource()))
                            {
                                List<Capability> original = ((ShadowList<Capability>) cands).getOriginal();
                                int removeIdx = original.indexOf(origCap);
                                if (removeIdx != -1)
                                {
                                    original.remove(removeIdx);
                                    cands.remove(removeIdx);
                                }
                                int insertIdx = rc.insertHostedCapability(
                                    original,
                                    new SimpleHostedCapability(
                                        hostResource.getDeclaredResource(),
                                        origCap));
                                cands.add(insertIdx, c);
                            }
                            // If the original capability is from the host, then
                            // we just need to replace it in the shadow list.
                            else
                            {
                                int idx = cands.indexOf(origCap);
                                cands.set(idx, c);
                            }
                        }
                    }
                }
            }

            // Copy candidates for fragment requirements to the host.
            for (Requirement r : hostResource.getRequirements(null))
            {
                Requirement origReq = ((WrappedRequirement) r).getDeclaredRequirement();
                List<Capability> cands = m_candidateMap.get(origReq);
                if (cands != null)
                {
                    m_candidateMap.put(r, new CopyOnWriteList<Capability>(cands));
                    for (Capability cand : cands)
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
        for (Resource resource : m_mandatoryResources)
        {
            if (!isPopulated(resource))
            {
                throw getResolveException(resource);
            }
        }

        populateSubstitutables();

        m_candidateMap.concat();
        m_dependentMap.concat();
    }

    // Maps a host capability to a map containing its potential fragments;
    // the fragment map maps a fragment symbolic name to a map that maps
    // a version to a list of fragments requirements matching that symbolic
    // name and version.
    private Map<Capability, Map<String, Map<Version, List<Requirement>>>> populateDependents()
    {
        Map<Capability, Map<String, Map<Version, List<Requirement>>>> hostFragments =
            new HashMap<Capability, Map<String, Map<Version, List<Requirement>>>>();
        for (Entry<Requirement, CopyOnWriteList<Capability>> entry : m_candidateMap.entrySet())
        {
            Requirement req = entry.getKey();
            List<Capability> caps = entry.getValue();
            for (Capability cap : caps)
            {
                // Record the requirement as dependent on the capability.
                CopyOnWriteSet<Requirement> dependents = m_dependentMap.get(cap);
                if (dependents == null)
                {
                    dependents = new CopyOnWriteSet<Requirement>();
                    m_dependentMap.put(cap, dependents);
                }
                dependents.add(req);

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
     * @throws ResolutionException if removing the module caused the resolve to
     * fail.
     */
    private void removeResource(Resource resource, ResolutionException ex)
        throws ResolutionException
    {
        // Add removal reason to result cache.
        m_populateResultCache.put(resource, ex);
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
     * @throws ResolutionException if removing the module caused the resolve to
     * fail.
     */
    private void remove(Resource resource, Set<Resource> unresolvedResources)
        throws ResolutionException
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
        List<Capability> candidates = m_candidateMap.remove(req);
        if (candidates != null)
        {
            for (Capability cap : candidates)
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
     * @throws ResolutionException if removing the module caused the resolve to
     * fail.
     */
    private void remove(Capability c, Set<Resource> unresolvedResources)
        throws ResolutionException
    {
        Set<Requirement> dependents = m_dependentMap.remove(c);
        if (dependents != null)
        {
            for (Requirement r : dependents)
            {
                List<Capability> candidates = m_candidateMap.get(r);
                candidates.remove(c);
                if (candidates.isEmpty())
                {
                    m_candidateMap.remove(r);
                    if (!Util.isOptional(r))
                    {
                        String msg = "Unable to resolve " + r.getResource()
                            + ": missing requirement " + r;
                        m_populateResultCache.put(
                            r.getResource(),
                            new ResolutionException(msg, null, Collections.singleton(r)));
                        unresolvedResources.add(r.getResource());
                    }
                }
            }
        }
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
                m_mandatoryResources,
                m_dependentMap.deepClone(),
                m_candidateMap.deepClone(),
                m_allWrappedHosts,
                m_populateResultCache,
                m_fragmentsPresent,
                m_validOnDemandResources,
                m_subtitutableMap,
                m_path.deepClone());
    }

    public void dump(ResolveContext rc)
    {
        // Create set of all revisions from requirements.
        Set<Resource> resources = new CopyOnWriteSet<Resource>();
        for (Entry<Requirement, CopyOnWriteList<Capability>> entry
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
                List<Capability> candidates = m_candidateMap.get(req);
                if ((candidates != null) && (candidates.size() > 0))
                {
                    System.out.println("    " + req + ": " + candidates);
                }
            }
            reqs = (wiring != null)
                ? Util.getDynamicRequirements(wiring.getResourceRequirements(null))
                : Util.getDynamicRequirements(resource.getRequirements(null));
            for (Requirement req : reqs)
            {
                List<Capability> candidates = m_candidateMap.get(req);
                if ((candidates != null) && (candidates.size() > 0))
                {
                    System.out.println("    " + req + ": " + candidates);
                }
            }
        }
        System.out.println("=== END CANDIDATE MAP ===");
    }

    public void permutate(Requirement req, List<Candidates> permutations)
    {
        if (!Util.isMultiple(req) && canRemoveCandidate(req))
        {
            Candidates perm = copy();
            perm.removeFirstCandidate(req);
            permutations.add(perm);
        }
    }

    public boolean canRemoveCandidate(Requirement req)
    {
        List<Capability> candidates = m_candidateMap.get(req);
        return ((candidates != null) && (candidates.size() > 1 || Util.isOptional(req)));
    }

    public void permutateIfNeeded(Requirement req, List<Candidates> permutations)
    {
        List<Capability> candidates = m_candidateMap.get(req);
        if ((candidates != null) && (candidates.size() > 1))
        {
            // Check existing permutations to make sure we haven't
            // already permutated this requirement. This check for
            // duplicate permutations is simplistic. It assumes if
            // there is any permutation that contains a different
            // initial candidate for the requirement in question,
            // then it has already been permutated.
            boolean permutated = false;
            for (Candidates existingPerm : permutations)
            {
                List<Capability> existingPermCands = existingPerm.m_candidateMap.get(req);
                if (existingPermCands != null && !existingPermCands.get(0).equals(candidates.get(0)))
                {
                    permutated = true;
                    break;
                }
            }
            // If we haven't already permutated the existing
            // import, do so now.
            if (!permutated)
            {
                permutate(req, permutations);
            }
        }
    }

}
