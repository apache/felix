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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.felix.framework.BundleRevisionImpl;
import org.apache.felix.framework.BundleWiringImpl;
import org.apache.felix.framework.resolver.Resolver.ResolverState;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.wiring.BundleCapabilityImpl;
import org.apache.felix.framework.wiring.BundleRequirementImpl;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;

class Candidates
{
    private final BundleRevision m_root;

    // Set of all candidate bundle revisions.
    private final Set<BundleRevision> m_candidateRevisions;
    // Maps a capability to requirements that match it.
    private final Map<BundleCapability, Set<BundleRequirement>> m_dependentMap;
    // Maps a requirement to the capability it matches.
    private final Map<BundleRequirement, SortedSet<BundleCapability>> m_candidateMap;
    // Maps a host capability to a map containing its potential fragments;
    // the fragment map maps a fragment symbolic name to a map that maps
    // a version to a list of fragments requirements matching that symbolic
    // name and version.
    private final Map<BundleCapability,
        Map<String, Map<Version, List<BundleRequirement>>>> m_hostFragments;
    // Maps a bundle revision to its associated wrapped revision; this only happens
    // when a revision being resolved has fragments to attach to it.
    private final Map<BundleRevision, HostBundleRevision> m_allWrappedHosts;
    // Map used when populating candidates to hold intermediate and final results.
    private final Map<BundleRevision, Object> m_populateResultCache;

    // Flag to signal if fragments are present in the candidate map.
    private boolean m_fragmentsPresent = false;

    /**
     * Private copy constructor used by the copy() method.
     * @param root the root module for the resolve.
     * @param dependentMap the capability dependency map.
     * @param candidateMap the requirement candidate map.
     * @param hostFragments the fragment map.
     * @param wrappedHosts the wrapped hosts map.
    **/
    private Candidates(
        BundleRevision root,
        Set<BundleRevision> candidateRevisions,
        Map<BundleCapability, Set<BundleRequirement>> dependentMap,
        Map<BundleRequirement, SortedSet<BundleCapability>> candidateMap,
        Map<BundleCapability, Map<String, Map<Version, List<BundleRequirement>>>> hostFragments,
        Map<BundleRevision, HostBundleRevision> wrappedHosts, Map<BundleRevision, Object> populateResultCache,
        boolean fragmentsPresent)
    {
        m_root = root;
        m_candidateRevisions = candidateRevisions;
        m_dependentMap = dependentMap;
        m_candidateMap = candidateMap;
        m_hostFragments = hostFragments;
        m_allWrappedHosts = wrappedHosts;
        m_populateResultCache = populateResultCache;
        m_fragmentsPresent = fragmentsPresent;
    }

    /**
     * Constructs a new populated Candidates object for the specified root module.
     * @param state the resolver state used for populating the candidates.
     * @param root the root module for the resolve.
    **/
    public Candidates(ResolverState state, BundleRevision root)
    {
        m_root = root;
        m_candidateRevisions = new HashSet<BundleRevision>();
        m_dependentMap = new HashMap<BundleCapability, Set<BundleRequirement>>();
        m_candidateMap = new HashMap<BundleRequirement, SortedSet<BundleCapability>>();
        m_hostFragments =
            new HashMap<BundleCapability, Map<String, Map<Version, List<BundleRequirement>>>>();
        m_allWrappedHosts = new HashMap<BundleRevision, HostBundleRevision>();
        m_populateResultCache = new HashMap<BundleRevision, Object>();

        populate(state, m_root);
    }

    /**
     * Constructs a new populated Candidates object with the specified root module and
     * starting requirement and matching candidates. This constructor is used
     * when the root module is performing a dynamic import for the given
     * requirement and the given potential candidates.
     * @param state the resolver state used for populating the candidates.
     * @param root the module with a dynamic import to resolve.
     * @param req the requirement being resolved.
     * @param candidates the potential candidates matching the requirement.
    **/
    public Candidates(ResolverState state, BundleRevision root,
        BundleRequirement req, SortedSet<BundleCapability> candidates)
    {
        m_root = root;
        m_candidateRevisions = new HashSet<BundleRevision>();
        m_dependentMap = new HashMap<BundleCapability, Set<BundleRequirement>>();
        m_candidateMap = new HashMap<BundleRequirement, SortedSet<BundleCapability>>();
        m_hostFragments =
            new HashMap<BundleCapability, Map<String, Map<Version, List<BundleRequirement>>>>();
        m_allWrappedHosts = new HashMap<BundleRevision, HostBundleRevision>();
        m_populateResultCache = new HashMap<BundleRevision, Object>();

        add(req, candidates);

        populateDynamic(state, m_root);
    }

