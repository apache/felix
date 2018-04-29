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
<<<<<<< HEAD
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.impl.ResourceImpl;
import org.apache.felix.utils.log.Logger;
import org.osgi.framework.*;
=======
import org.apache.felix.utils.log.Logger;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

public class ResolverImpl implements Resolver
{
    private final BundleContext m_context;
    private final Logger m_logger;
    private final Repository[] m_repositories;
<<<<<<< HEAD
    private final Set m_addedSet = new HashSet();
    private final Set m_addedRequirementSet = new HashSet();
    private final Set m_globalCapabilities = new HashSet();
    private final Set m_failedSet = new HashSet();
    private final Set m_resolveSet = new HashSet();
    private final Set m_requiredSet = new HashSet();
    private final Set m_optionalSet = new HashSet();
    private final Map m_reasonMap = new HashMap();
    private final Set m_unsatisfiedSet = new HashSet();
    private boolean m_resolved = false;
    private long m_resolveTimeStamp;
    private int m_resolutionFlags;
    private int m_deployFlags;
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

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
<<<<<<< HEAD
        return (Resource[]) m_addedSet.toArray(new Resource[m_addedSet.size()]);
=======
        return m_addedSet.toArray(new Resource[m_addedSet.size()]);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    public synchronized void add(Requirement requirement)
    {
        m_resolved = false;
        m_addedRequirementSet.add(requirement);
    }

