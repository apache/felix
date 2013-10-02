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
 * Interface to retrieve information about the job execution.
 */
public interface JobInfo {

    /**
     * Gets the submission time of the job.
     *
     * @return the submission time
     */
    long getEnlistmentTime();

    /**
     * Gets the starting time. This is the date when the job execution starts.
     *
     * @return the start time
     */
    long getStartTime();

    /**
     * Gets the completion time. This is the date when the job execution ends.
     *
     * @return the end time
     */
    long getEndTime();

    /**
     * Gets the time spent in the waiting queue.
     *
     * @return the waited time
     */
    long getWaitDuration();

    /**
     * Gets the execution duration.
     *
     * @return the execution duration, {@literal -1} if this duration cannot be computed
     */
    long getExecutionDuration();

    /**
     * Gets the job description
     *
     * @return the description
     */
    String getDescription();

    /**
     * Gets the job's type identifier. May be {@code null} if not provided.
     * @return job type identifier
     */
    String getJobType();
}
