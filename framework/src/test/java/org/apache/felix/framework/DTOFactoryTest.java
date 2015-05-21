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
package org.apache.felix.framework;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.dto.BundleStartLevelDTO;
import org.osgi.framework.wiring.dto.BundleRevisionDTO;
import org.osgi.framework.wiring.dto.BundleWiringDTO;
import org.osgi.resource.dto.CapabilityDTO;

public class DTOFactoryTest
{
    private int counter;
    private Framework framework;
    private File testDir;

    @Before
    public void setUp() throws Exception
    {
        String path = "/" + getClass().getName().replace('.', '/') + ".class";
        String url = getClass().getResource(path).getFile();
        String baseDir = url.substring(0, url.length() - path.length());
        String rndStr = Long.toString(System.nanoTime(), Character.MAX_RADIX);
        rndStr = rndStr.substring(rndStr.length() - 6, rndStr.length() - 1);
        testDir = new File(baseDir, getClass().getSimpleName() + "_" + rndStr);

        File cacheDir = new File(testDir, "cache");
        cacheDir.mkdirs();
        String cache = cacheDir.getAbsolutePath();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("felix.cache.profiledir", cache);
        params.put("felix.cache.dir", cache);
        params.put(Constants.FRAMEWORK_STORAGE, cache);

        framework = new Felix(params);
        framework.init();
        framework.start();
    }

    @After
    public void tearDown() throws Exception
    {
        framework.stop();
    }

    @Test
    public void testBundleStartLevelDTO() throws Exception
    {
        String mf = "Bundle-SymbolicName: tb1\n"
                + "Bundle-Version: 1.0.0\n"
                + "Bundle-ManifestVersion: 2\n";
        File bf = createBundle(mf);
        Bundle bundle = framework.getBundleContext().installBundle(bf.toURI().toURL().toExternalForm());

        BundleStartLevel sl = bundle.adapt(BundleStartLevel.class);
        sl.setStartLevel(7);

        BundleStartLevelDTO dto = bundle.adapt(BundleStartLevelDTO.class);
        assertEquals(bundle.getBundleId(), dto.bundle);
        assertEquals(7, dto.startLevel);
    }

    @Test
    public void testServiceReferenceDTOArray() throws Exception
    {
        ServiceRegistration<String> reg = framework.getBundleContext().registerService(String.class, "hi", null);
        Long sid = (Long) reg.getReference().getProperty(Constants.SERVICE_ID);

        ServiceReferenceDTO[] dtos = framework.adapt(ServiceReferenceDTO[].class);
        assertTrue(dtos.length > 0);

        boolean found = false;
        for (ServiceReferenceDTO dto : dtos)
        {
            if (dto.id == sid)
            {
                found = true;
                assertEquals(0L, dto.bundle);
                assertEquals(sid, dto.properties.get(Constants.SERVICE_ID));
                assertTrue(Arrays.equals(new String [] {String.class.getName()},
                        (String []) dto.properties.get(Constants.OBJECTCLASS)));
                assertEquals(0L, dto.properties.get(Constants.SERVICE_BUNDLEID));
                assertEquals(Constants.SCOPE_SINGLETON, dto.properties.get(Constants.SERVICE_SCOPE));
                assertEquals(0, dto.usingBundles.length);
            }
        }
        assertTrue(found);
    }

    @Test
    public void testServiceReferenceDTOArrayStoppedBundle() throws Exception
    {
        String mf = "Bundle-SymbolicName: tb2\n"
                + "Bundle-Version: 1.2.3\n"
                + "Bundle-ManifestVersion: 2\n"
                + "Import-Package: org.osgi.framework;version=\"[1.1,2)\"";
        File bf = createBundle(mf);
        Bundle bundle = framework.getBundleContext().installBundle(bf.toURI().toURL().toExternalForm());

        assertNull("Precondition", bundle.getBundleContext());
        ServiceReferenceDTO[] dtos = bundle.adapt(ServiceReferenceDTO[].class);

        // Note this is incorrectly tested by the Core Framework R6 CT, which expects an
        // empty array. However this is not correct and recorded as a deviation.
        assertNull("As the bundle is not started, the dtos should be null", dtos);
    }

