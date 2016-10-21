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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

/**
 * OSGi log service which logs messages to standard output.
 * This class can also be used to detect if the ConfigurationAdmin service has logged
 * some warnings during a stress integration test.
 */
public class Log implements LogService, FrameworkListener
{
    // Default OSGI log service level logged to standard output.
    private final static int LOG_LEVEL = LogService.LOG_WARNING;

    // Flag used to check if some errors have been logged during the execution of a given test.
    private volatile boolean m_errorsLogged;

    // We implement OSGI log service.
    protected ServiceRegistration logService;

    // Bundle context used to register our log listener
    private BundleContext ctx;

    /**
     * Default constructor. 
     * @Param ctx the Bundle Context used to register this log service. The {@link #close} must
     * be called when the logger is not used anymore.
     */
    public Log(BundleContext ctx)
    {
        this.ctx = ctx;
        logService = ctx.registerService(LogService.class.getName(), this, null);
        ctx.addFrameworkListener(this);
    }

    /**
     * Unregister our log listener
     */
    public void close()
    {
        logService.unregister();
        ctx.removeFrameworkListener(this);
    }

    public void log(int level, String message)
    {
        checkError(level, null);
        if (LOG_LEVEL >= level)
        {
            System.out.println(getLevel(level) + " - " + Thread.currentThread().getName() + " : " + message);
        }
    }

    public void log(int level, String message, Throwable exception)
    {
        checkError(level, exception);
        if (LOG_LEVEL >= level)
        {
            StringBuilder sb = new StringBuilder();
            sb.append(getLevel(level) + " - " + Thread.currentThread().getName() + " : ");
            sb.append(message);
            parse(sb, exception);
            System.out.println(sb.toString());
        }
    }

    public void log(ServiceReference sr, int level, String message)
    {
        checkError(level, null);
        if (LOG_LEVEL >= level)
        {
            StringBuilder sb = new StringBuilder();
            sb.append(getLevel(level) + " - " + Thread.currentThread().getName() + " : ");
            sb.append(message);
            System.out.println(sb.toString());
        }
    }

    public void log(ServiceReference sr, int level, String message, Throwable exception)
    {
        checkError(level, exception);
        if (LOG_LEVEL >= level)
        {
            StringBuilder sb = new StringBuilder();
            sb.append(getLevel(level) + " - " + Thread.currentThread().getName() + " : ");
            sb.append(message);
            parse(sb, exception);
            System.out.println(sb.toString());
        }
    }

    public boolean errorsLogged()
    {
        return m_errorsLogged;
    }

    private void parse(StringBuilder sb, Throwable t)
    {
        if (t != null)
        {
            sb.append(" - ");
            StringWriter buffer = new StringWriter();
            PrintWriter pw = new PrintWriter(buffer);
            t.printStackTrace(pw);
            sb.append(buffer.toString());
            m_errorsLogged = true;
        }
    }

    private String getLevel(int level)
    {
        switch (level)
        {
            case LogService.LOG_DEBUG:
                return "DEBUG";
            case LogService.LOG_ERROR:
                return "ERROR";
            case LogService.LOG_INFO:
                return "INFO";
            case LogService.LOG_WARNING:
                return "WARN";
            default:
                return "";
        }
    }

    private void checkError(int level, Throwable exception)
    {
        if (level <= LOG_ERROR)
        {
            m_errorsLogged = true;
        }
        if (exception != null)
        {
            m_errorsLogged = true;
        }
    }

    public void frameworkEvent(FrameworkEvent event)
    {
        int eventType = event.getType();
        String msg = getFrameworkEventMessage(eventType);
        int level = (eventType == FrameworkEvent.ERROR) ? LOG_ERROR : LOG_WARNING;
        if (msg != null)
        {
            log(level, msg, event.getThrowable());
        }
        else
        {
            log(level, "Unknown fwk event: " + event);
        }
    }

    private String getFrameworkEventMessage(int event)
    {
        switch (event)
        {
            case FrameworkEvent.ERROR:
                return "FrameworkEvent: ERROR";
            case FrameworkEvent.INFO:
                return "FrameworkEvent INFO";
            case FrameworkEvent.PACKAGES_REFRESHED:
                return "FrameworkEvent: PACKAGE REFRESHED";
            case FrameworkEvent.STARTED:
                return "FrameworkEvent: STARTED";
            case FrameworkEvent.STARTLEVEL_CHANGED:
                return "FrameworkEvent: STARTLEVEL CHANGED";
            case FrameworkEvent.WARNING:
                return "FrameworkEvent: WARNING";
            default:
                return null;
        }
    }

    public void warn(String msg, Object... params)
    {
        if (LOG_LEVEL >= LogService.LOG_WARNING)
        {
            log(LogService.LOG_WARNING, params.length > 0 ? String.format(msg, params) : msg);
        }
    }

    public void info(String msg, Object... params)
    {
        if (LOG_LEVEL >= LogService.LOG_INFO)
        {
            log(LogService.LOG_INFO, params.length > 0 ? String.format(msg, params) : msg);
        }
    }

    public void debug(String msg, Object... params)
    {
        if (LOG_LEVEL >= LogService.LOG_DEBUG)
        {
            log(LogService.LOG_DEBUG, params.length > 0 ? String.format(msg, params) : msg);
        }
    }

    public void error(String msg, Object... params)
    {
        log(LogService.LOG_ERROR, params.length > 0 ? String.format(msg, params) : msg);
    }

    public void error(String msg, Throwable err, Object... params)
    {
        log(LogService.LOG_ERROR, params.length > 0 ? String.format(msg, params) : msg, err);
    }

    public void error(Throwable err)
    {
        log(LogService.LOG_ERROR, "error", err);
    }
}
