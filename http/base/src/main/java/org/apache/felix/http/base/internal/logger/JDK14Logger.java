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

import org.osgi.service.log.LogService;

/**
 * Logger based on Java Logging API.
 */
public final class JDK14Logger implements InternalLogger
{
    private final Logger logger = Logger.getLogger("org.apache.felix.http");

    private Level getLevel(final int level)
    {
        Level logLevel;
        switch (level)
        {
            case LogService.LOG_DEBUG:
                logLevel = Level.FINE;
                break;
            case LogService.LOG_INFO:
                logLevel = Level.INFO;
                break;
            case LogService.LOG_WARNING:
                logLevel = Level.WARNING;
                break;
            case LogService.LOG_ERROR:
                logLevel = Level.SEVERE;
                break;
            default: logLevel = Level.FINE;
        }
        return logLevel;
    }

    @Override
    public boolean isLogEnabled(final int level) {
        return this.logger.isLoggable(getLevel(level));
    }

    @Override
    public void log(final int level, final String message, final Throwable exception)
    {
        final Level logLevel = getLevel(level);

        if (exception != null)
        {
            this.logger.log(logLevel, message, exception);
        }
        else
        {
            this.logger.log(logLevel, message);
        }
    }
}
