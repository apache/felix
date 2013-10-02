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

import org.apache.felix.ipojo.extender.internal.LifecycleQueueService;
import org.apache.felix.ipojo.extender.queue.Callback;
import org.apache.felix.ipojo.extender.queue.Job;
import org.apache.felix.ipojo.extender.queue.JobInfo;
import org.apache.felix.ipojo.extender.queue.QueueService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import java.util.*;
import java.util.concurrent.*;

/**
 * An asynchronous implementation of the queue service. This implementation relies on an executor service.
 */
public class ExecutorQueueService extends AbstractQueueService implements LifecycleQueueService, ManagedService {

    /**
     * Property name used to configure this ThreadPool's size (usable as System Property or ConfigAdmin property).
     */
    public static final String THREADPOOL_SIZE_PROPERTY = "org.apache.felix.ipojo.extender.ThreadPoolSize";

    /**
     * Service PID used to identify service with ConfigAdmin.
     */
    public static final String EXECUTOR_QUEUE_SERVICE_PID = "org.apache.felix.ipojo.extender.ExecutorQueueService";

    /**
     * The default thread pool size (3).
     */
    private final static int DEFAULT_QUEUE_SIZE = 3;

    /**
     * The executor service.
     */
    private final ThreadPoolExecutor m_executorService;

    /**
     * The statistics populated by this queue service.
     */
    private final Statistic m_statistic = new Statistic();

    /**
     * Store service properties (used when updating their values)
     */
    private Hashtable<String, Object> m_properties;

    /**
     * Initial thread pool size.
     */
    private final int initialSize;

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
        this(bundleContext, (ThreadPoolExecutor) Executors.newFixedThreadPool(size));
    }

    /**
     * Creates the queue service.
     *
     * @param bundleContext the bundle context.
     * @param size          the thread pool size
     * @param threadFactory the thread factory
     */
    public ExecutorQueueService(BundleContext bundleContext, int size, ThreadFactory threadFactory) {
        this(bundleContext, (ThreadPoolExecutor) Executors.newFixedThreadPool(size, threadFactory));
    }


    /**
     * Creates the queue service.
     * All others constructors delegates to this one.
     *
     * @param bundleContext   the bundle context
     * @param executorService the executor service we have to use
     */
    private ExecutorQueueService(BundleContext bundleContext, ThreadPoolExecutor executorService) {
        super(bundleContext, QueueService.class);
        m_executorService = executorService;
        initialSize = executorService.getCorePoolSize();
        m_properties = getDefaultProperties();
    }

    @Override
    protected ServiceRegistration<?> registerService() {
        // Register the instance under QueueService and ManagedService types
        return getBundleContext().registerService(new String[] {QueueService.class.getName(), ManagedService.class.getName()},
                                                  this,
                                                  getServiceProperties());
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
        return m_properties;
    }

    private Hashtable<String, Object> getDefaultProperties() {
        Hashtable<String, Object> initial = new Hashtable<String, Object>();
        initial.put(Constants.SERVICE_PID, EXECUTOR_QUEUE_SERVICE_PID);
        initial.put(QueueService.QUEUE_MODE_PROPERTY, QueueService.ASYNCHRONOUS_QUEUE_MODE);
        initial.put(THREADPOOL_SIZE_PROPERTY, initialSize);
        return initial;
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
    public <T> Future<T> submit(Job<T> callable, Callback<T> callback, String description) {
        JobInfoCallable<T> task = new JobInfoCallable<T>(this, m_statistic, callable, callback, description);
        return m_executorService.submit(task);
    }

    public <T> Future<T> submit(Job<T> callable, String description) {
        return submit(callable, null, description);
    }

    public <T> Future<T> submit(Job<T> callable) {
        return submit(callable, "No description");
    }

    public void updated(Dictionary properties) throws ConfigurationException {

        // Default configuration
        if (properties == null) {
            properties = getDefaultProperties();
        }

        boolean changed = false;

        // Try to read configuration
        Object o = properties.get(THREADPOOL_SIZE_PROPERTY);
        if (o != null) {
            // Convert value
            Integer newSize = getIntegerProperty(o, DEFAULT_QUEUE_SIZE);

            if (newSize != m_executorService.getMaximumPoolSize()) {
                // Apply configuration change
                m_executorService.setCorePoolSize(newSize);
                m_executorService.setMaximumPoolSize(newSize);
                m_properties.put(THREADPOOL_SIZE_PROPERTY, newSize);
                changed = true;
            }
        }

        if (changed) {
            // Transfer unrecognized values in service properties as per spec. recommendation
            for (Object key : Collections.list(properties.keys())) {
                if (!THREADPOOL_SIZE_PROPERTY.equals(key)) {
                    m_properties.put(key.toString(), properties.get(key));
                }
            }

            // Update registration object
            getRegistration().setProperties(m_properties);
        }

    }

    private Integer getIntegerProperty(final Object value, final Integer defaultValue) throws ConfigurationException {
        Integer newSize = null;
        if (value instanceof Integer) {
            newSize = (Integer) value;
        } else {
            // Try to convert the value
            try {
                newSize = Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return newSize;
    }
}
