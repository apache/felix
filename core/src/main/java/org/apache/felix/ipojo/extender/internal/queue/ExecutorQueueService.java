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

import org.apache.felix.ipojo.extender.internal.AbstractService;
import org.apache.felix.ipojo.extender.internal.LifecycleQueueService;
import org.apache.felix.ipojo.extender.queue.Callback;
import org.apache.felix.ipojo.extender.queue.JobInfo;
import org.apache.felix.ipojo.extender.queue.QueueService;
import org.osgi.framework.BundleContext;

import java.util.*;
import java.util.concurrent.*;

/**
 * An asynchronous implementation of the queue service. This implementation relies on an executor service.
 */
public class ExecutorQueueService extends AbstractService implements LifecycleQueueService {

    /**
     * The default thread pool size (3).
     */
    private final static int DEFAULT_QUEUE_SIZE = 3;

    /**
     * The executor service.
     */
    private final ExecutorService m_executorService;

    /**
     * The statistics populated by this queue service.
     */
    private final Statistic m_statistic = new Statistic();

    /**
     * Creates the queue service using the default pool size.
     *
     * @param bundleContext the bundle context.
     */
    public ExecutorQueueService(BundleContext bundleContext) {
        this(bundleContext, DEFAULT_QUEUE_SIZE);
    }

    /**
     * Creates the queue service.
     *
     * @param bundleContext the bundle context.
     * @param size          the thread pool size.
     */
    public ExecutorQueueService(BundleContext bundleContext, int size) {
        this(bundleContext, Executors.newFixedThreadPool(size));
    }

    /**
     * Creates the queue service.
     *
     * @param bundleContext the bundle context.
     * @param size          the thread pool size
     * @param threadFactory the thread factory
     */
    public ExecutorQueueService(BundleContext bundleContext, int size, ThreadFactory threadFactory) {
        this(bundleContext, Executors.newFixedThreadPool(size, threadFactory));
    }


    /**
     * Creates the queue service.
     * All others constructors delegates to this one.
     *
     * @param bundleContext   the bundle context
     * @param executorService the executor service we have to use
     */
    private ExecutorQueueService(BundleContext bundleContext, ExecutorService executorService) {
        super(bundleContext, QueueService.class);
        m_executorService = executorService;
    }

    /**
     * Stops the service.
     */
    public void stop() {
        m_executorService.shutdown();
        // Wait for potential executed tasks to finish their executions
        try {
            m_executorService.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // Ignored
        }
        super.stop();
    }

    @Override
    protected Dictionary<String, ?> getServiceProperties() {
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        properties.put(QueueService.QUEUE_MODE_PROPERTY, QueueService.ASYNCHRONOUS_QUEUE_MODE);
        return properties;
    }

    public int getFinished() {
        return m_statistic.getFinishedCounter().get();
    }

    public int getWaiters() {
        return m_statistic.getWaiters().size();
    }

    public int getCurrents() {
        return m_statistic.getCurrentsCounter().get();
    }

    public List<JobInfo> getWaitersInfo() {
        List<JobInfo> snapshot;
        synchronized (m_statistic.getWaiters()) {
            snapshot = new ArrayList<JobInfo>(m_statistic.getWaiters());
        }
        return Collections.unmodifiableList(snapshot);
    }

    /**
     * Submits a job to the queue. The submitted job is wrapped into a {@link JobInfoCallable} to collect the
     * statistics.
     *
     * @param callable    the job
     * @param callback    callback called when the job is processed
     * @param description a description of the job
     * @return the reference on the submitted job
     */
    public <T> Future<T> submit(Callable<T> callable, Callback<T> callback, String description) {
        return m_executorService.submit(new JobInfoCallable<T>(m_statistic, callable, callback, description));
    }

    public <T> Future<T> submit(Callable<T> callable, String description) {
        return submit(callable, null, description);
    }

    public <T> Future<T> submit(Callable<T> callable) {
        return submit(callable, "No description");
    }

}
