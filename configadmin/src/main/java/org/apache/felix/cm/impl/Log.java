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

import java.text.MessageFormat;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Log implementation either logging to a {@code LogService} or to {@code System.err}.
 * The logger can be get using the static {@link #logger} field.
 *
 * The logger is initialized through {@link #start(BundleContext)} and {@link #set(ServiceReference)}.
 * It gets cleaned up through {@link #stop()}.
 */
public class Log
{
    /** The shared logger instance. */
    public static final Log logger = new Log();

    /**
     * The name of the bundle context property defining the maximum log level
     * (value is "felix.cm.loglevel"). The log level setting is only used if
     * there is no OSGi LogService available. Otherwise this setting is ignored.
     * <p>
     * This value of this property is expected to be an integer number
     * corresponding to the log level values of the OSGi LogService. That is 1
     * for errors, 2 for warnings, 3 for informational messages and 4 for debug
     * messages. The default value is 2, such that only warnings and errors are
     * logged in the absence of a LogService.
     */
    private static final String CM_LOG_LEVEL = "felix.cm.loglevel";

    // The name of the LogService (not using the class, which might be missing)
    private static final String LOG_SERVICE_NAME = "org.osgi.service.log.LogService";

    private static final int CM_LOG_LEVEL_DEFAULT = 2;

    // the ServiceTracker to emit log services (see log(int, String, Throwable))
    private volatile ServiceTracker logTracker;

    // the maximum log level when no LogService is available
    private volatile int logLevel = CM_LOG_LEVEL_DEFAULT;

    private volatile ServiceReference<ConfigurationAdmin> serviceReference;

    /**
     * Start the tracker for the logger and set the log level according to the configuration.
     * @param bundleContext The bundle context
     */
    public void start( final BundleContext bundleContext)
    {
        // track the log service using a ServiceTracker
        logTracker = new ServiceTracker( bundleContext, LOG_SERVICE_NAME , null );
        logTracker.open();

        // assign the log level
        String logLevelProp = bundleContext.getProperty( CM_LOG_LEVEL );
        if ( logLevelProp == null )
        {
            logLevel = CM_LOG_LEVEL_DEFAULT;
        }
        else
        {
            try
            {
                logLevel = Integer.parseInt( logLevelProp );
            }
            catch ( NumberFormatException nfe )
            {
                logLevel = CM_LOG_LEVEL_DEFAULT;
            }
        }
    }

    /**
     * Set the service reference to the configuration admin in order to include this
     * in every log message.
     * @param ref The service reference
     */
    public void set(final ServiceReference<ConfigurationAdmin> ref)
    {
        this.serviceReference = ref;
    }

    /**
     * Stop the log service tracker and clear the service reference
     */
    public void stop()
    {
        if ( logTracker != null )
        {
            logTracker.close();
            logTracker = null;
        }
        serviceReference = null;
    }

    /**
     * Is the log level enabled?
     * @param level The level
     * @return {@code true} if enabled
     */
    public boolean isLogEnabled( final int level )
    {
        return level <= logLevel;
    }

    /**
     * Log a message in the given level.
     * If arguments are provided and contain a {@code ServiceReference} then
     * the argument is replaced with the result of {@link #toString(ServiceReference)}.
     *
     * @param level The log level
     * @param format The message text
     * @param args The optional arguments
     */
    public void log( final int level, final String format, final Object[] args )
    {
        final ServiceTracker tracker = this.logTracker;
        final Object log = tracker == null ? null : tracker.getService();
        if ( log != null || isLogEnabled( level ) )
        {
            Throwable throwable = null;
            String message = format;

            if ( args != null && args.length > 0 )
            {
                for(int i=0; i<args.length; i++)
                {
                    if ( args[i] instanceof ServiceReference )
                    {
                        args[i] = toString((ServiceReference)args[i]);
                    }
                }
                if ( args[args.length - 1] instanceof Throwable )
                {
                    throwable = ( Throwable ) args[args.length - 1];
                }
                message = MessageFormat.format( format, args );
            }

            log( level, message, throwable );
        }
    }

    /**
     * Log the message with the given level and throwable.
     * @param level The log level
     * @param message The message
     * @param t The exception
     */
    public void log( final int level, final String message, final Throwable t )
    {
        // log using the LogService if available
        final ServiceTracker tracker = this.logTracker;
        final Object log = tracker == null ? null : tracker.getService();
        if ( log != null )
        {
            ( ( LogService ) log ).log( serviceReference, level, message, t );
            return;
        }

        // Otherwise only log if more serious than the configured level
        if ( isLogEnabled( level ) )
        {
            String code;
            switch ( level )
            {
                case LogService.LOG_INFO:
                    code = "*INFO *";
                    break;

                case LogService.LOG_WARNING:
                    code = "*WARN *";
                    break;

                case LogService.LOG_ERROR:
                    code = "*ERROR*";
                    break;

                case LogService.LOG_DEBUG:
                default:
                    code = "*DEBUG*";
            }

            System.err.println( code + " " + message );
            if ( t != null )
            {
                t.printStackTrace( System.err );
            }
        }
    }

    /**
     * Create a string representation of the service reference
     * @param ref The service reference
     * @return The string representation
     */
    private static String toString( final ServiceReference<?> ref )
    {
        String[] ocs = ( String[] ) ref.getProperty( "objectClass" );
        StringBuilder buf = new StringBuilder( "[" );
        for ( int i = 0; i < ocs.length; i++ )
        {
            buf.append( ocs[i] );
            if ( i < ocs.length - 1 )
                buf.append( ", " );
        }

        buf.append( ", id=" ).append( ref.getProperty( Constants.SERVICE_ID ) );

        Bundle provider = ref.getBundle();
        if ( provider != null )
        {
            buf.append( ", bundle=" ).append( provider.getBundleId() );
            buf.append( '/' ).append( provider.getLocation() );
        }
        else
        {
            buf.append( ", unregistered" );
        }

        buf.append( "]" );
        return buf.toString();
    }
}

