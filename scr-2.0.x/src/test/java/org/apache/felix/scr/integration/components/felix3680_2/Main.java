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
package org.apache.felix.scr.integration.components.felix3680_2;


import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.felix.scr.impl.manager.ThreadDump;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.runtime.ServiceComponentRuntime;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;
import org.osgi.service.log.LogService;


public class Main implements Runnable
{
    private static final int LATCH_TIMEOUT = 10000;
    private volatile ComponentContext m_ctx;
    private volatile AtomicInteger m_counter = new AtomicInteger();
    private volatile CountDownLatch m_enabledLatch;
    private volatile CountDownLatch m_disabledLatch;
    private volatile LogService m_logService;
    private ServiceComponentRuntime m_scr;
    private final Executor m_exec = Executors.newFixedThreadPool( 12 );
    private volatile BundleContext m_bctx;
    volatile ConcurrentHashMap<Class, ServiceRegistration> m_registrations = new ConcurrentHashMap<Class, ServiceRegistration>();
    volatile Exception _bindStackTrace;

    private volatile boolean running = true;


    /**
     * Helper used to randomly enable or disable a list of components.
     */
    class RegistrationHelper
    {
        public void registerBCDEFGHIJK( Executor exec )
        {
            enableOrDisable( true );
        }


        public void unregisterBCDEFGHIJK( Executor exec )
        {
            enableOrDisable( false );
        }


        private void enableOrDisable( final boolean enable )
        {
            if ( enable )
            {
                register( B.class );
                register( C.class );
                register( D.class );
                register( E.class );
                register( F.class );
                register( G.class );
                register( H.class );
                register( I.class );
                register( J.class );
                register( K.class );
            }
            else
            {
                unregister( B.class );
                unregister( C.class );
                unregister( D.class );
                unregister( E.class );
                unregister( F.class );
                unregister( G.class );
                unregister( H.class );
                unregister( I.class );
                unregister( J.class );
                unregister( K.class );
            }
        }


        private void register( final Class clazz )
        {
            m_exec.execute( new Runnable()
            {
                public void run()
                {
                    try
                    {
                        Object instance = clazz.newInstance();
                        m_registrations.put( clazz, m_bctx.registerService( clazz.getName(), instance, null ) );
                        m_enabledLatch.countDown();
                    }
                    catch ( Throwable e )
                    {
                        m_logService.log( LogService.LOG_ERROR, "error while enabling " + clazz, e );
                    }
                }
            } );
        }


        private void unregister( final Class clazz )
        {
            m_exec.execute( new Runnable()
            {
                public void run()
                {
                    try
                    {
                        ServiceRegistration sr = m_registrations.remove( clazz );
                        sr.unregister();
                        m_disabledLatch.countDown();
                    }
                    catch ( Throwable e )
                    {
                        m_logService.log( LogService.LOG_ERROR, "error while enabling " + clazz, e );
                    }
                }
            } );
        }
    }


    void bindSCR( ServiceComponentRuntime scr )
    {
        m_scr = scr;
    }


    void bindLogService( LogService logService )
    {
        m_logService = logService;
    }


    void bindA( ServiceReference sr )
    {
        Exception trace = new Exception( "bindA (" + Thread.currentThread() + ")" );
        if ( _bindStackTrace != null )
        {
            m_logService.log( LogService.LOG_ERROR, "Already bound A from stacktrace:", _bindStackTrace );
            m_logService.log( LogService.LOG_ERROR, "Current stacktrace is:", trace );
            return;
        }

        _bindStackTrace = trace;

        A a = ( A ) m_ctx.locateService( "a", sr );
        if ( a == null )
        {
            throw new IllegalStateException( "bindA: bundleContext.getService returned null" );
        }
        if ( m_counter.incrementAndGet() != 1 )
        {
            throw new IllegalStateException( "bindA: invalid counter value: " + m_counter );
        }
        m_enabledLatch.countDown();
    }


    void unbindA( A a )
    {
        if ( m_counter.decrementAndGet() != 0 )
        {
            throw new IllegalStateException( "unbindA: invalid counter value: " + m_counter );
        }
        _bindStackTrace = null;
        m_disabledLatch.countDown();
    }


