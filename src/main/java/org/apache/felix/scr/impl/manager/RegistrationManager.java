package org.apache.felix.scr.impl.manager;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.service.log.LogService;

abstract class RegistrationManager<T>
{
    enum RegState {unregistered, registered};
    private final Lock registrationLock = new ReentrantLock();
    //Deque, ArrayDeque if we had java 6
    private final List<RegState> opqueue = new ArrayList<RegState>();

    private volatile T m_serviceRegistration;
    /**
     * 
     * @param desired
     * @param services TODO
     * @return true if this thread reached the requested state
     */
    boolean changeRegistration( RegState desired, String[] services )
    {
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
            else if (opqueue.get( opqueue.size() - 1 ) == desired)
            {
                return false; //another thread will do our work.
            }
            opqueue.add( desired );
            if (opqueue.size() > 1)
            {
                return false; //some other thread will do it later
            }
            //we're next
            do
            {
                log( LogService.LOG_DEBUG, "registration change queue {0}", new Object[]
                        {opqueue}, null );
                desired = opqueue.get( 0 );
                T serviceRegistration = m_serviceRegistration;
                if ( desired == RegState.unregistered)
                {
                    m_serviceRegistration = null;
                }
                    
                registrationLock.unlock();
                try
                {
                    if (desired == RegState.registered)
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
                    if ( desired == RegState.registered)
                    {
                        m_serviceRegistration = serviceRegistration;
                    }
                }
            }
            while (!opqueue.isEmpty());
            return true;
        }
        finally
        {
            registrationLock.unlock();
        }

    }
    
    abstract T register(String[] services);

    abstract void unregister(T serviceRegistration);
    
    abstract void log( int level, String message, Object[] arguments, Throwable ex );
    
    T getServiceRegistration()
    {
        return m_serviceRegistration;
    }
    
}
