/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.jetty.internal;

import java.text.MessageFormat;

import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.eclipse.jetty.util.log.Logger;

public final class JettyLogger implements Logger
{
    private final String name;
    private boolean debugEnabled;

    public JettyLogger()
    {
        this("org.eclipse.jetty.log");
    }

    public JettyLogger(String name)
    {
        this.name = name;
    }

    public Logger getLogger(String name)
    {
        JettyLogger logger = new JettyLogger(name);
        logger.setDebugEnabled(this.debugEnabled);
        return logger;
    }

    public boolean isDebugEnabled()
    {
        return this.debugEnabled;
    }

    public void setDebugEnabled(boolean enabled)
    {
        this.debugEnabled = enabled;
    }

    public void debug(Throwable throwable)
    {
        if (this.debugEnabled)
        {
            SystemLogger.debug(throwable.getMessage());
        }

    }

    public void debug(String msg, Object... args)
    {
        if (this.debugEnabled)
        {
            SystemLogger.debug(MessageFormat.format(msg, args));
        }
    }

    @Override
    public void debug(String msg, long value)
    {
        debug(msg, value);
    }

    public void debug(String msg, Throwable throwable)
    {
        if (this.debugEnabled)
        {
            SystemLogger.debug(msg + ": " + throwable.getMessage());
        }
    }

    public String getName()
    {
        return name;
    }

    public void ignore(Throwable throwable)
    {

    }

    public void info(Throwable throwable)
    {
        SystemLogger.info(throwable.getMessage());
    }

    public void info(String msg, Object... args)
    {
        SystemLogger.info(MessageFormat.format(msg, args));

    }

    public void info(String msg, Throwable throwable)
    {
        SystemLogger.info(msg + ": " + throwable.getMessage());
    }

    public void warn(Throwable throwable)
    {
        SystemLogger.warning(null, throwable);
    }

    public void warn(String msg, Object... args)
    {
        SystemLogger.warning(MessageFormat.format(msg, args), null);

    }

    public void warn(String msg, Throwable throwable)
    {
        SystemLogger.warning(msg, throwable);
    }

}
