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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Dictionary;

import org.apache.felix.cm.MockBundle;
import org.apache.felix.cm.MockBundleContext;
import org.apache.felix.cm.file.FilePersistenceManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;


public class DynamicBindingsTest
{

    private File configLocation;

    private File bindingsFile;

    private FilePersistenceManager persistenceManager;

    private static final String PID1 = "test.pid.1";

    private static final String LOCATION1 = "test://location.1";

    @Before
    public void setUp() throws Exception
    {
        configLocation = new File( "target/config." + System.currentTimeMillis() );
        persistenceManager = new FilePersistenceManager( configLocation.getAbsolutePath() );

        bindingsFile = new File( configLocation, DynamicBindings.BINDINGS_FILE_NAME + ".config" );
    }

    @After
    public void tearDown() throws Exception
    {
        bindingsFile.delete();
        configLocation.delete();
    }


    @Test public void test_no_bindings() throws IOException
    {

        // ensure there is no file
        bindingsFile.delete();

        final BundleContext ctx = new MockBundleContext();
        final DynamicBindings dm = new DynamicBindings( ctx, persistenceManager );
        final Dictionary<String, Object> bindings = getBindings( dm );

        assertNotNull( bindings );
        assertTrue( bindings.isEmpty() );
    }


    @Test public void test_store_bindings() throws IOException
    {
        // ensure there is no file
        bindingsFile.delete();

        final BundleContext ctx = new MockBundleContext();
        final DynamicBindings dm = new DynamicBindings( ctx, persistenceManager );

        dm.putLocation( PID1, LOCATION1 );
        assertEquals( LOCATION1, dm.getLocation( PID1 ) );

        assertTrue( bindingsFile.exists() );

        @SuppressWarnings("unchecked")
        final Dictionary<String, Object> bindings = persistenceManager.load( DynamicBindings.BINDINGS_FILE_NAME );
        assertNotNull( bindings );
        assertEquals( 1, bindings.size() );
        assertEquals( LOCATION1, bindings.get( PID1 ) );
    }


    @Test public void test_store_and_load_bindings() throws IOException
    {
        // ensure there is no file
        bindingsFile.delete();

        // preset bindings
        final DynamicBindings dm0 = new DynamicBindings( new MockBundleContext(), persistenceManager );
        dm0.putLocation( PID1, LOCATION1 );

        // check bindings
        final BundleContext ctx = new DMTestMockBundleContext();
        final DynamicBindings dm = new DynamicBindings( ctx, persistenceManager );

        // API check
        assertEquals( LOCATION1, dm.getLocation( PID1 ) );

        // low level check
        final Dictionary<String, Object> bindings = getBindings( dm );
        assertNotNull( bindings );
        assertEquals( 1, bindings.size() );
        assertEquals( LOCATION1, bindings.get( PID1 ) );
    }


    @Test public void test_store_and_load_bindings_with_cleanup() throws IOException
    {
        // ensure there is no file
        bindingsFile.delete();

        // preset bindings
        final DynamicBindings dm0 = new DynamicBindings( new MockBundleContext(), persistenceManager );
        dm0.putLocation( PID1, LOCATION1 );

        // check bindings
        final DynamicBindings dm = new DynamicBindings( new MockBundleContext(), persistenceManager );

        // API check
        assertNull( dm.getLocation( PID1 ) );

        // low level check
        final Dictionary<String, Object> bindings = getBindings( dm );
        assertNotNull( bindings );
        assertTrue( bindings.isEmpty() );
    }


    @SuppressWarnings("unchecked")
    private static Dictionary<String, Object> getBindings( DynamicBindings dm )
    {
        try
        {
            final Field bindings = dm.getClass().getDeclaredField( "bindings" );
            bindings.setAccessible( true );
            return ( Dictionary<String, Object> ) bindings.get( dm );
        }
        catch ( Throwable t )
        {
            fail( "Cannot get bindings field: " + t );
            return null;
        }
    }

    private static class DMTestMockBundleContext extends MockBundleContext
    {
        @Override
        public Bundle[] getBundles()
        {
            return new Bundle[]
                { new MockBundle( this, LOCATION1 ) };
        }
    }
}
