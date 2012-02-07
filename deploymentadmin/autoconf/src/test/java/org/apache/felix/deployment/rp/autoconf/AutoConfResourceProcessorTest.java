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
import java.util.Dictionary;
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
import org.osgi.service.log.LogService;

public class AutoConfResourceProcessorTest extends TestCase {
    /** Make sure the processor does not accept a 'null' session. */
    public void testNullSession() throws Exception {
        AutoConfResourceProcessor p = new AutoConfResourceProcessor();
        try {
            p.begin(null);
            fail("Should have gotten an exception when trying to begin with null session.");
        }
        catch (Exception e) {
            // expected
        }
    }
    
    /** Go through a simple session, containing two empty configurations. */
    public void testSimpleSession() throws Exception {
        AutoConfResourceProcessor p = new AutoConfResourceProcessor();
        Utils.configureObject(p, LogService.class);
        Utils.configureObject(p, Component.class, Utils.createMockObjectAdapter(Component.class, new Object() {
            public DependencyManager getDependencyManager() {
                return new DependencyManager((BundleContext) Utils.createNullObject(BundleContext.class));
            }
        }));
        File tempDir = File.createTempFile("persistence", "dir");
        tempDir.delete();
        tempDir.mkdirs();
        
        System.out.println("Temporary dir: " + tempDir);
        
        Utils.configureObject(p, PersistencyManager.class, new PersistencyManager(tempDir));
        Session s = new Session();
        p.begin(s);
        p.process("a", new ByteArrayInputStream("<MetaData />".getBytes()));
        p.process("b", new ByteArrayInputStream("<MetaData />".getBytes()));
        p.prepare();
        p.commit();
        p.postcommit();
        Utils.removeDirectoryWithContent(tempDir);
    }

    /** Go through a simple session, containing two empty configurations. */
    public void testSimpleInstallAndUninstallSession() throws Throwable {
        AutoConfResourceProcessor p = new AutoConfResourceProcessor();
        Utils.configureObject(p, LogService.class);
        Utils.configureObject(p, Component.class, Utils.createMockObjectAdapter(Component.class, new Object() {
            public DependencyManager getDependencyManager() {
                return new DependencyManager((BundleContext) Utils.createNullObject(BundleContext.class));
            }
        }));
        Logger logger = new Logger();
        Utils.configureObject(p, LogService.class, logger);
        File tempDir = File.createTempFile("persistence", "dir");
        tempDir.delete();
        tempDir.mkdirs();
        
        System.out.println("Temporary dir: " + tempDir);
        
        Utils.configureObject(p, PersistencyManager.class, new PersistencyManager(tempDir));
        Session s = new Session();
        p.begin(s);
        p.process("a", new ByteArrayInputStream("<MetaData />".getBytes()));
        p.prepare();
        p.commit();
        p.postcommit();
        logger.failOnException();
        s = new Session();
        p.begin(s);
        p.dropped("a");
        p.prepare();
        p.commit();
        p.postcommit();
        logger.failOnException();
        Utils.removeDirectoryWithContent(tempDir);
    }
    
    /** Go through a simple session, containing two empty configurations. */
    public void testBasicConfigurationSession() throws Throwable {
        AutoConfResourceProcessor p = new AutoConfResourceProcessor();
        Logger logger = new Logger();
        Utils.configureObject(p, LogService.class, logger);
        Utils.configureObject(p, Component.class, Utils.createMockObjectAdapter(Component.class, new Object() {
            public DependencyManager getDependencyManager() {
                return new DependencyManager((BundleContext) Utils.createNullObject(BundleContext.class));
            }
        }));
        File tempDir = File.createTempFile("persistence", "dir");
        tempDir.delete();
        tempDir.mkdirs();
        
        System.out.println("Temporary dir: " + tempDir);
        
        Utils.configureObject(p, PersistencyManager.class, new PersistencyManager(tempDir));
        Session s = new Session();
        p.begin(s);
        String config =
            "<MetaData xmlns:metatype='http://www.osgi.org/xmlns/metatype/v1.0.0'>\n" + 
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
        p.addConfigurationAdmin(null, new ConfigurationAdmin() {
            public Configuration[] listConfigurations(String filter) throws IOException, InvalidSyntaxException {
                return null;
            }
            
            public Configuration getConfiguration(String pid, String location) throws IOException {
                return new ConfigurationImpl();
            }
            
            public Configuration getConfiguration(String pid) throws IOException {
                return null;
            }
            
            public Configuration createFactoryConfiguration(String factoryPid, String location) throws IOException {
                return null;
            }
            
            public Configuration createFactoryConfiguration(String factoryPid) throws IOException {
                return null;
            }
        });
        p.postcommit();
        logger.failOnException();
        Utils.removeDirectoryWithContent(tempDir);
    }

