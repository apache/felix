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
package org.apache.felix.deploymentadmin;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;

/**
 * Test cases for {@link DeploymentAdminConfig}.
 */
public class DeploymentAdminConfigTest extends TestCase {
    private static final String KEY_STOP_UNAFFECTED_BUNDLE = DeploymentAdminConfig.KEY_STOP_UNAFFECTED_BUNDLE;
    private static final String KEY_STOP_UNAFFECTED_BUNDLES = DeploymentAdminConfig.KEY_STOP_UNAFFECTED_BUNDLES;
    private static final String KEY_ALLOW_FOREIGN_CUSTOMIZERS = DeploymentAdminConfig.KEY_ALLOW_FOREIGN_CUSTOMIZERS;

    private static final boolean DEFAULT_STOP_UNAFFECTED_BUNDLES = DeploymentAdminConfig.DEFAULT_STOP_UNAFFECTED_BUNDLES;
    private static final boolean DEFAULT_ALLOW_FOREIGN_CUSTOMIZERS = DeploymentAdminConfig.DEFAULT_ALLOW_FOREIGN_CUSTOMIZERS;

    private final Map m_fwProperties = new HashMap();

    /**
     * Tests the configuration values of {@link DeploymentAdminImpl} without any explicit configuration.
     */
    public void testDefaultConfigurationOk() throws ConfigurationException {
        DeploymentAdminConfig config = createDeploymentAdminConfig();

        assertEquals(DEFAULT_STOP_UNAFFECTED_BUNDLES, config.isStopUnaffectedBundles());
        assertEquals(DEFAULT_ALLOW_FOREIGN_CUSTOMIZERS, config.isAllowForeignCustomizers());
    }

    /**
     * Tests the configuration values of {@link DeploymentAdminImpl} without any explicit configuration.
     */
    public void testFrameworkConfigurationOk() throws ConfigurationException {
        m_fwProperties.put(KEY_STOP_UNAFFECTED_BUNDLES, Boolean.toString(!DEFAULT_STOP_UNAFFECTED_BUNDLES));
        m_fwProperties.put(KEY_ALLOW_FOREIGN_CUSTOMIZERS, Boolean.toString(!DEFAULT_ALLOW_FOREIGN_CUSTOMIZERS));

        DeploymentAdminConfig config = createDeploymentAdminConfig();

        assertEquals(!DEFAULT_STOP_UNAFFECTED_BUNDLES, config.isStopUnaffectedBundles());
        assertEquals(!DEFAULT_ALLOW_FOREIGN_CUSTOMIZERS, config.isAllowForeignCustomizers());
    }

    /**
     * Tests the configuration values of {@link DeploymentAdminImpl} without any explicit configuration.
     */
    public void testFrameworkConfigurationDeprecatedKeyOk() throws ConfigurationException {
        m_fwProperties.put(KEY_STOP_UNAFFECTED_BUNDLE, Boolean.toString(!DEFAULT_STOP_UNAFFECTED_BUNDLES));

        DeploymentAdminConfig config = createDeploymentAdminConfig();

        assertEquals(!DEFAULT_STOP_UNAFFECTED_BUNDLES, config.isStopUnaffectedBundles());
    }

    /**
     * Tests the configuration values of {@link DeploymentAdminImpl} without any explicit configuration.
     */
    public void testSystemConfigurationOk() throws ConfigurationException {
        String stopUnaffectedBundle = KEY_STOP_UNAFFECTED_BUNDLES;
        String allowForeignCustomizers = KEY_ALLOW_FOREIGN_CUSTOMIZERS;

        System.setProperty(stopUnaffectedBundle, Boolean.toString(!DEFAULT_STOP_UNAFFECTED_BUNDLES));
        System.setProperty(allowForeignCustomizers, Boolean.toString(!DEFAULT_ALLOW_FOREIGN_CUSTOMIZERS));

        try {
            DeploymentAdminConfig config = createDeploymentAdminConfig();

            assertEquals(!DEFAULT_STOP_UNAFFECTED_BUNDLES, config.isStopUnaffectedBundles());
            assertEquals(!DEFAULT_ALLOW_FOREIGN_CUSTOMIZERS, config.isAllowForeignCustomizers());
        }
        finally {
            System.clearProperty(stopUnaffectedBundle);
            System.clearProperty(allowForeignCustomizers);
        }

        System.setProperty(stopUnaffectedBundle.toLowerCase(), Boolean.toString(!DEFAULT_STOP_UNAFFECTED_BUNDLES));
        System.setProperty(allowForeignCustomizers.toLowerCase(), Boolean.toString(!DEFAULT_ALLOW_FOREIGN_CUSTOMIZERS));

        try {
            DeploymentAdminConfig config = createDeploymentAdminConfig();

            assertEquals(!DEFAULT_STOP_UNAFFECTED_BUNDLES, config.isStopUnaffectedBundles());
            assertEquals(!DEFAULT_ALLOW_FOREIGN_CUSTOMIZERS, config.isAllowForeignCustomizers());
        }
        finally {
            System.clearProperty(stopUnaffectedBundle.toLowerCase());
            System.clearProperty(allowForeignCustomizers.toLowerCase());
        }
    }

    protected void setUp() throws Exception {
        m_fwProperties.clear();
    }

    private DeploymentAdminConfig createDeploymentAdminConfig() throws ConfigurationException {
        return new DeploymentAdminConfig(createMockBundleContext());
    }

    private BundleContext createMockBundleContext() {
        BundleContext result = (BundleContext) Mockito.mock(BundleContext.class);
        Mockito.when(result.getProperty(Matchers.anyString())).thenAnswer(new Answer() {
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String prop = (String) invocation.getArguments()[0];

                Object result = m_fwProperties.get(prop);
                if (result == null) {
                    result = System.getProperty(prop);
                }
                return result;
            }
        });
        return result;
    }
}