    @Test
    public void testBundleRevisionDTO() throws Exception
    {
        String mf = "Bundle-SymbolicName: tb2\n"
                + "Bundle-Version: 1.2.3\n"
                + "Bundle-ManifestVersion: 2\n"
                + "Import-Package: org.osgi.framework;version=\"[1.1,2)\"";
        File bf = createBundle(mf);
        Bundle bundle = framework.getBundleContext().installBundle(bf.toURI().toURL().toExternalForm());
        bundle.start();
        assertEquals("Precondition", Bundle.ACTIVE, bundle.getState());

        BundleRevisionDTO dto = bundle.adapt(BundleRevisionDTO.class);
        assertEquals(bundle.getBundleId(), dto.bundle);
        assertTrue(dto.id != 0);
        assertEquals("tb2", dto.symbolicName);
        assertEquals("1.2.3", dto.version);
        assertEquals(0, dto.type);

        boolean foundBundle = false;
        boolean foundHost = false;
        boolean foundIdentity = false;
        int resource = 0;
        for (CapabilityDTO cap : dto.capabilities)
        {
            assertTrue(cap.id != 0);
            if (resource == 0)
                resource = cap.resource;
            else
                assertEquals(resource, cap.resource);

            if (BundleNamespace.BUNDLE_NAMESPACE.equals(cap.namespace))
            {
                foundBundle = true;
                assertEquals("tb2", cap.attributes.get(BundleNamespace.BUNDLE_NAMESPACE));
                assertEquals("1.2.3", cap.attributes.get(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE));
            }
            else if (HostNamespace.HOST_NAMESPACE.equals(cap.namespace))
            {
                foundHost = true;
                assertEquals("tb2", cap.attributes.get(HostNamespace.HOST_NAMESPACE));
                assertEquals("1.2.3", cap.attributes.get(HostNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE));
            }
            else if (IdentityNamespace.IDENTITY_NAMESPACE.equals(cap.namespace))
            {
                foundIdentity = true;
                assertEquals("tb2", cap.attributes.get(IdentityNamespace.IDENTITY_NAMESPACE));
                assertEquals("1.2.3", cap.attributes.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE));
                assertEquals(IdentityNamespace.TYPE_BUNDLE, cap.attributes.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));
            }
        }
        assertTrue(foundBundle);
        assertTrue(foundHost);
        assertTrue(foundIdentity);
    }

    @Test
    public void testBundleRevisionDTOArray() throws Exception {
        String mf = "Bundle-SymbolicName: tb2\n"
                + "Bundle-Version: 1.2.3\n"
                + "Bundle-ManifestVersion: 2\n"
                + "Import-Package: org.osgi.framework;version=\"[1.1,2)\"";
        File bf = createBundle(mf);
        Bundle bundle = framework.getBundleContext().installBundle(bf.toURI().toURL().toExternalForm());
        bundle.start();
        assertEquals("Precondition", Bundle.ACTIVE, bundle.getState());

        BundleRevisionDTO[] dtos = bundle.adapt(BundleRevisionDTO[].class);
        assertEquals(1, dtos.length);
        BundleRevisionDTO dto = dtos[0];

        assertEquals(bundle.getBundleId(), dto.bundle);
        assertTrue(dto.id != 0);
        assertEquals("tb2", dto.symbolicName);
        assertEquals("1.2.3", dto.version);
        assertEquals(0, dto.type);

        boolean foundBundle = false;
        boolean foundHost = false;
        boolean foundIdentity = false;
        int resource = 0;
        for (CapabilityDTO cap : dto.capabilities)
        {
            assertTrue(cap.id != 0);
            if (resource == 0)
                resource = cap.resource;
            else
                assertEquals(resource, cap.resource);

            if (BundleNamespace.BUNDLE_NAMESPACE.equals(cap.namespace))
            {
                foundBundle = true;
                assertEquals("tb2", cap.attributes.get(BundleNamespace.BUNDLE_NAMESPACE));
                assertEquals("1.2.3", cap.attributes.get(BundleNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE));
            }
            else if (HostNamespace.HOST_NAMESPACE.equals(cap.namespace))
            {
                foundHost = true;
                assertEquals("tb2", cap.attributes.get(HostNamespace.HOST_NAMESPACE));
                assertEquals("1.2.3", cap.attributes.get(HostNamespace.CAPABILITY_BUNDLE_VERSION_ATTRIBUTE));
            }
            else if (IdentityNamespace.IDENTITY_NAMESPACE.equals(cap.namespace))
            {
                foundIdentity = true;
                assertEquals("tb2", cap.attributes.get(IdentityNamespace.IDENTITY_NAMESPACE));
                assertEquals("1.2.3", cap.attributes.get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE));
                assertEquals(IdentityNamespace.TYPE_BUNDLE, cap.attributes.get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE));
            }
        }
        assertTrue(foundBundle);
        assertTrue(foundHost);
        assertTrue(foundIdentity);
    }

    @Test
    public void testBundleWiringDTO() throws Exception {
        String mf = "Bundle-SymbolicName: tb2\n"
                + "Bundle-Version: 1.2.3\n"
                + "Bundle-ManifestVersion: 2\n"
                + "Import-Package: org.osgi.framework;version=\"[1.1,2)\"";
        File bf = createBundle(mf);
        Bundle bundle = framework.getBundleContext().installBundle(bf.toURI().toURL().toExternalForm());
        bundle.start();
        assertEquals("Precondition", Bundle.ACTIVE, bundle.getState());

        BundleWiringDTO dto = bundle.adapt(BundleWiringDTO.class);
        assertEquals(bundle.getBundleId(), dto.bundle);
    }

    private File createBundle(String manifest) throws IOException
    {
        File f = File.createTempFile("felix-bundle" + counter++, ".jar", testDir);

        Manifest mf = new Manifest(new ByteArrayInputStream(manifest.getBytes("utf-8")));
        mf.getMainAttributes().putValue("Manifest-Version", "1.0");
        JarOutputStream os = new JarOutputStream(new FileOutputStream(f), mf);

        os.close();
        return f;
    }
}
