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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.LazyHashMap.LazyValue;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.Repository;

class OSGiRepositoryImpl implements Repository
{
    private final RepositoryAdmin repository;

    OSGiRepositoryImpl(RepositoryAdmin repository)
    {
        this.repository = repository;
    }

    public Map<Requirement, Collection<Capability>> findProviders(Collection<? extends Requirement> requirements)
    {
        Map<Requirement, Collection<Capability>> m = new HashMap<Requirement, Collection<Capability>>();
        for (Requirement r : requirements)
        {
            m.put(r, findProviders(r));
        }
        return m;
    }

    private Collection<Capability> findProviders(Requirement req)
    {
        List<Capability> caps = new ArrayList<Capability>();
        if (IdentityNamespace.IDENTITY_NAMESPACE.equals(req.getNamespace()))
        {
            for(org.apache.felix.bundlerepository.Repository repo : repository.listRepositories())
            {
                for (org.apache.felix.bundlerepository.Resource res : repo.getResources())
                {
                    String f = req.getDirectives().get(Namespace.REQUIREMENT_FILTER_DIRECTIVE);
                    try
                    {
                        addResourceForIdentity(res,
                            f == null ? null : FrameworkUtil.createFilter(f), caps);
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        else
        {
            org.apache.felix.bundlerepository.Resource[] resources = repository.discoverResources(
                new org.apache.felix.bundlerepository.Requirement[] {new OSGiRequirementAdapter(req)});
            OSGiRequirementAdapter adapter = new OSGiRequirementAdapter(req);
            for (org.apache.felix.bundlerepository.Resource resource : resources)
            {
                for (org.apache.felix.bundlerepository.Capability cap : resource.getCapabilities())
                {
                    if (adapter.isSatisfied(cap))
                        caps.add(new FelixCapabilityAdapter(cap, new FelixResourceAdapter(resource)));
                }
            }
        }

        return caps;
    }

    private void addResourceForIdentity(final org.apache.felix.bundlerepository.Resource res, Filter filter, List<Capability> caps)
        throws Exception
    {
        List<Capability> idCaps = new FelixResourceAdapter(res).getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
        if (idCaps.size() == 0)
            return;

        Capability idCap = idCaps.get(0); // there should only be one osgi.identity anyway
        if (filter != null)
        {
            if (!filter.matches(idCap.getAttributes()))
                return;
        }
        caps.add(idCap);
    }

    static OSGiCapabilityImpl newOSGiIdentityCapability(org.apache.felix.bundlerepository.Resource res)
    {
        @SuppressWarnings("unchecked")
        Map<String, Object> idAttrs = new HashMap<String, Object>(res.getProperties());

        // Set a number of specific properties that need to be translated
        idAttrs.put(IdentityNamespace.IDENTITY_NAMESPACE, res.getSymbolicName());

        if (idAttrs.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE) == null)
            idAttrs.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE, IdentityNamespace.TYPE_BUNDLE);

        return new OSGiCapabilityImpl(IdentityNamespace.IDENTITY_NAMESPACE, idAttrs, Collections.<String, String> emptyMap());
    }

    static OSGiCapabilityImpl newOSGiContentCapability(Resource resource)
    {
        final String uri = resource.getURI();
        LazyValue<String, Object> lazyValue =
            new LazyValue<String, Object>(ContentNamespace.CONTENT_NAMESPACE, new Callable<Object>()
            {
                public Object call() throws Exception
                {
                    // This is expensive to do, so only compute it when actually obtained...
                    return OSGiRepositoryImpl.getSHA256(uri);
                }
            });

        Object mime = resource.getProperties().get("mime");
        if (mime == null)
            mime = "application/vnd.osgi.bundle";

        Map<String, Object> contentAttrs = new LazyHashMap<String, Object>(Collections.singleton(lazyValue));
        contentAttrs.put(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE, mime);
        contentAttrs.put(ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE, resource.getSize());
        contentAttrs.put(ContentNamespace.CAPABILITY_URL_ATTRIBUTE, uri);
        return new OSGiCapabilityImpl(ContentNamespace.CONTENT_NAMESPACE, contentAttrs, Collections.<String, String> emptyMap());
    }

    static String getSHA256(String uri) throws IOException, NoSuchAlgorithmException // TODO find a good place for this
    {
        InputStream is = new URL(uri).openStream();
        MessageDigest md = MessageDigest.getInstance("SHA-256");

        // Use a digest inputstream as using byte arrays directly to compute the SHA-256 can
        // have big effects on memory consumption. I.e. you don't want to have to read the
        // entire resource in memory. We rather stream it through...
        DigestInputStream dis = new DigestInputStream(is, md);

        byte[] buffer = new byte[16384];
        while (dis.read(buffer) != -1) {
            // we just drain the stream here to compute the Message Digest
        }

        StringBuilder sb = new StringBuilder(64); // SHA-256 is always 64 hex characters
        for (byte b : md.digest())
        {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
