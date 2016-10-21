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


import java.util.Dictionary;
import java.util.Hashtable;

import junit.framework.TestCase;

import org.apache.felix.cm.MockBundle;
import org.apache.felix.cm.MockBundleContext;
import org.apache.felix.cm.MockServiceReference;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;


public class TargetedPidTest
{

    @Test
    public void test_matchLevel()
    {
        //        TestCase.fail( "not implemented" );
    }


    @Test
    public void test_equals()
    {
        //        TestCase.fail( "not implemented" );
    }


    @Test
    public void test_matchesTarget_no_target()
    {
        final String pid = "a.b.c";
        final String symbolicName = "b1";
        final Version version = new Version( "1.0.0" );
        final String location = "loc:" + symbolicName;

        final Bundle b1 = createBundle( symbolicName, version, location );
        final ServiceReference r1 = createServiceReference( b1, pid );

        final ServiceReference rn = createServiceReference( createBundle( symbolicName + "_", version, location ), pid );
        final ServiceReference rv = createServiceReference(
            createBundle( symbolicName, new Version( "0.2.0" ), location ), pid );
        final ServiceReference rl = createServiceReference( createBundle( symbolicName, version, location + "_" ), pid );
        final ServiceReference rnone = createServiceReference( null, pid );

        final TargetedPID p1 = new TargetedPID( String.format( "%s", pid ) );

        TestCase.assertTrue( p1.matchesTarget( r1 ) );
        TestCase.assertTrue( p1.matchesTarget( rn ) );
        TestCase.assertTrue( p1.matchesTarget( rv ) );
        TestCase.assertTrue( p1.matchesTarget( rl ) );
        TestCase.assertFalse( "Unregistered service must not match targeted PID",  p1.matchesTarget( rnone ) );
    }


    @Test
    public void test_matchesTarget_name()
    {
        final String pid = "a.b.c";
        final String symbolicName = "b1";
        final Version version = new Version( "1.0.0" );
        final String location = "loc:" + symbolicName;

        final Bundle b1 = createBundle( symbolicName, version, location );
        final ServiceReference r1 = createServiceReference( b1, pid );

        final ServiceReference rn = createServiceReference( createBundle( symbolicName + "_", version, location ), pid );
        final ServiceReference rv = createServiceReference(
            createBundle( symbolicName, new Version( "0.2.0" ), location ), pid );
        final ServiceReference rl = createServiceReference( createBundle( symbolicName, version, location + "_" ), pid );
        final ServiceReference rnone = createServiceReference( null, pid );

        final TargetedPID p1 = new TargetedPID( String.format( "%s|%s", pid, symbolicName ) );

        TestCase.assertTrue( "Reference from same bundle must match targeted PID",  p1.matchesTarget( r1 ) );
        TestCase.assertFalse( "Different symbolic name must not match targeted PID",  p1.matchesTarget( rn ) );
        TestCase.assertTrue( p1.matchesTarget( rv ) );
        TestCase.assertTrue( p1.matchesTarget( rl ) );
        TestCase.assertFalse( "Unregistered service must not match targeted PID",  p1.matchesTarget( rnone ) );
    }


    @Test
    public void test_matchesTarget_name_version()
    {
        final String pid = "a.b.c";
        final String symbolicName = "b1";
        final Version version = new Version( "1.0.0" );
        final String location = "loc:" + symbolicName;

        final Bundle b1 = createBundle( symbolicName, version, location );
        final ServiceReference r1 = createServiceReference( b1, pid );

        final ServiceReference rn = createServiceReference( createBundle( symbolicName + "_", version, location ), pid );
        final ServiceReference rv = createServiceReference(
            createBundle( symbolicName, new Version( "0.2.0" ), location ), pid );
        final ServiceReference rl = createServiceReference( createBundle( symbolicName, version, location + "_" ), pid );
        final ServiceReference rnone = createServiceReference( null, pid );

        final TargetedPID p1 = new TargetedPID( String.format( "%s|%s|%s", pid, symbolicName, version ) );

        TestCase.assertTrue( "Reference from same bundle must match targeted PID",  p1.matchesTarget( r1 ) );
        TestCase.assertFalse( "Different symbolic name must not match targeted PID",  p1.matchesTarget( rn ) );
        TestCase.assertFalse( "Different version must not match targeted PID",  p1.matchesTarget( rv ) );
        TestCase.assertTrue( p1.matchesTarget( rl ) );
        TestCase.assertFalse( "Unregistered service must not match targeted PID",  p1.matchesTarget( rnone ) );
    }



    @Test
    public void test_matchesTarget_name_version_location()
    {
        final String pid = "a.b.c";
        final String symbolicName = "b1";
        final Version version = new Version( "1.0.0" );
        final String location = "loc:" + symbolicName;

        final Bundle b1 = createBundle( symbolicName, version, location );
        final ServiceReference r1 = createServiceReference( b1, pid );

        final ServiceReference rn = createServiceReference( createBundle( symbolicName + "_", version, location ), pid );
        final ServiceReference rv = createServiceReference(
            createBundle( symbolicName, new Version( "0.2.0" ), location ), pid );
        final ServiceReference rl = createServiceReference( createBundle( symbolicName, version, location + "_" ), pid );
        final ServiceReference rnone = createServiceReference( null, pid );

        final TargetedPID p1 = new TargetedPID( String.format( "%s|%s|%s|%s", pid, symbolicName, version, location ) );

        TestCase.assertTrue( "Reference from same bundle must match targeted PID",  p1.matchesTarget( r1 ) );
        TestCase.assertFalse( "Different symbolic name must not match targeted PID",  p1.matchesTarget( rn ) );
        TestCase.assertFalse( "Different version must not match targeted PID",  p1.matchesTarget( rv ) );
        TestCase.assertFalse( "Different location must not match targeted PID",  p1.matchesTarget( rl ) );
        TestCase.assertFalse( "Unregistered service must not match targeted PID",  p1.matchesTarget( rnone ) );
    }


    Bundle createBundle( final String symbolicName, final Version version, final String location )
    {
        BundleContext ctx = new MockBundleContext();
        return new MockBundle( ctx, location )
        {
            public String getSymbolicName()
            {
                return symbolicName;
            }


            @Override
            public Dictionary getHeaders()
            {
                return new Hashtable<String, Object>()
                {
                    {
                        put( Constants.BUNDLE_VERSION, version.toString() );
                    }
                };
            }
        };
    }


    ServiceReference<?> createServiceReference( final Bundle bundle, final Object pids )
    {
        return new MockServiceReference()
        {
            @Override
            public Bundle getBundle()
            {
                return bundle;
            }


            @Override
            public Object getProperty( String key )
            {
                if ( Constants.SERVICE_PID.equals( key ) )
                {
                    return pids;
                }
                return super.getProperty( key );
            }
        };
    }
}