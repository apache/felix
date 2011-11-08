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
import org.apache.felix.framework.BundleWiringImpl;
import org.apache.felix.framework.Logger;
import org.apache.felix.framework.capabilityset.CapabilitySet;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.wiring.BundleCapabilityImpl;
import org.apache.felix.framework.wiring.BundleRequirementImpl;
import org.osgi.framework.Constants;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleRequirement;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWire;

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

    public Map<BundleRevision, List<ResolverWire>> resolve(
        ResolverState state,
        Set<BundleRevision> mandatoryRevisions,
        Set<BundleRevision> optionalRevisions,
        Set<BundleRevision> ondemandFragments)
    {
        Map<BundleRevision, List<ResolverWire>> wireMap =
            new HashMap<BundleRevision, List<ResolverWire>>();
        Map<BundleRevision, Packages> revisionPkgMap =
            new HashMap<BundleRevision, Packages>();

        boolean retry;
        do
        {
            retry = false;

            try
            {
                // Create object to hold all candidates.
                Candidates allCandidates = new Candidates();

                // Populate mandatory revisions; since these are mandatory
                // revisions, failure throws a resolve exception.
                for (Iterator<BundleRevision> it = mandatoryRevisions.iterator();
                    it.hasNext(); )
                {
                    BundleRevision br = it.next();
                    if (Util.isFragment(br) || (br.getWiring() == null))
                    {
                        allCandidates.populate(state, br, Candidates.MANDATORY);
                    }
                    else
                    {
                        it.remove();
                    }
                }

                // Populate optional revisions; since these are optional
                // revisions, failure does not throw a resolve exception.
                for (BundleRevision br : optionalRevisions)
                {
                    boolean isFragment = Util.isFragment(br);
                    if (isFragment || (br.getWiring() == null))
                    {
                        allCandidates.populate(state, br, Candidates.OPTIONAL);
                    }
                }

                // Populate ondemand fragments; since these are optional
                // revisions, failure does not throw a resolve exception.
                for (BundleRevision br : ondemandFragments)
                {
                    boolean isFragment = Util.isFragment(br);
                    if (isFragment)
                    {
                        allCandidates.populate(state, br, Candidates.ON_DEMAND);
                    }
                }

                // Merge any fragments into hosts.
                allCandidates.prepare();

                // Create a combined list of populated revisions; for
                // optional revisions. We do not need to consider ondemand
                // fragments, since they will only be pulled in if their
                // host is already present.
                Set<BundleRevision> allRevisions =
                    new HashSet<BundleRevision>(mandatoryRevisions);
                for (BundleRevision br : optionalRevisions)
                {
                    if (allCandidates.isPopulated(br))
                    {
                        allRevisions.add(br);
                    }
                }

                // Record the initial candidate permutation.
                m_usesPermutations.add(allCandidates);

                ResolveException rethrow = null;

                // If a populated revision is a fragment, then its host
                // must ultimately be verified, so store its host requirement
                // to use for package space calculation.
                Map<BundleRevision, List<BundleRequirement>> hostReqs =
                    new HashMap<BundleRevision, List<BundleRequirement>>();
                for (BundleRevision br : allRevisions)
                {
                    if (Util.isFragment(br))
                    {
                        hostReqs.put(
                            br,
                            br.getDeclaredRequirements(BundleRevision.HOST_NAMESPACE));
                    }
                }

                do
                {
                    rethrow = null;

                    revisionPkgMap.clear();
                    m_packageSourcesCache.clear();

                    allCandidates = (m_usesPermutations.size() > 0)
                        ? m_usesPermutations.remove(0)
                        : m_importPermutations.remove(0);
//allCandidates.dump();

                    for (BundleRevision br : allRevisions)
                    {
                        BundleRevision target = br;

                        // If we are resolving a fragment, then get its
                        // host candidate and verify it instead.
                        List<BundleRequirement> hostReq = hostReqs.get(br);
                        if (hostReq != null)
                        {
                            target = allCandidates.getCandidates(hostReq.get(0))
                                .iterator().next().getRevision();
                        }

                        calculatePackageSpaces(
                            allCandidates.getWrappedHost(target), allCandidates, revisionPkgMap,
                            new HashMap(), new HashSet());
//System.out.println("+++ PACKAGE SPACES START +++");
//dumpRevisionPkgMap(revisionPkgMap);
//System.out.println("+++ PACKAGE SPACES END +++");

                        try
                        {
                            checkPackageSpaceConsistency(
                                false, allCandidates.getWrappedHost(target),
                                allCandidates, revisionPkgMap, new HashMap());
                        }
                        catch (ResolveException ex)
                        {
                            rethrow = ex;
                        }
                    }
                }
                while ((rethrow != null)
                    && ((m_usesPermutations.size() > 0) || (m_importPermutations.size() > 0)));

                // If there is a resolve exception, then determine if an
                // optionally resolved revision is to blame (typically a fragment).
                // If so, then remove the optionally resolved resolved and try
                // again; otherwise, rethrow the resolve exception.
                if (rethrow != null)
                {
                    BundleRevision faultyRevision =
                        getActualBundleRevision(rethrow.getRevision());
                    if (rethrow.getRequirement() instanceof HostedRequirement)
                    {
                        faultyRevision =
                            ((HostedRequirement) rethrow.getRequirement())
                                .getOriginalRequirement().getRevision();
                    }
                    if (optionalRevisions.remove(faultyRevision))
                    {
                        retry = true;
                    }
                    else if (ondemandFragments.remove(faultyRevision))
                    {
                        retry = true;
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
                    for (BundleRevision br : allRevisions)
                    {
                        BundleRevision target = br;

                        // If we are resolving a fragment, then we
                        // actually want to populate its host's wires.
                        List<BundleRequirement> hostReq = hostReqs.get(br);
                        if (hostReq != null)
                        {
                            target = allCandidates.getCandidates(hostReq.get(0))
                                .iterator().next().getRevision();
                        }

                        if (allCandidates.isPopulated(target))
                        {
                            wireMap =
                                populateWireMap(
                                    allCandidates.getWrappedHost(target),
                                    revisionPkgMap, wireMap, allCandidates);
                        }
                    }
                }
            }
            finally
            {
                // Always clear the state.
                m_usesPermutations.clear();
                m_importPermutations.clear();
            }
        }
        while (retry);

        return wireMap;
    }

    public Map<BundleRevision, List<ResolverWire>> resolve(
        ResolverState state, BundleRevision revision, String pkgName,
        Set<BundleRevision> ondemandFragments)
    {
        // We can only create a dynamic import if the following
        // conditions are met:
        // 1. The specified revision is resolved.
        // 2. The package in question is not already imported.
        // 3. The package in question is not accessible via require-bundle.
        // 4. The package in question is not exported by the revision.
        // 5. The package in question matches a dynamic import of the revision.
        // The following call checks all of these conditions and returns
        // the associated dynamic import and matching capabilities.
        Candidates allCandidates =
            getDynamicImportCandidates(state, revision, pkgName);
        if (allCandidates != null)
        {
            Map<BundleRevision, List<ResolverWire>> wireMap = new HashMap<BundleRevision, List<ResolverWire>>();
            Map<BundleRevision, Packages> revisionPkgMap = new HashMap<BundleRevision, Packages>();

            boolean retry;
            do
            {
                retry = false;

                try
                {
                    // Try to populate optional fragments.
                    for (BundleRevision br : ondemandFragments)
                    {
                        if (Util.isFragment(br))
                        {
                            allCandidates.populate(state, br, Candidates.ON_DEMAND);
                        }
                    }

                    // Merge any fragments into hosts.
                    allCandidates.prepare();

                    // Record the initial candidate permutation.
                    m_usesPermutations.add(allCandidates);

                    ResolveException rethrow = null;

                    do
                    {
                        rethrow = null;

                        revisionPkgMap.clear();
                        m_packageSourcesCache.clear();

                        allCandidates = (m_usesPermutations.size() > 0)
                            ? m_usesPermutations.remove(0)
                            : m_importPermutations.remove(0);
//allCandidates.dump();

                        // For a dynamic import, the instigating revision
                        // will never be a fragment since fragments never
                        // execute code, so we don't need to check for
                        // this case like we do for a normal resolve.

                        calculatePackageSpaces(
                            allCandidates.getWrappedHost(revision), allCandidates, revisionPkgMap,
                            new HashMap(), new HashSet());
//System.out.println("+++ PACKAGE SPACES START +++");
//dumpRevisionPkgMap(revisionPkgMap);
//System.out.println("+++ PACKAGE SPACES END +++");

                        try
                        {
                            checkPackageSpaceConsistency(
                                false, allCandidates.getWrappedHost(revision),
                                allCandidates, revisionPkgMap, new HashMap());
                        }
                        catch (ResolveException ex)
                        {
                            rethrow = ex;
                        }
                    }
                    while ((rethrow != null)
                        && ((m_usesPermutations.size() > 0) || (m_importPermutations.size() > 0)));

                    // If there is a resolve exception, then determine if an
                    // optionally resolved revision is to blame (typically a fragment).
                    // If so, then remove the optionally resolved revision and try
                    // again; otherwise, rethrow the resolve exception.
                    if (rethrow != null)
                    {
                        BundleRevision faultyRevision =
                            getActualBundleRevision(rethrow.getRevision());
                        if (rethrow.getRequirement() instanceof HostedRequirement)
                        {
                            faultyRevision =
                                ((HostedRequirement) rethrow.getRequirement())
                                    .getOriginalRequirement().getRevision();
                        }
                        if (ondemandFragments.remove(faultyRevision))
                        {
                            retry = true;
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
                            revision, pkgName, revisionPkgMap, wireMap, allCandidates);
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
            while (retry);
        }

        return null;
    }

    private static Candidates getDynamicImportCandidates(
        ResolverState state, BundleRevision revision, String pkgName)
    {
        // Unresolved revisions cannot dynamically import, nor can the default
        // package be dynamically imported.
        if ((revision.getWiring() == null) || pkgName.length() == 0)
        {
            return null;
        }

        // If the revision doesn't have dynamic imports, then just return
        // immediately.
        List<BundleRequirement> dynamics =
            Util.getDynamicRequirements(revision.getWiring().getRequirements(null));
        if ((dynamics == null) || dynamics.isEmpty())
        {
            return null;
        }

        // If the revision exports this package, then we cannot
        // attempt to dynamically import it.
        for (BundleCapability cap : revision.getWiring().getCapabilities(null))
        {
            if (cap.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE)
                && cap.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE).equals(pkgName))
            {
                return null;
            }
        }

        // If this revision already imports or requires this package, then
        // we cannot dynamically import it.
        if (((BundleWiringImpl) revision.getWiring()).hasPackageSource(pkgName))
        {
            return null;
        }

        // Determine if any providers of the package exist.
        Map<String, Object> attrs = Collections.singletonMap(
            BundleRevision.PACKAGE_NAMESPACE, (Object) pkgName);
        BundleRequirementImpl req = new BundleRequirementImpl(
            revision,
            BundleRevision.PACKAGE_NAMESPACE,
            Collections.EMPTY_MAP,
            attrs);
        SortedSet<BundleCapability> candidates = state.getCandidates(req, false);

        // Try to find a dynamic requirement that matches the capabilities.
        BundleRequirementImpl dynReq = null;
        for (int dynIdx = 0;
            (candidates.size() > 0) && (dynReq == null) && (dynIdx < dynamics.size());
            dynIdx++)
        {
            for (Iterator<BundleCapability> itCand = candidates.iterator();
                (dynReq == null) && itCand.hasNext(); )
            {
                BundleCapability cap = itCand.next();
                if (CapabilitySet.matches(
                    (BundleCapabilityImpl) cap,
                    ((BundleRequirementImpl) dynamics.get(dynIdx)).getFilter()))
                {
                    dynReq = (BundleRequirementImpl) dynamics.get(dynIdx);
                }
            }
        }

        // If we found a matching dynamic requirement, then filter out
        // any candidates that do not match it.
        if (dynReq != null)
        {
            for (Iterator<BundleCapability> itCand = candidates.iterator();
                itCand.hasNext(); )
            {
                BundleCapability cap = itCand.next();
                if (!CapabilitySet.matches(
                    (BundleCapabilityImpl) cap, dynReq.getFilter()))
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
            allCandidates = new Candidates();
            allCandidates.populateDynamic(state, revision, dynReq, candidates);
        }

        return allCandidates;
    }

    private void calculatePackageSpaces(
        BundleRevision revision,
        Candidates allCandidates,
        Map<BundleRevision, Packages> revisionPkgMap,
        Map<BundleCapability, List<BundleRevision>> usesCycleMap,
        Set<BundleRevision> cycle)
    {
        if (cycle.contains(revision))
        {
            return;
        }
        cycle.add(revision);

        // Create parallel arrays for requirement and proposed candidate
        // capability or actual capability if revision is resolved or not.
        List<BundleRequirement> reqs = new ArrayList();
        List<BundleCapability> caps = new ArrayList();
        boolean isDynamicImporting = false;
        if (revision.getWiring() != null)
        {
            // Use wires to get actual requirements and satisfying capabilities.
            for (BundleWire wire : revision.getWiring().getRequiredWires(null))
            {
                // Wrap the requirement as a hosted requirement if it comes
                // from a fragment, since we will need to know the host. We
                // also need to wrap if the requirement is a dynamic import,
                // since that requirement will be shared with any other
                // matching dynamic imports.
                BundleRequirement r = wire.getRequirement();
                if (!r.getRevision().equals(wire.getRequirerWiring().getRevision())
                    || ((r.getDirectives().get(Constants.RESOLUTION_DIRECTIVE) != null)
                        && r.getDirectives().get(Constants.RESOLUTION_DIRECTIVE).equals("dynamic")))
                {
                    r = new HostedRequirement(
                        wire.getRequirerWiring().getRevision(),
                        (BundleRequirementImpl) r);
                }
                // Wrap the capability as a hosted capability if it comes
                // from a fragment, since we will need to know the host.
                BundleCapability c = wire.getCapability();
                if (!c.getRevision().equals(wire.getProviderWiring().getRevision()))
                {
                    c = new HostedCapability(
                        wire.getProviderWiring().getRevision(),
                        (BundleCapabilityImpl) c);
                }
                reqs.add(r);
                caps.add(c);
            }

            // Since the revision is resolved, it could be dynamically importing,
            // so check to see if there are candidates for any of its dynamic
            // imports.
            for (BundleRequirement req
                : Util.getDynamicRequirements(revision.getWiring().getRequirements(null)))
            {
                // Get the candidates for the current requirement.
                SortedSet<BundleCapability> candCaps =
                    allCandidates.getCandidates((BundleRequirementImpl) req);
                // Optional requirements may not have any candidates.
                if (candCaps == null)
                {
                    continue;
                }

                BundleCapability cap = candCaps.iterator().next();
                reqs.add(req);
                caps.add(cap);
                isDynamicImporting = true;
                // Can only dynamically import one at a time, so break
                // out of the loop after the first.
                break;
            }
        }
        else
        {
            for (BundleRequirement req : revision.getDeclaredRequirements(null))
            {
                String resolution = req.getDirectives().get(Constants.RESOLUTION_DIRECTIVE);
                if ((resolution == null)
                    || !resolution.equals(FelixConstants.RESOLUTION_DYNAMIC))
                {
                    // Get the candidates for the current requirement.
                    SortedSet<BundleCapability> candCaps =
                        allCandidates.getCandidates((BundleRequirementImpl) req);
                    // Optional requirements may not have any candidates.
                    if (candCaps == null)
                    {
                        continue;
                    }

                    BundleCapability cap = candCaps.iterator().next();
                    reqs.add(req);
                    caps.add(cap);
                }
            }
        }

        // First, add all exported packages to the target revision's package space.
        calculateExportedPackages(revision, allCandidates, revisionPkgMap);
        Packages revisionPkgs = revisionPkgMap.get(revision);

        // Second, add all imported packages to the target revision's package space.
        for (int i = 0; i < reqs.size(); i++)
        {
            BundleRequirement req = reqs.get(i);
            BundleCapability cap = caps.get(i);
            calculateExportedPackages(cap.getRevision(), allCandidates, revisionPkgMap);
            mergeCandidatePackages(
                revision, req, cap, revisionPkgMap, allCandidates,
                new HashMap<BundleRevision, List<BundleCapability>>());
        }

        // Third, have all candidates to calculate their package spaces.
        for (int i = 0; i < caps.size(); i++)
        {
            calculatePackageSpaces(
                caps.get(i).getRevision(), allCandidates, revisionPkgMap,
                usesCycleMap, cycle);
        }

        // Fourth, if the target revision is unresolved or is dynamically importing,
        // then add all the uses constraints implied by its imported and required
        // packages to its package space.
        // NOTE: We do not need to do this for resolved revisions because their
        // package space is consistent by definition and these uses constraints
        // are only needed to verify the consistency of a resolving revision. The
        // only exception is if a resolved revision is dynamically importing, then
        // we need to calculate its uses constraints again to make sure the new
        // import is consistent with the existing package space.
        if ((revision.getWiring() == null) || isDynamicImporting)
        {
            // Merge uses constraints from required capabilities.
            for (int i = 0; i < reqs.size(); i++)
            {
                BundleRequirement req = reqs.get(i);
                BundleCapability cap = caps.get(i);
                // Ignore bundle/package requirements, since they are
                // considered below.
                if (!req.getNamespace().equals(BundleRevision.BUNDLE_NAMESPACE)
                    && !req.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE))
                {
                    List<BundleRequirement> blameReqs = new ArrayList();
                    blameReqs.add(req);

                    mergeUses(
                        revision,
                        revisionPkgs,
                        cap,
                        blameReqs,
                        revisionPkgMap,
                        allCandidates,
                        usesCycleMap);
                }
            }
            // Merge uses constraints from imported packages.
            for (Entry<String, List<Blame>> entry : revisionPkgs.m_importedPkgs.entrySet())
            {
                for (Blame blame : entry.getValue())
                {
                    // Ignore revisions that import from themselves.
                    if (!blame.m_cap.getRevision().equals(revision))
                    {
                        List<BundleRequirement> blameReqs = new ArrayList();
                        blameReqs.add(blame.m_reqs.get(0));

                        mergeUses(
                            revision,
                            revisionPkgs,
                            blame.m_cap,
                            blameReqs,
                            revisionPkgMap,
                            allCandidates,
                            usesCycleMap);
                    }
                }
            }
            // Merge uses constraints from required bundles.
            for (Entry<String, List<Blame>> entry : revisionPkgs.m_requiredPkgs.entrySet())
            {
                for (Blame blame : entry.getValue())
                {
                    List<BundleRequirement> blameReqs = new ArrayList();
                    blameReqs.add(blame.m_reqs.get(0));

                    mergeUses(
                        revision,
                        revisionPkgs,
                        blame.m_cap,
                        blameReqs,
                        revisionPkgMap,
                        allCandidates,
                        usesCycleMap);
                }
            }
        }
    }

    private void mergeCandidatePackages(
        BundleRevision current, BundleRequirement currentReq, BundleCapability candCap,
        Map<BundleRevision, Packages> revisionPkgMap,
        Candidates allCandidates, Map<BundleRevision, List<BundleCapability>> cycles)
    {
        List<BundleCapability> cycleCaps = cycles.get(current);
        if (cycleCaps == null)
        {
            cycleCaps = new ArrayList<BundleCapability>();
            cycles.put(current, cycleCaps);
        }
        if (cycleCaps.contains(candCap))
        {
            return;
        }
        cycleCaps.add(candCap);

        if (candCap.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE))
        {
            mergeCandidatePackage(
                current, false, currentReq, candCap, revisionPkgMap);
        }
        else if (candCap.getNamespace().equals(BundleRevision.BUNDLE_NAMESPACE))
        {
// TODO: FELIX3 - THIS NEXT LINE IS A HACK. IMPROVE HOW/WHEN WE CALCULATE EXPORTS.
            calculateExportedPackages(
                candCap.getRevision(), allCandidates, revisionPkgMap);

            // Get the candidate's package space to determine which packages
            // will be visible to the current revision.
            Packages candPkgs = revisionPkgMap.get(candCap.getRevision());

            // We have to merge all exported packages from the candidate,
            // since the current revision requires it.
            for (Entry<String, Blame> entry : candPkgs.m_exportedPkgs.entrySet())
            {
                mergeCandidatePackage(
                    current,
                    true,
                    currentReq,
                    entry.getValue().m_cap,
                    revisionPkgMap);
            }

            // If the candidate requires any other bundles with reexport visibility,
            // then we also need to merge their packages too.
            if (candCap.getRevision().getWiring() != null)
            {
                for (BundleWire bw
                    : candCap.getRevision().getWiring().getRequiredWires(null))
                {
                    if (bw.getRequirement().getNamespace()
                        .equals(BundleRevision.BUNDLE_NAMESPACE))
                    {
                        String value = bw.getRequirement()
                            .getDirectives().get(Constants.VISIBILITY_DIRECTIVE);
                        if ((value != null)
                            && value.equals(Constants.VISIBILITY_REEXPORT))
                        {
                            mergeCandidatePackages(
                                current,
                                currentReq,
                                bw.getCapability(),
                                revisionPkgMap,
                                allCandidates,
                                cycles);
                        }
                    }
                }
            }
            else
            {
                for (BundleRequirement req
                    : candCap.getRevision().getDeclaredRequirements(null))
                {
                    if (req.getNamespace().equals(BundleRevision.BUNDLE_NAMESPACE))
                    {
                        String value =
                            req.getDirectives().get(Constants.VISIBILITY_DIRECTIVE);
                        if ((value != null)
                            && value.equals(Constants.VISIBILITY_REEXPORT)
                            && (allCandidates.getCandidates(req) != null))
                        {
                            mergeCandidatePackages(
                                current,
                                currentReq,
                                allCandidates.getCandidates(req).iterator().next(),
                                revisionPkgMap,
                                allCandidates,
                                cycles);
                        }
                    }
                }
            }
        }

        cycles.remove(current);
    }

    private void mergeCandidatePackage(
        BundleRevision current, boolean requires,
        BundleRequirement currentReq, BundleCapability candCap,
        Map<BundleRevision, Packages> revisionPkgMap)
    {
        if (candCap.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE))
        {
            // Merge the candidate capability into the revision's package space
            // for imported or required packages, appropriately.

            String pkgName = (String)
                candCap.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE);

            List blameReqs = new ArrayList();
            blameReqs.add(currentReq);

            Packages currentPkgs = revisionPkgMap.get(current);

            Map<String, List<Blame>> packages = (requires)
                ? currentPkgs.m_requiredPkgs
                : currentPkgs.m_importedPkgs;
            List<Blame> blames = packages.get(pkgName);
            if (blames == null)
            {
                blames = new ArrayList<Blame>();
                packages.put(pkgName, blames);
            }
            blames.add(new Blame(candCap, blameReqs));

//dumpRevisionPkgs(current, currentPkgs);
        }
    }

    private void mergeUses(
        BundleRevision current, Packages currentPkgs,
        BundleCapability mergeCap, List<BundleRequirement> blameReqs,
        Map<BundleRevision, Packages> revisionPkgMap,
        Candidates allCandidates,
        Map<BundleCapability, List<BundleRevision>> cycleMap)
    {
        // If there are no uses, then just return.
        // If the candidate revision is the same as the current revision,
        // then we don't need to verify and merge the uses constraints
        // since this will happen as we build up the package space.
        if (current.equals(mergeCap.getRevision()))
        {
            return;
        }

        // Check for cycles.
        List<BundleRevision> list = cycleMap.get(mergeCap);
        if ((list != null) && list.contains(current))
        {
            return;
        }
        list = (list == null) ? new ArrayList<BundleRevision>() : list;
        list.add(current);
        cycleMap.put(mergeCap, list);

        for (BundleCapability candSourceCap : getPackageSources(mergeCap, revisionPkgMap))
        {
            for (String usedPkgName : ((BundleCapabilityImpl) candSourceCap).getUses())
            {
                Packages candSourcePkgs = revisionPkgMap.get(candSourceCap.getRevision());
                List<Blame> candSourceBlames = null;
                // Check to see if the used package is exported.
                Blame candExportedBlame = candSourcePkgs.m_exportedPkgs.get(usedPkgName);
                if (candExportedBlame != null)
                {
                    candSourceBlames = new ArrayList(1);
                    candSourceBlames.add(candExportedBlame);
                }
                else
                {
                    // If the used package is not exported, check to see if it
                    // is required.
                    candSourceBlames = candSourcePkgs.m_requiredPkgs.get(usedPkgName);
                    // Lastly, if the used package is not required, check to see if it
                    // is imported.
                    candSourceBlames = (candSourceBlames != null)
                        ? candSourceBlames : candSourcePkgs.m_importedPkgs.get(usedPkgName);
                }

                // If the used package cannot be found, then just ignore it
                // since it has no impact.
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
                        List<BundleRequirement> blameReqs2 = new ArrayList(blameReqs);
                        blameReqs2.add(blame.m_reqs.get(blame.m_reqs.size() - 1));
                        usedCaps.add(new Blame(blame.m_cap, blameReqs2));
                        mergeUses(current, currentPkgs, blame.m_cap, blameReqs2,
                            revisionPkgMap, allCandidates, cycleMap);
                    }
                    else
                    {
                        usedCaps.add(new Blame(blame.m_cap, blameReqs));
                        mergeUses(current, currentPkgs, blame.m_cap, blameReqs,
                            revisionPkgMap, allCandidates, cycleMap);
                    }
                }
            }
        }
    }

    private void checkPackageSpaceConsistency(
        boolean isDynamicImporting,
        BundleRevision revision,
        Candidates allCandidates,
        Map<BundleRevision, Packages> revisionPkgMap,
        Map<BundleRevision, Object> resultCache)
    {
        if ((revision.getWiring() != null) && !isDynamicImporting)
        {
            return;
        }
        else if(resultCache.containsKey(revision))
        {
            return;
        }

        Packages pkgs = revisionPkgMap.get(revision);

        ResolveException rethrow = null;
        Candidates permutation = null;
        Set<BundleRequirement> mutated = null;

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
                    else if (!sourceBlame.m_cap.getRevision().equals(blame.m_cap.getRevision()))
                    {
                        // Try to permutate the conflicting requirement.
                        permutate(allCandidates, blame.m_reqs.get(0), m_importPermutations);
                        // Try to permutate the source requirement.
                        permutate(allCandidates, sourceBlame.m_reqs.get(0), m_importPermutations);
                        // Report conflict.
                        ResolveException ex = new ResolveException(
                            "Uses constraint violation. Unable to resolve bundle revision "
                            + revision.getSymbolicName()
                            + " [" + revision
                            + "] because it is exposed to package '"
                            + entry.getKey()
                            + "' from bundle revisions "
                            + sourceBlame.m_cap.getRevision().getSymbolicName()
                            + " [" + sourceBlame.m_cap.getRevision()
                            + "] and "
                            + blame.m_cap.getRevision().getSymbolicName()
                            + " [" + blame.m_cap.getRevision()
                            + "] via two dependency chains.\n\nChain 1:\n"
                            + toStringBlame(sourceBlame)
                            + "\n\nChain 2:\n"
                            + toStringBlame(blame),
                            revision,
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
                if (!isCompatible(exportBlame.m_cap, usedBlame.m_cap, revisionPkgMap))
                {
                    // Create a candidate permutation that eliminates all candidates
                    // that conflict with existing selected candidates.
                    permutation = (permutation != null)
                        ? permutation
                        : allCandidates.copy();
                    rethrow = (rethrow != null)
                        ? rethrow
                        : new ResolveException(
                            "Uses constraint violation. Unable to resolve bundle revision "
                            + revision.getSymbolicName()
                            + " [" + revision
                            + "] because it exports package '"
                            + pkgName
                            + "' and is also exposed to it from bundle revision "
                            + usedBlame.m_cap.getRevision().getSymbolicName()
                            + " [" + usedBlame.m_cap.getRevision()
                            + "] via the following dependency chain:\n\n"
                            + toStringBlame(usedBlame),
                            null,
                            null);

                    mutated = (mutated != null)
                        ? mutated
                        : new HashSet<BundleRequirement>();

                    for (int reqIdx = usedBlame.m_reqs.size() - 1; reqIdx >= 0; reqIdx--)
                    {
                        BundleRequirement req = usedBlame.m_reqs.get(reqIdx);

                        // If we've already permutated this requirement in another
                        // uses constraint, don't permutate it again just continue
                        // with the next uses constraint.
                        if (mutated.contains(req))
                        {
                            break;
                        }

                        // See if we can permutate the candidates for blamed
                        // requirement; there may be no candidates if the revision
                        // associated with the requirement is already resolved.
                        SortedSet<BundleCapability> candidates =
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
                    if (!isCompatible(importBlame.m_cap, usedBlame.m_cap, revisionPkgMap))
                    {
                        // Create a candidate permutation that eliminates any candidates
                        // that conflict with existing selected candidates.
                        permutation = (permutation != null)
                            ? permutation
                            : allCandidates.copy();
                        rethrow = (rethrow != null)
                            ? rethrow
                            : new ResolveException(
                                "Uses constraint violation. Unable to resolve bundle revision "
                                + revision.getSymbolicName()
                                + " [" + revision
                                + "] because it is exposed to package '"
                                + pkgName
                                + "' from bundle revisions "
                                + importBlame.m_cap.getRevision().getSymbolicName()
                                + " [" + importBlame.m_cap.getRevision()
                                + "] and "
                                + usedBlame.m_cap.getRevision().getSymbolicName()
                                + " [" + usedBlame.m_cap.getRevision()
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
                            BundleRequirement req = usedBlame.m_reqs.get(reqIdx);

                            // If we've already permutated this requirement in another
                            // uses constraint, don't permutate it again just continue
                            // with the next uses constraint.
                            if (mutated.contains(req))
                            {
                                break;
                            }

                            // See if we can permutate the candidates for blamed
                            // requirement; there may be no candidates if the revision
                            // associated with the requirement is already resolved.
                            SortedSet<BundleCapability> candidates =
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
                    BundleRequirement req = importBlame.m_reqs.get(0);
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

        resultCache.put(revision, Boolean.TRUE);

        // Now check the consistency of all revisions on which the
        // current revision depends. Keep track of the current number
        // of permutations so we know if the lower level check was
        // able to create a permutation or not in the case of failure.
        int permCount = m_usesPermutations.size() + m_importPermutations.size();
        for (Entry<String, List<Blame>> entry : pkgs.m_importedPkgs.entrySet())
        {
            for (Blame importBlame : entry.getValue())
            {
                if (!revision.equals(importBlame.m_cap.getRevision()))
                {
                    try
                    {
                        checkPackageSpaceConsistency(
                            false, importBlame.m_cap.getRevision(),
                            allCandidates, revisionPkgMap, resultCache);
                    }
                    catch (ResolveException ex)
                    {
                        // If the lower level check didn't create any permutations,
                        // then we should create an import permutation for the
                        // requirement with the dependency on the failing revision
                        // to backtrack on our current candidate selection.
                        if (permCount == (m_usesPermutations.size() + m_importPermutations.size()))
                        {
                            BundleRequirement req = importBlame.m_reqs.get(0);
                            permutate(allCandidates, req, m_importPermutations);
                        }
                        throw ex;
                    }
                }
            }
        }
    }

    private static void permutate(
        Candidates allCandidates, BundleRequirement req, List<Candidates> permutations)
    {
        SortedSet<BundleCapability> candidates = allCandidates.getCandidates(req);
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
        Candidates allCandidates, BundleRequirement req, List<Candidates> permutations)
    {
        SortedSet<BundleCapability> candidates = allCandidates.getCandidates(req);
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
                Set<BundleCapability> existingPermCands = existingPerm.getCandidates(req);
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
        BundleRevision revision,
        Candidates allCandidates,
        Map<BundleRevision, Packages> revisionPkgMap)
    {
        Packages packages = revisionPkgMap.get(revision);
        if (packages != null)
        {
            return;
        }
        packages = new Packages(revision);

        // Get all exported packages.
        List<BundleCapability> caps = (revision.getWiring() != null)
            ? revision.getWiring().getCapabilities(null)
            : revision.getDeclaredCapabilities(null);
        Map<String, BundleCapability> exports =
            new HashMap<String, BundleCapability>(caps.size());
        for (BundleCapability cap : caps)
        {
            if (cap.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE))
            {
                if (!cap.getRevision().equals(revision))
                {
                    cap = new HostedCapability(revision, (BundleCapabilityImpl) cap);
                }
                exports.put(
                    (String) cap.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE),
                    cap);
            }
        }
        // Remove substitutable exports that were imported.
        // For resolved revisions BundleWiring.getCapabilities()
        // already excludes imported substitutable exports, but
        // for resolving revisions we must look in the candidate
        // map to determine which exports are substitutable.
        if (!exports.isEmpty())
        {
            if (revision.getWiring() == null)
            {
                for (BundleRequirement req : revision.getDeclaredRequirements(null))
                {
                    if (req.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE))
                    {
                        Set<BundleCapability> cands =
                            allCandidates.getCandidates((BundleRequirementImpl) req);
                        if ((cands != null) && !cands.isEmpty())
                        {
                            String pkgName = (String) cands.iterator().next()
                                .getAttributes().get(BundleRevision.PACKAGE_NAMESPACE);
                            exports.remove(pkgName);
                        }
                    }
                }
            }

            // Add all non-substituted exports to the revisions's package space.
            for (Entry<String, BundleCapability> entry : exports.entrySet())
            {
                packages.m_exportedPkgs.put(
                    entry.getKey(), new Blame(entry.getValue(), null));
            }
        }

        revisionPkgMap.put(revision, packages);
    }

    private boolean isCompatible(
        BundleCapability currentCap, BundleCapability candCap,
        Map<BundleRevision, Packages> revisionPkgMap)
    {
        if ((currentCap != null) && (candCap != null))
        {
            if (currentCap.equals(candCap))
            {
                return true;
            }

            List<BundleCapability> currentSources =
                getPackageSources(
                    currentCap,
                    revisionPkgMap);
            List<BundleCapability> candSources =
                getPackageSources(
                    candCap,
                    revisionPkgMap);

            return currentSources.containsAll(candSources)
                || candSources.containsAll(currentSources);
        }
        return true;
    }

    private Map<BundleCapability, List<BundleCapability>> m_packageSourcesCache
        = new HashMap();

    private List<BundleCapability> getPackageSources(
        BundleCapability cap, Map<BundleRevision, Packages> revisionPkgMap)
    {
        if (cap.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE))
        {
            List<BundleCapability> sources = m_packageSourcesCache.get(cap);
            if (sources == null)
            {
                sources = getPackageSourcesInternal(
                    cap, revisionPkgMap, new ArrayList(), new HashSet());
                m_packageSourcesCache.put(cap, sources);
            }
            return sources;
        }

        if (!((BundleCapabilityImpl) cap).getUses().isEmpty())
        {
            List<BundleCapability> caps = new ArrayList<BundleCapability>(1);
            caps.add(cap);
            return caps;
        }

        return Collections.EMPTY_LIST;
    }

    private static List<BundleCapability> getPackageSourcesInternal(
        BundleCapability cap, Map<BundleRevision, Packages> revisionPkgMap,
        List<BundleCapability> sources, Set<BundleCapability> cycleMap)
    {
        if (cap.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE))
        {
            if (cycleMap.contains(cap))
            {
                return sources;
            }
            cycleMap.add(cap);

            // Get the package name associated with the capability.
            String pkgName = cap.getAttributes()
                .get(BundleRevision.PACKAGE_NAMESPACE).toString();

            // Since a revision can export the same package more than once, get
            // all package capabilities for the specified package name.
            List<BundleCapability> caps = (cap.getRevision().getWiring() != null)
                ? cap.getRevision().getWiring().getCapabilities(null)
                : cap.getRevision().getDeclaredCapabilities(null);
            for (BundleCapability sourceCap : caps)
            {
                if (sourceCap.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE)
                    && sourceCap.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE).equals(pkgName))
                {
                    // Since capabilities may come from fragments, we need to check
                    // for that case and wrap them.
                    if (!cap.getRevision().equals(sourceCap.getRevision()))
                    {
                        sources.add(
                            new HostedCapability(cap.getRevision(), (BundleCapabilityImpl) sourceCap));
                    }
                    else
                    {
                        sources.add(sourceCap);
                    }
                }
            }

            // Then get any addition sources for the package from required bundles.
            Packages pkgs = revisionPkgMap.get(cap.getRevision());
            List<Blame> required = pkgs.m_requiredPkgs.get(pkgName);
            if (required != null)
            {
                for (Blame blame : required)
                {
                    getPackageSourcesInternal(blame.m_cap, revisionPkgMap, sources, cycleMap);
                }
            }
        }

        return sources;
    }

    private static BundleRevision getActualBundleRevision(BundleRevision br)
    {
        if (br instanceof HostBundleRevision)
        {
            return ((HostBundleRevision) br).getHost();
        }
        return br;
    }

    private static BundleCapability getActualCapability(BundleCapability c)
    {
        if (c instanceof HostedCapability)
        {
            return ((HostedCapability) c).getOriginalCapability();
        }
        return c;
    }

    private static BundleRequirement getActualRequirement(BundleRequirement r)
    {
        if (r instanceof HostedRequirement)
        {
            return ((HostedRequirement) r).getOriginalRequirement();
        }
        return r;
    }

    private static Map<BundleRevision, List<ResolverWire>> populateWireMap(
        BundleRevision revision, Map<BundleRevision, Packages> revisionPkgMap,
        Map<BundleRevision, List<ResolverWire>> wireMap,
        Candidates allCandidates)
    {
        BundleRevision unwrappedRevision = getActualBundleRevision(revision);
        if ((unwrappedRevision.getWiring() == null)
            && !wireMap.containsKey(unwrappedRevision))
        {
            wireMap.put(unwrappedRevision, (List<ResolverWire>) Collections.EMPTY_LIST);

            List<ResolverWire> packageWires = new ArrayList<ResolverWire>();
            List<ResolverWire> bundleWires = new ArrayList<ResolverWire>();
            List<ResolverWire> capabilityWires = new ArrayList<ResolverWire>();

            for (BundleRequirement req : revision.getDeclaredRequirements(null))
            {
                SortedSet<BundleCapability> cands = allCandidates.getCandidates(req);
                if ((cands != null) && (cands.size() > 0))
                {
                    BundleCapability cand = cands.iterator().next();
                    // Ignore revisions that import themselves.
                    if (!revision.equals(cand.getRevision()))
                    {
                        if (cand.getRevision().getWiring() == null)
                        {
                            populateWireMap(cand.getRevision(),
                                revisionPkgMap, wireMap, allCandidates);
                        }
                        Packages candPkgs = revisionPkgMap.get(cand.getRevision());
                        ResolverWire wire = new ResolverWireImpl(
                            unwrappedRevision,
                            getActualRequirement(req),
                            getActualBundleRevision(cand.getRevision()),
                            getActualCapability(cand));
                        if (req.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE))
                        {
                            packageWires.add(wire);
                        }
                        else if (req.getNamespace().equals(BundleRevision.BUNDLE_NAMESPACE))
                        {
                            bundleWires.add(wire);
                        }
                        else
                        {
                            capabilityWires.add(wire);
                        }
                    }
                }
            }

            // Combine package wires with require wires last.
            packageWires.addAll(bundleWires);
            packageWires.addAll(capabilityWires);
            wireMap.put(unwrappedRevision, packageWires);

            // Add host wire for any fragments.
            if (revision instanceof HostBundleRevision)
            {
                List<BundleRevision> fragments = ((HostBundleRevision) revision).getFragments();
                for (BundleRevision fragment : fragments)
                {
                    List<ResolverWire> hostWires = wireMap.get(fragment);
                    if (hostWires == null)
                    {
                        hostWires = new ArrayList<ResolverWire>();
                        wireMap.put(fragment, hostWires);
                    }
                    hostWires.add(
                        new ResolverWireImpl(
                            getActualBundleRevision(fragment),
                            fragment.getDeclaredRequirements(
                                BundleRevision.HOST_NAMESPACE).get(0),
                            unwrappedRevision,
                            unwrappedRevision.getDeclaredCapabilities(
                                BundleRevision.HOST_NAMESPACE).get(0)));
                }
            }
        }

        return wireMap;
    }

    private static Map<BundleRevision, List<ResolverWire>> populateDynamicWireMap(
        BundleRevision revision, String pkgName, Map<BundleRevision, Packages> revisionPkgMap,
        Map<BundleRevision, List<ResolverWire>> wireMap, Candidates allCandidates)
    {
        wireMap.put(revision, (List<ResolverWire>) Collections.EMPTY_LIST);

        List<ResolverWire> packageWires = new ArrayList<ResolverWire>();

        BundleRequirement dynReq = null;
        BundleCapability dynCand = null;
        for (BundleRequirement req
            : Util.getDynamicRequirements(revision.getWiring().getRequirements(null)))
        {
            // Get the candidates for the current dynamic requirement.
            SortedSet<BundleCapability> candCaps =
                allCandidates.getCandidates((BundleRequirementImpl) req);
            // Optional requirements may not have any candidates.
            if ((candCaps == null) || candCaps.isEmpty())
            {
                continue;
            }

            // Record the dynamic requirement.
            dynReq = req;
            dynCand = candCaps.first();

            // Can only dynamically import one at a time, so break
            // out of the loop after the first.
            break;
        }

        if (dynReq != null)
        {
            if (dynCand.getRevision().getWiring() == null)
            {
                populateWireMap(dynCand.getRevision(), revisionPkgMap, wireMap,
                    allCandidates);
            }

            Map<String, Object> attrs = new HashMap(1);
            attrs.put(BundleRevision.PACKAGE_NAMESPACE, pkgName);
            packageWires.add(
                new ResolverWireImpl(
                    revision,
                    dynReq,
                    getActualBundleRevision(dynCand.getRevision()),
                    getActualCapability(dynCand)));
        }

        wireMap.put(revision, packageWires);

        return wireMap;
    }

    private static void dumpRevisionPkgMap(Map<BundleRevision, Packages> revisionPkgMap)
    {
        System.out.println("+++BUNDLE REVISION PKG MAP+++");
        for (Entry<BundleRevision, Packages> entry : revisionPkgMap.entrySet())
        {
            dumpRevisionPkgs(entry.getKey(), entry.getValue());
        }
    }

    private static void dumpRevisionPkgs(BundleRevision revision, Packages packages)
    {
        System.out.println(revision
            + " (" + ((revision.getWiring() != null) ? "RESOLVED)" : "UNRESOLVED)"));
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
                BundleRequirement req = blame.m_reqs.get(i);
                sb.append("  ");
                sb.append(req.getRevision().getSymbolicName());
                sb.append(" [");
                sb.append(req.getRevision().toString());
                sb.append("]\n");
                if (req.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE))
                {
                    sb.append("    import: ");
                }
                else
                {
                    sb.append("    require: ");
                }
                sb.append(((BundleRequirementImpl) req).getFilter().toString());
                sb.append("\n     |");
                if (req.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE))
                {
                    sb.append("\n    export: ");
                }
                else
                {
                    sb.append("\n    provide: ");
                }
                if ((i + 1) < blame.m_reqs.size())
                {
                    BundleCapability cap = Util.getSatisfyingCapability(
                        blame.m_reqs.get(i + 1).getRevision(),
                        (BundleRequirementImpl) blame.m_reqs.get(i));
                    if (cap.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE))
                    {
                        sb.append(BundleRevision.PACKAGE_NAMESPACE);
                        sb.append("=");
                        sb.append(cap.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE).toString());
                        BundleCapability usedCap;
                        if ((i + 2) < blame.m_reqs.size())
                        {
                            usedCap = Util.getSatisfyingCapability(
                                blame.m_reqs.get(i + 2).getRevision(),
                                (BundleRequirementImpl) blame.m_reqs.get(i + 1));
                        }
                        else
                        {
                            usedCap = Util.getSatisfyingCapability(
                                blame.m_cap.getRevision(),
                                (BundleRequirementImpl) blame.m_reqs.get(i + 1));
                        }
                        sb.append("; uses:=");
                        sb.append(usedCap.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE));
                    }
                    else
                    {
                        sb.append(cap);
                    }
                    sb.append("\n");
                }
                else
                {
                    BundleCapability export = Util.getSatisfyingCapability(
                        blame.m_cap.getRevision(),
                        (BundleRequirementImpl) blame.m_reqs.get(i));
                    sb.append(export.getNamespace());
                    sb.append("=");
                    sb.append(export.getAttributes().get(export.getNamespace()).toString());
                    if (export.getNamespace().equals(BundleRevision.PACKAGE_NAMESPACE)
                        && !export.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE)
                            .equals(blame.m_cap.getAttributes().get(
                                BundleRevision.PACKAGE_NAMESPACE)))
                    {
                        sb.append("; uses:=");
                        sb.append(blame.m_cap.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE));
                        sb.append("\n    export: ");
                        sb.append(BundleRevision.PACKAGE_NAMESPACE);
                        sb.append("=");
                        sb.append(blame.m_cap.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE).toString());
                    }
                    sb.append("\n  ");
                    sb.append(blame.m_cap.getRevision().getSymbolicName());
                    sb.append(" [");
                    sb.append(blame.m_cap.getRevision().toString());
                    sb.append("]");
                }
            }
        }
        else
        {
            sb.append(blame.m_cap.getRevision().toString());
        }
        return sb.toString();
    }

    private static class Packages
    {
        private final BundleRevision m_revision;
        public final Map<String, Blame> m_exportedPkgs = new HashMap();
        public final Map<String, List<Blame>> m_importedPkgs = new HashMap();
        public final Map<String, List<Blame>> m_requiredPkgs = new HashMap();
        public final Map<String, List<Blame>> m_usedPkgs = new HashMap();

        public Packages(BundleRevision revision)
        {
            m_revision = revision;
        }
    }

    private static class Blame
    {
        public final BundleCapability m_cap;
        public final List<BundleRequirement> m_reqs;

        public Blame(BundleCapability cap, List<BundleRequirement> reqs)
        {
            m_cap = cap;
            m_reqs = reqs;
        }

        @Override
        public String toString()
        {
            return m_cap.getRevision()
                + "." + m_cap.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE)
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