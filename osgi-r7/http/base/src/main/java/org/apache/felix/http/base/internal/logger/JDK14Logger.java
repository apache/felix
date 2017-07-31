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
package org.apache.felix.http.base.internal.logger;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Logger based on Java Logging API.
 *
 */
public final class JDK14Logger
    extends AbstractLogger
{
    private final Logger defaultLogger = Logger.getLogger(LogServiceLogger.class.getName());
    private final Level defaultLevel = Level.OFF;
    private BundleContext context;

    public JDK14Logger(BundleContext context)
    {
        this.context = context;
    }

    @Override
    public void log(ServiceReference ref, int level, String message, Throwable cause)
    {
        Object service = null;
        Class clazz = null;
        Logger logger = null;

        if(ref != null)
        {
            service = context.getService(ref);
        }

        if(service != null)
        {
            clazz = service.getClass();
        }

        if(clazz != null)
        {
            logger = Logger.getLogger(clazz.getName());
        }
        else
        {
            logger = defaultLogger;
        }

        Level logLevel = defaultLevel;
        switch (level)
        {
            case LOG_DEBUG:
                logLevel = Level.FINE;
                break;
            case LOG_INFO:
                logLevel = Level.INFO;
                break;
            case LOG_WARNING:
                logLevel = Level.WARNING;
                break;
            case LOG_ERROR:
                logLevel = Level.SEVERE;
                break;
        }

        if (cause != null)
        {
            logger.log(logLevel, message, cause);
        }
        else
        {
            logger.log(logLevel, message);
        }
    }
}
