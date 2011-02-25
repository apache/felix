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
import org.apache.felix.framework.capabilityset.Requirement;
import org.osgi.framework.Version;

public class Candidates
{
    private final Module m_root;

    // Maps a capability to requirements that match it.
    private final Map<Capability, Set<Requirement>> m_dependentMap;
    // Maps a requirement to the capability it matches.
    private final Map<Requirement, SortedSet<Capability>> m_candidateMap;
    // Maps a module to a map containing its potential fragments; the
    // fragment map maps a fragment symbolic name to a map that maps
    // a version to a list of fragments matching that symbolic name
    // and version.
    private final Map<Module, Map<String, Map<Version, List<Module>>>> m_hostFragments;
    // Maps a module to its associated wrapped module; this only happens
    // when a module being resolved has fragments to attach to it.
    private final Map<Module, WrappedModule> m_allWrappedHosts;

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
        Map<Capability, Set<Requirement>> dependentMap,
        Map<Requirement, SortedSet<Capability>> candidateMap,
        Map<Module, Map<String, Map<Version, List<Module>>>> hostFragments,
        Map<Module, WrappedModule> wrappedHosts)
    {
        m_root = root;
        m_dependentMap = dependentMap;
        m_candidateMap = candidateMap;
        m_hostFragments = hostFragments;
        m_allWrappedHosts = wrappedHosts;
    }

    /**
     * Constructs a new object with the specified root module. The root module
     * is used to determine if the resolves fails when manipulating the candidates.
     * For example, it may be necessary to remove an unselected fragment, which
     * can cause a ripple effect all the way to the root module. If that happens
     * then the resolve fails.
     * @param root the root module for the resolve.
    **/
    public Candidates(Module root)
    {
        m_root = root;
        m_dependentMap = new HashMap<Capability, Set<Requirement>>();
        m_candidateMap = new HashMap<Requirement, SortedSet<Capability>>();
        m_hostFragments = new HashMap<Module, Map<String, Map<Version, List<Module>>>>();
        m_allWrappedHosts = new HashMap<Module, WrappedModule>();
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
    public void add(Requirement req, SortedSet<Capability> candidates)
    {
        boolean isFragment = req.getNamespace().equals(Capability.HOST_NAMESPACE);

        // Record the candidates.
        m_candidateMap.put(req, candidates);
        // Add the requirement as a dependent on the candidates.
        for (Capability cap : candidates)
        {
            Set<Requirement> dependents = m_dependentMap.get(cap);
            if (dependents == null)
            {
                dependents = new HashSet<Requirement>();
                m_dependentMap.put(cap, dependents);
            }
            dependents.add(req);
            // Keep track of hosts and associated fragments.
            if (isFragment)
            {
                Map<String, Map<Version, List<Module>>> fragments =
                    m_hostFragments.get(cap.getModule());
                if (fragments == null)
                {
                    fragments = new HashMap<String, Map<Version, List<Module>>>();
                    m_hostFragments.put(cap.getModule(), fragments);
                }
                Map<Version, List<Module>> fragmentVersions =
                    fragments.get(req.getModule().getSymbolicName());
                if (fragmentVersions == null)
                {
                    fragmentVersions = new TreeMap<Version, List<Module>>(Collections.reverseOrder());
                    fragments.put(req.getModule().getSymbolicName(), fragmentVersions);
                }
                List<Module> actual = fragmentVersions.get(req.getModule().getVersion());
                if (actual == null)
                {
                    actual = new ArrayList<Module>();
                    fragmentVersions.put(req.getModule().getVersion(), actual);
                }
                actual.add(req.getModule());
            }
        }
    }

    /**
     * Adds requirements and candidates in bulk. The outer map is not retained
     * by this method, but the inner data structures are, so they should not
     * be further modified by the caller.
     * @param candidates the bulk requirements and candidates to add.
    **/
    public void add(Map<Requirement, SortedSet<Capability>> candidates)
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
     * Gets the complete candidate map. This is only used in the dynamic
     * import case, since the requirement is unknown and it just needs the
     * one and only requirement in the map. (This could probably be handled
     * differently or better, but it is sufficient for now.)
     * @return The entire candidate map.
    **/
    public Map<Requirement, SortedSet<Capability>> getCandidateMap()
    {
        return m_candidateMap;
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
    **/
    public void mergeFragments()
    {
        // This method performs the following steps:
        // 1. Select the fragments to attach to a given host.
        // 2. Wrap hosts and attach fragments.
        // 3. Remove any unselected fragments. This is necessary because
        //    other modules may depend on the capabilities of unselected
        //    fragments, so we need to remove the unselected fragments and
        //    any module that depends on them, which could ultimately cause
        //    the entire resolve to fail.
        // 4. Replace all fragments with any host it was merged into
        //    (effectively multiplying it).
        //    * This includes setting candidates for attached fragment
        //      requirements as well as replacing fragment capabilities
        //      with host's attached fragment capabilities.

        // Steps 1 and 2
        List<WrappedModule> wrappedHosts = new ArrayList<WrappedModule>();
        List<Module> unselectedFragments = new ArrayList<Module>();
        for (Entry<Module, Map<String, Map<Version, List<Module>>>> entry :
            m_hostFragments.entrySet())
        {
            // Step 1
            List<Module> selectedFragments = new ArrayList<Module>();
            Module host = entry.getKey();
            Map<String, Map<Version, List<Module>>> fragments = entry.getValue();
            for (Entry<String, Map<Version, List<Module>>> fragEntry : fragments.entrySet())
            {
                boolean isFirst = true;
                for (Entry<Version, List<Module>> versionEntry : fragEntry.getValue().entrySet())
                {
                    for (Module m : versionEntry.getValue())
                    {
                        if (isFirst && !m.isRemovalPending())
                        {
                            selectedFragments.add(m);
                            isFirst = false;
                        }
                        else
                        {
// TODO: FRAGMENT RESOLVER - Fragments should only be removed when they no longer
//       match any hosts, not immediately.
                            unselectedFragments.add(m);
                        }
                    }
                }
            }

            // Step 2
            WrappedModule wrappedHost = new WrappedModule(host, selectedFragments);
            wrappedHosts.add(wrappedHost);
            m_allWrappedHosts.put(host, wrappedHost);
        }

        // Step 3
        for (Module m : unselectedFragments)
        {
            unselectFragment(m);
        }

        // Step 4
        for (WrappedModule wrappedHost : wrappedHosts)
        {
            // Replaces capabilities from fragments with the capabilities
            // from the merged host.
            for (Capability c : wrappedHost.getCapabilities())
            {
                Set<Requirement> dependents =
                    m_dependentMap.get(((WrappedCapability) c).getWrappedCapability());
                if (dependents != null)
                {
                    for (Requirement r : dependents)
                    {
                        Set<Capability> cands = m_candidateMap.get(r);
                        cands.remove(((WrappedCapability) c).getWrappedCapability());
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
                    m_candidateMap.get(((WrappedRequirement) r).getWrappedRequirement());
                if (cands != null)
                {
                    m_candidateMap.put(r, new TreeSet<Capability>(cands));
                }
            }
        }
    }

    /**
     * Removes a fragment from the internal data structures if it wasn't selected.
     * This process may cause other modules to become unresolved if they depended
     * on fragment capabilities and there is no other candidate.
     * @param fragment the fragment to remove.
     * @throws ResolveException if removing the fragment caused the resolve to fail.
    **/
    private void unselectFragment(Module fragment) throws ResolveException
    {
        Set<Module> unresolvedModules = new HashSet<Module>();
        remove(fragment, unresolvedModules);
        while (!unresolvedModules.isEmpty())
        {
            Iterator<Module> it = unresolvedModules.iterator();
            fragment = it.next();
            it.remove();
            remove(fragment, unresolvedModules);
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
                    Map<String, Map<Version, List<Module>>> fragments =
                        m_hostFragments.get(cap.getModule());
                    if (fragments != null)
                    {
                        Map<Version, List<Module>> fragmentVersions =
                            fragments.get(req.getModule().getSymbolicName());
                        if (fragmentVersions != null)
                        {
                            List<Module> actual = fragmentVersions.get(req.getModule().getVersion());
                            if (actual != null)
                            {
                                actual.remove(req.getModule());
                                if (actual.isEmpty())
                                {
                                    fragmentVersions.remove(req.getModule().getVersion());
                                    if (fragmentVersions.isEmpty())
                                    {
                                        fragments.remove(req.getModule().getSymbolicName());
                                        if (fragments.isEmpty())
                                        {
                                            m_hostFragments.remove(cap.getModule());
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
            m_root, dependentMap, candidateMap, m_hostFragments, m_allWrappedHosts);
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
}