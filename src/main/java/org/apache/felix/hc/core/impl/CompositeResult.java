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
package org.apache.felix.hc.core.impl;

import static org.apache.felix.hc.util.FormattingResultLog.msHumanReadable;

import java.util.List;

import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog;
import org.apache.felix.hc.api.ResultLog.Entry;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.api.execution.HealthCheckMetadata;

public class CompositeResult extends Result {

    public CompositeResult(ResultLog log, List<HealthCheckExecutionResult> executionResults) {
        super(log);

        for (HealthCheckExecutionResult executionResult : executionResults) {
            HealthCheckMetadata healthCheckMetadata = executionResult.getHealthCheckMetadata();
            Result healthCheckResult = executionResult.getHealthCheckResult();
            for (Entry entry : healthCheckResult) {
                resultLog.add(new ResultLog.Entry(entry.getStatus(), healthCheckMetadata.getName() + ": " + entry.getMessage(),
                        entry.getException()));
            }
            resultLog.add(new ResultLog.Entry(healthCheckMetadata.getName() + " finished after "
                    + msHumanReadable(executionResult.getElapsedTimeInMs()) + (executionResult.hasTimedOut() ? " (timed out)" : ""), true));
        }
    }

}
