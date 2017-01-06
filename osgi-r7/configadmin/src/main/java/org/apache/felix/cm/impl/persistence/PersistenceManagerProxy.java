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
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.felix.cm.PersistenceManager;
import org.apache.felix.cm.impl.CaseInsensitiveDictionary;
import org.apache.felix.cm.impl.SimpleFilter;
import org.osgi.framework.Constants;

/**
 * The <code>PersistenceManagerProxy</code> proxies a persistence
 * manager and adds a read/write lock.
 */
public class PersistenceManagerProxy implements ExtPersistenceManager
{
    /** the actual PersistenceManager */
    private final PersistenceManager pm;

    /** protecting lock */
    private final ReadWriteLock globalLock = new ReentrantReadWriteLock();

    /**
     * Creates a new proxy for the given actual {@link PersistenceManager}.
     * @param pm The actual {@link PersistenceManager}
     */
    public PersistenceManagerProxy( final PersistenceManager pm )
    {
        this.pm = pm;
    }

    /**
     * Remove the configuration with the given PID. This implementation removes
     * the entry from the cache before calling the underlying persistence
     * manager.
     */
    @Override
    public void delete( String pid ) throws IOException
    {
        Lock lock = globalLock.writeLock();
        try
        {
            lock.lock();
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
    @Override
    public boolean exists( String pid )
    {
        Lock lock = globalLock.readLock();
        try
        {
            lock.lock();
            return pm.exists( pid );
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
    @Override
    public Enumeration getDictionaries() throws IOException
    {
        return Collections.enumeration(getDictionaries( null ));
    }

    @Override
    public Collection<Dictionary> getDictionaries( final SimpleFilter filter ) throws IOException
    {
        Lock lock = globalLock.readLock();
        try
        {
            final Set<String> pids = new HashSet<String>();
            final List<Dictionary> result = new ArrayList<Dictionary>();

            lock.lock();
            Enumeration fromPm = pm.getDictionaries();
            while ( fromPm.hasMoreElements() )
            {
                Dictionary next = (Dictionary) fromPm.nextElement();
                String pid = (String) next.get( Constants.SERVICE_PID );
                if ( pid != null && !pids.contains(pid) && ( filter == null || filter.matches( next ) ) )
                {
                    pids.add(pid);
                    result.add(  new CaseInsensitiveDictionary( next ) );
                }
            }

            return result;
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
    @Override
    public Dictionary load( String pid ) throws IOException
    {
        Lock lock = globalLock.readLock();
        try
        {
            lock.lock();
            Dictionary loaded = pm.load( pid );
            if ( loaded != null )
            {
                return new CaseInsensitiveDictionary( loaded );
            }
            return null;
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
    @Override
    public void store( String pid, Dictionary properties ) throws IOException
    {
        Lock lock = globalLock.writeLock();
        try
        {
            lock.lock();
            pm.store( pid, properties );
        }
        finally
        {
            lock.unlock();
        }
    }
}
