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

package org.apache.felix.ipojo.bnd;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.manipulator.reporter.EmptyReporter;

import aQute.service.reporter.Reporter;

/**
 * A {@code BndReporter} knows how to wrap a Bnd Reporter into an iPOJO Reporter.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class BndReporter extends EmptyReporter {

    /**
     * Bnd reporter.
     */
    private Reporter m_reporter;

    /**
     * Errors which occur during the manipulation.
     */
    private List<String> m_errors = new ArrayList<String>();

    /**
     * Warnings which occur during the manipulation.
     */
    private List<String> m_warnings = new ArrayList<String>();

    public BndReporter(Reporter reporter) {
        m_reporter = reporter;
    }

    public List<String> getErrors() {
        return m_errors;
    }

    public List<String> getWarnings() {
        return m_warnings;
    }

    @Override
    public void trace(String message, Object... args) {
        m_reporter.trace(message, args);
    }

    @Override
    public void info(String message, Object... args) {
        m_reporter.trace(message, args);
    }

    @Override
    public void warn(String message, Object... args) {
        m_reporter.warning(message, args);
        m_warnings.add(String.format(message, args));
    }

    @Override
    public void error(String message, Object... args) {
        m_reporter.error(message, args);
        m_errors.add(String.format(message, args));
    }
}
