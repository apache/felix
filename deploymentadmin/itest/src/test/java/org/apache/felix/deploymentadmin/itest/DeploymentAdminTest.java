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

import static org.osgi.service.deploymentadmin.DeploymentException.CODE_BUNDLE_NAME_ERROR;
import static org.osgi.service.deploymentadmin.DeploymentException.CODE_OTHER_ERROR;


import org.apache.felix.deploymentadmin.itest.util.DeploymentPackageBuilder;
import org.apache.felix.deploymentadmin.itest.util.DeploymentPackageBuilder.JarManifestManipulatingFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;

/**
 * Generic tests for {@link DeploymentAdmin}.
 */
@RunWith(PaxExam.class)
public class DeploymentAdminTest extends BaseIntegrationTest
{
    /**
     * Tests that we can update the configuration of {@link DeploymentAdmin} at runtime. Based on the test case for FELIX-4184, see 
     * {@link org.apache.felix.deploymentadmin.itest.InstallFixPackageTest#testInstallAndUpdateImplementationBundleWithSeparateAPIBundle_FELIX4184()}
     */
    @Test
    public void testUpdateConfigurationOk() throws Exception
    {
        System.setProperty("org.apache.felix.deploymentadmin.stopUnaffectedBundles", "false");
        System.setProperty("org.apache.felix.deploymentadmin.allowForeignCustomizers", "false");

        // This test case will only work if stopUnaffectedBundle is set to 'false'...
        try
        {
            // first, install a deployment package with implementation and api bundles in version 1.0.0
            DeploymentPackageBuilder dpBuilder = createDeploymentPackageBuilder("a", "1.0.0");
            dpBuilder.add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundleimpl1", "bundleimpl1", "1.0.0")));
            dpBuilder.add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundleapi1", "bundleapi1", "1.0.0")));

            DeploymentPackage dp1 = installDeploymentPackage(dpBuilder);
            assertNotNull("No deployment package returned?!", dp1);

            assertEquals("Expected a single deployment package?!", 1, countDeploymentPackages());

            // then, install a fix package with implementation and api bundles in version 2.0.0
            dpBuilder = createDeploymentPackageBuilder("a", "2.0.0").setFixPackage("[1.0.0,2.0.0]");
            dpBuilder.add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundleimpl2", "bundleimpl2", "2.0.0")));
            dpBuilder.add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundleapi2", "bundleapi2", "2.0.0")));

            DeploymentPackage dp2 = installDeploymentPackage(dpBuilder);
            assertNotNull("No deployment package returned?!", dp2);

            awaitRefreshPackagesEvent();

            assertBundleExists(getSymbolicName("bundleimpl"), "2.0.0");
            assertBundleExists(getSymbolicName("bundleapi"), "2.0.0");
            assertBundleNotExists(getSymbolicName("bundleimpl"), "1.0.0");
            assertBundleNotExists(getSymbolicName("bundleapi"), "1.0.0");
        }
        finally
        {
            System.clearProperty("org.apache.felix.deploymentadmin.stopUnaffectedBundles");
            System.clearProperty("org.apache.felix.deploymentadmin.allowForeignCustomizers");
        }
    }

    @Test
    public void testBundleSymbolicNameMustMatchManifestEntry() throws Exception
    {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder.add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle1"))).add(
            dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle2")).setFilter(new JarManifestManipulatingFilter("Bundle-SymbolicName", "foo")));

        try
        {
            installDeploymentPackage(dpBuilder);
            fail("Succeeded into installing a bundle with a fake symbolic name?!");
        }
        catch (DeploymentException exception)
        {
            // Ok; expected...
            assertDeploymentException(CODE_BUNDLE_NAME_ERROR, exception);
        }
    }

    @Test
    public void testBundleVersionMustMatchManifestEntry() throws Exception
    {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder.add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle1"))).add(
            dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle2")).setFilter(new JarManifestManipulatingFilter("Bundle-Version", "1.1.0")));

        try
        {
            installDeploymentPackage(dpBuilder);
            fail("Succeeded into installing a bundle with a fake version?!");
        }
        catch (DeploymentException exception)
        {
            // Ok; expected...
            assertDeploymentException(CODE_OTHER_ERROR, exception);
        }
    }

    @Test
    public void testManifestEntryMustMatchBundleSymbolicName() throws Exception
    {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder.add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle1"))).add(dpBuilder.createBundleResource().setSymbolicName("foo").setUrl(getTestBundleURL("bundle2")));

        try
        {
            installDeploymentPackage(dpBuilder);
            fail("Succeeded into installing a bundle with a fake symbolic name?!");
        }
        catch (DeploymentException exception)
        {
            // Ok; expected...
            assertDeploymentException(CODE_BUNDLE_NAME_ERROR, exception);
        }
    }

    @Test
    public void testManifestEntryMustMatchBundleVersion() throws Exception
    {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder.add(dpBuilder.createBundleResource().setUrl(getTestBundleURL("bundle1"))).add(dpBuilder.createBundleResource().setVersion("1.1.0").setUrl(getTestBundleURL("bundle2")));

        try
        {
            installDeploymentPackage(dpBuilder);
            fail("Succeeded into installing a bundle with a fake version?!");
        }
        catch (DeploymentException exception)
        {
            // Ok; expected...
            assertDeploymentException(CODE_OTHER_ERROR, exception);
        }
    }
}