    /** Go through a simple session, containing two empty configurations. */
    public void testFilteredConfigurationSession() throws Throwable {
        AutoConfResourceProcessor p = new AutoConfResourceProcessor();
        Logger logger = new Logger();
        Utils.configureObject(p, LogService.class, logger);
        Utils.configureObject(p, Component.class, Utils.createMockObjectAdapter(Component.class, new Object() {
            public DependencyManager getDependencyManager() {
                return new DependencyManager((BundleContext) Utils.createNullObject(BundleContext.class));
            }
        }));
        Utils.configureObject(p, BundleContext.class, Utils.createMockObjectAdapter(BundleContext.class, new Object() {
            public Filter createFilter(String condition) {
                return (Filter) Utils.createMockObjectAdapter(Filter.class, new Object() {
                    public boolean match(ServiceReference ref) {
                        Object id = ref.getProperty("id");
                        if (id != null && id.equals(Integer.valueOf(42))) {
                            return true;
                        }
                        return false;
                    }
                });
            }
        }));
        File tempDir = File.createTempFile("persistence", "dir");
        tempDir.delete();
        tempDir.mkdirs();
        
        System.out.println("Temporary dir: " + tempDir);
        
        Utils.configureObject(p, PersistencyManager.class, new PersistencyManager(tempDir));
        Session s = new Session();
        p.begin(s);
        String config =
            "<MetaData xmlns:metatype='http://www.osgi.org/xmlns/metatype/v1.0.0' filter='(id=42)'>\n" + 
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
        final Configuration configuration = new ConfigurationImpl();
        p.addConfigurationAdmin(new Reference(props), new ConfigurationAdmin() {
            public Configuration[] listConfigurations(String filter) throws IOException, InvalidSyntaxException {
                return null;
            }
            
            public Configuration getConfiguration(String pid, String location) throws IOException {
                return configuration;
            }
            
            public Configuration getConfiguration(String pid) throws IOException {
                return null;
            }
            
            public Configuration createFactoryConfiguration(String factoryPid, String location) throws IOException {
                return null;
            }
            
            public Configuration createFactoryConfiguration(String factoryPid) throws IOException {
                return null;
            }
        });
        
        final Configuration emptyConfiguration = new ConfigurationImpl();
        p.addConfigurationAdmin(new Reference(new Properties()), new ConfigurationAdmin() {
            public Configuration[] listConfigurations(String filter) throws IOException, InvalidSyntaxException {
                return null;
            }
            
            public Configuration getConfiguration(String pid, String location) throws IOException {
                return emptyConfiguration;
            }
            
            public Configuration getConfiguration(String pid) throws IOException {
                return null;
            }
            
            public Configuration createFactoryConfiguration(String factoryPid, String location) throws IOException {
                return null;
            }
            
            public Configuration createFactoryConfiguration(String factoryPid) throws IOException {
                return null;
            }
        });        
        p.postcommit();
        logger.failOnException();
        assertEquals("test", configuration.getProperties().get("name"));
        assertNull(emptyConfiguration.getProperties());
        Utils.removeDirectoryWithContent(tempDir);
    }

    private static class ConfigurationImpl implements Configuration {
        private String m_bundleLocation = "osgi-dp:location";
        private Dictionary m_properties;

        public String getPid() {
            return null;
        }

        public Dictionary getProperties() {
            return m_properties;
        }

        public void update(Dictionary properties) throws IOException {
            m_properties = properties;
        }

        public void delete() throws IOException {
        }

        public String getFactoryPid() {
            return null;
        }

        public void update() throws IOException {
        }

        public void setBundleLocation(String bundleLocation) {
            m_bundleLocation = bundleLocation;
        }

        public String getBundleLocation() {
            return m_bundleLocation;
        }
    }

    /** Dummy session. */
    private static class Session implements DeploymentSession {
        public DeploymentPackage getTargetDeploymentPackage() {
            return null;
        }
        public DeploymentPackage getSourceDeploymentPackage() {
            return null;
        }
        public File getDataFile(Bundle bundle) {
            return null;
        }
    }
    
    private static class Logger implements LogService {
        private static final String[] LEVEL = { "", "[ERROR]", "[WARN ]", "[INFO ]", "[DEBUG]" };
        private Throwable m_exception;
        
        public void log(int level, String message) {
            System.out.println(LEVEL[level] + " - " + message);
        }

        public void log(int level, String message, Throwable exception) {
            System.out.println(LEVEL[level] + " - " + message + " - " + exception.getMessage());
            m_exception = exception;
        }

        public void log(ServiceReference sr, int level, String message) {
            System.out.println(LEVEL[level] + " - " + message);
        }

        public void log(ServiceReference sr, int level, String message, Throwable exception) {
            System.out.println(LEVEL[level] + " - " + message + " - " + exception.getMessage());
            m_exception = exception;
        }
        
        public void failOnException() throws Throwable {
            if (m_exception != null) {
                throw m_exception;
            }
        }
    }
    private static class Reference implements ServiceReference {
        private final Dictionary m_properties;

        public Reference(Dictionary properties) {
            m_properties = properties;
        }

        public Object getProperty(String key) {
            return m_properties.get(key);
        }

        public String[] getPropertyKeys() {
            return null;
        }

        public Bundle getBundle() {
            return null;
        }

        public Bundle[] getUsingBundles() {
            return null;
        }

        public boolean isAssignableTo(Bundle bundle, String className) {
            return false;
        }

        public int compareTo(Object reference) {
            return 0;
        }
    }
}
