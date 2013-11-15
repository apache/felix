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

package org.apache.felix.http.sslfilter.internal;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
final class SystemLogger
{
    private static volatile LogService log;

    private SystemLogger()
    {
        // Nop
    }

    static void setLogService(LogService _log)
    {
        log = _log;
    }

    static LogService getLogService()
    {
        return log;
    }

    public static void log(int level, String message)
    {
        LogService service = getLogService();
        if (service != null)
        {
            service.log(level, message);
        }
    }

    public static void log(int level, String message, Throwable exception)
    {
        LogService service = getLogService();
        if (service != null)
        {
            service.log(level, message, exception);
        }
    }

    public static void log(ServiceReference sr, int level, String message)
    {
        LogService service = getLogService();
        if (service != null)
        {
            service.log(sr, level, message);
        }
    }

    public static void log(ServiceReference sr, int level, String message, Throwable exception)
    {
        LogService service = getLogService();
        if (service != null)
        {
            service.log(sr, level, message, exception);
        }
    }
}
