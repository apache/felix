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
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.cm.impl.persistence.ExtPersistenceManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * The configuration admin starter starts and stops the configuration admin. It
 * also keeps track of an optional coordinator service.
 */
public class ConfigurationAdminStarter {

    private final BundleContext bundleContext;

    private volatile ConfigurationManager configurationManager;

    private volatile ExtPersistenceManager persistenceManager;

    private final AtomicBoolean pluginsAvailable = new AtomicBoolean(false);

    // service tracker for optional coordinator
    private volatile ServiceTracker<Object, Object> coordinatorTracker;

    public ConfigurationAdminStarter(final BundleContext bundleContext)
            throws BundleException, InvalidSyntaxException
    {
        this.bundleContext = bundleContext;
    }

    /**
     * Stop configuration admin
     */
    public void stop( )
    {
        this.deactivate();
    }

    public void activate(final ExtPersistenceManager pm)
    {
        try
        {
            configurationManager = new ConfigurationManager(pm, bundleContext);
            // start coordinator tracker
            this.startCoordinatorTracker();

            final ServiceReference<ConfigurationAdmin> ref = configurationManager.start();
            // update log
            Log.logger.set(ref);

        }
        catch (final IOException ioe )
        {
            Log.logger.log( LogService.LOG_ERROR, "Failure setting up dynamic configuration bindings", ioe );
        }
    }

    public void deactivate()
    {
        this.stopCoordinatorTracker();
        if ( this.configurationManager != null )
        {
            this.configurationManager.stop();
            this.configurationManager = null;
        }
        // update log
        Log.logger.set(null);
    }

    private void startCoordinatorTracker()
    {
        this.coordinatorTracker = new ServiceTracker<>(bundleContext, "org.osgi.service.coordinator.Coordinator",
                new ServiceTrackerCustomizer<Object, Object>()
        {
            private final SortedMap<ServiceReference<Object>, Object> sortedServices = new TreeMap<>();

            @Override
            public Object addingService(final ServiceReference<Object> reference)
            {
                final Object srv = bundleContext.getService(reference);
                if ( srv != null )
                {
                    synchronized ( this.sortedServices )
                    {
                        sortedServices.put(reference, srv);
                        configurationManager.setCoordinator(sortedServices.get(sortedServices.lastKey()));
                    }
                }
                return srv;
            }

            @Override
            public void modifiedService(final ServiceReference<Object> reference, final Object srv) {
                synchronized ( this.sortedServices )
                {
                    // update the map, service ranking might have changed
                    sortedServices.remove(reference);
                    sortedServices.put(reference, srv);
                    configurationManager.setCoordinator(sortedServices.get(sortedServices.lastKey()));
                }
            }

            @Override
            public void removedService(final ServiceReference<Object> reference, final Object service) {
                synchronized ( this.sortedServices )
                {
                    sortedServices.remove(reference);
                    if ( sortedServices.isEmpty() )
                    {
                        configurationManager.setCoordinator(null);
                    }
                    else
                    {
                        configurationManager.setCoordinator(sortedServices.get(sortedServices.lastKey()));
                    }
                }
                bundleContext.ungetService(reference);
            }
        });
        coordinatorTracker.open();
    }

    private void stopCoordinatorTracker()
    {
        if ( this.coordinatorTracker != null )
        {
            this.coordinatorTracker.close();
            this.coordinatorTracker = null;
        }
    }

    public void unsetPersistenceManager() {
        this.persistenceManager = null;
        this.deactivate();
    }

    public void setPersistenceManager(final ExtPersistenceManager persistenceManager) {
        this.persistenceManager = persistenceManager;
        this.checkStart();
    }

    public void updatePluginsSet(final boolean value) {
        if (this.pluginsAvailable.compareAndSet(!value, value)) {
            if (!value) {
                this.deactivate();
            } else {
                this.checkStart();
            }
        }
    }

    public void checkStart() {
        if (this.pluginsAvailable.get() && this.persistenceManager != null) {
            this.activate(this.persistenceManager);
        }
    }
}

