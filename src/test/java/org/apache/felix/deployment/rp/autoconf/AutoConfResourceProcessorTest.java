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
package org.apache.felix.deployment.rp.autoconf;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.deploymentadmin.DeploymentPackage;
import org.osgi.service.deploymentadmin.spi.DeploymentSession;
import org.osgi.service.deploymentadmin.spi.ResourceProcessorException;
import org.osgi.service.log.LogService;

public class AutoConfResourceProcessorTest extends TestCase
{
    private static class ConfigurationAdminImpl implements ConfigurationAdmin
    {
        private final String[] m_expectedPIDs;
        private final String[] m_expectedFactoryPIDs;
        private final Map<String, ConfigurationImpl> m_configs;

        public ConfigurationAdminImpl(String... expectedPIDs)
        {
            this(expectedPIDs, new String[0]);
        }

        public ConfigurationAdminImpl(String[] expectedPIDs, String[] expectedFactoryPIDs)
        {
            m_expectedPIDs = expectedPIDs;
            m_expectedFactoryPIDs = expectedFactoryPIDs;

            m_configs = new LinkedHashMap<String, ConfigurationImpl>();
        }

        public Configuration createFactoryConfiguration(String factoryPid) throws IOException
        {
            return createFactoryConfiguration(factoryPid, null);
        }

        public Configuration createFactoryConfiguration(String factoryPid, String location) throws IOException
        {
            if (!isExpected(m_expectedFactoryPIDs, factoryPid))
            {
                throw new IOException("Unexpected factory PID: " + factoryPid);
            }
            // This should be unique enough for our use cases...
            String pid = String.format("pid%d", m_configs.size());

            ConfigurationImpl config = m_configs.get(pid);
            if (config == null)
            {
                config = new ConfigurationImpl(factoryPid, pid, location);
                m_configs.put(pid, config);
            }
            config.setBundleLocation(location);
            return config;
        }

        public Configuration getConfiguration(String pid) throws IOException
        {
            return getConfiguration(pid, null);
        }

        public Configuration getConfiguration(String pid, String location) throws IOException
        {
            if (!isExpected(m_expectedPIDs, pid))
            {
                throw new IOException("Unexpected PID: " + pid);
            }

            ConfigurationImpl config = m_configs.get(pid);
            if (config == null)
            {
                config = new ConfigurationImpl(null, pid, location);
                m_configs.put(pid, config);
            }
            config.setBundleLocation(location);
            return config;
        }

        public Configuration[] listConfigurations(String filter) throws IOException, InvalidSyntaxException
        {
            return null;
        }

        private boolean isExpected(String[] expectedPIDs, String actualPID)
        {
            for (String expectedPID : expectedPIDs)
            {
                if (actualPID.equals(expectedPID))
                {
                    return true;
                }
            }
            return false;
        }
    }

    private static class ConfigurationImpl implements Configuration
    {
        private final String m_factoryPID;
        private final String m_pid;
        private String m_bundleLocation;
        private Dictionary m_properties;
        private boolean m_deleted;

        public ConfigurationImpl(String factoryPid, String pid, String bundleLocation)
        {
            m_factoryPID = factoryPid;
            m_pid = pid;
            m_bundleLocation = bundleLocation;
        }

        public void delete() throws IOException
        {
            m_deleted = true;
        }

        public String getBundleLocation()
        {
            return m_bundleLocation;
        }

        public String getFactoryPid()
        {
            return m_factoryPID;
        }

        public String getPid()
        {
            return m_pid;
        }

        public Dictionary getProperties()
        {
            return m_properties;
        }

        public void setBundleLocation(String bundleLocation)
        {
            if (m_bundleLocation != null && !m_bundleLocation.equals(bundleLocation))
            {
                throw new RuntimeException("Configuration already bound to location: " + m_bundleLocation + " (trying to set to: " + bundleLocation + ")");
            }
            m_bundleLocation = bundleLocation;
        }

        public void update() throws IOException
        {
        }

        public void update(Dictionary properties) throws IOException
        {
            m_properties = properties;
        }
    }

    /** Dummy session. */
    private static class DeploymentSessionImpl implements DeploymentSession
    {
        public File getDataFile(Bundle bundle)
        {
            return null;
        }

        public DeploymentPackage getSourceDeploymentPackage()
        {
            return null;
        }

        public DeploymentPackage getTargetDeploymentPackage()
        {
            return null;
        }

        @Override
        public String toString()
        {
            return "Test DeploymentSession @ 0x" + System.identityHashCode(this);
        }
    }

