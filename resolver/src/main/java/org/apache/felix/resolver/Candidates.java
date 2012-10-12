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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.HostNamespace;
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
    public static final int ON_DEMAND = 2;

    private final Set<Resource> m_mandatoryResources;
    // Maps a capability to requirements that match it.
    private final Map<Capability, Set<Requirement>> m_dependentMap;
    // Maps a requirement to the capability it matches.
    private final Map<Requirement, List<Capability>> m_candidateMap;
    // Maps a bundle revision to its associated wrapped revision; this only happens
    // when a revision being resolved has fragments to attach to it.
    private final Map<Resource, WrappedResource> m_allWrappedHosts;
    // Map used when populating candidates to hold intermediate and final results.
    private final Map<Resource, Object> m_populateResultCache;

    // Flag to signal if fragments are present in the candidate map.
    private boolean m_fragmentsPresent = false;

    /**
     * Private copy constructor used by the copy() method.
     * @param dependentMap the capability dependency map.
     * @param candidateMap the requirement candidate map.
     * @param hostFragments the fragment map.
     * @param wrappedHosts the wrapped hosts map.
    **/
    private Candidates(
        Set<Resource> mandatoryResources,
        Map<Capability, Set<Requirement>> dependentMap,
        Map<Requirement, List<Capability>> candidateMap,
        Map<Resource, WrappedResource> wrappedHosts, Map<Resource, Object> populateResultCache,
        boolean fragmentsPresent)
    {
        m_mandatoryResources = mandatoryResources;
        m_dependentMap = dependentMap;
        m_candidateMap = candidateMap;
        m_allWrappedHosts = wrappedHosts;
        m_populateResultCache = populateResultCache;
        m_fragmentsPresent = fragmentsPresent;
    }

    /**
     * Constructs an empty Candidates object.
    **/
    public Candidates()
    {
        m_mandatoryResources = new HashSet<Resource>();
        m_dependentMap = new HashMap<Capability, Set<Requirement>>();
        m_candidateMap = new HashMap<Requirement, List<Capability>>();
        m_allWrappedHosts = new HashMap<Resource, WrappedResource>();
        m_populateResultCache = new HashMap<Resource, Object>();
    }

    /**
     * Populates candidates for the specified revision. How a revision is
     * resolved depends on its resolution type as follows:
     * <ul>
     *   <li><tt>MANDATORY</tt> - must resolve and failure to do so throws
     *       an exception.</li>
     *   <li><tt>OPTIONAL</tt> - attempt to resolve, but no exception is thrown
     *       if the resolve fails.</li>
     *   <li><tt>ON_DEMAND</tt> - only resolve on demand; this only applies to
     *       fragments and will only resolve a fragment if its host is already
     *       selected as a candidate.</li>
     * </ul>
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

        // Always attempt to populate mandatory or optional revisions.
        // However, for on-demand fragments only populate if their host
        // is already populated.
        if ((resolution != ON_DEMAND)
            || (isFragment && populateFragmentOndemand(rc, resource)))
        {
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
    }

    /**
     * Populates candidates for the specified revision.
     * @param state the resolver state used for populating the candidates.
     * @param revision the revision whose candidates should be populated.
     */
