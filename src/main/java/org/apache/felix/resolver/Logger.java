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
package org.apache.felix.resolver;

import org.osgi.resource.Resource;

import org.osgi.service.resolver.ResolutionException;

/**
 * <p>
 * This class mimics the standard OSGi <tt>LogService</tt> interface. An
 * instance of this class is used by the framework for all logging. By default
 * this class logs messages to standard out. The log level can be set to control
 * the amount of logging performed, where a higher number results in more
 * logging. A log level of zero turns off logging completely.
 * </p>
 * <p>
 * The log levels match those specified in the OSGi Log Service (i.e., 1 =
 * error, 2 = warning, 3 = information, and 4 = debug). The default value is 1.
 * </p>
 * <p>
 * This class also uses the System Bundle's context to track log services and
 * will use the highest ranking log service, if present, as a back end instead
 * of printing to standard out. The class uses reflection to invoking the log
 * service's method to avoid a dependency on the log interface.
 * </p>
 */
public class Logger
{
    public static final int LOG_ERROR = 1;
    public static final int LOG_WARNING = 2;
    public static final int LOG_INFO = 3;
    public static final int LOG_DEBUG = 4;

    private int m_logLevel = 1;

    public Logger(int i)
    {
        m_logLevel = i;
    }

    public final synchronized void setLogLevel(int i)
    {
        m_logLevel = i;
    }

    public final synchronized int getLogLevel()
    {
        return m_logLevel;
    }

    public final void log(int level, String msg)
    {
        _log(level, msg, null);
    }

    public final void log(int level, String msg, Throwable throwable)
    {
        _log(level, msg, throwable);
    }

    protected void doLog(int level, String msg, Throwable throwable)
    {
        if (level > m_logLevel)
        {
            return;
        }
        String s = "";
        s = s + msg;
        if (throwable != null)
        {
            s = s + " (" + throwable + ")";
        }
        switch (level)
        {
            case LOG_DEBUG:
                System.out.println("DEBUG: " + s);
                break;
            case LOG_ERROR:
                System.out.println("ERROR: " + s);
                if (throwable != null)
                {
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

    private void _log(
        int level,
        String msg, Throwable throwable)
    {
        if (m_logLevel >= level)
        {
            doLog(level, msg, throwable);
        }
    }

    public void logUsesConstraintViolation(Resource resource, ResolutionException error)
    {
        // do nothing by default
    }
}
