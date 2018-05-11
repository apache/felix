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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import org.junit.Test;


public class CaseInsensitiveDictionaryTest
{

    @Test
    public void testLocaleIndependence() {
        Locale defaultLocal = Locale.getDefault();
        CaseInsensitiveDictionary dict = new CaseInsensitiveDictionary();
        dict.put("illegal", "value1");
        dict.put("ILLEGAL", "value2");
        assertEquals(dict.get("illegal"), "value2");
        assertEquals(dict.get("ILLEGAL"), "value2");

        // validate "i" conversion with Turkish default locale
        Locale.setDefault(new Locale("tr", "" ,""));
        try {
            dict = new CaseInsensitiveDictionary();
            dict.put("illegal", "value1");
            dict.put("ILLEGAL", "value2");
            assertEquals(dict.get("illegal"), "value2");
            assertEquals(dict.get("ILLEGAL"), "value2");
        } finally {
            Locale.setDefault(defaultLocal);
        }
    }


    @Test
    public void testCheckValueNull()
    {
        // null which must throw IllegalArgumentException
        try
        {
            CaseInsensitiveDictionary.checkValue( null );
            fail( "Expected IllegalArgumentException for null value" );
        }
        catch ( IllegalArgumentException iae )
        {

        }

    }


    @Test
    public void testCheckValueSimple()
    {
        internalTestCheckValue( "String" );
        internalTestCheckValue( new Integer( 1 ) );
        internalTestCheckValue( new Long( 1 ) );
        internalTestCheckValue( new Float( 1 ) );
        internalTestCheckValue( new Double( 1 ) );
        internalTestCheckValue( new Byte( ( byte ) 1 ) );
        internalTestCheckValue( new Short( ( short ) 1 ) );
        internalTestCheckValue( new Character( 'a' ) );
        internalTestCheckValue( Boolean.TRUE );
    }


    @Test
    public void testCheckValueSimpleArray()
    {
        internalTestCheckValue( new String[]
            { "String" } );
        internalTestCheckValue( new Integer[]
            { new Integer( 1 ) } );
        internalTestCheckValue( new Long[]
            { new Long( 1 ) } );
        internalTestCheckValue( new Float[]
            { new Float( 1 ) } );
        internalTestCheckValue( new Double[]
            { new Double( 1 ) } );
        internalTestCheckValue( new Byte[]
            { new Byte( ( byte ) 1 ) } );
        internalTestCheckValue( new Short[]
            { new Short( ( short ) 1 ) } );
        internalTestCheckValue( new Character[]
            { new Character( 'a' ) } );
        internalTestCheckValue( new Boolean[]
            { Boolean.TRUE } );
    }


    @Test
    public void testCheckValuePrimitiveArray()
    {
        internalTestCheckValue( new long[]
            { 1 } );
        internalTestCheckValue( new int[]
            { 1 } );
        internalTestCheckValue( new short[]
            { 1 } );
        internalTestCheckValue( new char[]
            { 1 } );
        internalTestCheckValue( new byte[]
            { 1 } );
        internalTestCheckValue( new double[]
            { 1 } );
        internalTestCheckValue( new float[]
            { 1 } );
        internalTestCheckValue( new boolean[]
            { true } );
    }


    @Test
    public void testCheckValueSimpleVector()
    {
        internalTestCheckValueVector( "String", String.class );
        internalTestCheckValueVector( new Integer( 1 ), Integer.class );
        internalTestCheckValueVector( new Long( 1 ), Long.class );
        internalTestCheckValueVector( new Float( 1 ), Float.class );
        internalTestCheckValueVector( new Double( 1 ), Double.class );
        internalTestCheckValueVector( new Byte( ( byte ) 1 ), Byte.class );
        internalTestCheckValueVector( new Short( ( short ) 1 ), Short.class );
        internalTestCheckValueVector( new Character( 'a' ), Character.class );
        internalTestCheckValueVector( Boolean.TRUE, Boolean.class );
    }

    @Test
    public void testCheckValueSimpleSet()
    {
        internalTestCheckValueSet( "String", String.class );
        internalTestCheckValueSet( new Integer( 1 ), Integer.class );
        internalTestCheckValueSet( new Long( 1 ), Long.class );
        internalTestCheckValueSet( new Float( 1 ), Float.class );
        internalTestCheckValueSet( new Double( 1 ), Double.class );
        internalTestCheckValueSet( new Byte( ( byte ) 1 ), Byte.class );
        internalTestCheckValueSet( new Short( ( short ) 1 ), Short.class );
        internalTestCheckValueSet( new Character( 'a' ), Character.class );
        internalTestCheckValueSet( Boolean.TRUE, Boolean.class );
    }


