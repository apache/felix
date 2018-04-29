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
<<<<<<< HEAD
import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.TestCase;
=======
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import junit.framework.TestCase;

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.utils.log.Logger;
import org.easymock.EasyMock;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
<<<<<<< HEAD
=======
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

public class RepositoryImplTest extends TestCase
{
    public void testReferral1() throws Exception
    {
        URL url = getClass().getResource("/referral1_repository.xml");

        RepositoryAdminImpl repoAdmin = createRepositoryAdmin();
        RepositoryImpl repo = (RepositoryImpl) repoAdmin.addRepository(url);
        Referral[] refs = repo.getReferrals();

        assertNotNull("Expect referrals", refs);
        assertTrue("Expect one referral", refs.length == 1);

        // <referral depth="1" url="referred.xml" />
        assertEquals(1, refs[0].getDepth());
        assertEquals("referred.xml", refs[0].getUrl());

        // expect two resources
        Resource[] res = repoAdmin.discoverResources((String) null);
        assertNotNull("Expect Resource", res);
        assertEquals("Expect two resources", 2, res.length);

        // first resource is from the referral1_repository.xml
        assertEquals("6", res[0].getId());
//        assertEquals("referral1_repository", res[0].getRepository().getName());

        // second resource is from the referred.xml
        assertEquals("99", res[1].getId());
//        assertEquals("referred", res[1].getRepository().getName());
    }

    public void testReferral2() throws Exception
    {
        URL url = getClass().getResource("/referral1_repository.xml");

        RepositoryAdminImpl repoAdmin = createRepositoryAdmin();
<<<<<<< HEAD
        RepositoryImpl repo = (RepositoryImpl) repoAdmin.addRepository(url, 1);
=======
        RepositoryImpl repo = repoAdmin.addRepository(url, 1);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        Referral[] refs = repo.getReferrals();

        assertNotNull("Expect referrals", refs);
        assertTrue("Expect one referral", refs.length == 1);

        // <referral depth="1" url="referred.xml" />
        assertEquals(1, refs[0].getDepth());
        assertEquals("referred.xml", refs[0].getUrl());

        // expect one resource (referral is not followed
        Resource[] res = repoAdmin.discoverResources((String) null);
        assertNotNull("Expect Resource", res);
        assertEquals("Expect one resource", 1, res.length);

        // first resource is from the referral1_repository.xml
        assertEquals("6", res[0].getId());
//        assertEquals("referral1_repository", res[0].getRepository().getName());
    }

    private RepositoryAdminImpl createRepositoryAdmin() throws Exception
    {
<<<<<<< HEAD
        BundleContext bundleContext = (BundleContext) EasyMock.createMock(BundleContext.class);
        Bundle systemBundle = (Bundle) EasyMock.createMock(Bundle.class);
=======
        BundleContext bundleContext = EasyMock.createMock(BundleContext.class);
        Bundle systemBundle = EasyMock.createMock(Bundle.class);
        BundleRevision systemBundleRevision = EasyMock.createMock(BundleRevision.class);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        Activator.setContext(bundleContext);
        EasyMock.expect(bundleContext.getProperty((String) EasyMock.anyObject())).andReturn(null).anyTimes();
        EasyMock.expect(bundleContext.getBundle(0)).andReturn(systemBundle);
        EasyMock.expect(systemBundle.getHeaders()).andReturn(new Hashtable());
        EasyMock.expect(systemBundle.getRegisteredServices()).andReturn(null);
        EasyMock.expect(new Long(systemBundle.getBundleId())).andReturn(new Long(0)).anyTimes();
        EasyMock.expect(systemBundle.getBundleContext()).andReturn(bundleContext);
<<<<<<< HEAD
=======
        EasyMock.expect(systemBundleRevision.getCapabilities(null)).andReturn(Collections.<Capability>emptyList());
        EasyMock.expect(systemBundle.adapt(BundleRevision.class)).andReturn(systemBundleRevision);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        bundleContext.addBundleListener((BundleListener) EasyMock.anyObject());
        bundleContext.addServiceListener((ServiceListener) EasyMock.anyObject());
        EasyMock.expect(bundleContext.getBundles()).andReturn(new Bundle[] { systemBundle });
        EasyMock.expect(bundleContext.createFilter(null)).andReturn(new Filter() {
            public boolean match(ServiceReference reference) {
                return true;
            }
            public boolean match(Dictionary dictionary) {
                return true;
            }
            public boolean matchCase(Dictionary dictionary) {
                return true;
            }
<<<<<<< HEAD
        }).anyTimes();
        EasyMock.replay(new Object[] { bundleContext, systemBundle });
=======
            public boolean matches(Map<String, ?> map) {
                return true;
            }
        }).anyTimes();
        EasyMock.replay(new Object[] { bundleContext, systemBundle, systemBundleRevision });
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        RepositoryAdminImpl repoAdmin = new RepositoryAdminImpl(bundleContext, new Logger(bundleContext));

        // force initialization && remove all initial repositories
        Repository[] repos = repoAdmin.listRepositories();
        for (int i = 0; repos != null && i < repos.length; i++)
        {
            repoAdmin.removeRepository(repos[i].getURI());
        }

        return repoAdmin;
    }

}