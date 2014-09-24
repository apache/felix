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
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.TestCase;

import org.apache.felix.utils.log.Logger;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.resource.Resource;
import org.osgi.service.repository.ContentNamespace;
import org.osgi.service.repository.Repository;

public class OSGiRepositoryXMLTest extends TestCase
{
    public void testIdentityCapability() throws Exception
    {
        RepositoryAdminImpl repoAdmin = createRepositoryAdmin();
        URL url = getClass().getResource("/spec_repository.xml");
        repoAdmin.addRepository(url);

        Repository repo = new OSGiRepositoryImpl(repoAdmin);
        Requirement req = new OSGiRequirementImpl("osgi.identity", "(osgi.identity=cdi-subsystem)");

        Map<Requirement, Collection<Capability>> result = repo.findProviders(Collections.singleton(req));
        assertEquals(1, result.size());
        Collection<Capability> caps = result.values().iterator().next();
        assertEquals(1, caps.size());
        Capability cap = caps.iterator().next();

        assertEquals("cdi-subsystem", cap.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE));
        assertEquals(Version.parseVersion("0.5.0"), cap.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE));
        assertEquals("osgi.subsystem.feature", cap.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));
    }

    public void testOtherIdentityAttribute() throws Exception
    {
        RepositoryAdminImpl repoAdmin = createRepositoryAdmin();
        URL url = getClass().getResource("/spec_repository.xml");
        repoAdmin.addRepository(url);

        Repository repo = new OSGiRepositoryImpl(repoAdmin);
        Requirement req = new OSGiRequirementImpl("osgi.identity",
                "(license=http://www.opensource.org/licenses/mytestlicense)");

        Map<Requirement, Collection<Capability>> result = repo.findProviders(Collections.singleton(req));
        assertEquals(1, result.size());
        Collection<Capability> caps = result.values().iterator().next();
        assertEquals(1, caps.size());
        Capability cap = caps.iterator().next();
        assertEquals("org.apache.felix.bundlerepository.test_file_3", cap.getAttributes().
                get(IdentityNamespace.IDENTITY_NAMESPACE));
    }

    public void testContentCapability() throws Exception
    {
        RepositoryAdminImpl repoAdmin = createRepositoryAdmin();
        URL url = getClass().getResource("/spec_repository.xml");
        repoAdmin.addRepository(url);

        Repository repo = new OSGiRepositoryImpl(repoAdmin);
        Requirement req = new OSGiRequirementImpl("foo", "(bar=toast)");

        Map<Requirement, Collection<Capability>> result = repo.findProviders(Collections.singleton(req));
        assertEquals(1, result.size());
        Collection<Capability> caps = result.values().iterator().next();
        assertEquals(1, caps.size());
        Capability cap = caps.iterator().next();

        assertEquals("foo", cap.getNamespace());
        assertEquals(0, cap.getDirectives().size());
        assertEquals(1, cap.getAttributes().size());
        Entry<String, Object> fooCap = cap.getAttributes().entrySet().iterator().next();
        assertEquals("bar", fooCap.getKey());
        assertEquals("toast", fooCap.getValue());

        Resource res = cap.getResource();
        List<Capability> idCaps = res.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE);
        assertEquals(1, idCaps.size());
        Capability idCap = idCaps.iterator().next();

        assertEquals("org.apache.felix.bundlerepository.test_file_3", idCap.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE));
        assertEquals(Version.parseVersion("1.2.3.something"), idCap.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE));
        assertEquals("osgi.bundle", idCap.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));

        List<Capability> contentCaps = res.getCapabilities(ContentNamespace.CONTENT_NAMESPACE);
        assertEquals(1, contentCaps.size());
        Capability contentCap = contentCaps.iterator().next();

        assertEquals("b5d4045c3f466fa91fe2cc6abe79232a1a57cdf104f7a26e716e0a1e2789df78",
            contentCap.getAttributes().get(ContentNamespace.CONTENT_NAMESPACE));
        assertEquals(new Long(3), contentCap.getAttributes().get(ContentNamespace.CAPABILITY_SIZE_ATTRIBUTE));
        assertEquals("application/vnd.osgi.bundle", contentCap.getAttributes().get(ContentNamespace.CAPABILITY_MIME_ATTRIBUTE));

        URL fileURL = getClass().getResource("/repo_files/test_file_3.jar");
        byte[] expectedBytes = Streams.suck(fileURL.openStream());

        String resourceURL = (String) contentCap.getAttributes().get(ContentNamespace.CAPABILITY_URL_ATTRIBUTE);
        byte[] actualBytes = Streams.suck(new URL(resourceURL).openStream());
        assertEquals(3L, actualBytes.length);
        assertTrue(Arrays.equals(expectedBytes, actualBytes));
    }

    private RepositoryAdminImpl createRepositoryAdmin() throws Exception
    {
        Bundle sysBundle = Mockito.mock(Bundle.class);
        Mockito.when(sysBundle.getHeaders()).thenReturn(new Hashtable<String, String>());
        BundleRevision br = Mockito.mock(BundleRevision.class);
        Mockito.when(sysBundle.adapt(BundleRevision.class)).thenReturn(br);

        BundleContext bc = Mockito.mock(BundleContext.class);
        Mockito.when(bc.getBundle(0)).thenReturn(sysBundle);
        Mockito.when(sysBundle.getBundleContext()).thenReturn(bc);

        return new RepositoryAdminImpl(bc, new Logger(bc));
    }


}
