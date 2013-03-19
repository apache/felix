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
package org.apache.felix.ipojo.util;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ErrorHandler;
import org.apache.felix.ipojo.extender.internal.Extender;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * iPOJO Logger.
 * This class is an helper class implementing a simple log system.
 * This logger sends log messages to a log service if available.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Logger implements Log {

    /**
     * The iPOJO default log level property.
     */
    public static final String IPOJO_LOG_LEVEL_PROP = "ipojo.log.level";
    /**
     * iPOJO log level manifest header.
     * The uppercase 'I' is important as BND removes all headers that do not
     * start with an uppercase are not added to the bundle.
     * Use an upper case to support bnd.
     */
    public static final String IPOJO_LOG_LEVEL_HEADER = "IPOJO-log-level";
    /**
     * A shared log service implementation writing messages on the console.
     */
    private static final LogService m_defaultLogger = new ConsoleLogService();
    private static final String DEBUG_HEADER = "[DEBUG]";
    private static final String INFO_HEADER = "[INFO]";
    private static final String WARNING_HEADER = "[WARNING]";
    private static final String ERROR_HEADER = "[ERROR]";
    private static final String UNKNOWN_HEADER = "[UNKNOWN]";
    /**
     * The Bundle Context used to get the
     * log service.
     */
    private final BundleContext m_context;
    /**
     * The name of the logger.
     */
    private final String m_name;
    /**
     * The trace level of this logger.
     */
    private final int m_level;
    /**
     * The instance associated to the logger if any.
     */
    private ComponentInstance m_instance;

    /**
     * Creates a logger.
     *
     * @param context the bundle context
     * @param name    the name of the logger
     * @param level   the trace level
     */
    public Logger(BundleContext context, String name, int level) {
        m_name = name;
        m_level = level;
        m_context = context;
    }

    /**
     * Creates a logger.
     *
     * @param context  the bundle context
     * @param instance the instance
     * @param level    the trace level
     */
    public Logger(BundleContext context, ComponentInstance instance, int level) {
        this(context, instance.getInstanceName(), level);
        m_instance = instance;
    }

    /**
     * Create a logger.
     * Uses the default logger level.
     *
     * @param context the bundle context
     * @param name    the name of the logger
     */
    public Logger(BundleContext context, String name) {
        this(context, name, getDefaultLevel(context));
    }

    /**
     * Create a logger.
     * Uses the default logger level.
     *
     * @param context  the bundle context
     * @param instance the instance
     */
    public Logger(BundleContext context, ComponentInstance instance) {
        this(context, instance, getDefaultLevel(context));
    }

    /**
     * Gets the default logger level.
     * The property is searched inside the framework properties,
     * the system properties, and in the manifest from the given
     * bundle context. By default, set the level to {@link Logger#WARNING}.
     *
     * @param context the bundle context.
     * @return the default log level.
     */
    private static int getDefaultLevel(BundleContext context) {
        // First check in the framework and in the system properties
        String level = context.getProperty(IPOJO_LOG_LEVEL_PROP);

        // If null, look in the bundle manifest
        if (level == null) {
            String key = IPOJO_LOG_LEVEL_PROP.replace('.', '-');
            level = (String) context.getBundle().getHeaders().get(key);
        }

        // if still null try the second header
        if (level == null) {
            level = (String) context.getBundle().getHeaders().get(IPOJO_LOG_LEVEL_HEADER);
        }

        if (level != null) {
            if (level.equalsIgnoreCase("info")) {
                return INFO;
            } else if (level.equalsIgnoreCase("debug")) {
                return DEBUG;
            } else if (level.equalsIgnoreCase("warning")) {
                return WARNING;
            } else if (level.equalsIgnoreCase("error")) {
                return ERROR;
            }
        }

        // Either l is null, either the specified log level was unknown
        // Set the default to WARNING
        return WARNING;

    }

    private static String getLogHeaderForLevel(int level) {
        switch (level) {
            case DEBUG:
                return DEBUG_HEADER;
            case INFO:
                return INFO_HEADER;
            case WARNING:
                return WARNING_HEADER;
            case ERROR:
                return ERROR_HEADER;
            default:
                return UNKNOWN_HEADER;
        }
    }

    /**
     * Logs a message.
     *
     * @param level the level of the message
     * @param msg   the the message to log
     */
    public void log(int level, String msg) {
        if (m_level >= level) {
            dispatch(level, msg, null);
        }
        invokeErrorHandler(level, msg, null);
    }

    /**
     * Logs a message with an exception.
     *
     * @param level     the level of the message
     * @param msg       the message to log
     * @param exception the exception attached to the message
     */
    public void log(int level, String msg, Throwable exception) {
        if (m_level >= level) {
            dispatch(level, msg, exception);
        }
        invokeErrorHandler(level, msg, exception);
    }

    /**
     * Internal log method.
     *
     * @param level     the level of the message.
     * @param msg       the message to log
     * @param exception the exception attached to the message
     */
    private void dispatch(int level, String msg, Throwable exception) {
        LogService log = null;
        ServiceReference ref = null;
        try {
            // Security Check
            if (SecurityHelper.hasPermissionToGetService(LogService.class.getName(), m_context)) {
                ref = m_context.getServiceReference(LogService.class.getName());
            } else {
                Extender.getIPOJOBundleContext().getServiceReference(LogService.class.getName());
            }

            if (ref != null) {
                log = (LogService) m_context.getService(ref);
            }
        } catch (IllegalStateException e) {
            // Handle the case where the iPOJO bundle is stopping, or the log service already ran away.
        }

        if (log == null) {
            log = m_defaultLogger;
        }

        String name = m_name;
        if (name == null) {
            name = "";
        }

        String message = String.format("%s %s : %s", getLogHeaderForLevel(level), name, msg);
        switch (level) {
            case DEBUG:
                log.log(LogService.LOG_DEBUG, message, exception);
                break;
            case INFO:
                log.log(LogService.LOG_INFO, message, exception);
                break;
            case WARNING:
                log.log(LogService.LOG_WARNING, message, exception);
                break;
            case ERROR:
                log.log(LogService.LOG_ERROR, message, exception);
                break;
            default:
                log.log(LogService.LOG_INFO, message, exception);
                break;
        }

        if (ref != null) {
            m_context.ungetService(ref);
        }
    }

    /**
     * Invokes the error handler service is present.
     *
     * @param level the log level
     * @param msg   the message
     * @param error the error
     */
    private void invokeErrorHandler(int level, String msg, Throwable error) {
        // First check the level
        if (level > WARNING) {
            return; // Others levels are not supported.
        }
        // Try to get the error handler service
        try {
            ServiceReference ref = m_context.getServiceReference(ErrorHandler.class.getName());
            if (ref != null) {
                ErrorHandler handler = (ErrorHandler) m_context.getService(ref);
                if (level == ERROR) {
                    handler.onError(m_instance, msg, error);
                } else if (level == WARNING) {
                    handler.onWarning(m_instance, msg, error);
                } // The others case are not supported
                m_context.ungetService(ref);
            } // Else do nothing...
        } catch (IllegalStateException e) {
            // Ignore
            // The iPOJO bundle is stopping.
        }
    }

    /**
     * A simple log service implementation writing the messages and stack trace on System.err.
     */
    private static class ConsoleLogService implements LogService {

        public void log(int i, String s) {
            log(i, s, null);
        }

        public void log(int level, String message, Throwable exception) {
            System.err.println(message);
            if (exception != null) {
                exception.printStackTrace();
            }
        }

        public void log(ServiceReference serviceReference, int i, String s) {
            // not used.
        }

        public void log(ServiceReference serviceReference, int i, String s, Throwable throwable) {
            // not used.
        }
    }
}
