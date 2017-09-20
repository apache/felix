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
package org.apache.felix.cm.impl.persistence;


import org.apache.felix.cm.PersistenceManager;
import org.apache.felix.cm.impl.RankingComparator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;


public class PersistenceManagerTracker implements PersistenceManagerProvider, ServiceTrackerCustomizer<PersistenceManager, PersistenceManager>
{
    // PersistenceManager services
    private ServiceTracker<PersistenceManager, PersistenceManager> persistenceManagerTracker;

    /**
     * The actual list of {@link PersistenceManager persistence managers} to use
     * when looking for configuration data. This list is built from the
     * {@link #persistenceManagerMap}, which is ordered according to the
     * {@link RankingComparator}.
     */
    private ExtPersistenceManager[] persistenceManagers;

    private final BundleContext bundleContext;

    public PersistenceManagerTracker(final BundleContext bundleContext, final String pmName )
    {
        this.bundleContext = bundleContext;
        persistenceManagerTracker = new ServiceTracker<PersistenceManager, PersistenceManager>( bundleContext,
                "(&(" + Constants.OBJECTCLASS + "=" + PersistenceManager.class.getName() + ")(name=" + pmName + "))",
                 this );
        persistenceManagerTracker.open();
    }


    public void stop( )
    {
        persistenceManagerTracker.close();
    }


    @Override
    public PersistenceManager addingService(final ServiceReference<PersistenceManager> reference)
    {
        if ( persistenceManagers == null )
        {
            final PersistenceManager pm = this.bundleContext.getService(reference);
            if ( pm != null )
            {
                // activate
                return pm;
            }
        }
        return null;
    }


    @Override
    public void modifiedService(final ServiceReference<PersistenceManager> reference, final PersistenceManager service)
    {
        // nothing to do
    }


    @Override
    public void removedService(final ServiceReference<PersistenceManager> reference, final PersistenceManager service)
    {
        if ( persistenceManagers != null && persistenceManagers[0].getDelegatee() == service )
        {
            // deactivate
            persistenceManagers = null;
        }
    }


    @Override
    public ExtPersistenceManager[] getPersistenceManagers()
    {
        return persistenceManagers;
    }
}