    private static class LogServiceImpl implements LogService
    {
        private static final String[] LEVEL = { "", "[ERROR]", "[WARN ]", "[INFO ]", "[DEBUG]" };
        private Throwable m_exception;

        public void failOnException() throws Throwable
        {
            if (m_exception != null)
            {
                throw m_exception;
            }
        }

        public void log(int level, String message)
        {
            System.out.println(LEVEL[level] + " - " + message);
        }

        public void log(int level, String message, Throwable exception)
        {
            System.out.println(LEVEL[level] + " - " + message + " - " + exception.getMessage());
            m_exception = exception;
        }

        public void log(ServiceReference sr, int level, String message)
        {
            System.out.println(LEVEL[level] + " - " + message);
        }

        public void log(ServiceReference sr, int level, String message, Throwable exception)
        {
            System.out.println(LEVEL[level] + " - " + message + " - " + exception.getMessage());
            m_exception = exception;
        }
    }

    private static class ServiceReferenceImpl implements ServiceReference
    {
        private final Properties m_properties;

        public ServiceReferenceImpl()
        {
            this(new Properties());
        }

        public ServiceReferenceImpl(Properties properties)
        {
            m_properties = properties;
        }

        public int compareTo(Object reference)
        {
            return 0;
        }

        public Bundle getBundle()
        {
            return null;
        }

        public Object getProperty(String key)
        {
            return m_properties.get(key);
        }

        public String[] getPropertyKeys()
        {
            return Collections.list(m_properties.keys()).toArray(new String[0]);
        }

        public Bundle[] getUsingBundles()
        {
            return null;
        }

        public boolean isAssignableTo(Bundle bundle, String className)
        {
            return false;
        }

        @Override
        public String toString()
        {
            return "Test ConfigAdmin @ 0x" + System.identityHashCode(this);
        }
    }

    private File m_tempDir;
    private LogServiceImpl m_logger;

    /** Go through a simple session, containing two empty configurations. */
    public void testBasicConfigurationSession() throws Throwable
    {
        AutoConfResourceProcessor p = createAutoConfRP();

        createNewSession(p);
        String config = "<MetaData xmlns:metatype='http://www.osgi.org/xmlns/metatype/v1.0.0'>\n" +
            "  <OCD name='ocd' id='ocd'>\n" +
            "    <AD id='name' type='STRING' cardinality='0' />\n" +
            "  </OCD>\n" +
            "  <Designate pid='simple' bundle='osgi-dp:location'>\n" +
            "    <Object ocdref='ocd'>\n" +
            "      <Attribute adref='name'>\n" +
            "        <Value><![CDATA[test]]></Value>\n" +
            "      </Attribute>\n" +
            "    </Object>\n" +
            "  </Designate>\n" +
            "</MetaData>\n";
        p.process("basic", new ByteArrayInputStream(config.getBytes()));
        p.prepare();
        p.commit();
        p.addConfigurationAdmin(new ServiceReferenceImpl(), new ConfigurationAdminImpl("simple"));
        p.postcommit();
        m_logger.failOnException();
    }

    /** Go through a simple session, containing two empty configurations. */
    public void testFilteredConfigurationSession() throws Throwable
    {
        AutoConfResourceProcessor p = createAutoConfRP();

        createNewSession(p);
        String config = "<MetaData xmlns:metatype='http://www.osgi.org/xmlns/metatype/v1.0.0' filter='(id=42)'>\n" +
            "  <OCD name='ocd' id='ocd'>\n" +
            "    <AD id='name' type='STRING' cardinality='0' />\n" +
            "  </OCD>\n" +
            "  <Designate pid='simple' bundle='osgi-dp:location'>\n" +
            "    <Object ocdref='ocd'>\n" +
            "      <Attribute adref='name'>\n" +
            "        <Value><![CDATA[test]]></Value>\n" +
            "      </Attribute>\n" +
            "    </Object>\n" +
            "  </Designate>\n" +
            "</MetaData>\n";
        p.process("basic", new ByteArrayInputStream(config.getBytes()));
        p.prepare();
        p.commit();

        Properties props = new Properties();
        props.put("id", Integer.valueOf(42));

        ConfigurationAdminImpl ca1 = new ConfigurationAdminImpl("simple");
        ConfigurationAdminImpl ca2 = new ConfigurationAdminImpl();

        p.addConfigurationAdmin(new ServiceReferenceImpl(props), ca1);
        p.addConfigurationAdmin(new ServiceReferenceImpl(), ca2);
        p.postcommit();

        m_logger.failOnException();

        assertEquals("test", ca1.m_configs.get("simple").getProperties().get("name"));
        assertTrue(ca2.m_configs.isEmpty());
    }