    public synchronized Requirement[] getAddedRequirements()
    {
<<<<<<< HEAD
        return (Requirement[]) m_addedRequirementSet.toArray(new Requirement[m_addedRequirementSet.size()]);
=======
        return m_addedRequirementSet.toArray(new Requirement[m_addedRequirementSet.size()]);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    public void addGlobalCapability(Capability capability)
    {
        m_globalCapabilities.add(capability);
    }

    public Capability[] getGlobalCapabilities()
    {
<<<<<<< HEAD
        return (Capability[]) m_globalCapabilities.toArray(new Capability[m_globalCapabilities.size()]);
=======
        return m_globalCapabilities.toArray(new Capability[m_globalCapabilities.size()]);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    public synchronized Resource[] getRequiredResources()
    {
        if (m_resolved)
        {
<<<<<<< HEAD
            return (Resource[]) m_requiredSet.toArray(new Resource[m_requiredSet.size()]);
=======
            return m_requiredSet.toArray(new Resource[m_requiredSet.size()]);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        throw new IllegalStateException("The resources have not been resolved.");
    }

    public synchronized Resource[] getOptionalResources()
    {
        if (m_resolved)
        {
<<<<<<< HEAD
            return (Resource[]) m_optionalSet.toArray(new Resource[m_optionalSet.size()]);
=======
            return m_optionalSet.toArray(new Resource[m_optionalSet.size()]);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        throw new IllegalStateException("The resources have not been resolved.");
    }

    public synchronized Reason[] getReason(Resource resource)
    {
        if (m_resolved)
        {
<<<<<<< HEAD
            List l = (List) m_reasonMap.get(resource);
            return l != null ? (Reason[]) l.toArray(new Reason[l.size()]) : null;
=======
            List<Reason> l = m_reasonMap.get(resource);
            return l != null ? l.toArray(new Reason[l.size()]) : null;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        throw new IllegalStateException("The resources have not been resolved.");
    }

    public synchronized Reason[] getUnsatisfiedRequirements()
    {
        if (m_resolved)
        {
<<<<<<< HEAD
            return (Reason[]) m_unsatisfiedSet.toArray(new Reason[m_unsatisfiedSet.size()]);
=======
            return m_unsatisfiedSet.toArray(new Reason[m_unsatisfiedSet.size()]);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        throw new IllegalStateException("The resources have not been resolved.");
    }

<<<<<<< HEAD
    private Resource[] getResources(boolean local)
    {
        List resources = new ArrayList();
        for (int repoIdx = 0; (m_repositories != null) && (repoIdx < m_repositories.length); repoIdx++)
        {
            boolean isLocal = m_repositories[repoIdx] instanceof LocalRepositoryImpl;
            boolean isSystem = m_repositories[repoIdx] instanceof SystemRepositoryImpl;
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            if (isLocal && (m_resolutionFlags & NO_LOCAL_RESOURCES) != 0) {
                continue;
            }
            if (isSystem && (m_resolutionFlags & NO_SYSTEM_BUNDLE) != 0) {
                continue;
            }
<<<<<<< HEAD
            Resource[] res = m_repositories[repoIdx].getResources();
            for (int resIdx = 0; (res != null) && (resIdx < res.length); resIdx++)
            {
                if (res[resIdx].isLocal() == local)
                {
                    resources.add(res[resIdx]);
                }
            }
        }
        return (Resource[]) resources.toArray(new Resource[resources.size()]);
=======
            Collections.addAll(resources, m_repositories[repoIdx].getResources());
        }
        return resources.toArray(new Resource[resources.size()]);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    public synchronized boolean resolve()
    {
        return resolve(0);
    }

    public synchronized boolean resolve(int flags)
    {
        // Find resources
<<<<<<< HEAD
        Resource[] locals = getResources(true);
        Resource[] remotes = getResources(false);
=======
        Resource[] locals = getLocalResources();
        Resource[] remotes = getRemoteResources();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

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
<<<<<<< HEAD
            for (Iterator iter = m_globalCapabilities.iterator(); iter.hasNext();)
            {
                Capability cap = (Capability) iter.next();
                fake.addCapability(cap);
            }
            for (Iterator iter = m_addedRequirementSet.iterator(); iter.hasNext();)
            {
                Requirement req = (Requirement) iter.next();
=======
            for (Capability cap : m_globalCapabilities) {
                fake.addCapability(cap);
            }
            for (Requirement req : m_addedRequirementSet) {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                fake.addRequire(req);
            }
            if (!resolve(fake, locals, remotes, false))
            {
                result = false;
            }
        }

        // Loop through each resource in added list and resolve.
<<<<<<< HEAD
        for (Iterator iter = m_addedSet.iterator(); iter.hasNext(); )
        {
            if (!resolve((Resource) iter.next(), locals, remotes, false))
            {
=======
        for (Resource aM_addedSet : m_addedSet) {
            if (!resolve(aM_addedSet, locals, remotes, false)) {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
            for (int reqIdx = 0; reqIdx < reqs.length; reqIdx++)
            {
                // Do not resolve optional requirements
                if ((m_resolutionFlags & NO_OPTIONAL_RESOURCES) != 0 && reqs[reqIdx].isOptional())
                {
                    continue;
                }
                candidate = searchResources(reqs[reqIdx], m_addedSet);
                if (candidate == null)
                {
                    candidate = searchResources(reqs[reqIdx], m_requiredSet);
                }
                if (candidate == null)
                {
                    candidate = searchResources(reqs[reqIdx], m_optionalSet);
                }
                if (candidate == null)
                {
                    candidate = searchResources(reqs[reqIdx], m_resolveSet);
                }
                if (candidate == null)
                {
                    List candidateCapabilities = searchResources(reqs[reqIdx], locals);
                    candidateCapabilities.addAll(searchResources(reqs[reqIdx], remotes));

                    // Determine the best candidate available that
                    // can resolve.
                    while ((candidate == null) && !candidateCapabilities.isEmpty())
                    {
                        ResourceCapability bestCapability = getBestCandidate(candidateCapabilities);

                        // Try to resolve the best resource.
                        if (resolve(bestCapability.getResource(), locals, remotes, optional || reqs[reqIdx].isOptional()))
                        {
                            candidate = bestCapability.getResource();
                        }
                        else
                        {
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                            candidateCapabilities.remove(bestCapability);
                        }
                    }
                }

<<<<<<< HEAD
                if ((candidate == null) && !reqs[reqIdx].isOptional())
                {
=======
                if ((candidate == null) && !req.isOptional()) {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                    // The resolve failed.
                    result = false;
                    // Associated the current resource to the requirement
                    // in the unsatisfied requirement set.
<<<<<<< HEAD
                    m_unsatisfiedSet.add(new ReasonImpl(resource, reqs[reqIdx]));
                }
                else if (candidate != null)
                {

                    // Try to resolve the candidate.
                    if (resolve(candidate, locals, remotes, optional || reqs[reqIdx].isOptional()))
                    {
                        // The resolved succeeded; record the candidate
                        // as either optional or required.
                        if (optional || reqs[reqIdx].isOptional())
                        {
                            m_optionalSet.add(candidate);
                            m_resolveSet.remove(candidate);
                        }
                        else
                        {
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                            m_requiredSet.add(candidate);
                            m_optionalSet.remove(candidate);
                            m_resolveSet.remove(candidate);
                        }

                        // Add the reason why the candidate was selected.
<<<<<<< HEAD
                        List reasons = (List) m_reasonMap.get(candidate);
                        if (reasons == null)
                        {
                            reasons = new ArrayList();
                            m_reasonMap.put(candidate, reasons);
                        }
                        reasons.add(new ReasonImpl(resource, reqs[reqIdx]));
                    }
                    else
                    {
=======
                        List<Reason> reasons = m_reasonMap.get(candidate);
                        if (reasons == null) {
                            reasons = new ArrayList<Reason>();
                            m_reasonMap.put(candidate, reasons);
                        }
                        reasons.add(new ReasonImpl(resource, req));
                    } else {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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

<<<<<<< HEAD
    private Resource searchResources(Requirement req, Set resourceSet)
    {
        for (Iterator iter = resourceSet.iterator(); iter.hasNext(); )
        {
            checkInterrupt();
            Resource resource = (Resource) iter.next();
            Capability[] caps = resource.getCapabilities();
            for (int capIdx = 0; (caps != null) && (capIdx < caps.length); capIdx++)
            {
                if (req.isSatisfied(caps[capIdx]))
                {
                    // The requirement is already satisfied an existing
                    // resource, return the resource.
                    return resource;
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
    private List searchResources(Requirement req, Resource[] resources)
    {
        List matchingCapabilities = new ArrayList();

        for (int resIdx = 0; (resources != null) && (resIdx < resources.length); resIdx++)
        {
            checkInterrupt();
            // We don't need to look at resources we've already looked at.
            if (!m_failedSet.contains(resources[resIdx]))
            {
                Capability[] caps = resources[resIdx].getCapabilities();
                for (int capIdx = 0; (caps != null) && (capIdx < caps.length); capIdx++)
                {
                    if (req.isSatisfied(caps[capIdx]))
                    {
                        matchingCapabilities.add(new ResourceCapabilityImpl(resources[resIdx], caps[capIdx]));
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
    private ResourceCapability getBestCandidate(List caps)
=======
    private ResourceCapability getBestCandidate(List<ResourceCapability> caps)
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        Version bestVersion = null;
        ResourceCapability best = null;
        boolean bestLocal = false;

<<<<<<< HEAD
        for(int capIdx = 0; capIdx < caps.size(); capIdx++)
        {
            ResourceCapability current = (ResourceCapability) caps.get(capIdx);
            boolean isCurrentLocal = current.getResource().isLocal();

            if (best == null)
            {
                best = current;
                bestLocal = isCurrentLocal;
                Object v = current.getCapability().getPropertiesAsMap().get(Resource.VERSION);
                if ((v != null) && (v instanceof Version))
                {
                    bestVersion = (Version) v;
                }
            }
            else if ((m_resolutionFlags & DO_NOT_PREFER_LOCAL) != 0 || !bestLocal || isCurrentLocal)
            {
                Object v = current.getCapability().getPropertiesAsMap().get(Resource.VERSION);
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

                // If there is no version, then select the resource
                // with the greatest number of capabilities.
                if ((v == null) && (bestVersion == null)
<<<<<<< HEAD
                    && (best.getResource().getCapabilities().length
                        < current.getResource().getCapabilities().length))
                {
                    best = current;
                    bestLocal = isCurrentLocal;
                    bestVersion = null;
                }
                else if ((v != null) && (v instanceof Version))
                {
                    // If there is no best version or if the current
                    // resource's version is lower, then select it.
                    if ((bestVersion == null) || (bestVersion.compareTo(v) < 0))
                    {
                        best = current;
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                        bestLocal = isCurrentLocal;
                        bestVersion = (Version) v;
                    }
                    // If the current resource version is equal to the
                    // best
<<<<<<< HEAD
                    else if ((bestVersion != null) && (bestVersion.compareTo(v) == 0))
                    {
                        // If the symbolic name is the same, use the highest
                        // bundle version.
                        if ((best.getResource().getSymbolicName() != null)
                            && best.getResource().getSymbolicName().equals(
                                current.getResource().getSymbolicName()))
                        {
                            if (best.getResource().getVersion().compareTo(
                                current.getResource().getVersion()) < 0)
                            {
                                best = current;
=======
                    else if ((bestVersion.compareTo((Version) v) == 0)) {
                        // If the symbolic name is the same, use the highest
                        // bundle version.
                        if ((best.getResource().getSymbolicName() != null)
                                && best.getResource().getSymbolicName().equals(
                                cap.getResource().getSymbolicName())) {
                            if (best.getResource().getVersion().compareTo(
                                    cap.getResource().getVersion()) < 0) {
                                best = cap;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                                bestLocal = isCurrentLocal;
                                bestVersion = (Version) v;
                            }
                        }
                        // Otherwise select the one with the greatest
                        // number of capabilities.
                        else if (best.getResource().getCapabilities().length
<<<<<<< HEAD
                            < current.getResource().getCapabilities().length)
                        {
                            best = current;
=======
                                < cap.getResource().getCapabilities().length) {
                            best = cap;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
        m_deployFlags = flags;
=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
        Map deployMap = new HashMap();
        Resource[] resources = getAddedResources();
        for (int i = 0; (resources != null) && (i < resources.length); i++)
        {
            deployMap.put(resources[i], resources[i]);
=======
        Set<Resource> resourceSet = new HashSet<Resource>();
        Resource[] resources = getAddedResources();
        for (int i = 0; (resources != null) && (i < resources.length); i++)
        {
            resourceSet.add(resources[i]);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        resources = getRequiredResources();
        for (int i = 0; (resources != null) && (i < resources.length); i++)
        {
<<<<<<< HEAD
            deployMap.put(resources[i], resources[i]);
=======
            resourceSet.add(resources[i]);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        if ((flags & NO_OPTIONAL_RESOURCES) == 0)
        {
            resources = getOptionalResources();
            for (int i = 0; (resources != null) && (i < resources.length); i++)
            {
<<<<<<< HEAD
                deployMap.put(resources[i], resources[i]);
            }
        }
        Resource[] deployResources = (Resource[])
            deployMap.keySet().toArray(new Resource[deployMap.size()]);

        // List to hold all resources to be started.
        List startList = new ArrayList();
=======
                resourceSet.add(resources[i]);
            }
        }
        Resource[] deployResources = resourceSet.toArray(new Resource[resourceSet.size()]);

        // List to hold all resources to be started.
        List<Bundle> startList = new ArrayList<Bundle>();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        // Deploy each resource, which will involve either finding a locally
        // installed resource to update or the installation of a new version
        // of the resource to be deployed.
<<<<<<< HEAD
        for (int i = 0; i < deployResources.length; i++)
        {
            // For the resource being deployed, see if there is an older
            // version of the resource already installed that can potentially
            // be updated.
            LocalResourceImpl localResource =
                findUpdatableLocalResource(deployResources[i]);
=======
        for (Resource deployResource : deployResources) {
            // For the resource being deployed, see if there is an older
            // version of the resource already installed that can potentially
            // be updated.
            LocalResource localResource = findUpdatableLocalResource(deployResource);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            // If a potentially updatable older version was found,
            // then verify that updating the local resource will not
            // break any of the requirements of any of the other
            // resources being deployed.
            if ((localResource != null) &&
<<<<<<< HEAD
                isResourceUpdatable(localResource, deployResources[i], deployResources))
            {
                // Only update if it is a different version.
                if (!localResource.equals(deployResources[i]))
                {
                    // Update the installed bundle.
                    try
                    {
=======
                    isResourceUpdatable(localResource, deployResource, deployResources)) {
                // Only update if it is a different version.
                if (!localResource.equals(deployResource)) {
                    // Update the installed bundle.
                    try {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                        // stop the bundle before updating to prevent
                        // the bundle update from throwing due to not yet
                        // resolved dependencies
                        boolean doStartBundle = (flags & START) != 0;
<<<<<<< HEAD
                        if (localResource.getBundle().getState() == Bundle.ACTIVE)
                        {
=======
                        if (localResource.getBundle().getState() == Bundle.ACTIVE) {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                            doStartBundle = true;
                            localResource.getBundle().stop();
                        }

<<<<<<< HEAD
                        localResource.getBundle().update(FileUtil.openURL(new URL(deployResources[i].getURI())));

                        // If necessary, save the updated bundle to be
                        // started later.
                        if (doStartBundle)
                        {
                            Bundle bundle = localResource.getBundle();
                            if (!isFragmentBundle(bundle))
                            {
                                startList.add(bundle);
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        m_logger.log(
                            Logger.LOG_ERROR,
                            "Resolver: Update error - " + getBundleName(localResource.getBundle()),
                            ex);
                        return;
                    }
                }
            }
            else
            {
                // Install the bundle.
                try
                {
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                    // Perform the install, but do not use the actual
                    // bundle JAR URL for the bundle location, since this will
                    // limit OBR's ability to manipulate bundle versions. Instead,
                    // use a unique timestamp as the bundle location.
<<<<<<< HEAD
                    URL url = new URL(deployResources[i].getURI());
                    if (url != null)
                    {
                        Bundle bundle = m_context.installBundle(
                            "obr://"
                            + deployResources[i].getSymbolicName()
                            + "/-" + System.currentTimeMillis(),
                            FileUtil.openURL(url));

                        // If necessary, save the installed bundle to be
                        // started later.
                        if ((flags & START) != 0)
                        {
                            if (!isFragmentBundle(bundle))
                            {
                                startList.add(bundle);
                            }
                        }
                    }
                }
                catch (Exception ex)
                {
                    m_logger.log(
                        Logger.LOG_ERROR,
                        "Resolver: Install error - " + deployResources[i].getSymbolicName(),
                        ex);
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                    return;
                }
            }
        }

<<<<<<< HEAD
        for (int i = 0; i < startList.size(); i++)
        {
            try
            {
                ((Bundle) startList.get(i)).start();
            }
            catch (BundleException ex)
            {
                m_logger.log(
                    Logger.LOG_ERROR,
                    "Resolver: Start error - " + ((Bundle) startList.get(i)).getSymbolicName(),
                    ex);
=======
        for (Bundle aStartList : startList) {
            try {
                aStartList.start();
            } catch (BundleException ex) {
                m_logger.log(
                        Logger.LOG_ERROR,
                        "Resolver: Start error - " + aStartList.getSymbolicName(),
                        ex);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
    private LocalResourceImpl findUpdatableLocalResource(Resource resource)
    {
        // Determine if any other versions of the specified resource
        // already installed.
        Resource[] localResources = findLocalResources(resource.getSymbolicName());
        if (localResources != null)
        {
            // Since there are local resources with the same symbolic
            // name installed, then we must determine if we can
            // update an existing resource or if we must install
            // another one. Loop through all local resources with same
            // symbolic name and find the first one that can be updated
            // without breaking constraints of existing local resources.
            for (int i = 0; i < localResources.length; i++)
            {
                if (isResourceUpdatable(localResources[i], resource, localResources))
                {
                    return (LocalResourceImpl) localResources[i];
                }
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            }
        }
        return null;
    }

    /**
     * Returns all local resources with the given symbolic name.
     * @param symName The symbolic name of the wanted local resources.
     * @return The local resources with the specified symbolic name.
     */
<<<<<<< HEAD
    private Resource[] findLocalResources(String symName)
    {
        Resource[] localResources = getResources(true);

        List matchList = new ArrayList();
        for (int i = 0; i < localResources.length; i++)
        {
            String localSymName = localResources[i].getSymbolicName();
            if ((localSymName != null) && localSymName.equals(symName))
            {
                matchList.add(localResources[i]);
            }
        }
        return (Resource[]) matchList.toArray(new Resource[matchList.size()]);
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
        for (int reqIdx = 0; reqIdx < reqs.length; reqIdx++)
        {
            boolean satisfied = false;
            for (int capIdx = 0; !satisfied && (capIdx < caps.length); capIdx++)
            {
                if (reqs[reqIdx].isSatisfied(caps[capIdx]))
                {
=======
        for (Requirement req : reqs) {
            boolean satisfied = false;
            for (int capIdx = 0; !satisfied && (capIdx < caps.length); capIdx++) {
                if (req.isSatisfied(caps[capIdx])) {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                    satisfied = true;
                }
            }

            // If any of the previously resolved requirements cannot
            // be resolved, then the resource is not updatable.
<<<<<<< HEAD
            if (!satisfied)
            {
=======
            if (!satisfied) {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
            List reqList = new ArrayList();
            for (int capIdx = 0; capIdx < caps.length; capIdx++)
            {
                boolean added = false;
                for (int resIdx = 0; !added && (resIdx < resources.length); resIdx++)
                {
                    Requirement[] reqs = resources[resIdx].getRequirements();
                    for (int reqIdx = 0;
                        (reqs != null) && (reqIdx < reqs.length);
                        reqIdx++)
                    {
                        if (reqs[reqIdx].isSatisfied(caps[capIdx]))
                        {
                            added = true;
                            reqList.add(reqs[reqIdx]);
                        }
                    }
                }
            }
            return (Requirement[]) reqList.toArray(new Requirement[reqList.size()]);
=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        }
        return null;
    }

    public static String getBundleName(Bundle bundle)
    {
<<<<<<< HEAD
        String name = (String) bundle.getHeaders().get(Constants.BUNDLE_NAME);
=======
        String name = bundle.getHeaders().get(Constants.BUNDLE_NAME);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        return (name == null)
            ? "Bundle " + Long.toString(bundle.getBundleId())
            : name;
    }

}