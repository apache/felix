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
package org.apache.felix.fileinstall.internal;

import java.io.File;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IArgumentMatcher;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Tests for ConfigInstaller
 */
public class ConfigInstallerTest extends TestCase {

    BundleContext mockBundleContext;
    Bundle mockBundle;
    ConfigurationAdmin mockConfigurationAdmin;
    Configuration mockConfiguration;

    protected void setUp() throws Exception
    {
        super.setUp();
        mockBundleContext = EasyMock.createMock(BundleContext.class);
        mockBundle = EasyMock.createMock(Bundle.class);
        mockConfigurationAdmin = EasyMock.createMock(ConfigurationAdmin.class);
        mockConfiguration = EasyMock.createMock(Configuration.class);
    }


    public void testParsePidWithoutFactoryPid()
    {
        ConfigInstaller ci = new ConfigInstaller(null, null, null);

        String path = "pid.cfg";
        assertEquals( "Pid without Factory Pid calculated", "pid", ci.parsePid( path )[0] );
        assertEquals( "Pid without Factory Pid calculated", null, ci.parsePid( path )[1] );
    }


    public void testParsePidWithFactoryPid()
    {
        ConfigInstaller ci = new ConfigInstaller(null, null, null);

        String path = "factory-pid.cfg";
        assertEquals( "Pid with Factory Pid calculated", "factory", ci.parsePid( path )[0] );
        assertEquals( "Pid with Factory Pid calculated", "pid", ci.parsePid( path )[1] );
    }

    public void testGetNewFactoryConfiguration() throws Exception
    {
        EasyMock.expect(mockConfigurationAdmin.listConfigurations((String) EasyMock.anyObject()))
                    .andReturn(null);
        EasyMock.expect(mockConfigurationAdmin.createFactoryConfiguration( "pid", null ))
                    .andReturn(mockConfiguration);
        EasyMock.replay(mockConfiguration, mockConfigurationAdmin, mockBundleContext);

        ConfigInstaller ci = new ConfigInstaller( mockBundleContext, mockConfigurationAdmin, new FileInstall() );

        assertEquals( "Factory configuration retrieved", mockConfiguration, ci.getConfiguration( "pid-factoryPid.cfg", "pid", "factoryPid" ) );

        EasyMock.verify(mockConfiguration, mockConfigurationAdmin, mockBundleContext);
    }


    public void testGetExistentFactoryConfiguration() throws Exception
    {
        EasyMock.expect(mockConfigurationAdmin.listConfigurations((String) EasyMock.anyObject()))
                        .andReturn(null);
        EasyMock.expect(mockConfigurationAdmin.createFactoryConfiguration( "pid", null ))
                        .andReturn(mockConfiguration);
        EasyMock.replay(mockConfiguration, mockConfigurationAdmin, mockBundleContext);

        ConfigInstaller ci = new ConfigInstaller( mockBundleContext, mockConfigurationAdmin, new FileInstall() );

        assertEquals( "Factory configuration retrieved", mockConfiguration, ci.getConfiguration( "pid-factoryPid.cfg","pid", "factoryPid" ) );

        EasyMock.verify(mockConfiguration, mockConfigurationAdmin, mockBundleContext);
    }


    public void testGetExistentNoFactoryConfiguration() throws Exception
    {
        EasyMock.expect(mockConfigurationAdmin.listConfigurations((String) EasyMock.anyObject()))
                        .andReturn(null);
        EasyMock.expect(mockConfigurationAdmin.getConfiguration( "pid", null ))
                        .andReturn(mockConfiguration);
        EasyMock.replay(mockConfiguration, mockConfigurationAdmin, mockBundleContext);

        ConfigInstaller ci = new ConfigInstaller( mockBundleContext, mockConfigurationAdmin, new FileInstall() );

        assertEquals( "Factory configuration retrieved", mockConfiguration, ci.getConfiguration( "pid.cfg", "pid", null ) );

        EasyMock.verify(mockConfiguration, mockConfigurationAdmin, mockBundleContext);
    }


