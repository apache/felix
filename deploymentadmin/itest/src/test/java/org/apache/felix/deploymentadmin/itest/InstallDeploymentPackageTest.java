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

import static org.apache.felix.deploymentadmin.itest.util.CertificateUtil.createSelfSignedCert;

import java.io.File;
import java.net.URL;

import org.apache.felix.deploymentadmin.itest.util.CertificateUtil.KeyType;
import org.apache.felix.deploymentadmin.itest.util.CertificateUtil.SignerInfo;
import org.apache.felix.deploymentadmin.itest.util.DeploymentPackageBuilder;
import org.apache.felix.deploymentadmin.itest.util.DeploymentPackageBuilder.JarManifestManipulatingFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.service.deploymentadmin.BundleInfo;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;

/**
 * Provides test cases regarding the use of "normal" deployment packages in DeploymentAdmin.
 */
@RunWith(PaxExam.class)
public class InstallDeploymentPackageTest extends BaseIntegrationTest {
    /**
     * FELIX-518 - Test that DP with localization and signature files are properly deployed.
     */
    @Test
    public void testInstallDeploymentPackageWithLocalizationAndSignatureFilesOk() throws Exception {
        URL dpProps = getClass().getResource("/dp.properties");
        assertNotNull(dpProps);

        SignerInfo signer = createSelfSignedCert("CN=dpTest", KeyType.EC);

        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder.signOutput(signer.getPrivate(), signer.getCert())
            .add(dpBuilder.createLocalizationResource().setUrl(dpProps).setResourceProcessorPID(TEST_FAILING_BUNDLE_RP1).setFilename("dp.properties"))
            .add(dpBuilder.createResourceProcessorResource().setUrl(getTestBundleURL("rp1")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle1")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle2")));

        installDeploymentPackage(dpBuilder); // should succeed.
        
