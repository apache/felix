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

import static org.osgi.service.deploymentadmin.DeploymentException.CODE_FOREIGN_CUSTOMIZER;
import static org.osgi.service.deploymentadmin.DeploymentException.CODE_OTHER_ERROR;
import static org.osgi.service.deploymentadmin.DeploymentException.CODE_PROCESSOR_NOT_FOUND;

import org.apache.felix.deploymentadmin.itest.util.DeploymentPackageBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;

/**
 * Provides test cases on the use of customizers in Deployment Admin. 
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class CustomizerTest extends BaseIntegrationTest {

    /**
     * FELIX-4491 - Only resources with a resource processor defined should be processed. 
     */
    @Test
    public void testInstallBundleWithNonProcessedResourcesOk() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createResource().setNeedResourceProcessor(false).setUrl(getTestResource("LICENSE")))
            .add(dpBuilder.createResourceProcessorResource().setUrl(getTestBundleURL("rp1")))
            .add(dpBuilder.createResource().setResourceProcessorPID(TEST_FAILING_BUNDLE_RP1).setUrl(getTestResource("test-config1.xml")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle1")));

        DeploymentPackage dp = installDeploymentPackage(dpBuilder);
        assertNotNull("No deployment package returned?!", dp);

        assertEquals("Expected a single deployment package?!", 1, countDeploymentPackages());

        awaitRefreshPackagesEvent();

        assertBundleExists(getSymbolicName("bundle1"), "1.0.0");
        
        // Check that only the processed resources are dropped...
        dp.uninstall();

        assertEquals("Expected no deployment package?!", 0, countDeploymentPackages());

        awaitRefreshPackagesEvent();
        
        assertBundleNotExists(getSymbolicName("bundle1"), "1.0.0");
    }
    
    /**
     * Tests that if an exception is thrown during the commit-phase, the installation proceeds and succeeds.
     */
    @Test
    public void testInstallBundleWithExceptionThrowingInCommitCauseNoRollbackOk() throws Exception {
        System.setProperty("rp1", "commit");

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
     * Tests that if an exception is thrown during the prepare-phase, the installation is cancelled and rolled back.
     */
    @Test
    public void testInstallBundleWithExceptionThrowingInPrepareCausesRollbackOk() throws Exception {
        System.setProperty("rp1", "prepare");

        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createResourceProcessorResource().setUrl(getTestBundleURL("rp1")))
            .add(dpBuilder.createResource().setResourceProcessorPID(TEST_FAILING_BUNDLE_RP1).setUrl(getTestResource("test-config1.xml")));

        try {
            installDeploymentPackage(dpBuilder);
            fail("Succeeded into installing a failing deployment package?!");
        }
        catch (DeploymentException exception) {
            // Ok; expected
            assertDeploymentException(DeploymentException.CODE_COMMIT_ERROR, exception);
        }

        assertTrue("No bundles should be started!", getCurrentBundles().isEmpty());

        assertEquals("Expected no deployment package?!", 0, countDeploymentPackages());
    }

    /**
     * Tests that if an exception is thrown during the processing of a resource, the installation is cancelled and rolled back.
     */
    @Test
    public void testInstallResourceWithExceptionThrowingInProcessCausesRollbackOk() throws Exception {
        System.setProperty("rp1", "process");

        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createResourceProcessorResource().setUrl(getTestBundleURL("rp1")))
            .add(dpBuilder.createResource().setResourceProcessorPID(TEST_FAILING_BUNDLE_RP1).setUrl(getTestResource("test-config1.xml")));

        try {
            installDeploymentPackage(dpBuilder);
            fail("Succeeded into installing a failing deployment package?!");
        }
        catch (DeploymentException exception) {
            // Ok; expected
            assertDeploymentException(DeploymentException.CODE_RESOURCE_SHARING_VIOLATION, exception);
        }

        assertTrue("No bundles should be started!", getCurrentBundles().isEmpty());

        assertEquals("Expected no deployment package?!", 0, countDeploymentPackages());
    }

    /**
     * Tests that if an exception is thrown during the dropping of a resource, the installation is continued and finishes normally.
     */
    @Test
    public void testDropResourceWithExceptionThrowingInDroppedCausesRollbackOk() throws Exception {
        System.setProperty("rp1", "dropped");

        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createResourceProcessorResource().setUrl(getTestBundleURL("rp1")))
            .add(dpBuilder.createResource().setResourceProcessorPID(TEST_FAILING_BUNDLE_RP1).setUrl(getTestResource("test-config1.xml")));

        DeploymentPackage dp = installDeploymentPackage(dpBuilder);
        assertNotNull("No deployment package returned?!", dp);

        awaitRefreshPackagesEvent();

        assertTrue("One bundle should be started!", getCurrentBundles().size() == 1);

        assertEquals("Expected no deployment package?!", 1, countDeploymentPackages());
    }

    /**
     * Tests that if an exception is thrown during the commit-phase, the installation proceeds and succeeds.
     */
    @Test
    public void testInstallResourceProcessorWithExceptionThrowingInStartCausesRollbackOk() throws Exception {
        System.setProperty("rp1", "start");

        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createResourceProcessorResource().setUrl(getTestBundleURL("rp1")))
            .add(dpBuilder.createResource().setResourceProcessorPID(TEST_FAILING_BUNDLE_RP1).setUrl(getTestResource("test-config1.xml")))
            .add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle3")));

        try {
            installDeploymentPackage(dpBuilder);
            fail("Succeeded into installing a failing RP?!");
        }
        catch (DeploymentException exception) {
            // Ok; expected...
            assertDeploymentException(CODE_OTHER_ERROR, exception);
        }

        assertEquals("Expected no deployment package?!", 0, countDeploymentPackages());
        assertTrue("Expected no artifacts to be installed?!", getCurrentBundles().isEmpty());
    }

    /**
     * Tests that if a resource is installed which mentions a RP that does not belong to the same package, a rollback takes place.
     */
    @Test
    public void testInstallResourceWithForeignCustomizerFail() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createResourceProcessorResource().setUrl(getTestBundleURL("rp1")));

        installDeploymentPackage(dpBuilder);

        awaitRefreshPackagesEvent();
        
        assertEquals("Expected no deployment package?!", 1, countDeploymentPackages());
        assertBundleExists(getSymbolicName("rp1"), "1.0.0");

        dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .disableVerification()
            .add(dpBuilder.createResource().setResourceProcessorPID(TEST_FAILING_BUNDLE_RP1).setUrl(getTestResource("test-config1.xml")));

        try {
            installDeploymentPackage(dpBuilder);
            fail("Succeeded into installing a resource with an non-existing RP?!");
        }
        catch (DeploymentException exception) {
            // Ok; expected...
            assertDeploymentException(CODE_FOREIGN_CUSTOMIZER, exception);
        }

        assertEquals("Expected no deployment package?!", 1, countDeploymentPackages());
        assertTrue("Expected no additional artifacts to be installed?!", getCurrentBundles().size() == 1);
    }

    /**
     * Tests that if a resource is installed which mentions a RP that does not exist a rollback takes place.
     */
    @Test
    public void testInstallResourceWithNonAvailableCustomizerFail() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .disableVerification()
            .add(dpBuilder.createResource().setResourceProcessorPID("my.unknown.rp").setUrl(getTestResource("test-config1.xml")));

        try {
            installDeploymentPackage(dpBuilder);
            fail("Succeeded into installing a resource with an non-existing RP?!");
        }
        catch (DeploymentException exception) {
            // Ok; expected...
            assertDeploymentException(CODE_PROCESSOR_NOT_FOUND, exception);
        }

        assertEquals("Expected no deployment package?!", 0, countDeploymentPackages());
        assertTrue("Expected no artifacts to be installed?!", getCurrentBundles().isEmpty());
    }

}