    /** Go through a simple session, containing two empty configurations. */
    public void testMissingMandatoryValueInConfig() throws Throwable
    {
        AutoConfResourceProcessor p = createAutoConfRP();

        createNewSession(p);

        String config = "<MetaData xmlns:metatype='http://www.osgi.org/xmlns/metatype/v1.1.0' filter='(id=42)'>\n" +
            "  <OCD name='ocd' id='ocd'>\n" +
            "    <AD id='name' type='Integer' />\n" +
            "  </OCD>\n" +
            "  <Designate pid='simple' bundle='osgi-dp:location'>\n" +
            "    <Object ocdref='ocd'>\n" +
            "      <Attribute adref='name'>\n" +
            "        <Value><![CDATA[]]></Value>\n" +
            "      </Attribute>\n" +
            "    </Object>\n" +
            "  </Designate>\n" +
            "</MetaData>\n";

        try
        {
            p.process("missing-value", new ByteArrayInputStream(config.getBytes()));
            fail("Expected ResourceProcessorException for missing value!");
        }
        catch (ResourceProcessorException e)
        {
            // Ok; expected...
            assertEquals("Unable to parse value for definition: adref=name", e.getMessage());
        }
    }

    /** Make sure the processor does not accept a 'null' session. */
    public void testNullSession() throws Exception
    {
        AutoConfResourceProcessor p = new AutoConfResourceProcessor();
        try
        {
            p.begin(null);
            fail("Should have gotten an exception when trying to begin with null session.");
        }
        catch (Exception e)
        {
            // expected
        }
    }

    /** Go through a simple session, containing two empty configurations. */
    public void testSimpleInstallAndUninstallSession() throws Throwable
    {
        AutoConfResourceProcessor p = createAutoConfRP();

        createNewSession(p);

        p.process("a", new ByteArrayInputStream("<MetaData />".getBytes()));
        p.prepare();
        p.commit();
        p.postcommit();
        m_logger.failOnException();

        createNewSession(p);

        p.dropAllResources();
        p.prepare();
        p.commit();
        p.postcommit();
        m_logger.failOnException();
    }

    /** Go through a simple session, containing two empty configurations. */
    public void testSimpleSession() throws Throwable
    {
        AutoConfResourceProcessor p = createAutoConfRP();

        createNewSession(p);
        p.process("a", new ByteArrayInputStream("<MetaData />".getBytes()));
        p.process("b", new ByteArrayInputStream("<MetaData />".getBytes()));
        p.prepare();
        p.commit();
        p.postcommit();
        m_logger.failOnException();
    }

