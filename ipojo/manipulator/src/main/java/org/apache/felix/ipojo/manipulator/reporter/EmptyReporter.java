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

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.manipulator.Reporter;
import org.apache.felix.ipojo.manipulator.util.Collections5;

/**
 * An {@code EmptyReporter} is the basis implementation for Reporters.
 * It is basically a no-op implementation.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class EmptyReporter implements Reporter {

    /**
     * List of warnings
     */
    private List<String> m_warnings = new ArrayList<String>();

    /**
     * List of errors
     */
    private List<String> m_errors = new ArrayList<String>();

    public void trace(String message, Object... args) {}

    public void info(String message, Object... args) {}

    public void warn(String message, Object... args) {}

    public void error(String message, Object... args) {}

    public List<String> getErrors() {
        return m_errors;
    }

    public List<String> getWarnings() {
        return m_warnings;
    }

    protected Object[] getMessageArguments(Object... args) {
        Object[] params = args;
        if (args != null && args.length > 1) {
            if (args[args.length - 1] instanceof Throwable) {
                // last argument is an Exception, remove it
                params = Collections5.copyOf(args, args.length - 1);
            }
        }

        return params;
    }

    protected Throwable getThrowable(Object... args) {
        Throwable throwable = null;
        if (args != null && args.length > 1) {
            if (args[args.length - 1] instanceof Throwable) {
                // last argument is an Exception
                throwable = (Throwable) args[args.length - 1];
            }
        }
        return throwable;
    }
}
