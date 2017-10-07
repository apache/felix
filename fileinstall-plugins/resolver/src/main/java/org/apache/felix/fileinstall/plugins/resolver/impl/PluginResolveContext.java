/*******************************************************************************
 * Copyright (c) Intel Corporation
 * Copyright (c) 2017
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.apache.felix.fileinstall.plugins.resolver.impl;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.felix.fileinstall.plugins.resolver.ResolveRequest;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.resource.Wiring;
import org.osgi.service.log.LogService;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.HostedCapability;
import org.osgi.service.resolver.ResolveContext;

import aQute.bnd.deployer.repository.FixedIndexedRepo;
import aQute.bnd.service.url.URLConnector;

class PluginResolveContext extends ResolveContext {

    private static final String INITIAL_RESOURCE_CAPABILITY_NAMESPACE = "__initial";
    private static final String MIME_BUNDLE = "application/vnd.osgi.bundle";

    private final BundleContext bundleContext;

    // The repositories that will be queries for providers
    private final Map<URI, Repository> repositories = new HashMap<>();
    // A cache of resource->location (URL), generated during resolve and queried
    // after resolve in order to fetch the resource.
    private final Map<Resource, String> resourceLocationMap = new IdentityHashMap<>();
    // A cache of resources to the repositories which own them; used from
    // insertHostedCapability method.
    private final Map<Resource, Repository> resourceRepositoryMap = new IdentityHashMap<>();

    private final ResourceImpl initialResource;
    private final LogService log;

    PluginResolveContext(BundleContext bundleContext, ResolveRequest request, LogService log) throws IOException {
        this.bundleContext = bundleContext;
        this.log = log;

        this.initialResource = new ResourceImpl();
        for (Requirement requirement : request.getRequirements()) {
            this.initialResource.addRequirement(requirement);
        }
        this.initialResource.addCapability(createIdentityCap(this.initialResource, request.getName()));
        this.initialResource.addCapability(createInitialMarkerCapability(this.initialResource));

        BasicRegistry registry = new BasicRegistry().put(URLConnector.class, new JarURLConnector());

        for (URI indexUri : request.getIndexes()) {
            // URI cachedIndexUri = getCacheIndexURI(indexUri);
            Map<String, String> repoProps = new HashMap<>();
            repoProps.put("locations", indexUri.toString());
            FixedIndexedRepo repo = new FixedIndexedRepo();
            repo.setRegistry(registry);
            repo.setProperties(repoProps);

            this.repositories.put(indexUri, repo);
        }
    }

    @Override
    public Collection<Resource> getMandatoryResources() {
        return Collections.<Resource>singleton(this.initialResource);
    }

    @Override
    public List<Capability> findProviders(Requirement requirement) {
        List<Capability> resultCaps = new LinkedList<>();

        // Find from installed bundles
        Bundle[] bundles = this.bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            if (bundle.getState() == Bundle.UNINSTALLED) {
                continue; // Skip UNINSTALLED bundles
            }

            BundleRevision revision = bundle.adapt(BundleRevision.class);
            List<Capability> bundleCaps = revision.getCapabilities(requirement.getNamespace());
            if (bundleCaps != null) {
                for (Capability bundleCap : bundleCaps) {
                    if (match(requirement, bundleCap, this.log)) {
                        resultCaps.add(bundleCap);
                    }
                }
            }
        }

        // Find from repositories
        for (Entry<URI, Repository> repoEntry : this.repositories.entrySet()) {
            Repository repository = repoEntry.getValue();
            Map<Requirement, Collection<Capability>> providers = repository
                    .findProviders(Collections.singleton(requirement));
            if (providers != null) {
                Collection<Capability> repoCaps = providers.get(requirement);
                if (repoCaps != null) {
                    resultCaps.addAll(repoCaps);

                    for (Capability repoCap : repoCaps) {
                        // Get the list of physical URIs for this resource.
                        Resource resource = repoCap.getResource();
                        // Keep track of which repositories own which resources.
                        this.resourceRepositoryMap.put(resource, repository);

                        // Resolve the Resource's URI relative to the Repository
                        // Index URI and save for later.
                        URI repoIndexUri = repoEntry.getKey();
                        URI resolvedUri = resolveResourceLocation(resource, repoIndexUri);
                        if (resolvedUri != null) {
                            // Cache the resolved URI into the resource URI map,
                            // which will be used after resolve.
                            this.resourceLocationMap.put(resource, resolvedUri.toString());
                        }
                    }
                }
            }
        }
        return resultCaps;
    }

    static boolean match(Requirement requirement, Capability capability) {
        return match(requirement, capability, null);
    }

    static boolean match(Requirement requirement, Capability capability, LogService log) {
        // Namespace MUST match
        if (!requirement.getNamespace().equals(capability.getNamespace())) {
            return false;
        }

        // If capability effective!=resolve then it matches only requirements
        // with same effective
        String capabilityEffective = capability.getDirectives().get(Namespace.CAPABILITY_EFFECTIVE_DIRECTIVE);
        if (capabilityEffective != null) {
            String requirementEffective = requirement.getDirectives().get(Namespace.REQUIREMENT_EFFECTIVE_DIRECTIVE);
            if (!capabilityEffective.equals(Namespace.EFFECTIVE_RESOLVE)
                    && !capabilityEffective.equals(requirementEffective)) {
                return false;
            }
        }

        String filterStr = requirement.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
        if (filterStr == null) {
            return true; // no filter, the requirement always matches
        }

        try {
            Filter filter = FrameworkUtil.createFilter(filterStr);
            return filter.matches(capability.getAttributes());
        } catch (InvalidSyntaxException e) {
            if (log != null) {
                Resource resource = requirement.getResource();
                String id = resource != null ? getIdentity(resource) : "<unknown>";
                log.log(LogService.LOG_ERROR,
                        String.format("Invalid filter syntax in requirement from resource %s: %s", id, filterStr), e);
            }
            return false;
        }
    }

    /**
     * Get the repository index for the specified resource, where 0 indicates an
     * existing OSGi bundle in the framework and -1 indicates not found. This
     * method is used by
     * {@link #insertHostedCapability(List, HostedCapability)}.
     */
    private int findResourceRepositoryIndex(Resource resource) {
        if (resource instanceof BundleRevision) {
            return 0;
        }

        int index = 1;
        Repository repo = this.resourceRepositoryMap.get(resource);
        if (repo == null) {
            return -1;
        }
        for (Repository match : this.repositories.values()) {
            if (repo == match) {
                return index;
            }
            index++;
        }
        // Still not found
        return -1;
    }

    @Override
    public int insertHostedCapability(List<Capability> capabilities, HostedCapability hc) {
        int hostIndex = findResourceRepositoryIndex(hc.getResource());
        if (hostIndex == -1) {
            throw new IllegalArgumentException(
                    "Hosted capability has host resource not found in any known repository.");
        }

        for (int pos = 0; pos < capabilities.size(); pos++) {
            int resourceIndex = findResourceRepositoryIndex(capabilities.get(pos).getResource());
            if (resourceIndex > hostIndex) {
                capabilities.add(pos, hc);
                return pos;
            }
        }

        // The list passed by (some versions of) Felix does not support the
        // single-arg add() method... this throws UnsupportedOperationException.
        // So we have to call the two-arg add() with an explicit index.
        int lastPos = capabilities.size();
        capabilities.add(lastPos, hc);
        return lastPos;
    }

    @Override
    public boolean isEffective(Requirement requirement) {
        return true;
    }

    @Override
    public Map<Resource, Wiring> getWirings() {
        Map<Resource, Wiring> wiringMap = new HashMap<>();
        Bundle[] bundles = this.bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            // BundleRevision extends Resource
            BundleRevision revision = bundle.adapt(BundleRevision.class);
            // BundleWiring extends Wiring
            BundleWiring wiring = revision.getWiring();
            if (wiring != null) {
                wiringMap.put(revision, wiring);
            }
        }
        return wiringMap;
    }

    boolean isInitialResource(Resource resource) {
        List<Capability> markerCaps = resource.getCapabilities(INITIAL_RESOURCE_CAPABILITY_NAMESPACE);
        return markerCaps != null && !markerCaps.isEmpty();
    }

    String getLocation(Resource resource) {
        String location;
        if (resource instanceof BundleRevision) {
            location = ((BundleRevision) resource).getBundle().getLocation();
        } else {
            location = this.resourceLocationMap.get(resource);
        }
        return location;
    }

    private static CapabilityImpl createInitialMarkerCapability(Resource resource) {
        return new CapabilityImpl(INITIAL_RESOURCE_CAPABILITY_NAMESPACE, Collections.<String,String>emptyMap(), Collections.<String,Object>emptyMap(),
                resource);
    }

    private static CapabilityImpl createIdentityCap(Resource resource, String identity) {
        Map<String, Object> idCapAttrs = new HashMap<>();
        idCapAttrs.put(IdentityNamespace.IDENTITY_NAMESPACE, identity);
        CapabilityImpl idCap = new CapabilityImpl(IdentityNamespace.IDENTITY_NAMESPACE, Collections.<String,String>emptyMap(),
                idCapAttrs, resource);
        return idCap;
    }

    private static URI resolveResourceLocation(Resource resource, URI indexUri) {
        List<Capability> contentCaps = resource.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
        if (contentCaps != null) {
            for (Capability contentCap : contentCaps) {
                // Ensure this content entry has the correct MIME type for a
                // bundle
                if (MIME_BUNDLE.equals(contentCap.getAttributes().get(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE))) {
                    // Get the URI attribute in the index, which is either an
                    // asbolute URI, or a relative URI to be resolved against
                    // the index URI.
                    URI rawUri = null;
                    Object uriObj = contentCap.getAttributes().get(ContentNamespace.CAPABILITY_URL_ATTRIBUTE);
                    if (uriObj instanceof URI) {
                        rawUri = (URI) uriObj;
                    } else if (uriObj instanceof String) {
                        rawUri = URI.create((String) uriObj);
                    }
                    if (rawUri != null) {
                        URI resolvedUri = URIUtils.resolve(indexUri, rawUri);
                        return resolvedUri;
                    }
                }
            }
        }

        // No content capability was found in the appropriate form
        return null;
    }

    private static String getIdentity(Resource resource) {
        List<Capability> caps = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
        if (caps == null || caps.isEmpty()) {
            return "<unknown>";
        }

        Object idObj = caps.get(0).getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE);
        if (!(idObj instanceof String)) {
            return "<unknown>";
        }

        return (String) idObj;
    }

}
