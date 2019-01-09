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
package org.apache.felix.hc.core.impl.executor;

import java.text.Collator;
import java.util.Date;

import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.api.execution.HealthCheckMetadata;

/** The result of executing a {@link HealthCheck}. */
public class ExecutionResult implements Comparable<ExecutionResult>, HealthCheckExecutionResult {

    private final Result resultFromHC;

    private final HealthCheckMetadata metaData;

    private final Date finishedAt;

    private final long elapsedTimeInMs;

    private final boolean timedOut;

    public ExecutionResult(final HealthCheckMetadata metadata,
            final Result simpleResult,
            final long elapsedTimeInMs,
            final boolean timedout) {
        this(metadata, simpleResult, new Date(), elapsedTimeInMs, timedout);
    }
    
    /** Full constructor */
    public ExecutionResult(final HealthCheckMetadata metadata,
            final Result simpleResult,
            final Date finishedAt,
            final long elapsedTimeInMs,
            final boolean timedout) {
        this.metaData = metadata;
        this.resultFromHC = simpleResult;
        this.finishedAt = finishedAt;
        this.elapsedTimeInMs = elapsedTimeInMs;
        this.timedOut = timedout;
    }

    /** Shortcut constructor for a result */
    public ExecutionResult(final HealthCheckMetadata metadata,
            final Result simpleResult,
            final long elapsedTimeInMs) {
        this(metadata, simpleResult, elapsedTimeInMs, false);
    }

    /** Shortcut constructor to create error/timed out result. */
    public ExecutionResult(final HealthCheckMetadata metadata,
            final Result.Status status,
            final String errorMessage,
            final long elapsedTime, boolean timedOut) {
        this(metadata, new Result(status, errorMessage), elapsedTime, timedOut);
    }

    @Override
    public Result getHealthCheckResult() {
        return this.resultFromHC;
    }

    @Override
    public String toString() {
        return "ExecutionResult [status=" + this.resultFromHC.getStatus() +
                ", finishedAt=" + finishedAt +
                ", elapsedTimeInMs=" + elapsedTimeInMs +
                ", timedOut=" + timedOut +
                "]";
    }

    @Override
    public long getElapsedTimeInMs() {
        return elapsedTimeInMs;
    }

    @Override
    public HealthCheckMetadata getHealthCheckMetadata() {
        return this.metaData;
    }

    @Override
    public Date getFinishedAt() {
        return finishedAt;
    }

    @Override
    public boolean hasTimedOut() {
        return this.timedOut;
    }

    /** Natural order of results (failed results are sorted before ok results). */
    @Override
    public int compareTo(ExecutionResult otherResult) {
        int retVal = otherResult.getHealthCheckResult().getStatus().compareTo(this.getHealthCheckResult().getStatus());
        if (retVal == 0) {
            retVal = Collator.getInstance().compare(this.getHealthCheckMetadata().getTitle(),
                    otherResult.getHealthCheckMetadata().getTitle());
        }
        return retVal;
    }

    long getServiceId() {
        return this.metaData.getServiceId();
    }
}