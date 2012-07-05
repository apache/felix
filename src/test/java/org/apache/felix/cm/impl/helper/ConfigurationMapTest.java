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
package org.apache.felix.cm.impl.helper;


import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.Test;


public class ConfigurationMapTest
{

    @Test
    public void test_accepts()
    {
        ConfigurationMap holder = new TestConfigurationMap( new String[]
            { "a", "b", "c" } );

        TestCase.assertTrue( holder.accepts( "a" ) );
        TestCase.assertTrue( holder.accepts( "b" ) );
        TestCase.assertTrue( holder.accepts( "c" ) );

        TestCase.assertFalse( holder.accepts( "x" ) );
    }

    @Test
    public void test_isDifferentPids_null_null()
    {
        ConfigurationMap holder = new TestConfigurationMap( null );
        TestCase.assertFalse( "Expect both pids null to be the same", holder.isDifferentPids( null ) );
    }


    @Test
    public void test_isDifferentPids_null_notNull()
    {
        ConfigurationMap holder = new TestConfigurationMap( null );
        TestCase.assertTrue( "Expect not same for one pid not null", holder.isDifferentPids( new String[]
            { "entry" } ) );
    }


    @Test
    public void test_isDifferentPids_notNull_null()
    {
        ConfigurationMap holder = new TestConfigurationMap( new String[]
            { "entry" } );
        TestCase.assertTrue( "Expect not same for one pid not null", holder.isDifferentPids( null ) );
    }


    @Test
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

        final ConfigurationMap holder10 = new TestConfigurationMap( pids10 );
        TestCase.assertFalse( holder10.isDifferentPids( pids10 ) );
        TestCase.assertFalse( holder10.isDifferentPids( pids11 ) );
        TestCase.assertTrue( holder10.isDifferentPids( pids20 ) );
        TestCase.assertTrue( holder10.isDifferentPids( pids30 ) );

        final ConfigurationMap holder20 = new TestConfigurationMap( pids20 );
        TestCase.assertTrue( holder20.isDifferentPids( pids10 ) );
        TestCase.assertTrue( holder20.isDifferentPids( pids11 ) );
        TestCase.assertFalse( holder20.isDifferentPids( pids20 ) );
        TestCase.assertTrue( holder20.isDifferentPids( pids30 ) );
    }

    /*
     * Simple ConfigurationMap implementation sufficing for these tests
     * which only test the methods in the abstract base class.
     */
    static class TestConfigurationMap extends ConfigurationMap<String>
    {

        protected TestConfigurationMap( String[] configuredPids )
        {
            super( configuredPids );
        }


        @Override
        protected Map<String, String> createMap( int size )
        {
            return new HashMap<String, String>( size );
        }


        @Override
        protected void record( TargetedPID configPid, TargetedPID factoryPid, long revision )
        {
            TestCase.fail( "<record> is not implemented" );
        }


        @Override
        protected boolean shallTake( TargetedPID configPid, TargetedPID factoryPid, long revision )
        {
            TestCase.fail( "<shallTake> is not implemented" );
            return false;
        }


        @Override
        protected boolean removeConfiguration( TargetedPID configPid, TargetedPID factoryPid )
        {
            TestCase.fail( "<removeConfiguration> is not implemented" );
            return false;
        }

    }
}
