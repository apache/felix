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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.felix.cm.PersistenceManager;
import org.apache.felix.cm.file.FilePersistenceManager;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

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
     * The name of the bundle context property defining the location for the
     * configuration files (value is "felix.cm.dir").
     *
     * @see #start(BundleContext)
     */
    private static final String CM_CONFIG_DIR = "felix.cm.dir";

    private volatile ConfigurationManager manager;

    // the service registration of the default file persistence manager
    private volatile ServiceRegistration<PersistenceManager> filepmRegistration;

    // service tracker for optional coordinator
    @SuppressWarnings("rawtypes")
    private volatile ServiceTracker coordinatorTracker;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void start( final BundleContext bundleContext )
    {
        // setup log
        Log.logger.start(bundleContext);

        // register default file persistence manager
        // set up the location (might throw IllegalArgumentException)
        DynamicBindings dynamicBindings = null;
        try
        {
            final FilePersistenceManager fpm = new FilePersistenceManager( bundleContext,
                    bundleContext.getProperty( CM_CONFIG_DIR ) );
            final Dictionary<String, Object> props = new Hashtable<String, Object>();
            props.put( Constants.SERVICE_DESCRIPTION, "Platform Filesystem Persistence Manager" );
            props.put( Constants.SERVICE_VENDOR, "The Apache Software Foundation" );
            props.put( Constants.SERVICE_RANKING, new Integer( Integer.MIN_VALUE ) );
            filepmRegistration = bundleContext.registerService( PersistenceManager.class, fpm, props );

            // setup dynamic configuration bindings
            dynamicBindings = new DynamicBindings( bundleContext, fpm );
        }
        catch ( IOException ioe )
        {
            Log.logger.log( LogService.LOG_ERROR, "Failure setting up dynamic configuration bindings", ioe );
        }
        catch ( IllegalArgumentException iae )
        {
            Log.logger.log( LogService.LOG_ERROR, "Cannot create the FilePersistenceManager", iae );
        }

        this.manager = new ConfigurationManager();
        // start coordinator tracker
        coordinatorTracker = new ServiceTracker(bundleContext, "org.osgi.service.coordinator.Coordinator",
                new ServiceTrackerCustomizer()
        {
            private final SortedMap<ServiceReference, Object> sortedServices = new TreeMap<ServiceReference, Object>();

            @Override
            public Object addingService(final ServiceReference reference)
            {
                final Object srv = bundleContext.getService(reference);
                if ( srv != null )
                {
                    synchronized ( this.sortedServices )
                    {
                        sortedServices.put(reference, srv);
                        manager.setCoordinator(sortedServices.get(sortedServices.lastKey()));
                    }
                }
                return srv;
            }

            @Override
            public void modifiedService(final ServiceReference reference, final Object srv) {
                synchronized ( this.sortedServices )
                {
                    // update the map, service ranking might have changed
                    sortedServices.remove(reference);
                    sortedServices.put(reference, srv);
                    manager.setCoordinator(sortedServices.get(sortedServices.lastKey()));
                }
            }

            @Override
            public void removedService(final ServiceReference reference, final Object service) {
                synchronized ( this.sortedServices )
                {
                    sortedServices.remove(reference);
                    if ( sortedServices.isEmpty() )
                    {
                        manager.setCoordinator(null);
                    }
                    else
                    {
                        manager.setCoordinator(sortedServices.get(sortedServices.lastKey()));
                    }
                }
                bundleContext.ungetService(reference);
            }
        });
        coordinatorTracker.open();
        // start configuration manager implementation
        final ServiceReference<ConfigurationAdmin> ref = this.manager.start(dynamicBindings, bundleContext);

        // update log
        Log.logger.set(ref);
    }


    @Override
    public void stop( final BundleContext bundleContext )
    {
        // stop logger
        Log.logger.stop();

        // stop configuration manager implementation
        if ( this.manager != null )
        {
            this.manager.stop(bundleContext);
            this.manager = null;
        }

        // stop coordinator tracker
        if ( this.coordinatorTracker != null )
        {
            this.coordinatorTracker.close();
            this.coordinatorTracker = null;
        }

        // shutdown the file persistence manager
        if ( filepmRegistration != null )
        {
            filepmRegistration.unregister();
            filepmRegistration = null;
        }
    }
}

