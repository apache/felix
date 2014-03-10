/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.online.manipulator;

import static java.lang.String.format;

import java.io.PrintStream;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
* Default implementation based on System.out
*/
public class SystemLogService implements LogService {

    public void log(final int level, final String message) {
        log(null, level, message, null);
    }

    public void log(final int level, final String message, final Throwable throwable) {
        log(null, level, message, throwable);
    }

    public void log(final ServiceReference reference, final int level, final String message) {
        log(null, level, message, null);
    }

    public void log(final ServiceReference reference, final int level, final String message, final Throwable throwable) {
        PrintStream stream = System.out;
        if (level >= LogService.LOG_WARNING) {
            stream = System.err;
        }

        String formatted = format("%s %s", asString(level), message);

        if (reference != null) {
            formatted = format("[%s] %s", reference.getBundle().getSymbolicName(), formatted);
        }

        // Print the message
        stream.println(formatted);
        if (throwable != null) {
            throwable.printStackTrace(stream);
        }
    }

    private static String asString(final int level) {
        String levelStr = "?";
        switch (level) {
            case LOG_DEBUG:
                levelStr = "D";
                break;
            case LOG_INFO:
                levelStr = "I";
                break;
            case LOG_WARNING:
                levelStr = "W";
                break;
            case LOG_ERROR:
                levelStr = "E";
                break;
        }
        return levelStr;
    }
}
