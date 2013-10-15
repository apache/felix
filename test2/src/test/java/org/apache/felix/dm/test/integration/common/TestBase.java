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
package org.apache.felix.dm.test.integration.common;

import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.cleanCaches;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.vmOption;
import static org.ops4j.pax.exam.CoreOptions.workingDirectory;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Hashtable;

import javax.inject.Inject;

import org.apache.felix.dm.test.components.Ensure;
import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

public abstract class TestBase implements LogService, FrameworkListener {
    private final static int LOG_LEVEL = LogService.LOG_WARNING;
    private volatile boolean m_errorsLogged;

    // the name of the system property providing the bundle file to be installed
    // and tested
    protected static final String BUNDLE_JAR_SYS_PROP = "project.bundle.file";

    // the default bundle jar file name
    protected static final String BUNDLE_JAR_DEFAULT = "target/org.apache.felix.dependencymanager.test-3.0.1-SNAPSHOT.jar";

    // the JVM option to set to enable remote debugging
    protected static final String DEBUG_VM_OPTION = "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=30303";

    // the actual JVM option set, extensions may implement a static
    // initializer overwriting this value to have the configuration()
    // method include it when starting the OSGi framework JVM
    protected static String paxRunnerVmOption = null;

    @Inject
    protected BundleContext context;

    protected ServiceRegistration logService;
    
    // Flag used to check if our test components (src/main/java/**/*) bundle must be started
    private final boolean m_startTestComponentsBundle;
    
    public TestBase() {
        this(false);
    }
    
    public TestBase(boolean startTestComponentsBundle) {
        m_startTestComponentsBundle = startTestComponentsBundle;
    }
 
    @Configuration
    public Option[] configuration() {
        final String bundleFileName = System.getProperty(BUNDLE_JAR_SYS_PROP, BUNDLE_JAR_DEFAULT);
        final File bundleFile = new File(bundleFileName);
        if (!bundleFile.canRead()) {
            throw new IllegalArgumentException("Cannot read from bundle file " + bundleFileName
                    + " specified in the " + BUNDLE_JAR_SYS_PROP + " system property");
        }
        
        // Reduce pax exam log level.
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.WARN);

