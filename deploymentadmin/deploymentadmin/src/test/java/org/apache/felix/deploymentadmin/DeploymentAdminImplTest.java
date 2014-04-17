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
 * Test cases for {@link DeploymentAdminImpl}.
 */
public class DeploymentAdminImplTest extends TestCase
{
    private static final String CONF_KEY = DeploymentAdminImpl.KEY_STOP_UNAFFECTED_BUNDLE;
    private static final String SYS_PROP = DeploymentAdminImpl.PID + "." + CONF_KEY.toLowerCase();
    private static final String SYS_PROP_LOWERCASE = DeploymentAdminImpl.PID + "." + CONF_KEY.toLowerCase();

    private final Map m_fwProperties = new HashMap();

    /**
     * Tests the configuration values of {@link DeploymentAdminImpl} without any explicit configuration.
     */
    public void testDefaultConfigurationOk()
    {
        DeploymentAdminImpl da = createDeploymentAdmin();

        assertTrue(da.isStopUnaffectedBundles());
    }

    /**
     * Tests the configuration values of {@link DeploymentAdminImpl} without any explicit configuration.
     */
    public void testExplicitConfigurationOk() throws ConfigurationException
    {
        Dictionary dict = new Hashtable();
        dict.put(CONF_KEY, "false");

        DeploymentAdminImpl da = createDeploymentAdmin();
        da.updated(dict);

        // Should use the explicit configured value...
        assertFalse(da.isStopUnaffectedBundles());

        da.updated(null);

        // Should use the system wide value...
        assertTrue(da.isStopUnaffectedBundles());
    }

    /**
     * Tests that an explicit configuration cannot miss any properties. 
     */
    public void testExplicitConfigurationWithMissingValueFail() throws ConfigurationException
    {
        Dictionary dict = new Hashtable();

        DeploymentAdminImpl da = createDeploymentAdmin();
        try
        {
            da.updated(dict);
            fail("ConfigurationException expected!");
        }
        catch (ConfigurationException e)
        {
            assertEquals(CONF_KEY, e.getProperty());
        }
    }

    /**
     * Tests the configuration values of {@link DeploymentAdminImpl} without any explicit configuration.
     */
    public void testFrameworkConfigurationOk()
    {
        m_fwProperties.put(SYS_PROP, "false");

        DeploymentAdminImpl da = createDeploymentAdmin();

        assertFalse(da.isStopUnaffectedBundles());
    }

    /**
     * Tests the configuration values of {@link DeploymentAdminImpl} without any explicit configuration.
     */
    public void testSystemConfigurationOk()
    {
        System.setProperty(SYS_PROP, "false");

        try
        {
            DeploymentAdminImpl da = createDeploymentAdmin();

            assertFalse(da.isStopUnaffectedBundles());
        }
        finally
        {
            System.clearProperty(SYS_PROP);
        }

        System.setProperty(SYS_PROP_LOWERCASE, "false");

        try
        {
            DeploymentAdminImpl da = createDeploymentAdmin();

            assertFalse(da.isStopUnaffectedBundles());
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

    private DeploymentAdminImpl createDeploymentAdmin()
    {
        return new DeploymentAdminImpl(createMockBundleContext());
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