// TODO: FELIX3 - Modify to not be recursive.
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
            cycleCount = (Integer)
                (((Object[]) cacheValue)[0]
                    = new Integer(((Integer) ((Object[]) cacheValue)[0]).intValue() + 1));
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
            cycleCount = new Integer(0);

            // Create a local map for populating candidates first, just in case
            // the revision is not resolvable.
            localCandidateMap = new HashMap();

            // Create a modifiable list of the revision's requirements.
            remainingReqs = new ArrayList(resource.getRequirements(null));

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
        if (cycleCount.intValue() > 0)
        {
            ((Object[]) cacheValue)[0] = new Integer(cycleCount.intValue() - 1);
        }
        else if (cycleCount.intValue() == 0)
        {
            // Record that the revision was successfully populated.
            m_populateResultCache.put(resource, Boolean.TRUE);

            // Merge local candidate map into global candidate map.
            if (localCandidateMap.size() > 0)
            {
                add(localCandidateMap);
            }
        }
    }

    private boolean populateFragmentOndemand(ResolveContext rc, Resource resource)
        throws ResolutionException
    {
        // Create a modifiable list of the revision's requirements.
        List<Requirement> remainingReqs =
            new ArrayList(resource.getRequirements(null));
        // Find the host requirement.
        Requirement hostReq = null;
        for (Iterator<Requirement> it = remainingReqs.iterator();
            it.hasNext(); )
        {
            Requirement r = it.next();
            if (r.getNamespace().equals(HostNamespace.HOST_NAMESPACE))
            {
                hostReq = r;
                it.remove();
                break;
            }
        }
        // Get candidates hosts and keep any that have been populated.
        List<Capability> hosts = rc.findProviders(hostReq);
        for (Iterator<Capability> it = hosts.iterator(); it.hasNext(); )
        {
            Capability host = it.next();
            if (!isPopulated(host.getResource()))
            {
                it.remove();
            }
        }
        // If there aren't any populated hosts, then we can just
        // return since this fragment isn't needed.
        if (hosts.isEmpty())
        {
            return false;
        }

        // If there are populated host candidates, then prepopulate
        // the result cache with the work we've done so far.
        // Record cycle count, but start at -1 since it will
        // be incremented again in populate().
        Integer cycleCount = new Integer(-1);
        // Create a local map for populating candidates first, just in case
        // the revision is not resolvable.
        Map<Requirement, List<Capability>> localCandidateMap =
            new HashMap<Requirement, List<Capability>>();
        // Add the discovered host candidates to the local candidate map.
        localCandidateMap.put(hostReq, hosts);
        // Add these value to the result cache so we know we are
        // in the middle of populating candidates for the current
        // revision.
        m_populateResultCache.put(resource,
            new Object[] { cycleCount, localCandidateMap, remainingReqs });
        return true;
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
     * @param state the resolver state.
     * @param revision the revision being resolved.
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
            itCandCap.hasNext(); )
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
                // Only necessary for resolved fragments.
                Wiring wiring = rc.getWirings().get(fragCand.getResource());
                if (wiring != null)
                {
                    // Fragments only have host wire, so each wire represents
                    // an attached host.
                    for (Wire wire : wiring.getRequiredResourceWires(null))
                    {
                        // If the capability is a package, then make sure the
                        // host actually provides it in its resolved capabilities,
                        // since it may be a substitutable export.
                        if (!fragCand.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE)
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
     * structure. This method assumes it owns the data being passed in and
     * does not make a copy. It takes the data and processes, such as calculating
     * which requirements depend on which capabilities and recording any fragments
     * it finds for future merging.
     * @param req the requirement to add.
     * @param candidates the candidates matching the requirement.
    **/
    private void add(Requirement req, List<Capability> candidates)
    {
        if (req.getNamespace().equals(HostNamespace.HOST_NAMESPACE))
        {
            m_fragmentsPresent = true;
        }

        // Record the candidates.
        m_candidateMap.put(req, candidates);
    }

    /**
     * Adds requirements and candidates in bulk. The outer map is not retained
     * by this method, but the inner data structures are, so they should not
     * be further modified by the caller.
     * @param candidates the bulk requirements and candidates to add.
    **/
    private void add(Map<Requirement, List<Capability>> candidates)
    {
        for (Entry<Requirement, List<Capability>> entry : candidates.entrySet())
        {
            add(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Returns the wrapped resource associated with the given resource. If the resource
     * was not wrapped, then the resource itself is returned. This is really only
     * needed to determine if the root resources of the resolve have been wrapped.
     * @param r the resource whose wrapper is desired.
     * @return the wrapper resource or the resource itself if it was not wrapped.
    **/
    public Resource getWrappedHost(Resource r)
    {
        Resource wrapped = m_allWrappedHosts.get(r);
        return (wrapped == null) ? r : wrapped;
    }

    /**
     * Gets the candidates associated with a given requirement.
     * @param req the requirement whose candidates are desired.
     * @return the matching candidates or null.
    **/
    public List<Capability> getCandidates(Requirement req)
    {
        return m_candidateMap.get(req);
    }

    /**
     * Merges fragments into their hosts. It does this by wrapping all host
     * modules and attaching their selected fragments, removing all unselected
     * fragment modules, and replacing all occurrences of the original fragments
     * in the internal data structures with the wrapped host modules instead.
     * Thus, fragment capabilities and requirements are merged into the appropriate
     * host and the candidates for the fragment now become candidates for the host.
     * Likewise, any module depending on a fragment now depend on the host. Note
     * that this process is sort of like multiplication, since one fragment that
     * can attach to two hosts effectively gets multiplied across the two hosts.
     * So, any modules being satisfied by the fragment will end up having the
     * two hosts as potential candidates, rather than the single fragment.
     * @throws ResolutionException if the removal of any unselected fragments result
     *         in the root module being unable to resolve.
    **/
    public void prepare(ResolveContext rc) throws ResolutionException
    {
        // Maps a host capability to a map containing its potential fragments;
        // the fragment map maps a fragment symbolic name to a map that maps
        // a version to a list of fragments requirements matching that symbolic
        // name and version.
        Map<Capability, Map<String, Map<Version, List<Requirement>>>>
            hostFragments = Collections.EMPTY_MAP;
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
        for (Entry<Capability, Map<String, Map<Version, List<Requirement>>>>
            hostEntry : hostFragments.entrySet())
        {
            // Step 1
            Capability hostCap = hostEntry.getKey();
            Map<String, Map<Version, List<Requirement>>> fragments
                = hostEntry.getValue();
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
                    Set<Requirement> dependents = m_dependentMap.get(origCap);
                    if (dependents != null)
                    {
                        dependents = new HashSet<Requirement>(dependents);
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
                                ShadowList<Capability> shadow =
                                    new ShadowList<Capability>(cands);
                                m_candidateMap.put(r, shadow);
                                cands = shadow;
                            }

                            // If the original capability is from a fragment, then
                            // ask the ResolveContext to insert it and update the
                            // shadow copy of the list accordingly.
                            if (!origCap.getResource().equals(hostResource.getDeclaredResource()))
                            {
                                List<Capability> original = ((ShadowList) cands).getOriginal();
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
                    m_candidateMap.put(r, new ArrayList<Capability>(cands));
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
    }

    // Maps a host capability to a map containing its potential fragments;
    // the fragment map maps a fragment symbolic name to a map that maps
    // a version to a list of fragments requirements matching that symbolic
    // name and version.
    private Map<Capability,
        Map<String, Map<Version, List<Requirement>>>> populateDependents()
    {
        Map<Capability, Map<String, Map<Version, List<Requirement>>>>
            hostFragments = new HashMap<Capability,
                Map<String, Map<Version, List<Requirement>>>>();
        for (Entry<Requirement, List<Capability>> entry : m_candidateMap.entrySet())
        {
            Requirement req = entry.getKey();
            List<Capability> caps = entry.getValue();
            for (Capability cap : caps)
            {
                // Record the requirement as dependent on the capability.
                Set<Requirement> dependents = m_dependentMap.get(cap);
                if (dependents == null)
                {
                    dependents = new HashSet<Requirement>();
                    m_dependentMap.put(cap, dependents);
                }
                dependents.add(req);

                // Keep track of hosts and associated fragments.
                if (req.getNamespace().equals(HostNamespace.HOST_NAMESPACE))
                {
                    String resSymName = Util.getSymbolicName(req.getResource());
                    Version resVersion = Util.getVersion(req.getResource());

                    Map<String, Map<Version, List<Requirement>>>
                        fragments = hostFragments.get(cap);
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
     * @param revision the module to remove.
     * @throws ResolveException if removing the module caused the resolve to fail.
    **/
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
     * @param br the module to remove.
     * @param unresolvedRevisions a list to containing any additional modules that
     *        that became unresolved as a result of removing this module and will
     *        also need to be removed.
     * @throws ResolveException if removing the module caused the resolve to fail.
    **/
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
     * @param req the requirement to remove.
    **/
    private void remove(Requirement req)
    {
        boolean isFragment = req.getNamespace().equals(HostNamespace.HOST_NAMESPACE);

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
     * @param c the capability to remove.
     * @param unresolvedRevisions a list to containing any additional modules that
     *        that became unresolved as a result of removing this module and will
     *        also need to be removed.
     * @throws ResolveException if removing the module caused the resolve to fail.
    **/
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
     * @return copy of this Candidates object.
    **/
    public Candidates copy()
    {
        Map<Capability, Set<Requirement>> dependentMap =
            new HashMap<Capability, Set<Requirement>>();
        for (Entry<Capability, Set<Requirement>> entry : m_dependentMap.entrySet())
        {
            Set<Requirement> dependents = new HashSet<Requirement>(entry.getValue());
            dependentMap.put(entry.getKey(), dependents);
        }

        Map<Requirement, List<Capability>> candidateMap =
            new HashMap<Requirement, List<Capability>>();
        for (Entry<Requirement, List<Capability>> entry
            : m_candidateMap.entrySet())
        {
            List<Capability> candidates =
                new ArrayList<Capability>(entry.getValue());
            candidateMap.put(entry.getKey(), candidates);
        }

        return new Candidates(
            m_mandatoryResources, dependentMap, candidateMap,
            m_allWrappedHosts, m_populateResultCache, m_fragmentsPresent);
    }

    public void dump(ResolveContext rc)
    {
        // Create set of all revisions from requirements.
        Set<Resource> resources = new HashSet<Resource>();
        for (Entry<Requirement, List<Capability>> entry
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
}