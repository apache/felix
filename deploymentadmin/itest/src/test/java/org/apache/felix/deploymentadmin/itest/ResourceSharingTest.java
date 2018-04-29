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

import org.apache.felix.deploymentadmin.itest.util.DeploymentPackageBuilder;
import org.apache.felix.deploymentadmin.itest.util.DeploymentPackageBuilder.JarManifestManipulatingFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.osgi.framework.Bundle;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;

/**
 * Generic tests for {@link DeploymentAdmin}.
 */
@RunWith(PaxExam.class)
public class ResourceSharingTest extends BaseIntegrationTest {

    @Test
    public void testBundleCanBelongToOneDeploymentPackageOnly() throws Exception {
        DeploymentPackageBuilder dpBuilder1 = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder1
            .add(dpBuilder1.createBundleResource()
<<<<<<< HEAD
                .setUrl(getTestBundle("bundle1"))
            )
            .add(dpBuilder1.createBundleResource()
                .setUrl(getTestBundle("bundle2"))
=======
                .setUrl(getTestBundleURL("bundle1"))
            )
            .add(dpBuilder1.createBundleResource()
                .setUrl(getTestBundleURL("bundle2"))
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            );

        DeploymentPackageBuilder dpBuilder2 = createNewDeploymentPackageBuilder("0.8.0");
        dpBuilder2
            .add(dpBuilder2.createBundleResource()
<<<<<<< HEAD
                .setUrl(getTestBundle("bundle1"))
            );

        DeploymentPackage dp1 = m_deploymentAdmin.installDeploymentPackage(dpBuilder1.generate());
=======
                .setUrl(getTestBundleURL("bundle1"))
            );

        DeploymentPackage dp1 = installDeploymentPackage(dpBuilder1);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        assertNotNull("No deployment package returned?!", dp1);

        awaitRefreshPackagesEvent();

        try {
            // should fail: valid-bundle1 belongs to another DP...
<<<<<<< HEAD
            m_deploymentAdmin.installDeploymentPackage(dpBuilder2.generate());
=======
            installDeploymentPackage(dpBuilder2);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            fail("Expected a DeploymentException with code " + DeploymentException.CODE_BUNDLE_SHARING_VIOLATION);
        }
        catch (DeploymentException exception) {
            // Ok; expected...
            assertEquals(DeploymentException.CODE_BUNDLE_SHARING_VIOLATION, exception.getCode());
        }
    }

    @Test
    public void testBundleCannotBeSharedWithNonDeploymentPackagedBundle() throws Exception {
        // Manually install a bundle...
<<<<<<< HEAD
        Bundle result = m_context.installBundle(getTestBundle("bundle1").toExternalForm());
=======
        Bundle result = m_context.installBundle(getTestBundleURL("bundle1").toExternalForm());
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        assertNotNull(result);
        
        assertBundleExists(getSymbolicName("bundle1"), "1.0.0");
        
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource()
<<<<<<< HEAD
                .setUrl(getTestBundle("bundle1"))
            )
            .add(dpBuilder.createBundleResource()
                .setUrl(getTestBundle("bundle2"))
=======
                .setUrl(getTestBundleURL("bundle1"))
            )
            .add(dpBuilder.createBundleResource()
                .setUrl(getTestBundleURL("bundle2"))
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            );

        try {
            // should fail: valid-bundle1 is installed, but does not belong to any DP...
<<<<<<< HEAD
            m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
=======
            installDeploymentPackage(dpBuilder);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            fail("Expected a DeploymentException with code " + DeploymentException.CODE_BUNDLE_SHARING_VIOLATION);
        }
        catch (DeploymentException exception) {
            // Ok; expected...
            assertEquals(DeploymentException.CODE_BUNDLE_SHARING_VIOLATION, exception.getCode());
        }
    }

    @Test
    public void testForeignBundleCanCoexistWithPackagedBundleIfVersionsDiffer() throws Exception {
        // Manually install a bundle...
<<<<<<< HEAD
        Bundle result = m_context.installBundle(getTestBundle("bundle1").toExternalForm());
=======
        Bundle result = m_context.installBundle(getTestBundleURL("bundle1").toExternalForm());
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        assertNotNull(result);

        long bundleId = result.getBundleId();

        assertBundleExists(getSymbolicName("bundle1"), "1.0.0");
        assertTrue(isBundleInstalled(result));

        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource()
                .setVersion("1.1.0")
<<<<<<< HEAD
                .setUrl(getTestBundle("bundle1"))
                .setFilter(new JarManifestManipulatingFilter("Bundle-Version", "1.1.0"))
            )
            .add(dpBuilder.createBundleResource()
                .setUrl(getTestBundle("bundle2"))
            );
        
        // should succeed: valid-bundle1 is installed, but has a different version than the one in our DP...
        DeploymentPackage dp = m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
=======
                .setUrl(getTestBundleURL("bundle1"))
                .setFilter(new JarManifestManipulatingFilter("Bundle-Version", "1.1.0"))
            )
            .add(dpBuilder.createBundleResource()
                .setUrl(getTestBundleURL("bundle2"))
            );
        
        // should succeed: valid-bundle1 is installed, but has a different version than the one in our DP...
        DeploymentPackage dp = installDeploymentPackage(dpBuilder);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        awaitRefreshPackagesEvent();

        assertBundleExists(getSymbolicName("bundle1"), "1.0.0");
        assertBundleExists(getSymbolicName("bundle1"), "1.1.0");
        
        // The manually installed bundle should still be in the installed or resolved state...
        assertTrue(isBundleInstalled(m_context.getBundle(bundleId)) || isBundleResolved(m_context.getBundle(bundleId)));
        assertTrue(isBundleActive(dp.getBundle(getSymbolicName("bundle1"))));
    }
}