        assertBundleExists("testbundles.bundle1", "1.0.0");
        assertBundleExists("testbundles.bundle2", "1.0.0");
        assertBundleExists("testbundles.rp1", "1.0.0");
    }

    /**
     * FELIX-4409/4410/4463 - test the installation of an invalid deployment package.
     */
    @Test
    public void testInstallInvalidDeploymentPackageFail() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        // incluse two different versions of the same bundle (with the same BSN), this is *not* allowed per the DA
        // spec...
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundleapi1", "bundleapi1", "1.0.0")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundleapi2", "bundleapi2", "2.0.0")));

        try {
            installDeploymentPackage(dpBuilder);
            fail("DeploymentException expected!");
        }
        catch (DeploymentException e) {
            // Ok; expected...
        }

        // Verify that none of the bundles are installed...
        assertBundleNotExists("testbundles.bundleapi", "1.0.0");
        assertBundleNotExists("testbundles.bundleapi", "2.0.0");
    }

    /**
     * FELIX-1835 - test whether we can install bundles with a non-root path inside the DP.
     */
    @Test
    public void testInstallBundlesWithPathsOk() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        // incluse two different versions of the same bundle (with the same BSN), this is *not* allowed per the DA
        // spec...
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundleapi1", "bundleapi1", "1.0.0")).setFilename("bundles/bundleapi1.jar"))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundleimpl1", "bundleimpl1", "1.0.0")).setFilename("bundles/bundleimpl1.jar"));

        DeploymentPackage dp = installDeploymentPackage(dpBuilder);
        assertNotNull(dp);

        BundleInfo[] bundleInfos = dp.getBundleInfos();
        assertEquals(2, bundleInfos.length);

        // Verify that none of the bundles are installed...
        assertBundleExists("testbundles.bundleapi", "1.0.0");
        assertBundleExists("testbundles.bundleimpl", "1.0.0");
    }

    /**
     * Tests that adding the dependency for a bundle in an update package causes the depending bundle to be resolved and
     * started.
     */
    @Test
    public void testInstallBundleWithDependencyInPackageUpdateOk() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        // missing bundle1 as dependency...
        dpBuilder.add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle2")));

        DeploymentPackage dp1 = installDeploymentPackage(dpBuilder);
        assertNotNull("No deployment package returned?!", dp1);

        awaitRefreshPackagesEvent();

        Bundle bundle = dp1.getBundle(getSymbolicName("bundle2"));
        assertNotNull("Failed to obtain bundle from deployment package?!", bundle);

        assertTrue(isBundleInstalled(dp1.getBundle(getSymbolicName("bundle2"))));

        dpBuilder = createDeploymentPackageBuilder(dpBuilder.getSymbolicName(), "1.0.1");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle2")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle1")));

        DeploymentPackage dp2 = installDeploymentPackage(dpBuilder);
        assertNotNull("No deployment package returned?!", dp2);

        awaitRefreshPackagesEvent();

        assertTrue(isBundleActive(dp2.getBundle(getSymbolicName("bundle1"))));
        assertTrue(isBundleActive(dp2.getBundle(getSymbolicName("bundle2"))));
    }

    /**
     * Tests that installing a bundle with a dependency installed by another deployment package is not started, but is
     * resolved.
     */
    @Test
    public void testInstallBundleWithDependencyInSeparatePackageOk() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder.add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle2")));

        DeploymentPackage dp1 = installDeploymentPackage(dpBuilder);
        assertNotNull("No deployment package returned?!", dp1);

        awaitRefreshPackagesEvent();

        assertBundleExists(getSymbolicName("bundle2"), "1.0.0");

        // We shouldn't be able to resolve the deps for bundle2...
        assertFalse(resolveBundles(dp1.getBundle(getSymbolicName("bundle2"))));

        assertTrue(isBundleInstalled(dp1.getBundle(getSymbolicName("bundle2"))));

        dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        // as missing bundle1...
        dpBuilder.add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle1")));

        DeploymentPackage dp2 = installDeploymentPackage(dpBuilder);
        assertNotNull("No deployment package returned?!", dp2);

        awaitRefreshPackagesEvent();

        assertBundleExists(getSymbolicName("bundle1"), "1.0.0");
        assertBundleExists(getSymbolicName("bundle2"), "1.0.0");

        // Now we should be able to resolve the dependencies for bundle2...
        assertTrue(resolveBundles(dp1.getBundle(getSymbolicName("bundle2"))));

        assertTrue(isBundleActive(dp2.getBundle(getSymbolicName("bundle1"))));
        assertTrue(isBundleResolved(dp1.getBundle(getSymbolicName("bundle2"))));
    }

    /**
     * Tests that if an exception is thrown in the start method of a bundle, the installation is not rolled back.
     */
    @Test
    public void testInstallBundleWithExceptionThrownInStartCausesNoRollbackOk() throws Exception {
        System.setProperty("bundle3", "start");

        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle1")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle3")));

        DeploymentPackage dp = installDeploymentPackage(dpBuilder);
        assertNotNull("No deployment package returned?!", dp);

        awaitRefreshPackagesEvent();

        assertBundleExists(getSymbolicName("bundle1"), "1.0.0");
        assertBundleExists(getSymbolicName("bundle3"), "1.0.0");

        assertTrue(isBundleActive(dp.getBundle(getSymbolicName("bundle1"))));
        // the bundle threw an exception during start, so it is not active...
        assertFalse(isBundleActive(dp.getBundle(getSymbolicName("bundle3"))));

        assertEquals("Expected a single deployment package?!", 1, countDeploymentPackages());
    }

    /**
     * Tests that installing a bundle along with a fragment bundle succeeds (DA should not try to start the fragment,
     * see FELIX-4167).
     */
    @Test
    public void testInstallBundleWithFragmentOk() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle1")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("fragment1")));

        DeploymentPackage dp = installDeploymentPackage(dpBuilder);
        assertNotNull("No deployment package returned?!", dp);

        awaitRefreshPackagesEvent();

        assertBundleExists(getSymbolicName("bundle1"), "1.0.0");
        assertBundleExists(getSymbolicName("fragment1"), "1.0.0");

        assertTrue(isBundleActive(dp.getBundle(getSymbolicName("bundle1"))));
        assertFalse(isBundleActive(dp.getBundle(getSymbolicName("fragment1"))));

        assertEquals("Expected a single deployment package?!", 1, countDeploymentPackages());
    }

    /**
     * Tests that installing a bundle whose dependencies cannot be met, is installed, but not started.
     */
    @Test
    public void testInstallBundleWithMissingDependencyOk() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder.add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle2")));

        DeploymentPackage dp = installDeploymentPackage(dpBuilder);
        assertNotNull("No deployment package returned?!", dp);

        awaitRefreshPackagesEvent();

        Bundle bundle = dp.getBundle(getSymbolicName("bundle2"));
        assertNotNull("Failed to obtain bundle from deployment package?!", bundle);

        assertBundleExists(getSymbolicName("bundle2"), "1.0.0");

        assertTrue(isBundleInstalled(dp.getBundle(getSymbolicName("bundle2"))));
    }

    /**
     * Tests that installing a bundle along with other (non-bundle) artifacts succeeds.
     */
    @Test
    public void testInstallBundleWithOtherArtifactsOk() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createResourceProcessorResource().setUrl(getTestBundleURL("rp1")))
            .add(dpBuilder.createResource().setResourceProcessorPID(TEST_FAILING_BUNDLE_RP1).setUrl(getTestResource("test-config1.xml")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle3")));

        DeploymentPackage dp = installDeploymentPackage(dpBuilder);
        assertNotNull("No deployment package returned?!", dp);

        awaitRefreshPackagesEvent();

        // Though the commit failed; the package should be installed...
        assertBundleExists(getSymbolicName("rp1"), "1.0.0");
        assertBundleExists(getSymbolicName("bundle3"), "1.0.0");

        assertEquals("Expected a single deployment package?!", 1, countDeploymentPackages());
    }

    /**
     * Tests that installing a new bundle works as expected.
     */
    @Test
    public void testInstallSingleValidBundleOk() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder.add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle1")));

        DeploymentPackage dp = installDeploymentPackage(dpBuilder);
        assertNotNull("No deployment package returned?!", dp);

        awaitRefreshPackagesEvent();

        assertNotNull("Failed to obtain test service?!", awaitService(TEST_SERVICE_NAME));

        assertBundleExists(getSymbolicName("bundle1"), "1.0.0");
        assertTrue(isBundleActive(dp.getBundle(getSymbolicName("bundle1"))));
    }

    /**
     * Tests that installing two bundles works as expected.
     */
    @Test
    public void testInstallTwoValidBundlesOk() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle1")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle2")));

        DeploymentPackage dp = installDeploymentPackage(dpBuilder);
        assertNotNull("No deployment package returned?!", dp);

        awaitRefreshPackagesEvent();

        assertNotNull("Failed to obtain test service?!", awaitService(TEST_SERVICE_NAME));

        assertBundleExists(getSymbolicName("bundle1"), "1.0.0");
        assertBundleExists(getSymbolicName("bundle2"), "1.0.0");

        assertTrue(isBundleActive(dp.getBundle(getSymbolicName("bundle1"))));
        assertTrue(isBundleActive(dp.getBundle(getSymbolicName("bundle2"))));
    }

    /**
     * Tests that if an exception is thrown during the uninstall of a bundle, the installation/update continues and
     * succeeds.
     */
    @Test
    public void testUninstallBundleWithExceptionThrownInStopCauseNoRollbackOk() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle1")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle3")));

        DeploymentPackage dp = installDeploymentPackage(dpBuilder);
        assertNotNull("No deployment package returned?!", dp);

        awaitRefreshPackagesEvent();

        assertBundleExists(getSymbolicName("bundle3"), "1.0.0");

        System.setProperty("bundle3", "stop");

        dpBuilder = dpBuilder.create("1.0.1");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle1")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle2")));

        dp = installDeploymentPackage(dpBuilder);
        assertNotNull("No deployment package returned?!", dp);

        assertBundleExists(getSymbolicName("bundle1"), "1.0.0");
        assertBundleExists(getSymbolicName("bundle2"), "1.0.0");
        assertBundleNotExists(getSymbolicName("bundle3"), "1.0.0");

        assertTrue(isBundleActive(dp.getBundle(getSymbolicName("bundle1"))));
        assertTrue(isBundleActive(dp.getBundle(getSymbolicName("bundle2"))));

        assertEquals("Expected a single deployment package?!", 1, countDeploymentPackages());
    }

    /**
     * Tests that if an exception is thrown during the stop of a bundle, the installation/update continues and succeeds.
     */
    @Test
    public void testUpdateBundleWithExceptionThrownInStopCauseNoRollbackOk() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle1")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle3")));

        DeploymentPackage dp = installDeploymentPackage(dpBuilder);
        assertNotNull("No deployment package returned?!", dp);

        awaitRefreshPackagesEvent();

        assertBundleExists(getSymbolicName("bundle3"), "1.0.0");

        System.setProperty("bundle3", "stop");

        dpBuilder = createDeploymentPackageBuilder(dpBuilder.getSymbolicName(), "1.0.1");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle1")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle2")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle3")));

        dp = installDeploymentPackage(dpBuilder);
        assertNotNull("No deployment package returned?!", dp);

        assertBundleExists(getSymbolicName("bundle1"), "1.0.0");
        assertBundleExists(getSymbolicName("bundle2"), "1.0.0");
        assertBundleExists(getSymbolicName("bundle3"), "1.0.0");

        assertTrue(isBundleActive(dp.getBundle(getSymbolicName("bundle1"))));
        assertTrue(isBundleActive(dp.getBundle(getSymbolicName("bundle2"))));
        assertTrue(isBundleActive(dp.getBundle(getSymbolicName("bundle3"))));

        assertEquals("Expected a single deployment package?!", 1, countDeploymentPackages());
    }

    /**
     * Tests that we can correctly rollback the installation of a deployment package for bundles that have their data
     * area populated.
     */
    @Test
    public void testRollbackWithPopulatedDataAreaOk() throws Exception {
        // Install a first version, in which we're going to change the data area of a bundle...
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle1")))
            .add(dpBuilder.createResourceProcessorResource().setUrl(getTestBundleURL("rp1")));

        DeploymentPackage dp = installDeploymentPackage(dpBuilder);
        assertNotNull("No deployment package returned?!", dp);

        awaitRefreshPackagesEvent();

        Bundle bundle1 = getBundle("testbundles.bundle1");
        assertNotNull("Unable to get installed test bundle?!", bundle1);

        File dataArea = bundle1.getDataFile("");
        assertNotNull("No data area obtained for test bundle?!", dataArea);

        // Populate the data area...
        assertTrue("No file created?!", new File(dataArea, "file1").createNewFile());
        assertTrue("No file created?!", new File(dataArea, "file2").createNewFile());
        assertTrue("No file created?!", new File(dataArea, "file3").createNewFile());

        // This will cause the new bundle to fail in its stop method...
        System.setProperty("rp1", "process");

        // Simulate an upgrade for our bundle, which should cause its data area to be retained...
        dpBuilder = createDeploymentPackageBuilder(dpBuilder.getSymbolicName(), "1.0.1");
        dpBuilder
            .add(dpBuilder.createBundleResource().setVersion("1.1.0").setUrl(getTestBundleURL("bundle1")).setFilter(new JarManifestManipulatingFilter("Bundle-Version", "1.1.0")))
            .add(dpBuilder.createResourceProcessorResource().setUrl(getTestBundleURL("rp1")))
            .add(dpBuilder.createResource().setResourceProcessorPID(TEST_FAILING_BUNDLE_RP1).setUrl(getTestResource("test-config1.xml")));

        try {
            dp = installDeploymentPackage(dpBuilder); // should fail!
            fail("Deployment of upgrade package should have failed?!");
        }
        catch (DeploymentException e) {
            // Ok; expected...
        }

        // We should still have this bundle..
        bundle1 = getBundle("testbundles.bundle1");
        assertNotNull("Unable to get installed test bundle?!", bundle1);

        dataArea = bundle1.getDataFile("");
        assertNotNull("No data area obtained for test bundle?!", dataArea);

        // Data area should be restored exactly as-is...
        assertTrue("File not restored?!", new File(dataArea, "file1").exists());
        assertTrue("File not restored?!", new File(dataArea, "file2").exists());
        assertTrue("File not restored?!", new File(dataArea, "file3").exists());
    }

    /**
     * Tests that we can correctly install a deployment package with bundles that have their data area populated.
     */
    @Test
    public void testUpgradeWithPopulatedDataAreaOk() throws Exception {
        // Install a first version, in which we're going to change the data area of a bundle...
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setVersion("1.0.0").setUrl(getTestBundleURL("bundle1")))
            .add(dpBuilder.createResourceProcessorResource().setUrl(getTestBundleURL("rp1")));

        DeploymentPackage dp = installDeploymentPackage(dpBuilder);
        assertNotNull("No deployment package returned?!", dp);

        awaitRefreshPackagesEvent();

        Bundle bundle1 = getBundle("testbundles.bundle1");
        assertNotNull("Unable to get installed test bundle?!", bundle1);

        File dataArea = bundle1.getDataFile("");
        assertNotNull("No data area obtained for test bundle?!", dataArea);

        // Populate the data area...
        assertTrue("No file created?!", new File(dataArea, "file1").createNewFile());
        assertTrue("No file created?!", new File(dataArea, "file2").createNewFile());
        assertTrue("No file created?!", new File(dataArea, "file3").createNewFile());

        dpBuilder = createDeploymentPackageBuilder(dpBuilder.getSymbolicName(), "1.0.1");
        dpBuilder
            .add(dpBuilder.createBundleResource().setVersion("1.1.0").setUrl(getTestBundleURL("bundle1")).setFilter(new JarManifestManipulatingFilter("Bundle-Version", "1.1.0")))
            .add(dpBuilder.createResourceProcessorResource().setUrl(getTestBundleURL("rp1")))
            .add(dpBuilder.createResource().setResourceProcessorPID(TEST_FAILING_BUNDLE_RP1).setUrl(getTestResource("test-config1.xml")));

        dp = installDeploymentPackage(dpBuilder); // should succeed!

        // We should still have this bundle..
        bundle1 = getBundle("testbundles.bundle1");
        assertNotNull("Unable to get installed test bundle?!", bundle1);

        dataArea = bundle1.getDataFile("");
        assertNotNull("No data area obtained for test bundle?!", dataArea);

        // Data area should be restored exactly as-is...
        assertTrue("File not restored?!", new File(dataArea, "file1").exists());
        assertTrue("File not restored?!", new File(dataArea, "file2").exists());
        assertTrue("File not restored?!", new File(dataArea, "file3").exists());
    }

    /**
     * Tests that we can correctly install a deployment package with bundles that have their data area populated.
     */
    @Test
    public void testUninstallBundleWithPopulatedDataAreaOk() throws Exception {
        // Install a first version, in which we're going to change the data area of a bundle...
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setVersion("1.0.0").setUrl(getTestBundleURL("bundle1")))
            .add(dpBuilder.createResourceProcessorResource().setUrl(getTestBundleURL("rp1")));

        DeploymentPackage dp = installDeploymentPackage(dpBuilder);
        assertNotNull("No deployment package returned?!", dp);

        awaitRefreshPackagesEvent();

        Bundle bundle1 = getBundle("testbundles.bundle1");
        assertNotNull("Unable to get installed test bundle?!", bundle1);

        File dataArea = bundle1.getDataFile("");
        assertNotNull("No data area obtained for test bundle?!", dataArea);

        // Populate the data area...
        assertTrue("No file created?!", new File(dataArea, "file1").createNewFile());
        assertTrue("No file created?!", new File(dataArea, "file2").createNewFile());
        assertTrue("No file created?!", new File(dataArea, "file3").createNewFile());

        dpBuilder = createDeploymentPackageBuilder(dpBuilder.getSymbolicName(), "1.0.1");
        dpBuilder
            .add(dpBuilder.createResourceProcessorResource().setUrl(getTestBundleURL("rp1")))
            .add(dpBuilder.createResource().setResourceProcessorPID(TEST_FAILING_BUNDLE_RP1).setUrl(getTestResource("test-config1.xml")));

        dp = installDeploymentPackage(dpBuilder); // should succeed!

        // We should no longer have this bundle..
        bundle1 = getBundle("testbundles.bundle1");
        assertNull("Unable to get installed test bundle?!", bundle1);

        // Data area should be restored exactly as-is...
        assertFalse("Data area not purged?!", dataArea.exists());
    }

    /**
     * Tests that we can correctly install a deployment package with bundles that have their data area populated.
     */
    @Test
    public void testRollbackUninstallBundleWithPopulatedDataAreaOk() throws Exception {
        // Install a first version, in which we're going to change the data area of a bundle...
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setVersion("1.0.0").setUrl(getTestBundleURL("bundle1")))
            .add(dpBuilder.createResourceProcessorResource().setUrl(getTestBundleURL("rp1")));

        DeploymentPackage dp = installDeploymentPackage(dpBuilder);
        assertNotNull("No deployment package returned?!", dp);

        awaitRefreshPackagesEvent();

        Bundle bundle1 = getBundle("testbundles.bundle1");
        assertNotNull("Unable to get installed test bundle?!", bundle1);

        File dataArea = bundle1.getDataFile("");
        assertNotNull("No data area obtained for test bundle?!", dataArea);

        // Populate the data area...
        assertTrue("No file created?!", new File(dataArea, "file1").createNewFile());
        assertTrue("No file created?!", new File(dataArea, "file2").createNewFile());
        assertTrue("No file created?!", new File(dataArea, "file3").createNewFile());

        // This will cause the new bundle to fail in its stop method...
        System.setProperty("rp1", "process");

        dpBuilder = createDeploymentPackageBuilder(dpBuilder.getSymbolicName(), "1.0.1");
        dpBuilder
            .add(dpBuilder.createResourceProcessorResource().setUrl(getTestBundleURL("rp1")))
            .add(dpBuilder.createResource().setResourceProcessorPID(TEST_FAILING_BUNDLE_RP1).setUrl(getTestResource("test-config1.xml")));

        try {
            dp = installDeploymentPackage(dpBuilder); // should fail!
            fail("Deployment of upgrade package should have failed?!");
        }
        catch (DeploymentException e) {
            // Ok; expected...
        }

        // We should still have this bundle..
        bundle1 = getBundle("testbundles.bundle1");
        assertNotNull("Unable to get installed test bundle?!", bundle1);

        dataArea = bundle1.getDataFile("");
        assertNotNull("No data area obtained for test bundle?!", dataArea);

        // Data area should be restored exactly as-is...
        assertTrue("File not restored?!", new File(dataArea, "file1").exists());
        assertTrue("File not restored?!", new File(dataArea, "file2").exists());
        assertTrue("File not restored?!", new File(dataArea, "file3").exists());
    }
}
