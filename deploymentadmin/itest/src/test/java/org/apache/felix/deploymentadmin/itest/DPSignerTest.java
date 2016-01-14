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
package org.apache.felix.deploymentadmin.itest;

import static junit.framework.TestCase.assertNotNull;
import static org.apache.felix.deploymentadmin.itest.BaseIntegrationTest.TEST_FAILING_BUNDLE_RP1;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.security.Security;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.felix.deploymentadmin.itest.util.CertificateUtil;
import org.apache.felix.deploymentadmin.itest.util.CertificateUtil.KeyType;
import org.apache.felix.deploymentadmin.itest.util.CertificateUtil.SignerInfo;
import org.apache.felix.deploymentadmin.itest.util.DPSigner;
import org.apache.felix.deploymentadmin.itest.util.DeploymentPackageBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for {@link DPSigner}.
 */
public class DPSignerTest {

    @Test
    public void testSignArtifactsOk() throws Exception {
        URL dpProps = getClass().getResource("/dp.properties");
        assertNotNull(dpProps);

        DeploymentPackageBuilder builder = DeploymentPackageBuilder.create("dpSignerTest1", "1.0.0");
        builder.add(builder.createLocalizationResource().setUrl(dpProps).setResourceProcessorPID(TEST_FAILING_BUNDLE_RP1).setFilename("dp.properties"));

        SignerInfo signerInfo = CertificateUtil.createSelfSignedCert("CN=testCert", KeyType.RSA);

        DPSigner signer = new DPSigner();

        byte[] rawData;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            signer.sign(builder, signerInfo.getPrivate(), signerInfo.getCert(), baos);
            rawData = baos.toByteArray();
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(rawData); JarInputStream jis = new JarInputStream(bais, true)) {
            JarEntry entry;
            while ((entry = jis.getNextJarEntry()) != null) {
                assertNotNull(entry);
                jis.closeEntry();
            }
            
            assertNotNull(jis.getManifest());
        }
    }

    @Before
    public void setUp() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @After
    public void tearDown() {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
    }
}