    /**
     * Populates additional candidates for the specified module.
     * @param state the resolver state used for populating the candidates.
     * @param revision the module whose candidates should be populated.
     */
// TODO: FELIX3 - Modify to not be recursive.
    public final void populate(ResolverState state, BundleRevision revision)
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
        Map<BundleRequirement, SortedSet<BundleCapability>> localCandidateMap = null;

        // Keeps track of the current revision's requirements for which we
        // haven't yet found candidates.
        List<BundleRequirement> remainingReqs = null;

        // Get the cache value for the current revision.
        Object cacheValue = m_populateResultCache.get(revision);

        // This is case 1.
        if (cacheValue instanceof ResolveException)
        {
            throw (ResolveException) cacheValue;
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
            // Verify that any required execution environment is satisfied.
            state.checkExecutionEnvironment(revision);

            // Verify that any native libraries match the current platform.
            state.checkNativeLibraries(revision);

            // Record cycle count.
            cycleCount = new Integer(0);

            // Create a local map for populating candidates first, just in case
            // the revision is not resolvable.
            localCandidateMap = new HashMap();

            // Create a modifiable list of the revision's requirements.
            remainingReqs = new ArrayList(revision.getDeclaredRequirements(null));

            // Add these value to the result cache so we know we are
            // in the middle of populating candidates for the current
            // revision.
            m_populateResultCache.put(revision,
                cacheValue = new Object[] { cycleCount, localCandidateMap, remainingReqs });
        }

