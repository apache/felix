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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.utils.log.Logger;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.Repository;
import org.osgi.service.repository.RepositoryContent;

public class OSGiRepositoryImplTest extends TestCase
{
    public void testCapabilities() throws Exception
    {
        RepositoryAdminImpl repoAdmin = createRepositoryAdmin();
        URL url = getClass().getResource("/another_repository.xml");
        repoAdmin.addRepository(url);

        Repository repo = new OSGiRepositoryImpl(repoAdmin);
        Requirement req = new OSGiRequirementImpl("osgi.identity", null);

        Map<Requirement, Collection<Capability>> result = repo.findProviders(Collections.singleton(req));
        assertEquals(1, result.size());
        Collection<Capability> caps = result.values().iterator().next();
        assertEquals(2, caps.size());

        Capability tf1Cap = null;
        for (Capability cap : caps)
        {
            if ("test_file_1".equals(cap.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE))) {
                tf1Cap = cap;
                break;
            }
        }

        assertEquals(Version.parseVersion("1.0.0.SNAPSHOT"), tf1Cap.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE));
        assertEquals(IdentityNamespace.TYPE_BUNDLE, tf1Cap.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));

        Resource res = tf1Cap.getResource();
        assertEquals(0, res.getRequirements(null).size());
        assertEquals(1, res.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).size());
        assertEquals(1, res.getCapabilities(ContentNamespace.CONTENT_NAMESPACE).size());
        assertEquals(1, res.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE).size());
        assertEquals(8, res.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE).size());
        assertEquals(1, res.getCapabilities("foo").size());
        assertEquals(12, res.getCapabilities(null).size());

        Capability contentCap = res.getCapabilities(ContentNamespace.CONTENT_NAMESPACE).iterator().next();
        assertEquals("4b68ab3847feda7d6c62c1fbcbeebfa35eab7351ed5e78f4ddadea5df64b8015",
                contentCap.getAttributes().get(ContentNamespace.CONTENT_NAMESPACE));
        assertEquals(getClass().getResource("/repo_files/test_file_1.jar").toExternalForm(),
                contentCap.getAttributes().get(ContentNamespace.CAPABILITY_URL_ATTRIBUTE));
        assertEquals(1L, contentCap.getAttributes().get(ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE));
        assertEquals("application/vnd.osgi.bundle", contentCap.getAttributes().get(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE));

        Capability bundleCap = res.getCapabilities(BundleNamespace.BUNDLE_NAMESPACE).iterator().next();
        assertEquals("2", bundleCap.getAttributes().get("manifestversion"));
        assertEquals("dummy", bundleCap.getAttributes().get(BundleNamespace.BUNDLE_NAMESPACE));
        assertEquals(Version.parseVersion("1.0.0.SNAPSHOT"), bundleCap.getAttributes().get(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE));
        assertEquals("Unnamed - dummy", bundleCap.getAttributes().get("presentationname"));

        Capability packageCap = res.getCapabilities(PackageNamespace.PACKAGE_NAMESPACE).get(7);
        assertEquals("org.apache.commons.logging", packageCap.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE));
        assertEquals(Version.parseVersion("1.0.4"), packageCap.getAttributes().get(PackageNamespace.CAPABILITY_VERSION_ATTRIBUTE));
        assertEquals("dummy", packageCap.getAttributes().get(PackageNamespace.CAPABILITY_BUNDLE_SYMBOLICNAME_ATTRIBUTE));
        assertEquals(Version.parseVersion("1.0.0.SNAPSHOT"), packageCap.getAttributes().get(PackageNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE));

        Capability fooCap = res.getCapabilities("foo").iterator().next();
        assertEquals("someVal", fooCap.getAttributes().get("someKey"));
    }

    public void testIdentityCapabilityFilter() throws Exception
    {
        RepositoryAdminImpl repoAdmin = createRepositoryAdmin();
        URL url = getClass().getResource("/another_repository.xml");
        repoAdmin.addRepository(url);

        Repository repo = new OSGiRepositoryImpl(repoAdmin);
        Requirement req = new OSGiRequirementImpl("osgi.identity", "(osgi.identity=test_file_2)");

        Map<Requirement, Collection<Capability>> result = repo.findProviders(Collections.singleton(req));
        assertEquals(1, result.size());
        Collection<Capability> caps = result.values().iterator().next();
        assertEquals(1, caps.size());
        Capability cap = caps.iterator().next();

        assertEquals("test_file_2", cap.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE));
        assertEquals(Version.parseVersion("1.0.0"), cap.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE));
        assertEquals(IdentityNamespace.TYPE_BUNDLE, cap.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));
    }

    public void testFilterOnCapability() throws Exception
    {
        RepositoryAdminImpl repoAdmin = createRepositoryAdmin();
        URL url = getClass().getResource("/another_repository.xml");
        repoAdmin.addRepository(url);

        Repository repo = new OSGiRepositoryImpl(repoAdmin);
        Requirement req = new OSGiRequirementImpl("foo", "(someKey=someOtherVal)");

        Map<Requirement, Collection<Capability>> result = repo.findProviders(Collections.singleton(req));
        assertEquals(1, result.size());
        Collection<Capability> caps = result.values().iterator().next();
        assertEquals(1, caps.size());

        Resource res = caps.iterator().next().getResource();
        assertEquals("test_file_2",
            res.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).iterator().next().
            getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE));
    }

    public void testFilterOnCapabilityExistence() throws Exception
    {
        RepositoryAdminImpl repoAdmin = createRepositoryAdmin();
        URL url = getClass().getResource("/another_repository.xml");
        repoAdmin.addRepository(url);

        Repository repo = new OSGiRepositoryImpl(repoAdmin);
        Requirement req = new OSGiRequirementImpl("foo", "(someKey=*)");

        Map<Requirement, Collection<Capability>> result = repo.findProviders(Collections.singleton(req));
        assertEquals(1, result.size());
        Collection<Capability> caps = result.values().iterator().next();
        assertEquals(2, caps.size());

        Set<Object> identities = new HashSet<Object>();
        for (Capability cap : caps)
        {
            identities.add(cap.getResource().getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).
                iterator().next().getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE));
        }

        Set<String> expected = new HashSet<String>(Arrays.asList("test_file_1", "test_file_2"));
        assertEquals(expected, identities);
    }

    public void testRepositoryContent() throws Exception {
        RepositoryAdminImpl repoAdmin = createRepositoryAdmin();
        URL url = getClass().getResource("/another_repository.xml");
        repoAdmin.addRepository(url);

        Repository repo = new OSGiRepositoryImpl(repoAdmin);
        Requirement req = new OSGiRequirementImpl("osgi.wiring.package",
                "(&(osgi.wiring.package=org.apache.commons.logging)(version>=1.0.1)(!(version>=2)))");

        Map<Requirement, Collection<Capability>> result = repo.findProviders(Collections.singleton(req));
        assertEquals(1, result.size());
        Collection<Capability> caps = result.values().iterator().next();
        assertEquals(1, caps.size());
        Capability cap = caps.iterator().next();
        assertEquals("osgi.wiring.package", cap.getNamespace());
        assertEquals("org.apache.commons.logging", cap.getAttributes().get("osgi.wiring.package"));
        assertEquals(Version.parseVersion("1.0.4"), cap.getAttributes().get("version"));

        Resource resource = cap.getResource();
        RepositoryContent rc = (RepositoryContent) resource; // Repository Resources must implement this interface
        byte[] actualBytes = Streams.suck(rc.getContent());

        URL actualURL = getClass().getResource("/repo_files/test_file_1.jar");
        byte[] expectedBytes = Streams.suck(actualURL.openStream());

        assertTrue(Arrays.equals(expectedBytes, actualBytes));
    }

    public void testSystemBundleCapabilities() throws Exception {
        RepositoryAdminImpl repoAdmin = createRepositoryAdmin();
        Resolver resolver = repoAdmin.resolver();
        RequirementImpl req = new RequirementImpl("some.system.cap");
        req.setFilter("(sys.cap=something)");
        resolver.add(req);
        ResourceImpl res = new ResourceImpl();
        res.addRequire(req);

        resolver.add(res);
        assertTrue(resolver.resolve());

        // This should add the system bundle repo to the resolved set.
        org.apache.felix.bundlerepository.Resource sysBundleRes = repoAdmin.getSystemRepository().getResources()[0];
        Reason[] reason = resolver.getReason(sysBundleRes);
        assertTrue(reason.length >= 1);
        assertEquals(req, reason[0].getRequirement());
    }

    private RepositoryAdminImpl createRepositoryAdmin() throws Exception
    {
        Bundle sysBundle = Mockito.mock(Bundle.class);
        Mockito.when(sysBundle.getHeaders()).thenReturn(new Hashtable<String, String>());

        BundleRevision br = Mockito.mock(BundleRevision.class);
        Mockito.when(sysBundle.adapt(BundleRevision.class)).thenReturn(br);
        Capability cap1 = new OSGiCapabilityImpl("some.system.cap",
                Collections.<String, Object>singletonMap("sys.cap", "something"),
                Collections.singletonMap("x", "y"));
        Capability cap2 = new OSGiCapabilityImpl("some.system.cap",
                Collections.<String, Object>singletonMap("sys.cap", "somethingelse"),
                Collections.<String, String>emptyMap());
        Mockito.when(br.getCapabilities(null)).thenReturn(Arrays.asList(cap1, cap2));

        BundleContext bc = Mockito.mock(BundleContext.class);
        Mockito.when(bc.getBundle(0)).thenReturn(sysBundle);
        Mockito.when(sysBundle.getBundleContext()).thenReturn(bc);

        return new RepositoryAdminImpl(bc, new Logger(bc));
    }
}
