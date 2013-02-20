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
package org.apache.felix.dm.test;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * Base class for all test cases.
 */
public class Base implements LogService, FrameworkListener {
    public static final String OSGI_SPEC_VERSION = "4.2.0";
    private final static int LOG_LEVEL = LogService.LOG_WARNING;
    private volatile boolean m_errorsLogged;

    /**
     * Register us as a LogService
     * 
     * @param context
     */
    @Before
    public void startup(BundleContext context) {
        context.registerService(LogService.class.getName(), this, null);
        context.addFrameworkListener(this);
    }

    /**
     * Always cleanup our bundle location file (because pax seems to forget to
     * cleanup it)
     * 
     * @param context
     */

    @After
    public void tearDown(BundleContext context) {
        // The following code forces the temporary bundle files (from /tmp/tb/*)
        // to be deleted when jvm exits
        // (this patch seems to be only required with pax examp 2.0.0)

        try {
            File f = new File(new URL(context.getBundle().getLocation()).getPath());
            f.deleteOnExit();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        context.removeFrameworkListener(this);
    }

    /**
     * Suspend the current thread for a while.
     * 
     * @param n
     *            the number of milliseconds to wait for.
     */
    protected void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

    public void log(int level, String message) {
        checkError(level, null);
        if (LOG_LEVEL >= level) {
            System.out.println(getLevel(level) + " " + message);
        }
    }

    public void log(int level, String message, Throwable exception) {
        checkError(level, exception);
        if (LOG_LEVEL >= level) {
            StringBuilder sb = new StringBuilder();
            sb.append(getLevel(level) + " ");
            sb.append(message);
            parse(sb, exception);
            System.out.println(sb.toString());
        }
    }

    public void log(ServiceReference sr, int level, String message) {
        checkError(level, null);
        if (LOG_LEVEL >= level) {
            StringBuilder sb = new StringBuilder();
            sb.append(getLevel(level) + " ");
            sb.append(message);
            System.out.println(sb.toString());
        }
    }

    public void log(ServiceReference sr, int level, String message, Throwable exception) {
        checkError(level, exception);
        if (LOG_LEVEL >= level) {
            StringBuilder sb = new StringBuilder();
            sb.append(getLevel(level) + " ");
            sb.append(message);
            parse(sb, exception);
            System.out.println(sb.toString());
        }
    }

    protected boolean errorsLogged() {
        return m_errorsLogged;
    }

    private void parse(StringBuilder sb, Throwable t) {
        if (t != null) {
            sb.append(" - ");
            StringWriter buffer = new StringWriter();
            PrintWriter pw = new PrintWriter(buffer);
            t.printStackTrace(pw);
            sb.append(buffer.toString());
            m_errorsLogged = true;
        }
    }

    private String getLevel(int level) {
        switch (level) {
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

    private void checkError(int level, Throwable exception) {
        if (level >= LOG_ERROR) {
            m_errorsLogged = true;
        }
        if (exception != null) {
            m_errorsLogged = true;
        }
    }

    public void frameworkEvent(FrameworkEvent event) {
        int eventType = event.getType();
        String msg = getFrameworkEventMessage(eventType);
        int level = (eventType == FrameworkEvent.ERROR) ? LOG_ERROR : LOG_WARNING;
        if (msg != null) {
            log(level, msg, event.getThrowable());
        } else {
            log(level, "Unknown fwk event: " + event);
        }
    }

    private String getFrameworkEventMessage(int event) {
        switch (event) {
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
}
