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
package org.apache.felix.hc.api.execution;

import java.util.Date;

import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.util.HealthCheckMetadata;
import org.osgi.annotation.versioning.ProviderType;

/** Interface for health check executions via the {@link HealthCheckExecutor}. */
@ProviderType
public interface HealthCheckExecutionResult {

    /** Get the result of the health check run. */
    Result getHealthCheckResult();

    /** Get the elapsed time in ms */
    long getElapsedTimeInMs();

    /** Get the date, the health check finished or if the execution timed out, the execution was aborted.
     * 
     * @return The finished date of the execution. */
    Date getFinishedAt();

    /** Returns true if the execution has timed out. In this case the result does not reflect the real result of the underlying check, but a
     * result indicating the timeout.
     * 
     * @return <code>true</code> if execution timed out. */
    boolean hasTimedOut();

    /** Get the meta data about the health check service */
    HealthCheckMetadata getHealthCheckMetadata();
}