    /** Tests that we can update an existing configuration and properly handling deleted & updated configurations. */
    public void testUpdateConfigurationSession() throws Throwable
    {
        AutoConfResourceProcessor p = createAutoConfRP();

        createNewSession(p);

        String config1 = "<MetaData xmlns:metatype='http://www.osgi.org/xmlns/metatype/v1.0.0'>" +
            "<OCD name='ocd1' id='ocd1'>" +
            "  <AD id='nameA' type='STRING' cardinality='0' />" +
            "</OCD>" +
            "<OCD name='ocd2' id='ocd2'>" +
            "  <AD id='nameB' type='STRING' cardinality='0' />" +
            "</OCD>" +
            "<Designate pid='pid2' bundle='osgi-dp:location2'>" +
            "  <Object ocdref='ocd2'>" +
            "    <Attribute adref='nameB'>" +
            "      <Value><![CDATA[test2]]></Value>" +
            "    </Attribute>" +
            "  </Object>" +
            "</Designate>" +
            "<Designate pid='pid1' bundle='osgi-dp:location1'>" +
            "  <Object ocdref='ocd1'>" +
            "    <Attribute adref='nameA'>" +
            "      <Value><![CDATA[test1]]></Value>" +
            "    </Attribute>" +
            "  </Object>" +
            "</Designate>" +
            "</MetaData>";

        ConfigurationAdminImpl ca = new ConfigurationAdminImpl("pid1", "pid2", "pid3");

        p.process("update", new ByteArrayInputStream(config1.getBytes()));
        p.prepare();
        p.commit();
        p.addConfigurationAdmin(new ServiceReferenceImpl(), ca);
        p.postcommit();
        m_logger.failOnException();

        assertEquals(2, ca.m_configs.size());
        assertTrue(ca.m_configs.containsKey("pid1"));
        assertFalse(ca.m_configs.get("pid1").m_deleted);
        assertEquals("test1", ca.m_configs.get("pid1").getProperties().get("nameA"));

        assertTrue(ca.m_configs.containsKey("pid2"));
        assertFalse(ca.m_configs.get("pid2").m_deleted);
        assertEquals("test2", ca.m_configs.get("pid2").getProperties().get("nameB"));

        String config2 = "<MetaData xmlns:metatype='http://www.osgi.org/xmlns/metatype/v1.0.0'>" +
            "<OCD name='ocd3' id='ocd3'>" +
            "  <AD id='nameC' type='STRING' cardinality='0' />" +
            "</OCD>" +
            "<OCD name='ocd2' id='ocd2'>" +
            "  <AD id='nameB' type='STRING' cardinality='0' />" +
            "</OCD>" +
            "<Designate pid='pid2' bundle='osgi-dp:location2'>" +
            "  <Object ocdref='ocd2'>" +
            "    <Attribute adref='nameB'>" +
            "      <Value><![CDATA[test4]]></Value>" +
            "    </Attribute>" +
            "  </Object>" +
            "</Designate>" +
            "<Designate pid='pid3' bundle='osgi-dp:location3'>" +
            "  <Object ocdref='ocd3'>" +
            "    <Attribute adref='nameC'>" +
            "      <Value><![CDATA[test3]]></Value>" +
            "    </Attribute>" +
            "  </Object>" +
            "</Designate>" +
            "</MetaData>";

        createNewSession(p);

        p.process("update", new ByteArrayInputStream(config2.getBytes()));
        p.prepare();
        p.commit();
        p.addConfigurationAdmin(new ServiceReferenceImpl(), ca);
        p.postcommit();
        m_logger.failOnException();

        assertEquals(3, ca.m_configs.size());
        assertTrue(ca.m_configs.containsKey("pid1"));
        assertTrue(ca.m_configs.get("pid1").m_deleted);
        assertEquals("test1", ca.m_configs.get("pid1").getProperties().get("nameA"));

        assertTrue(ca.m_configs.containsKey("pid2"));
        assertFalse(ca.m_configs.get("pid2").m_deleted);
        assertEquals("test4", ca.m_configs.get("pid2").getProperties().get("nameB"));

        assertTrue(ca.m_configs.containsKey("pid3"));
        assertFalse(ca.m_configs.get("pid3").m_deleted);
        assertEquals("test3", ca.m_configs.get("pid3").getProperties().get("nameC"));
    }

    @Override
    protected void setUp() throws IOException
    {
        m_tempDir = File.createTempFile("persistence", "dir");
        m_tempDir.delete();
        m_tempDir.mkdirs();

        m_logger = new LogServiceImpl();
    }

    @Override
    protected void tearDown() throws Exception
    {
        Utils.removeDirectoryWithContent(m_tempDir);
    }

    private AutoConfResourceProcessor createAutoConfRP()
    {
        AutoConfResourceProcessor p = new AutoConfResourceProcessor();
        Utils.configureObject(p, LogService.class, m_logger);
        Utils.configureObject(p, DependencyManager.class, createMockDM());
        Utils.configureObject(p, PersistencyManager.class, new PersistencyManager(m_tempDir));
        return p;
    }

    @SuppressWarnings("unused")
    private BundleContext createMockBundleContext()
    {
        return Utils.createMockObjectAdapter(BundleContext.class, new Object()
        {
            public Filter createFilter(String condition)
            {
                return Utils.createMockObjectAdapter(Filter.class, new Object()
                {
                    public boolean match(ServiceReference ref)
                    {
                        Object id = ref.getProperty("id");
                        if (id != null && id.equals(Integer.valueOf(42)))
                        {
                            return true;
                        }
                        return false;
                    }

                    public void remove(Component service)
                    {
                    }
                });
            }
        });
    }

    @SuppressWarnings("unused")
    private Component createMockComponent()
    {
        return Utils.createMockObjectAdapter(Component.class, new Object()
        {
            public DependencyManager getDependencyManager()
            {
                return new DependencyManager(createMockBundleContext());
            }
        });
    }

    private DependencyManager createMockDM()
    {
        return new DependencyManager(createMockBundleContext())
        {
            public void remove(Component service)
            {
            }
        };
    }

    private DeploymentSession createNewSession(AutoConfResourceProcessor p)
    {
        DeploymentSessionImpl s = new DeploymentSessionImpl();
        p.begin(s);
        Utils.configureObject(p, Component.class, createMockComponent());
        return s;
    }
}
