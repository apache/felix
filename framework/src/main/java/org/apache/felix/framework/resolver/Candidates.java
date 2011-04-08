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
import org.apache.felix.framework.capabilityset.Capability;
import org.apache.felix.framework.capabilityset.Directive;
import org.apache.felix.framework.capabilityset.Requirement;
import org.apache.felix.framework.resolver.Resolver.ResolverState;
import org.apache.felix.framework.util.Util;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

class Candidates
{
    private final Module m_root;

    // Set of all candidate modules.
    private final Set<Module> m_candidateModules;
    // Maps a capability to requirements that match it.
    private final Map<Capability, Set<Requirement>> m_dependentMap;
    // Maps a requirement to the capability it matches.
    private final Map<Requirement, SortedSet<Capability>> m_candidateMap;
    // Maps a host capability to a map containing its potential fragments;
    // the fragment map maps a fragment symbolic name to a map that maps
    // a version to a list of fragments requirements matching that symbolic
    // name and version.
    private final Map<Capability, Map<String, Map<Version, List<Requirement>>>> m_hostFragments;
    // Maps a module to its associated wrapped module; this only happens
    // when a module being resolved has fragments to attach to it.
    private final Map<Module, HostModule> m_allWrappedHosts;
    // Map used when populating candidates to hold intermediate and final results.
    private final Map<Module, Object> m_populateResultCache;

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
        Module root,
        Set<Module> candidateModules,
        Map<Capability, Set<Requirement>> dependentMap,
        Map<Requirement, SortedSet<Capability>> candidateMap,
        Map<Capability, Map<String, Map<Version, List<Requirement>>>> hostFragments,
        Map<Module, HostModule> wrappedHosts, Map<Module, Object> populateResultCache,
        boolean fragmentsPresent)
    {
        m_root = root;
        m_candidateModules = candidateModules;
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
    public Candidates(ResolverState state, Module root)
    {
        m_root = root;
        m_candidateModules = new HashSet<Module>();
        m_dependentMap = new HashMap<Capability, Set<Requirement>>();
        m_candidateMap = new HashMap<Requirement, SortedSet<Capability>>();
        m_hostFragments =
            new HashMap<Capability, Map<String, Map<Version, List<Requirement>>>>();
        m_allWrappedHosts = new HashMap<Module, HostModule>();
        m_populateResultCache = new HashMap<Module, Object>();

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
    public Candidates(ResolverState state, Module root,
        Requirement req, SortedSet<Capability> candidates)
    {
        m_root = root;
        m_candidateModules = new HashSet<Module>();
        m_dependentMap = new HashMap<Capability, Set<Requirement>>();
        m_candidateMap = new HashMap<Requirement, SortedSet<Capability>>();
        m_hostFragments =
            new HashMap<Capability, Map<String, Map<Version, List<Requirement>>>>();
        m_allWrappedHosts = new HashMap<Module, HostModule>();
        m_populateResultCache = new HashMap<Module, Object>();

        add(req, candidates);

        populateDynamic(state, m_root);
    }

    /**
     * Populates additional candidates for the specified module.
     * @param state the resolver state used for populating the candidates.
     * @param module the module whose candidates should be populated.
     */
// TODO: FELIX3 - Modify to not be recursive.
    public final void populate(ResolverState state, Module module)
    {
        // Determine if we've already calculated this module's candidates.
        // The result cache will have one of three values:
        //   1. A resolve exception if we've already attempted to populate the
        //      module's candidates but were unsuccessful.
        //   2. Boolean.TRUE indicating we've already attempted to populate the
        //      module's candidates and were successful.
        //   3. An array containing the cycle count, current map of candidates
        //      for already processed requirements, and a list of remaining
        //      requirements whose candidates still need to be calculated.
        // For case 1, rethrow the exception. For case 2, simply return immediately.
        // For case 3, this means we have a cycle so we should continue to populate
        // the candidates where we left off and not record any results globally
        // until we've popped completely out of the cycle.

        // Keeps track of the number of times we've reentered this method
        // for the current module.
        Integer cycleCount = null;

        // Keeps track of the candidates we've already calculated for the
        // current module's requirements.
        Map<Requirement, SortedSet<Capability>> localCandidateMap = null;

        // Keeps track of the current module's requirements for which we
        // haven't yet found candidates.
        List<Requirement> remainingReqs = null;

        // Get the cache value for the current module.
        Object cacheValue = m_populateResultCache.get(module);

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

        // If there is no cache value for the current module, then this is
        // the first time we are attempting to populate its candidates, so
        // do some one-time checks and initialization.
        if ((remainingReqs == null) && (localCandidateMap == null))
        {
            // Verify that any required execution environment is satisfied.
            state.checkExecutionEnvironment(module);

            // Verify that any native libraries match the current platform.
            state.checkNativeLibraries(module);

            // Record cycle count.
            cycleCount = new Integer(0);

            // Create a local map for populating candidates first, just in case
            // the module is not resolvable.
            localCandidateMap = new HashMap();

            // Create a modifiable list of the module's requirements.
            remainingReqs = new ArrayList(module.getRequirements());

            // Add these value to the result cache so we know we are
            // in the middle of populating candidates for the current
            // module.
            m_populateResultCache.put(module,
                cacheValue = new Object[] { cycleCount, localCandidateMap, remainingReqs });
        }

        // If we have requirements remaining, then find candidates for them.
        while (remainingReqs.size() > 0)
        {
            Requirement req = remainingReqs.remove(0);

            // Get satisfying candidates and populate their candidates if necessary.
            ResolveException rethrow = null;
            SortedSet<Capability> candidates = state.getCandidates(req, true);
            for (Iterator<Capability> itCandCap = candidates.iterator(); itCandCap.hasNext(); )
            {
                Capability candCap = itCandCap.next();

                // If the candidate module is a fragment, then always attempt
                // to populate candidates for its dependency, since it must be
                // attached to a host to be used. Otherwise, if the candidate
                // module is not already resolved and is not the current module
                // we are trying to populate, then populate the candidates for
                // its dependencies as well.
                // NOTE: Technically, we don't have to check to see if the
                // candidate module is equal to the current module, but this
                // saves us from recursing and also simplifies exceptions messages
                // since we effectively chain exception messages for each level
                // of recursion; thus, any avoided recursion results in fewer
                // exceptions to chain when an error does occur.
                if (Util.isFragment(candCap.getModule())
                    || (!candCap.getModule().isResolved()
                        && !candCap.getModule().equals(module)))
                {
                    try
                    {
                        populate(state, candCap.getModule());
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
            if (candidates.isEmpty() && !req.isOptional())
            {
                String msg = "Unable to resolve " + module
                    + ": missing requirement " + req;
                if (rethrow != null)
                {
                    msg = msg + " [caused by: " + rethrow.getMessage() + "]";
                }
                rethrow = new ResolveException(msg, module, req);
                m_populateResultCache.put(module, rethrow);
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
            // Record that the module was successfully populated.
            m_populateResultCache.put(module, Boolean.TRUE);

            // Merge local candidate map into global candidate map.
            if (localCandidateMap.size() > 0)
            {
                add(localCandidateMap);
            }
        }
    }

    public final void populateOptional(ResolverState state, Module module)
    {
        try
        {
            // If the optional module is a fragment, then we only want to populate
            // the fragment if it has a candidate host in the set of already populated
            // modules. We do this to avoid unnecessary work in prepare(). If the
            // fragment has a host, we'll prepopulate the result cache here to avoid
            // having to do the host lookup again in populate().
            boolean isFragment = Util.isFragment(module);
            if (isFragment)
            {
                // Get the current result cache value, to make sure the module
                // hasn't already been populated.
                Object cacheValue = m_populateResultCache.get(module);
                if (cacheValue == null)
                {
                    // Create a modifiable list of the module's requirements.
                    List<Requirement> remainingReqs = new ArrayList(module.getRequirements());

                    // Find the host requirement.
                    Requirement hostReq = null;
                    for (Iterator<Requirement> it = remainingReqs.iterator(); it.hasNext(); )
                    {
                        Requirement r = it.next();
                        if (r.getNamespace().equals(Capability.HOST_NAMESPACE))
                        {
                            hostReq = r;
                            it.remove();
                        }
                    }

                    // Get candidates hosts and keep any that have been populated.
                    SortedSet<Capability> hosts = state.getCandidates(hostReq, false);
                    for (Iterator<Capability> it = hosts.iterator(); it.hasNext(); )
                    {
                        Capability host = it.next();
                        if (!isPopulated(host.getModule()))
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
                    state.checkExecutionEnvironment(module);

                    // Verify that any native libraries match the current platform.
                    state.checkNativeLibraries(module);

                    // Record cycle count, but start at -1 since it will
                    // be incremented again in populate().
                    Integer cycleCount = new Integer(-1);

                    // Create a local map for populating candidates first, just in case
                    // the module is not resolvable.
                    Map<Requirement, SortedSet<Capability>> localCandidateMap = new HashMap();

                    // Add the discovered host candidates to the local candidate map.
                    localCandidateMap.put(hostReq, hosts);

                    // Add these value to the result cache so we know we are
                    // in the middle of populating candidates for the current
                    // module.
                    m_populateResultCache.put(module,
                        new Object[] { cycleCount, localCandidateMap, remainingReqs });
                }
            }

            // Try to populate candidates for the optional module.
            populate(state, module);
        }
        catch (ResolveException ex)
        {
            // Ignore since the module is optional.
        }
    }

    private boolean isPopulated(Module module)
    {
        Object value = m_populateResultCache.get(module);
        return ((value != null) && (value instanceof Boolean));
    }

    private void populateDynamic(ResolverState state, Module module)
    {
        // There should be one entry in the candidate map, which are the
        // the candidates for the matching dynamic requirement. Get the
        // matching candidates and populate their candidates if necessary.
        ResolveException rethrow = null;
        Entry<Requirement, SortedSet<Capability>> entry =
            m_candidateMap.entrySet().iterator().next();
        Requirement dynReq = entry.getKey();
        SortedSet<Capability> candidates = entry.getValue();
        for (Iterator<Capability> itCandCap = candidates.iterator(); itCandCap.hasNext(); )
        {
            Capability candCap = itCandCap.next();
            if (!candCap.getModule().isResolved())
            {
                try
                {
                    populate(state, candCap.getModule());
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
                rethrow = new ResolveException("Dynamic import failed.", module, dynReq);
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
    private void add(Requirement req, SortedSet<Capability> candidates)
    {
        if (req.getNamespace().equals(Capability.HOST_NAMESPACE))
        {
            m_fragmentsPresent = true;
        }

        // Record the candidates.
        m_candidateMap.put(req, candidates);

        // Make a list of all candidate modules for determining singetons.
        // Add the requirement as a dependent on the candidates. Keep track
        // of fragments for hosts.
        for (Capability cap : candidates)
        {
            // Remember the module for all capabilities so we can
            // determine which ones are singletons.
            m_candidateModules.add(cap.getModule());
        }
    }

    /**
     * Adds requirements and candidates in bulk. The outer map is not retained
     * by this method, but the inner data structures are, so they should not
     * be further modified by the caller.
     * @param candidates the bulk requirements and candidates to add.
    **/
    private void add(Map<Requirement, SortedSet<Capability>> candidates)
    {
        for (Entry<Requirement, SortedSet<Capability>> entry : candidates.entrySet())
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
    public Module getWrappedHost(Module m)
    {
        Module wrapped = m_allWrappedHosts.get(m);
        return (wrapped == null) ? m : wrapped;
    }

    /**
     * Gets the candidates associated with a given requirement.
     * @param req the requirement whose candidates are desired.
     * @return the matching candidates or null.
    **/
    public SortedSet<Capability> getCandidates(Requirement req)
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
    public void prepare(List<Module> existingSingletons)
    {
        boolean init = false;

        if (m_fragmentsPresent)
        {
            populateDependents();
            init = true;
        }

        final Map<String, Module> singletons = new HashMap<String, Module>();

        for (Iterator<Module> it = m_candidateModules.iterator(); it.hasNext(); )
        {
            Module m = it.next();
            if (isSingleton(m))
            {
                if (!init)
                {
                    populateDependents();
                    init = true;
                }

                // See if there is an existing singleton for the
                // module's symbolic name.
                Module singleton = singletons.get(m.getSymbolicName());
                // If there is no existing singleton or this module is
                // a resolved singleton or this module has a higher version
                // and the existing singleton is not resolved, then select
                // this module as the singleton.
                if ((singleton == null)
                    || m.isResolved()
                    || ((m.getVersion().compareTo(singleton.getVersion()) > 0)
                        && !singleton.isResolved()))
                {
                    singletons.put(m.getSymbolicName(), m);
                    // Remove the singleton module from the candidates
                    // if it wasn't selected.
                    if (singleton != null)
                    {
                        removeModule(singleton);
                    }
                }
                else
                {
                    removeModule(m);
                }
            }
        }

        // If the root is a singleton, then prefer it over any other singleton.
        if (isSingleton(m_root))
        {
            Module singleton = singletons.get(m_root.getSymbolicName());
            singletons.put(m_root.getSymbolicName(), m_root);
            if ((singleton != null) && !singleton.equals(m_root))
            {
                if (singleton.isResolved())
                {
                    throw new ResolveException(
                        "Cannot resolve singleton "
                        + m_root
                        + " because "
                        + singleton
                        + " singleton is already resolved.",
                        m_root, null);
                }
                removeModule(singleton);
            }
        }

        // Make sure selected singletons do not conflict with existing
        // singletons passed into this method.
        for (int i = 0; (existingSingletons != null) && (i < existingSingletons.size()); i++)
        {
            Module existing = existingSingletons.get(i);
            Module singleton = singletons.get(existing.getSymbolicName());
            if ((singleton != null) && (singleton != existing))
            {
                singletons.remove(singleton.getSymbolicName());
                removeModule(singleton);
            }
        }

        // This method performs the following steps:
        // 1. Select the fragments to attach to a given host.
        // 2. Wrap hosts and attach fragments.
        // 3. Remove any unselected fragments. This is necessary because
        //    other modules may depend on the capabilities of unselected
        //    fragments, so we need to remove the unselected fragments and
        //    any modules that depends on them, which could ultimately cause
        //    the entire resolve to fail.
        // 4. Replace all fragments with any host it was merged into
        //    (effectively multiplying it).
        //    * This includes setting candidates for attached fragment
        //      requirements as well as replacing fragment capabilities
        //      with host's attached fragment capabilities.

        // Steps 1 and 2
        List<HostModule> wrappedHosts = new ArrayList<HostModule>();
        List<Module> unselectedFragments = new ArrayList<Module>();
        for (Entry<Capability, Map<String, Map<Version, List<Requirement>>>> hostEntry :
            m_hostFragments.entrySet())
        {
            // Step 1
            Capability hostCap = hostEntry.getKey();
            Map<String, Map<Version, List<Requirement>>> fragments = hostEntry.getValue();
            List<Module> selectedFragments = new ArrayList<Module>();
            for (Entry<String, Map<Version, List<Requirement>>> fragEntry : fragments.entrySet())
            {
                boolean isFirst = true;
                for (Entry<Version, List<Requirement>> versionEntry
                    : fragEntry.getValue().entrySet())
                {
                    for (Requirement hostReq : versionEntry.getValue())
                    {
                        // Select the highest version of the fragment that
                        // is not removal pending.
                        if (isFirst && !hostReq.getModule().isRemovalPending())
                        {
                            selectedFragments.add(hostReq.getModule());
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
                            SortedSet<Capability> hosts = m_candidateMap.get(hostReq);
                            hosts.remove(hostCap);
                            if (hosts.isEmpty())
                            {
                                unselectedFragments.add(hostReq.getModule());
                            }
                        }
                    }
                }
            }

            // Step 2
            HostModule wrappedHost = new HostModule(hostCap.getModule(), selectedFragments);
            wrappedHosts.add(wrappedHost);
            m_allWrappedHosts.put(hostCap.getModule(), wrappedHost);
        }

        // Step 3
        for (Module m : unselectedFragments)
        {
            removeModule(m);
        }

        // Step 4
        for (HostModule wrappedHost : wrappedHosts)
        {
            // Replaces capabilities from fragments with the capabilities
            // from the merged host.
            for (Capability c : wrappedHost.getCapabilities())
            {
                Set<Requirement> dependents =
                    m_dependentMap.get(((HostedCapability) c).getDeclaredCapability());
                if (dependents != null)
                {
                    for (Requirement r : dependents)
                    {
                        Set<Capability> cands = m_candidateMap.get(r);
                        cands.remove(((HostedCapability) c).getDeclaredCapability());
                        cands.add(c);
                    }
                }
            }

            // Copies candidates for fragment requirements to the host.
            // This doesn't record the reverse dependency, but that
            // information should not be needed at this point anymore.
            for (Requirement r : wrappedHost.getRequirements())
            {
                SortedSet<Capability> cands =
                    m_candidateMap.get(((HostedRequirement) r).getDeclaredRequirement());
                if (cands != null)
                {
                    m_candidateMap.put(r, new TreeSet<Capability>(cands));
                }
            }
        }
    }

    private void populateDependents()
    {
        for (Entry<Requirement, SortedSet<Capability>> entry : m_candidateMap.entrySet())
        {
            Requirement req = entry.getKey();
            SortedSet<Capability> caps = entry.getValue();
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
                if (req.getNamespace().equals(Capability.HOST_NAMESPACE))
                {
                    Map<String, Map<Version, List<Requirement>>>
                        fragments = m_hostFragments.get(cap);
                    if (fragments == null)
                    {
                        fragments = new HashMap<String, Map<Version, List<Requirement>>>();
                        m_hostFragments.put(cap, fragments);
                    }
                    Map<Version, List<Requirement>> fragmentVersions =
                        fragments.get(req.getModule().getSymbolicName());
                    if (fragmentVersions == null)
                    {
                        fragmentVersions =
                            new TreeMap<Version, List<Requirement>>(Collections.reverseOrder());
                        fragments.put(req.getModule().getSymbolicName(), fragmentVersions);
                    }
                    List<Requirement> actual = fragmentVersions.get(req.getModule().getVersion());
                    if (actual == null)
                    {
                        actual = new ArrayList<Requirement>();
                        fragmentVersions.put(req.getModule().getVersion(), actual);
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
     * @param module the module to remove.
     * @throws ResolveException if removing the module caused the resolve to fail.
    **/
    private void removeModule(Module module) throws ResolveException
    {
        if (m_root.equals(module))
        {
// TODO: SINGLETON RESOLVER - Improve this message.
            String msg = "Unable to resolve " + m_root;
            ResolveException ex = new ResolveException(msg, m_root, null);
            throw ex;
        }
        Set<Module> unresolvedModules = new HashSet<Module>();
        remove(module, unresolvedModules);
        while (!unresolvedModules.isEmpty())
        {
            Iterator<Module> it = unresolvedModules.iterator();
            module = it.next();
            it.remove();
            remove(module, unresolvedModules);
        }
    }

    /**
     * Removes the specified module from the internal data structures, which
     * involves removing its requirements and its capabilities. This may cause
     * other modules to become unresolved as a result.
     * @param m the module to remove.
     * @param unresolvedModules a list to containing any additional modules that
     *        that became unresolved as a result of removing this module and will
     *        also need to be removed.
     * @throws ResolveException if removing the module caused the resolve to fail.
    **/
    private void remove(Module m, Set<Module> unresolvedModules) throws ResolveException
    {
        for (Requirement r : m.getRequirements())
        {
            remove(r);
        }

        for (Capability c : m.getCapabilities())
        {
            remove(c, unresolvedModules);
        }
    }

    /**
     * Removes a requirement from the internal data structures.
     * @param req the requirement to remove.
    **/
    private void remove(Requirement req)
    {
        boolean isFragment = req.getNamespace().equals(Capability.HOST_NAMESPACE);

        SortedSet<Capability> candidates = m_candidateMap.remove(req);
        if (candidates != null)
        {
            for (Capability cap : candidates)
            {
                Set<Requirement> dependents = m_dependentMap.get(cap);
                if (dependents != null)
                {
                    dependents.remove(req);
                }

                if (isFragment)
                {
                    Map<String, Map<Version, List<Requirement>>>
                        fragments = m_hostFragments.get(cap);
                    if (fragments != null)
                    {
                        Map<Version, List<Requirement>> fragmentVersions =
                            fragments.get(req.getModule().getSymbolicName());
                        if (fragmentVersions != null)
                        {
                            List<Requirement> actual =
                                fragmentVersions.get(req.getModule().getVersion());
                            if (actual != null)
                            {
                                actual.remove(req);
                                if (actual.isEmpty())
                                {
                                    fragmentVersions.remove(req.getModule().getVersion());
                                    if (fragmentVersions.isEmpty())
                                    {
                                        fragments.remove(req.getModule().getSymbolicName());
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
     * @param unresolvedModules a list to containing any additional modules that
     *        that became unresolved as a result of removing this module and will
     *        also need to be removed.
     * @throws ResolveException if removing the module caused the resolve to fail.
    **/
    private void remove(Capability c, Set<Module> unresolvedModules) throws ResolveException
    {
        Set<Requirement> dependents = m_dependentMap.remove(c);
        if (dependents != null)
        {
            for (Requirement r : dependents)
            {
                SortedSet<Capability> candidates = m_candidateMap.get(r);
                candidates.remove(c);
                if (candidates.isEmpty())
                {
                    m_candidateMap.remove(r);
                    if (!r.isOptional())
                    {
                        if (m_root.equals(r.getModule()))
                        {
                            String msg = "Unable to resolve " + m_root
                                + ": missing requirement " + r;
                            ResolveException ex = new ResolveException(msg, m_root, r);
                            throw ex;
                        }
                        unresolvedModules.add(r.getModule());
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

        Map<Requirement, SortedSet<Capability>> candidateMap =
            new HashMap<Requirement, SortedSet<Capability>>();
        for (Entry<Requirement, SortedSet<Capability>> entry : m_candidateMap.entrySet())
        {
            SortedSet<Capability> candidates = new TreeSet<Capability>(entry.getValue());
            candidateMap.put(entry.getKey(), candidates);
        }

        return new Candidates(
            m_root, m_candidateModules, dependentMap, candidateMap,
            m_hostFragments, m_allWrappedHosts, m_populateResultCache,
            m_fragmentsPresent);
    }

    public void dump()
    {
        // Create set of all modules from requirements.
        Set<Module> modules = new HashSet();
        for (Entry<Requirement, SortedSet<Capability>> entry
            : m_candidateMap.entrySet())
        {
            modules.add(entry.getKey().getModule());
        }
        // Now dump the modules.
        System.out.println("=== BEGIN CANDIDATE MAP ===");
        for (Module module : modules)
        {
            System.out.println("  " + module
                 + " (" + (module.isResolved() ? "RESOLVED)" : "UNRESOLVED)"));
            for (Requirement req : module.getRequirements())
            {
                Set<Capability> candidates = m_candidateMap.get(req);
                if ((candidates != null) && (candidates.size() > 0))
                {
                    System.out.println("    " + req + ": " + candidates);
                }
            }
            for (Requirement req : module.getDynamicRequirements())
            {
                Set<Capability> candidates = m_candidateMap.get(req);
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
     * @param module the module to check for singleton status.
     * @return true if the module is a singleton, false otherwise.
    **/
    private static boolean isSingleton(Module module)
    {
        final List<Capability> modCaps =
            Util.getCapabilityByNamespace(
                module, Capability.MODULE_NAMESPACE);
        if (modCaps == null || modCaps.isEmpty())
        {
            return false;
        }
        final List<Directive> dirs = modCaps.get(0).getDirectives();
        for (int dirIdx = 0; (dirs != null) && (dirIdx < dirs.size()); dirIdx++)
        {
            if (dirs.get(dirIdx).getName().equalsIgnoreCase(Constants.SINGLETON_DIRECTIVE)
                && Boolean.valueOf((String) dirs.get(dirIdx).getValue()))
            {
                return true;
            }
        }
        return false;
    }
}
