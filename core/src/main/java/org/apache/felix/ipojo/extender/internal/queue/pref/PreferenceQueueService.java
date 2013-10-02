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

package org.apache.felix.ipojo.extender.internal.queue.pref;

import org.apache.felix.ipojo.extender.internal.LifecycleQueueService;
import org.apache.felix.ipojo.extender.queue.Callback;
import org.apache.felix.ipojo.extender.queue.Job;
import org.apache.felix.ipojo.extender.queue.JobInfo;
import org.apache.felix.ipojo.extender.queue.QueueListener;
import org.apache.felix.ipojo.extender.queue.QueueService;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleReference;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * An implementation of the queue service delegating on the synchronous and asynchronous implementations according to
 * the processing preference.
 */
public class PreferenceQueueService implements LifecycleQueueService {

    /**
     * The preference selection strategy.
     */
    private final PreferenceSelection m_strategy;

    /**
     * The synchronous queue service.
     */
    private final LifecycleQueueService m_syncQueue;

    /**
     * The asynchronous queue service.
     */
    private final LifecycleQueueService m_asyncQueue;

    /**
     * The default queue service (chosen using the global preference).
     */
    private QueueService m_defaultQueue;

    /**
     * Creates the preference queue service.
     *
     * @param strategy   the preference strategy
     * @param syncQueue  the synchronous queue service
     * @param asyncQueue the asynchronous queue service
     */
    public PreferenceQueueService(PreferenceSelection strategy, LifecycleQueueService syncQueue, LifecycleQueueService asyncQueue) {
        m_strategy = strategy;
        m_syncQueue = syncQueue;
        m_asyncQueue = asyncQueue;

        // By default, system queue is asynchronous
        m_defaultQueue = asyncQueue;
    }

    /**
     * Starting queues.
     */
    public void start() {
        m_syncQueue.start();
        m_asyncQueue.start();
    }

    /**
     * Stopping queues.
     */
    public void stop() {
        m_syncQueue.stop();
        m_asyncQueue.stop();
    }

    /**
     * @return the number of completed jobs.
     */
    public int getFinished() {
        return m_syncQueue.getFinished() + m_asyncQueue.getFinished();
    }

    /**
     * @return the number of waiting jobs.
     */
    public int getWaiters() {
        return m_syncQueue.getWaiters() + m_asyncQueue.getWaiters();
    }

    /**
     * @return the number of jobs being processed.
     */
    public int getCurrents() {
        return m_syncQueue.getCurrents() + m_asyncQueue.getCurrents();
    }

    /**
     * Gets the number of waiting job. Notice that the synchronous queue does not have a waiting queue.
     *
     * @return the number of waiting job.
     */
    public List<JobInfo> getWaitersInfo() {
        // synchronous queue as no waiters, so snapshot is always empty and can be ignored
        return m_asyncQueue.getWaitersInfo();
    }

    /**
     * Submits a job to the right queue.
     * The queue selection works as follow:
     * If the bundle submitting the queue has a preference, use this preference, otherwise use the default preference.
     *
     * @param callable    the job
     * @param callback    callback called when the job is processed
     * @param description a description of the job
     * @return the reference of the submitted job
     */
    public <T> Future<T> submit(Job<T> callable, Callback<T> callback, String description) {

        Bundle bundle = callable.getBundle();
        Preference preference = m_strategy.select(bundle);

        QueueService selected = m_defaultQueue;
        switch (preference) {
            case ASYNC:
                selected = m_asyncQueue;
                break;
            case SYNC:
                selected = m_syncQueue;
                break;
        }

        return selected.submit(callable, callback, description);
    }

    public <T> Future<T> submit(Job<T> callable, String description) {
        return submit(callable, null, description);
    }

    public <T> Future<T> submit(Job<T> callable) {
        return submit(callable, "No description");
    }

    public void addQueueListener(final QueueListener listener) {
        // Intentionally blank, not intended to have listeners
    }

    public void removeQueueListener(final QueueListener listener) {
        // Intentionally blank, not intended to have listeners
    }
}