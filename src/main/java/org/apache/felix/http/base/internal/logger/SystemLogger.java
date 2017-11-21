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

import java.lang.reflect.Array;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

public final class SystemLogger
{
    private static volatile LogServiceEnabledLogger LOGGER;

    public static void init(final BundleContext bundleContext) {
        LOGGER = new LogServiceEnabledLogger(bundleContext);
    }

    public static void destroy() {
        if ( LOGGER != null ) {
            LOGGER.close();
            LOGGER = null;
        }
    }

    private static String getMessage(final ServiceReference<?> ref, final String message)
    {
        if ( ref == null )
        {
            return message;
        }
        final Bundle bundle = ref.getBundle();
        final StringBuilder ib = new StringBuilder();
        ib.append("[ServiceReference ");
        ib.append(String.valueOf(ref.getProperty(Constants.SERVICE_ID)));
        ib.append(" from bundle ");
        if ( bundle == null )
        {
            ib.append("<uninstalled>");
        }
        else
        {
            ib.append(bundle.getBundleId());
            if ( bundle.getSymbolicName() != null )
            {
                ib.append(" : ");
                ib.append(bundle.getSymbolicName());
                ib.append(":");
                ib.append(bundle.getVersion());
            }
        }
        ib.append(" ref=");
        ib.append(ref);
        ib.append(" properties={");
        boolean first = true;
        for(final String name : ref.getPropertyKeys())
        {
            if ( first )
            {
                first = false;
            }
            else
            {
                ib.append(", ");
            }
            final Object val = ref.getProperty(name);
            ib.append(name);
            ib.append("=");
            if ( val.getClass().isArray() )
            {
                boolean fa = true;
                ib.append('[');
                for(int i=0;i<Array.getLength(val);i++)
                {
                    if ( fa )
                    {
                        fa = false;
                    }
                    else
                    {
                        ib.append(", ");
                    }
                    ib.append(Array.get(val, i));
                }
            ib.append(']');
            }
            else
            {
                ib.append(val);
            }
        }
        ib.append("}] ");
        ib.append(message);

        return ib.toString();
    }

    private static void log(
            final int level,
            final ServiceReference<?> ref,
            final String message,
            final Throwable cause) {
        final LogServiceEnabledLogger l = LOGGER;
        if ( l != null ) {
            l.log(level, getMessage(ref, message), cause);
        }
    }

    public static void debug(final String message)
    {
        log(LogService.LOG_DEBUG, null, message, null);
    }

    public static void debug(final ServiceReference<?> ref,  final String message)
    {
        log(LogService.LOG_DEBUG, ref, message, null);
    }

    public static void debug(final String message, final Throwable cause)
    {
        log(LogService.LOG_DEBUG, null, message, cause);
    }

    public static void info(final String message)
    {
        log(LogService.LOG_INFO, null, message, null);
    }

    public static void warning(final String message, final Throwable cause)
    {
        log(LogService.LOG_WARNING, null, message, cause);
    }

    public static void error(final String message, final Throwable cause)
    {
        log(LogService.LOG_ERROR, null, message, cause);
    }

    public static void error(final ServiceReference<?> ref, final String message, final Throwable cause)
    {
        log(LogService.LOG_ERROR, ref, message, cause);
    }
}
