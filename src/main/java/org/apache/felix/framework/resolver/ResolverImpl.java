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
import org.apache.felix.framework.Logger;
import org.apache.felix.framework.capabilityset.CapabilitySet;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.wiring.BundleCapabilityImpl;
import org.apache.felix.framework.wiring.BundleRequirementImpl;
import org.osgi.framework.Constants;

public class ResolverImpl implements Resolver
{
    private final Logger m_logger;

    // Holds candidate permutations based on permutating "uses" chains.
    // These permutations are given higher priority.
    private final List<Candidates> m_usesPermutations = new ArrayList<Candidates>();
    // Holds candidate permutations based on permutating requirement candidates.
    // These permutations represent backtracking on previous decisions.
    private final List<Candidates> m_importPermutations = new ArrayList<Candidates>();

    public ResolverImpl(Logger logger)
    {
        m_logger = logger;
    }

    public Map<Module, List<Wire>> resolve(
        ResolverState state, Module module, Set<Module> fragments)
    {
        Map<Module, List<Wire>> wireMap = new HashMap<Module, List<Wire>>();
        Map<Module, Packages> modulePkgMap = new HashMap<Module, Packages>();

        if (!module.isResolved())
        {
            boolean retryFragments;
            do
            {
                retryFragments = false;

                try
                {
                    // Populate all candidates.
                    Candidates allCandidates = new Candidates(state, module);

                    // Try to populate optional fragments.
                    for (Module fragment : fragments)
                    {
                        allCandidates.populateOptional(state, fragment);
                    }

                    // Merge any fragments into hosts.
                    allCandidates.prepare(getResolvedSingletons(state));

                    // Record the initial candidate permutation.
                     m_usesPermutations.add(allCandidates);

                    ResolveException rethrow = null;

                    // If the requested module is a fragment, then
                    // ultimately we will verify the host.
                    BundleRequirementImpl hostReq = getHostRequirement(module);
                    Module target = module;

                    do
                    {
                        rethrow = null;

                        modulePkgMap.clear();
                        m_packageSourcesCache.clear();

                        allCandidates = (m_usesPermutations.size() > 0)
                            ? m_usesPermutations.remove(0)
                            : m_importPermutations.remove(0);
//allCandidates.dump();

                        // If we are resolving a fragment, then we
                        // actually want to verify its host.
                        if (hostReq != null)
                        {
                            target = allCandidates.getCandidates(hostReq)
                                .iterator().next().getModule();
                        }

                        calculatePackageSpaces(
                            allCandidates.getWrappedHost(target), allCandidates, modulePkgMap,
                            new HashMap(), new HashSet());
//System.out.println("+++ PACKAGE SPACES START +++");
//dumpModulePkgMap(modulePkgMap);
//System.out.println("+++ PACKAGE SPACES END +++");

                        try
                        {
                            checkPackageSpaceConsistency(
                                false, allCandidates.getWrappedHost(target),
                                allCandidates, modulePkgMap, new HashMap());
                        }
                        catch (ResolveException ex)
                        {
                            rethrow = ex;
                        }
                    }
                    while ((rethrow != null)
                        && ((m_usesPermutations.size() > 0) || (m_importPermutations.size() > 0)));

                    // If there is a resolve exception, then determine if an
                    // optionally resolved module is to blame (typically a fragment).
                    // If so, then remove the optionally resolved module and try
                    // again; otherwise, rethrow the resolve exception.
                    if (rethrow != null)
                    {
                        Module faultyModule = getActualModule(rethrow.getModule());
                        if (rethrow.getRequirement() instanceof HostedRequirement)
                        {
                            faultyModule =
                                ((HostedRequirement) rethrow.getRequirement())
                                    .getDeclaredRequirement().getModule();
                        }
                        if (fragments.remove(faultyModule))
                        {
                            retryFragments = true;
                        }
                        else
                        {
                            throw rethrow;
                        }
                    }
                    // If there is no exception to rethrow, then this was a clean
                    // resolve, so populate the wire map.
                    else
                    {
                        wireMap =
                            populateWireMap(
                                allCandidates.getWrappedHost(target),
                                modulePkgMap, wireMap, allCandidates);
                    }
                }
                finally
                {
                    // Always clear the state.
                    m_usesPermutations.clear();
                    m_importPermutations.clear();
                }
            }
            while (retryFragments);
        }

        return wireMap;
    }

    public Map<Module, List<Wire>> resolve(
        ResolverState state, Module module, String pkgName, Set<Module> fragments)
    {
        // We can only create a dynamic import if the following
        // conditions are met:
        // 1. The specified module is resolved.
        // 2. The package in question is not already imported.
        // 3. The package in question is not accessible via require-bundle.
        // 4. The package in question is not exported by the bundle.
        // 5. The package in question matches a dynamic import of the bundle.
        // The following call checks all of these conditions and returns
        // the associated dynamic import and matching capabilities.
        Candidates allCandidates =
            getDynamicImportCandidates(state, module, pkgName);
        if (allCandidates != null)
        {
            Map<Module, List<Wire>> wireMap = new HashMap<Module, List<Wire>>();
            Map<Module, Packages> modulePkgMap = new HashMap<Module, Packages>();

            boolean retryFragments;
            do
            {
                retryFragments = false;

                try
                {
                    // Try to populate optional fragments.
                    for (Module fragment : fragments)
                    {
                        allCandidates.populateOptional(state, fragment);
                    }

                    // Merge any fragments into hosts.
                    allCandidates.prepare(getResolvedSingletons(state));

                    // Record the initial candidate permutation.
                     m_usesPermutations.add(allCandidates);

                    ResolveException rethrow = null;

                    do
                    {
                        rethrow = null;

                        modulePkgMap.clear();
                        m_packageSourcesCache.clear();

                        allCandidates = (m_usesPermutations.size() > 0)
                            ? m_usesPermutations.remove(0)
                            : m_importPermutations.remove(0);
//allCandidates.dump();

                        // For a dynamic import, the instigating module
                        // will never be a fragment since fragments never
                        // execute code, so we don't need to check for
                        // this case like we do for a normal resolve.

                        calculatePackageSpaces(
                            allCandidates.getWrappedHost(module), allCandidates, modulePkgMap,
                            new HashMap(), new HashSet());
//System.out.println("+++ PACKAGE SPACES START +++");
//dumpModulePkgMap(modulePkgMap);
//System.out.println("+++ PACKAGE SPACES END +++");

                        try
                        {
                            checkPackageSpaceConsistency(
                                false, allCandidates.getWrappedHost(module),
                                allCandidates, modulePkgMap, new HashMap());
                        }
                        catch (ResolveException ex)
                        {
                            rethrow = ex;
                        }
                    }
                    while ((rethrow != null)
                        && ((m_usesPermutations.size() > 0) || (m_importPermutations.size() > 0)));

                    // If there is a resolve exception, then determine if an
                    // optionally resolved module is to blame (typically a fragment).
                    // If so, then remove the optionally resolved module and try
                    // again; otherwise, rethrow the resolve exception.
                    if (rethrow != null)
                    {
                        Module faultyModule = getActualModule(rethrow.getModule());
                        if (rethrow.getRequirement() instanceof HostedRequirement)
                        {
                            faultyModule =
                                ((HostedRequirement) rethrow.getRequirement())
                                    .getDeclaredRequirement().getModule();
                        }
                        if (fragments.remove(faultyModule))
                        {
                            retryFragments = true;
                        }
                        else
                        {
                            throw rethrow;
                        }
                    }
                    // If there is no exception to rethrow, then this was a clean
                    // resolve, so populate the wire map.
                    else
                    {
                        wireMap = populateDynamicWireMap(
                            module, pkgName, modulePkgMap, wireMap, allCandidates);
                        return wireMap;
                    }
                }
                finally
                {
                    // Always clear the state.
                    m_usesPermutations.clear();
                    m_importPermutations.clear();
                }
            }
            while (retryFragments);
        }

        return null;
    }

