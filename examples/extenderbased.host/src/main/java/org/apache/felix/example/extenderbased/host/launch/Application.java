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
package org.apache.felix.example.extenderbased.host.launch;

import java.util.Map;
import java.util.ServiceLoader;
import org.apache.felix.example.extenderbased.host.Activator;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/**
 * This class provides a static {@code main()} method so that the bundle can be
 * run as a stand-alone host application. In such a scenario, the application
 * creates its own embedded OSGi framework instance and interacts with the
 * internal extensions to providing drawing functionality. To successfully
 * launch the stand-alone application, it must be run from this bundle's
 * installation directory using "{@code java -jar}".
 * The locations of any additional extensions that have to be started, have to
 * be passed as command line arguments to this method.
 */
public class Application
{
    private static Framework m_framework = null;

    /**
     * Enables the bundle to run as a stand-alone application. When this
     * static {@code main()} method is invoked, the application creates
     * its own embedded OSGi framework instance and interacts with the
     * internal extensions to provide drawing functionality. To successfully
     * launch as a stand-alone application, this method should be invoked from
     * the bundle's installation directory using "{@code java -jar}".
     * The location of any extension that shall be installed can be passed
     * as parameters.
     * <p>
     * For example if you build the bundles inside your workspace, maven will
     * create a target directory in every project. To start the application
     * from within your IDE you should pass:
     * <p>
     * <pre>
     * {@code file:../extenderbased.circle/target/extenderbased.circle-1.0.0.jar
     * file:../extenderbased.square/target/extenderbased.square-1.0.0.jar
     * file:../extenderbased.triangle/target/extenderbased.triangle-1.0.0.jar}
     * </pre>
     *
     * @param args The locations of additional bundles to start.
    **/
    public static void main(String[] args)
    {
        // args should never be null if the application is run from the command line. Check it anyway.
        String[] locations = args != null ? args : new String[0];

        // Print welcome banner.
        System.out.println("\nWelcome to My Launcher");
        System.out.println("======================\n");

        try
        {
            Map<String, String> config = ConfigUtil.createConfig();
            m_framework = createFramework(config);
            m_framework.init();
            m_framework.start();
            installAndStartBundles(locations);
            m_framework.waitForStop(0);
            System.exit(0);
        }
        catch (Exception ex)
        {
            System.err.println("Could not create framework: " + ex);
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Util method for creating an embedded Framework. Tries to create a {@link FrameworkFactory}
     * which is then be used to create the framework.
     *
     * @param config the configuration to create the framework with
     * @return a Framework with the given configuration
     */
    private static Framework createFramework(Map<String, String> config)
    {
        ServiceLoader<FrameworkFactory> factoryLoader = ServiceLoader.load(FrameworkFactory.class);
        for(FrameworkFactory factory : factoryLoader){
            return factory.newFramework(config);
        }
        throw new IllegalStateException("Unable to load FrameworkFactory service.");
    }

    /**
     * Installs and starts all bundles used by the application. Therefore the host bundle will be started. The locations
     * of extensions for the host bundle can be passed in as parameters.
     *
     * @param bundleLocations the locations where extension for the host bundle are located. Must not be {@code null}!
     * @throws BundleException if something went wrong while installing or starting the bundles.
     */
    private static void installAndStartBundles(String... bundleLocations) throws BundleException
    {
        BundleContext bundleContext = m_framework.getBundleContext();
        Activator hostActivator = new Activator();
        hostActivator.start(bundleContext);
        for (String location : bundleLocations)
        {
            Bundle addition = bundleContext.installBundle(location);
            addition.start();
        }
    }
}