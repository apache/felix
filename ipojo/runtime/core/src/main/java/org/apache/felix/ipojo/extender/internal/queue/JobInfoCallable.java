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

package org.apache.felix.ipojo.extender.internal.queue;

import org.apache.felix.ipojo.extender.queue.Callback;
import org.apache.felix.ipojo.extender.queue.Job;
import org.apache.felix.ipojo.extender.queue.JobInfo;

import java.util.concurrent.Callable;

/**
 * A callable computing job statistics. The job is given as another callable.
 * The statistics are global, so must be used carefully.
 */
public class JobInfoCallable<T> implements Callable<T>, JobInfo {

    /**
     * Notifier helper for {@link org.apache.felix.ipojo.extender.queue.QueueListener}.
     */
    private final QueueNotifier m_queueNotifier;

    /**
     * The statistic object.
     */
    private final Statistic m_statistic;

    /**
     * The genuine job.
     */
    private final Job<T> m_delegate;

    /**
     * A callback notified when the job is processed.
     */
    private final Callback<T> m_callback;

    /**
     * The job description.
     */
    private final String m_description;

    /**
     * The date (in milli) when this object is created.
     */
    private long enlistmentTime = System.currentTimeMillis();

    /**
     * The date when the job processing started.
     */
    private long startTime = -1;

    /**
     * The date when the job processing is completed.
     */
    private long endTime = -1;

    /**
     * Creates the job info callable.
     *
     * @param queueNotifier notifier for QueueListeners
     * @param statistic   the statistics that will be populated
     * @param delegate    the real job
     * @param callback    the callback notified when the job is completed
     * @param description the job description
     */
    public JobInfoCallable(QueueNotifier queueNotifier,
                           Statistic statistic,
                           Job<T> delegate,
                           Callback<T> callback,
                           String description) {
        m_queueNotifier = queueNotifier;
        m_statistic = statistic;
        m_delegate = delegate;
        m_callback = callback;
        m_description = description;
        m_statistic.getWaiters().add(this);

        // Assume that we will be enlisted in the next few cycles
        m_queueNotifier.fireEnlistedJobInfo(this);
    }

    /**
     * Executes the job.
     * This method updates the statistics.
     *
     * @return the job result
     * @throws Exception the job execution failed
     */
    public T call() throws Exception {
        m_statistic.getWaiters().remove(this);
        startTime = System.currentTimeMillis();
        m_statistic.getCurrentsCounter().incrementAndGet();
        T result = null;
        Exception exception = null;
        try {
            m_queueNotifier.fireStartedJobInfo(this);
            result = m_delegate.call();
            endTime = System.currentTimeMillis();
            return result;
        } catch (Exception e) {
            endTime = System.currentTimeMillis();
            m_queueNotifier.fireFailedJobInfo(this, e);
            if (m_callback != null) {
                m_callback.error(this, e);
            }
            exception = e;
            throw e;
        } finally {
            m_statistic.getCurrentsCounter().decrementAndGet();
            m_statistic.getFinishedCounter().incrementAndGet();

            // Only exec success callbacks when no error occurred
            if (exception == null) {
                m_queueNotifier.fireExecutedJobInfo(this, result);
                if (m_callback != null) {
                    m_callback.success(this, result);
                }
            }
        }
    }

    /**
     * @return the enlistment date.
     */
    public long getEnlistmentTime() {
        return enlistmentTime;
    }

    /**
     * @return the job start date.
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * @return the job completion date.
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * Computes the time spent in the waiting queue
     *
     * @return the waited time, if the job is still waiting, gets the current waited time
     */
    public long getWaitDuration() {
        long end = startTime;
        if (end == -1) {
            // Not yet started
            // Still waiting
            end = System.currentTimeMillis();
        }
        return end - enlistmentTime;
    }

    /**
     * Computes the time spent to execute the job (this does not include the waiting).
     * If the job is not executed yet, or is still executing, {@literal -1} is returned
     *
     * @return the execution duration, or {@literal -1}.
     */
    public long getExecutionDuration() {
        if ((startTime == -1) || (endTime == -1)) {
            return -1;
        }
        return endTime - startTime;
    }

    /**
     * @return the job description
     */
    public String getDescription() {
        return m_description;
    }

    public String getJobType() {
        return m_delegate.getJobType();
    }
}
