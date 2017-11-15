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
package org.apache.felix.configurator.impl.logger;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

public final class SystemLogger {

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

    private static void log(final int level, final String message, final Throwable cause) {
        final LogServiceEnabledLogger l = LOGGER;
        if ( l != null ) {
            l.log(level, message, cause);
        }
    }

    public static void debug(final String message) {
        log(LogService.LOG_DEBUG, message, null);
    }

    public static void debug(final String message, final Throwable cause) {
        log(LogService.LOG_DEBUG, message, cause);
    }

    public static void info(final String message) {
        log(LogService.LOG_INFO, message, null);
    }

    public static void warning(final String message) {
        log(LogService.LOG_WARNING, message, null);
    }

    public static void warning(final String message, final Throwable cause) {
        log(LogService.LOG_WARNING, message, cause);
    }

    public static void error(final String message) {
        log(LogService.LOG_ERROR, message, null);
    }

    public static void error(final String message, final Throwable cause) {
        log(LogService.LOG_ERROR, message, cause);
    }
}
