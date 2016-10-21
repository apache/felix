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
package org.apache.felix.cm.integration.helper;


import junit.framework.TestCase;

import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.ConfigurationListener;


public class TestListener implements ConfigurationListener
{

    private final Thread mainThread;

    private ConfigurationEvent event;

    private Thread eventThread;

    private int numberOfEvents;

    {
        this.mainThread = Thread.currentThread();
        this.numberOfEvents = 0;
    }


    public void configurationEvent( final ConfigurationEvent event )
    {
        this.numberOfEvents++;

        if ( this.event != null )
        {
            throw new IllegalStateException( "Untested event to be replaced: " + this.event.getType() + "/"
                + this.event.getPid() );
        }

        this.event = event;
        this.eventThread = Thread.currentThread();
    }


    void resetNumberOfEvents()
    {
        this.numberOfEvents = 0;
    }


    /**
     * Asserts an expected event has arrived since the last call to
     * {@link #assertEvent(int, String, String, boolean, int)} and
     * {@link #assertNoEvent()}.
     *
     * @param type The expected event type
     * @param pid The expected PID of the event
     * @param factoryPid The expected factory PID of the event or
     *      <code>null</code> if no factory PID is expected
     * @param expectAsync Whether the event is expected to have been
     *      provided asynchronously
     * @param numberOfEvents The number of events to have arrived in total
     */
    public void assertEvent( final int type, final String pid, final String factoryPid, final boolean expectAsync,
        final int numberOfEvents )
    {
        try
        {
            TestCase.assertNotNull( "Expecting an event", this.event );
            TestCase.assertEquals( "Expecting event type " + type, type, this.event.getType() );
            TestCase.assertEquals( "Expecting pid " + pid, pid, this.event.getPid() );
            if ( factoryPid == null )
            {
                TestCase.assertNull( "Expecting no factoryPid", this.event.getFactoryPid() );
            }
            else
            {
                TestCase.assertEquals( "Expecting factory pid " + factoryPid, factoryPid, this.event.getFactoryPid() );
            }

            TestCase.assertEquals( "Expecting " + numberOfEvents + " events", numberOfEvents, this.numberOfEvents );

            if ( expectAsync )
            {
                TestCase.assertNotSame( "Expecting asynchronous event", this.mainThread, this.eventThread );
            }
            else
            {
                TestCase.assertSame( "Expecting synchronous event", this.mainThread, this.eventThread );
            }
        }
        finally
        {
            this.event = null;
            this.eventThread = null;
        }
    }


    /**
     * Fails if an event has been received since the last call to
     * {@link #assertEvent(int, String, String, boolean, int)} or
     * {@link #assertNoEvent()}.
     */
    public void assertNoEvent()
    {
        TestCase.assertNull( this.event );
    }
}