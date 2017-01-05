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
 * a <code>Map</code> whose modification methods (like {@link #put(Object, Object)},
 * {@link #remove(Object)}, etc.) throw an {@link UnsupportedOperationException}.
 */
public class ReadOnlyDictionary extends Dictionary<String, Object>
    implements Map<String, Object>, Comparable<ReadOnlyDictionary>
{

    private final Hashtable<String, Object> m_delegate;

    private final ServiceReference<?> m_serviceReference;

    /**
     * Creates a wrapper for the given delegate dictionary providing read
     * only access to the data.
     */
    public ReadOnlyDictionary( final Map<String, Object> delegate )
    {
        if ( delegate instanceof Hashtable )
        {
            this.m_delegate = ( Hashtable<String, Object> ) delegate;
        }
        else
        {
            this.m_delegate = new Hashtable<String, Object>();
            for ( Map.Entry<String, Object> entry: delegate.entrySet() )
            {
                this.m_delegate.put( entry.getKey(), entry.getValue() );
            }
        }
        m_serviceReference = null;
    }

    /**
     * Creates a wrapper for the given service reference providing read only
     * access to the reference properties.
     */
    public ReadOnlyDictionary( final ServiceReference<?> serviceReference )
    {
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
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
        m_serviceReference = serviceReference;
    }


    //---------- Dictionary API

    @Override
    public Enumeration<Object> elements()
    {
        return m_delegate.elements();
    }

    @Override
    public Object get( final Object key )
    {
        return m_delegate.get( key );
    }


    @Override
    public boolean isEmpty()
    {
        return m_delegate.isEmpty();
    }


    @Override
    public Enumeration<String> keys()
    {
        return m_delegate.keys();
    }


    /**
     * This method has no effect and always returns <code>null</code> as this
     * instance is read-only and cannot modify and properties.
     */
    @Override
    public Object put( final String key, final Object value )
    {
        throw new UnsupportedOperationException();
    }


    /**
     * This method has no effect and always returns <code>null</code> as this
     * instance is read-only and cannot modify and properties.
     */
    @Override
    public Object remove( final Object key )
    {
        throw new UnsupportedOperationException();
    }


    @Override
    public int size()
    {
        return m_delegate.size();
    }


    @Override
    public String toString()
    {
        return m_delegate.toString();
    }


    //---------- Map API

    public void clear()
    {
        throw new UnsupportedOperationException();
    }


    public boolean containsKey( Object key )
    {
        return m_delegate.containsKey( key );
    }


    public boolean containsValue( Object value )
    {
        return m_delegate.containsValue( value );
    }


    public Set<Entry<String, Object>> entrySet()
    {
        return Collections.unmodifiableSet( m_delegate.entrySet() );
    }


    public Set<String> keySet()
    {
        return Collections.unmodifiableSet( m_delegate.keySet() );
    }


    public void putAll( Map<? extends String, ? extends Object> m )
    {
        throw new UnsupportedOperationException();
    }


    public Collection<Object> values()
    {
        return Collections.unmodifiableCollection( m_delegate.values() );
    }


    public int compareTo(final ReadOnlyDictionary o)
    {
        if ( m_serviceReference == null )
        {
            if ( o.m_serviceReference == null )
            {
                return 0;
            }
            return 1;
        }
        else if ( o.m_serviceReference == null )
        {
            return -1;
        }
        return m_serviceReference.compareTo(o.m_serviceReference);
    }

}
