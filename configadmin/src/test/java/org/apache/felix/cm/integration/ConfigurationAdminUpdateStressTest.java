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
package org.apache.felix.cm.integration;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;


/**
 * The <code>ConfigurationAdminUpdateStressTest</code> repeatedly updates
 * a ManagedFactoryService with configuration to verify configuration is
 * exactly delivered once and no update is lost.
 *
 * @see <a href="https://issues.apache.org/jira/browse/FELIX-1545">FELIX-1545</a>
 */
@RunWith(JUnit4TestRunner.class)
public class ConfigurationAdminUpdateStressTest extends ConfigurationTestBase implements LogService
{
    public static final int TEST_LOOP = 10;
    public static final int UPDATE_LOOP = 100;

    private String _FACTORYPID = "MyPID";

    private volatile CountDownLatch _factoryConfigCreateLatch;
    private volatile CountDownLatch _factoryConfigUpdateLatch;
    private volatile CountDownLatch _factoryConfigDeleteLatch;
    private volatile CountDownLatch _testLatch;
    private volatile ServiceTracker _tracker;


    // ----------------------- Initialization -------------------------------------------

    @Before
    public void startup()
    {
        bundleContext.registerService( LogService.class.getName(), this, null );
        _tracker = new ServiceTracker( bundleContext, ConfigurationAdmin.class.getName(), null );
        _tracker.open();
    }


    /**
     * Always cleanup our bundle location file (because pax seems to forget to cleanup it)
     * @param context
     */

    @After
    public void tearDown()
    {
        _tracker.close();
    }


    // ----------------------- LogService -----------------------------------------------

    public void log( int level, String message )
    {
        System.out.println( "[LogService/" + level + "] " + message );
    }


