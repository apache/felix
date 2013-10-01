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

package org.apache.felix.ipojo.extender.queue;

/**
 * A {@link org.apache.felix.ipojo.extender.queue.QueueListener} provides queue management information to external entities:
 * <ul>
 *     <li>Job submission</li>
 *     <li>Job execution</li>
 *     <li>Job result (success or failure)</li>
 * </ul>
 *
 * Implementer of this interface should not block as the invocation is done synchronously.
 * Implementers are responsible to register themselves in the {@link org.apache.felix.ipojo.extender.queue.QueueService} they'll observe.
 */
public interface QueueListener {

    /**
     * Invoked when a job is just being enlisted (before processing).
     * Only {@link JobInfo#getEnlistmentTime()} and {@link JobInfo#getWaitDuration()} provides meaningful values.
     * Note that {@code waitDuration} value is re-evaluated at each call.
     * @param info The job being enlisted
     */
    void enlisted(JobInfo info);

    /**
     * Invoked when a job's execution is just about to be started.
     * Only {@link JobInfo#getEnlistmentTime()}, {@link JobInfo#getWaitDuration()}, {@link JobInfo#getStartTime()}
     * and {@link JobInfo#getExecutionDuration()} provides meaningful values.
     * Note that {@code executionDuration} value is re-evaluated at each call.
     * @param info The job being started
     */
    void started(JobInfo info);

    /**
     * Invoked when a job's execution is finished successfully.
     * Note the implementers should not retain any references to the provided {@code result} (memory leak).
     * @param info The executed job
     * @param result The job's result
     */
    void executed(JobInfo info, Object result);

    /**
     * Invoked when a job's execution is finished with error.
     * Note the implementers should not retain any references to the provided {@code throwable} (memory leak).
     * @param info The failed job
     * @param throwable The job's thrown exception
     */
    void failed(JobInfo info, Throwable throwable);
}
