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

import static org.osgi.service.deploymentadmin.DeploymentException.CODE_BAD_HEADER;
import static org.osgi.service.deploymentadmin.DeploymentException.CODE_BUNDLE_SHARING_VIOLATION;
import static org.osgi.service.deploymentadmin.DeploymentException.CODE_MISSING_BUNDLE;
import static org.osgi.service.deploymentadmin.DeploymentException.CODE_MISSING_FIXPACK_TARGET;
import static org.osgi.service.deploymentadmin.DeploymentException.CODE_MISSING_RESOURCE;

import org.apache.felix.deploymentadmin.itest.util.DeploymentPackageBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;

/**
 * Provides test cases regarding the use of "fix-packages" in DeploymentAdmin.
 */
@RunWith(PaxExam.class)
public class InstallFixPackageTest extends BaseIntegrationTest {

    /**
     * Tests that we can install a new bundle through a fix-package.
     */
    @Test
    public void testInstallBundleWithDependencyInFixPackageUpdateOk() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle2")));

        DeploymentPackage dp1 = m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
        assertNotNull("No deployment package returned?!", dp1);

        awaitRefreshPackagesEvent();

        Bundle bundle = dp1.getBundle(getSymbolicName("bundle2"));
        assertNotNull("Failed to obtain bundle from deployment package?!", bundle);

        assertEquals(Bundle.INSTALLED, bundle.getState());

