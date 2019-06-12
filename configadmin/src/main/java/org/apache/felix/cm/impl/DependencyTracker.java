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


import java.util.Arrays;

import org.apache.felix.cm.PersistenceManager;
import org.apache.felix.cm.impl.persistence.ExtPersistenceManager;
import org.apache.felix.cm.impl.persistence.PersistenceManagerTracker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.service.log.LogService;

/**
 * The dependency tracker keeps track of all required dependencies (services)
 * for the configuration admin and only activates the admin if everything is
 * satisfied.
 */
public class DependencyTracker
{
    /** The persistence manager tracker (optional) */
    private final PersistenceManagerTracker persistenceManagerTracker;

    /** The configuration plugin tracker (optional) */
    private final RequiredConfigurationPluginTracker configurationPluginTracker;

    private final ActivatorWorkerQueue workerQueue;

    private final ConfigurationAdminStarter starter;

    public DependencyTracker(final BundleContext bundleContext,
            final ServiceFactory<PersistenceManager> defaultFactory,
            final String pmName, final String[] pluginNames)
            throws BundleException, InvalidSyntaxException
    {
        this.starter = new ConfigurationAdminStarter(bundleContext);

        final boolean useQueue = pmName != null || pluginNames != null;
        if (useQueue) {
            this.workerQueue = new ActivatorWorkerQueue();
        } else {
            this.workerQueue = null;
        }
        if (pluginNames != null) {
            Log.logger.log(LogService.LOG_DEBUG, "Requiring configuration plugins {0}",
                    new Object[] { Arrays.toString(pluginNames) });
            this.configurationPluginTracker = new RequiredConfigurationPluginTracker(bundleContext, workerQueue,
                    starter, pluginNames);
        } else {
            this.configurationPluginTracker = null;
            if (useQueue) {
                starter.updatePluginsSet(true);
            }
        }

        if ( pmName != null )
        {
            Log.logger.log(LogService.LOG_DEBUG, "Using persistence manager {0}", new Object[] {pmName});
            this.persistenceManagerTracker = new PersistenceManagerTracker(bundleContext, workerQueue, starter, pmName);
        }
        else
        {
            this.persistenceManagerTracker = null;

            Log.logger.log(LogService.LOG_DEBUG, "Using default persistence manager", (Object[])null);
            PersistenceManager defaultPM = null;
            try {
                defaultPM = defaultFactory.getService(null, null);
            } catch (final IllegalArgumentException iae) {
                Log.logger.log(LogService.LOG_ERROR, "Cannot create the FilePersistenceManager", iae);
            }
            if (defaultPM == null) {
                throw new BundleException("Unable to register default persistence manager.");
            }

            final ExtPersistenceManager epm = PersistenceManagerTracker.createPersistenceManagerProxy(defaultPM);
            if (useQueue) {
                starter.setPersistenceManager(epm);
            } else {
                this.starter.activate(epm);
            }
        }
    }

    /**
     * Stop the tracker, stop configuration admin
     */
    public void stop( )
    {
        if (this.workerQueue != null) {
            this.workerQueue.stop();
        }
        this.starter.deactivate();
        if ( this.persistenceManagerTracker != null )
        {
            this.persistenceManagerTracker.stop();
        }
        if (this.configurationPluginTracker != null)
        {
            this.configurationPluginTracker.stop();
        }
    }
}

