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
package org.apache.felix.scr.impl.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.service.log.LogService;

abstract class RegistrationManager<T>
{
    enum RegState {unregistered, registered};
    private static class RegStateWrapper 
    {
        private final CountDownLatch latch = new CountDownLatch(1);
        private final RegState regState;
        
        RegStateWrapper( RegState regState )
        {
            this.regState = regState;
        }
        
        public RegState getRegState()
        {
            return regState;
        }
        
        public CountDownLatch getLatch()
        {
            return latch;
        }
        
        @Override
        public int hashCode()
        {
            return regState.hashCode();
        }
        
        @Override
        public boolean equals(Object other)
        {
            return other instanceof RegStateWrapper && regState == ((RegStateWrapper)other).getRegState();
        }

        @Override
        public String toString()
        {
            return regState.toString();
        }

    }
    private final Lock registrationLock = new ReentrantLock();
    //Deque, ArrayDeque if we had java 6
    private final List<RegStateWrapper> opqueue = new ArrayList<RegStateWrapper>();

    private volatile T m_serviceRegistration;

    /**
     * 
     * @param desired desired registration state
     * @param services services to register this under
     * @return true if this request results in a state change, false if we are already in the desired state or some other thread 
     * will deal with the consequences of the state change.
     */
    boolean changeRegistration( RegState desired, String[] services )
    {
        RegStateWrapper rsw = null;
        registrationLock.lock();
        try
        {
            if (opqueue.isEmpty())
            {
                if ((desired == RegState.unregistered) == (m_serviceRegistration == null))
                {
                    log( LogService.LOG_DEBUG, "Already in desired state {0}", new Object[]
                            {desired}, null );
                    return false; 
                }
            }
            else if (opqueue.get( opqueue.size() - 1 ).getRegState() == desired)
            {
                log( LogService.LOG_DEBUG, "Duplicate request on other thread: registration change queue {0}", new Object[]
                        {opqueue}, null );
                rsw = opqueue.get( opqueue.size() - 1 );
                return false; //another thread will do our work and owns the state change
            }
            rsw = new RegStateWrapper( desired );
            opqueue.add( rsw );
            if (opqueue.size() > 1)
            {
                log( LogService.LOG_DEBUG, "Allowing other thread to process request: registration change queue {0}", new Object[]
                        {opqueue}, null );
                return true; //some other thread will do it later but this thread owns the state change.
            }
            //we're next
            do
            {
                log( LogService.LOG_DEBUG, "registration change queue {0}", new Object[]
                        {opqueue}, null );
                RegStateWrapper next = opqueue.get( 0 );
                T serviceRegistration = m_serviceRegistration;
                if ( next.getRegState() == RegState.unregistered)
                {
                    m_serviceRegistration = null;
                }
                    
                registrationLock.unlock();
                try
                {
                    if (next.getRegState() == RegState.registered)
                    {
                        serviceRegistration = register(services );

                    }
                    else 
                    {
                        if ( serviceRegistration != null )
                        {
                            unregister( serviceRegistration );
                        }
                        else
                        {
                            log( LogService.LOG_ERROR, "Unexpected unregistration request with no registration present", new Object[]
                                    {}, new Exception("Stack trace") );
                           
                        }
                    }
                }
                finally
                {
                    registrationLock.lock();
                    opqueue.remove(0);
                    if ( next.getRegState() == RegState.registered)
                    {
                        m_serviceRegistration = serviceRegistration;
                        postRegister( m_serviceRegistration );
                    }
                    next.getLatch().countDown();
                }
            }
            while (!opqueue.isEmpty());
            return true;
        }
        finally
        {
            registrationLock.unlock();
            if (rsw != null)
            {
                try
                {
                    if ( !rsw.getLatch().await( getTimeout(), TimeUnit.MILLISECONDS ))
                    {
                        log( LogService.LOG_ERROR, "Timeout waiting for reg change to complete {0}", new Object[]
                                {rsw.getRegState()}, null);
                        reportTimeout();
                    }
                }
                catch ( InterruptedException e )
                {
                    try
                    {
                        if ( !rsw.getLatch().await( getTimeout(), TimeUnit.MILLISECONDS ))
                        {
                            log( LogService.LOG_ERROR, "Timeout waiting for reg change to complete {0}", new Object[]
                                    {rsw.getRegState()}, null);
                            reportTimeout();
                        }
                    }
                    catch ( InterruptedException e1 )
                    {
                        log( LogService.LOG_ERROR, "Interrupted twice waiting for reg change to complete {0}", new Object[]
                                {rsw.getRegState()}, null);
                    }
                    Thread.currentThread().interrupt();
                }
            }
        }

    }
    
    abstract T register(String[] services);

    abstract void postRegister(T t);

    abstract void unregister(T serviceRegistration);
    
    abstract void log( int level, String message, Object[] arguments, Throwable ex );
    
    abstract long getTimeout();
    
    abstract void reportTimeout();
    
    T getServiceRegistration()
    {
        return m_serviceRegistration;
    }
    
}
