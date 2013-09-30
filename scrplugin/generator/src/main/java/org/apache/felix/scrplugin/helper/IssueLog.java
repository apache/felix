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
package org.apache.felix.scrplugin.helper;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.felix.scrplugin.Log;

/**
 * Utility class for handling errors and warnings
 */
public class IssueLog {

    private final boolean strictMode;

    private final List<Entry> errors = new ArrayList<Entry>();

    private final List<Entry> warnings = new ArrayList<Entry>();

    private final List<Entry> deprecationWarnings = new ArrayList<Entry>();

    public IssueLog(final boolean strictMode) {
        this.strictMode = strictMode;
    }

    public int getNumberOfErrors() {
        return this.errors.size();
    }

    public boolean hasErrors() {
        return errors.size() > 0 || (this.strictMode && (warnings.size() > 0 || this.deprecationWarnings.size() > 0));
    }

    public void addError(final String message, final String location) {
        errors.add(new Entry(message, location));
    }

    public void addWarning(final String message, final String location) {
        warnings.add(new Entry(message, location));
    }

    public void addDeprecationWarning(final String message, final String location) {
        deprecationWarnings.add(new Entry(message, location));
    }

    public void logMessages(final Log log) {
        // now log warnings and errors (warnings first)
        // in strict mode everything is an error!
        final Iterator<Entry> depWarnings = this.deprecationWarnings.iterator();
        while (depWarnings.hasNext()) {
            final Entry entry = depWarnings.next();
            if (strictMode) {
                log.error(entry.message, entry.location, entry.lineNumber, entry.columnNumber);
            } else {
                log.warn(entry.message, entry.location, entry.lineNumber, entry.columnNumber);
            }
        }
        if (this.deprecationWarnings.size() > 0) {
            final String msg = "It is highly recommended to fix these problems, as future versions might not "
                            + "support these features anymore.";
            if (strictMode) {
                log.error(msg);
            } else {
                log.warn(msg);
            }
        }

        final Iterator<Entry> warnings = this.warnings.iterator();
        while (warnings.hasNext()) {
            final Entry entry = warnings.next();
            if (strictMode) {
                log.error(entry.message, entry.location, entry.lineNumber, entry.columnNumber);
            } else {
                log.warn(entry.message, entry.location, entry.lineNumber, entry.columnNumber);
            }
        }

        final Iterator<Entry> errors = this.errors.iterator();
        while (errors.hasNext()) {
            final Entry entry = errors.next();
            log.error(entry.message, entry.location, entry.lineNumber, entry.columnNumber);
        }
    }

    private static class Entry {

    	static final int LINE_NUMBER_UNKNOWN = 1;
    	static final int COLUMN_NUMBER_UNKNOWN = 1;

        final String message;
        final String location;
        final int lineNumber;
        final int columnNumber;

        Entry(final String message, final String location) {
        	this(message, location, LINE_NUMBER_UNKNOWN, COLUMN_NUMBER_UNKNOWN);
        }

        Entry(final String message, final String location, final int lineNumber, final int columnNumber) {
        	this.message = message;
        	this.location = location;
        	this.lineNumber = lineNumber;
        	this.columnNumber = columnNumber;
        }


        @Override
        public String toString() {
            return this.location + " [" + this.lineNumber+ "," + this.columnNumber+"] : " + this.message;
        }
    }
}
