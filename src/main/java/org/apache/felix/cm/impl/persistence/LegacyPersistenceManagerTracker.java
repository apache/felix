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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.felix.cm.NotCachablePersistenceManager;
import org.apache.felix.cm.PersistenceManager;
import org.apache.felix.cm.impl.RankingComparator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;


public class LegacyPersistenceManagerTracker implements PersistenceManagerProvider
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

    // the persistenceManagerTracker.getTrackingCount when the
    // persistenceManagers were last got
    private int pmtCount;

    public void start( final BundleContext bundleContext )
    {
        // get all persistence managers to begin with
        pmtCount = 1; // make sure to get the persistence managers at least once
        persistenceManagerTracker = new ServiceTracker<PersistenceManager, PersistenceManager>( bundleContext, PersistenceManager.class.getName(), null );
        persistenceManagerTracker.open();
    }


    public void stop( )
    {
        // don't care for PersistenceManagers any more
        persistenceManagerTracker.close();
    }

    @Override
    public ExtPersistenceManager[] getPersistenceManagers()
    {
        int currentPmtCount = persistenceManagerTracker.getTrackingCount();
        if ( persistenceManagers == null || currentPmtCount > pmtCount )
        {

            ExtPersistenceManager[] pm;

            ServiceReference<PersistenceManager>[] refs = persistenceManagerTracker.getServiceReferences();
            if ( refs == null || refs.length == 0 )
            {
                pm = new ExtPersistenceManager[0];
            }
            else
            {
                List<ExtPersistenceManager> pmList = new ArrayList<ExtPersistenceManager>();
                // sort the references according to the cmRanking property
                if ( refs.length > 1 )
                {
                    Arrays.sort( refs, RankingComparator.SRV_RANKING );
                }

                // create the service array from the sorted set of references
                for ( int i = 0; i < refs.length; i++ )
                {
                    PersistenceManager service = persistenceManagerTracker.getService( refs[i] );
                    if ( service != null )
                    {
                        // check for existing proxy
                        final ExtPersistenceManager pmProxy = getProxyForPersistenceManager(service);
                        if ( pmProxy != null )
                        {
                            pmList.add(pmProxy);
                        }
                        else
                        {
                            if ( service instanceof NotCachablePersistenceManager )
                            {
                                pmList.add( new PersistenceManagerProxy( service ) );
                            }
                            else
                            {
                                pmList.add(new CachingPersistenceManagerProxy(service));
                            }
                        }
                    }
                }

                pm = pmList.toArray( new ExtPersistenceManager[pmList.size()] );
            }

            pmtCount = currentPmtCount;
            persistenceManagers = pm;
        }

        return persistenceManagers;
    }

    private ExtPersistenceManager getProxyForPersistenceManager(final PersistenceManager pm)
    {
        if (persistenceManagers != null)
        {
            for (final ExtPersistenceManager pmProxy : persistenceManagers)
            {
                if (pmProxy.getDelegatee() == pm)
                {
                    return pmProxy;
                }
            }
        }
        return null;
    }
}

