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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.utils.log.Logger;
import org.apache.felix.utils.resource.CapabilityImpl;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.AndExpression;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.ExpressionCombiner;
import org.osgi.service.repository.IdentityExpression;
import org.osgi.service.repository.NotExpression;
import org.osgi.service.repository.OrExpression;
import org.osgi.service.repository.Repository;
import org.osgi.service.repository.RequirementBuilder;
import org.osgi.service.repository.RequirementExpression;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;

class OSGiRepositoryImpl implements Repository {
    private final Logger logger;

    private final PromiseFactory pf = new PromiseFactory(null, null);

    private final RepositoryAdmin repository;

    OSGiRepositoryImpl(RepositoryAdmin repository, Logger pLogger) {
        this.repository = repository;
        this.logger = pLogger;
    }

    static String getSHA256(String uri)
            throws IOException, NoSuchAlgorithmException // TODO find a good
                                                         // place for this
    {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream is = new URL(uri).openStream();

                // Use a digest inputstream as using byte arrays directly to
                // compute the
                // SHA-256 can
                // have big effects on memory consumption. I.e. you don't want
                // to have
                // to read the
                // entire resource in memory. We rather stream it through...
                DigestInputStream dis = new DigestInputStream(is, md);) {

            byte[] buffer = new byte[16384];
            while (dis.read(buffer) != -1) {
                // we just drain the stream here to compute the Message Digest
            }

            StringBuilder sb = new StringBuilder(64); // SHA-256 is always 64
                                                      // hex
                                                      // characters
            for (byte b : md.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } finally {
            //
        }
    }

