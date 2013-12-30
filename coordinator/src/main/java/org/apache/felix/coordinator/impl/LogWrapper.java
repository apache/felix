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
package org.apache.felix.coordinator.impl;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * This class mimics the standard OSGi <tt>LogService</tt> interface. It logs to an
 * available log service with the highest service ranking.
 *
 * @see org.osgi.service.log.LogService
**/
public class LogWrapper
{
    /**
     * ERROR LEVEL
     *
     * @see org.osgi.service.log.LogService#LOG_ERROR
     */
    public static final int LOG_ERROR = 1;

    /**
     * WARNING LEVEL
     *
     * @see org.osgi.service.log.LogService#LOG_WARNING
     */
    public static final int LOG_WARNING = 2;

    /**
     * INFO LEVEL
     *
     * @see org.osgi.service.log.LogService#LOG_INFO
     */
    public static final int LOG_INFO = 3;

    /**
     * DEBUG LEVEL
     *
     * @see org.osgi.service.log.LogService#LOG_DEBUG
     */
    public static final int LOG_DEBUG = 4;

    /** A sorted set containing the currently available LogServices.
     * Furthermore used as lock
     */
    private final Set<ServiceReference> loggerRefs = new TreeSet<ServiceReference>(
            new Comparator<ServiceReference>() {

                public int compare(ServiceReference o1, ServiceReference o2) {
                    return o2.compareTo(o1);
                }

            });

    /**
     *  Only null while not set and loggerRefs is empty hence, only needs to be
     *  checked in case m_loggerRefs is empty otherwise it will not be null.
     */
    private BundleContext context;

    private ServiceListener logServiceListener;

    /**
     * Current log level. Message with log level less than or equal to
     * current log level will be logged.
     * The default value is {@link #LOG_WARNING}
     *
     * @see #setLogLevel(int)
     */
    private int logLevel = LOG_WARNING;

    /**
     * Create the singleton
     */
    private static class LogWrapperLoader
    {
        static final LogWrapper SINGLETON = new LogWrapper();
    }

    /**
     * Returns the singleton instance of this LogWrapper that can be used to send
     * log messages to all currently available LogServices or to standard output,
     * respectively.
     *
     * @return the singleton instance of this LogWrapper.
     */
    public static LogWrapper getLogger()
    {
        return LogWrapperLoader.SINGLETON;
    }

    /**
     * Set the <tt>BundleContext</tt> of the bundle. This method registers a service
     * listener for LogServices with the framework that are subsequently used to
     * log messages.
     * <p>
     * If the bundle context is <code>null</code>, the service listener is
     * unregistered and all remaining references to LogServices dropped before
     * internally clearing the bundle context field.
     *
     *  @param context The context of the bundle.
     */
    public static void setContext( final BundleContext context )
    {
        final LogWrapper logWrapper = LogWrapperLoader.SINGLETON;

        // context is removed, unregister and drop references
        if ( context == null )
        {
            if ( logWrapper.logServiceListener != null )
            {
                logWrapper.context.removeServiceListener( logWrapper.logServiceListener );
                logWrapper.logServiceListener = null;
            }
            logWrapper.removeLoggerRefs();
        }

        // set field
        logWrapper.setBundleContext( context );

        // context is set, register and get existing services
        if ( context != null )
        {
            try
            {
                final ServiceListener listener = new ServiceListener()
                {
                    // Add a newly available LogService reference to the singleton.
                    public void serviceChanged( final ServiceEvent event )
                    {
                        if ( ServiceEvent.REGISTERED == event.getType() )
                        {
                            LogWrapperLoader.SINGLETON.addLoggerRef( event.getServiceReference() );
                        }
                        else if ( ServiceEvent.UNREGISTERING == event.getType() )
                        {
                            LogWrapperLoader.SINGLETON.removeLoggerRef( event.getServiceReference() );
                        }
                    }

                };
                context.addServiceListener( listener, "(" + Constants.OBJECTCLASS + "=org.osgi.service.log.LogService)" );
                logWrapper.logServiceListener = listener;

                // Add all available LogService references to the singleton.
                final ServiceReference[] refs = context.getServiceReferences( "org.osgi.service.log.LogService", null );

                if ( null != refs )
                {
                    for ( int i = 0; i < refs.length; i++ )
                    {
                        logWrapper.addLoggerRef( refs[i] );
                    }
                }
            }
            catch ( InvalidSyntaxException e )
            {
                // this never happens
            }
        }
    }


