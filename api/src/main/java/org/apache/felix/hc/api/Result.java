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

import java.util.Iterator;

/** The result of executing a {@link HealthCheck} */
public class Result implements Iterable<ResultLog.Entry> {

    protected final ResultLog resultLog;

    public enum Status {
        OK, // no problem
        WARN, // health check detected something wrong but not critical
        CRITICAL, // health check detected a critical problem
        HEALTH_CHECK_ERROR // health check did not execute properly
    }

    /** Build a single-value Result
     * 
     * @param s if lower than OK, our status is set to OK */
    public Result(final Status s, final String explanation) {
        resultLog = new ResultLog().add(new ResultLog.Entry(s, explanation));
    }

    /** Build a single-value Result with exception
     * 
     * @param s if lower than OK, our status is set to OK */
    public Result(final Status s, final String explanation, final Exception e) {
        resultLog = new ResultLog().add(new ResultLog.Entry(s, explanation, e));
    }

    /** Build a a Result based on a ResultLog, which can provide more details than a single-value Result. */
    public Result(final ResultLog log) {
        resultLog = new ResultLog(log);
    }

    /** True if our status is OK - provides a convenient way of checking that. */
    public boolean isOk() {
        return getStatus().equals(Status.OK);
    }

    /** Return our Status */
    public Status getStatus() {
        return resultLog.getAggregateStatus();
    }

    /** Return an Iterator on the entries of our ResultLog */
    @Override
    public Iterator<ResultLog.Entry> iterator() {
        return resultLog.iterator();
    }

    @Override
    public String toString() {
        return "Result [status=" + getStatus() + ", resultLog=" + resultLog + "]";
    }

}