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


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;


/**
 * The <code>CaseInsensitiveDictionary</code> is a
 * <code>java.util.Dictionary</code> which conforms to the requirements laid
 * out by the Configuration Admin Service Specification requiring the property
 * names to keep case but to ignore case when accessing the properties.
 */
public class CaseInsensitiveDictionary extends Dictionary<String, Object>
{

    /**
     * The backend dictionary with lower case keys.
     */
    private SortedMap<String, Object> internalMap;

    public CaseInsensitiveDictionary()
    {
        internalMap = new TreeMap<String, Object>( CASE_INSENSITIVE_ORDER );
    }


    public CaseInsensitiveDictionary( Dictionary props )
    {
        if ( props instanceof CaseInsensitiveDictionary)
        {
            internalMap = new TreeMap<String, Object>( ((CaseInsensitiveDictionary) props).internalMap );
        }
        else if ( props != null )
        {
            internalMap = new TreeMap<String, Object>( CASE_INSENSITIVE_ORDER );
            Enumeration keys = props.keys();
            while ( keys.hasMoreElements() )
            {
                Object key = keys.nextElement();

                // check the correct syntax of the key
                String k = checkKey( key );

                // check uniqueness of key
                if ( internalMap.containsKey( k ) )
                {
                    throw new IllegalArgumentException( "Key [" + key + "] already present in different case" );
                }

                // check the value
                Object value = props.get( key );
                value = checkValue( value );

                // add the key/value pair
                internalMap.put( k, value );
            }
        }
        else
        {
            internalMap = new TreeMap<String, Object>( CASE_INSENSITIVE_ORDER );
        }
    }


    CaseInsensitiveDictionary( CaseInsensitiveDictionary props, boolean deepCopy )
    {
        if ( deepCopy )
        {
            internalMap = new TreeMap<String, Object>( CASE_INSENSITIVE_ORDER );
            for( Map.Entry<String, Object> entry : props.internalMap.entrySet() )
            {
                Object value = entry.getValue();
                if ( value.getClass().isArray() )
                {
                    // copy array
                    int length = Array.getLength( value );
                    Object newValue = Array.newInstance( value.getClass().getComponentType(), length );
                    System.arraycopy( value, 0, newValue, 0, length );
                    value = newValue;
                }
                else if ( value instanceof Collection )
                {
                    // copy collection, create Vector
                    // a Vector is created because the R4 and R4.1 specs
                    // state that the values must be simple, array or
                    // Vector. And even though we accept Collection nowadays
                    // there might be clients out there still written against
                    // R4 and R4.1 spec expecting Vector
                    value = new Vector<Object>( ( Collection ) value );
                }
                internalMap.put( entry.getKey(), value );
            }
        }
        else
        {
            internalMap = new TreeMap<String, Object>( props.internalMap );
        }
    }


    /*
     * (non-Javadoc)
     *
     * @see java.util.Dictionary#elements()
     */
    @Override
    public Enumeration<Object> elements()
    {
        return Collections.enumeration( internalMap.values() );
    }


    /*
     * (non-Javadoc)
     *
     * @see java.util.Dictionary#get(java.lang.Object)
     */
    @Override
    public Object get( Object key )
    {
        if ( key == null )
        {
            throw new NullPointerException( "key" );
        }

        return internalMap.get( key );
    }


    /*
     * (non-Javadoc)
     *
     * @see java.util.Dictionary#isEmpty()
     */
    @Override
    public boolean isEmpty()
    {
        return internalMap.isEmpty();
    }


    /*
     * (non-Javadoc)
     *
     * @see java.util.Dictionary#keys()
     */
    @Override
    public Enumeration<String> keys()
    {
        return Collections.enumeration( internalMap.keySet() );
    }


    /*
     * (non-Javadoc)
     *
     * @see java.util.Dictionary#put(java.lang.String, java.lang.Object)
     */
    @Override
    public Object put( String key, Object value )
    {
        if ( key == null || value == null )
        {
            throw new NullPointerException( "key or value" );
        }

        checkKey( key );
        value = checkValue( value );

        return internalMap.put( key, value );
    }


    /*
     * (non-Javadoc)
     *
     * @see java.util.Dictionary#remove(java.lang.Object)
     */
    @Override
    public Object remove( Object key )
    {
        if ( key == null )
        {
            throw new NullPointerException( "key" );
        }

        return internalMap.remove( key );
    }


    /*
     * (non-Javadoc)
     *
     * @see java.util.Dictionary#size()
     */
    @Override
    public int size()
    {
        return internalMap.size();
    }


    // ---------- internal -----------------------------------------------------

    /**
     * Ensures the <code>key</code> complies with the <em>symbolic-name</em>
     * production of the OSGi core specification (1.3.2):
     *
     * <pre>
     * symbolic-name :: = token('.'token)*
     * digit    ::= [0..9]
     * alpha    ::= [a..zA..Z]
     * alphanum ::= alpha | digit
     * token    ::= ( alphanum | ’_’ | ’-’ )+
     * </pre>
     *
     * If the key does not comply an <code>IllegalArgumentException</code> is
     * thrown.
     *
     * @param keyObject
     *            The configuration property key to check.
     * @throws IllegalArgumentException
     *             if the key does not comply with the symbolic-name production.
     */
    static String checkKey( Object keyObject )
    {
        // check for wrong type or null key
        if ( !( keyObject instanceof String ) )
        {
            throw new IllegalArgumentException( "Key [" + keyObject + "] must be a String" );
        }

        String key = ( String ) keyObject;

        // check for empty string
        if ( key.length() == 0 )
        {
            throw new IllegalArgumentException( "Key [" + key + "] must not be an empty string" );
        }

        return key;
    }


