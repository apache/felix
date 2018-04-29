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

<<<<<<< HEAD

=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.ServiceReference;

<<<<<<< HEAD

/**
 * The <code>ReadOnlyDictionary</code> is both a <code>Dictionary</code> and
 * a <code>Map</code> whose modificaiton methods (like {@link #put(Object, Object)},
 * {@link #remove(Object)}, etc.) have no effect.
 */
public class ReadOnlyDictionary<S, T> extends Dictionary<S, T> implements Map<S, T>
{

    private final Hashtable<S, T> m_delegate;

=======
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
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368

    /**
     * Creates a wrapper for the given delegate dictionary providing read
     * only access to the data.
     */
<<<<<<< HEAD
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


=======
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

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    /**
     * Creates a wrapper for the given service reference providing read only
     * access to the reference properties.
     */
<<<<<<< HEAD
    public ReadOnlyDictionary( final ServiceReference serviceReference )
    {
        Hashtable properties = new Hashtable();
=======
    public ReadOnlyDictionary( final ServiceReference<?> serviceReference )
    {
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
=======
        m_serviceReference = serviceReference;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }


    //---------- Dictionary API

<<<<<<< HEAD
    public Enumeration<T> elements()
=======
    @Override
    public Enumeration<Object> elements()
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        return m_delegate.elements();
    }

<<<<<<< HEAD
    public T get( final Object key )
=======
    @Override
    public Object get( final Object key )
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        return m_delegate.get( key );
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public boolean isEmpty()
    {
        return m_delegate.isEmpty();
    }


<<<<<<< HEAD
    public Enumeration<S> keys()
=======
    @Override
    public Enumeration<String> keys()
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        return m_delegate.keys();
    }


    /**
     * This method has no effect and always returns <code>null</code> as this
     * instance is read-only and cannot modify and properties.
     */
<<<<<<< HEAD
    public T put( final S key, final T value )
    {
        return null;
=======
    @Override
    public Object put( final String key, final Object value )
    {
        throw new UnsupportedOperationException();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }


    /**
     * This method has no effect and always returns <code>null</code> as this
     * instance is read-only and cannot modify and properties.
     */
<<<<<<< HEAD
    public T remove( final Object key )
    {
        return null;
    }


=======
    @Override
    public Object remove( final Object key )
    {
        throw new UnsupportedOperationException();
    }


    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public int size()
    {
        return m_delegate.size();
    }


<<<<<<< HEAD
=======
    @Override
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    public String toString()
    {
        return m_delegate.toString();
    }


    //---------- Map API

    public void clear()
    {
<<<<<<< HEAD
        // nop, this map is read only
=======
        throw new UnsupportedOperationException();
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }


    public boolean containsKey( Object key )
    {
        return m_delegate.containsKey( key );
    }


    public boolean containsValue( Object value )
    {
        return m_delegate.containsValue( value );
    }


<<<<<<< HEAD
    public Set entrySet()
=======
    public Set<Entry<String, Object>> entrySet()
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        return Collections.unmodifiableSet( m_delegate.entrySet() );
    }


<<<<<<< HEAD
    public Set keySet()
=======
    public Set<String> keySet()
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    {
        return Collections.unmodifiableSet( m_delegate.keySet() );
    }


<<<<<<< HEAD
    public void putAll( Map m )
    {
        // nop, this map is read only
    }


    public Collection values()
    {
        return Collections.unmodifiableCollection( m_delegate.values() );
    }
=======
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

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
}
