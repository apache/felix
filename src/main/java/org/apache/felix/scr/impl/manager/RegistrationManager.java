package org.apache.felix.scr.impl.manager;

import java.util.ArrayList;
import java.util.Dictionary;
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
        
        public int hashCode()
        {
            return regState.hashCode();
        }
        
        public boolean equals(Object other)
        {
            return other instanceof RegStateWrapper && regState == ((RegStateWrapper)other).getRegState();
        }
        
    }
    private final Lock registrationLock = new ReentrantLock();
    //Deque, ArrayDeque if we had java 6
    private final List<RegStateWrapper> opqueue = new ArrayList<RegStateWrapper>();

    private volatile T m_serviceRegistration;
    /**
     * 
     * @param desired
     * @param services TODO
     * @return true if this thread reached the requested state
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
                    return false; //already in desired state
                }
            }
            else if (opqueue.get( opqueue.size() - 1 ).getRegState() == desired)
            {
                rsw = opqueue.get( opqueue.size() - 1 );
                return false; //another thread will do our work.
            }
            rsw = new RegStateWrapper( desired );
            opqueue.add( rsw );
            if (opqueue.size() > 1)
            {
                return false; //some other thread will do it later
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
                    }
                }
                catch ( InterruptedException e )
                {
                    log( LogService.LOG_ERROR, "Interrupted exception waiting for reg change to complete {0}", new Object[]
                            {rsw.getRegState()}, null);
                }
            }
        }

    }
    
    abstract T register(String[] services);

    abstract void unregister(T serviceRegistration);
    
    abstract void log( int level, String message, Object[] arguments, Throwable ex );
    
    abstract long getTimeout();
    
    T getServiceRegistration()
    {
        return m_serviceRegistration;
    }
    
}
