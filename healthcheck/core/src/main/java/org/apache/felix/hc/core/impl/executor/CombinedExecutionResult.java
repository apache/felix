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

import java.util.Date;
import java.util.List;

import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.Result.Status;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.api.execution.HealthCheckMetadata;

/** Used to group execution results. */
public class CombinedExecutionResult implements HealthCheckExecutionResult {

    final List<HealthCheckExecutionResult> executionResults;
    final Result overallResult;
    
    public CombinedExecutionResult(List<HealthCheckExecutionResult> executionResults) {
        this.executionResults = executionResults;
        Result.Status mostSevereStatus = Result.Status.OK;
        for (HealthCheckExecutionResult executionResult : executionResults) {
            Status status = executionResult.getHealthCheckResult().getStatus();
            if (status.ordinal() > mostSevereStatus.ordinal()) {
                mostSevereStatus = status;
            }
        }
        overallResult = new Result(mostSevereStatus, "Overall status " + mostSevereStatus);
    }

    @Override
    public Result getHealthCheckResult() {
        return overallResult;
    }

    @Override
    public long getElapsedTimeInMs() {
        long maxElapsed = 0;
        for(HealthCheckExecutionResult result: executionResults) {
            maxElapsed = Math.max(maxElapsed,  result.getElapsedTimeInMs());
        }
        return maxElapsed;
    }

    @Override
    public Date getFinishedAt() {
        Date latestFinishedAt = null;
        for(HealthCheckExecutionResult result: executionResults) {
            if(latestFinishedAt == null || latestFinishedAt.before(result.getFinishedAt())) {
                latestFinishedAt = result.getFinishedAt();
            }
        }
        return latestFinishedAt;
    }

    @Override
    public boolean hasTimedOut() {
        for(HealthCheckExecutionResult result: executionResults) {
            if(result.hasTimedOut()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public HealthCheckMetadata getHealthCheckMetadata() {
        throw new UnsupportedOperationException();
    }

}