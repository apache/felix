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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.resource.Wiring;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.ResolveContext;
import org.osgi.service.resolver.Resolver;

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

    public Map<Resource, List<Wire>> resolve(ResolveContext rc) throws ResolutionException
    {
        Map<Resource, List<Wire>> wireMap =
            new HashMap<Resource, List<Wire>>();
        Map<Resource, Packages> resourcePkgMap =
            new HashMap<Resource, Packages>();

        // Make copies of arguments in case we want to modify them.
        Collection<Resource> mandatoryResources = new ArrayList(rc.getMandatoryResources());
        Collection<Resource> optionalResources = new ArrayList(rc.getOptionalResources());
// TODO: RFC-112 - Need impl-specific type.
//        Collection<Resource> ondemandFragments = (rc instanceof ResolveContextImpl)
//            ? ((ResolveContextImpl) rc).getOndemandResources() : Collections.EMPTY_LIST;
        Collection<Resource> ondemandFragments =  Collections.EMPTY_LIST;

        boolean retry;
        do
        {
            retry = false;

            try
            {
                // Create object to hold all candidates.
                Candidates allCandidates = new Candidates();

                // Populate mandatory resources; since these are mandatory
                // resources, failure throws a resolve exception.
                for (Iterator<Resource> it = mandatoryResources.iterator();
                    it.hasNext(); )
                {
                    Resource resource = it.next();
                    if (Util.isFragment(resource) || (rc.getWirings().get(resource) == null))
                    {
                        allCandidates.populate(rc, resource, Candidates.MANDATORY);
                    }
                    else
                    {
                        it.remove();
                    }
                }

                // Populate optional resources; since these are optional
                // resources, failure does not throw a resolve exception.
                for (Resource resource : optionalResources)
                {
                    boolean isFragment = Util.isFragment(resource);
                    if (isFragment || (rc.getWirings().get(resource) == null))
                    {
                        allCandidates.populate(rc, resource, Candidates.OPTIONAL);
                    }
                }

                // Populate ondemand fragments; since these are optional
                // resources, failure does not throw a resolve exception.
                for (Resource resource : ondemandFragments)
                {
                    boolean isFragment = Util.isFragment(resource);
                    if (isFragment)
                    {
                        allCandidates.populate(rc, resource, Candidates.ON_DEMAND);
                    }
                }

                // Merge any fragments into hosts.
                allCandidates.prepare(rc);

                // Create a combined list of populated resources; for
                // optional resources. We do not need to consider ondemand
                // fragments, since they will only be pulled in if their
                // host is already present.
                Set<Resource> allResources =
                    new HashSet<Resource>(mandatoryResources);
                for (Resource resource : optionalResources)
                {
                    if (allCandidates.isPopulated(resource))
                    {
                        allResources.add(resource);
                    }
                }

                // Record the initial candidate permutation.
                m_usesPermutations.add(allCandidates);

                ResolutionException rethrow = null;

                // If a populated resource is a fragment, then its host
                // must ultimately be verified, so store its host requirement
                // to use for package space calculation.
                Map<Resource, List<Requirement>> hostReqs =
                    new HashMap<Resource, List<Requirement>>();
                for (Resource resource : allResources)
                {
                    if (Util.isFragment(resource))
                    {
                        hostReqs.put(
                            resource,
                            resource.getRequirements(HostNamespace.HOST_NAMESPACE));
                    }
                }

                do
                {
                    rethrow = null;

                    resourcePkgMap.clear();
                    m_packageSourcesCache.clear();

                    allCandidates = (m_usesPermutations.size() > 0)
                        ? m_usesPermutations.remove(0)
                        : m_importPermutations.remove(0);
//allCandidates.dump();

                    for (Resource resource : allResources)
                    {
                        Resource target = resource;

                        // If we are resolving a fragment, then get its
                        // host candidate and verify it instead.
                        List<Requirement> hostReq = hostReqs.get(resource);
                        if (hostReq != null)
                        {
                            target = allCandidates.getCandidates(hostReq.get(0))
                                .iterator().next().getResource();
                        }

                        calculatePackageSpaces(
                            rc, allCandidates.getWrappedHost(target), allCandidates,
                            resourcePkgMap, new HashMap(), new HashSet());
//System.out.println("+++ PACKAGE SPACES START +++");
//dumpResourcePkgMap(resourcePkgMap);
//System.out.println("+++ PACKAGE SPACES END +++");

                        try
                        {
                            checkPackageSpaceConsistency(
                                rc, allCandidates.getWrappedHost(target),
                                allCandidates, resourcePkgMap, new HashMap());
                        }
                        catch (ResolutionException ex)
                        {
                            rethrow = ex;
                        }
                    }
                }
                while ((rethrow != null)
                    && ((m_usesPermutations.size() > 0) || (m_importPermutations.size() > 0)));

                // If there is a resolve exception, then determine if an
                // optionally resolved resource is to blame (typically a fragment).
                // If so, then remove the optionally resolved resolved and try
                // again; otherwise, rethrow the resolve exception.
                if (rethrow != null)
                {
                    Collection<Requirement> exReqs = rethrow.getUnresolvedRequirements();
                    Requirement faultyReq = ((exReqs == null) || (exReqs.isEmpty()))
                        ? null : exReqs.iterator().next();
                    Resource faultyResource = (faultyReq == null)
                        ? null : getDeclaredResource(faultyReq.getResource());
                    // If the faulty requirement is wrapped, then it may
                    // be from a fragment, so consider the fragment faulty
                    // instead of the host.
                    if (faultyReq instanceof WrappedRequirement)
                    {
                        faultyResource =
                            ((WrappedRequirement) faultyReq)
                                .getDeclaredRequirement().getResource();
                    }
                    // Try to ignore the faulty resource if it is not mandatory.
                    if (optionalResources.remove(faultyResource))
                    {
                        retry = true;
                    }
                    else if (ondemandFragments.remove(faultyResource))
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
                    for (Resource resource : allResources)
                    {
                        Resource target = resource;

                        // If we are resolving a fragment, then we
                        // actually want to populate its host's wires.
                        List<Requirement> hostReq = hostReqs.get(resource);
                        if (hostReq != null)
                        {
                            target = allCandidates.getCandidates(hostReq.get(0))
                                .iterator().next().getResource();
                        }

                        if (allCandidates.isPopulated(target))
                        {
                            wireMap =
                                populateWireMap(
                                    rc, allCandidates.getWrappedHost(target),
                                    resourcePkgMap, wireMap, allCandidates);
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

    /**
     * Resolves a dynamic requirement for the specified host resource using the specified
     * {@link ResolveContext}.  The dynamic requirement may contain wild cards in its filter
     * for the package name.  The matching candidates are used to resolve the requirement and
     * the resolve context is not asked to find providers for the dynamic requirement.
     * The host resource is expected to not be a fragment, to already be resolved and
     * have an existing wiring provided by the resolve context.
     * <p>
     * This operation may resolve additional resources in order to resolve the dynamic
     * requirement.  The returned map will contain entries for each resource that got resolved
     * in addition to the specified host resource.  The wire list for the host resource
     * will only contain a single wire which is for the dynamic requirement.
     * @param rc the resolve context
     * @param host the hosting resource
     * @param dynamicReq the dynamic requirement
     * @param matches a list of matching capabilities
     * @param ondemandFragments collection of on demand fragments that will attach to any host that is a candidate
     * @return The new resources and wires required to satisfy the specified
     *         dynamic requirement. The returned map is the property of the caller
     *         and can be modified by the caller.
     * @throws ResolutionException
     */
    public Map<Resource, List<Wire>> resolve(
        ResolveContext rc, Resource host, Requirement dynamicReq,
        List<Capability> matches, Collection<Resource> ondemandFragments)
        throws ResolutionException
    {
        Map<Resource, List<Wire>> wireMap = new HashMap<Resource, List<Wire>>();

        // We can only create a dynamic import if the following
        // conditions are met:
        // 1. The specified resource is resolved.
        // 2. The package in question is not already imported.
        // 3. The package in question is not accessible via require-bundle.
        // 4. The package in question is not exported by the resource.
        // 5. The package in question matches a dynamic import of the resource.
        if (!matches.isEmpty() && rc.getWirings().containsKey(host))
        {
            // Make sure all matching candidates are packages.
            for (Capability cap : matches)
            {
                if (!cap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
                {
                    throw new IllegalArgumentException(
                        "Matching candidate does not provide a package name.");
                }
            }

            // Make copy of args in case we want to modify them.
            ondemandFragments = new ArrayList<Resource>(ondemandFragments);

            // Create all candidates pre-populated with the single candidate set
            // for the resolving dynamic import of the host.
            Candidates allCandidates = new Candidates();
            allCandidates.populateDynamic(rc, host, dynamicReq, matches);

            Map<Resource, Packages> resourcePkgMap = new HashMap<Resource, Packages>();

            boolean retry;
            do
            {
                retry = false;

                try
                {
                    // Try to populate optional fragments.
                    for (Resource r : ondemandFragments)
                    {
                        if (Util.isFragment(r))
                        {
                            allCandidates.populate(rc, r, Candidates.ON_DEMAND);
                        }
                    }

                    // Merge any fragments into hosts.
                    allCandidates.prepare(rc);

                    // Record the initial candidate permutation.
                    m_usesPermutations.add(allCandidates);

                    ResolutionException rethrow = null;

                    do
                    {
                        rethrow = null;

                        resourcePkgMap.clear();
                        m_packageSourcesCache.clear();

                        allCandidates = (m_usesPermutations.size() > 0)
                            ? m_usesPermutations.remove(0)
                            : m_importPermutations.remove(0);
//allCandidates.dump();

                        // For a dynamic import, the instigating resource
                        // will never be a fragment since fragments never
                        // execute code, so we don't need to check for
                        // this case like we do for a normal resolve.

                        calculatePackageSpaces(rc,
                            allCandidates.getWrappedHost(host), allCandidates,
                            resourcePkgMap, new HashMap(), new HashSet());
//System.out.println("+++ PACKAGE SPACES START +++");
//dumpResourcePkgMap(resourcePkgMap);
//System.out.println("+++ PACKAGE SPACES END +++");

                        try
                        {
                            checkDynamicPackageSpaceConsistency(rc,
                                allCandidates.getWrappedHost(host),
                                allCandidates, resourcePkgMap, new HashMap());
                        }
                        catch (ResolutionException ex)
                        {
                            rethrow = ex;
                        }
                    }
                    while ((rethrow != null)
                        && ((m_usesPermutations.size() > 0) || (m_importPermutations.size() > 0)));

                    // If there is a resolve exception, then determine if an
                    // optionally resolved resource is to blame (typically a fragment).
                    // If so, then remove the optionally resolved resource and try
                    // again; otherwise, rethrow the resolve exception.
                    if (rethrow != null)
                    {
                        Collection<Requirement> exReqs = rethrow.getUnresolvedRequirements();
                        Requirement faultyReq = ((exReqs == null) || (exReqs.isEmpty()))
                            ? null : exReqs.iterator().next();
                        Resource faultyResource = (faultyReq == null)
                            ? null : getDeclaredResource(faultyReq.getResource());
                        // If the faulty requirement is wrapped, then it may
                        // be from a fragment, so consider the fragment faulty
                        // instead of the host.
                        if (faultyReq instanceof WrappedRequirement)
                        {
                            faultyResource =
                                ((WrappedRequirement) faultyReq)
                                    .getDeclaredRequirement().getResource();
                        }
                        // Try to ignore the faulty resource if it is not mandatory.
                        if (ondemandFragments.remove(faultyResource))
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
                        wireMap = populateDynamicWireMap(rc,
                            host, dynamicReq, resourcePkgMap, wireMap, allCandidates);
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

        return wireMap;
    }

    private void calculatePackageSpaces(
        ResolveContext rc,
        Resource resource,
        Candidates allCandidates,
        Map<Resource, Packages> resourcePkgMap,
        Map<Capability, List<Resource>> usesCycleMap,
        Set<Resource> cycle)
    {
        if (cycle.contains(resource))
        {
            return;
        }
        cycle.add(resource);

        // Make sure package space hasn't already been calculated.
        Packages resourcePkgs = resourcePkgMap.get(resource);
        if (resourcePkgs != null)
        {
            if (resourcePkgs.m_isCalculated)
            {
                return;
            }
            else
            {
                resourcePkgs.m_isCalculated = true;
            }
        }

        // Create parallel lists for requirement and proposed candidate
        // capability or actual capability if resource is resolved or not.
        // We use parallel lists so we can calculate the packages spaces for
        // resolved and unresolved resources in an identical fashion.
        List<Requirement> reqs = new ArrayList();
        List<Capability> caps = new ArrayList();
        boolean isDynamicImporting = false;
        Wiring wiring = rc.getWirings().get(resource);
        if (wiring != null)
        {
            // Use wires to get actual requirements and satisfying capabilities.
            for (Wire wire : wiring.getRequiredResourceWires(null))
            {
                // Wrap the requirement as a hosted requirement if it comes
                // from a fragment, since we will need to know the host. We
                // also need to wrap if the requirement is a dynamic import,
                // since that requirement will be shared with any other
                // matching dynamic imports.
                Requirement r = wire.getRequirement();
                if (!r.getResource().equals(wire.getRequirer())
                    || ((r.getDirectives()
                        .get(PackageNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE) != null)
                    && r.getDirectives()
                        .get(PackageNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE)
                            .equals(PackageNamespace.RESOLUTION_DYNAMIC)))
                {
                    r = new WrappedRequirement(wire.getRequirer(), r);
                }
                // Wrap the capability as a hosted capability if it comes
                // from a fragment, since we will need to know the host.
                Capability c = wire.getCapability();
                if (!c.getResource().equals(wire.getProvider()))
                {
                    c = new WrappedCapability(wire.getProvider(), c);
                }
                reqs.add(r);
                caps.add(c);
            }

            // Since the resource is resolved, it could be dynamically importing,
            // so check to see if there are candidates for any of its dynamic
            // imports.
            //
            // NOTE: If the resource is dynamically importing, the fact that
            // the dynamic import is added here last to the parallel reqs/caps
            // list is used later when checking to see if the package being
            // dynamically imported shadows an existing provider.
            for (Requirement req
                : Util.getDynamicRequirements(wiring.getResourceRequirements(null)))
            {
                // Get the candidates for the current requirement.
                List<Capability> candCaps = allCandidates.getCandidates(req);
                // Optional requirements may not have any candidates.
                if (candCaps == null)
                {
                    continue;
                }
                // Grab first (i.e., highest priority) candidate.
                Capability cap = candCaps.get(0);
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
            for (Requirement req : resource.getRequirements(null))
            {
                String resolution = req.getDirectives()
                    .get(PackageNamespace.REQUIREMENT_RESOLUTION_DIRECTIVE);
                if ((resolution == null)
                    || !resolution.equals(PackageNamespace.RESOLUTION_DYNAMIC))
                {
                    // Get the candidates for the current requirement.
                    List<Capability> candCaps = allCandidates.getCandidates(req);
                    // Optional requirements may not have any candidates.
                    if (candCaps == null)
                    {
                        continue;
                    }

                    // Grab first (i.e., highest priority) candidate.
                    Capability cap = candCaps.get(0);
                    reqs.add(req);
                    caps.add(cap);
                }
            }
        }

        // First, add all exported packages to the target resource's package space.
        calculateExportedPackages(rc, resource, allCandidates, resourcePkgMap);
        resourcePkgs = resourcePkgMap.get(resource);

        // Second, add all imported packages to the target resource's package space.
        for (int i = 0; i < reqs.size(); i++)
        {
            Requirement req = reqs.get(i);
            Capability cap = caps.get(i);
            calculateExportedPackages(rc, cap.getResource(), allCandidates, resourcePkgMap);

            // If this resource is dynamically importing, then the last requirement
            // is the dynamic import being resolved, since it is added last to the
            // parallel lists above. For the dynamically imported package, make
            // sure that the resource doesn't already have a provider for that
            // package, which would be illegal and shouldn't be allowed.
            if (isDynamicImporting && ((i + 1) == reqs.size()))
            {
                String pkgName = (String)
                    cap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
                if (resourcePkgs.m_exportedPkgs.containsKey(pkgName)
                    || resourcePkgs.m_importedPkgs.containsKey(pkgName)
                    || resourcePkgs.m_requiredPkgs.containsKey(pkgName))
                {
                    throw new IllegalArgumentException(
                        "Resource "
                        + resource
                        + " cannot dynamically import package '"
                        + pkgName
                        + "' since it already has access to it."
                        );
                }
            }

            mergeCandidatePackages(
                rc, resource, req, cap, resourcePkgMap, allCandidates,
                new HashMap<Resource, List<Capability>>());
        }

        // Third, have all candidates to calculate their package spaces.
        for (int i = 0; i < caps.size(); i++)
        {
            calculatePackageSpaces(
                rc, caps.get(i).getResource(), allCandidates, resourcePkgMap,
                usesCycleMap, cycle);
        }

        // Fourth, if the target resource is unresolved or is dynamically importing,
        // then add all the uses constraints implied by its imported and required
        // packages to its package space.
        // NOTE: We do not need to do this for resolved resources because their
        // package space is consistent by definition and these uses constraints
        // are only needed to verify the consistency of a resolving resource. The
        // only exception is if a resolved resource is dynamically importing, then
        // we need to calculate its uses constraints again to make sure the new
        // import is consistent with the existing package space.
        if ((wiring == null) || isDynamicImporting)
        {
            // Merge uses constraints from required capabilities.
            for (int i = 0; i < reqs.size(); i++)
            {
                Requirement req = reqs.get(i);
                Capability cap = caps.get(i);
                // Ignore bundle/package requirements, since they are
                // considered below.
                if (!req.getNamespace().equals(BundleNamespace.BUNDLE_NAMESPACE)
                    && !req.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
                {
                    List<Requirement> blameReqs = new ArrayList<Requirement>();
                    blameReqs.add(req);

                    mergeUses(
                        rc,
                        resource,
                        resourcePkgs,
                        cap,
                        blameReqs,
                        resourcePkgMap,
                        allCandidates,
                        usesCycleMap);
                }
            }
            // Merge uses constraints from imported packages.
            for (Entry<String, List<Blame>> entry : resourcePkgs.m_importedPkgs.entrySet())
            {
                for (Blame blame : entry.getValue())
                {
                    // Ignore resources that import from themselves.
                    if (!blame.m_cap.getResource().equals(resource))
                    {
                        List<Requirement> blameReqs = new ArrayList();
                        blameReqs.add(blame.m_reqs.get(0));

                        mergeUses(
                            rc,
                            resource,
                            resourcePkgs,
                            blame.m_cap,
                            blameReqs,
                            resourcePkgMap,
                            allCandidates,
                            usesCycleMap);
                    }
                }
            }
            // Merge uses constraints from required bundles.
            for (Entry<String, List<Blame>> entry : resourcePkgs.m_requiredPkgs.entrySet())
            {
                for (Blame blame : entry.getValue())
                {
                    List<Requirement> blameReqs = new ArrayList();
                    blameReqs.add(blame.m_reqs.get(0));

                    mergeUses(
                        rc,
                        resource,
                        resourcePkgs,
                        blame.m_cap,
                        blameReqs,
                        resourcePkgMap,
                        allCandidates,
                        usesCycleMap);
                }
            }
        }
    }

    private void mergeCandidatePackages(
        ResolveContext rc, Resource current, Requirement currentReq,
        Capability candCap, Map<Resource, Packages> resourcePkgMap,
        Candidates allCandidates, Map<Resource, List<Capability>> cycles)
    {
        List<Capability> cycleCaps = cycles.get(current);
        if (cycleCaps == null)
        {
            cycleCaps = new ArrayList<Capability>();
            cycles.put(current, cycleCaps);
        }
        if (cycleCaps.contains(candCap))
        {
            return;
        }
        cycleCaps.add(candCap);

        if (candCap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
        {
            mergeCandidatePackage(
                current, false, currentReq, candCap, resourcePkgMap);
        }
        else if (candCap.getNamespace().equals(BundleNamespace.BUNDLE_NAMESPACE))
        {
// TODO: FELIX3 - THIS NEXT LINE IS A HACK. IMPROVE HOW/WHEN WE CALCULATE EXPORTS.
            calculateExportedPackages(
                rc, candCap.getResource(), allCandidates, resourcePkgMap);

            // Get the candidate's package space to determine which packages
            // will be visible to the current resource.
            Packages candPkgs = resourcePkgMap.get(candCap.getResource());

            // We have to merge all exported packages from the candidate,
            // since the current resource requires it.
            for (Entry<String, Blame> entry : candPkgs.m_exportedPkgs.entrySet())
            {
                mergeCandidatePackage(
                    current,
                    true,
                    currentReq,
                    entry.getValue().m_cap,
                    resourcePkgMap);
            }

            // If the candidate requires any other bundles with reexport visibility,
            // then we also need to merge their packages too.
            Wiring candWiring = rc.getWirings().get(candCap.getResource());
            if (candWiring != null)
            {
                for (Wire w : candWiring.getRequiredResourceWires(null))
                {
                    if (w.getRequirement().getNamespace()
                        .equals(BundleNamespace.BUNDLE_NAMESPACE))
                    {
                        String value = w.getRequirement()
                            .getDirectives()
                                .get(BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE);
                        if ((value != null)
                            && value.equals(BundleNamespace.VISIBILITY_REEXPORT))
                        {
                            mergeCandidatePackages(
                                rc,
                                current,
                                currentReq,
                                w.getCapability(),
                                resourcePkgMap,
                                allCandidates,
                                cycles);
                        }
                    }
                }
            }
            else
            {
                for (Requirement req : candCap.getResource().getRequirements(null))
                {
                    if (req.getNamespace().equals(BundleNamespace.BUNDLE_NAMESPACE))
                    {
                        String value =
                            req.getDirectives()
                                .get(BundleNamespace.REQUIREMENT_VISIBILITY_DIRECTIVE);
                        if ((value != null)
                            && value.equals(BundleNamespace.VISIBILITY_REEXPORT)
                            && (allCandidates.getCandidates(req) != null))
                        {
                            mergeCandidatePackages(
                                rc,
                                current,
                                currentReq,
                                allCandidates.getCandidates(req).iterator().next(),
                                resourcePkgMap,
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
        Resource current, boolean requires,
        Requirement currentReq, Capability candCap,
        Map<Resource, Packages> resourcePkgMap)
    {
        if (candCap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
        {
            // Merge the candidate capability into the resource's package space
            // for imported or required packages, appropriately.

            String pkgName = (String)
                candCap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);

            List blameReqs = new ArrayList();
            blameReqs.add(currentReq);

            Packages currentPkgs = resourcePkgMap.get(current);

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

//dumpResourcePkgs(current, currentPkgs);
        }
    }

    private void mergeUses(
        ResolveContext rc, Resource current, Packages currentPkgs,
        Capability mergeCap, List<Requirement> blameReqs,
        Map<Resource, Packages> resourcePkgMap,
        Candidates allCandidates,
        Map<Capability, List<Resource>> cycleMap)
    {
        // If there are no uses, then just return.
        // If the candidate resource is the same as the current resource,
        // then we don't need to verify and merge the uses constraints
        // since this will happen as we build up the package space.
        if (current.equals(mergeCap.getResource()))
        {
            return;
        }

        // Check for cycles.
        List<Resource> list = cycleMap.get(mergeCap);
        if ((list != null) && list.contains(current))
        {
            return;
        }
        list = (list == null) ? new ArrayList<Resource>() : list;
        list.add(current);
        cycleMap.put(mergeCap, list);

        for (Capability candSourceCap : getPackageSources(rc, mergeCap, resourcePkgMap))
        {
            List<String> uses;
// TODO: RFC-112 - Need impl-specific type
//            if (candSourceCap instanceof FelixCapability)
//            {
//                uses = ((FelixCapability) candSourceCap).getUses();
//            }
//            else
            {
                uses = Collections.EMPTY_LIST;
                String s = candSourceCap.getDirectives()
                    .get(Namespace.CAPABILITY_USES_DIRECTIVE);
                if (s != null)
                {
                    // Parse these uses directive.
                    StringTokenizer tok = new StringTokenizer(s, ",");
                    uses = new ArrayList(tok.countTokens());
                    while (tok.hasMoreTokens())
                    {
                        uses.add(tok.nextToken().trim());
                    }
                }
            }
            for (String usedPkgName : uses)
            {
                Packages candSourcePkgs = resourcePkgMap.get(candSourceCap.getResource());
                List<Blame> candSourceBlames;
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
                        List<Requirement> blameReqs2 = new ArrayList(blameReqs);
                        blameReqs2.add(blame.m_reqs.get(blame.m_reqs.size() - 1));
                        usedCaps.add(new Blame(blame.m_cap, blameReqs2));
                        mergeUses(rc, current, currentPkgs, blame.m_cap, blameReqs2,
                            resourcePkgMap, allCandidates, cycleMap);
                    }
                    else
                    {
                        usedCaps.add(new Blame(blame.m_cap, blameReqs));
                        mergeUses(rc, current, currentPkgs, blame.m_cap, blameReqs,
                            resourcePkgMap, allCandidates, cycleMap);
                    }
                }
            }
        }
    }

    private void checkPackageSpaceConsistency(
        ResolveContext rc,
        Resource resource,
        Candidates allCandidates,
        Map<Resource, Packages> resourcePkgMap,
        Map<Resource, Object> resultCache) throws ResolutionException
    {
        if (rc.getWirings().containsKey(resource))
        {
            return;
        }
        checkDynamicPackageSpaceConsistency(
            rc, resource, allCandidates, resourcePkgMap, resultCache);
    }

    private void checkDynamicPackageSpaceConsistency(
        ResolveContext rc,
        Resource resource,
        Candidates allCandidates,
        Map<Resource, Packages> resourcePkgMap,
        Map<Resource, Object> resultCache) throws ResolutionException
    {
        if (resultCache.containsKey(resource))
        {
            return;
        }

        Packages pkgs = resourcePkgMap.get(resource);

        ResolutionException rethrow = null;
        Candidates permutation = null;
        Set<Requirement> mutated = null;

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
                    else if (!sourceBlame.m_cap.getResource().equals(blame.m_cap.getResource()))
                    {
                        // Try to permutate the conflicting requirement.
                        permutate(allCandidates, blame.m_reqs.get(0), m_importPermutations);
                        // Try to permutate the source requirement.
                        permutate(allCandidates, sourceBlame.m_reqs.get(0), m_importPermutations);
                        // Report conflict.
                        ResolutionException ex = new ResolutionException(
                            "Uses constraint violation. Unable to resolve resource "
                            + Util.getSymbolicName(resource)
                            + " [" + resource
                            + "] because it is exposed to package '"
                            + entry.getKey()
                            + "' from resources "
                            + Util.getSymbolicName(sourceBlame.m_cap.getResource())
                            + " [" + sourceBlame.m_cap.getResource()
                            + "] and "
                            + Util.getSymbolicName(blame.m_cap.getResource())
                            + " [" + blame.m_cap.getResource()
                            + "] via two dependency chains.\n\nChain 1:\n"
                            + toStringBlame(rc, allCandidates, sourceBlame)
                            + "\n\nChain 2:\n"
                            + toStringBlame(rc, allCandidates, blame),
                            null,
                            Collections.singleton(blame.m_reqs.get(0)));
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

        // Check if there are any uses conflicts with exported packages.
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
                if (!isCompatible(rc, exportBlame.m_cap, usedBlame.m_cap, resourcePkgMap))
                {
                    // Create a candidate permutation that eliminates all candidates
                    // that conflict with existing selected candidates.
                    permutation = (permutation != null)
                        ? permutation
                        : allCandidates.copy();
                    rethrow = (rethrow != null)
                        ? rethrow
                        : new ResolutionException(
                            "Uses constraint violation. Unable to resolve resource "
                            + Util.getSymbolicName(resource)
                            + " [" + resource
                            + "] because it exports package '"
                            + pkgName
                            + "' and is also exposed to it from resource "
                            + Util.getSymbolicName(usedBlame.m_cap.getResource())
                            + " [" + usedBlame.m_cap.getResource()
                            + "] via the following dependency chain:\n\n"
                            + toStringBlame(rc, allCandidates, usedBlame),
                            null,
                            null);

                    mutated = (mutated != null)
                        ? mutated
                        : new HashSet<Requirement>();

                    for (int reqIdx = usedBlame.m_reqs.size() - 1; reqIdx >= 0; reqIdx--)
                    {
                        Requirement req = usedBlame.m_reqs.get(reqIdx);

                        // If we've already permutated this requirement in another
                        // uses constraint, don't permutate it again just continue
                        // with the next uses constraint.
                        if (mutated.contains(req))
                        {
                            break;
                        }

                        // See if we can permutate the candidates for blamed
                        // requirement; there may be no candidates if the resource
                        // associated with the requirement is already resolved.
                        List<Capability> candidates = permutation.getCandidates(req);
                        if ((candidates != null) && (candidates.size() > 1))
                        {
                            mutated.add(req);
                            // Remove the conflicting candidate.
                            candidates.remove(0);
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
                    if (!isCompatible(rc, importBlame.m_cap, usedBlame.m_cap, resourcePkgMap))
                    {
                        // Create a candidate permutation that eliminates any candidates
                        // that conflict with existing selected candidates.
                        permutation = (permutation != null)
                            ? permutation
                            : allCandidates.copy();
                        rethrow = (rethrow != null)
                            ? rethrow
                            : new ResolutionException(
                                "Uses constraint violation. Unable to resolve resource "
                                + Util.getSymbolicName(resource)
                                + " [" + resource
                                + "] because it is exposed to package '"
                                + pkgName
                                + "' from resources "
                                + Util.getSymbolicName(importBlame.m_cap.getResource())
                                + " [" + importBlame.m_cap.getResource()
                                + "] and "
                                + Util.getSymbolicName(usedBlame.m_cap.getResource())
                                + " [" + usedBlame.m_cap.getResource()
                                + "] via two dependency chains.\n\nChain 1:\n"
                                + toStringBlame(rc, allCandidates, importBlame)
                                + "\n\nChain 2:\n"
                                + toStringBlame(rc, allCandidates, usedBlame),
                                null,
                                null);

                        mutated = (mutated != null)
                            ? mutated
                            : new HashSet();

                        for (int reqIdx = usedBlame.m_reqs.size() - 1; reqIdx >= 0; reqIdx--)
                        {
                            Requirement req = usedBlame.m_reqs.get(reqIdx);

                            // If we've already permutated this requirement in another
                            // uses constraint, don't permutate it again just continue
                            // with the next uses constraint.
                            if (mutated.contains(req))
                            {
                                break;
                            }

                            // See if we can permutate the candidates for blamed
                            // requirement; there may be no candidates if the resource
                            // associated with the requirement is already resolved.
                            List<Capability> candidates = permutation.getCandidates(req);
                            if ((candidates != null) && (candidates.size() > 1))
                            {
                                mutated.add(req);
                                // Remove the conflicting candidate.
                                candidates.remove(0);
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
                    Requirement req = importBlame.m_reqs.get(0);
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

        resultCache.put(resource, Boolean.TRUE);

        // Now check the consistency of all resources on which the
        // current resource depends. Keep track of the current number
        // of permutations so we know if the lower level check was
        // able to create a permutation or not in the case of failure.
        int permCount = m_usesPermutations.size() + m_importPermutations.size();
        for (Entry<String, List<Blame>> entry : pkgs.m_importedPkgs.entrySet())
        {
            for (Blame importBlame : entry.getValue())
            {
                if (!resource.equals(importBlame.m_cap.getResource()))
                {
                    try
                    {
                        checkPackageSpaceConsistency(
                            rc, importBlame.m_cap.getResource(),
                            allCandidates, resourcePkgMap, resultCache);
                    }
                    catch (ResolutionException ex)
                    {
                        // If the lower level check didn't create any permutations,
                        // then we should create an import permutation for the
                        // requirement with the dependency on the failing resource
                        // to backtrack on our current candidate selection.
                        if (permCount == (m_usesPermutations.size() + m_importPermutations.size()))
                        {
                            Requirement req = importBlame.m_reqs.get(0);
                            permutate(allCandidates, req, m_importPermutations);
                        }
                        throw ex;
                    }
                }
            }
        }
    }

    private static void permutate(
        Candidates allCandidates, Requirement req, List<Candidates> permutations)
    {
        List<Capability> candidates = allCandidates.getCandidates(req);
        if ((candidates != null) && (candidates.size() > 1))
        {
            Candidates perm = allCandidates.copy();
            candidates = perm.getCandidates(req);
            candidates.remove(0);
            permutations.add(perm);
        }
    }

    private static void permutateIfNeeded(
        Candidates allCandidates, Requirement req, List<Candidates> permutations)
    {
        List<Capability> candidates = allCandidates.getCandidates(req);
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
                List<Capability> existingPermCands = existingPerm.getCandidates(req);
                if (!existingPermCands.get(0).equals(candidates.get(0)))
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
        ResolveContext rc,
        Resource resource,
        Candidates allCandidates,
        Map<Resource, Packages> resourcePkgMap)
    {
        Packages packages = resourcePkgMap.get(resource);
        if (packages != null)
        {
            return;
        }
        packages = new Packages(resource);

        // Get all exported packages.
        Wiring wiring = rc.getWirings().get(resource);
        List<Capability> caps = (wiring != null)
            ? wiring.getResourceCapabilities(null)
            : resource.getCapabilities(null);
        Map<String, Capability> exports = new HashMap<String, Capability>(caps.size());
        for (Capability cap : caps)
        {
            if (cap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
            {
                if (!cap.getResource().equals(resource))
                {
                    cap = new WrappedCapability(resource, cap);
                }
                exports.put(
                    (String) cap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE),
                    cap);
            }
        }
        // Remove substitutable exports that were imported.
        // For resolved resources Wiring.getCapabilities()
        // already excludes imported substitutable exports, but
        // for resolving resources we must look in the candidate
        // map to determine which exports are substitutable.
        if (!exports.isEmpty())
        {
            if (wiring == null)
            {
                for (Requirement req : resource.getRequirements(null))
                {
                    if (req.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
                    {
                        List<Capability> cands = allCandidates.getCandidates(req);
                        if ((cands != null) && !cands.isEmpty())
                        {
                            String pkgName = (String) cands.get(0)
                                .getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE);
                            exports.remove(pkgName);
                        }
                    }
                }
            }

            // Add all non-substituted exports to the resources's package space.
            for (Entry<String, Capability> entry : exports.entrySet())
            {
                packages.m_exportedPkgs.put(
                    entry.getKey(), new Blame(entry.getValue(), null));
            }
        }

        resourcePkgMap.put(resource, packages);
    }

    private boolean isCompatible(
        ResolveContext rc, Capability currentCap, Capability candCap,
        Map<Resource, Packages> resourcePkgMap)
    {
        if ((currentCap != null) && (candCap != null))
        {
            if (currentCap.equals(candCap))
            {
                return true;
            }

            List<Capability> currentSources =
                getPackageSources(
                    rc,
                    currentCap,
                    resourcePkgMap);
            List<Capability> candSources =
                getPackageSources(
                    rc,
                    candCap,
                    resourcePkgMap);

            return currentSources.containsAll(candSources)
                || candSources.containsAll(currentSources);
        }
        return true;
    }

    private Map<Capability, List<Capability>> m_packageSourcesCache = new HashMap();

    private List<Capability> getPackageSources(
        ResolveContext rc, Capability cap, Map<Resource, Packages> resourcePkgMap)
    {
        // If it is a package, then calculate sources for it.
        if (cap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
        {
            List<Capability> sources = m_packageSourcesCache.get(cap);
            if (sources == null)
            {
                sources = getPackageSourcesInternal(
                    rc, cap, resourcePkgMap, new ArrayList(), new HashSet());
                m_packageSourcesCache.put(cap, sources);
            }
            return sources;
        }

        // Otherwise, need to return generic capabilies that have
        // uses constraints so they are included for consistency
        // checking.
        String uses = cap.getDirectives().get(Namespace.CAPABILITY_USES_DIRECTIVE);
        if ((uses != null) && (uses.length() > 0))
        {
            return Collections.singletonList(cap);
        }

        return Collections.EMPTY_LIST;
    }

    private static List<Capability> getPackageSourcesInternal(
        ResolveContext rc, Capability cap, Map<Resource, Packages> resourcePkgMap,
        List<Capability> sources, Set<Capability> cycleMap)
    {
        if (cap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
        {
            if (cycleMap.contains(cap))
            {
                return sources;
            }
            cycleMap.add(cap);

            // Get the package name associated with the capability.
            String pkgName = cap.getAttributes()
                .get(PackageNamespace.PACKAGE_NAMESPACE).toString();

            // Since a resource can export the same package more than once, get
            // all package capabilities for the specified package name.
            Wiring wiring = rc.getWirings().get(cap.getResource());
            List<Capability> caps = (wiring != null)
                ? wiring.getResourceCapabilities(null)
                : cap.getResource().getCapabilities(null);
            for (Capability sourceCap : caps)
            {
                if (sourceCap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE)
                    && sourceCap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE).equals(pkgName))
                {
                    // Since capabilities may come from fragments, we need to check
                    // for that case and wrap them.
                    if (!cap.getResource().equals(sourceCap.getResource()))
                    {
                        sources.add(new WrappedCapability(cap.getResource(), sourceCap));
                    }
                    else
                    {
                        sources.add(sourceCap);
                    }
                }
            }

            // Then get any addition sources for the package from required bundles.
            Packages pkgs = resourcePkgMap.get(cap.getResource());
            List<Blame> required = pkgs.m_requiredPkgs.get(pkgName);
            if (required != null)
            {
                for (Blame blame : required)
                {
                    getPackageSourcesInternal(rc, blame.m_cap, resourcePkgMap, sources, cycleMap);
                }
            }
        }

        return sources;
    }

    private static Resource getDeclaredResource(Resource resource)
    {
        if (resource instanceof WrappedResource)
        {
            return ((WrappedResource) resource).getDeclaredResource();
        }
        return resource;
    }

    private static Capability getDeclaredCapability(Capability c)
    {
        if (c instanceof HostedCapability)
        {
            return ((HostedCapability) c).getDeclaredCapability();
        }
        return c;
    }

    private static Requirement getDeclaredRequirement(Requirement r)
    {
        if (r instanceof WrappedRequirement)
        {
            return ((WrappedRequirement) r).getDeclaredRequirement();
        }
        return r;
    }

    private static Map<Resource, List<Wire>> populateWireMap(
        ResolveContext rc, Resource resource, Map<Resource, Packages> resourcePkgMap,
        Map<Resource, List<Wire>> wireMap, Candidates allCandidates)
    {
        Resource unwrappedResource = getDeclaredResource(resource);
        if (!rc.getWirings().containsKey(unwrappedResource)
            && !wireMap.containsKey(unwrappedResource))
        {
            wireMap.put(unwrappedResource, (List<Wire>) Collections.EMPTY_LIST);

            List<Wire> packageWires = new ArrayList<Wire>();
            List<Wire> bundleWires = new ArrayList<Wire>();
            List<Wire> capabilityWires = new ArrayList<Wire>();

            for (Requirement req : resource.getRequirements(null))
            {
                List<Capability> cands = allCandidates.getCandidates(req);
                if ((cands != null) && (cands.size() > 0))
                {
                    Capability cand = cands.get(0);
                    // Do not create wires for the osgi.wiring.* namespaces
                    // if the provider and requirer are the same resource;
                    // allow such wires for non-OSGi wiring namespaces.
                    if (!cand.getNamespace().startsWith("osgi.wiring.")
                        || !resource.equals(cand.getResource()))
                    {
                        if (!rc.getWirings().containsKey(cand.getResource()))
                        {
                            populateWireMap(rc, cand.getResource(),
                                resourcePkgMap, wireMap, allCandidates);
                        }
                        Wire wire = new WireImpl(
                            unwrappedResource,
                            getDeclaredRequirement(req),
                            getDeclaredResource(cand.getResource()),
                            getDeclaredCapability(cand));
                        if (req.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
                        {
                            packageWires.add(wire);
                        }
                        else if (req.getNamespace().equals(BundleNamespace.BUNDLE_NAMESPACE))
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
            wireMap.put(unwrappedResource, packageWires);

            // Add host wire for any fragments.
            if (resource instanceof WrappedResource)
            {
                List<Resource> fragments = ((WrappedResource) resource).getFragments();
                for (Resource fragment : fragments)
                {
                    // Get wire list for the fragment from the wire map.
                    // If there isn't one, then create one. Note that we won't
                    // add the wire list to the wire map until the end, so
                    // we can determine below if this is the first time we've
                    // seen the fragment while populating wires to avoid
                    // creating duplicate non-payload wires if the fragment
                    // is attached to more than one host.
                    List<Wire> fragmentWires = wireMap.get(fragment);
                    fragmentWires = (fragmentWires == null)
                        ? new ArrayList<Wire>() : fragmentWires;

                    // Loop through all of the fragment's requirements and create
                    // any necessary wires for non-payload requirements.
                    for (Requirement req : fragment.getRequirements(null))
                    {
                        // Only look at non-payload requirements.
                        if (!isPayload(req))
                        {
                            // If this is the host requirement, then always create
                            // a wire for it to the current resource.
                            if (req.getNamespace().equals(HostNamespace.HOST_NAMESPACE))
                            {
                                fragmentWires.add(
                                    new WireImpl(
                                        getDeclaredResource(fragment),
                                        req,
                                        unwrappedResource,
                                        unwrappedResource.getCapabilities(
                                            HostNamespace.HOST_NAMESPACE).get(0)));
                            }
                            // Otherwise, if the fragment isn't already resolved and
                            // this is the first time we are seeing it, then create
                            // a wire for the non-payload requirement.
                            else if (!rc.getWirings().containsKey(fragment)
                                && !wireMap.containsKey(fragment))
                            {
                                Wire wire = createWire(req, allCandidates);
                                if (wire != null)
                                {
                                    fragmentWires.add(wire);
                                }
                            }
                        }
                    }

                    // Finally, add the fragment's wire list to the wire map.
                    wireMap.put(fragment, fragmentWires);
                }
            }
        }

        return wireMap;
    }

    private static Wire createWire(Requirement requirement, Candidates allCandidates)
    {
        List<Capability> candidates = allCandidates.getCandidates(requirement);
        if (candidates == null || candidates.isEmpty())
        {
            return null;
        }
        Capability cand = candidates.get(0);
        return new WireImpl(
                getDeclaredResource(requirement.getResource()),
                getDeclaredRequirement(requirement),
                getDeclaredResource(cand.getResource()),
                getDeclaredCapability(cand));
    }

    private static boolean isPayload(Requirement fragmentReq)
    {
        // this is where we would add other non-payload namespaces
        if (ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE
            .equals(fragmentReq.getNamespace()))
        {
            return false;
        }
        if (HostNamespace.HOST_NAMESPACE.equals(fragmentReq.getNamespace()))
        {
            return false;
        }
        return true;
    }

	private static Map<Resource, List<Wire>> populateDynamicWireMap(
        ResolveContext rc, Resource resource, Requirement dynReq,
        Map<Resource, Packages> resourcePkgMap,
        Map<Resource, List<Wire>> wireMap, Candidates allCandidates)
    {
        wireMap.put(resource, (List<Wire>) Collections.EMPTY_LIST);

        List<Wire> packageWires = new ArrayList<Wire>();

        // Get the candidates for the current dynamic requirement.
        List<Capability> candCaps = allCandidates.getCandidates(dynReq);
        // Record the dynamic candidate.
        Capability dynCand = candCaps.get(0);

        if (!rc.getWirings().containsKey(dynCand.getResource()))
        {
            populateWireMap(rc, dynCand.getResource(), resourcePkgMap,
                wireMap, allCandidates);
        }

        packageWires.add(
            new WireImpl(
                resource,
                dynReq,
                getDeclaredResource(dynCand.getResource()),
                getDeclaredCapability(dynCand)));

        wireMap.put(resource, packageWires);

        return wireMap;
    }

    private static void dumpResourcePkgMap(
        ResolveContext rc, Map<Resource, Packages> resourcePkgMap)
    {
        System.out.println("+++RESOURCE PKG MAP+++");
        for (Entry<Resource, Packages> entry : resourcePkgMap.entrySet())
        {
            dumpResourcePkgs(rc, entry.getKey(), entry.getValue());
        }
    }

    private static void dumpResourcePkgs(
        ResolveContext rc, Resource resource, Packages packages)
    {
        Wiring wiring = rc.getWirings().get(resource);
        System.out.println(resource
            + " (" + ((wiring != null) ? "RESOLVED)" : "UNRESOLVED)"));
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

    private static String toStringBlame(
        ResolveContext rc, Candidates allCandidates, Blame blame)
    {
        StringBuffer sb = new StringBuffer();
        if ((blame.m_reqs != null) && !blame.m_reqs.isEmpty())
        {
            for (int i = 0; i < blame.m_reqs.size(); i++)
            {
                Requirement req = blame.m_reqs.get(i);
                sb.append("  ");
                sb.append(Util.getSymbolicName(req.getResource()));
                sb.append(" [");
                sb.append(req.getResource().toString());
                sb.append("]\n");
                if (req.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
                {
                    sb.append("    import: ");
                }
                else
                {
                    sb.append("    require: ");
                }
                sb.append(req.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE));
                sb.append("\n     |");
                if (req.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
                {
                    sb.append("\n    export: ");
                }
                else
                {
                    sb.append("\n    provide: ");
                }
                if ((i + 1) < blame.m_reqs.size())
                {
                    Capability cap = getSatisfyingCapability(
                        rc,
                        allCandidates,
                        blame.m_reqs.get(i));
                    if (cap.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE))
                    {
                        sb.append(PackageNamespace.PACKAGE_NAMESPACE);
                        sb.append("=");
                        sb.append(cap.getAttributes()
                            .get(PackageNamespace.PACKAGE_NAMESPACE).toString());
                        Capability usedCap =
                            getSatisfyingCapability(
                                rc,
                                allCandidates,
                                blame.m_reqs.get(i + 1));
                        sb.append("; uses:=");
                        sb.append(usedCap.getAttributes()
                            .get(PackageNamespace.PACKAGE_NAMESPACE));
                    }
                    else
                    {
                        sb.append(cap);
                    }
                    sb.append("\n");
                }
                else
                {
                    Capability export = getSatisfyingCapability(
                        rc,
                        allCandidates,
                        blame.m_reqs.get(i));
                    sb.append(export.getNamespace());
                    sb.append("=");
                    sb.append(export.getAttributes().get(export.getNamespace()).toString());
                    if (export.getNamespace().equals(PackageNamespace.PACKAGE_NAMESPACE)
                        && !export.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE)
                            .equals(blame.m_cap.getAttributes().get(
                                PackageNamespace.PACKAGE_NAMESPACE)))
                    {
                        sb.append("; uses:=");
                        sb.append(blame.m_cap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
                        sb.append("\n    export: ");
                        sb.append(PackageNamespace.PACKAGE_NAMESPACE);
                        sb.append("=");
                        sb.append(blame.m_cap.getAttributes()
                            .get(PackageNamespace.PACKAGE_NAMESPACE).toString());
                    }
                    sb.append("\n  ");
                    sb.append(Util.getSymbolicName(blame.m_cap.getResource()));
                    sb.append(" [");
                    sb.append(blame.m_cap.getResource().toString());
                    sb.append("]");
                }
            }
        }
        else
        {
            sb.append(blame.m_cap.getResource().toString());
        }
        return sb.toString();
    }

    private static Capability getSatisfyingCapability(
        ResolveContext rc, Candidates allCandidates, Requirement req)
    {
        Capability cap = null;

        // If the requiring revision is not resolved, then check in the
        // candidate map for its matching candidate.
        List<Capability> cands = allCandidates.getCandidates(req);
        if (cands != null)
        {
            cap = cands.get(0);
        }
        // Otherwise, if the requiring revision is resolved then check
        // in its wires for the capability satisfying the requirement.
        else if (rc.getWirings().containsKey(req.getResource()))
        {
            List<Wire> wires =
                rc.getWirings().get(req.getResource()).getRequiredResourceWires(null);
            req = getDeclaredRequirement(req);
            for (Wire w : wires)
            {
                if (w.getRequirement().equals(req))
                {
// TODO: RESOLVER - This is not 100% correct, since requirements for
//       dynamic imports with wildcards will reside on many wires and
//       this code only finds the first one, not necessarily the correct
//       one. This is only used for the diagnostic message, but it still
//       could confuse the user.
                    cap = w.getCapability();
                    break;
                }
            }
        }

        return cap;
    }

    private static class Packages
    {
        private final Resource m_resource;
        public final Map<String, Blame> m_exportedPkgs = new HashMap();
        public final Map<String, List<Blame>> m_importedPkgs = new HashMap();
        public final Map<String, List<Blame>> m_requiredPkgs = new HashMap();
        public final Map<String, List<Blame>> m_usedPkgs = new HashMap();
        public boolean m_isCalculated = false;

        public Packages(Resource resource)
        {
            m_resource = resource;
        }
    }

    private static class Blame
    {
        public final Capability m_cap;
        public final List<Requirement> m_reqs;

        public Blame(Capability cap, List<Requirement> reqs)
        {
            m_cap = cap;
            m_reqs = reqs;
        }

        @Override
        public String toString()
        {
            return m_cap.getResource()
                + "." + m_cap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE)
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