    private static List<Module> getResolvedSingletons(ResolverState state)
    {
        BundleRequirementImpl req = new BundleRequirementImpl(
            null,
            BundleCapabilityImpl.SINGLETON_NAMESPACE,
            Collections.EMPTY_MAP,
            Collections.EMPTY_MAP);
        SortedSet<BundleCapabilityImpl> caps = state.getCandidates(req, true);
        List<Module> singletons = new ArrayList();
        for (BundleCapabilityImpl cap : caps)
        {
            if (cap.getModule().isResolved())
            {
                singletons.add(cap.getModule());
            }
        }
        return singletons;
    }

    private static BundleCapabilityImpl getHostCapability(Module m)
    {
        for (BundleCapabilityImpl c : m.getCapabilities())
        {
            if (c.getNamespace().equals(BundleCapabilityImpl.HOST_NAMESPACE))
            {
                return c;
            }
        }
        return null;
    }

    private static BundleRequirementImpl getHostRequirement(Module m)
    {
        for (BundleRequirementImpl r : m.getRequirements())
        {
            if (r.getNamespace().equals(BundleCapabilityImpl.HOST_NAMESPACE))
            {
                return r;
            }
        }
        return null;
    }

    private static Candidates getDynamicImportCandidates(
        ResolverState state, Module module, String pkgName)
    {
        // Unresolved modules cannot dynamically import, nor can the default
        // package be dynamically imported.
        if (!module.isResolved() || pkgName.length() == 0)
        {
            return null;
        }

        // If the module doesn't have dynamic imports, then just return
        // immediately.
        List<BundleRequirementImpl> dynamics = module.getDynamicRequirements();
        if ((dynamics == null) || dynamics.isEmpty())
        {
            return null;
        }

        // If any of the module exports this package, then we cannot
        // attempt to dynamically import it.
        List<BundleCapabilityImpl> caps = module.getCapabilities();
        for (int i = 0; (caps != null) && (i < caps.size()); i++)
        {
            if (caps.get(i).getNamespace().equals(BundleCapabilityImpl.PACKAGE_NAMESPACE)
                && caps.get(i).getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR).equals(pkgName))
            {
                return null;
            }
        }
        // If any of our wires have this package, then we cannot
        // attempt to dynamically import it.
        List<Wire> wires = module.getWires();
        for (int i = 0; (wires != null) && (i < wires.size()); i++)
        {
            if (wires.get(i).hasPackage(pkgName))
            {
                return null;
            }
        }

        // Loop through the importer's dynamic requirements to determine if
        // there is a matching one for the package from which we want to
        // load a class.
        Map<String, String> dirs = Collections.EMPTY_MAP;
        Map<String, Object> attrs = new HashMap(1);
        attrs.put(BundleCapabilityImpl.PACKAGE_ATTR, pkgName);
        BundleRequirementImpl req = new BundleRequirementImpl(
            module, BundleCapabilityImpl.PACKAGE_NAMESPACE, dirs, attrs);
        SortedSet<BundleCapabilityImpl> candidates = state.getCandidates(req, false);

        // First find a dynamic requirement that matches the capabilities.
        BundleRequirementImpl dynReq = null;
        for (int dynIdx = 0;
            (candidates.size() > 0) && (dynReq == null) && (dynIdx < dynamics.size());
            dynIdx++)
        {
            for (Iterator<BundleCapabilityImpl> itCand = candidates.iterator();
                (dynReq == null) && itCand.hasNext(); )
            {
                BundleCapabilityImpl cap = itCand.next();
                if (CapabilitySet.matches(cap, dynamics.get(dynIdx).getFilter()))
                {
                    dynReq = dynamics.get(dynIdx);
                }
            }
        }

        // If we found a matching dynamic requirement, then filter out
        // any candidates that do not match it.
        if (dynReq != null)
        {
            for (Iterator<BundleCapabilityImpl> itCand = candidates.iterator(); itCand.hasNext(); )
            {
                BundleCapabilityImpl cap = itCand.next();
                if (!CapabilitySet.matches(cap, dynReq.getFilter()))
                {
                    itCand.remove();
                }
            }
        }
        else
        {
            candidates.clear();
        }

        Candidates allCandidates = null;

        if (candidates.size() > 0)
        {
            allCandidates = new Candidates(state, module, dynReq, candidates);
        }