        dpBuilder = createDeploymentPackageBuilder(dpBuilder.getSymbolicName(), "1.0.1");
        dpBuilder
            .setFixPackage("[1.0,2.0)")
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle1")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle2")).setMissing());

        DeploymentPackage dp2 = m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
        assertNotNull("No deployment package returned?!", dp2);

        awaitRefreshPackagesEvent();

        bundle = dp2.getBundle(getSymbolicName("bundle2"));
        assertNotNull("Failed to obtain bundle from bundle context?!", bundle);

        assertBundleExists(getSymbolicName("bundle1"), "1.0.0");
        assertBundleExists(getSymbolicName("bundle2"), "1.0.0");

        assertTrue(isBundleActive(bundle));
    }

    /**
     * Tests that it is not possible to install a fix package if it specifies a fix-version range that falls outside the installed target deployment package.
     */
    @Test
    public void testInstallFixPackageOutsideLowerTargetRangeFail() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle2")));

        DeploymentPackage dp1 = m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
        assertNotNull("No deployment package returned?!", dp1);

        awaitRefreshPackagesEvent();

        Bundle bundle = dp1.getBundle(getSymbolicName("bundle2"));
        assertNotNull("Failed to obtain bundle from deployment package?!", bundle);

        assertEquals(Bundle.INSTALLED, bundle.getState());

        dpBuilder = createDeploymentPackageBuilder(dpBuilder.getSymbolicName(), "1.0.1");
        dpBuilder
            .setFixPackage("(1.0,2.0)") // should not include version 1.0.0!
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle1")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle2")).setMissing());

        try {
            m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
            fail("Succeeded into installing fix package for undefined target package?!");
        }
        catch (DeploymentException exception) {
            // Ok; expected
            assertDeploymentException(CODE_MISSING_FIXPACK_TARGET, exception);
        }
    }

    /**
     * Tests that it is not possible to install a fix package if it specifies a fix-version range that falls outside the installed target deployment package.
     */
    @Test
    public void testInstallFixPackageOutsideUpperTargetRangeFail() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle2")));

        DeploymentPackage dp1 = m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
        assertNotNull("No deployment package returned?!", dp1);

        awaitRefreshPackagesEvent();

        Bundle bundle = dp1.getBundle(getSymbolicName("bundle2"));
        assertNotNull("Failed to obtain bundle from deployment package?!", bundle);

        assertEquals(Bundle.INSTALLED, bundle.getState());

        dpBuilder = createDeploymentPackageBuilder(dpBuilder.getSymbolicName(), "1.0.1");
        dpBuilder
            .setFixPackage("[0.9,1.0)") // should not include version 1.0.0!
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle1")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle2")).setMissing());

        try {
            m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
            fail("Succeeded into installing fix package for undefined target package?!");
        }
        catch (DeploymentException exception) {
            // Ok; expected
            assertDeploymentException(CODE_MISSING_FIXPACK_TARGET, exception);
        }
    }

    /**
     * Tests that a fix package can only be installed after at least one version of the denoted target package is installed.
     */
    @Test
    public void testInstallFixPackageWithoutTargetFail() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .setFixPackage("[1.0,2.0)")
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle1")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle2")));

        try {
            m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
            fail("Should not be able to install fix package without target?!");
        }
        catch (DeploymentException exception) {
            // Ok; expected
            assertDeploymentException(CODE_MISSING_FIXPACK_TARGET, exception);
        }
    }

    /**
     * Tests that installing a fix-package causes the original target package to be replaced.
     */
    @Test
    public void testInstallFixPackageReplacesOriginalTargetPackageOk() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle2")));

        DeploymentPackage dp1 = m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
        assertNotNull("No deployment package returned?!", dp1);

        assertEquals("Expected only a single deployment package?!", 1, m_deploymentAdmin.listDeploymentPackages().length);

        awaitRefreshPackagesEvent();

        dpBuilder = createDeploymentPackageBuilder(dpBuilder.getSymbolicName(), "1.0.1");
        dpBuilder
            .setFixPackage("[1.0,2.0)")
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle1")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle2")).setMissing());

        DeploymentPackage dp2 = m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
        assertNotNull("No deployment package returned?!", dp2);

        awaitRefreshPackagesEvent();

        assertEquals("Expected only a single deployment package?!", 1, m_deploymentAdmin.listDeploymentPackages().length);
    }

    /**
     * Tests that installing a fix-package that mentions a bundle that is not in the target package fails.
     */
    @Test
    public void testInstallFixPackageWithMissingTargetBundleFail() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        // missed valid-bundle1 as dependency...
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle1")));

        DeploymentPackage dp1 = m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
        assertNotNull("No deployment package returned?!", dp1);

        assertEquals("Expected only a single deployment package?!", 1, m_deploymentAdmin.listDeploymentPackages().length);

        awaitRefreshPackagesEvent();

        dpBuilder = createDeploymentPackageBuilder(dpBuilder.getSymbolicName(), "1.0.1");
        dpBuilder
            .setFixPackage("[1.0,2.0)")
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle3")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle2")).setMissing());

        try {
            m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
            fail("Succeeded into installing a fix-package with a missing bundle on target?!");
        }
        catch (DeploymentException exception) {
            assertDeploymentException(CODE_MISSING_BUNDLE, exception);
        }
    }

    /**
     * Tests that installing a fix-package that mentions a resource that is not in the target package fails.
     */
    @Test
    public void testInstallFixPackageWithMissingTargetResourceFail() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle1")));

        DeploymentPackage dp1 = m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
        assertNotNull("No deployment package returned?!", dp1);

        assertEquals("Expected only a single deployment package?!", 1, m_deploymentAdmin.listDeploymentPackages().length);

        awaitRefreshPackagesEvent();

        dpBuilder = createDeploymentPackageBuilder(dpBuilder.getSymbolicName(), "1.0.1");
        dpBuilder
            .setFixPackage("[1.0,2.0)")
            .add(dpBuilder.createResourceProcessorResource().setUrl(getTestBundle("rp1")))
            .add(dpBuilder.createResource().setResourceProcessorPID(TEST_FAILING_BUNDLE_RP1).setUrl(getTestResource("test-config1.xml")).setMissing());

        try {
            m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
            fail("Succeeded into installing a fix-package with a missing bundle on target?!");
        }
        catch (DeploymentException exception) {
            assertDeploymentException(CODE_MISSING_RESOURCE, exception);
        }
    }

    /**
     * Tests that installing a fix-package that mentions a resource processor that is not in the target package fails.
     */
    @Test
    public void testInstallFixPackageWithMissingTargetResourceProcessorFail() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle1")));

        DeploymentPackage dp1 = m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
        assertNotNull("No deployment package returned?!", dp1);

        assertEquals("Expected only a single deployment package?!", 1, m_deploymentAdmin.listDeploymentPackages().length);

        awaitRefreshPackagesEvent();

        dpBuilder = createDeploymentPackageBuilder(dpBuilder.getSymbolicName(), "1.0.1");
        dpBuilder
            .setFixPackage("[1.0,2.0)")
            .add(dpBuilder.createResourceProcessorResource().setUrl(getTestBundle("rp1")).setMissing())
            .add(dpBuilder.createResource().setResourceProcessorPID(TEST_FAILING_BUNDLE_RP1).setUrl(getTestResource("test-config1.xml")));

        try {
            m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
            fail("Succeeded into installing a fix-package with a missing bundle on target?!");
        }
        catch (DeploymentException exception) {
            assertDeploymentException(CODE_MISSING_BUNDLE, exception);
        }
    }

    /**
     * Tests that installing a fix-package that mentions a bundle that does exist (in another DP), but is not in the target package fails.
     */
    @Test
    public void testInstallFixPackageWithMissingTargetBundleFromOtherPackageFail() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle1")));

        DeploymentPackage dp1 = m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
        assertNotNull("No deployment package returned?!", dp1);

        awaitRefreshPackagesEvent();

        assertEquals("Expected only a single deployment package?!", 1, m_deploymentAdmin.listDeploymentPackages().length);

        dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle2")));

        DeploymentPackage dp2 = m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
        assertNotNull("No deployment package returned?!", dp2);

        awaitRefreshPackagesEvent();

        assertEquals("Expected only a single deployment package?!", 2, m_deploymentAdmin.listDeploymentPackages().length);

        dpBuilder = createDeploymentPackageBuilder(dpBuilder.getSymbolicName(), "1.0.1");
        dpBuilder
            .setFixPackage("[1.0,2.0)")
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle1")).setMissing())
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle3")));

        try {
            m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
            fail("Succeeded into installing a fix-package with a missing bundle on target?!");
        }
        catch (DeploymentException exception) {
            assertDeploymentException(CODE_BUNDLE_SHARING_VIOLATION, exception);
        }
    }

    /**
     * Tests that only in a fix-package bundle can be marked as missing.
     */
    @Test
    public void testMissingBundlesOnlyInFixPackageFail() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle2")));

        DeploymentPackage dp1 = m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
        assertNotNull("No deployment package returned?!", dp1);

        awaitRefreshPackagesEvent();

        Bundle bundle = dp1.getBundle(getSymbolicName("bundle2"));
        assertNotNull("Failed to obtain bundle from deployment package?!", bundle);

        assertEquals(Bundle.INSTALLED, bundle.getState());

        dpBuilder = createDeploymentPackageBuilder(dpBuilder.getSymbolicName(), "1.0.1");
        dpBuilder
            .disableVerification()
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle1")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle2")).setMissing());

        try {
            m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
            fail("Failed to install missing bundle?!");
        }
        catch (DeploymentException exception) {
            // Ok; expected...
            assertEquals("Invalid exception code?!", CODE_BAD_HEADER, exception.getCode());
        }
    }

    /**
     * Tests the removal of a bundle through a fix package.
     */
    @Test
    public void testRemoveBundleInFixPackageUpdateOk() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle1")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle2")));

        DeploymentPackage dp1 = m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
        assertNotNull("No deployment package returned?!", dp1);

        awaitRefreshPackagesEvent();

        Bundle bundle = dp1.getBundle(getSymbolicName("bundle2"));
        assertNotNull("Failed to obtain bundle from deployment package?!", bundle);

        assertEquals(Bundle.ACTIVE, bundle.getState());

        // valid-bundle2 is to be removed by this fix package...
        dpBuilder = createDeploymentPackageBuilder(dpBuilder.getSymbolicName(), "1.0.1");
        dpBuilder
            .setFixPackage("[1.0,2.0)")
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle1")).setMissing());

        DeploymentPackage dp2 = m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
        assertNotNull("No deployment package returned?!", dp2);

        awaitRefreshPackagesEvent();

        assertBundleExists(getSymbolicName("bundle1"), "1.0.0");
        assertBundleNotExists(getSymbolicName("bundle2"), "1.0.0");
    }

    /**
     * Tests that we can uninstall a fix-package.
     */
    @Test
    public void testUninstallBundleAddedInFixPackageOk() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle2")));

        DeploymentPackage dp1 = m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
        assertNotNull("No deployment package returned?!", dp1);

        awaitRefreshPackagesEvent();

        // Add valid-bundle1 through fix-package...
        dpBuilder = createDeploymentPackageBuilder(dpBuilder.getSymbolicName(), "1.0.1");
        dpBuilder
            .setFixPackage("[1.0,2.0)")
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle1")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle2")).setMissing());

        DeploymentPackage dp2 = m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
        assertNotNull("No deployment package returned?!", dp2);

        awaitRefreshPackagesEvent();

        assertEquals("Expected a single deployment package?!", 1, m_deploymentAdmin.listDeploymentPackages().length);

        assertBundleExists(getSymbolicName("bundle1"), "1.0.0");
        assertBundleExists(getSymbolicName("bundle2"), "1.0.0");

        // Uninstall the deployment package; should yield the original situation again...
        dp2.uninstall();

        awaitRefreshPackagesEvent();

        assertEquals("Expected no deployment package?!", 0, m_deploymentAdmin.listDeploymentPackages().length);

        // None of our installed bundles should remain...
        assertBundleNotExists(getSymbolicName("bundle1"), "1.0.0");
        assertBundleNotExists(getSymbolicName("bundle2"), "1.0.0");
    }

    /**
     * Tests that we can uninstall a fix-package.
     */
    @Test
    public void testUninstallBundleRemovedInFixPackageOk() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle1")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle2")));

        DeploymentPackage dp1 = m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
        assertNotNull("No deployment package returned?!", dp1);

        awaitRefreshPackagesEvent();

        // remove valid-bundle1 through fix package...
        dpBuilder = createDeploymentPackageBuilder(dpBuilder.getSymbolicName(), "1.0.1");
        dpBuilder
            .setFixPackage("[1.0,2.0)")
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle1")));

        DeploymentPackage dp2 = m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
        assertNotNull("No deployment package returned?!", dp2);

        awaitRefreshPackagesEvent();

        assertEquals("Expected a single deployment package?!", 1, m_deploymentAdmin.listDeploymentPackages().length);

        assertBundleExists(getSymbolicName("bundle1"), "1.0.0");
        assertBundleNotExists(getSymbolicName("bundle2"), "1.0.0");

        // Uninstall the deployment package; should yield the initial situation again...
        dp2.uninstall();

        awaitRefreshPackagesEvent();

        assertEquals("Expected no deployment package?!", 0, m_deploymentAdmin.listDeploymentPackages().length);

        // None of our installed bundles should remain...
        assertBundleNotExists(getSymbolicName("bundle1"), "1.0.0");
        assertBundleNotExists(getSymbolicName("bundle2"), "1.0.0");
    }

    /**
     * Tests that we can uninstall a fix-package and that this will only uninstall the bundles installed by the fix-package.
     */
    @Test
    public void testUninstallFixPackageOnlyRemovesOwnArtifactsOk() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle1")));

        DeploymentPackage dp1 = m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
        assertNotNull("No deployment package returned?!", dp1);

        assertEquals("Expected a single deployment package?!", 1, m_deploymentAdmin.listDeploymentPackages().length);

        dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle2")));

        DeploymentPackage dp2 = m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
        assertNotNull("No deployment package returned?!", dp2);

        awaitRefreshPackagesEvent();

        assertEquals("Expected two deployment packages?!", 2, m_deploymentAdmin.listDeploymentPackages().length);

        // add bundle2 through fix package...
        dpBuilder = createDeploymentPackageBuilder(dpBuilder.getSymbolicName(), "1.0.1");
        dpBuilder
            .setFixPackage("[1.0,2.0)")
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle3")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundle("bundle2")).setMissing());

        DeploymentPackage dp3 = m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
        assertNotNull("No deployment package returned?!", dp3);

        awaitRefreshPackagesEvent();

        assertEquals("Expected two deployment packages?!", 2, m_deploymentAdmin.listDeploymentPackages().length);

        assertBundleExists(getSymbolicName("bundle1"), "1.0.0");
        assertBundleExists(getSymbolicName("bundle2"), "1.0.0");
        assertBundleExists(getSymbolicName("bundle3"), "1.0.0");

        // Uninstall the deployment package; should yield the initial situation again...
        dp3.uninstall();

        awaitRefreshPackagesEvent();

        assertEquals("Expected a single deployment package?!", 1, m_deploymentAdmin.listDeploymentPackages().length);

        // None of our installed bundles should remain...
        assertBundleNotExists(getSymbolicName("bundle3"), "1.0.0");
        assertBundleNotExists(getSymbolicName("bundle2"), "1.0.0");
        // The bundle installed in another deployment package should still remain...
        assertBundleExists(getSymbolicName("bundle1"), "1.0.0");
    }
}
