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
<<<<<<< HEAD
import java.util.Dictionary;
=======
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.service.log.LogService;

abstract class RegistrationManager<T>
{
    enum RegState {unregistered, registered};
<<<<<<< HEAD
    private static class RegStateWrapper 
    {
        private final CountDownLatch latch = new CountDownLatch(1);
        private final RegState regState;
        
=======
    private static class RegStateWrapper
    {
        private final CountDownLatch latch = new CountDownLatch(1);
        private final RegState regState;

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        RegStateWrapper( RegState regState )
        {
            this.regState = regState;
        }
<<<<<<< HEAD
        
=======

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        public RegState getRegState()
        {
            return regState;
        }
<<<<<<< HEAD
        
=======

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        public CountDownLatch getLatch()
        {
            return latch;
        }
<<<<<<< HEAD
        
=======

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        @Override
        public int hashCode()
        {
            return regState.hashCode();
        }
<<<<<<< HEAD
        
=======

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
        
        
        
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
=======

    }
    private final Lock registrationLock = new ReentrantLock();
    //Deque, ArrayDeque if we had java 6
    private final List<RegStateWrapper> opqueue = new ArrayList<>();

    private volatile T m_serviceRegistration;

    /**
     *
     * @param desired desired registration state
     * @param services services to register this under
     * @return true if this request results in a state change, false if we are already in the desired state or some other thread
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
                    log( LogService.LOG_DEBUG, "Already in desired state {0}", new Object[]
                            {desired}, null );
                    return false; 
=======
                    log( LogService.LOG_DEBUG, "Already in desired state {0}", null, desired);
                    return false;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                }
            }
            else if (opqueue.get( opqueue.size() - 1 ).getRegState() == desired)
            {
<<<<<<< HEAD
                log( LogService.LOG_DEBUG, "Duplicate request on other thread: registration change queue {0}", new Object[]
                        {opqueue}, null );
=======
                log( LogService.LOG_DEBUG, "Duplicate request on other thread: registration change queue {0}", null, opqueue);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                rsw = opqueue.get( opqueue.size() - 1 );
                return false; //another thread will do our work and owns the state change
            }
            rsw = new RegStateWrapper( desired );
            opqueue.add( rsw );
            if (opqueue.size() > 1)
            {
<<<<<<< HEAD
                log( LogService.LOG_DEBUG, "Allowing other thread to process request: registration change queue {0}", new Object[]
                        {opqueue}, null );
=======
                log( LogService.LOG_DEBUG, "Allowing other thread to process request: registration change queue {0}", null, opqueue);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                return true; //some other thread will do it later but this thread owns the state change.
            }
            //we're next
            do
            {
<<<<<<< HEAD
                log( LogService.LOG_DEBUG, "registration change queue {0}", new Object[]
                        {opqueue}, null );
=======
                log( LogService.LOG_DEBUG, "registration change queue {0}", null, opqueue);;
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                RegStateWrapper next = opqueue.get( 0 );
                T serviceRegistration = m_serviceRegistration;
                if ( next.getRegState() == RegState.unregistered)
                {
                    m_serviceRegistration = null;
                }
<<<<<<< HEAD
                    
=======

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                registrationLock.unlock();
                try
                {
                    if (next.getRegState() == RegState.registered)
                    {
                        serviceRegistration = register(services );

                    }
<<<<<<< HEAD
                    else 
=======
                    else
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                    {
                        if ( serviceRegistration != null )
                        {
                            unregister( serviceRegistration );
                        }
                        else
                        {
<<<<<<< HEAD
                            log( LogService.LOG_ERROR, "Unexpected unregistration request with no registration present", new Object[]
                                    {}, new Exception("Stack trace") );
                           
=======
                            log( LogService.LOG_ERROR, "Unexpected unregistration request with no registration present",  new Exception("Stack trace"));

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
=======
                        postRegister( m_serviceRegistration );
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
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
<<<<<<< HEAD
                        log( LogService.LOG_ERROR, "Timeout waiting for reg change to complete {0}", new Object[]
                                {rsw.getRegState()}, null);
=======
                        log( LogService.LOG_ERROR, "Timeout waiting for reg change to complete {0}", null, rsw.getRegState());
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                        reportTimeout();
                    }
                }
                catch ( InterruptedException e )
                {
                    try
                    {
                        if ( !rsw.getLatch().await( getTimeout(), TimeUnit.MILLISECONDS ))
                        {
<<<<<<< HEAD
                            log( LogService.LOG_ERROR, "Timeout waiting for reg change to complete {0}", new Object[]
                                    {rsw.getRegState()}, null);
=======
                            log( LogService.LOG_ERROR, "Timeout waiting for reg change to complete {0}", null, rsw.getRegState());
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                            reportTimeout();
                        }
                    }
                    catch ( InterruptedException e1 )
                    {
<<<<<<< HEAD
                        log( LogService.LOG_ERROR, "Interrupted twice waiting for reg change to complete {0}", new Object[]
                                {rsw.getRegState()}, null);
=======
                        log( LogService.LOG_ERROR, "Interrupted twice waiting for reg change to complete {0}",null, rsw.getRegState());
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
                    }
                    Thread.currentThread().interrupt();
                }
            }
        }

    }
<<<<<<< HEAD
    
    abstract T register(String[] services);

    abstract void unregister(T serviceRegistration);
    
    abstract void log( int level, String message, Object[] arguments, Throwable ex );
    
    abstract long getTimeout();
    
    abstract void reportTimeout();
    
=======

    abstract T register(String[] services);

    abstract void postRegister(T t);

    abstract void unregister(T serviceRegistration);

    abstract void log( int level, String message, Throwable ex, Object... arguments );

    abstract long getTimeout();

    abstract void reportTimeout();

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    T getServiceRegistration()
    {
        return m_serviceRegistration;
    }
<<<<<<< HEAD
    
=======

>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
}