        final Option[] base = options(
                workingDirectory("target/paxexam/"),
                systemProperty("dm.runtime.log").value("false"),
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("WARN"),
                systemTimeout(5000),
                cleanCaches(true),
                junitBundles(),
                mavenBundle("org.apache.felix", "org.apache.felix.metatype", "1.0.8"),
                mavenBundle("org.apache.felix", "org.apache.felix.configadmin", "1.6.0"),
                mavenBundle("org.apache.felix", "org.apache.felix.shell", "1.4.2"),
                mavenBundle("org.apache.felix", "org.apache.felix.deploymentadmin", "0.9.0").start(false),
                mavenBundle("org.ops4j.pax.tinybundles", "tinybundles", "1.0.0"),
                mavenBundle("org.apache.felix", "org.apache.felix.dependencymanager","3.1.1-SNAPSHOT"),
                mavenBundle("org.apache.felix", "org.apache.felix.dependencymanager.shell", "3.0.2-SNAPSHOT"),
                mavenBundle("org.apache.felix", "org.apache.felix.dependencymanager.runtime", "3.1.1-SNAPSHOT"),
                bundle(bundleFile.toURI().toString()).start(m_startTestComponentsBundle));
        final Option option = (paxRunnerVmOption != null) ? vmOption(paxRunnerVmOption) : null;
        return OptionUtils.combine(base, option);
    }

    @ProbeBuilder
    public TestProbeBuilder buildProbe(TestProbeBuilder builder) {
        return builder.setHeader(Constants.IMPORT_PACKAGE,
                "org.apache.felix.dm.test.components");
    }

    @Before
    public void setUp() {
        logService = context.registerService(LogService.class.getName(), this, null);
        context.addFrameworkListener(this);
    }

    @After
    public void tearDown() throws BundleException {
       logService.unregister();
       context.removeFrameworkListener(this);
    }

    /**
     * Create and provide an Ensure object with a name service property.
     */
    protected ServiceRegistration register(Ensure e, String name) {
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put("name", name);
        return context.registerService(Ensure.class.getName(), e, props);
    }

    /**
     * Helper method used to stop a given bundle.
     * 
     * @param symbolicName
     *            the symbolic name of the bundle to be stopped.
     * @param context
     *            the context used to lookup all installed bundles.
     */
    protected void stopBundle(String symbolicName) {
        // Stop the test.annotation bundle
        boolean found = false;
        for (Bundle b : context.getBundles()) {
            if (b.getSymbolicName().equals(symbolicName)) {
                try {
                    found = true;
                    b.stop();
                } catch (BundleException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!found) {
            throw new IllegalStateException("bundle " + symbolicName + " not found");
        }
    }

    /**
     * Stops our test components bundle.
     */
    protected void stopTestComponentsBundle() {
        stopBundle("org.apache.felix.dependencymanager.test.components");
    }

    /**
     * Suspend the current thread for a while.
     * 
     * @param n
     *            the number of milliseconds to wait for.
     */
    protected void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

    public void log(int level, String message) {
        checkError(level, null);
        if (LOG_LEVEL >= level) {
            System.out.println(getLevel(level) + " - " + Thread.currentThread().getName() + " : " + message);
        }
    }

    public void log(int level, String message, Throwable exception) {
        checkError(level, exception);
        if (LOG_LEVEL >= level) {
            StringBuilder sb = new StringBuilder();
            sb.append(getLevel(level) + " - " + Thread.currentThread().getName() + " : ");
            sb.append(message);
            parse(sb, exception);
            System.out.println(sb.toString());
        }
    }

    public void log(ServiceReference sr, int level, String message) {
        checkError(level, null);
        if (LOG_LEVEL >= level) {
            StringBuilder sb = new StringBuilder();
            sb.append(getLevel(level) + " - " + Thread.currentThread().getName() + " : ");
            sb.append(message);
            System.out.println(sb.toString());
        }
    }

    public void log(ServiceReference sr, int level, String message, Throwable exception) {
        checkError(level, exception);
        if (LOG_LEVEL >= level) {
            StringBuilder sb = new StringBuilder();
            sb.append(getLevel(level) + " - " + Thread.currentThread().getName() + " : ");
            sb.append(message);
            parse(sb, exception);
            System.out.println(sb.toString());
        }
    }

    protected boolean errorsLogged() {
        return m_errorsLogged;
    }

    private void parse(StringBuilder sb, Throwable t) {
        if (t != null) {
            sb.append(" - ");
            StringWriter buffer = new StringWriter();
            PrintWriter pw = new PrintWriter(buffer);
            t.printStackTrace(pw);
            sb.append(buffer.toString());
            m_errorsLogged = true;
        }
    }

    private String getLevel(int level) {
        switch (level) {
            case LogService.LOG_DEBUG :
                return "DEBUG";
            case LogService.LOG_ERROR :
                return "ERROR";
            case LogService.LOG_INFO :
                return "INFO";
            case LogService.LOG_WARNING :
                return "WARN";
            default :
                return "";
        }
    }

    private void checkError(int level, Throwable exception) {
        if (level >= LOG_ERROR) {
            m_errorsLogged = true;
        }
        if (exception != null) {
            m_errorsLogged = true;
        }
    }

    public void frameworkEvent(FrameworkEvent event) {
        int eventType = event.getType();
        String msg = getFrameworkEventMessage(eventType);
        int level = (eventType == FrameworkEvent.ERROR) ? LOG_ERROR : LOG_WARNING;
        if (msg != null) {
            log(level, msg, event.getThrowable());
        } else {
            log(level, "Unknown fwk event: " + event);
        }
    }

    private String getFrameworkEventMessage(int event) {
        switch (event) {
            case FrameworkEvent.ERROR :
                return "FrameworkEvent: ERROR";
            case FrameworkEvent.INFO :
                return "FrameworkEvent INFO";
            case FrameworkEvent.PACKAGES_REFRESHED :
                return "FrameworkEvent: PACKAGE REFRESHED";
            case FrameworkEvent.STARTED :
                return "FrameworkEvent: STARTED";
            case FrameworkEvent.STARTLEVEL_CHANGED :
                return "FrameworkEvent: STARTLEVEL CHANGED";
            case FrameworkEvent.WARNING :
                return "FrameworkEvent: WARNING";
            default :
                return null;
        }
    }

    protected void warn(String msg) {
        log(LogService.LOG_WARNING, msg);
    }

    protected void info(String msg) {
        log(LogService.LOG_INFO, msg);
    }

    protected void debug(String msg) {
        log(LogService.LOG_DEBUG, msg);
    }

    protected void error(String msg) {
        log(LogService.LOG_ERROR, msg);
    }

    protected void error(String msg, Throwable err) {
        log(LogService.LOG_ERROR, msg, err);
    }

    protected void error(Throwable err) {
        log(LogService.LOG_ERROR, "error", err);
    }
}
