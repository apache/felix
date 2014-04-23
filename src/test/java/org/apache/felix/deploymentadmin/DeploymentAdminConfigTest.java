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

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
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
public class DeploymentAdminConfigTest extends TestCase
{
    private static final String KEY_STOP_UNAFFECTED_BUNDLE = DeploymentAdminConfig.KEY_STOP_UNAFFECTED_BUNDLE;
    private static final String KEY_ALLOW_FOREIGN_CUSTOMIZERS = DeploymentAdminConfig.KEY_ALLOW_FOREIGN_CUSTOMIZERS;
    
    private static final boolean DEFAULT_STOP_UNAFFECTED_BUNDLE = DeploymentAdminConfig.DEFAULT_STOP_UNAFFECTED_BUNDLE;
    private static final boolean DEFAULT_ALLOW_FOREIGN_CUSTOMIZERS = DeploymentAdminConfig.DEFAULT_ALLOW_FOREIGN_CUSTOMIZERS;

    private static final String SYS_PROP = DeploymentAdminImpl.PID + "." + KEY_STOP_UNAFFECTED_BUNDLE.toLowerCase();
    private static final String SYS_PROP_LOWERCASE = DeploymentAdminImpl.PID + "." + KEY_STOP_UNAFFECTED_BUNDLE.toLowerCase();

    private final Map m_fwProperties = new HashMap();

    /**
     * Tests the configuration values of {@link DeploymentAdminImpl} without any explicit configuration.
     */
    public void testDefaultConfigurationOk() throws ConfigurationException
    {
        DeploymentAdminConfig config = createDeploymentAdminConfig(null);

        assertEquals(DEFAULT_STOP_UNAFFECTED_BUNDLE, config.isStopUnaffectedBundles());
        assertEquals(DEFAULT_ALLOW_FOREIGN_CUSTOMIZERS, config.isAllowForeignCustomizers());
    }

    /**
     * Tests the configuration values of {@link DeploymentAdminImpl} without any explicit configuration.
     */
    public void testExplicitConfigurationOk() throws ConfigurationException
    {
        Dictionary dict = new Hashtable();
        dict.put(KEY_STOP_UNAFFECTED_BUNDLE, "false");
        dict.put(KEY_ALLOW_FOREIGN_CUSTOMIZERS, "true");

        DeploymentAdminConfig config = createDeploymentAdminConfig(dict);

        // Should use the explicit configured value...
        assertFalse(config.isStopUnaffectedBundles());
        assertTrue(config.isAllowForeignCustomizers());
    }

    /**
     * Tests that an explicit configuration cannot miss any properties. 
     */
    public void testExplicitConfigurationWithMissingValueFail() throws ConfigurationException
    {
        Dictionary dict = new Hashtable();
        dict.put(KEY_ALLOW_FOREIGN_CUSTOMIZERS, "true");

        try
        {
            createDeploymentAdminConfig(dict);
            fail("ConfigurationException expected!");
        }
        catch (ConfigurationException e)
        {
            assertEquals(KEY_STOP_UNAFFECTED_BUNDLE, e.getProperty());
        }
        
        dict = new Hashtable();
        dict.put(KEY_STOP_UNAFFECTED_BUNDLE, "true");

        try
        {
            createDeploymentAdminConfig(dict);
            fail("ConfigurationException expected!");
        }
        catch (ConfigurationException e)
        {
            assertEquals(KEY_ALLOW_FOREIGN_CUSTOMIZERS, e.getProperty());
        }
    }

    /**
     * Tests the configuration values of {@link DeploymentAdminImpl} without any explicit configuration.
     */
    public void testFrameworkConfigurationOk() throws ConfigurationException
    {
        m_fwProperties.put(SYS_PROP, "false");

        DeploymentAdminConfig config = createDeploymentAdminConfig(null);

        assertEquals(false, config.isStopUnaffectedBundles());
        assertEquals(DEFAULT_ALLOW_FOREIGN_CUSTOMIZERS, config.isAllowForeignCustomizers());
    }

    /**
     * Tests the configuration values of {@link DeploymentAdminImpl} without any explicit configuration.
     */
    public void testSystemConfigurationOk() throws ConfigurationException
    {
        System.setProperty(SYS_PROP, "false");

        try
        {
            DeploymentAdminConfig config = createDeploymentAdminConfig(null);

            assertFalse(config.isStopUnaffectedBundles());
        }
        finally
        {
            System.clearProperty(SYS_PROP);
        }

        System.setProperty(SYS_PROP_LOWERCASE, "false");

        try
        {
            DeploymentAdminConfig config = createDeploymentAdminConfig(null);

            assertFalse(config.isStopUnaffectedBundles());
        }
        finally
        {
            System.clearProperty(SYS_PROP_LOWERCASE);
        }
    }

    protected void setUp() throws Exception
    {
        m_fwProperties.clear();
    }

    private DeploymentAdminConfig createDeploymentAdminConfig(Dictionary dict) throws ConfigurationException
    {
        return new DeploymentAdminConfig(createMockBundleContext(), dict);
    }

    private BundleContext createMockBundleContext()
    {
        BundleContext result = (BundleContext) Mockito.mock(BundleContext.class);
        Mockito.when(result.getProperty(Matchers.anyString())).thenAnswer(new Answer()
        {
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                String prop = (String) invocation.getArguments()[0];

                Object result = m_fwProperties.get(prop);
                if (result == null)
                {
                    result = System.getProperty(prop);
                }
                return result;
            }
        });
        return result;
    }
}
