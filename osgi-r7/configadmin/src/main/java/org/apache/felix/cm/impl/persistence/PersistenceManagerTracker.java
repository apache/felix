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


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.felix.cm.NotCachablePersistenceManager;
import org.apache.felix.cm.PersistenceManager;
import org.apache.felix.cm.impl.ConfigurationManager;
import org.apache.felix.cm.impl.Log;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * This tracker tracks registered persistence managers and
 * if the required PM becomes available, configuration admin
 * is registered.
 * Service ranking of registered persistence managers
 * is respected.
 */
public class PersistenceManagerTracker
    implements ServiceTrackerCustomizer<PersistenceManager, PersistenceManagerTracker.Holder>
{
    /** Tracker for the persistence manager. */
    private final ServiceTracker<PersistenceManager, Holder> persistenceManagerTracker;

    private final List<Holder> holders = new ArrayList<>();

    private final WorkerQueue workerQueue;

    private final BundleContext bundleContext;

    private volatile ConfigurationManager configurationManager;

    // service tracker for optional coordinator
    private volatile ServiceTracker<Object, Object> coordinatorTracker;

    public PersistenceManagerTracker(final BundleContext bundleContext,
            final PersistenceManager defaultPM,
            final String pmName )
    throws InvalidSyntaxException
    {
        this.bundleContext = bundleContext;
        if ( pmName != null )
        {
            Log.logger.log(LogService.LOG_DEBUG, "Using persistence manager {0}", new Object[] {pmName});
            this.workerQueue = new WorkerQueue();
            this.persistenceManagerTracker = new ServiceTracker<>( bundleContext,
                    bundleContext.createFilter("(&(" + Constants.OBJECTCLASS + "=" + PersistenceManager.class.getName() + ")(name=" + pmName + "))"),
                     this );
            this.persistenceManagerTracker.open();
        }
        else
        {
            Log.logger.log(LogService.LOG_DEBUG, "Using default persistence manager", (Object[])null);
            this.workerQueue = null;
            this.persistenceManagerTracker = null;
            this.activate(this.createPersistenceManagerProxy(defaultPM));
        }
    }

    /**
     * Stop the tracker, stop configuration admin
     */
    public void stop( )
    {
        if ( this.persistenceManagerTracker != null )
        {
            this.workerQueue.stop();
            this.deactivate();
            this.persistenceManagerTracker.close();
        }
        else
        {
            this.deactivate();
        }
    }

    private void activate(final ExtPersistenceManager pm)
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

    private void deactivate()
    {
        if ( this.configurationManager != null )
        {
            this.configurationManager.stop();
            this.configurationManager = null;
        }
        this.stopCoordinatorTracker();
        // update log
        Log.logger.set(null);
    }

    private ExtPersistenceManager createPersistenceManagerProxy(final PersistenceManager pm)
    {
        final ExtPersistenceManager extPM;
        if ( pm instanceof NotCachablePersistenceManager )
        {
            extPM = new PersistenceManagerProxy( pm );
        }
        else
        {
            extPM = new CachingPersistenceManagerProxy( pm );
        }
        return extPM;
    }

    @Override
    public Holder addingService(final ServiceReference<PersistenceManager> reference)
    {
        final PersistenceManager pm = this.bundleContext.getService(reference);
        if ( pm != null )
        {
            final ExtPersistenceManager extPM = createPersistenceManagerProxy(pm);
            final Holder holder = new Holder(reference, extPM);

            synchronized ( this.holders )
            {
                final Holder oldHolder = this.holders.isEmpty() ? null : this.holders.get(0);
                this.holders.add(holder);
                Collections.sort(holders);
                if ( holders.get(0) == holder )
                {
                    this.workerQueue.enqueue(new Runnable()
                    {

                        @Override
                        public void run()
                        {
                            if ( oldHolder != null )
                            {
                                deactivate();
                            }
                            activate(holder.getPersistenceManager());
                        }
                    });
                }
            }
            return holder;
        }
        return null;
    }


    @Override
    public void modifiedService(final ServiceReference<PersistenceManager> reference, final Holder holder)
    {
        // find the old holder, remove, add new holder, sort
        synchronized ( this.holders )
        {
            final Holder oldHolder = this.holders.isEmpty() ? null : this.holders.get(0);

            this.holders.remove(holder);
            this.holders.add(new Holder(reference, holder.getPersistenceManager()));
            Collections.sort(this.holders);
            if ( holders.get(0) == holder && oldHolder.compareTo(holder) != 0 )
            {
                this.workerQueue.enqueue(new Runnable()
                {

                    @Override
                    public void run()
                    {
                        deactivate();
                        activate(holder.getPersistenceManager());
                    }
                });
            }
        }

    }


    @Override
    public void removedService(final ServiceReference<PersistenceManager> reference,
            final Holder holder)
    {
        synchronized ( this.holders )
        {
            final boolean deactivate = holders.get(0) == holder;
            this.holders.remove(holder);
            if ( deactivate )
            {
                this.workerQueue.enqueue(new Runnable()
                {

                    @Override
                    public void run()
                    {
                        deactivate();
                        if ( !holders.isEmpty() )
                        {
                            activate(holders.get(0).getPersistenceManager());
                        }
                    }
                });
            }
        }
    }

    public static final class Holder implements Comparable<Holder>
    {
        private final ServiceReference<PersistenceManager> reference;

        private final ExtPersistenceManager manager;

        public Holder(final ServiceReference<PersistenceManager> ref, final ExtPersistenceManager epm)
        {
            this.reference = ref;
            this.manager = epm;
        }

        public ExtPersistenceManager getPersistenceManager()
        {
            return this.manager;
        }

        @Override
        public int compareTo(final Holder o)
        {
            // sort, highest first
            return -reference.compareTo(o.reference);
        }

        @Override
        public int hashCode()
        {
            return this.reference.hashCode();
        }

        @Override
        public boolean equals(final Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (obj == null || getClass() != obj.getClass())
            {
                return false;
            }
            final Holder other = (Holder) obj;
            return this.reference.equals(other.reference);
        }
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
}

