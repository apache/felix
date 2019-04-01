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

import static org.apache.felix.hc.api.FormattingResultLog.msHumanReadable;

import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog;
import org.apache.felix.hc.api.ResultLog.Entry;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Checks a result set for TEMPORARILY_UNAVAILABLE that have exceeded grace period. */
public class TempUnavailableGracePeriodEvaluator {

    private static final Logger LOG = LoggerFactory.getLogger(TempUnavailableGracePeriodEvaluator.class);

    /** Timestamp for last TEMPORARILY_UNAVAILABLE result */
    private final Map<Long, Date> firstTempUnavailableDateByServiceId = new ConcurrentHashMap<Long, Date>();

    private final long temporarilyAvailableGracePeriodInMs;

    public TempUnavailableGracePeriodEvaluator(long temporarilyAvailableGracePeriodInMs) {
        this.temporarilyAvailableGracePeriodInMs = temporarilyAvailableGracePeriodInMs;
        LOG.debug("Configured temporarilyAvailableGracePeriodInMs: {}", temporarilyAvailableGracePeriodInMs);
    }

    void updateTemporarilyUnavailableTimestampWith(HealthCheckExecutionResult result) {
        final ExecutionResult executionResult = (ExecutionResult) result;
        if (executionResult.getHealthCheckResult().getStatus() == Result.Status.TEMPORARILY_UNAVAILABLE) {
            if (!firstTempUnavailableDateByServiceId.containsKey(executionResult.getServiceId())) {
                firstTempUnavailableDateByServiceId.put(executionResult.getServiceId(), executionResult.getFinishedAt());
            } // else keep date of first occurrence of TEMPORARILY_UNAVAILABLE
        } else {
            firstTempUnavailableDateByServiceId.remove(executionResult.getServiceId());
        }
    }

    void evaluateGracePeriodForTemporarilyUnavailableResults(List<HealthCheckExecutionResult> results) {
        ListIterator<HealthCheckExecutionResult> resultsIt = results.listIterator();
        while (resultsIt.hasNext()) {
            ExecutionResult executionResult = (ExecutionResult) resultsIt.next();
            if (executionResult.getHealthCheckResult().getStatus() != Result.Status.TEMPORARILY_UNAVAILABLE) {
                continue; // not TEMPORARILY_UNAVAILABLE
            }

            Date firstTempUnavailableDate = firstTempUnavailableDateByServiceId.get(executionResult.getServiceId());
            
            if(firstTempUnavailableDate == null) {
                continue; // no previous TEMPORARILY_UNAVAILABLE found
            }
            
            long timestampForCritical = firstTempUnavailableDate.getTime() + temporarilyAvailableGracePeriodInMs;
            if (executionResult.getFinishedAt().getTime() < timestampForCritical) {
                continue; // grace period not exceeded
            }

            ResultLog resultLog = new ResultLog();
            for (ResultLog.Entry entry : executionResult.getHealthCheckResult()) {
                resultLog.add(entry);
            }

            resultLog.add(new Entry(Result.Status.CRITICAL, "Grace period for being temporarily unavailable exceeded "
                    + "by " + msHumanReadable(executionResult.getFinishedAt().getTime() - timestampForCritical)
                    + " (configured grace period: " + msHumanReadable(temporarilyAvailableGracePeriodInMs) + ")"));
            executionResult = new ExecutionResult(executionResult.getHealthCheckMetadata(), new Result(resultLog),
                    executionResult.getFinishedAt(), executionResult.getElapsedTimeInMs(), false);
            resultsIt.set(executionResult);

        }
    }
    

    @Override
    public String toString() {
        return "[TempUnavailableGracePeriodTester count checks currently TEMPORARILY_UNAVAILABLE: " + firstTempUnavailableDateByServiceId.size() + "]";
    }

}
