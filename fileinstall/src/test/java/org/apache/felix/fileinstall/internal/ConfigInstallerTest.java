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
<<<<<<< HEAD
=======
import java.io.FileOutputStream;
import java.io.OutputStream;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.TestCase;
<<<<<<< HEAD
=======
import org.easymock.Capture;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IArgumentMatcher;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
<<<<<<< HEAD
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
=======
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

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
<<<<<<< HEAD
        mockBundleContext = (BundleContext) EasyMock.createMock(BundleContext.class);
        mockBundle = ( Bundle ) EasyMock.createMock(Bundle.class);
        mockConfigurationAdmin = ( ConfigurationAdmin ) EasyMock.createMock(ConfigurationAdmin.class);
        mockConfiguration = ( Configuration ) EasyMock.createMock(Configuration.class);
=======
        mockBundleContext = EasyMock.createMock(BundleContext.class);
        mockBundle = EasyMock.createMock(Bundle.class);
        mockConfigurationAdmin = EasyMock.createMock(ConfigurationAdmin.class);
        mockConfiguration = EasyMock.createMock(Configuration.class);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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

<<<<<<< HEAD
=======
    public void testTypedConfiguration() throws Exception
    {
        File file = File.createTempFile("test", ".config");
        try (OutputStream os = new FileOutputStream(file)) {
            os.write("networkInterface=\"wlp3s0\"\n".getBytes("UTF-8"));
        }
        String pid = file.getName().substring(0, file.getName().indexOf(".config"));

        Capture<Dictionary<String, Object>> props = new Capture<>();
        EasyMock.expect(mockConfigurationAdmin.listConfigurations((String) EasyMock.anyObject()))
                .andReturn(null);
        EasyMock.expect(mockConfigurationAdmin.getConfiguration(pid, "?"))
                .andReturn(mockConfiguration);
        EasyMock.expect(mockConfiguration.getProperties())
                .andReturn(null);
        EasyMock.expect(mockBundleContext.getProperty((String) EasyMock.anyObject()))
                .andReturn(null)
                .anyTimes();
        mockConfiguration.update(EasyMock.capture(props));
        EasyMock.expectLastCall();
        EasyMock.replay(mockConfiguration, mockConfigurationAdmin, mockBundleContext);

        ConfigInstaller ci = new ConfigInstaller( mockBundleContext, mockConfigurationAdmin, new FileInstall() );
        ci.install(file);

        EasyMock.verify(mockConfiguration, mockConfigurationAdmin, mockBundleContext);
        Dictionary<String, Object> loaded = props.getValue();
        assertNotNull(loaded);
        assertEquals("wlp3s0", loaded.get("networkInterface"));
    }

    public void testTypedConfigurationFloat() throws Exception
    {
        File file = File.createTempFile("test", ".config");
        try (OutputStream os = new FileOutputStream(file)) {
            os.write("key=F\"1137191584\"\n".getBytes("UTF-8"));
        }
        String pid = file.getName().substring(0, file.getName().indexOf(".config"));

        Capture<Dictionary<String, Object>> props = new Capture<>();
        EasyMock.expect(mockConfigurationAdmin.listConfigurations((String) EasyMock.anyObject()))
                .andReturn(null);
        EasyMock.expect(mockConfigurationAdmin.getConfiguration(pid, "?"))
                .andReturn(mockConfiguration);
        EasyMock.expect(mockConfiguration.getProperties())
                .andReturn(null);
        EasyMock.expect(mockBundleContext.getProperty((String) EasyMock.anyObject()))
                .andReturn(null)
                .anyTimes();
        mockConfiguration.update(EasyMock.capture(props));
        EasyMock.expectLastCall();
        EasyMock.replay(mockConfiguration, mockConfigurationAdmin, mockBundleContext);

        ConfigInstaller ci = new ConfigInstaller( mockBundleContext, mockConfigurationAdmin, new FileInstall() );
        ci.install(file);

        EasyMock.verify(mockConfiguration, mockConfigurationAdmin, mockBundleContext);
        Dictionary<String, Object> loaded = props.getValue();
        assertNotNull(loaded);
        assertEquals(400.333F, loaded.get("key"));
    }

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public void testGetNewFactoryConfiguration() throws Exception
    {
        EasyMock.expect(mockConfigurationAdmin.listConfigurations((String) EasyMock.anyObject()))
                    .andReturn(null);
<<<<<<< HEAD
        EasyMock.expect(mockConfigurationAdmin.createFactoryConfiguration( "pid", null ))
                    .andReturn(mockConfiguration);
        EasyMock.replay(new Object[]{mockConfiguration, mockConfigurationAdmin, mockBundleContext});
=======
        EasyMock.expect(mockConfigurationAdmin.createFactoryConfiguration( "pid", "?" ))
                    .andReturn(mockConfiguration);
        EasyMock.replay(mockConfiguration, mockConfigurationAdmin, mockBundleContext);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        ConfigInstaller ci = new ConfigInstaller( mockBundleContext, mockConfigurationAdmin, new FileInstall() );

        assertEquals( "Factory configuration retrieved", mockConfiguration, ci.getConfiguration( "pid-factoryPid.cfg", "pid", "factoryPid" ) );

<<<<<<< HEAD
        EasyMock.verify(new Object[]{mockConfiguration, mockConfigurationAdmin, mockBundleContext});
=======
        EasyMock.verify(mockConfiguration, mockConfigurationAdmin, mockBundleContext);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }


    public void testGetExistentFactoryConfiguration() throws Exception
    {
        EasyMock.expect(mockConfigurationAdmin.listConfigurations((String) EasyMock.anyObject()))
                        .andReturn(null);
<<<<<<< HEAD
        EasyMock.expect(mockConfigurationAdmin.createFactoryConfiguration( "pid", null ))
                        .andReturn(mockConfiguration);
        EasyMock.replay(new Object[]{mockConfiguration, mockConfigurationAdmin, mockBundleContext});
=======
        EasyMock.expect(mockConfigurationAdmin.createFactoryConfiguration( "pid", "?" ))
                        .andReturn(mockConfiguration);
        EasyMock.replay(mockConfiguration, mockConfigurationAdmin, mockBundleContext);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        ConfigInstaller ci = new ConfigInstaller( mockBundleContext, mockConfigurationAdmin, new FileInstall() );

        assertEquals( "Factory configuration retrieved", mockConfiguration, ci.getConfiguration( "pid-factoryPid.cfg","pid", "factoryPid" ) );

<<<<<<< HEAD
        EasyMock.verify(new Object[]{mockConfiguration, mockConfigurationAdmin, mockBundleContext});
=======
        EasyMock.verify(mockConfiguration, mockConfigurationAdmin, mockBundleContext);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }


    public void testGetExistentNoFactoryConfiguration() throws Exception
    {
        EasyMock.expect(mockConfigurationAdmin.listConfigurations((String) EasyMock.anyObject()))
                        .andReturn(null);
<<<<<<< HEAD
        EasyMock.expect(mockConfigurationAdmin.getConfiguration( "pid", null ))
                        .andReturn(mockConfiguration);
        EasyMock.replay(new Object[]{mockConfiguration, mockConfigurationAdmin, mockBundleContext});
=======
        EasyMock.expect(mockConfigurationAdmin.getConfiguration( "pid", "?" ))
                        .andReturn(mockConfiguration);
        EasyMock.replay(mockConfiguration, mockConfigurationAdmin, mockBundleContext);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        ConfigInstaller ci = new ConfigInstaller( mockBundleContext, mockConfigurationAdmin, new FileInstall() );

        assertEquals( "Factory configuration retrieved", mockConfiguration, ci.getConfiguration( "pid.cfg", "pid", null ) );

<<<<<<< HEAD
        EasyMock.verify(new Object[]{mockConfiguration, mockConfigurationAdmin, mockBundleContext});
=======
        EasyMock.verify(mockConfiguration, mockConfigurationAdmin, mockBundleContext);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }


    public void testDeleteConfig() throws Exception
    {
        mockConfiguration.delete();
<<<<<<< HEAD
        EasyMock.expect(mockConfigurationAdmin.listConfigurations((String) EasyMock.anyObject()))
                        .andReturn(null);
        EasyMock.expect(mockConfigurationAdmin.getConfiguration("pid", null ))
                        .andReturn(mockConfiguration);
        EasyMock.replay(new Object[]{mockConfiguration, mockConfigurationAdmin, mockBundleContext});
=======
        EasyMock.expect(mockBundleContext.getProperty(DirectoryWatcher.LOG_DEFAULT)).andReturn(null);
        EasyMock.expect(mockBundleContext.getProperty(DirectoryWatcher.LOG_LEVEL)).andReturn(null);
        EasyMock.expect(mockConfigurationAdmin.listConfigurations((String) EasyMock.anyObject()))
                        .andReturn(null);
        EasyMock.expect(mockConfigurationAdmin.getConfiguration("pid", "?" ))
                        .andReturn(mockConfiguration);
        EasyMock.replay(mockConfiguration, mockConfigurationAdmin, mockBundleContext);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        ConfigInstaller ci = new ConfigInstaller( mockBundleContext, mockConfigurationAdmin, new FileInstall() );

        assertTrue( ci.deleteConfig( new File( "pid.cfg" ) ) );

<<<<<<< HEAD
        EasyMock.verify(new Object[]{mockConfiguration, mockConfigurationAdmin, mockBundleContext});
    }


    public void testSetConfiguration() throws Exception
    {
        EasyMock.expect(mockConfiguration.getBundleLocation()).andReturn(null);
        EasyMock.expect(mockConfiguration.getProperties()).andReturn(new Hashtable());
=======
        EasyMock.verify(mockConfiguration, mockConfigurationAdmin, mockBundleContext);
    }

    public void testCreateConfigAndObserveCMDeleted() throws Exception
    {
        File file = File.createTempFile("test", ".config");
        try (OutputStream os = new FileOutputStream(file)) {
            os.write("key=F\"1137191584\"\n".getBytes("UTF-8"));
        }
        String pid = file.getName().substring(0, file.getName().indexOf(".config"));

        final Capture<Dictionary<String, Object>> props = new Capture<>();

        EasyMock.expect(mockConfiguration.getProperties())
                .andReturn(null);
        EasyMock.expect(mockBundleContext.getProperty((String) EasyMock.anyObject()))
                .andReturn(null)
                .anyTimes();
        EasyMock.expect(mockConfigurationAdmin.listConfigurations((String) EasyMock.anyObject()))
                .andReturn(null);
        EasyMock.expect(mockConfigurationAdmin.getConfiguration(pid, "?"))
                .andReturn(mockConfiguration);

        ServiceReference<ConfigurationAdmin> sr = EasyMock.createMock(ServiceReference.class);
        mockConfiguration.update(EasyMock.capture(props));
        EasyMock.expectLastCall();

        EasyMock.expect(mockConfigurationAdmin.getConfiguration(pid, "?"))
                .andReturn(mockConfiguration);
        EasyMock.expect(mockConfiguration.getProperties())
                .andAnswer(new IAnswer<Dictionary<String, Object>>() {
                    @Override
                    public Dictionary<String, Object> answer() throws Throwable {
                        return props.getValue();
                    }
                });
        EasyMock.expect(mockConfiguration.getPid())
                .andReturn(pid);

        EasyMock.replay(mockConfiguration, mockConfigurationAdmin, mockBundleContext, sr);

        ConfigInstaller ci = new ConfigInstaller( mockBundleContext, mockConfigurationAdmin, new FileInstall() );
        ci.install(file);

        ci.doConfigurationEvent( new ConfigurationEvent(sr , ConfigurationEvent.CM_UPDATED, null, pid ) );

        ci.doConfigurationEvent( new ConfigurationEvent(sr , ConfigurationEvent.CM_DELETED, null, pid ) );

        assertFalse("Configuration file should be deleted", file.isFile());
    }

    public void testUseExistingConfigAndObserveCMDeleted() throws Exception
    {
        String pid = "test";

        Capture<Dictionary<String, Object>> props = new Capture<>();

        EasyMock.expect(mockConfiguration.getProperties())
                .andReturn(null);
        EasyMock.expect(mockBundleContext.getProperty((String) EasyMock.anyObject()))
                .andReturn(null)
                .anyTimes();
        EasyMock.expect(mockConfigurationAdmin.listConfigurations((String) EasyMock.anyObject()))
                .andReturn(null);
        EasyMock.expect(mockConfigurationAdmin.getConfiguration(pid, "?"))
                .andReturn(mockConfiguration);

        ServiceReference<ConfigurationAdmin> sr = EasyMock.createMock(ServiceReference.class);
        mockConfiguration.update(EasyMock.capture(props));
        EasyMock.expectLastCall();

        EasyMock.replay(mockConfiguration, mockConfigurationAdmin, mockBundleContext, sr);

        ConfigInstaller ci = new ConfigInstaller( mockBundleContext, mockConfigurationAdmin, new FileInstall() );

        ci.doConfigurationEvent( new ConfigurationEvent(sr , ConfigurationEvent.CM_UPDATED, null, pid ) );
        ci.doConfigurationEvent( new ConfigurationEvent(sr , ConfigurationEvent.CM_DELETED, null, pid ) );
    }

    public void testUseExistingConfigWithFileinstallFilenameAndObserveCMDeleted() throws Exception
    {
        File file = File.createTempFile("test", ".config");
        try (OutputStream os = new FileOutputStream(file)) {
            os.write("key=F\"1137191584\"\n".getBytes("UTF-8"));
        }
        String pid = "test";

        Capture<Dictionary<String, Object>> propsCapture = new Capture<>();
        Dictionary<String, Object> props = new Hashtable<>();
        props.put(DirectoryWatcher.FILENAME, file.toURI().toString());

        EasyMock.expect(mockConfiguration.getProperties())
                .andReturn(props);
        EasyMock.expect(mockBundleContext.getProperty((String) EasyMock.anyObject()))
                .andReturn(null)
                .anyTimes();
        EasyMock.expect(mockConfigurationAdmin.listConfigurations((String) EasyMock.anyObject()))
                .andReturn(null);
        EasyMock.expect(mockConfigurationAdmin.getConfiguration(pid, "?"))
                .andReturn(mockConfiguration);
        EasyMock.expect(mockConfiguration.getPid())
                .andReturn(pid);

        ServiceReference<ConfigurationAdmin> sr = EasyMock.createMock(ServiceReference.class);
        mockConfiguration.update(EasyMock.capture(propsCapture));
        EasyMock.expectLastCall();

        EasyMock.replay(mockConfiguration, mockConfigurationAdmin, mockBundleContext, sr);

        ConfigInstaller ci = new ConfigInstaller( mockBundleContext, mockConfigurationAdmin, new FileInstall() );

        ci.doConfigurationEvent( new ConfigurationEvent(sr , ConfigurationEvent.CM_UPDATED, null, pid ) );
        ci.doConfigurationEvent( new ConfigurationEvent(sr , ConfigurationEvent.CM_DELETED, null, pid ) );

        assertFalse("Configuration file should be deleted", file.isFile());
    }

    public void testSetConfiguration() throws Exception
    {
        EasyMock.expect(mockBundleContext.getProperty(DirectoryWatcher.CONFIG_ENCODING)).andReturn(null);
        EasyMock.expect(mockBundleContext.getProperty(DirectoryWatcher.LOG_DEFAULT)).andReturn(null);
        EasyMock.expect(mockBundleContext.getProperty(DirectoryWatcher.LOG_LEVEL)).andReturn(null);
        EasyMock.expect(mockConfiguration.getProperties()).andReturn(new Hashtable<String, Object>());
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
        mockConfiguration.update(new Hashtable());
        EasyMock.expect(mockConfigurationAdmin.listConfigurations((String) EasyMock.anyObject()))
                        .andReturn(null);
        EasyMock.expect(mockConfigurationAdmin.getConfiguration("firstcfg", null))
                        .andReturn(mockConfiguration);
        EasyMock.replay(new Object[]{mockConfiguration, mockConfigurationAdmin, mockBundleContext});
=======
        mockConfiguration.update(new Hashtable<String, Object>());
        EasyMock.expect(mockConfigurationAdmin.listConfigurations((String) EasyMock.anyObject()))
                        .andReturn(null);
        EasyMock.expect(mockConfigurationAdmin.getConfiguration("firstcfg", "?"))
                        .andReturn(mockConfiguration);
        EasyMock.replay(mockConfiguration, mockConfigurationAdmin, mockBundleContext);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

        ConfigInstaller ci = new ConfigInstaller( mockBundleContext, mockConfigurationAdmin, new FileInstall() );

        assertTrue( ci.setConfig( new File( "src/test/resources/watched/firstcfg.cfg" ) ) );

<<<<<<< HEAD
        EasyMock.verify(new Object[]{mockConfiguration, mockConfigurationAdmin, mockBundleContext});
=======
        EasyMock.verify(mockConfiguration, mockConfigurationAdmin, mockBundleContext);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }
    
    public void testShouldSaveConfig() 
    {
<<<<<<< HEAD
        final AtomicReference disable = new AtomicReference();
        final AtomicReference enable = new AtomicReference();
        
        EasyMock.expect(mockBundleContext.getProperty(DirectoryWatcher.DISABLE_CONFIG_SAVE)).andAnswer(
                new IAnswer() {
                    public Object answer() throws Throwable {
=======
        final AtomicReference<Boolean> disable = new AtomicReference<Boolean>();
        final AtomicReference<Boolean> enable = new AtomicReference<Boolean>();
        
        EasyMock.expect(mockBundleContext.getProperty(DirectoryWatcher.DISABLE_CONFIG_SAVE)).andAnswer(
                new IAnswer<String>() {
                    public String answer() throws Throwable {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                        return disable.get() != null ? disable.get().toString() : null;
                    }
                }
        ).anyTimes();
        EasyMock.expect(mockBundleContext.getProperty(DirectoryWatcher.ENABLE_CONFIG_SAVE)).andAnswer(
<<<<<<< HEAD
                new IAnswer() {
                    public Object answer() throws Throwable {
=======
                new IAnswer<String>() {
                    public String answer() throws Throwable {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                        return enable.get() != null ? enable.get().toString() : null;
                    }
                }
        ).anyTimes();
<<<<<<< HEAD
        EasyMock.replay(new Object[]{mockConfiguration, mockConfigurationAdmin, mockBundleContext});
=======
        EasyMock.replay(mockConfiguration, mockConfigurationAdmin, mockBundleContext);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

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

<<<<<<< HEAD
        EasyMock.verify(new Object[]{mockConfiguration, mockConfigurationAdmin, mockBundleContext});
=======
        EasyMock.verify(mockConfiguration, mockConfigurationAdmin, mockBundleContext);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

}
