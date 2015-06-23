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

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.felix.resolver.ResolverImpl.Blame;
import org.apache.felix.resolver.ResolverImpl.Packages;
import org.apache.felix.resolver.ResolverImpl.ResolveSession;
import org.apache.felix.resolver.ResolverImpl.UsedBlames;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.ResolutionException;
import org.osgi.service.resolver.ResolveContext;

public class ConsistencyChecker {
    private ResolveSession session;
    private Candidates allCandidates;
    private Logger m_logger;

    public ConsistencyChecker(ResolveSession session, Candidates allCandidates, Logger m_logger) {
        this.session = session;
        this.allCandidates = allCandidates;
        this.m_logger = m_logger;
    }

    public void checkPackageSpace(
        Resource resource,
        Map<Resource, Packages> resourcePkgMap,
        Map<Resource, Object> resultCache) throws ResolutionException
    {
        if (session.getContext().getWirings().containsKey(resource))
        {
            return;
        }
        checkDynamicPackageSpace(resource, resourcePkgMap, resultCache);
    }

    public void checkDynamicPackageSpace(
        Resource resource,
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

        List<Candidates> importPermutations = session.getImportPermutations();
        List<Candidates> usesPermutations = session.getUsesPermutations();

        // Check for conflicting imports from fragments.
        // TODO: Is this only needed for imports or are generic and bundle requirements also needed?
            //       I think this is only a special case for fragment imports because they can overlap
            //       host imports, which is not allowed in normal metadata.
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
                            allCandidates.permutate(blame.m_reqs.get(0), importPermutations);
                            // Try to permutate the source requirement.
                            allCandidates.permutate(sourceBlame.m_reqs.get(0), importPermutations);
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
                                                                                 + toStringBlame(session.getContext(), allCandidates, sourceBlame)
                                                                                 + "\n\nChain 2:\n"
                                                                                 + toStringBlame(session.getContext(), allCandidates, blame),
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
                for (UsedBlames usedBlames : pkgs.m_usedPkgs.get(pkgName).values())
                {
                    if (!isCompatible(Collections.singletonList(exportBlame), usedBlames.m_cap, resourcePkgMap))
                    {
                        for (Blame usedBlame : usedBlames.m_blames)
                        {
                            if (checkMultiple(usedBlames, usedBlame, allCandidates))
                            {
                                // Continue to the next usedBlame, if possible we
                                // removed the conflicting candidates.
                                continue;
                            }
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
                                                                  + toStringBlame(session.getContext(), allCandidates, usedBlame),
                                                                  null,
                                                                  null);

                            mutated = (mutated != null)
                                ? mutated
                                    : new HashSet<Requirement>();

                            for (int reqIdx = usedBlame.m_reqs.size() - 1; reqIdx >= 0; reqIdx--)
                            {
                                Requirement req = usedBlame.m_reqs.get(reqIdx);
                                // Sanity check for multiple.
                                if (Util.isMultiple(req))
                                {
                                    continue;
                                }
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
                                if (permutation.canRemoveCandidate(req)) {
                                    permutation.removeFirstCandidate(req);
                                    mutated.add(req);
                                    break;
                                }
                            }
                        }
                    }
                }

