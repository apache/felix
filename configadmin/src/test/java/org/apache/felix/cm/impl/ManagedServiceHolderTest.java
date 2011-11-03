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


import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import junit.framework.TestCase;


public class ManagedServiceHolderTest extends TestCase
{

    private static final Constructor mshConstructor;
    private static final Method msh_isDifferentPids;

    static
    {
        try
        {
            Class clazz = Class.forName( "org.apache.felix.cm.impl.ConfigurationManager$ManagedServiceHolder" );
            mshConstructor = clazz.getDeclaredConstructor( new Class[]
                { Object.class, String[].class } );
            mshConstructor.setAccessible( true );
            msh_isDifferentPids = clazz.getDeclaredMethod( "isDifferentPids", new Class[]
                { String[].class } );
            msh_isDifferentPids.setAccessible( true );
        }
        catch ( Throwable t )
        {
            throw new RuntimeException( t );
        }
    }


    public void test_isDifferentPids_null_null()
    {
        Object holder = createHolder( null );
        assertFalse( "Expect both pids null to be the same", isDifferentPids( holder, null ) );
    }


    public void test_isDifferentPids_null_notNull()
    {
        Object holder = createHolder( null );
        assertTrue( "Expect not same for one pid not null", isDifferentPids( holder, new String[]
            { "entry" } ) );
    }


    public void test_isDifferentPids_notNull_null()
    {
        Object holder = createHolder( new String[]
            { "entry" } );
        assertTrue( "Expect not same for one pid not null", isDifferentPids( holder, null ) );
    }


    public void test_isDifferentPids_notNull_notNull()
    {
        final String[] pids10 =
            { "a", "b" };
        final String[] pids11 =
            { "b", "a" };
        final String[] pids20 =
            { "a", "c" };
        final String[] pids30 =
            { "a", "b", "c" };

        final Object holder10 = createHolder( pids10 );
        assertFalse( isDifferentPids( holder10, pids10 ) );
        assertFalse( isDifferentPids( holder10, pids11 ) );
        assertTrue( isDifferentPids( holder10, pids20 ) );
        assertTrue( isDifferentPids( holder10, pids30 ) );

        final Object holder20 = createHolder( pids20 );
        assertTrue( isDifferentPids( holder20, pids10 ) );
        assertTrue( isDifferentPids( holder20, pids11 ) );
        assertFalse( isDifferentPids( holder20, pids20 ) );
        assertTrue( isDifferentPids( holder20, pids30 ) );
    }


    private Object createHolder( final String[] pids )
    {
        try
        {
            return mshConstructor.newInstance( new Object[]
                { null, pids } );
        }
        catch ( Throwable t )
        {
            fail( t.getMessage() );
            return null; // keep compiler quiet
        }
    }


    private boolean isDifferentPids( final Object holder, final String[] pids )
    {
        try
        {
            Object result = msh_isDifferentPids.invoke( holder, new Object[]
                { pids } );
            return ( result instanceof Boolean ) && ( ( Boolean ) result ).booleanValue();
        }
        catch ( Throwable t )
        {
            fail( t.getMessage() );
            return false; // keep compiler quiet
        }
    }
}
