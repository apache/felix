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

package org.apache.felix.ipojo.manipulator.reporter;

import java.io.PrintStream;

/**
 * A {@code SystemReporter} reports feedback from within the manipulation process.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class SystemReporter extends EmptyReporter {

    /**
     * Micro enum used to prefix messages.
     */
    private enum Level {
        TRACE("#"),
        INFO("I"),
        WARN("W"),
        ERROR("E");

        private String value;

        Level(String value) {
            this.value = value;
        }

        public String append(String message) {
            return value + " " + message;
        }
    }

    /**
     * Enable/disable trace logging.
     */
    private boolean enableTrace = false;

    public void setEnableTrace(boolean enableTrace) {
        this.enableTrace = enableTrace;
    }

    private void log(PrintStream stream, Level level, String formatted, Throwable throwable) {
        // Print the message
        stream.println(level.append(formatted));

        // And the exception if any
        if (throwable != null) {
            throwable.printStackTrace(stream);
        }
    }

    @Override
    public void trace(String message, Object... args) {
        if (enableTrace) {
            String formatted = String.format(message, getMessageArguments(args));
            log(System.out, Level.TRACE, formatted, getThrowable());
        }
    }

    @Override
    public void info(String message, Object... args) {
        String formatted = String.format(message, getMessageArguments(args));
        log(System.out, Level.INFO, formatted, getThrowable());
    }

    @Override
    public void warn(String message, Object... args) {
        String formatted = String.format(message, getMessageArguments(args));
        Throwable throwable = getThrowable();
        log(System.out, Level.WARN, formatted, throwable);

        // Append Exception message if any
        if (throwable != null) {
            formatted += " ";
            formatted += throwable.getMessage();
        }
        getWarnings().add(formatted);
    }

    @Override
    public void error(String message, Object... args) {
        String formatted = String.format(message, getMessageArguments(args));
        Throwable throwable = getThrowable();
        log(System.out, Level.ERROR, formatted, throwable);

        // Append Exception message if any
        if (throwable != null) {
            formatted += " ";
            formatted += throwable.getMessage();
        }
        getErrors().add(formatted);
    }

}
