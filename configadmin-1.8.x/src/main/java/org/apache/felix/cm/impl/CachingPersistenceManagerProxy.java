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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.felix.cm.NotCachablePersistenceManager;
import org.apache.felix.cm.PersistenceManager;
import org.osgi.framework.Constants;


/**
 * The <code>CachingPersistenceManagerProxy</code> adds a caching layer to the
 * underlying actual {@link PersistenceManager} implementation. All API calls
 * are also (or primarily) routed through a local cache of dictionaries indexed
 * by the <code>service.pid</code>.
 */
class CachingPersistenceManagerProxy implements PersistenceManager
{
    /** the actual PersistenceManager */
    private final PersistenceManager pm;

    /** cached dictionaries */
    private final Hashtable<String, CaseInsensitiveDictionary> cache;

    /** protecting lock */
    private final ReadWriteLock globalLock = new ReentrantReadWriteLock();

    /**
     * Indicates whether the getDictionaries method has already been called
     * and the cache is complete with respect to the contents of the underlying
     * persistence manager.
     */
    private boolean fullyLoaded;


    /**
     * Creates a new caching layer for the given actual {@link PersistenceManager}.
     * @param pm The actual {@link PersistenceManager}
     */
    public CachingPersistenceManagerProxy( final PersistenceManager pm )
    {
        this.pm = pm;
        this.cache = new Hashtable<String, CaseInsensitiveDictionary>();
    }


    /**
     * Remove the configuration with the given PID. This implementation removes
     * the entry from the cache before calling the underlying persistence
     * manager.
     */
    public void delete( String pid ) throws IOException
    {
        Lock lock = globalLock.writeLock();
        try
        {
            lock.lock();
            cache.remove( pid );
            pm.delete(pid);
        }
        finally
        {
            lock.unlock();
        }
    }


    /**
     * Checks whether a dictionary with the given pid exists. First checks for
     * the existence in the cache. If not in the cache the underlying
     * persistence manager is asked.
     */
    public boolean exists( String pid )
    {
        Lock lock = globalLock.readLock();
        try
        {
            lock.lock();
            return cache.containsKey( pid ) || ( !fullyLoaded && pm.exists( pid ) );
        }
        finally
        {
            lock.unlock();
        }
    }


    /**
     * Returns an <code>Enumeration</code> of <code>Dictionary</code> objects
     * representing the configurations stored in the underlying persistence
     * managers. The dictionaries returned are garanteed to contain the
     * <code>service.pid</code> property.
     * <p>
     * Note, that each call to this method will return new dictionary objects.
     * That is modifying the contents of a dictionary returned from this method
     * has no influence on the dictionaries stored in the cache.
     */
    public Enumeration getDictionaries() throws IOException
    {
        return getDictionaries( null );
    }

    public Enumeration getDictionaries( SimpleFilter filter ) throws IOException
    {
        Lock lock = globalLock.readLock();
        try
        {
            lock.lock();
            boolean fullyLoaded = this.fullyLoaded;
            if ( pm instanceof NotCachablePersistenceManager )
            {
                fullyLoaded = false;
            }
            // if not fully loaded, call back to the underlying persistence
            // manager and cach all dictionaries whose service.pid is set
            if ( !fullyLoaded )
            {
                lock.unlock();
                lock = globalLock.writeLock();
                lock.lock();
                if ( !fullyLoaded )
                {
                    Enumeration fromPm = pm.getDictionaries();
                    while ( fromPm.hasMoreElements() )
                    {
                        Dictionary next = (Dictionary) fromPm.nextElement();
                        String pid = (String) next.get( Constants.SERVICE_PID );
                        if ( pid != null )
                        {
                            cache.put( pid, copy( next ) );
                        }
                        else
                        {
                            pid = (String) next.get( Factory.FACTORY_PID );
                            if ( pid != null )
                            {
                                pid = Factory.factoryPidToIdentifier( pid );
                                cache.put( pid, copy( next ) );
                            }
                        }
                    }
                    this.fullyLoaded = true;
                }
            }

            // Deep copy the configuration to avoid any threading issue
            Vector<Dictionary> configs = new Vector<Dictionary>();
            for (Dictionary d : cache.values())
            {
                if ( d.get( Constants.SERVICE_PID ) != null && ( filter == null || filter.matches( d ) ) )
                {
                    configs.add( copy( d ) );
                }
            }
            return configs.elements();
        }
        finally
        {
            lock.unlock();
        }
    }


    /**
     * Returns the dictionary for the given PID or <code>null</code> if no
     * such dictionary is stored by the underyling persistence manager. This
     * method caches the returned dictionary for future use after retrieving
     * if from the persistence manager.
     * <p>
     * Note, that each call to this method will return new dictionary instance.
     * That is modifying the contents of a dictionary returned from this method
     * has no influence on the dictionaries stored in the cache.
     */
    public Dictionary load( String pid ) throws IOException
    {
        Lock lock = globalLock.readLock();
        try
        {
            lock.lock();
            Dictionary loaded = cache.get( pid );
            if ( loaded == null && !fullyLoaded )
            {
                lock.unlock();
                lock = globalLock.writeLock();
                lock.lock();
                loaded = cache.get( pid );
                if ( loaded == null )
                {
                    loaded = pm.load( pid );
                    cache.put( pid, copy( loaded ) );
                }
            }
            return copy( loaded );
        }
        finally
        {
            lock.unlock();
        }
    }


    /**
     * Stores the dictionary in the cache and in the underlying persistence
     * manager. This method first calls the underlying persistence manager
     * before updating the dictionary in the cache.
     * <p>
     * Note, that actually a copy of the dictionary is stored in the cache. That
     * is subsequent modification to the given dictionary has no influence on
     * the cached data.
     */
    public void store( String pid, Dictionary properties ) throws IOException
    {
        Lock lock = globalLock.writeLock();
        try
        {
            lock.lock();
            pm.store( pid, properties );
            cache.put( pid, copy( properties ) );
        }
        finally
        {
            lock.unlock();
        }
    }


    /**
     * Creates and returns a copy of the given dictionary. This method simply
     * copies all entries from the source dictionary to the newly created
     * target.
     */
    CaseInsensitiveDictionary copy( final Dictionary source )
    {
        return new CaseInsensitiveDictionary( source );
    }
}
