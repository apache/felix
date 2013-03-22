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

package org.apache.felix.jaas.integration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.felix.jaas.integration.common.SimpleCallbackHandler;
import org.apache.felix.jaas.integration.common.SimplePrincipal;
import org.apache.felix.jaas.integration.sample1.ConfigLoginModule;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;

import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

public abstract class JaasTestBase
{
    @Inject
    protected BundleContext bundleContext;

    @Inject
    protected ConfigurationAdmin ca;

    // the name of the system property providing the bundle file to be installed and tested
    protected static final String BUNDLE_JAR_SYS_PROP = "project.bundle.file";

    // the name of the system property which captures the jococo coverage agent command
    //if specified then agent would be specified otherwise ignored
    protected static final String COVERAGE_COMMAND = "coverage.command";

    // the default bundle jar file name
    protected static final String BUNDLE_JAR_DEFAULT = "target/jaas.jar";

    // the JVM option to set to enable remote debugging
    @SuppressWarnings("UnusedDeclaration")
    protected static final String DEBUG_VM_OPTION = "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=31313";

    // the actual JVM option set, extensions may implement a static
    // initializer overwriting this value to have the configuration()
    // method include it when starting the OSGi framework JVM
    protected static String paxRunnerVmOption = null;

    @Configuration
    public Option[] config()
    {
        final String bundleFileName = System.getProperty(BUNDLE_JAR_SYS_PROP,
            BUNDLE_JAR_DEFAULT);
        final File bundleFile = new File(bundleFileName);
        if (!bundleFile.canRead())
        {
            throw new IllegalArgumentException("Cannot read from bundle file "
                + bundleFileName + " specified in the " + BUNDLE_JAR_SYS_PROP
                + " system property");
        }
        Option[] base = options(
            // the current project (the bundle under test)
            CoreOptions.bundle(bundleFile.toURI().toString()),
            mavenBundle("org.ops4j.pax.tinybundles", "tinybundles").versionAsInProject(),
            mavenBundle( "org.apache.felix", "org.apache.felix.configadmin").versionAsInProject(),

            frameworkProperty("osgi.clean").value("true"),
            systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),

            junitBundles(),

            streamBundle(createCommonTestUtilBundle()),

            addCodeCoverageOption(),
            addExtraOptions());
        final Option vmOption = (paxRunnerVmOption != null) ? CoreOptions.vmOption(paxRunnerVmOption)  : null;
        return OptionUtils.combine(base, vmOption);
    }

    private Option addCodeCoverageOption()
    {
        String coverageCommand = System.getProperty(COVERAGE_COMMAND);
        if(coverageCommand != null){
            return CoreOptions.vmOption(coverageCommand);
        }
        return null;
    }

    @ProbeBuilder
    public TestProbeBuilder extendProbe(TestProbeBuilder builder)
    {
        builder.setHeader("Export-Package", "org.apache.felix.jaas.integration");
        builder.setHeader("Bundle-ManifestVersion", "2");
        return builder;
    }

    protected InputStream createConfigBasedBundle()
    {
        return TinyBundles.bundle()
                .add(ConfigLoginModule.class)
                .set("Jaas-ModuleClass","org.apache.felix.jaas.integration.sample1.ConfigLoginModule")
                .set(Constants.BUNDLE_SYMBOLICNAME, "org.apache.felix.jaas.sample1")
                .build(withBnd());
    }

    protected InputStream createCommonTestUtilBundle()
    {
        return TinyBundles.bundle().add(SimplePrincipal.class)
                .add(SimpleCallbackHandler.class)
                .set(Constants.BUNDLE_SYMBOLICNAME,"org.apache.felix.jaas.testcommon")
                .set(Constants.EXPORT_PACKAGE,"org.apache.felix.jaas.integration.common")
                .build(withBnd());
    }

    protected Option addExtraOptions()
    {
        return new DefaultCompositeOption();
    }

    protected static void delay()
    {
        try
        {
            TimeUnit.MILLISECONDS.sleep(300);
        }
        catch ( InterruptedException ie )
        {
            // dont care
        }
    }

    protected String createLoginModuleConfig(String realmName) throws IOException {
        org.osgi.service.cm.Configuration config =
                ca.createFactoryConfiguration("org.apache.felix.jaas.Configuration.factory",null);
        Properties p = new Properties();
        p.setProperty("jaas.classname","org.apache.felix.jaas.integration.sample1.ConfigLoginModule");
        p.setProperty("jaas.realmName",realmName);
        config.update(p);
        return config.getPid();
    }
}