    public void log( int level, String message, Throwable exception )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "[LogService/" + level + "] " );
        sb.append( message );
        parse( sb, exception );
        System.out.println( sb.toString() );
    }


    public void log( ServiceReference sr, int level, String message )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "[LogService/" + level + "] " );
        sb.append( message );
        System.out.println( sb.toString() );
    }


    public void log( ServiceReference sr, int level, String message, Throwable exception )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "[LogService/" + level + "] " );
        sb.append( message );
        parse( sb, exception );
        System.out.println( sb.toString() );
    }


    private void parse( StringBuilder sb, Throwable t )
    {
        if ( t != null )
        {
            sb.append( " - " );
            StringWriter buffer = new StringWriter();
            PrintWriter pw = new PrintWriter( buffer );
            t.printStackTrace( pw );
            sb.append( buffer.toString() );
        }
    }


    // --------------------------- CM Update stress test -------------------------------------

    @Test
    public void testCMUpdateStress()
    {
        _testLatch = new CountDownLatch( 1 );
        try
        {
            CreateUpdateStress stress = new CreateUpdateStress( bundleContext );
            stress.start();

            if ( !_testLatch.await( 15, TimeUnit.SECONDS ) )
            {

                log( LogService.LOG_DEBUG, "create latch: " + _factoryConfigCreateLatch.getCount() );
                log( LogService.LOG_DEBUG, "update latch: " + _factoryConfigUpdateLatch.getCount() );
                log( LogService.LOG_DEBUG, "delete latch: " + _factoryConfigDeleteLatch.getCount() );

                Assert.fail( "Test did not completed timely" );
            }
        }
        catch ( InterruptedException e )
        {
            Assert.fail( "Test interrupted" );
        }
    }


    /**
     * Setup the latches used throughout this test
     */
    private void setupLatches()
    {
        _factoryConfigCreateLatch = new CountDownLatch( 1 );
        _factoryConfigUpdateLatch = new CountDownLatch( UPDATE_LOOP );
        _factoryConfigDeleteLatch = new CountDownLatch( 1 );
    }

    /**
     * This is our Factory class which will react up CM factory configuration objects.
     * Each time a factory configuration object is created, the _factoryConfigCreatedLatch is counted down.
     * Each time a factory configuration object is updated, the _factoryConfigUpdatedLatch is counted down.
     * Each time a factory configuration object is deleted, the _factoryConfigDeletedLatch is counted down.
     */
    @Ignore
    class Factory implements ManagedServiceFactory
    {
        Set<String> _pids = new HashSet<String>();


        public synchronized void updated( String pid, Dictionary properties ) throws ConfigurationException
        {
            if ( _pids.add( pid ) )
            {
                // pid created
                _factoryConfigCreateLatch.countDown();
                log( LogService.LOG_DEBUG, "Config created; create latch= " + _factoryConfigCreateLatch.getCount() );
            }
            else
            {
                // pid updated
                try
                {
                    Long number = ( Long ) properties.get( "number" );
                    long currentNumber = _factoryConfigUpdateLatch.getCount();
                    if ( number.longValue() != currentNumber )
                    {
                        throw new ConfigurationException( "number", "Expected number=" + currentNumber + ", actual="
                            + number );
                    }
                    _factoryConfigUpdateLatch.countDown();
                    log( LogService.LOG_DEBUG, "Config updated; update latch= " + _factoryConfigUpdateLatch.getCount()
                        + " (number=" + number + ")" );
                }
                catch ( ClassCastException e )
                {
                    throw new ConfigurationException( "number", e.getMessage(), e );
                }
            }
        }


        public void deleted( String pid )
        {
            _factoryConfigDeleteLatch.countDown();
            log( LogService.LOG_DEBUG, "Config deleted; delete latch= " + _factoryConfigDeleteLatch.getCount() );
        }


        public String getName()
        {
            return "MyPID";
        }
    }

    /**
     * This class creates/update/delete some factory configuration instances, using a separate thread.
     */
    @Ignore
    class CreateUpdateStress extends Thread
    {
        BundleContext _bc;


        CreateUpdateStress( BundleContext bctx )
        {
            _bc = bctx;
        }


        public void run()
        {
            try
            {
                System.out.println( "Starting CM stress test ..." );
                ConfigurationAdmin cm = ( ConfigurationAdmin ) _tracker.waitForService( 2000 );
                setupLatches();
                Factory factory = new Factory();
                Hashtable<String, Object> serviceProps = new Hashtable<String, Object>();
                serviceProps.put( "service.pid", _FACTORYPID );
                _bc.registerService( ManagedServiceFactory.class.getName(), factory, serviceProps );

                for ( int l = 0; l < TEST_LOOP; l++ )
                {
                    // Create factory configuration
                    org.osgi.service.cm.Configuration conf = cm.createFactoryConfiguration( _FACTORYPID, null );
                    Hashtable<String, Object> props = new Hashtable<String, Object>();
                    props.put( "foo", "bar" );
                    conf.update( props );

                    // Check if our Factory has seen the factory configuration creation
                    if ( !_factoryConfigCreateLatch.await( 10, TimeUnit.SECONDS ) )
                    {
                        throw new RuntimeException( "_factoryConfigCreateLatch did not reach zero timely" );
                    }

                    // Update factory configuration many times
                    for ( int i = 0; i < UPDATE_LOOP; i++ )
                    {
                        props = new Hashtable<String, Object>();
                        props.put( "foo", "bar" + i );
                        props.put( "number", new Long( UPDATE_LOOP - i ) );
                        conf.update( props );
                    }

                    // Check if all configuration updates have been caught by our Factory
                    if ( !_factoryConfigUpdateLatch.await( 10, TimeUnit.SECONDS ) )
                    {
                        throw new RuntimeException( "_factoryConfigUpdateLatch did not reach zero timely" );
                    }

                    // Remove factory configuration
                    conf.delete();

                    // Check if our Factory has seen the configration removal
                    if ( !_factoryConfigDeleteLatch.await( 10, TimeUnit.SECONDS ) )
                    {
                        throw new RuntimeException( "_factoryConfigDeleteLatch did not reach zero timely" );
                    }

                    // Reset latches
                    setupLatches();
                }
            }
            catch ( Exception e )
            {
                e.printStackTrace( System.err );
                return;
            }
            _testLatch.countDown(); // Notify that our test is done
        }
    }
}
