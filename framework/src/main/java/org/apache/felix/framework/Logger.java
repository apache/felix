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
package org.apache.felix.framework;

import java.lang.reflect.Method;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

/**
 * <p>
 * This class mimics the standard OSGi <tt>LogService</tt> interface. An
 * instance of this class is used by the framework for all logging. By default
 * this class logs messages to standard out. The log level can be set to
 * control the amount of logging performed, where a higher number results in
 * more logging. A log level of zero turns off logging completely.
 * </p>
 * <p>
 * The log levels match those specified in the OSGi Log Service (i.e., 1 = error,
 * 2 = warning, 3 = information, and 4 = debug). The default value is 1.
 * </p>
**/
public class Logger extends org.apache.felix.resolver.Logger
{
    private Object[] m_logger;

    public Logger()
    {
        super(LOG_ERROR);
    }

    public void setLogger(Object logger)
    {
        if (logger == null)
        {
            m_logger = null;
        }
        else
        {
            try
            {
                Method mth = logger.getClass().getMethod("log",
                        Integer.TYPE, String.class, Throwable.class);
                mth.setAccessible(true);
                m_logger = new Object[] { logger, mth };
            }
            catch (NoSuchMethodException ex)
            {
                System.err.println("Logger: " + ex);
                m_logger = null;
            }
        }
    }

    public final void log(ServiceReference sr, int level, String msg)
    {
        _log(null, sr, level, msg, null);
    }

    public final void log(ServiceReference sr, int level, String msg, Throwable throwable)
    {
<<<<<<< HEAD
        // TODO: Find a way to log to a log service inside the framework.
        // The issue is that we log messages while holding framework
        // internal locks -- hence, when a log service calls back into 
        // the framework (e.g., by loading a class) we might deadlock. 
        // One instance of this problem is tracked in FELIX-536.
        // For now we just disable logging to log services inside the
        // framework. 

        // m_context = context;
        // startListeningForLogService();
=======
        _log(null, sr, level, msg, throwable);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    public final void log(Bundle bundle, int level, String msg)
    {
<<<<<<< HEAD
        _log(null, null, level, msg, null);
=======
        _log(bundle, null, level, msg, null);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    public final void log(Bundle bundle, int level, String msg, Throwable throwable)
    {
<<<<<<< HEAD
        _log(null, null, level, msg, throwable);
=======
        _log(bundle, null, level, msg, throwable);
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    protected void _log(
            Bundle bundle, ServiceReference sr, int level,
            String msg, Throwable throwable)
    {
<<<<<<< HEAD
        _log(null, sr, level, msg, null);
=======
        if (getLogLevel() >= level)
        {
            // Default logging action.
            doLog(bundle, sr, level, msg, throwable);
        }
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
    }

    protected void doLog(
        Bundle bundle, ServiceReference sr, int level,
        String msg, Throwable throwable)
    {
<<<<<<< HEAD
        _log(null, sr, level, msg, throwable);
    }

    public final void log(Bundle bundle, int level, String msg)
    {
        _log(bundle, null, level, msg, null);
    }

    public final void log(Bundle bundle, int level, String msg, Throwable throwable)
    {
        _log(bundle, null, level, msg, throwable);
    }

    protected void doLog(
        Bundle bundle, ServiceReference sr, int level,
        String msg, Throwable throwable)
    {
        String s = "";
        if (sr != null)
        {
            s = s + "SvcRef "  + sr + " ";
        }
        else if (bundle != null)
        {
            s = s + "Bundle " + bundle.toString() + " ";
        }
        s = s + msg;
        if (throwable != null)
        {
            s = s + " (" + throwable + ")";
        }
=======
        StringBuilder s = new StringBuilder();
        if (sr != null)
        {
            s.append("SvcRef ").append(sr).append(" ").append(msg);
        }
        else if (bundle != null)
        {
            s.append("Bundle ").append(bundle.toString()).append(" ").append(msg);
        }
        else
        {
            s.append(msg);
        }
        if (throwable != null)
        {
            s.append(" (").append(throwable).append(")");
        }
        doLog(level, s.toString(), throwable);
    }

    protected void doLog(int level, String msg, Throwable throwable)
    {
        if (m_logger != null)
        {
            doLogReflectively(level, msg, throwable);
        }
        else
        {
            doLogOut(level, msg, throwable);
        }
    }

    protected void doLogOut(int level, String s, Throwable throwable)
    {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        switch (level)
        {
            case LOG_DEBUG:
                System.out.println("DEBUG: " + s);
                break;
            case LOG_ERROR:
                System.out.println("ERROR: " + s);
                if (throwable != null)
                {
                    if ((throwable instanceof BundleException) &&
                        (((BundleException) throwable).getNestedException() != null))
                    {
                        throwable = ((BundleException) throwable).getNestedException();
                    }
                    throwable.printStackTrace();
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

<<<<<<< HEAD
    private void _log(
        Bundle bundle, ServiceReference sr, int level,
        String msg, Throwable throwable)
    {
        // Save our own copy just in case it changes. We could try to do
        // more conservative locking here, but let's be optimistic.
        Object[] logger = m_logger;

        if (m_logLevel >= level)
        {
            // Use the log service if available.
            if (logger != null)
            {
                _logReflectively(logger, sr, level, msg, throwable);
            }
            // Otherwise, default logging action.
            else
            {
                doLog(bundle, sr, level, msg, throwable);
            }
        }
    }

    private void _logReflectively(
        Object[] logger, ServiceReference sr, int level, String msg, Throwable throwable)
    {
        if (logger != null)
        {
            Object[] params = {
                sr, new Integer(level), msg, throwable
            };
            try
            {
                ((Method) logger[LOGGER_METHOD_IDX]).invoke(logger[LOGGER_OBJECT_IDX], params);
            }
            catch (InvocationTargetException ex)
            {
                System.err.println("Logger: " + ex);
            }
            catch (IllegalAccessException ex)
            {
                System.err.println("Logger: " + ex);
            }
        }
    }

    /**
     * This method is called when the system bundle context is set;
     * it simply adds a service listener so that the system bundle can track
     * log services to be used as the back end of the logging mechanism. It also
     * attempts to get an existing log service, if present, but in general
     * there will never be a log service present since the system bundle is
     * started before every other bundle.
    **/
    private synchronized void startListeningForLogService()
    {
        // Add a service listener for log services.
=======
    protected void doLogReflectively(int level, String msg, Throwable throwable)
    {
>>>>>>> 502e622adcc798bcbd433d6b42ca78673cfab368
        try
        {
            ((Method) m_logger[1]).invoke(
                    m_logger[0],
                    level,
                    msg,
                    throwable
            );
        }
        catch (Exception ex)
        {
            System.err.println("Logger: " + ex);
        }
    }
}
