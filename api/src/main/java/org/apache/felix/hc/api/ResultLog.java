/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.felix.hc.api;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.felix.hc.api.Result.Status;

/** The log of a Result, allows for providing multiple lines of information which are aggregated as a single Result. */
public class ResultLog implements Iterable<ResultLog.Entry> {

    private List<Entry> entries = new LinkedList<Entry>();
    private Status aggregateStatus;

    /** An entry in this log */
    public static class Entry {
        private final Status status;
        private final String message;
        private final boolean isDebug;
        private final Exception exception;

        public Entry(Status s, String message) {
            this(s, message, false, null);
        }

        public Entry(String message, boolean isDebug) {
            this(Status.OK, message, isDebug, null);
        }
        public Entry(String message, boolean isDebug, Exception exception) {
            this(Status.OK, message, isDebug, exception);
        }
        
        public Entry(Status s, String message, Exception exception) {
            this(s, message, false, exception);
        }

        // private to not allow invalid combinations of isDebug=true and a status different than Status.OK
        private Entry(Status s, String message, boolean isDebug, Exception exception) {
            this.status = s;
            this.message = message;
            this.exception = exception;
            this.isDebug = isDebug;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(status.toString()).append(" ").append(message);
            if (exception != null) {
                builder.append(" Exception: " + exception.getMessage());
            }
            return builder.toString();
        }

        public Status getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public Exception getException() {
            return exception;
        }

        public boolean isDebug() {
            return isDebug;
        }
    }

    /** Build a log. Initial aggregate status is set to WARN, as an empty log is not considered ok. That's reset to OK before adding the
     * first log entry, and then the status aggregation rules take over. */
    public ResultLog() {
        aggregateStatus = Result.Status.WARN;
    }

    /** Create a copy of the result log */
    public ResultLog(final ResultLog log) {
        this.aggregateStatus = log.aggregateStatus;
        this.entries = new ArrayList<ResultLog.Entry>(log.entries);
    }

    /** Add an entry to this log. The aggregate status of this is set to the highest of the current aggregate status and the new Entry's
     * status */
    public ResultLog add(Entry e) {
        if (entries.isEmpty()) {
            aggregateStatus = Result.Status.OK;
        }
        entries.add(e);
        if (e.getStatus().ordinal() > aggregateStatus.ordinal()) {
            aggregateStatus = e.getStatus();
        }
        return this;
    }

    /** Return an Iterator on our entries */
    @Override
    public Iterator<ResultLog.Entry> iterator() {
        return entries.iterator();
    }

    /** Return our aggregate status, i.e. the highest status of the entries added to this log. Starts at OK for an empty ResultLog, so
     * cannot be lower than that. */
    public Status getAggregateStatus() {
        return aggregateStatus;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ResultLog: ");
        sb.append(this.entries.toString());
        return sb.toString();
    }
}