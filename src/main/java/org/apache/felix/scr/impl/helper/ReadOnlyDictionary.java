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
package org.apache.felix.scr.impl.helper;


import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.ServiceReference;


/**
 * The <code>ReadOnlyDictionary</code> is both a <code>Dictionary</code> and
 * a <code>Map</code> whose modificaiton methods (like {@link #put(Object, Object)},
 * {@link #remove(Object)}, etc.) have no effect.
 */
public class ReadOnlyDictionary<S, T> extends Dictionary<S, T> implements Map<S, T>
{

    private final Hashtable<S, T> m_delegate;


    /**
     * Creates a wrapper for the given delegate dictionary providing read
     * only access to the data.
     */
    public ReadOnlyDictionary( final Dictionary<S, T> delegate )
    {
        if ( delegate instanceof Hashtable )
        {
            this.m_delegate = ( Hashtable<S, T> ) delegate;
        }
        else
        {
            this.m_delegate = new Hashtable<S, T>();
            for ( Enumeration<S> ke = delegate.keys(); ke.hasMoreElements(); )
            {
                S key = ke.nextElement();
                this.m_delegate.put( key, delegate.get( key ) );
            }
        }
    }


    /**
     * Creates a wrapper for the given service reference providing read only
     * access to the reference properties.
     */
    public ReadOnlyDictionary( final ServiceReference serviceReference )
    {
        Hashtable properties = new Hashtable();
        final String[] keys = serviceReference.getPropertyKeys();
        if ( keys != null )
        {
            for ( int j = 0; j < keys.length; j++ )
            {
                final String key = keys[j];
                properties.put( key, serviceReference.getProperty( key ) );
            }
        }
        m_delegate = properties;
    }


    //---------- Dictionary API

    public Enumeration<T> elements()
    {
        return m_delegate.elements();
    }

    public T get( final Object key )
    {
        return m_delegate.get( key );
    }


    public boolean isEmpty()
    {
        return m_delegate.isEmpty();
    }


    public Enumeration<S> keys()
    {
        return m_delegate.keys();
    }


    /**
     * This method has no effect and always returns <code>null</code> as this
     * instance is read-only and cannot modify and properties.
     */
    public T put( final S key, final T value )
    {
        return null;
    }


    /**
     * This method has no effect and always returns <code>null</code> as this
     * instance is read-only and cannot modify and properties.
     */
    public T remove( final Object key )
    {
        return null;
    }


    public int size()
    {
        return m_delegate.size();
    }


    public String toString()
    {
        return m_delegate.toString();
    }


    //---------- Map API

    public void clear()
    {
        // nop, this map is read only
    }


    public boolean containsKey( Object key )
    {
        return m_delegate.containsKey( key );
    }


    public boolean containsValue( Object value )
    {
        return m_delegate.containsValue( value );
    }


    public Set entrySet()
    {
        return Collections.unmodifiableSet( m_delegate.entrySet() );
    }


    public Set keySet()
    {
        return Collections.unmodifiableSet( m_delegate.keySet() );
    }


    public void putAll( Map m )
    {
        // nop, this map is read only
    }


    public Collection values()
    {
        return Collections.unmodifiableCollection( m_delegate.values() );
    }
}