    /**
     * The private singleton constructor.
     */
    LogWrapper()
    {
        // Singleton
    }

    /**
     * Removes all references to LogServices still kept
     */
    void removeLoggerRefs()
    {
        synchronized ( loggerRefs )
        {
            loggerRefs.clear();
        }
    }

    /**
     * Add a reference to a newly available LogService
     */
    void addLoggerRef( final ServiceReference ref )
    {
        synchronized (loggerRefs)
        {
            loggerRefs.add(ref);
        }
    }

    /**
     * Remove a reference to a LogService
     */
    void removeLoggerRef( final ServiceReference ref )
    {
        synchronized (loggerRefs)
        {
            loggerRefs.remove(ref);
        }
    }

    /**
     * Set the context of the bundle in the singleton implementation.
     */
    private void setBundleContext(final BundleContext context)
    {
        synchronized(loggerRefs)
        {
            this.context = context;
        }
    }

    public void log(final int level, final String msg)
    {
        log(null, level, msg, null);
    }

    public void log(final int level, final String msg, final Throwable ex)
    {
        log(null, level, msg, null);
    }

    public void log(final ServiceReference sr, final int level, final String msg)
    {
        log(sr, level, msg, null);
    }

    public void log(final ServiceReference sr, final int level, final String msg,
        final Throwable ex)
    {
        // The method will remove any unregistered service reference as well.
        synchronized (loggerRefs)
        {
            if (level > logLevel)
            {
                return; // don't log
            }

            boolean logged = false;

            if (!loggerRefs.isEmpty())
            {
                // There is at least one LogService available hence, we can use the
                // class as well.
                for (Iterator<ServiceReference> iter = loggerRefs.iterator(); iter.hasNext();)
                {
                    final ServiceReference next = iter.next();

                    org.osgi.service.log.LogService logger =
                        (org.osgi.service.log.LogService) context.getService(next);

                    if (null != logger)
                    {
                        if ( sr == null )
                        {
                            if ( ex == null )
                            {
                                logger.log(level, msg);
                            }
                            else
                            {
                                logger.log(level, msg, ex);
                            }
                        }
                        else
                        {
                            if ( ex == null )
                            {
                                logger.log(sr, level, msg);
                            }
                            else
                            {
                                logger.log(sr, level, msg, ex);
                            }
                        }
                        context.ungetService(next);
                        // we logged, so we can finish
                        logged = true;
                        break;
                    }
                    else
                    {
                        // The context returned null for the reference - it follows
                        // that the service is unregistered and we can remove it
                        iter.remove();
                    }
                }
            }
            if ( !logged)
            {
                _log(sr, level, msg, ex);
            }
        }
    }

    /*
     * Log the message to standard output. This appends the level to the message.
     * null values are handled appropriate.
     */
    private void _log(final ServiceReference sr, final int level, final String msg,
        Throwable ex)
    {
        String s = (sr == null) ? null : "SvcRef " + sr;
        s = (s == null) ? msg : s + " " + msg;
        s = (ex == null) ? s : s + " (" + ex + ")";

        switch (level)
        {
            case LOG_DEBUG:
                System.out.println("DEBUG: " + s);
                break;
            case LOG_ERROR:
                System.out.println("ERROR: " + s);
                if (ex != null)
                {
                    if ((ex instanceof BundleException)
                        && (((BundleException) ex).getNestedException() != null))
                    {
                        ex = ((BundleException) ex).getNestedException();
                    }

                    ex.printStackTrace();
                }
                break;
            case LOG_INFO:
                System.out.println("INFO: " + s);
                break;
            case LOG_WARNING:
                System.out.println("WARNING: " + s);
                break;
            default:
                System.out.println("UNKNOWN[" + level + "]: " + s);
        }
    }

    /**
     * Change the current log level. Log level decides what messages gets
     * logged. Any message with a log level higher than the currently set
     * log level is not logged.
     *
     * @param logLevel new log level
     */
    public void setLogLevel(int logLevel)
    {
        synchronized (loggerRefs)
        {
            logLevel = logLevel;
        }
    }

    /**
     * @return current log level.
     */
    public int getLogLevel()
    {
        synchronized (loggerRefs)
        {
            return logLevel;
        }
    }
}
