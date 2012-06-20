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
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;

/**
 * Generic tests for {@link DeploymentAdmin}.
 */
@RunWith(JUnit4TestRunner.class)
public class DeploymentAdminTest extends BaseIntegrationTest {

    @Test
    public void testBundleSymbolicNameMustMatchManifestEntry() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource()
                .setUrl(getTestBundle("bundle1"))
            )
            .add(dpBuilder.createBundleResource()
                .setUrl(getTestBundle("bundle2"))
                .setFilter(new JarManifestManipulatingFilter("Bundle-SymbolicName", "foo"))
            );
        
        try {
            m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
            fail("Succeeded into installing a bundle with a fake symbolic name?!");
        }
        catch (DeploymentException exception) {
            // Ok; expected...
            assertDeploymentException(CODE_BUNDLE_NAME_ERROR, exception);
        }
    }

    @Test
    public void testBundleVersionMustMatchManifestEntry() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource()
                .setUrl(getTestBundle("bundle1"))
            )
            .add(dpBuilder.createBundleResource()
                .setUrl(getTestBundle("bundle2"))
                .setFilter(new JarManifestManipulatingFilter("Bundle-Version", "1.1.0"))
            );
        
        try {
            m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
            fail("Succeeded into installing a bundle with a fake version?!");
        }
        catch (DeploymentException exception) {
            // Ok; expected...
            assertDeploymentException(CODE_OTHER_ERROR, exception);
        }
    }

    @Test
    public void testManifestEntryMustMatchBundleSymbolicName() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource()
                .setUrl(getTestBundle("bundle1"))
            )
            .add(dpBuilder.createBundleResource()
                .setSymbolicName("foo")
                .setUrl(getTestBundle("bundle2"))
            );
        
        try {
            m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
            fail("Succeeded into installing a bundle with a fake symbolic name?!");
        }
        catch (DeploymentException exception) {
            // Ok; expected...
            assertDeploymentException(CODE_BUNDLE_NAME_ERROR, exception);
        }
    }

    @Test
    public void testManifestEntryMustMatchBundleVersion() throws Exception {
        DeploymentPackageBuilder dpBuilder = createNewDeploymentPackageBuilder("1.0.0");
        dpBuilder
            .add(dpBuilder.createBundleResource()
                .setUrl(getTestBundle("bundle1"))
            )
            .add(dpBuilder.createBundleResource()
                .setVersion("1.1.0")
                .setUrl(getTestBundle("bundle2"))
            );
        
        try {
            m_deploymentAdmin.installDeploymentPackage(dpBuilder.generate());
            fail("Succeeded into installing a bundle with a fake version?!");
        }
        catch (DeploymentException exception) {
            // Ok; expected...
            assertDeploymentException(CODE_OTHER_ERROR, exception);
        }
    }
}