        return allCandidates;
    }

    private void calculatePackageSpaces(
        Module module,
        Candidates allCandidates,
        Map<Module, Packages> modulePkgMap,
        Map<BundleCapabilityImpl, List<Module>> usesCycleMap,
        Set<Module> cycle)
    {
        if (cycle.contains(module))
        {
            return;
        }
        cycle.add(module);

        // Create parallel arrays for requirement and proposed candidate
        // capability or actual capability if module is resolved or not.
        List<BundleRequirementImpl> reqs = new ArrayList();
        List<BundleCapabilityImpl> caps = new ArrayList();
        boolean isDynamicImport = false;
        if (module.isResolved())
        {
            // Use wires to get actual requirements and satisfying capabilities.
            for (Wire wire : module.getWires())
            {
                // Wrap the requirement as a hosted requirement
                // if it comes from a fragment, since we will need
                // to know the host.
                BundleRequirementImpl r = wire.getRequirement();
                if (!r.getModule().equals(wire.getImporter()))
                {
                    r = new HostedRequirement(wire.getImporter(), r);
                }
                // Wrap the capability as a hosted capability
                // if it comes from a fragment, since we will need
                // to know the host.
                BundleCapabilityImpl c = wire.getCapability();
                if (!c.getModule().equals(wire.getExporter()))
                {
                    c = new HostedCapability(wire.getExporter(), c);
                }
                reqs.add(r);
                caps.add(c);
            }

            // Since the module is resolved, it could be dynamically importing,
            // so check to see if there are candidates for any of its dynamic
            // imports.
            for (BundleRequirementImpl req : module.getDynamicRequirements())
            {
                // Get the candidates for the current requirement.
                SortedSet<BundleCapabilityImpl> candCaps = allCandidates.getCandidates(req);
                // Optional requirements may not have any candidates.
                if (candCaps == null)
                {
                    continue;
                }

                BundleCapabilityImpl cap = candCaps.iterator().next();
                reqs.add(req);
                caps.add(cap);
                isDynamicImport = true;
                // Can only dynamically import one at a time, so break
                // out of the loop after the first.
                break;
            }
        }
        else
        {
            for (BundleRequirementImpl req : module.getRequirements())
            {
                // Get the candidates for the current requirement.
                SortedSet<BundleCapabilityImpl> candCaps = allCandidates.getCandidates(req);
                // Optional requirements may not have any candidates.
                if (candCaps == null)
                {
                    continue;
                }

                BundleCapabilityImpl cap = candCaps.iterator().next();
                reqs.add(req);
                caps.add(cap);
            }
        }

        // First, add all exported packages to the target module's package space.
        calculateExportedPackages(module, allCandidates, modulePkgMap);
        Packages modulePkgs = modulePkgMap.get(module);

        // Second, add all imported packages to the target module's package space.
        for (int i = 0; i < reqs.size(); i++)
        {
            BundleRequirementImpl req = reqs.get(i);
            BundleCapabilityImpl cap = caps.get(i);
            calculateExportedPackages(cap.getModule(), allCandidates, modulePkgMap);
            mergeCandidatePackages(module, req, cap, modulePkgMap, allCandidates);
        }

        // Third, have all candidates to calculate their package spaces.
        for (int i = 0; i < caps.size(); i++)
        {
            calculatePackageSpaces(
                caps.get(i).getModule(), allCandidates, modulePkgMap,
                usesCycleMap, cycle);
        }

        // Fourth, if the target module is unresolved or is dynamically importing,
        // then add all the uses constraints implied by its imported and required
        // packages to its package space.
        // NOTE: We do not need to do this for resolved modules because their
        // package space is consistent by definition and these uses constraints
        // are only needed to verify the consistency of a resolving module. The
        // only exception is if a resolve module is dynamically importing, then
        // we need to calculate its uses constraints again to make sure the new
        // import is consistent with the existing package space.
        if (!module.isResolved() || isDynamicImport)
        {
            for (Entry<String, List<Blame>> entry : modulePkgs.m_importedPkgs.entrySet())
            {
                for (Blame blame : entry.getValue())
                {
                    // Ignore modules that import from themselves.
                    if (!blame.m_cap.getModule().equals(module))
                    {
                        List<BundleRequirementImpl> blameReqs = new ArrayList();
                        blameReqs.add(blame.m_reqs.get(0));

                        mergeUses(
                            module,
                            modulePkgs,
                            blame.m_cap,
                            blameReqs,
                            modulePkgMap,
                            allCandidates,
                            usesCycleMap);
                    }
                }
            }
            for (Entry<String, List<Blame>> entry : modulePkgs.m_requiredPkgs.entrySet())
            {
                for (Blame blame : entry.getValue())
                {
                    List<BundleRequirementImpl> blameReqs = new ArrayList();
                    blameReqs.add(blame.m_reqs.get(0));

                    mergeUses(
                        module,
                        modulePkgs,
                        blame.m_cap,
                        blameReqs,
                        modulePkgMap,
                        allCandidates,
                        usesCycleMap);
                }
            }
        }
    }

    private void mergeCandidatePackages(
        Module current, BundleRequirementImpl currentReq, BundleCapabilityImpl candCap,
        Map<Module, Packages> modulePkgMap,
        Candidates allCandidates)
    {
        if (candCap.getNamespace().equals(BundleCapabilityImpl.PACKAGE_NAMESPACE))
        {
            mergeCandidatePackage(
                current, false, currentReq, candCap, modulePkgMap);
        }
        else if (candCap.getNamespace().equals(BundleCapabilityImpl.MODULE_NAMESPACE))
        {
// TODO: FELIX3 - THIS NEXT LINE IS A HACK. IMPROVE HOW/WHEN WE CALCULATE EXPORTS.
            calculateExportedPackages(
                candCap.getModule(), allCandidates, modulePkgMap);

            // Get the candidate's package space to determine which packages
            // will be visible to the current module.
            Packages candPkgs = modulePkgMap.get(candCap.getModule());

            // We have to merge all exported packages from the candidate,
            // since the current module requires it.
            for (Entry<String, Blame> entry : candPkgs.m_exportedPkgs.entrySet())
            {
                mergeCandidatePackage(
                    current,
                    true,
                    currentReq,
                    entry.getValue().m_cap,
                    modulePkgMap);
            }

            // If the candidate requires any other bundles with reexport visibility,
            // then we also need to merge their packages too.
            for (BundleRequirementImpl req : candCap.getModule().getRequirements())
            {
                if (req.getNamespace().equals(BundleCapabilityImpl.MODULE_NAMESPACE))
                {
                    String value = req.getDirectives().get(Constants.VISIBILITY_DIRECTIVE);
                    if ((value != null) && value.equals(Constants.VISIBILITY_REEXPORT)
                        && (allCandidates.getCandidates(req) != null))
                    {
                        mergeCandidatePackages(
                            current,
                            currentReq,
                            allCandidates.getCandidates(req).iterator().next(),
                            modulePkgMap,
                            allCandidates);
                    }
                }
            }
        }
    }

    private void mergeCandidatePackage(
        Module current, boolean requires,
        BundleRequirementImpl currentReq, BundleCapabilityImpl candCap,
        Map<Module, Packages> modulePkgMap)
    {
        if (candCap.getNamespace().equals(BundleCapabilityImpl.PACKAGE_NAMESPACE))
        {
            String pkgName = (String)
                candCap.getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR);

            // Since this capability represents a package, it will become
            // a hard constraint on the module's package space, so we need
            // to make sure it doesn't conflict with any other hard constraints
            // or any other uses constraints.

            List blameReqs = new ArrayList();
            blameReqs.add(currentReq);

            //
            // First, check to see if the capability conflicts with
            // any existing hard constraints.
            //

            Packages currentPkgs = modulePkgMap.get(current);

            if (requires)
            {
                List<Blame> currentRequiredBlames = currentPkgs.m_requiredPkgs.get(pkgName);
                if (currentRequiredBlames == null)
                {
                    currentRequiredBlames = new ArrayList<Blame>();
                    currentPkgs.m_requiredPkgs.put(pkgName, currentRequiredBlames);
                }
                currentRequiredBlames.add(new Blame(candCap, blameReqs));
            }
            else
            {
                List<Blame> currentImportedBlames = currentPkgs.m_importedPkgs.get(pkgName);
                if (currentImportedBlames == null)
                {
                    currentImportedBlames = new ArrayList<Blame>();
                    currentPkgs.m_importedPkgs.put(pkgName, currentImportedBlames);
                }
                currentImportedBlames.add(new Blame(candCap, blameReqs));
            }

//dumpModulePkgs(current, currentPkgs);
        }
    }

    private void mergeUses(
        Module current, Packages currentPkgs,
        BundleCapabilityImpl mergeCap, List<BundleRequirementImpl> blameReqs,
        Map<Module, Packages> modulePkgMap,
        Candidates allCandidates,
        Map<BundleCapabilityImpl, List<Module>> cycleMap)
    {
        if (!mergeCap.getNamespace().equals(BundleCapabilityImpl.PACKAGE_NAMESPACE))
        {
            return;
        }
        // If the candidate module is the same as the current module,
        // then we don't need to verify and merge the uses constraints
        // since this will happen as we build up the package space.
        else if (current == mergeCap.getModule())
        {
            return;
        }

        // Check for cycles.
        List<Module> list = cycleMap.get(mergeCap);
        if ((list != null) && list.contains(current))
        {
            return;
        }
        list = (list == null) ? new ArrayList<Module>() : list;
        list.add(current);
        cycleMap.put(mergeCap, list);

        for (BundleCapabilityImpl candSourceCap : getPackageSources(mergeCap, modulePkgMap))
        {
            for (String usedPkgName : candSourceCap.getUses())
            {
                Packages candSourcePkgs = modulePkgMap.get(candSourceCap.getModule());
                Blame candExportedBlame = candSourcePkgs.m_exportedPkgs.get(usedPkgName);
                List<Blame> candSourceBlames = null;
                if (candExportedBlame != null)
                {
                    candSourceBlames = new ArrayList(1);
                    candSourceBlames.add(candExportedBlame);
                }
                else
                {
                    candSourceBlames = candSourcePkgs.m_importedPkgs.get(usedPkgName);
                }

                if (candSourceBlames == null)
                {
                    continue;
                }

                List<Blame> usedCaps = currentPkgs.m_usedPkgs.get(usedPkgName);
                if (usedCaps == null)
                {
                    usedCaps = new ArrayList<Blame>();
                    currentPkgs.m_usedPkgs.put(usedPkgName, usedCaps);
                }
                for (Blame blame : candSourceBlames)
                {
                    if (blame.m_reqs != null)
                    {
                        List<BundleRequirementImpl> blameReqs2 = new ArrayList(blameReqs);
                        blameReqs2.add(blame.m_reqs.get(blame.m_reqs.size() - 1));
                        usedCaps.add(new Blame(blame.m_cap, blameReqs2));
                        mergeUses(current, currentPkgs, blame.m_cap, blameReqs2,
                            modulePkgMap, allCandidates, cycleMap);
                    }
                    else
                    {
                        usedCaps.add(new Blame(blame.m_cap, blameReqs));
                        mergeUses(current, currentPkgs, blame.m_cap, blameReqs,
                            modulePkgMap, allCandidates, cycleMap);
                    }
                }
            }
        }
    }

    private void checkPackageSpaceConsistency(
        boolean isDynamicImport,
        Module module,
        Candidates allCandidates,
        Map<Module, Packages> modulePkgMap,
        Map<Module, Object> resultCache)
    {
        if (module.isResolved() && !isDynamicImport)
        {
            return;
        }
        else if(resultCache.containsKey(module))
        {
            return;
        }

        Packages pkgs = modulePkgMap.get(module);

        ResolveException rethrow = null;
        Candidates permutation = null;
        Set<BundleRequirementImpl> mutated = null;

        // Check for conflicting imports from fragments.
        for (Entry<String, List<Blame>> entry : pkgs.m_importedPkgs.entrySet())
        {
            if (entry.getValue().size() > 1)
            {
                Blame sourceBlame = null;
                for (Blame blame : entry.getValue())
                {
                    if (sourceBlame == null)
                    {
                        sourceBlame = blame;
                    }
                    else if (!sourceBlame.m_cap.getModule().equals(blame.m_cap.getModule()))
                    {
                        // Try to permutate the conflicting requirement.
                        permutate(allCandidates, blame.m_reqs.get(0), m_importPermutations);
                        // Try to permutate the source requirement.
                        permutate(allCandidates, sourceBlame.m_reqs.get(0), m_importPermutations);
                        // Report conflict.
                        ResolveException ex = new ResolveException(
                            "Uses constraint violation. Unable to resolve module "
                            + module.getSymbolicName()
                            + " [" + module
                            + "] because it is exposed to package '"
                            + entry.getKey()
                            + "' from modules "
                            + sourceBlame.m_cap.getModule().getSymbolicName()
                            + " [" + sourceBlame.m_cap.getModule()
                            + "] and "
                            + blame.m_cap.getModule().getSymbolicName()
                            + " [" + blame.m_cap.getModule()
                            + "] via two dependency chains.\n\nChain 1:\n"
                            + toStringBlame(sourceBlame)
                            + "\n\nChain 2:\n"
                            + toStringBlame(blame),
                            module,
                            blame.m_reqs.get(0));
                        m_logger.log(
                            Logger.LOG_DEBUG,
                            "Candidate permutation failed due to a conflict with a "
                            + "fragment import; will try another if possible.",
                            ex);
                        throw ex;
                    }
                }
            }
        }

        for (Entry<String, Blame> entry : pkgs.m_exportedPkgs.entrySet())
        {
            String pkgName = entry.getKey();
            Blame exportBlame = entry.getValue();
            if (!pkgs.m_usedPkgs.containsKey(pkgName))
            {
                continue;
            }
            for (Blame usedBlame : pkgs.m_usedPkgs.get(pkgName))
            {
                if (!isCompatible(exportBlame.m_cap, usedBlame.m_cap, modulePkgMap))
                {
                    // Create a candidate permutation that eliminates all candidates
                    // that conflict with existing selected candidates.
                    permutation = (permutation != null)
                        ? permutation
                        : allCandidates.copy();
                    rethrow = (rethrow != null)
                        ? rethrow
                        : new ResolveException(
                            "Uses constraint violation. Unable to resolve module "
                            + module.getSymbolicName()
                            + " [" + module
                            + "] because it exports package '"
                            + pkgName
                            + "' and is also exposed to it from module "
                            + usedBlame.m_cap.getModule().getSymbolicName()
                            + " [" + usedBlame.m_cap.getModule()
                            + "] via the following dependency chain:\n\n"
                            + toStringBlame(usedBlame),
                            null,
                            null);

                    mutated = (mutated != null)
                        ? mutated
                        : new HashSet();

                    for (int reqIdx = usedBlame.m_reqs.size() - 1; reqIdx >= 0; reqIdx--)
                    {
                        BundleRequirementImpl req = usedBlame.m_reqs.get(reqIdx);

                        // If we've already permutated this requirement in another
                        // uses constraint, don't permutate it again just continue
                        // with the next uses constraint.
                        if (mutated.contains(req))
                        {
                            break;
                        }

                        // See if we can permutate the candidates for blamed
                        // requirement; there may be no candidates if the module
                        // associated with the requirement is already resolved.
                        SortedSet<BundleCapabilityImpl> candidates =
                            permutation.getCandidates(req);
                        if ((candidates != null) && (candidates.size() > 1))
                        {
                            mutated.add(req);
                            Iterator it = candidates.iterator();
                            it.next();
                            it.remove();
                            // Continue with the next uses constraint.
                            break;
                        }
                    }
                }
            }

            if (rethrow != null)
            {
                if (mutated.size() > 0)
                {
                    m_usesPermutations.add(permutation);
                }
                m_logger.log(
                    Logger.LOG_DEBUG,
                    "Candidate permutation failed due to a conflict between "
                    + "an export and import; will try another if possible.",
                    rethrow);
                throw rethrow;
            }
        }

        // Check if there are any uses conflicts with imported packages.
        for (Entry<String, List<Blame>> entry : pkgs.m_importedPkgs.entrySet())
        {
            for (Blame importBlame : entry.getValue())
            {
                String pkgName = entry.getKey();
                if (!pkgs.m_usedPkgs.containsKey(pkgName))
                {
                    continue;
                }
                for (Blame usedBlame : pkgs.m_usedPkgs.get(pkgName))
                {
                    if (!isCompatible(importBlame.m_cap, usedBlame.m_cap, modulePkgMap))
                    {
                        // Create a candidate permutation that eliminates any candidates
                        // that conflict with existing selected candidates.
                        permutation = (permutation != null)
                            ? permutation
                            : allCandidates.copy();
                        rethrow = (rethrow != null)
                            ? rethrow
                            : new ResolveException(
                                "Uses constraint violation. Unable to resolve module "
                                + module.getSymbolicName()
                                + " [" + module
                                + "] because it is exposed to package '"
                                + pkgName
                                + "' from modules "
                                + importBlame.m_cap.getModule().getSymbolicName()
                                + " [" + importBlame.m_cap.getModule()
                                + "] and "
                                + usedBlame.m_cap.getModule().getSymbolicName()
                                + " [" + usedBlame.m_cap.getModule()
                                + "] via two dependency chains.\n\nChain 1:\n"
                                + toStringBlame(importBlame)
                                + "\n\nChain 2:\n"
                                + toStringBlame(usedBlame),
                                null,
                                null);

                        mutated = (mutated != null)
                            ? mutated
                            : new HashSet();

                        for (int reqIdx = usedBlame.m_reqs.size() - 1; reqIdx >= 0; reqIdx--)
                        {
                            BundleRequirementImpl req = usedBlame.m_reqs.get(reqIdx);

                            // If we've already permutated this requirement in another
                            // uses constraint, don't permutate it again just continue
                            // with the next uses constraint.
                            if (mutated.contains(req))
                            {
                                break;
                            }

                            // See if we can permutate the candidates for blamed
                            // requirement; there may be no candidates if the module
                            // associated with the requirement is already resolved.
                            SortedSet<BundleCapabilityImpl> candidates =
                                permutation.getCandidates(req);
                            if ((candidates != null) && (candidates.size() > 1))
                            {
                                mutated.add(req);
                                Iterator it = candidates.iterator();
                                it.next();
                                it.remove();
                                // Continue with the next uses constraint.
                                break;
                            }
                        }
                    }
                }

                // If there was a uses conflict, then we should add a uses
                // permutation if we were able to permutate any candidates.
                // Additionally, we should try to push an import permutation
                // for the original import to force a backtracking on the
                // original candidate decision if no viable candidate is found
                // for the conflicting uses constraint.
                if (rethrow != null)
                {
                    // Add uses permutation if we mutated any candidates.
                    if (mutated.size() > 0)
                    {
                        m_usesPermutations.add(permutation);
                    }

                    // Try to permutate the candidate for the original
                    // import requirement; only permutate it if we haven't
                    // done so already.
                    BundleRequirementImpl req = importBlame.m_reqs.get(0);
                    if (!mutated.contains(req))
                    {
                        // Since there may be lots of uses constraint violations
                        // with existing import decisions, we may end up trying
                        // to permutate the same import a lot of times, so we should
                        // try to check if that the case and only permutate it once.
                        permutateIfNeeded(allCandidates, req, m_importPermutations);
                    }

                    m_logger.log(
                        Logger.LOG_DEBUG,
                        "Candidate permutation failed due to a conflict between "
                        + "imports; will try another if possible.",
                        rethrow);
                    throw rethrow;
                }
            }
        }

        resultCache.put(module, Boolean.TRUE);

        // Now check the consistency of all modules on which the
        // current module depends. Keep track of the current number
        // of permutations so we know if the lower level check was
        // able to create a permutation or not in the case of failure.
        int permCount = m_usesPermutations.size() + m_importPermutations.size();
        for (Entry<String, List<Blame>> entry : pkgs.m_importedPkgs.entrySet())
        {
            for (Blame importBlame : entry.getValue())
            {
                if (!module.equals(importBlame.m_cap.getModule()))
                {
                    try
                    {
                        checkPackageSpaceConsistency(
                            false, importBlame.m_cap.getModule(),
                            allCandidates, modulePkgMap, resultCache);
                    }
                    catch (ResolveException ex)
                    {
                        // If the lower level check didn't create any permutations,
                        // then we should create an import permutation for the
                        // requirement with the dependency on the failing module
                        // to backtrack on our current candidate selection.
                        if (permCount == (m_usesPermutations.size() + m_importPermutations.size()))
                        {
                            BundleRequirementImpl req = importBlame.m_reqs.get(0);
                            permutate(allCandidates, req, m_importPermutations);
                        }
                        throw ex;
                    }
                }
            }
        }
    }

    private static void permutate(
        Candidates allCandidates, BundleRequirementImpl req, List<Candidates> permutations)
    {
        SortedSet<BundleCapabilityImpl> candidates = allCandidates.getCandidates(req);
        if (candidates.size() > 1)
        {
            Candidates perm = allCandidates.copy();
            candidates = perm.getCandidates(req);
            Iterator it = candidates.iterator();
            it.next();
            it.remove();
            permutations.add(perm);
        }
    }

    private static void permutateIfNeeded(
        Candidates allCandidates, BundleRequirementImpl req, List<Candidates> permutations)
    {
        SortedSet<BundleCapabilityImpl> candidates = allCandidates.getCandidates(req);
        if (candidates.size() > 1)
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
                Set<BundleCapabilityImpl> existingPermCands = existingPerm.getCandidates(req);
                if (!existingPermCands.iterator().next().equals(candidates.iterator().next()))
                {
                    permutated = true;
                }
            }
            // If we haven't already permutated the existing
            // import, do so now.
            if (!permutated)
            {
                permutate(allCandidates, req, permutations);
            }
        }
    }

    private static void calculateExportedPackages(
        Module module,
        Candidates allCandidates,
        Map<Module, Packages> modulePkgMap)
    {
        Packages packages = modulePkgMap.get(module);
        if (packages != null)
        {
            return;
        }
        packages = new Packages(module);

        // Get all exported packages.
        Map<String, BundleCapabilityImpl> exports =
            new HashMap<String, BundleCapabilityImpl>(module.getCapabilities().size());
        for (BundleCapabilityImpl cap : module.getCapabilities())
        {
            if (cap.getNamespace().equals(BundleCapabilityImpl.PACKAGE_NAMESPACE))
            {
                exports.put(
                    (String) cap.getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR),
                    cap);
            }
        }
        // Remove substitutable exports that were imported.
        // For resolved modules look at the wires, for resolving
        // modules look in the candidate map to determine which
        // exports are substitutable.
        if (module.isResolved())
        {
            for (Wire wire : module.getWires())
            {
                if (wire.getRequirement().getNamespace().equals(BundleCapabilityImpl.PACKAGE_NAMESPACE))
                {
                    String pkgName = (String) wire.getCapability()
                        .getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR);
                    exports.remove(pkgName);
                }
            }
        }
        else
        {
            for (BundleRequirementImpl req : module.getRequirements())
            {
                if (req.getNamespace().equals(BundleCapabilityImpl.PACKAGE_NAMESPACE))
                {
                    Set<BundleCapabilityImpl> cands = allCandidates.getCandidates(req);
                    if ((cands != null) && !cands.isEmpty())
                    {
                        String pkgName = (String) cands.iterator().next()
                            .getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR);
                        exports.remove(pkgName);
                    }
                }
            }
        }
        // Add all non-substituted exports to the module's package space.
        for (Entry<String, BundleCapabilityImpl> entry : exports.entrySet())
        {
            packages.m_exportedPkgs.put(
                entry.getKey(), new Blame(entry.getValue(), null));
        }

        modulePkgMap.put(module, packages);
    }

    private boolean isCompatible(
        BundleCapabilityImpl currentCap, BundleCapabilityImpl candCap,
        Map<Module, Packages> modulePkgMap)
    {
        if ((currentCap != null) && (candCap != null))
        {
            if (currentCap.equals(candCap))
            {
                return true;
            }

            List<BundleCapabilityImpl> currentSources =
                getPackageSources(
                    currentCap,
                    modulePkgMap);
            List<BundleCapabilityImpl> candSources =
                getPackageSources(
                    candCap,
                    modulePkgMap);

            return currentSources.containsAll(candSources)
                || candSources.containsAll(currentSources);
        }
        return true;
    }

    private Map<BundleCapabilityImpl, List<BundleCapabilityImpl>> m_packageSourcesCache
        = new HashMap();

    private List<BundleCapabilityImpl> getPackageSources(
        BundleCapabilityImpl cap, Map<Module, Packages> modulePkgMap)
    {
        if (cap.getNamespace().equals(BundleCapabilityImpl.PACKAGE_NAMESPACE))
        {
            List<BundleCapabilityImpl> sources = m_packageSourcesCache.get(cap);
            if (sources == null)
            {
                sources = getPackageSourcesInternal(
                    cap, modulePkgMap, new ArrayList(), new HashSet());
                m_packageSourcesCache.put(cap, sources);
            }
            return sources;
        }

        return Collections.EMPTY_LIST;
    }

    private static List<BundleCapabilityImpl> getPackageSourcesInternal(
        BundleCapabilityImpl cap, Map<Module, Packages> modulePkgMap,
        List<BundleCapabilityImpl> sources, Set<BundleCapabilityImpl> cycleMap)
    {
        if (cap.getNamespace().equals(BundleCapabilityImpl.PACKAGE_NAMESPACE))
        {
            if (cycleMap.contains(cap))
            {
                return sources;
            }
            cycleMap.add(cap);

            // Get the package name associated with the capability.
            String pkgName = cap.getAttributes()
                .get(BundleCapabilityImpl.PACKAGE_ATTR).toString();

            // Since a module can export the same package more than once, get
            // all package capabilities for the specified package name.
            List<BundleCapabilityImpl> caps = cap.getModule().getCapabilities();
            for (int capIdx = 0; capIdx < caps.size(); capIdx++)
            {
                if (caps.get(capIdx).getNamespace().equals(BundleCapabilityImpl.PACKAGE_NAMESPACE)
                    && caps.get(capIdx).getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR).equals(pkgName))
                {
                    sources.add(caps.get(capIdx));
                }
            }

            // Then get any addition sources for the package from required bundles.
            Packages pkgs = modulePkgMap.get(cap.getModule());
            List<Blame> required = pkgs.m_requiredPkgs.get(pkgName);
            if (required != null)
            {
                for (Blame blame : required)
                {
                    getPackageSourcesInternal(blame.m_cap, modulePkgMap, sources, cycleMap);
                }
            }
        }

        return sources;
    }

    private static Module getActualModule(Module m)
    {
        if (m instanceof HostModule)
        {
            return ((HostModule) m).getHost();
        }
        return m;
    }

    private static BundleCapabilityImpl getActualCapability(BundleCapabilityImpl c)
    {
        if (c instanceof HostedCapability)
        {
            return ((HostedCapability) c).getDeclaredCapability();
        }
        return c;
    }

    private static BundleRequirementImpl getActualRequirement(BundleRequirementImpl r)
    {
        if (r instanceof HostedRequirement)
        {
            return ((HostedRequirement) r).getDeclaredRequirement();
        }
        return r;
    }

    private static Map<Module, List<Wire>> populateWireMap(
        Module module, Map<Module, Packages> modulePkgMap,
        Map<Module, List<Wire>> wireMap,
        Candidates allCandidates)
    {
        Module unwrappedModule = getActualModule(module);
        if (!unwrappedModule.isResolved() && !wireMap.containsKey(unwrappedModule))
        {
            wireMap.put(unwrappedModule, (List<Wire>) Collections.EMPTY_LIST);

            List<Wire> packageWires = new ArrayList<Wire>();
            List<Wire> moduleWires = new ArrayList<Wire>();

            for (BundleRequirementImpl req : module.getRequirements())
            {
                SortedSet<BundleCapabilityImpl> cands = allCandidates.getCandidates(req);
                if ((cands != null) && (cands.size() > 0))
                {
                    BundleCapabilityImpl cand = cands.iterator().next();
                    if (!cand.getModule().isResolved())
                    {
                        populateWireMap(cand.getModule(),
                            modulePkgMap, wireMap, allCandidates);
                    }
                    // Ignore modules that import themselves.
                    if (req.getNamespace().equals(BundleCapabilityImpl.PACKAGE_NAMESPACE)
                        && !module.equals(cand.getModule()))
                    {
                        packageWires.add(
                            new WireImpl(
                                unwrappedModule,
                                getActualRequirement(req),
                                getActualModule(cand.getModule()),
                                getActualCapability(cand)));
                    }
                    else if (req.getNamespace().equals(BundleCapabilityImpl.MODULE_NAMESPACE))
                    {
                        Packages candPkgs = modulePkgMap.get(cand.getModule());
                        moduleWires.add(
                            new WireModuleImpl(
                                unwrappedModule,
                                getActualRequirement(req),
                                getActualModule(cand.getModule()),
                                getActualCapability(cand),
                                candPkgs.getExportedAndReexportedPackages()));
                    }
                }
            }

            // Combine wires with module wires last.
            packageWires.addAll(moduleWires);
            wireMap.put(unwrappedModule, packageWires);

            // Add host wire for any fragments.
            if (module instanceof HostModule)
            {
                List<Module> fragments = ((HostModule) module).getFragments();
                for (Module fragment : fragments)
                {
                    List<Wire> hostWires = wireMap.get(fragment);
                    if (hostWires == null)
                    {
                        hostWires = new ArrayList<Wire>();
                        wireMap.put(fragment, hostWires);
                    }
                    hostWires.add(
                        new WireImpl(
                            getActualModule(fragment),
                            getHostRequirement(fragment),
                            unwrappedModule,
                            getHostCapability(unwrappedModule)));
                }
            }
        }

        return wireMap;
    }

    private static Map<Module, List<Wire>> populateDynamicWireMap(
        Module module, String pkgName, Map<Module, Packages> modulePkgMap,
        Map<Module, List<Wire>> wireMap, Candidates allCandidates)
    {
        wireMap.put(module, (List<Wire>) Collections.EMPTY_LIST);

        List<Wire> packageWires = new ArrayList<Wire>();

        Packages pkgs = modulePkgMap.get(module);
        for (Entry<String, List<Blame>> entry : pkgs.m_importedPkgs.entrySet())
        {
            for (Blame blame : entry.getValue())
            {
                // Ignore modules that import themselves.
                if (!module.equals(blame.m_cap.getModule())
                    && blame.m_cap.getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR).equals(pkgName))
                {
                    if (!blame.m_cap.getModule().isResolved())
                    {
                        populateWireMap(blame.m_cap.getModule(), modulePkgMap, wireMap,
                            allCandidates);
                    }

                    Map<String, Object> attrs = new HashMap(1);
                    attrs.put(BundleCapabilityImpl.PACKAGE_ATTR, pkgName);
                    packageWires.add(
                        new WireImpl(
                            module,
                            // We need an unique requirement here or else subsequent
                            // dynamic imports for the same dynamic requirement will
                            // conflict with previous ones.
                            new BundleRequirementImpl(
                                module,
                                BundleCapabilityImpl.PACKAGE_NAMESPACE,
                                Collections.EMPTY_MAP,
                                attrs),
                            getActualModule(blame.m_cap.getModule()),
                            getActualCapability(blame.m_cap)));
                }
            }
        }

        wireMap.put(module, packageWires);

        return wireMap;
    }

    private static void dumpModulePkgMap(Map<Module, Packages> modulePkgMap)
    {
        System.out.println("+++MODULE PKG MAP+++");
        for (Entry<Module, Packages> entry : modulePkgMap.entrySet())
        {
            dumpModulePkgs(entry.getKey(), entry.getValue());
        }
    }

    private static void dumpModulePkgs(Module module, Packages packages)
    {
        System.out.println(module + " (" + (module.isResolved() ? "RESOLVED)" : "UNRESOLVED)"));
        System.out.println("  EXPORTED");
        for (Entry<String, Blame> entry : packages.m_exportedPkgs.entrySet())
        {
            System.out.println("    " + entry.getKey() + " - " + entry.getValue());
        }
        System.out.println("  IMPORTED");
        for (Entry<String, List<Blame>> entry : packages.m_importedPkgs.entrySet())
        {
            System.out.println("    " + entry.getKey() + " - " + entry.getValue());
        }
        System.out.println("  REQUIRED");
        for (Entry<String, List<Blame>> entry : packages.m_requiredPkgs.entrySet())
        {
            System.out.println("    " + entry.getKey() + " - " + entry.getValue());
        }
        System.out.println("  USED");
        for (Entry<String, List<Blame>> entry : packages.m_usedPkgs.entrySet())
        {
            System.out.println("    " + entry.getKey() + " - " + entry.getValue());
        }
    }

    private static String toStringBlame(Blame blame)
    {
        StringBuffer sb = new StringBuffer();
        if ((blame.m_reqs != null) && !blame.m_reqs.isEmpty())
        {
            for (int i = 0; i < blame.m_reqs.size(); i++)
            {
                BundleRequirementImpl req = blame.m_reqs.get(i);
                sb.append("  ");
                sb.append(req.getModule().getSymbolicName());
                sb.append(" [");
                sb.append(req.getModule().toString());
                sb.append("]\n");
                if (req.getNamespace().equals(BundleCapabilityImpl.PACKAGE_NAMESPACE))
                {
                    sb.append("    import: ");
                }
                else
                {
                    sb.append("    require: ");
                }
                sb.append(req.getFilter().toString());
                sb.append("\n     |");
                if (req.getNamespace().equals(BundleCapabilityImpl.PACKAGE_NAMESPACE))
                {
                    sb.append("\n    export: ");
                }
                else
                {
                    sb.append("\n    provide: ");
                }
                if ((i + 1) < blame.m_reqs.size())
                {
                    BundleCapabilityImpl cap = Util.getSatisfyingCapability(
                        blame.m_reqs.get(i + 1).getModule(),
                        blame.m_reqs.get(i));
                    if (cap.getNamespace().equals(BundleCapabilityImpl.PACKAGE_NAMESPACE))
                    {
                        sb.append(BundleCapabilityImpl.PACKAGE_ATTR);
                        sb.append("=");
                        sb.append(cap.getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR).toString());
                        BundleCapabilityImpl usedCap;
                        if ((i + 2) < blame.m_reqs.size())
                        {
                            usedCap = Util.getSatisfyingCapability(
                                blame.m_reqs.get(i + 2).getModule(),
                                blame.m_reqs.get(i + 1));
                        }
                        else
                        {
                            usedCap = Util.getSatisfyingCapability(
                                blame.m_cap.getModule(),
                                blame.m_reqs.get(i + 1));
                        }
                        sb.append("; uses:=");
                        sb.append(usedCap.getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR));
                    }
                    else
                    {
                        sb.append(cap);
                    }
                    sb.append("\n");
                }
                else
                {
                    BundleCapabilityImpl export = Util.getSatisfyingCapability(
                        blame.m_cap.getModule(),
                        blame.m_reqs.get(i));
                    sb.append(BundleCapabilityImpl.PACKAGE_ATTR);
                    sb.append("=");
                    sb.append(export.getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR).toString());
                    if (!export.getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR)
                        .equals(blame.m_cap.getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR)))
                    {
                        sb.append("; uses:=");
                        sb.append(blame.m_cap.getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR));
                        sb.append("\n    export: ");
                        sb.append(BundleCapabilityImpl.PACKAGE_ATTR);
                        sb.append("=");
                        sb.append(blame.m_cap.getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR).toString());
                    }
                    sb.append("\n  ");
                    sb.append(blame.m_cap.getModule().getSymbolicName());
                    sb.append(" [");
                    sb.append(blame.m_cap.getModule().toString());
                    sb.append("]");
                }
            }
        }
        else
        {
            sb.append(blame.m_cap.getModule().toString());
        }
        return sb.toString();
    }

    private static class Packages
    {
        private final Module m_module;
        public final Map<String, Blame> m_exportedPkgs = new HashMap();
        public final Map<String, List<Blame>> m_importedPkgs = new HashMap();
        public final Map<String, List<Blame>> m_requiredPkgs = new HashMap();
        public final Map<String, List<Blame>> m_usedPkgs = new HashMap();

        public Packages(Module module)
        {
            m_module = module;
        }

        public List<String> getExportedAndReexportedPackages()
        {
            List<String> pkgs = new ArrayList();
            // Grab the module's actual exported packages.
            // Note that we ignore the calculated exported packages here,
            // because bundles that import their own exports still continue
            // to provide access to their exports when they are required; i.e.,
            // the implicitly reexport the packages if wired to another provider.
            for (BundleCapabilityImpl cap : m_module.getCapabilities())
            {
                if (cap.getNamespace().equals(BundleCapabilityImpl.PACKAGE_NAMESPACE))
                {
                    pkgs.add((String)
                        cap.getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR));
                }
            }
            // Grab all required and reexported required packages.
            for (Entry<String, List<Blame>> entry : m_requiredPkgs.entrySet())
            {
                for (Blame blame : entry.getValue())
                {
                    String value = blame.m_reqs.get(
                        blame.m_reqs.size() - 1).getDirectives().get(Constants.VISIBILITY_DIRECTIVE);
                    if ((value != null)
                        && value.equals(Constants.VISIBILITY_REEXPORT))
                    {
                        pkgs.add((String)
                            blame.m_cap.getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR));
                        break;
                    }
                }
            }
            return pkgs;
        }
    }

    private static class Blame
    {
        public final BundleCapabilityImpl m_cap;
        public final List<BundleRequirementImpl> m_reqs;

        public Blame(BundleCapabilityImpl cap, List<BundleRequirementImpl> reqs)
        {
            m_cap = cap;
            m_reqs = reqs;
        }

        @Override
        public String toString()
        {
            return m_cap.getModule()
                + "." + m_cap.getAttributes().get(BundleCapabilityImpl.PACKAGE_ATTR)
                + (((m_reqs == null) || m_reqs.isEmpty())
                    ? " NO BLAME"
                    : " BLAMED ON " + m_reqs);
        }

        @Override
        public boolean equals(Object o)
        {
            return (o instanceof Blame) && m_reqs.equals(((Blame) o).m_reqs)
                && m_cap.equals(((Blame) o).m_cap);
        }
    }
}