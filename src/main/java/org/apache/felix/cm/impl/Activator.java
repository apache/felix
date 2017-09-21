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

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.cm.PersistenceManager;
import org.apache.felix.cm.file.FilePersistenceManager;
import org.apache.felix.cm.impl.persistence.PersistenceManagerTracker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
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
     * manager to be used. If not specified, the old behaviour is used
     * and all available pms are used
     *
     * @see #start(BundleContext)
     */
    private static final String CM_CONFIG_PM = "felix.cm.pm";

    private volatile PersistenceManagerTracker tracker;

    // the service registration of the default file persistence manager
    private volatile ServiceRegistration<PersistenceManager> filepmRegistration;

    @Override
    public void start( final BundleContext bundleContext ) throws BundleException
    {
        // setup log
        Log.logger.start(bundleContext);

        // register default file persistence manager
        final PersistenceManager defaultPM = this.registerFilePersistenceManager(bundleContext);
        if ( defaultPM == null )
        {
            throw new BundleException("Unable to register default persistence manager.");
        }

        String configuredPM = bundleContext.getProperty(CM_CONFIG_PM);
        if ( configuredPM != null && configuredPM.isEmpty() )
        {
            configuredPM = null;
        }
        try
        {
            this.tracker = new PersistenceManagerTracker(bundleContext, defaultPM, configuredPM);
        }
        catch ( InvalidSyntaxException iae )
        {
            Log.logger.log( LogService.LOG_ERROR, "Cannot create the persistence manager tracker", iae );
            throw new BundleException(iae.getMessage(), iae);
        }
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

        // shutdown the file persistence manager and unregister
        this.unregisterFilePersistenceManager();
    }

    private PersistenceManager registerFilePersistenceManager(final BundleContext bundleContext)
    {
        try
        {
            final FilePersistenceManager fpm = new FilePersistenceManager( bundleContext,
                    bundleContext.getProperty( CM_CONFIG_DIR ) );
            final Dictionary<String, Object> props = new Hashtable<>();
            props.put( Constants.SERVICE_DESCRIPTION, "Platform Filesystem Persistence Manager" );
            props.put( Constants.SERVICE_VENDOR, "The Apache Software Foundation" );
            props.put( Constants.SERVICE_RANKING, new Integer( Integer.MIN_VALUE ) );
            props.put( PersistenceManager.PROPERTY_NAME, FilePersistenceManager.DEFAULT_PERSISTENCE_MANAGER_NAME);
            filepmRegistration = bundleContext.registerService( PersistenceManager.class, fpm, props );

            return fpm;

        }
        catch ( final IllegalArgumentException iae )
        {
            Log.logger.log( LogService.LOG_ERROR, "Cannot create the FilePersistenceManager", iae );
        }
        return null;
    }

    private void unregisterFilePersistenceManager()
    {
        if ( this.filepmRegistration != null )
        {
            this.filepmRegistration.unregister();
            this.filepmRegistration = null;
        }
    }
}