                if (rethrow != null)
                {
                    if (!mutated.isEmpty())
                    {
                        usesPermutations.add(permutation);
                    }
                    m_logger.log(
                                 Logger.LOG_DEBUG,
                                 "Candidate permutation failed due to a conflict between "
                                     + "an export and import; will try another if possible.",
                                     rethrow);
                    throw rethrow;
                }
            }

            // Check if there are any uses conflicts with imported and required packages.
            // We combine the imported and required packages here into one map.
            // Imported packages are added after required packages because they shadow or override
            // the packages from required bundles.
            Map<String, List<Blame>> allImportRequirePkgs =
                new LinkedHashMap<String, List<Blame>>(pkgs.m_requiredPkgs.size() + pkgs.m_importedPkgs.size());
            allImportRequirePkgs.putAll(pkgs.m_requiredPkgs);
            allImportRequirePkgs.putAll(pkgs.m_importedPkgs);

            for (Entry<String, List<Blame>> requirementBlames : allImportRequirePkgs.entrySet())
            {
                String pkgName = requirementBlames.getKey();
                if (!pkgs.m_usedPkgs.containsKey(pkgName))
                {
                    continue;
                }

                for (UsedBlames usedBlames : pkgs.m_usedPkgs.get(pkgName).values())
                {
                    if (!isCompatible(requirementBlames.getValue(), usedBlames.m_cap, resourcePkgMap))
                    {
                        // Split packages, need to think how to get a good message for split packages (sigh)
                        // For now we just use the first requirement that brings in the package that conflicts
                        Blame requirementBlame = requirementBlames.getValue().get(0);
                            for (Blame usedBlame : usedBlames.m_blames)
                            {
                                if (checkMultiple(usedBlames, usedBlame, allCandidates))
                                {
                                    // Continue to the next usedBlame, if possible we
                                    // removed the conflicting candidates.
                                    continue;
                                }
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
                                                                      + "] because it is exposed to package '"
                                                                      + pkgName
                                                                      + "' from resources "
                                                                      + Util.getSymbolicName(requirementBlame.m_cap.getResource())
                                                                      + " [" + requirementBlame.m_cap.getResource()
                                                                      + "] and "
                                                                      + Util.getSymbolicName(usedBlame.m_cap.getResource())
                                                                      + " [" + usedBlame.m_cap.getResource()
                                                                      + "] via two dependency chains.\n\nChain 1:\n"
                                                                      + toStringBlame(session.getContext(), allCandidates, requirementBlame)
                                                                      + "\n\nChain 2:\n"
                                                                      + toStringBlame(session.getContext(), allCandidates, usedBlame),
                                                                      null,
                                                                      null);

                                mutated = (mutated != null)
                                    ? mutated
                                        : new HashSet<Requirement>();

                                for (int reqIdx = usedBlame.m_reqs.size() - 1; reqIdx >= 0; reqIdx--)
                                {
                                    Requirement req = usedBlame.m_reqs.get(reqIdx);
                                    // Sanity check for multiple.
                                    if (Util.isMultiple(req))
                                    {
                                        continue;
                                    }
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
                                    if (permutation.canRemoveCandidate(req)) {
                                        permutation.removeFirstCandidate(req);
                                        mutated.add(req);
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
                        if (!mutated.isEmpty())
                        {
                            usesPermutations.add(permutation);
                        }

                        // Try to permutate the candidate for the original
                        // import requirement; only permutate it if we haven't
                        // done so already.
                        for (Blame requirementBlame : requirementBlames.getValue())
                        {
                            Requirement req = requirementBlame.m_reqs.get(0);
                            if (!mutated.contains(req))
                            {
                                // Since there may be lots of uses constraint violations
                                // with existing import decisions, we may end up trying
                                // to permutate the same import a lot of times, so we should
                                // try to check if that the case and only permutate it once.
                                allCandidates.permutateIfNeeded(req, importPermutations);
                            }
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
            int permCount = usesPermutations.size() + importPermutations.size();
            for (Requirement req : resource.getRequirements(null))
            {
                Capability cap = allCandidates.getFirstCandidate(req);
                if (cap != null)
                {
                    if (!resource.equals(cap.getResource()))
                    {
                        try
                        {
                            checkPackageSpace(cap.getResource(), resourcePkgMap, resultCache);
                        }
                        catch (ResolutionException ex)
                        {
                            // If the lower level check didn't create any permutations,
                            // then we should create an import permutation for the
                            // requirement with the dependency on the failing resource
                            // to backtrack on our current candidate selection.
                            if (permCount == (usesPermutations.size() + importPermutations.size()))
                            {
                                allCandidates.permutate(req, importPermutations);
                            }
                            throw ex;
                        }
                    }
                }
            }
    }
    
    private boolean checkMultiple(
            UsedBlames usedBlames,
            Blame usedBlame,
            Candidates permutation)
    {
        // Check the root requirement to see if it is a multiple cardinality
        // requirement.
        List<Capability> candidates = null;
        Requirement req = usedBlame.m_reqs.get(0);
        if (Util.isMultiple(req))
        {
            // Create a copy of the current permutation so we can remove the
            // candidates causing the blame.
            if (session.getMultipleCardCandidates() == null)
            {
                session.setMultipleCardCandidates(permutation.copy());
            }
            // Get the current candidate list and remove all the offending root
            // cause candidates from a copy of the current permutation.
            candidates = session.getMultipleCardCandidates().clearCandidates(req, usedBlames.getRootCauses(req));
        }
        // We only are successful if there is at least one candidate left
        // for the requirement
        return (candidates != null) && !candidates.isEmpty();
    }
    
    private static String toStringBlame(ResolveContext rc, Candidates allCandidates, Blame blame)
    {
        StringBuilder sb = new StringBuilder();
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
                    sb.append(": ");
                    Object namespaceVal = export.getAttributes().get(export.getNamespace());
                    if (namespaceVal != null)
                    {
                        sb.append(namespaceVal.toString());
                    }
                    else
                    {
                        for (Entry<String, Object> attrEntry : export.getAttributes().entrySet())
                        {
                            sb.append(attrEntry.getKey()).append('=')
                            .append(attrEntry.getValue()).append(';');
                        }
                    }
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
        // If the requiring revision is not resolved, then check in the
        // candidate map for its matching candidate.
        Capability cap = allCandidates.getFirstCandidate(req);
        // Otherwise, if the requiring revision is resolved then check
        // in its wires for the capability satisfying the requirement.
        if (cap == null && rc.getWirings().containsKey(req.getResource()))
        {
            List<Wire> wires =
                rc.getWirings().get(req.getResource()).getRequiredResourceWires(null);
            req = Util.getDeclaredRequirement(req);
            for (Wire w : wires)
            {
                if (w.getRequirement().equals(req))
                {
                    // TODO: RESOLVER - This is not 100% correct, since requirements for
                    // dynamic imports with wildcards will reside on many wires and
                    // this code only finds the first one, not necessarily the correct
                    // one. This is only used for the diagnostic message, but it still
                    // could confuse the user.
                    cap = w.getCapability();
                    break;
                }
            }
        }

        return cap;
    }
    
    private boolean isCompatible(
            List<Blame> currentBlames, Capability candCap,
            Map<Resource, Packages> resourcePkgMap)
    {
        CapabilityFinder finder = session.getCapabilityFinder();
        if ((!currentBlames.isEmpty()) && (candCap != null))
        {
            Set<Capability> currentSources;
            // quick check for single source package
            if (currentBlames.size() == 1)
            {
                Capability currentCap = currentBlames.get(0).m_cap;
                if (currentCap.equals(candCap))
                {
                    return true;
                }
                currentSources = finder.getPackageSources(currentCap, resourcePkgMap);
            }
            else
            {
                currentSources = new HashSet<Capability>(currentBlames.size());
                for (Blame currentBlame : currentBlames)
                {
                    Set<Capability> blameSources = finder.getPackageSources(currentBlame.m_cap, resourcePkgMap);
                    for (Capability blameSource : blameSources)
                    {
                        currentSources.add(blameSource);
                    }
                }
            }

            Set<Capability> candSources = finder.getPackageSources(candCap, resourcePkgMap);

            return currentSources.containsAll(candSources)
                    || candSources.containsAll(currentSources);
        }
        return true;
    }

}
