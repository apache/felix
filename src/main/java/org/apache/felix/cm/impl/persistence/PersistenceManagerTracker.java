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
import java.util.Collections;
import java.util.List;

import org.apache.felix.cm.NotCachablePersistenceManager;
import org.apache.felix.cm.PersistenceManager;
import org.apache.felix.cm.impl.ActivatorWorkerQueue;
import org.apache.felix.cm.impl.ConfigurationAdminStarter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
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
    private final List<Holder> holders = new ArrayList<>();

    private final ServiceTracker<PersistenceManager, Holder> persistenceManagerTracker;

    private final BundleContext bundleContext;

    private final ActivatorWorkerQueue workerQueue;

    private final ConfigurationAdminStarter starter;

    public PersistenceManagerTracker(final BundleContext bundleContext,
            final ActivatorWorkerQueue workerQueue,
            final ConfigurationAdminStarter starter,
            final String pmName)
            throws BundleException, InvalidSyntaxException
    {
        this.workerQueue = workerQueue;
        this.starter = starter;
        this.bundleContext = bundleContext;
        this.persistenceManagerTracker = new ServiceTracker<>(bundleContext,
                    bundleContext.createFilter("(&(" + Constants.OBJECTCLASS + "=" + PersistenceManager.class.getName() + ")(name=" + pmName + "))"),
                     this );
        this.persistenceManagerTracker.open();
    }

    /**
     * Stop the tracker
     */
    public void stop( )
    {
        this.persistenceManagerTracker.close();
    }

    public static ExtPersistenceManager createPersistenceManagerProxy(final PersistenceManager pm)
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
                                starter.unsetPersistenceManager();
                            }
                            if (!holder.isActivated()) {
                                starter.setPersistenceManager(holder.getPersistenceManager());
                                holder.activate();
                            }
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
            if ( holders.get(0) == holder && oldHolder != null && oldHolder.compareTo(holder) != 0 )
            {
                this.workerQueue.enqueue(new Runnable()
                {

                    @Override
                    public void run()
                    {
                        starter.unsetPersistenceManager();
                        if (!holder.isActivated()) {
                            starter.setPersistenceManager(holder.getPersistenceManager());
                            holder.activate();
                        }
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
                        starter.unsetPersistenceManager();
                        if ( !holders.isEmpty() )
                        {
                            Holder h = holders.get(0);
                            if (!h.isActivated()) {
                                starter.setPersistenceManager(h.getPersistenceManager());
                                h.activate();
                            }
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

        // no need to synchronize, as it's changed only in WorkQueue tasks
        private boolean activated;

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

        public boolean isActivated() {
            return activated;
        }

        public void activate() {
            this.activated = true;
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
}

