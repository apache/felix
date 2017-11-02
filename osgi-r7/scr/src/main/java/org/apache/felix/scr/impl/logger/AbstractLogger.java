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
package org.apache.felix.scr.impl.logger;

import java.text.MessageFormat;

import org.apache.felix.scr.impl.manager.ScrConfiguration;
import org.osgi.framework.Bundle;

/**
 * This is a common base for all loggers
 *
 */
public abstract class AbstractLogger
{
    private final ScrConfiguration config;

    /**
     * The prefix put for each log message
     */
    private volatile String prefix;

    AbstractLogger(final ScrConfiguration config, final String prefix)
    {
        this.config = config;
        this.prefix = prefix;
    }

    ScrConfiguration getConfiguration()
    {
        return this.config;
    }

    void setPrefix(final String value)
    {
        this.prefix = value;
    }

    String getPrefix()
    {
        return this.prefix;
    }

    /**
     * Get the internal logger
     * @return The internal logger
     */
    abstract InternalLogger getLogger();

    /**
     * Returns {@code true} if logging for the given level is enabled.
     */
    public boolean isLogEnabled(final int level)
    {
        return config.getLogLevel() >= level
               && getLogger().isLogEnabled(level);
    }

    /**
     * Method to actually emit the log message. If the LogService is available,
     * the message will be logged through the LogService. Otherwise the message
     * is logged to stdout (or stderr in case of LOG_ERROR level messages),
     *
     * @param level The log level to log the message at
     * @param pattern The {@code java.text.MessageFormat} message format
     *      string for preparing the message
     * @param ex An optional <code>Throwable</code> whose stack trace is written,
     * @param arguments The format arguments for the <code>pattern</code>
     *      string.
     */
    public void log(final int level, final String pattern, final Throwable ex, final Object... arguments )
    {
        if ( isLogEnabled( level ) )
        {
            getLogger().log(level, format(pattern, arguments), ex);
        }
    }

    /**
     * Method to actually emit the log message. If the LogService is available,
     * the message will be logged through the LogService. Otherwise the message
     * is logged to stdout (or stderr in case of LOG_ERROR level messages),
     *
     * @param level The log level of the messages. This corresponds to the log
     *          levels defined by the OSGi LogService.
     * @param message The message to print
     * @param ex The <code>Throwable</code> causing the message to be logged.
     */
    public void log(final int level, final String message, final Throwable ex)
    {
        if ( isLogEnabled( level ) )
        {
            getLogger().log(level, prefix.concat(message), ex);
        }
    }

    static String getBundleIdentifier(final Bundle bundle)
    {
        final StringBuilder sb = new StringBuilder("bundle ");
        // symbolic name might be null
        if ( bundle.getSymbolicName() != null )
        {
            sb.append(bundle.getSymbolicName());
            sb.append(':');
            sb.append(bundle.getVersion());
            sb.append( " (" );
            sb.append( bundle.getBundleId() );
            sb.append( ")" );
        }
        else
        {
            sb.append( bundle.getBundleId() );
        }

        return sb.toString();
    }

    private String format( final String pattern, final Object... arguments )
    {
        final String message;
        if ( arguments == null || arguments.length == 0 )
        {
            message = pattern;
        }
        else
        {
            for(int i=0;i<arguments.length;i++)
            {
                if ( arguments[i] instanceof Bundle )
                {
                    arguments[i] = getBundleIdentifier((Bundle)arguments[i]);
                }
            }
            message = MessageFormat.format( pattern, arguments );
        }
        return prefix.concat(message);
    }
}