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
package org.apache.felix.bundlerepository.impl;

import java.net.URL;
import java.util.*;

import org.apache.felix.bundlerepository.*;
import org.apache.felix.utils.log.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class ResolverImpl implements Resolver
{
    private final BundleContext m_context;
    private final Logger m_logger;
    private final Repository[] m_repositories;
    private final Set<Resource> m_addedSet = new HashSet<Resource>();
    private final Set<Requirement> m_addedRequirementSet = new HashSet<Requirement>();
    private final Set<Capability> m_globalCapabilities = new HashSet<Capability>();
    private final Set<Resource> m_failedSet = new HashSet<Resource>();
    private final Set<Resource> m_resolveSet = new HashSet<Resource>();
    private final Set<Resource> m_requiredSet = new HashSet<Resource>();
    private final Set<Resource> m_optionalSet = new HashSet<Resource>();
    private final Map<Resource, List<Reason>> m_reasonMap = new HashMap<Resource, List<Reason>>();
    private final Set<Reason> m_unsatisfiedSet = new HashSet<Reason>();
    private boolean m_resolved = false;
    private long m_resolveTimeStamp;
    private int m_resolutionFlags;

    public ResolverImpl(BundleContext context, Repository[] repositories, Logger logger)
    {
        m_context = context;
        m_logger = logger;
        m_repositories = repositories;
    }

    public synchronized void add(Resource resource)
    {
        m_resolved = false;
        m_addedSet.add(resource);
    }

    public synchronized Resource[] getAddedResources()
    {
        return m_addedSet.toArray(new Resource[m_addedSet.size()]);
    }

    public synchronized void add(Requirement requirement)
    {
        m_resolved = false;
        m_addedRequirementSet.add(requirement);
    }

    public synchronized Requirement[] getAddedRequirements()
    {
        return m_addedRequirementSet.toArray(new Requirement[m_addedRequirementSet.size()]);
    }

    public void addGlobalCapability(Capability capability)
    {
        m_globalCapabilities.add(capability);
    }

    public Capability[] getGlobalCapabilities()
    {
        return m_globalCapabilities.toArray(new Capability[m_globalCapabilities.size()]);
    }

    public synchronized Resource[] getRequiredResources()
    {
        if (m_resolved)
        {
            return m_requiredSet.toArray(new Resource[m_requiredSet.size()]);
        }
        throw new IllegalStateException("The resources have not been resolved.");
    }

    public synchronized Resource[] getOptionalResources()
    {
        if (m_resolved)
        {
            return m_optionalSet.toArray(new Resource[m_optionalSet.size()]);
        }
        throw new IllegalStateException("The resources have not been resolved.");
    }

    public synchronized Reason[] getReason(Resource resource)
    {
        if (m_resolved)
        {
            List<Reason> l = m_reasonMap.get(resource);
            return l != null ? l.toArray(new Reason[l.size()]) : null;
        }
        throw new IllegalStateException("The resources have not been resolved.");
    }

    public synchronized Reason[] getUnsatisfiedRequirements()
    {
        if (m_resolved)
        {
            return m_unsatisfiedSet.toArray(new Reason[m_unsatisfiedSet.size()]);
        }
        throw new IllegalStateException("The resources have not been resolved.");
    }

    protected LocalResource[] getLocalResources()
    {
        List<LocalResource> resources = new ArrayList<LocalResource>();
        for (Resource resource : getResources())
        {
            if (resource != null && resource.isLocal())
            {
                resources.add((LocalResource) resource);
            }
        }
        return resources.toArray(new LocalResource[resources.size()]);
    }

    private Resource[] getRemoteResources()
    {
        List<Resource> resources = new ArrayList<Resource>();
        for (Resource resource : getResources())
        {
            if (resource != null && !resource.isLocal())
            {
                resources.add(resource);
            }
        }
        return resources.toArray(new Resource[resources.size()]);
    }

    private Resource[] getResources()
    {
        List<Resource> resources = new ArrayList<Resource>();
        for (int repoIdx = 0; (m_repositories != null) && (repoIdx < m_repositories.length); repoIdx++)
        {
            boolean isLocal = m_repositories[repoIdx].getURI().equals(Repository.LOCAL);
            boolean isSystem = m_repositories[repoIdx].getURI().equals(Repository.SYSTEM);
            if (isLocal && (m_resolutionFlags & NO_LOCAL_RESOURCES) != 0) {
                continue;
            }
            if (isSystem && (m_resolutionFlags & NO_SYSTEM_BUNDLE) != 0) {
                continue;
            }
            Collections.addAll(resources, m_repositories[repoIdx].getResources());
        }
        return resources.toArray(new Resource[resources.size()]);
    }

    public synchronized boolean resolve()
    {
        return resolve(0);
    }

    public synchronized boolean resolve(int flags)
    {
        // Find resources
        Resource[] locals = getLocalResources();
        Resource[] remotes = getRemoteResources();

        // time of the resolution process start
        m_resolveTimeStamp = 0;
        for (int repoIdx = 0; (m_repositories != null) && (repoIdx < m_repositories.length); repoIdx++)
        {
            m_resolveTimeStamp = Math.max(m_resolveTimeStamp, m_repositories[repoIdx].getLastModified());
        }

        // Reset instance values.
        m_failedSet.clear();
        m_resolveSet.clear();
        m_requiredSet.clear();
        m_optionalSet.clear();
        m_reasonMap.clear();
        m_unsatisfiedSet.clear();
        m_resolved = true;
        m_resolutionFlags = flags;

        boolean result = true;

        // Add a fake resource if needed
        if (!m_addedRequirementSet.isEmpty() || !m_globalCapabilities.isEmpty())
        {
            ResourceImpl fake = new ResourceImpl();
            for (Capability cap : m_globalCapabilities) {
                fake.addCapability(cap);
            }
            for (Requirement req : m_addedRequirementSet) {
                fake.addRequire(req);
            }
            if (!resolve(fake, locals, remotes, false))
            {
                result = false;
            }
        }

        // Loop through each resource in added list and resolve.
        for (Resource aM_addedSet : m_addedSet) {
            if (!resolve(aM_addedSet, locals, remotes, false)) {
                // If any resource does not resolve, then the
                // entire result will be false.
                result = false;
            }
        }

        // Clean up the resulting data structures.
        m_requiredSet.removeAll(m_addedSet);
        if ((flags & NO_LOCAL_RESOURCES) == 0)
        {
            m_requiredSet.removeAll(Arrays.asList(locals));
        }
        m_optionalSet.removeAll(m_addedSet);
        m_optionalSet.removeAll(m_requiredSet);
        if ((flags & NO_LOCAL_RESOURCES) == 0)
        {
            m_optionalSet.removeAll(Arrays.asList(locals));
        }

        // Return final result.
        return result;
    }

    private boolean resolve(Resource resource, Resource[] locals, Resource[] remotes, boolean optional)
    {
        boolean result = true;

        // Check for cycles.
        if (m_resolveSet.contains(resource) || m_requiredSet.contains(resource) || m_optionalSet.contains(resource))
        {
            return true;
        }
        else if (m_failedSet.contains(resource))
        {
            return false;
        }

        // Add to resolve map to avoid cycles.
        m_resolveSet.add(resource);

        // Resolve the requirements for the resource according to the
        // search order of: added, resolving, local and finally remote
        // resources.
        Requirement[] reqs = resource.getRequirements();
        if (reqs != null)
        {
            Resource candidate;
            for (Requirement req : reqs) {
                // Do not resolve optional requirements
                if ((m_resolutionFlags & NO_OPTIONAL_RESOURCES) != 0 && req.isOptional()) {
                    continue;
                }
                candidate = searchResources(req, m_addedSet);
                if (candidate == null) {
                    candidate = searchResources(req, m_requiredSet);
                }
                if (candidate == null) {
                    candidate = searchResources(req, m_optionalSet);
                }
                if (candidate == null) {
                    candidate = searchResources(req, m_resolveSet);
                }
                if (candidate == null) {
                    List<ResourceCapability> candidateCapabilities = searchResources(req, locals);
                    candidateCapabilities.addAll(searchResources(req, remotes));

                    // Determine the best candidate available that
                    // can resolve.
                    while ((candidate == null) && !candidateCapabilities.isEmpty()) {
                        ResourceCapability bestCapability = getBestCandidate(candidateCapabilities);

                        // Try to resolve the best resource.
                        if (resolve(bestCapability.getResource(), locals, remotes, optional || req.isOptional())) {
                            candidate = bestCapability.getResource();
                        } else {
                            candidateCapabilities.remove(bestCapability);
                        }
                    }
                }

                if ((candidate == null) && !req.isOptional()) {
                    // The resolve failed.
                    result = false;
                    // Associated the current resource to the requirement
                    // in the unsatisfied requirement set.
                    m_unsatisfiedSet.add(new ReasonImpl(resource, req));
                } else if (candidate != null) {

                    // Try to resolve the candidate.
                    if (resolve(candidate, locals, remotes, optional || req.isOptional())) {
                        // The resolved succeeded; record the candidate
                        // as either optional or required.
                        if (optional || req.isOptional()) {
                            m_optionalSet.add(candidate);
                            m_resolveSet.remove(candidate);
                        } else {
                            m_requiredSet.add(candidate);
                            m_optionalSet.remove(candidate);
                            m_resolveSet.remove(candidate);
                        }

                        // Add the reason why the candidate was selected.
                        List<Reason> reasons = m_reasonMap.get(candidate);
                        if (reasons == null) {
                            reasons = new ArrayList<Reason>();
                            m_reasonMap.put(candidate, reasons);
                        }
                        reasons.add(new ReasonImpl(resource, req));
                    } else {
                        result = false;
                    }
                }
            }
        }

        // If the resolve failed, remove the resource from the resolve set and
        // add it to the failed set to avoid trying to resolve it again.
        if (!result)
        {
            m_resolveSet.remove(resource);
            m_failedSet.add(resource);
        }

        return result;
    }

    private Resource searchResources(Requirement req, Set<Resource> resourceSet)
    {
        for (Resource aResourceSet : resourceSet) {
            checkInterrupt();
            Capability[] caps = aResourceSet.getCapabilities();
            if (caps != null) {
                for (Capability cap : caps) {
                    if (req.isSatisfied(cap)) {
                        // The requirement is already satisfied an existing
                        // resource, return the resource.
                        return aResourceSet;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Searches for resources that do meet the given requirement
     * @param req the the requirement that must be satisfied by resources
     * @param resources list of resources to look at
     * @return all resources meeting the given requirement
     */
    private List<ResourceCapability> searchResources(Requirement req, Resource[] resources)
    {
        List<ResourceCapability> matchingCapabilities = new ArrayList<ResourceCapability>();

        if (resources != null) {
            for (Resource resource : resources) {
                checkInterrupt();
                // We don't need to look at resources we've already looked at.
                if (!m_failedSet.contains(resource)) {
                    Capability[] caps = resource.getCapabilities();
                    if (caps != null) {
                        for (Capability cap : caps) {
                            if (req.isSatisfied(cap))
                                matchingCapabilities.add(new ResourceCapabilityImpl(resource, cap));
                        }
                    }
                }
            }
        }

        return matchingCapabilities;
    }

    /**
     * Determines which resource is preferred to deliver the required capability.
     * This method selects the resource providing the highest version of the capability.
     * If two resources provide the same version of the capability, the resource with
     * the largest number of cabailities be preferred
     * @param caps
     * @return
     */
    private ResourceCapability getBestCandidate(List<ResourceCapability> caps)
    {
        Version bestVersion = null;
        ResourceCapability best = null;
        boolean bestLocal = false;

        for (ResourceCapability cap : caps) {
            boolean isCurrentLocal = cap.getResource().isLocal();

            if (best == null) {
                best = cap;
                bestLocal = isCurrentLocal;
                Object v = cap.getCapability().getPropertiesAsMap().get(Resource.VERSION);
                if ((v != null) && (v instanceof Version)) {
                    bestVersion = (Version) v;
                }
            } else if ((m_resolutionFlags & DO_NOT_PREFER_LOCAL) != 0 || !bestLocal || isCurrentLocal) {
                Object v = cap.getCapability().getPropertiesAsMap().get(Resource.VERSION);

                // If there is no version, then select the resource
                // with the greatest number of capabilities.
                if ((v == null) && (bestVersion == null)
                        && (best.getResource().getCapabilities().length
                        < cap.getResource().getCapabilities().length)) {
                    best = cap;
                    bestLocal = isCurrentLocal;
                    bestVersion = null;
                } else if ((v != null) && (v instanceof Version)) {
                    // If there is no best version or if the current
                    // resource's version is lower, then select it.
                    if ((bestVersion == null) || (bestVersion.compareTo((Version) v) < 0)) {
                        best = cap;
                        bestLocal = isCurrentLocal;
                        bestVersion = (Version) v;
                    }
                    // If the current resource version is equal to the
                    // best
                    else if ((bestVersion.compareTo((Version) v) == 0)) {
                        // If the symbolic name is the same, use the highest
                        // bundle version.
                        if ((best.getResource().getSymbolicName() != null)
                                && best.getResource().getSymbolicName().equals(
                                cap.getResource().getSymbolicName())) {
                            if (best.getResource().getVersion().compareTo(
                                    cap.getResource().getVersion()) < 0) {
                                best = cap;
                                bestLocal = isCurrentLocal;
                                bestVersion = (Version) v;
                            }
                        }
                        // Otherwise select the one with the greatest
                        // number of capabilities.
                        else if (best.getResource().getCapabilities().length
                                < cap.getResource().getCapabilities().length) {
                            best = cap;
                            bestLocal = isCurrentLocal;
                            bestVersion = (Version) v;
                        }
                    }
                }
            }
        }

        return (best == null) ? null : best;
    }

    private void checkInterrupt()
    {
        if (Thread.interrupted())
        {
            throw new org.apache.felix.bundlerepository.InterruptedResolutionException();
        }
    }

    public synchronized void deploy(int flags)
    {
        // Must resolve if not already resolved.
        if (!m_resolved && !resolve(flags))
        {
            m_logger.log(Logger.LOG_ERROR, "Resolver: Cannot resolve target resources.");
            return;
        }

        // Check to make sure that our local state cache is up-to-date
        // and error if it is not. This is not completely safe, because
        // the state can still change during the operation, but we will
        // be optimistic. This could also be made smarter so that it checks
        // to see if the local state changes overlap with the resolver.
        for (int repoIdx = 0; (m_repositories != null) && (repoIdx < m_repositories.length); repoIdx++)
        {
            if (m_repositories[repoIdx].getLastModified() > m_resolveTimeStamp)
            {
                throw new IllegalStateException("Framework state has changed, must resolve again.");
            }
        }

        // Eliminate duplicates from target, required, optional resources.
        Set<Resource> resourceSet = new HashSet<Resource>();
        Resource[] resources = getAddedResources();
        for (int i = 0; (resources != null) && (i < resources.length); i++)
        {
            resourceSet.add(resources[i]);
        }
        resources = getRequiredResources();
        for (int i = 0; (resources != null) && (i < resources.length); i++)
        {
            resourceSet.add(resources[i]);
        }
        if ((flags & NO_OPTIONAL_RESOURCES) == 0)
        {
            resources = getOptionalResources();
            for (int i = 0; (resources != null) && (i < resources.length); i++)
            {
                resourceSet.add(resources[i]);
            }
        }
        Resource[] deployResources = resourceSet.toArray(new Resource[resourceSet.size()]);

        // List to hold all resources to be started.
        List<Bundle> startList = new ArrayList<Bundle>();

        // Deploy each resource, which will involve either finding a locally
        // installed resource to update or the installation of a new version
        // of the resource to be deployed.
        for (Resource deployResource : deployResources) {
            // For the resource being deployed, see if there is an older
            // version of the resource already installed that can potentially
            // be updated.
            LocalResource localResource = findUpdatableLocalResource(deployResource);
            // If a potentially updatable older version was found,
            // then verify that updating the local resource will not
            // break any of the requirements of any of the other
            // resources being deployed.
            if ((localResource != null) &&
                    isResourceUpdatable(localResource, deployResource, deployResources)) {
                // Only update if it is a different version.
                if (!localResource.equals(deployResource)) {
                    // Update the installed bundle.
                    try {
                        // stop the bundle before updating to prevent
                        // the bundle update from throwing due to not yet
                        // resolved dependencies
                        boolean doStartBundle = (flags & START) != 0;
                        if (localResource.getBundle().getState() == Bundle.ACTIVE) {
                            doStartBundle = true;
                            localResource.getBundle().stop();
                        }

                        localResource.getBundle().update(FileUtil.openURL(new URL(deployResource.getURI())));

                        // If necessary, save the updated bundle to be
                        // started later.
                        if (doStartBundle) {
                            Bundle bundle = localResource.getBundle();
                            if (!isFragmentBundle(bundle)) {
                                startList.add(bundle);
                            }
                        }
                    } catch (Exception ex) {
                        m_logger.log(
                                Logger.LOG_ERROR,
                                "Resolver: Update error - " + getBundleName(localResource.getBundle()),
                                ex);
                        return;
                    }
                }
            } else {
                // Install the bundle.
                try {
                    // Perform the install, but do not use the actual
                    // bundle JAR URL for the bundle location, since this will
                    // limit OBR's ability to manipulate bundle versions. Instead,
                    // use a unique timestamp as the bundle location.
                    URL url = new URL(deployResource.getURI());
                    Bundle bundle = m_context.installBundle(
                            "obr://"
                                    + deployResource.getSymbolicName()
                                    + "/-" + System.currentTimeMillis(),
                            FileUtil.openURL(url));

                    // If necessary, save the installed bundle to be
                    // started later.
                    if ((flags & START) != 0) {
                        if (!isFragmentBundle(bundle)) {
                            startList.add(bundle);
                        }
                    }
                } catch (Exception ex) {
                    m_logger.log(
                            Logger.LOG_ERROR,
                            "Resolver: Install error - " + deployResource.getSymbolicName(),
                            ex);
                    return;
                }
            }
        }

        for (Bundle aStartList : startList) {
            try {
                aStartList.start();
            } catch (BundleException ex) {
                m_logger.log(
                        Logger.LOG_ERROR,
                        "Resolver: Start error - " + aStartList.getSymbolicName(),
                        ex);
            }
        }
    }

    /**
     * Determines if the given bundle is a fragement bundle.
     *
     * @param bundle bundle to check
     * @return flag indicating if the given bundle is a fragement
     */
    private boolean isFragmentBundle(Bundle bundle)
    {
        return bundle.getHeaders().get(Constants.FRAGMENT_HOST) != null;
    }

    // TODO: OBR - Think about this again and make sure that deployment ordering
    // won't impact it...we need to update the local state too.
    private LocalResource findUpdatableLocalResource(Resource resource)
    {
        // Determine if any other versions of the specified resource
        // already installed.
        LocalResource[] localResources = findLocalResources(resource.getSymbolicName());
        // Since there are local resources with the same symbolic
        // name installed, then we must determine if we can
        // update an existing resource or if we must install
        // another one. Loop through all local resources with same
        // symbolic name and find the first one that can be updated
        // without breaking constraints of existing local resources.
        for (LocalResource localResource : localResources) {
            if (isResourceUpdatable(localResource, resource, localResources)) {
                return localResource;
            }
        }
        return null;
    }

    /**
     * Returns all local resources with the given symbolic name.
     * @param symName The symbolic name of the wanted local resources.
     * @return The local resources with the specified symbolic name.
     */
    private LocalResource[] findLocalResources(String symName)
    {
        LocalResource[] localResources = getLocalResources();

        List<LocalResource> matchList = new ArrayList<LocalResource>();
        for (LocalResource localResource : localResources) {
            String localSymName = localResource.getSymbolicName();
            if ((localSymName != null) && localSymName.equals(symName)) {
                matchList.add(localResource);
            }
        }
        return matchList.toArray(new LocalResource[matchList.size()]);
    }

    private boolean isResourceUpdatable(
        Resource oldVersion, Resource newVersion, Resource[] resources)
    {
        // Get all of the local resolvable requirements for the old
        // version of the resource from the specified resource array.
        Requirement[] reqs = getResolvableRequirements(oldVersion, resources);
        if (reqs == null)
        {
            return true;
        }

        // Now make sure that all of the requirements resolved by the
        // old version of the resource can also be resolved by the new
        // version of the resource.
        Capability[] caps = newVersion.getCapabilities();
        if (caps == null)
        {
            return false;
        }
        for (Requirement req : reqs) {
            boolean satisfied = false;
            for (int capIdx = 0; !satisfied && (capIdx < caps.length); capIdx++) {
                if (req.isSatisfied(caps[capIdx])) {
                    satisfied = true;
                }
            }

            // If any of the previously resolved requirements cannot
            // be resolved, then the resource is not updatable.
            if (!satisfied) {
                return false;
            }
        }

        return true;
    }

    private Requirement[] getResolvableRequirements(Resource resource, Resource[] resources)
    {
        // For the specified resource, find all requirements that are
        // satisfied by any of its capabilities in the specified resource
        // array.
        Capability[] caps = resource.getCapabilities();
        if ((caps != null) && (caps.length > 0))
        {
            List<Requirement> reqList = new ArrayList<Requirement>();
            for (Capability cap : caps) {
                boolean added = false;

                for (Resource aResource : resources) {
                    Requirement[] reqs = aResource.getRequirements();

                    if (reqs != null) {
                        for (Requirement req : reqs) {
                            if (req.isSatisfied(cap)) {
                                added = true;
                                reqList.add(req);
                            }
                        }
                    }

                    if (added) break;
                }
            }
            return reqList.toArray(new Requirement[reqList.size()]);
        }
        return null;
    }

    public static String getBundleName(Bundle bundle)
    {
        String name = bundle.getHeaders().get(Constants.BUNDLE_NAME);
        return (name == null)
            ? "Bundle " + Long.toString(bundle.getBundleId())
            : name;
    }

}