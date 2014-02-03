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
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.felix.utils.log.Logger;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;

public class OSGiRepositoryXMLTest extends TestCase
{
    public void testParseStandardRepositoryXML() throws Exception
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

    private RepositoryAdminImpl createRepositoryAdmin() throws Exception
    {
        Bundle sysBundle = Mockito.mock(Bundle.class);
        Mockito.when(sysBundle.getHeaders()).thenReturn(new Hashtable<String, String>());

        BundleContext bc = Mockito.mock(BundleContext.class);
        Mockito.when(bc.getBundle(0)).thenReturn(sysBundle);
        Mockito.when(sysBundle.getBundleContext()).thenReturn(bc);

        return new RepositoryAdminImpl(bc, new Logger(bc));
    }
}