    private static final Set<Class> KNOWN = new HashSet<Class>(Arrays.<Class>asList(
            String.class, Integer.class, Long.class, Float.class,
            Double.class, Byte.class, Short.class, Character.class,
            Boolean.class));

    static Object checkValue( Object value )
    {
        if ( value == null )
        {
            // null is illegal
            throw new IllegalArgumentException( "Value must not be null" );

        }

        Class type = value.getClass();
        // Fast check for simple types
        if ( KNOWN.contains( type ) )
        {
            return value;
        }
        else if ( type.isArray() )
        {
            // check simple or primitive
            type = value.getClass().getComponentType();

            // check for primitive type (simple types are checked below)
            // note: void[] cannot be created, so we ignore this here
            if ( type.isPrimitive() )
            {
                return value;
            }

        }
        else if ( value instanceof Collection )
        {
            // check simple
            Collection collection = ( Collection ) value;
            if ( collection.isEmpty() )
            {
                throw new IllegalArgumentException( "Collection must not be empty" );
            }

            // ensure all elements have the same type and to internal list
            Collection<Object> internalValue = new ArrayList<Object>( collection.size() );
            type = null;
            for ( Object el : collection )
            {
                if ( el == null )
                {
                    throw new IllegalArgumentException( "Collection must not contain null elements" );
                }
                if ( type == null )
                {
                    type = el.getClass();
                }
                else if ( type != el.getClass() )
                {
                    throw new IllegalArgumentException( "Collection element types must not be mixed" );
                }
                internalValue.add( el );
            }
            value = internalValue;
        }
        else
        {
            // get the type to check (must be simple)
            type = value.getClass();

        }

        // check for simple type
        if ( KNOWN.contains( type ) )
        {
            return value;
        }

        // not a valid type
        throw new IllegalArgumentException( "Value [" + value + "] has unsupported (base-) type " + type );
    }


    // ---------- Object Overwrites --------------------------------------------

    @Override
    public String toString()
    {
        return internalMap.toString();
    }

    @Override
    public int hashCode()
    {
        return internalMap.hashCode();
    }

    @Override
    public synchronized boolean equals(final Object o)
    {
        if (o == this)
        {
            return true;
        }

        if (!(o instanceof Dictionary))
        {
            return false;
        }

        @SuppressWarnings("unchecked")
        final Dictionary<String,Object> t = (Dictionary<String,Object>) o;
        if (t.size() != size())
        {
            return false;
        }

        try
        {
            final Enumeration<String> keys = keys();
            while ( keys.hasMoreElements() )
            {
                final String key = keys.nextElement();
                final Object value = get(key);

                if (!value.equals(t.get(key)))
                {
                        return false;
                }
            }
        }
        catch (ClassCastException unused)
        {
            return false;
        }
        catch (NullPointerException unused)
        {
            return false;
        }

        return true;
    }

    public static Dictionary<String, Object> unmodifiable(Dictionary<String, Object> dict) {
        return new UnmodifiableDictionary(dict);
    }

    public static final class UnmodifiableDictionary extends Dictionary<String, Object>
    {
        private final Dictionary<String, Object> delegatee;

        public UnmodifiableDictionary(final Dictionary<String, Object> delegatee)
        {
            this.delegatee = delegatee;
        }

        @Override
        public Object put(String key, Object value)
        {
            // prevent put
            return null;
        }

        @Override
        public Object remove(Object key)
        {
            // prevent remove
            return null;
        }

        @Override
        public int hashCode()
        {
            return delegatee.hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            return delegatee.equals(obj);
        }

        @Override
        public String toString()
        {
            return delegatee.toString();
        }

        @Override
        public int size()
        {
            return delegatee.size();
        }

        @Override
        public boolean isEmpty()
        {
            return delegatee.isEmpty();
        }

        @Override
        public Enumeration<String> keys()
        {
            return delegatee.keys();
        }

        @Override
        public Enumeration<Object> elements()
        {
            return delegatee.elements();
        }

        @Override
        public Object get(Object key)
        {
            return delegatee.get(key);
        }
    }

    public static final Comparator<String> CASE_INSENSITIVE_ORDER = new CaseInsensitiveComparator();

    private static class CaseInsensitiveComparator implements Comparator<String>
    {

        public int compare(String s1, String s2)
        {
            int n1 = s1.length();
            int n2 = s2.length();
            int min = n1 < n2 ? n1 : n2;
            for ( int i = 0; i < min; i++ )
            {
                char c1 = s1.charAt( i );
                char c2 = s2.charAt( i );
                if ( c1 != c2 )
                {
                    // Fast check for simple ascii codes
                    if ( c1 <= 128 && c2 <= 128 )
                    {
                        c1 = toLowerCaseFast(c1);
                        c2 = toLowerCaseFast(c2);
                        if ( c1 != c2 )
                        {
                            return c1 - c2;
                        }
                    }
                    else
                    {
                        c1 = Character.toUpperCase( c1 );
                        c2 = Character.toUpperCase( c2 );
                        if ( c1 != c2 )
                        {
                            c1 = Character.toLowerCase( c1 );
                            c2 = Character.toLowerCase( c2 );
                            if ( c1 != c2 )
                            {
                                // No overflow because of numeric promotion
                                return c1 - c2;
                            }
                        }
                    }
                }
            }
            return n1 - n2;
        }
    }

    private static char toLowerCaseFast( char ch )
    {
        return ( ch >= 'A' && ch <= 'Z' ) ? ( char ) ( ch + 'a' - 'A' ) : ch;
    }

}
