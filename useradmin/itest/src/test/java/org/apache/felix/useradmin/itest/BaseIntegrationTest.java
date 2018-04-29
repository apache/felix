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
package org.apache.felix.useradmin.itest;

import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.Constants.START_LEVEL_SYSTEM_BUNDLES;
import static org.ops4j.pax.exam.Constants.START_LEVEL_TEST_BUNDLE;
import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackage;
import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.frameworkStartLevel;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.url;

import javax.inject.Inject;

import org.junit.Before;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Base class for integration tests.
 *  
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
<<<<<<< HEAD
public abstract class BaseIntegrationTest {

	private static final int DEFAULT_TIMEOUT = 10000;
	
	protected static final String ORG_APACHE_FELIX_USERADMIN = "org.apache.felix.useradmin";
	protected static final String ORG_APACHE_FELIX_USERADMIN_FILESTORE = "org.apache.felix.useradmin.filestore";
=======
public abstract class BaseIntegrationTest
{

    private static final int DEFAULT_TIMEOUT = 10000;

    protected static final String ORG_APACHE_FELIX_USERADMIN = "org.apache.felix.useradmin";
    protected static final String ORG_APACHE_FELIX_USERADMIN_FILESTORE = "org.apache.felix.useradmin.filestore";
    protected static final String ORG_APACHE_FELIX_USERADMIN_MONGODBSTORE = "org.apache.felix.useradmin.mongodb";
    protected static final String ORG_MONGODB_MONGO_JAVA_DRIVER = "org.mongodb.mongo-java-driver";
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    @Inject
    protected volatile BundleContext m_context;

    @Configuration
<<<<<<< HEAD
    public Option[] config() {
        return options(
            bootDelegationPackage("sun.*"),
            cleanCaches(),
            CoreOptions.systemProperty("logback.configurationFile").value("file:src/test/resources/logback.xml"),
=======
    public Option[] config()
    {
        return options(
            bootDelegationPackage("sun.*"),
            cleanCaches(),
            CoreOptions.systemProperty("logback.configurationFile").value("file:src/test/resources/logback.xml"), //
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
//            CoreOptions.vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8787"),

            mavenBundle("org.slf4j", "slf4j-api").version("1.6.5").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            mavenBundle("ch.qos.logback", "logback-core").version("1.0.6").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            mavenBundle("ch.qos.logback", "logback-classic").version("1.0.6").startLevel(START_LEVEL_SYSTEM_BUNDLES),

            url("link:classpath:META-INF/links/org.ops4j.pax.exam.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            url("link:classpath:META-INF/links/org.ops4j.pax.exam.inject.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            url("link:classpath:META-INF/links/org.ops4j.pax.extender.service.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            url("link:classpath:META-INF/links/org.ops4j.base.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.core.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.extender.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.lifecycle.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            url("link:classpath:META-INF/links/org.ops4j.pax.swissbox.framework.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),
            url("link:classpath:META-INF/links/org.apache.geronimo.specs.atinject.link").startLevel(START_LEVEL_SYSTEM_BUNDLES),

            mavenBundle("org.apache.felix", ORG_APACHE_FELIX_USERADMIN).versionAsInProject().startLevel(START_LEVEL_SYSTEM_BUNDLES),
<<<<<<< HEAD
            mavenBundle("org.apache.felix", ORG_APACHE_FELIX_USERADMIN_FILESTORE).versionAsInProject().startLevel(START_LEVEL_SYSTEM_BUNDLES),
            
            junitBundles(),
            frameworkStartLevel(START_LEVEL_TEST_BUNDLE),
            felix());
    }

    @Before
    public void setUp() throws Exception {
=======
            mavenBundle("org.apache.felix", ORG_APACHE_FELIX_USERADMIN_FILESTORE).versionAsInProject().noStart(),
            mavenBundle("org.apache.felix", ORG_APACHE_FELIX_USERADMIN_MONGODBSTORE).versionAsInProject().noStart(), mavenBundle("org.mongodb", "mongo-java-driver").versionAsInProject().noStart(),

            junitBundles(), frameworkStartLevel(START_LEVEL_TEST_BUNDLE), felix());
    }

    @Before
    public void setUp() throws Exception
    {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        assertNotNull("No bundle context?!", m_context);
    }

    /**
     * Waits for a service to become available in certain time interval.
     * @param serviceName
     * @return
     * @throws Exception
     */
<<<<<<< HEAD
    protected <T> T awaitService(String serviceName) throws Exception {
        ServiceTracker tracker = new ServiceTracker(m_context, serviceName, null);
        tracker.open();
        T result;
        try {
            result = (T) tracker.waitForService(DEFAULT_TIMEOUT);
        }
        finally {
=======
    protected <T> T awaitService(String serviceName) throws Exception
    {
        ServiceTracker tracker = new ServiceTracker(m_context, serviceName, null);
        tracker.open();
        T result;
        try
        {
            result = (T) tracker.waitForService(DEFAULT_TIMEOUT);
        }
        finally
        {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            tracker.close();
        }
        return result;
    }

    /**
<<<<<<< HEAD
=======
     * @param bsn
     * @return
     */
    protected Bundle findBundle(String bsn)
    {
        for (Bundle bundle : m_context.getBundles())
        {
            if (bsn.equals(bundle.getSymbolicName()))
            {
                return bundle;
            }
        }
        return null;
    }

    protected Bundle getFileStoreBundle()
    {
        Bundle b = findBundle(ORG_APACHE_FELIX_USERADMIN_FILESTORE);
        assertNotNull("Filestore bundle not found?!", b);
        return b;
    }

    protected Bundle getMongoDBStoreBundle()
    {
        Bundle b = findBundle(ORG_APACHE_FELIX_USERADMIN_MONGODBSTORE);
        assertNotNull("MongoDB store bundle not found?!", b);
        return b;
    }

    protected Bundle getMongoDBBundle()
    {
        Bundle b = findBundle(ORG_MONGODB_MONGO_JAVA_DRIVER);
        assertNotNull("MongoDB bundle not found?!", b);
        return b;
    }

    /**
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
     * Obtains a service without waiting for it to become available.
     * @param serviceName
     * @return
     * @throws Exception
     */
<<<<<<< HEAD
    protected <T> T getService(String serviceName) throws Exception {
        ServiceTracker tracker = new ServiceTracker(m_context, serviceName, null);
        tracker.open();
        T result;
        try {
            result = (T) tracker.getService();
        }
        finally {
=======
    protected <T> T getService(String serviceName) throws Exception
    {
        ServiceTracker tracker = new ServiceTracker(m_context, serviceName, null);
        tracker.open();
        T result;
        try
        {
            result = (T) tracker.getService();
        }
        finally
        {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
            tracker.close();
        }
        return result;
    }
<<<<<<< HEAD
    
    /**
     * @return the {@link UserAdmin} service instance.
     */
    protected UserAdmin getUserAdmin() throws Exception {
        return getService(UserAdmin.class.getName());
    }

    /**
     * @param bsn
     * @return
     */
    protected Bundle findBundle(String bsn) {
    	for (Bundle bundle : m_context.getBundles()) {
    		if (bsn.equals(bundle.getSymbolicName())) {
    			return bundle;
    		}
    	}
    	return null;
    }
=======

    /**
     * @return the {@link UserAdmin} service instance.
     */
    protected UserAdmin getUserAdmin() throws Exception
    {
        return getService(UserAdmin.class.getName());
    }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
}