    public void testDeleteConfig() throws Exception
    {
        mockConfiguration.delete();
        EasyMock.expect(mockBundleContext.getProperty(DirectoryWatcher.LOG_DEFAULT)).andReturn(null);
        EasyMock.expect(mockBundleContext.getProperty(DirectoryWatcher.LOG_LEVEL)).andReturn(null);
        EasyMock.expect(mockConfigurationAdmin.listConfigurations((String) EasyMock.anyObject()))
                        .andReturn(null);
        EasyMock.expect(mockConfigurationAdmin.getConfiguration("pid", null ))
                        .andReturn(mockConfiguration);
        EasyMock.replay(mockConfiguration, mockConfigurationAdmin, mockBundleContext);

        ConfigInstaller ci = new ConfigInstaller( mockBundleContext, mockConfigurationAdmin, new FileInstall() );

        assertTrue( ci.deleteConfig( new File( "pid.cfg" ) ) );

        EasyMock.verify(mockConfiguration, mockConfigurationAdmin, mockBundleContext);
    }


    public void testSetConfiguration() throws Exception
    {
        EasyMock.expect(mockBundleContext.getProperty(DirectoryWatcher.LOG_DEFAULT)).andReturn(null);
        EasyMock.expect(mockBundleContext.getProperty(DirectoryWatcher.LOG_LEVEL)).andReturn(null);
        EasyMock.expect(mockConfiguration.getProperties()).andReturn(new Hashtable<String, Object>());
        EasyMock.reportMatcher(new IArgumentMatcher()
        {
            public boolean matches( Object argument )
            {
                return ((Dictionary) argument).get("testkey").equals("testvalue");
            }

            public void appendTo(StringBuffer buffer)
            {
                buffer.append("<Dictionary check: testkey present?>");
            }
        } );
        mockConfiguration.update(new Hashtable<String, Object>());
        EasyMock.expect(mockConfigurationAdmin.listConfigurations((String) EasyMock.anyObject()))
                        .andReturn(null);
        EasyMock.expect(mockConfigurationAdmin.getConfiguration("firstcfg", null))
                        .andReturn(mockConfiguration);
        EasyMock.replay(mockConfiguration, mockConfigurationAdmin, mockBundleContext);

        ConfigInstaller ci = new ConfigInstaller( mockBundleContext, mockConfigurationAdmin, new FileInstall() );

        assertTrue( ci.setConfig( new File( "src/test/resources/watched/firstcfg.cfg" ) ) );

        EasyMock.verify(mockConfiguration, mockConfigurationAdmin, mockBundleContext);
    }
    
    public void testShouldSaveConfig() 
    {
        final AtomicReference<Boolean> disable = new AtomicReference<Boolean>();
        final AtomicReference<Boolean> enable = new AtomicReference<Boolean>();
        
        EasyMock.expect(mockBundleContext.getProperty(DirectoryWatcher.DISABLE_CONFIG_SAVE)).andAnswer(
                new IAnswer<String>() {
                    public String answer() throws Throwable {
                        return disable.get() != null ? disable.get().toString() : null;
                    }
                }
        ).anyTimes();
        EasyMock.expect(mockBundleContext.getProperty(DirectoryWatcher.ENABLE_CONFIG_SAVE)).andAnswer(
                new IAnswer<String>() {
                    public String answer() throws Throwable {
                        return enable.get() != null ? enable.get().toString() : null;
                    }
                }
        ).anyTimes();
        EasyMock.replay(mockConfiguration, mockConfigurationAdmin, mockBundleContext);

        ConfigInstaller ci = new ConfigInstaller( mockBundleContext, mockConfigurationAdmin, new FileInstall() );

        disable.set(null);
        enable.set(null);
        assertTrue( ci.shouldSaveConfig() );

        disable.set(Boolean.FALSE);
        enable.set(null);
        assertFalse( ci.shouldSaveConfig() );

        disable.set(Boolean.TRUE);
        enable.set(null);
        assertTrue( ci.shouldSaveConfig() );

        disable.set(null);
        enable.set(Boolean.FALSE);
        assertFalse( ci.shouldSaveConfig() );

        disable.set(Boolean.FALSE);
        enable.set(Boolean.FALSE);
        assertFalse( ci.shouldSaveConfig() );

        disable.set(Boolean.TRUE);
        enable.set(Boolean.FALSE);
        assertFalse( ci.shouldSaveConfig() );

        disable.set(null);
        enable.set(Boolean.TRUE);
        assertTrue( ci.shouldSaveConfig() );

        disable.set(Boolean.FALSE);
        enable.set(Boolean.TRUE);
        assertTrue( ci.shouldSaveConfig() );

        disable.set(Boolean.TRUE);
        enable.set(Boolean.TRUE);
        assertTrue( ci.shouldSaveConfig() );

        EasyMock.verify(mockConfiguration, mockConfigurationAdmin, mockBundleContext);
    }

}