        // If we have requirements remaining, then find candidates for them.
        while (remainingReqs.size() > 0)
        {
            BundleRequirement req = remainingReqs.remove(0);

            // Ignore dynamic requirements.
            String resolution = req.getDirectives().get(Constants.RESOLUTION_DIRECTIVE);
// TODO: OSGi R4.3 - Use proper "dynamic" constant.
            if ((resolution != null) && resolution.equals("dynamic"))
            {
                continue;
            }

            // Get satisfying candidates and populate their candidates if necessary.
            ResolveException rethrow = null;
            SortedSet<BundleCapability> candidates =
                state.getCandidates((BundleRequirementImpl) req, true);
            for (Iterator<BundleCapability> itCandCap = candidates.iterator();
                itCandCap.hasNext(); )
            {
                BundleCapability candCap = itCandCap.next();

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
                if (Util.isFragment(candCap.getRevision())
                    || ((candCap.getRevision().getWiring() == null)
                        && !candCap.getRevision().equals(revision)))
                {
                    try
                    {
                        populate(state, candCap.getRevision());
                    }
                    catch (ResolveException ex)
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

            // If there are no candidates for the current requirement
            // and it is not optional, then create, cache, and throw
            // a resolve exception.
            if (candidates.isEmpty() && !((BundleRequirementImpl) req).isOptional())
            {
                String msg = "Unable to resolve " + revision
                    + ": missing requirement " + req;
                if (rethrow != null)
                {
                    msg = msg + " [caused by: " + rethrow.getMessage() + "]";
                }
                rethrow = new ResolveException(msg, revision, req);
                m_populateResultCache.put(revision, rethrow);
                throw rethrow;
            }
            // If we actually have candidates for the requirement, then
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
            m_populateResultCache.put(revision, Boolean.TRUE);

            // Merge local candidate map into global candidate map.
            if (localCandidateMap.size() > 0)
            {
                add(localCandidateMap);
            }
        }
    }

    public final void populateOptional(ResolverState state, BundleRevision revision)
    {
        // We will always attempt to populate optional fragments, since this
        // is necessary for greedy resolving of fragment. Howevere, we'll only
        // attempt to populate optional non-fragment revisions if they aren't
        // already resolved.
        boolean isFragment = Util.isFragment(revision);
        if (!isFragment && (revision.getWiring() != null))
        {
            return;
        }

        try
        {
            // If the optional revision is a fragment, then we only want to populate
            // the fragment if it has a candidate host in the set of already populated
            // revisions. We do this to avoid unnecessary work in prepare(). If the
            // fragment has a host, we'll prepopulate the result cache here to avoid
            // having to do the host lookup again in populate().
            if (isFragment)
            {
                // Get the current result cache value, to make sure the revision
                // hasn't already been populated.
                Object cacheValue = m_populateResultCache.get(revision);
                if (cacheValue == null)
                {
                    // Create a modifiable list of the revision's requirements.
                    List<BundleRequirement> remainingReqs =
                        new ArrayList(revision.getDeclaredRequirements(null));

                    // Find the host requirement.
                    BundleRequirement hostReq = null;
                    for (Iterator<BundleRequirement> it = remainingReqs.iterator();
                        it.hasNext(); )
                    {
                        BundleRequirement r = it.next();
                        if (r.getNamespace().equals(BundleCapabilityImpl.HOST_NAMESPACE))
                        {
                            hostReq = r;
                            it.remove();
                            break;
                        }
                    }

                    // Get candidates hosts and keep any that have been populated.
                    SortedSet<BundleCapability> hosts =
                        state.getCandidates((BundleRequirementImpl) hostReq, false);
                    for (Iterator<BundleCapability> it = hosts.iterator(); it.hasNext(); )
                    {
                        BundleCapability host = it.next();
                        if (!isPopulated(host.getRevision()))
                        {
                            it.remove();
                        }
                    }

                    // If there aren't any populated hosts, then we can just
                    // return since this fragment isn't needed.
                    if (hosts.isEmpty())
                    {
                        return;
                    }

                    // If there are populates host candidates, then finish up
                    // some other checks and prepopulate the result cache with
                    // the work we've done so far.
                    
                    // Verify that any required execution environment is satisfied.
                    state.checkExecutionEnvironment(revision);

                    // Verify that any native libraries match the current platform.
                    state.checkNativeLibraries(revision);

                    // Record cycle count, but start at -1 since it will
                    // be incremented again in populate().
                    Integer cycleCount = new Integer(-1);

                    // Create a local map for populating candidates first, just in case
                    // the revision is not resolvable.
                    Map<BundleRequirement, SortedSet<BundleCapability>> localCandidateMap =
                        new HashMap<BundleRequirement, SortedSet<BundleCapability>>();

                    // Add the discovered host candidates to the local candidate map.
                    localCandidateMap.put(hostReq, hosts);

                    // Add these value to the result cache so we know we are
                    // in the middle of populating candidates for the current
                    // revision.
                    m_populateResultCache.put(revision,
                        new Object[] { cycleCount, localCandidateMap, remainingReqs });
                }
            }

            // Try to populate candidates for the optional revision.
            populate(state, revision);
        }
        catch (ResolveException ex)
        {
            // Ignore since the revision is optional.
        }
    }

    private boolean isPopulated(BundleRevision revision)
    {
        Object value = m_populateResultCache.get(revision);
        return ((value != null) && (value instanceof Boolean));
    }

    private void populateDynamic(ResolverState state, BundleRevision revision)
    {
        // There should be one entry in the candidate map, which are the
        // the candidates for the matching dynamic requirement. Get the
        // matching candidates and populate their candidates if necessary.
        ResolveException rethrow = null;
        Entry<BundleRequirement, SortedSet<BundleCapability>> entry =
            m_candidateMap.entrySet().iterator().next();
        BundleRequirement dynReq = entry.getKey();
        SortedSet<BundleCapability> candidates = entry.getValue();
        for (Iterator<BundleCapability> itCandCap = candidates.iterator();
            itCandCap.hasNext(); )
        {
            BundleCapability candCap = itCandCap.next();
            if (candCap.getRevision().getWiring() == null)
            {
                try
                {
                    populate(state, candCap.getRevision());
                }
                catch (ResolveException ex)
                {
                    if (rethrow == null)
                    {
                        rethrow = ex;
                    }
                    itCandCap.remove();
                }
            }
        }

        if (candidates.isEmpty())
        {
            if (rethrow == null)
            {
                rethrow = new ResolveException("Dynamic import failed.", revision, dynReq);
            }
            throw rethrow;
        }
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
    private void add(BundleRequirement req, SortedSet<BundleCapability> candidates)
    {
        if (req.getNamespace().equals(BundleCapabilityImpl.HOST_NAMESPACE))
        {
            m_fragmentsPresent = true;
        }

        // Record the candidates.
        m_candidateMap.put(req, candidates);

        // Make a list of all candidate revisions for determining singetons.
        // Add the requirement as a dependent on the candidates. Keep track
        // of fragments for hosts.
        for (BundleCapability cap : candidates)
        {
            // Remember the revision for all capabilities so we can
            // determine which ones are singletons.
            m_candidateRevisions.add(cap.getRevision());
        }
    }

    /**
     * Adds requirements and candidates in bulk. The outer map is not retained
     * by this method, but the inner data structures are, so they should not
     * be further modified by the caller.
     * @param candidates the bulk requirements and candidates to add.
    **/
    private void add(Map<BundleRequirement, SortedSet<BundleCapability>> candidates)
    {
        for (Entry<BundleRequirement, SortedSet<BundleCapability>> entry : candidates.entrySet())
        {
            add(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Returns the wrapped module associated with the given module. If the module
     * was not wrapped, then the module itself is returned. This is really only
     * needed to determine if the root modules of the resolve have been wrapped.
     * @param m the module whose wrapper is desired.
     * @return the wrapper module or the module itself if it was not wrapped.
    **/
    public BundleRevision getWrappedHost(BundleRevision m)
    {
        BundleRevision wrapped = m_allWrappedHosts.get(m);
        return (wrapped == null) ? m : wrapped;
    }

    /**
     * Gets the candidates associated with a given requirement.
     * @param req the requirement whose candidates are desired.
     * @return the matching candidates or null.
    **/
    public SortedSet<BundleCapability> getCandidates(BundleRequirement req)
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
     * @param existingSingletons existing resolved singletons.
     * @throws ResolveException if the removal of any unselected fragments result
     *         in the root module being unable to resolve.
    **/
    public void prepare(List<BundleRevision> existingSingletons)
    {
        boolean init = false;

        if (m_fragmentsPresent)
        {
            populateDependents();
            init = true;
        }

        final Map<String, BundleRevision> singletons = new HashMap<String, BundleRevision>();

        for (Iterator<BundleRevision> it = m_candidateRevisions.iterator(); it.hasNext(); )
        {
            BundleRevision br = it.next();
            if (isSingleton(br))
            {
                if (!init)
                {
                    populateDependents();
                    init = true;
                }

                // See if there is an existing singleton for the
                // revision's symbolic name.
                BundleRevision singleton = singletons.get(br.getSymbolicName());
                // If there is no existing singleton or this revision is
                // a resolved singleton or this revision has a higher version
                // and the existing singleton is not resolved, then select
                // this revision as the singleton.
                if ((singleton == null)
                    || (br.getWiring() != null)
                    || ((br.getVersion().compareTo(singleton.getVersion()) > 0)
                        && (singleton.getWiring() == null)))
                {
                    singletons.put(br.getSymbolicName(), br);
                    // Remove the singleton revision from the candidates
                    // if it wasn't selected.
                    if (singleton != null)
                    {
                        removeRevision(singleton);
                    }
                }
                else
                {
                    removeRevision(br);
                }
            }
        }

        // If the root is a singleton, then prefer it over any other singleton.
        if (isSingleton(m_root))
        {
            BundleRevision singleton = singletons.get(m_root.getSymbolicName());
            singletons.put(m_root.getSymbolicName(), m_root);
            if ((singleton != null) && !singleton.equals(m_root))
            {
                if (singleton.getWiring() != null)
                {
                    throw new ResolveException(
                        "Cannot resolve singleton "
                        + m_root
                        + " because "
                        + singleton
                        + " singleton is already resolved.",
                        m_root, null);
                }
                removeRevision(singleton);
            }
        }

        // Make sure selected singletons do not conflict with existing
        // singletons passed into this method.
        for (int i = 0; (existingSingletons != null) && (i < existingSingletons.size()); i++)
        {
            BundleRevision existing = existingSingletons.get(i);
            BundleRevision singleton = singletons.get(existing.getSymbolicName());
            if ((singleton != null) && (singleton != existing))
            {
                singletons.remove(singleton.getSymbolicName());
                removeRevision(singleton);
            }
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
        List<HostBundleRevision> hostRevisions = new ArrayList<HostBundleRevision>();
        List<BundleRevision> unselectedFragments = new ArrayList<BundleRevision>();
        for (Entry<BundleCapability, Map<String, Map<Version, List<BundleRequirement>>>>
            hostEntry : m_hostFragments.entrySet())
        {
            // Step 1
            BundleCapability hostCap = hostEntry.getKey();
            Map<String, Map<Version, List<BundleRequirement>>> fragments
                = hostEntry.getValue();
            List<BundleRevision> selectedFragments = new ArrayList<BundleRevision>();
            for (Entry<String, Map<Version, List<BundleRequirement>>> fragEntry
                : fragments.entrySet())
            {
                boolean isFirst = true;
                for (Entry<Version, List<BundleRequirement>> versionEntry
                    : fragEntry.getValue().entrySet())
                {
                    for (BundleRequirement hostReq : versionEntry.getValue())
                    {
                        // Select the highest version of the fragment that
                        // is not removal pending.
                        if (isFirst
                            && !((BundleRevisionImpl) hostReq.getRevision()).isRemovalPending())
                        {
                            selectedFragments.add(hostReq.getRevision());
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
                            SortedSet<BundleCapability> hosts = m_candidateMap.get(hostReq);
                            hosts.remove(hostCap);
                            if (hosts.isEmpty())
                            {
                                unselectedFragments.add(hostReq.getRevision());
                            }
                        }
                    }
                }
            }

            // Step 2
            HostBundleRevision wrappedHost =
                new HostBundleRevision(hostCap.getRevision(), selectedFragments);
            hostRevisions.add(wrappedHost);
            m_allWrappedHosts.put(hostCap.getRevision(), wrappedHost);
        }

        // Step 3
        for (BundleRevision br : unselectedFragments)
        {
            removeRevision(br);
        }

        // Step 4
        for (HostBundleRevision hostRevision : hostRevisions)
        {
            // Replaces capabilities from fragments with the capabilities
            // from the merged host.
            for (BundleCapability c : hostRevision.getDeclaredCapabilities(null))
            {
                Set<BundleRequirement> dependents =
                    m_dependentMap.get(((HostedCapability) c).getDeclaredCapability());
                if (dependents != null)
                {
                    for (BundleRequirement r : dependents)
                    {
                        Set<BundleCapability> cands = m_candidateMap.get(r);
                        cands.remove(((HostedCapability) c).getDeclaredCapability());
                        cands.add(c);
                    }
                }
            }

            // Copy candidates for fragment requirements to the host.
            // This doesn't record the reverse dependency, but that
            // information should not be needed at this point anymore.
            for (BundleRequirement r : hostRevision.getDeclaredRequirements(null))
            {
                SortedSet<BundleCapability> cands =
                    m_candidateMap.get(((HostedRequirement) r).getDeclaredRequirement());
                if (cands != null)
                {
                    m_candidateMap.put(r, new TreeSet<BundleCapability>(cands));
                }
            }
        }
    }

    private void populateDependents()
    {
        for (Entry<BundleRequirement, SortedSet<BundleCapability>> entry
            : m_candidateMap.entrySet())
        {
            BundleRequirement req = entry.getKey();
            SortedSet<BundleCapability> caps = entry.getValue();
            for (BundleCapability cap : caps)
            {
                // Record the requirement as dependent on the capability.
                Set<BundleRequirement> dependents = m_dependentMap.get(cap);
                if (dependents == null)
                {
                    dependents = new HashSet<BundleRequirement>();
                    m_dependentMap.put(cap, dependents);
                }
                dependents.add(req);

                // Keep track of hosts and associated fragments.
                if (req.getNamespace().equals(BundleCapabilityImpl.HOST_NAMESPACE))
                {
                    Map<String, Map<Version, List<BundleRequirement>>>
                        fragments = m_hostFragments.get(cap);
                    if (fragments == null)
                    {
                        fragments = new HashMap<String, Map<Version, List<BundleRequirement>>>();
                        m_hostFragments.put(cap, fragments);
                    }
                    Map<Version, List<BundleRequirement>> fragmentVersions =
                        fragments.get(req.getRevision().getSymbolicName());
                    if (fragmentVersions == null)
                    {
                        fragmentVersions =
                            new TreeMap<Version, List<BundleRequirement>>(Collections.reverseOrder());
                        fragments.put(req.getRevision().getSymbolicName(), fragmentVersions);
                    }
                    List<BundleRequirement> actual = fragmentVersions.get(req.getRevision().getVersion());
                    if (actual == null)
                    {
                        actual = new ArrayList<BundleRequirement>();
                        fragmentVersions.put(req.getRevision().getVersion(), actual);
                    }
                    actual.add(req);
                }
            }
        }   
    }

    /**
     * Removes a module from the internal data structures if it wasn't selected
     * as a fragment or a singleton. This process may cause other modules to
     * become unresolved if they depended on the module's capabilities and there
     * is no other candidate.
     * @param revision the module to remove.
     * @throws ResolveException if removing the module caused the resolve to fail.
    **/
    private void removeRevision(BundleRevision revision) throws ResolveException
    {
        if (m_root.equals(revision))
        {
// TODO: SINGLETON RESOLVER - Improve this message.
            String msg = "Unable to resolve " + m_root;
            ResolveException ex = new ResolveException(msg, m_root, null);
            throw ex;
        }
        Set<BundleRevision> unresolvedRevisions = new HashSet<BundleRevision>();
        remove(revision, unresolvedRevisions);
        while (!unresolvedRevisions.isEmpty())
        {
            Iterator<BundleRevision> it = unresolvedRevisions.iterator();
            revision = it.next();
            it.remove();
            remove(revision, unresolvedRevisions);
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
    private void remove(BundleRevision br, Set<BundleRevision> unresolvedRevisions)
        throws ResolveException
    {
        for (BundleRequirement r : br.getDeclaredRequirements(null))
        {
            remove(r);
        }

        for (BundleCapability c : br.getDeclaredCapabilities(null))
        {
            remove(c, unresolvedRevisions);
        }
    }

    /**
     * Removes a requirement from the internal data structures.
     * @param req the requirement to remove.
    **/
    private void remove(BundleRequirement req)
    {
        boolean isFragment = req.getNamespace().equals(BundleCapabilityImpl.HOST_NAMESPACE);

        SortedSet<BundleCapability> candidates = m_candidateMap.remove(req);
        if (candidates != null)
        {
            for (BundleCapability cap : candidates)
            {
                Set<BundleRequirement> dependents = m_dependentMap.get(cap);
                if (dependents != null)
                {
                    dependents.remove(req);
                }

                if (isFragment)
                {
                    Map<String, Map<Version, List<BundleRequirement>>>
                        fragments = m_hostFragments.get(cap);
                    if (fragments != null)
                    {
                        Map<Version, List<BundleRequirement>> fragmentVersions =
                            fragments.get(req.getRevision().getSymbolicName());
                        if (fragmentVersions != null)
                        {
                            List<BundleRequirement> actual =
                                fragmentVersions.get(req.getRevision().getVersion());
                            if (actual != null)
                            {
                                actual.remove(req);
                                if (actual.isEmpty())
                                {
                                    fragmentVersions.remove(req.getRevision().getVersion());
                                    if (fragmentVersions.isEmpty())
                                    {
                                        fragments.remove(req.getRevision().getSymbolicName());
                                        if (fragments.isEmpty())
                                        {
                                            m_hostFragments.remove(cap);
                                        }
                                    }
                                }
                            }
                        }
                    }
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
    private void remove(BundleCapability c, Set<BundleRevision> unresolvedRevisions)
        throws ResolveException
    {
        Set<BundleRequirement> dependents = m_dependentMap.remove(c);
        if (dependents != null)
        {
            for (BundleRequirement r : dependents)
            {
                SortedSet<BundleCapability> candidates = m_candidateMap.get(r);
                candidates.remove(c);
                if (candidates.isEmpty())
                {
                    m_candidateMap.remove(r);
                    if (!((BundleRequirementImpl) r).isOptional())
                    {
                        if (m_root.equals(r.getRevision()))
                        {
                            String msg = "Unable to resolve " + m_root
                                + ": missing requirement " + r;
                            ResolveException ex = new ResolveException(msg, m_root, r);
                            throw ex;
                        }
                        unresolvedRevisions.add(r.getRevision());
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
        Map<BundleCapability, Set<BundleRequirement>> dependentMap =
            new HashMap<BundleCapability, Set<BundleRequirement>>();
        for (Entry<BundleCapability, Set<BundleRequirement>> entry : m_dependentMap.entrySet())
        {
            Set<BundleRequirement> dependents = new HashSet<BundleRequirement>(entry.getValue());
            dependentMap.put(entry.getKey(), dependents);
        }

        Map<BundleRequirement, SortedSet<BundleCapability>> candidateMap =
            new HashMap<BundleRequirement, SortedSet<BundleCapability>>();
        for (Entry<BundleRequirement, SortedSet<BundleCapability>> entry
            : m_candidateMap.entrySet())
        {
            SortedSet<BundleCapability> candidates =
                new TreeSet<BundleCapability>(entry.getValue());
            candidateMap.put(entry.getKey(), candidates);
        }

        return new Candidates(
            m_root, m_candidateRevisions, dependentMap, candidateMap,
            m_hostFragments, m_allWrappedHosts, m_populateResultCache,
            m_fragmentsPresent);
    }

    public void dump()
    {
        // Create set of all revisions from requirements.
        Set<BundleRevision> revisions = new HashSet<BundleRevision>();
        for (Entry<BundleRequirement, SortedSet<BundleCapability>> entry
            : m_candidateMap.entrySet())
        {
            revisions.add(entry.getKey().getRevision());
        }
        // Now dump the revisions.
        System.out.println("=== BEGIN CANDIDATE MAP ===");
        for (BundleRevision br : revisions)
        {
            System.out.println("  " + br
                 + " (" + ((br.getWiring() != null) ? "RESOLVED)" : "UNRESOLVED)"));
            List<BundleRequirement> reqs = (br.getWiring() != null)
                ? br.getWiring().getRequirements(null)
                : br.getDeclaredRequirements(null);
            for (BundleRequirement req : reqs)
            {
                Set<BundleCapability> candidates = m_candidateMap.get(req);
                if ((candidates != null) && (candidates.size() > 0))
                {
                    System.out.println("    " + req + ": " + candidates);
                }
            }
// TODO: OSGi R4.3 - Need to check what getWiring().getRequirements() returns
//       with respect to dynamic imports; is it the union of all declared
//       dynamic imports from fragments and host?
            reqs = (br.getWiring() != null)
                ? Util.getDynamicRequirements(br.getWiring().getRequirements(null))
                : Util.getDynamicRequirements(br.getDeclaredRequirements(null));
            for (BundleRequirement req : reqs)
            {
                Set<BundleCapability> candidates = m_candidateMap.get(req);
                if ((candidates != null) && (candidates.size() > 0))
                {
                    System.out.println("    " + req + ": " + candidates);
                }
            }
        }
        System.out.println("=== END CANDIDATE MAP ===");
    }

    /**
     * Returns true if the specified module is a singleton
     * (i.e., directive singleton:=true).
     *
     * @param revision the module to check for singleton status.
     * @return true if the module is a singleton, false otherwise.
    **/
    private static boolean isSingleton(BundleRevision revision)
    {
        final List<BundleCapability> modCaps =
            Util.getCapabilityByNamespace(
                revision, BundleCapabilityImpl.BUNDLE_NAMESPACE);
        if (modCaps == null || modCaps.isEmpty())
        {
            return false;
        }
        String value = modCaps.get(0).getDirectives().get(Constants.SINGLETON_DIRECTIVE);
        if (value != null)
        {
            return Boolean.valueOf(value);
        }
        return false;
    }
}
