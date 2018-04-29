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
import java.util.Map;

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
    private final Hashtable cache;

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
        this.cache = new Hashtable();
    }


    /**
     * Remove the configuration with the given PID. This implementation removes
     * the entry from the cache before calling the underlying persistence
     * manager.
     */
    public void delete( String pid ) throws IOException
    {
        cache.remove( pid );
        pm.delete( pid );
    }


    /**
     * Checks whether a dictionary with the given pid exists. First checks for
     * the existence in the cache. If not in the cache the underlying
     * persistence manager is asked.
     */
    public boolean exists( String pid )
    {
        return cache.containsKey( pid ) || pm.exists( pid );
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
        // if not fully loaded, call back to the underlying persistence
        // manager and cach all dictionaries whose service.pid is set
        if ( !fullyLoaded )
        {
            Enumeration fromPm = pm.getDictionaries();
            while ( fromPm.hasMoreElements() )
            {
                Dictionary next = ( Dictionary ) fromPm.nextElement();
                String pid = ( String ) next.get( Constants.SERVICE_PID );
                if ( pid != null )
                {
                    cache.put( pid, next );
                }
            }
            fullyLoaded = true;
        }

        return new Enumeration()
        {
            final Enumeration base = cache.elements();


            public boolean hasMoreElements()
            {
                return base.hasMoreElements();
            }


            public Object nextElement()
            {
                return copy( ( Dictionary ) base.nextElement() );
            }
        };
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
        Dictionary loaded = ( Dictionary ) cache.get( pid );
        if ( loaded == null )
        {
            loaded = pm.load( pid );
            if ( loaded != null )
            {
                cache.put( pid, loaded );
            }
        }
        return copy( loaded );
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
        pm.store( pid, properties );
        cache.put( pid, copy( properties ) );
    }


    /**
     * Creates and returns a copy of the given dictionary. This method simply
     * copies all entries from the source dictionary to the newly created
     * target.
     */
    Dictionary copy( final Dictionary source )
    {
        Hashtable copy = new Hashtable();
        if ( source instanceof Map )
        {
            copy.putAll( ( Map ) source );
        }
        else
        {
            Enumeration keys = source.keys();
            while ( keys.hasMoreElements() )
            {
                Object key = keys.nextElement();
                copy.put( key, source.get( key ) );
            }
        }
        return copy;
    }
}
