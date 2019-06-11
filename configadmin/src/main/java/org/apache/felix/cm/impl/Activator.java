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
package org.apache.felix.cm.impl;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.cm.PersistenceManager;
import org.apache.felix.cm.file.FilePersistenceManager;
import org.apache.felix.cm.impl.persistence.MemoryPersistenceManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

/**
 * Activator for the configuration admin implementation.
 * When the bundle is started this activator:
 * <ul>
 *  <li>Sets up the logger {@link Log}.
 *  <li>A {@link FilePersistenceManager} instance is registered as a default
 * {@link PersistenceManager}.
 *  <li>Creates and sets up the {@link ConfigurationManager}.
 * </ul>
 * <p>
 * The default {@link FilePersistenceManager} is configured with a configuration
 * location taken from the <code>felix.cm.dir</code> framework property. If
 * this property is not set the <code>config</code> directory in the current
 * working directory as specified in the <code>user.dir</code> system property
 * is used.
 */
public class Activator implements BundleActivator
{

    /**
     * The name of the framework context property defining the location for the
     * configuration files (value is "felix.cm.dir").
     *
     * @see #start(BundleContext)
     */
    private static final String CM_CONFIG_DIR = "felix.cm.dir";

    /**
     * The name of the framework context property defining the persistence
     * manager to be used. If this property is not set or empty, the built-in
     * persistence manager (named file) is used. If it is specified it refers
     * to the name property of a persistence manager and that persistence manager
     * needs to be registered.
     *
     * @see #start(BundleContext)
     */
    private static final String CM_CONFIG_PM = "felix.cm.pm";

    /**
     * The name of the framework context property defining the required
     * configuration plugins. If this property is specified it refers to the
     * {@link RequiredConfigurationPluginTracker#PROPERTY_NAME} property of a
     * configuration plugin and that configuration plugin must be registered and
     * available.
     *
     * @see #start(BundleContext)
     */
    private static final String CM_CONFIG_PLUGINS = "felix.cm.config.plugins";

    private volatile DependencyTracker tracker;

    // the service registration of the default file persistence manager
    private volatile ServiceRegistration<PersistenceManager> filepmRegistration;

    // the service registration of the memory persistence manager
    private volatile ServiceRegistration<PersistenceManager> memorypmRegistration;

    @Override
    public void start( final BundleContext bundleContext ) throws BundleException
    {
        // setup log
        Log.logger.start(bundleContext);

        // register default file persistence manager
        final ServiceFactory<PersistenceManager> defaultFactory = this.registerFilePersistenceManager(bundleContext);

        // register memory persistence manager
        registerMemoryPersistenceManager(bundleContext);

        try
        {
            this.tracker = new DependencyTracker(bundleContext, defaultFactory,
                    getConfiguredPersistenceManager(bundleContext),
                    getConfiguredConfigurationPlugins(bundleContext));
        }
        catch ( InvalidSyntaxException iae )
        {
            Log.logger.log( LogService.LOG_ERROR, "Cannot create the persistence manager tracker", iae );
            throw new BundleException(iae.getMessage(), iae);
        }
    }

    private String getConfiguredPersistenceManager(final BundleContext bundleContext) {
        String configuredPM = bundleContext.getProperty(CM_CONFIG_PM);
        if (configuredPM != null && (configuredPM.isEmpty()
                || FilePersistenceManager.DEFAULT_PERSISTENCE_MANAGER_NAME.equals(configuredPM))) {
            configuredPM = null;
        }
        return configuredPM;
    }

    private String[] getConfiguredConfigurationPlugins(final BundleContext bundleContext) {
        String[] configuredPlugins = null;
        String configuredPls = bundleContext.getProperty(CM_CONFIG_PLUGINS);
        if (configuredPls != null) {
            final List<String> values = new ArrayList<>();
            configuredPlugins = configuredPls.split(",");
            for (int i = 0; i < configuredPlugins.length; i++) {
                final String v = configuredPlugins[i].trim();
                if (!v.isEmpty()) {
                    values.add(v);
                }
            }
            if (!values.isEmpty()) {
                configuredPlugins = values.toArray(new String[values.size()]);
            }
        }
        return configuredPlugins;
    }

    @Override
    public void stop( final BundleContext bundleContext )
    {
        // stop logger
        Log.logger.stop();

        // stop tracker and configuration manager implementation
        if ( this.tracker != null )
        {
            this.tracker.stop();
            this.tracker = null;
        }

        // shutdown the file and memory persistence manager and unregister
        this.unregisterFilePersistenceManager();
        this.unregisterMemoryPersistenceManager();
    }

    private ServiceFactory<PersistenceManager> registerFilePersistenceManager(final BundleContext bundleContext)
    {
        final Dictionary<String, Object> props = new Hashtable<>();
        props.put(Constants.SERVICE_DESCRIPTION, "Platform Filesystem Persistence Manager");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        props.put(Constants.SERVICE_RANKING, new Integer(Integer.MIN_VALUE));
        props.put(PersistenceManager.PROPERTY_NAME, FilePersistenceManager.DEFAULT_PERSISTENCE_MANAGER_NAME);

        final ServiceFactory<PersistenceManager> factory = new ServiceFactory<PersistenceManager>()
        {

            private volatile FilePersistenceManager fpm;

            @Override
            public PersistenceManager getService(Bundle bundle, ServiceRegistration<PersistenceManager> registration) {
                if (fpm == null) {
                    fpm = new FilePersistenceManager(bundleContext, bundleContext.getProperty(CM_CONFIG_DIR));
                }

                return fpm;
            }

            @Override
            public void ungetService(Bundle bundle, ServiceRegistration<PersistenceManager> registration,
                    PersistenceManager service) {
                // nothing to do
            }

        };
        filepmRegistration = bundleContext.registerService(PersistenceManager.class, factory, props);

        return factory;
    }

    private void registerMemoryPersistenceManager(final BundleContext bundleContext) {
        final MemoryPersistenceManager mpm = new MemoryPersistenceManager();
        final Dictionary<String, Object> props = new Hashtable<>();
        props.put(Constants.SERVICE_DESCRIPTION, "Platform Memory Persistence Manager");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        props.put(PersistenceManager.PROPERTY_NAME, "memory");
        memorypmRegistration = bundleContext.registerService(PersistenceManager.class, mpm, props);
    }

    private void unregisterFilePersistenceManager()
    {
        if ( this.filepmRegistration != null )
        {
            this.filepmRegistration.unregister();
            this.filepmRegistration = null;
        }
    }

    private void unregisterMemoryPersistenceManager() {
        if (this.memorypmRegistration != null) {
            this.memorypmRegistration.unregister();
            this.memorypmRegistration = null;
        }
    }

    public static String getLocation(final Bundle bundle)
    {
        if (System.getSecurityManager() != null)
        {
            return AccessController.doPrivileged(new PrivilegedAction<String>()
            {
                @Override
                public String run()
                {
                    return bundle.getLocation();
                }
            });
        }
        else
        {
            return bundle.getLocation();
        }
    }
}