    void start( ComponentContext ctx )
    {
        m_ctx = ctx;
        m_bctx = ctx.getBundleContext();
        m_ctx.getBundleContext().registerService( Executor.class.getName(), m_exec, null );
        new Thread( this ).start();
    }

    void stop() throws InterruptedException
    {
        synchronized ( this )
        {
            running = false;
        }
        if (m_enabledLatch != null) 
        {
            m_enabledLatch.await( 1, TimeUnit.MILLISECONDS );
        }
        if (m_disabledLatch != null) 
        {
            m_disabledLatch.await( 1, TimeUnit.MILLISECONDS );
        }
    }


    public void run()
    {
        int loop = 0;
        while ( iterate() )
        {
            

            RegistrationHelper registry = new RegistrationHelper();
            registry.registerBCDEFGHIJK( m_exec );

            try
            {
                if ( !m_enabledLatch.await( LATCH_TIMEOUT, TimeUnit.MILLISECONDS ) )
                {
                    System.out.println( "Did not get A injected timely ... see logs.txt" );
                    m_logService.log( LogService.LOG_ERROR, "enableLatch TIMEOUT" );
                    m_logService.log(LogService.LOG_ERROR, dumpThreads());
                    dumpA();
                    System.exit( 1 );
                }
            }
            catch ( InterruptedException e )
            {
            }

            registry.unregisterBCDEFGHIJK( m_exec );
            try
            {
                if ( !m_disabledLatch.await( LATCH_TIMEOUT, TimeUnit.MILLISECONDS ) )
                {
                    System.out.println( "Could not disable components timely ... see logs.txt" );
                    m_logService.log( LogService.LOG_ERROR, "disableLatch TIMEOUT" );
                    m_logService.log(LogService.LOG_ERROR, dumpThreads());
                    dumpA();
                    System.exit( 1 );
                }
            }
            catch ( InterruptedException e )
            {
            }

            ++loop;
            if ( loop % 100 == 0 )
            {
                m_logService.log( LogService.LOG_INFO, "Performed " + loop + " tests." );
            }
        }
    }


    private synchronized boolean iterate()
    {
        if ( running )
        {
            m_enabledLatch = new CountDownLatch( 11 ); // 10 for registrations of B,C,D,E,F,G,H,I,J,K + 1 for Main.bindA
            m_disabledLatch = new CountDownLatch( 11 ); // 10 for unregistrations of B,C,D,E,F,G,H,I,J,K + 1 for Main.unbindA
        }
        return running;
    }

    private String dumpThreads()
    {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        StringBuffer b = new StringBuffer( "Thread dump\n" );
        ThreadInfo[] infos = threadMXBean.dumpAllThreads( threadMXBean.isObjectMonitorUsageSupported(), threadMXBean.isSynchronizerUsageSupported() );
        for ( int i = 0; i < infos.length; i++ )
        {
            ThreadInfo ti = infos[i];
            b.append( "\n\nThreadId: " ).append( ti.getThreadId() ).append( " : name: " ).append( ti.getThreadName() ).append( " State: " ).append( ti.getThreadState() );
            b.append( "\n  LockInfo: " ).append( ti.getLockInfo() ).append( " LockOwnerId: " ).append( ti.getLockOwnerId() ).append( " LockOwnerName: ").append( ti.getLockOwnerName() );
            StackTraceElement[] stackTrace = ti.getStackTrace();
            for (int j = 0; j < stackTrace.length; j++ )
            {
                b.append( "\n  " ).append( stackTrace[j] );
            }
        }
        return b.toString();
    }

    private void dumpA()
    {
        ComponentDescriptionDTO c = m_scr
            .getComponentDescriptionDTO(m_bctx.getBundle(), "org.apache.felix.scr.integration.components.felix3680_2.A" );
        m_logService.log( LogService.LOG_WARNING, "State of " + c.name + " enabled:" + m_scr.isComponentEnabled(c) + "\n" );
    }


}
