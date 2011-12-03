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

package org.apache.felix.ipojo.plugin;

import org.apache.felix.ipojo.manipulator.reporter.EmptyReporter;
import org.apache.maven.plugin.logging.Log;

/**
 * A {@code MavenReporter} wraps a maven logging system into an iPOJO Reporter.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MavenReporter extends EmptyReporter {

    /**
     * Maven logger.
     */
    private Log log;

    public MavenReporter(Log log) {
        this.log = log;
    }

    @Override
    public void trace(String message, Object... args) {
        String formatted = String.format(message, getMessageArguments(args));
        Throwable t = getThrowable(args);
        if (t != null) {
            log.debug(formatted, t);
        } else {
            log.debug(formatted);
        }
    }

    @Override
    public void info(String message, Object... args) {
        String formatted = String.format(message, getMessageArguments(args));
        Throwable t = getThrowable(args);
        if (t != null) {
            log.info(formatted, t);
        } else {
            log.info(formatted);
        }
    }

    @Override
    public void warn(String message, Object... args) {
        String formatted = String.format(message, getMessageArguments(args));
        Throwable t = getThrowable(args);
        if (t != null) {
            log.warn(formatted, t);
        } else {
            log.warn(formatted);
        }
        getWarnings().add(formatted);
    }

    @Override
    public void error(String message, Object... args) {
        String formatted = String.format(message, getMessageArguments(args));
        Throwable t = getThrowable(args);
        if (t != null) {
            log.error(formatted, t);
        } else {
            log.error(formatted);
        }
        getErrors().add(formatted);
    }
}