    static CapabilityImpl newOSGiContentCapability(
            org.osgi.resource.Resource or,
            org.apache.felix.bundlerepository.Resource resource) {
        final String uri = resource.getURI();
        LazyStringMap.LazyValue<String> content = new LazyStringMap.LazyValue<String>() {
            public String compute() {
                // This is expensive to do, so only compute it when actually
                // obtained...
                try {
                    return OSGiRepositoryImpl.getSHA256(uri);
                } catch (NoSuchAlgorithmException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        Object mime = resource.getProperties().get("mime");
        if (mime == null)
            mime = "application/vnd.osgi.bundle";

        Map<String, Object> contentAttrs = new LazyStringMap<>(4);
        contentAttrs.put(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE, mime);
        contentAttrs.put(ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE,
                resource.getSize());
        contentAttrs.put(ContentNamespace.CAPABILITY_URL_ATTRIBUTE, uri);
        contentAttrs.put(ContentNamespace.CONTENT_NAMESPACE, content);
        return new CapabilityImpl(or, ContentNamespace.CONTENT_NAMESPACE,
                Collections.<String, String> emptyMap(), contentAttrs);
    }

    static CapabilityImpl newOSGiIdentityCapability(
            org.osgi.resource.Resource or,
            org.apache.felix.bundlerepository.Resource res) {
        @SuppressWarnings("unchecked")
        Map<String, Object> idAttrs = new HashMap<>(res.getProperties());

        // Set a number of specific properties that need to be translated
        idAttrs.put(IdentityNamespace.IDENTITY_NAMESPACE,
                res.getSymbolicName());

        if (idAttrs.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE) == null)
            idAttrs.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE,
                    IdentityNamespace.TYPE_BUNDLE);

        return new CapabilityImpl(or, IdentityNamespace.IDENTITY_NAMESPACE,
                Collections.<String, String> emptyMap(), idAttrs);
    }

    private void addResourceForIdentity(
            final org.apache.felix.bundlerepository.Resource res, Filter filter,
            List<Capability> caps) {
        List<Capability> idCaps = new FelixResourceAdapter(res)
                .getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
        if (idCaps.isEmpty())
            return;

        Capability idCap = idCaps.get(0); // there should only be one
                                          // osgi.identity anyway
        if (filter != null && !filter.matches(idCap.getAttributes())) {
            return;
        }
        caps.add(idCap);
    }

    public Map<Requirement, Collection<Capability>> findProviders(
            Collection<? extends Requirement> requirements) {
        Map<Requirement, Collection<Capability>> m = new HashMap<>();
        for (Requirement r : requirements) {
            m.put(r, findProviders(r));
        }
        return m;
    }

    private Collection<Capability> findProviders(Requirement req) {
        List<Capability> caps = new ArrayList<>();
        if (IdentityNamespace.IDENTITY_NAMESPACE.equals(req.getNamespace())) {
            for (org.apache.felix.bundlerepository.Repository repo : repository
                    .listRepositories()) {
                for (org.apache.felix.bundlerepository.Resource res : repo
                        .getResources()) {
                    String f = req.getDirectives()
                            .get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
                    try {
                        addResourceForIdentity(res, f == null ? null
                                : FrameworkUtil.createFilter(f), caps);
                    } catch (Exception e) {
                        logger.log(Logger.LOG_ERROR,
                                "Failure while findind providers.", e);
                        throw new RuntimeException(e);
                    }
                }
            }
        } else {
            org.apache.felix.bundlerepository.Resource[] resources = repository
                    .discoverResources(
                            new org.apache.felix.bundlerepository.Requirement[] {
                                    new OSGiRequirementAdapter(req) });
            OSGiRequirementAdapter adapter = new OSGiRequirementAdapter(req);
            for (org.apache.felix.bundlerepository.Resource resource : resources) {
                for (org.apache.felix.bundlerepository.Capability cap : resource
                        .getCapabilities()) {
                    if (adapter.isSatisfied(cap))
                        caps.add(new FelixCapabilityAdapter(cap,
                                new FelixResourceAdapter(resource)));
                }
            }
        }

        return caps;
    }

    public Promise<Collection<Resource>> findProviders(
            final RequirementExpression pExpression) {
        logger.log(Logger.LOG_DEBUG,
                "Submiting resource searching to another thread.");

        return pf.submit(new Callable<Collection<Resource>>() {
            @Override
            public Collection<Resource> call() throws Exception {
                Set<Resource> resources = new HashSet<>();
                return resolveExpression(pExpression, resources);
            }
        });
    }

    public ExpressionCombiner getExpressionCombiner() {
        return new ExpressionCombinerImpl();
    }

    public RequirementBuilder newRequirementBuilder(String pNamespace) {
        return new RequirementBuilderImpl(pNamespace);
    }

    private Collection<Resource> resolveAndExpression(
            final AndExpression pAndExpression,
            final Set<Resource> pResources) {
        List<RequirementExpression> andRequirements = pAndExpression
                .getRequirementExpressions();
        if (andRequirements.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Resource> notResources = new HashSet<>();
        Set<Resource> andResources = new HashSet<>();
        for (RequirementExpression requirementExpression : andRequirements) {
            if (requirementExpression instanceof NotExpression) {
                notResources.addAll(resolveExpression(requirementExpression,
                        new HashSet<Resource>()));
                continue;
            }
            Collection<Resource> resolved = resolveExpression(
                    requirementExpression, new HashSet<Resource>());
            if (resolved.isEmpty()) {
                return resolved;
            } else {
                for (Resource r : resolved) {
                    if (!andResources.contains(r)) {
                        andResources.add(r);
                    }
                }
            }
        }
        pResources.addAll(andResources);
        pResources.addAll(notResources);
        return pResources;

    }

    private Collection<Resource> resolveSingleRequirement(
            final Requirement pRequirement, final Set<Resource> pResources) {
        Collection<Capability> capabilities = findProviders(
                Collections.singleton(pRequirement)).get(pRequirement);
        for (Capability capability : capabilities) {
            pResources.add(capability.getResource());
        }
        return pResources;
    }

    private Collection<Resource> resolveExpression(
            final RequirementExpression pExpression,
            final Set<Resource> pResources) {

        if (pExpression instanceof IdentityExpression) {
            Requirement requirement = ((IdentityExpression) pExpression)
                    .getRequirement();
            return resolveSingleRequirement(requirement, pResources);
        } else
            if (pExpression instanceof AndExpression) {
                return resolveAndExpression((AndExpression) pExpression,
                        pResources);
            } else
                if (pExpression instanceof OrExpression) {
                    return resolveOrExpression((OrExpression) pExpression,
                            pResources);
                } else
                    if (pExpression instanceof NotExpression) {
                        return resolveNotExpression((NotExpression) pExpression,
                                pResources);
                    }
        logger.log(Logger.LOG_ERROR,
                "The RequirementExpression was not recognized: " + pExpression);

        throw new IllegalArgumentException(
                "The RequirementExpression was not recognized: " + pExpression);
    }

    private Collection<Resource> resolveNotExpression(
            final NotExpression pNotExpression,
            final Set<Resource> pResources) {
        RequirementExpression notExpression = pNotExpression
                .getRequirementExpression();
        if (notExpression instanceof IdentityExpression) {
            Requirement negateRequirement = negateSingleRequirement(
                    ((IdentityExpression) notExpression).getRequirement());
            return resolveSingleRequirement(negateRequirement, pResources);
        } else
            if (notExpression instanceof NotExpression) {
                return resolveExpression(((NotExpression) notExpression)
                        .getRequirementExpression(), pResources);
            } else
                if (notExpression instanceof AndExpression) {
                    return resolveInverseExpression(notExpression);
                } else
                    if (notExpression instanceof OrExpression) {
                        return resolveInverseExpression(notExpression);
                    }
        logger.log(Logger.LOG_ERROR,
                "Failure while resolving NotExpression: " + pNotExpression);
        throw new UnsupportedOperationException();
    }

    private Requirement negateSingleRequirement(Requirement pRequeriment) {
        String filter = pRequeriment.getDirectives().get("filter");
        if (filter == null) {
            throw new IllegalStateException(
                    "No filter directive: " + pRequeriment);
        }
        String invFilter = "(!" + filter + ")";
        return newRequirementBuilder(pRequeriment.getNamespace())
                .setAttributes(pRequeriment.getAttributes())
                .setDirectives(pRequeriment.getDirectives())
                .addDirective("filter", invFilter).build();
    }

    private Collection<Resource> resolveInverseExpression(
            RequirementExpression pRequirementExpression) {
        Collection<Resource> andProviders = resolveExpression(
                pRequirementExpression, new HashSet<Resource>());

        Requirement matchAll = newRequirementBuilder(
                IdentityNamespace.IDENTITY_NAMESPACE).build();
        Collection<Resource> allResources = resolveSingleRequirement(matchAll,
                new HashSet<Resource>());

        allResources.removeAll(andProviders);

        return allResources;
    }

    private Collection<Resource> resolveOrExpression(
            final OrExpression pOrExpression, final Set<Resource> pResources) {
        List<RequirementExpression> orRequirements = pOrExpression
                .getRequirementExpressions();
        if (orRequirements.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Resource> orResources = new HashSet<>(orRequirements.size());
        for (RequirementExpression requirementExpression : orRequirements) {
            Collection<Resource> resolved = resolveExpression(
                    requirementExpression, orResources);
            for (Resource r : resolved) {
                if (!pResources.contains(r)) {
                    pResources.add(r);
                }
            }
        }
        return pResources;
    }

}