    @Test
    public void testCheckValueSimpleArrayList()
    {
        internalTestCheckValueList( "String", String.class );
        internalTestCheckValueList( new Integer( 1 ), Integer.class );
        internalTestCheckValueList( new Long( 1 ), Long.class );
        internalTestCheckValueList( new Float( 1 ), Float.class );
        internalTestCheckValueList( new Double( 1 ), Double.class );
        internalTestCheckValueList( new Byte( ( byte ) 1 ), Byte.class );
        internalTestCheckValueList( new Short( ( short ) 1 ), Short.class );
        internalTestCheckValueList( new Character( 'a' ), Character.class );
        internalTestCheckValueList( Boolean.TRUE, Boolean.class );
    }


    private <T> void internalTestCheckValueList( T value, Class<T> collectionType )
    {
        Collection<T> coll = new ArrayList<>();

        coll.add( value );
        internalTestCheckValue( coll );
    }

    private <T> void internalTestCheckValueVector( T value, Class<T> collectionType )
    {
        Collection<T> coll = new Vector<>();

        coll.add( value );
        internalTestCheckValue( coll );
    }

    private <T> void internalTestCheckValueSet( T value, Class<T> collectionType )
    {
        Collection<T> coll = new HashSet<>();

        coll.add( value );
        internalTestCheckValue( coll );
    }

    private void internalTestCheckValue( Object value )
    {
        assertEqualValue( value, CaseInsensitiveDictionary.checkValue( value ) );
    }


    private void assertEqualValue( Object expected, Object actual )
    {
        if ( ( expected instanceof Collection ) && ( actual instanceof Collection ) )
        {
            Collection<?> eColl = ( Collection<?> ) expected;
            Collection<?> aColl = ( Collection<?> ) actual;
            if ( eColl.size() != aColl.size() )
            {
                fail( "Unexpected size. expected:" + eColl.size() + ", actual: " + aColl.size() );
            }

            // create a list from the expected collection and remove
            // all values from the actual collection, this should get
            // an empty collection
            List<?> eList = new ArrayList<>( eColl );
            eList.removeAll( aColl );
            assertTrue( "Collections do not match. expected:" + eColl + ", actual: " + aColl, eList.isEmpty() );
        }
        else
        {
            assertEquals( expected, actual );
        }
    }


    @Test
    public void testValidKeys()
    {
        CaseInsensitiveDictionary.checkKey( "a" );
        CaseInsensitiveDictionary.checkKey( "1" );
        CaseInsensitiveDictionary.checkKey( "-" );
        CaseInsensitiveDictionary.checkKey( "_" );
        CaseInsensitiveDictionary.checkKey( "A" );
        CaseInsensitiveDictionary.checkKey( "a.b.c" );
        CaseInsensitiveDictionary.checkKey( "a.1.c" );
        CaseInsensitiveDictionary.checkKey( "a-sample.dotted_key.end" );
    }


    @Test
    public void testKeyDots()
    {
        // FELIX-2184 these keys are all valid (but not recommended)
        CaseInsensitiveDictionary.checkKey( "." );
        CaseInsensitiveDictionary.checkKey( "a.b.c." );
        CaseInsensitiveDictionary.checkKey( ".a.b.c." );
        CaseInsensitiveDictionary.checkKey( "a..b" );

        // valid key as of OSGi Compendium R4.2 (CM 1.3)
        CaseInsensitiveDictionary.checkKey( ".a.b.c" );
    }

    @Test
    public void testKeyIllegalCharacters()
    {
        testFailingKey( null );
        testFailingKey( "" );

        // FELIX-2184 these keys are all valid (but not recommended)
        CaseInsensitiveDictionary.checkKey( " " );
        CaseInsensitiveDictionary.checkKey( "§" );
        CaseInsensitiveDictionary.checkKey( "${yikes}" );
        CaseInsensitiveDictionary.checkKey( "a key with spaces" );
        CaseInsensitiveDictionary.checkKey( "fail:key" );
    }


    private void testFailingKey( String key )
    {
        try
        {
            CaseInsensitiveDictionary.checkKey( key );
            fail( "Expected IllegalArgumentException for key [" + key + "]" );
        }
        catch ( IllegalArgumentException iae )
        {
            // expected
        }
    }
}
