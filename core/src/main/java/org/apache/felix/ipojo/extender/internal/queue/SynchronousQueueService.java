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
import org.apache.felix.ipojo.extender.queue.Job;
import org.apache.felix.ipojo.extender.queue.JobInfo;
import org.apache.felix.ipojo.extender.queue.QueueService;
import org.osgi.framework.BundleContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.*;

/**
 * An implementation of the Lifecycle Queue Service for synchronous processing.
 */
public class SynchronousQueueService extends AbstractQueueService implements LifecycleQueueService {

    private final Statistic m_statistic = new Statistic();

    public SynchronousQueueService(BundleContext bundleContext) {
        super(bundleContext, QueueService.class);
    }

    @Override
    protected Dictionary<String, ?> getServiceProperties() {
        Hashtable<String, Object> properties = new Hashtable<String, Object>();
        properties.put(QueueService.QUEUE_MODE_PROPERTY, QueueService.SYNCHRONOUS_QUEUE_MODE);
        return properties;
    }

    public int getFinished() {
        return m_statistic.getFinishedCounter().get();
    }

    public int getWaiters() {
        return 0;
    }

    public int getCurrents() {
        return m_statistic.getCurrentsCounter().get();
    }

    public List<JobInfo> getWaitersInfo() {
        return Collections.emptyList();
    }

    public <T> Future<T> submit(Job<T> callable, Callback<T> callback, String description) {
        JobInfoCallable<T> exec = new JobInfoCallable<T>(this, m_statistic, callable, callback, description);
        try {
            return new ImmediateFuture<T>(exec.call());
        } catch (Exception e) {
            return new ExceptionFuture<T>(e);
        }

    }

    public <T> Future<T> submit(Job<T> callable, String description) {
        return submit(callable, null, description);
    }

    public <T> Future<T> submit(Job<T> callable) {
        return submit(callable, "No description");
    }

    private class ImmediateFuture<T> implements Future<T> {
        private T m_result;

        public ImmediateFuture(T result) {
            m_result = result;
        }

        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        public boolean isCancelled() {
            return false;
        }

        public boolean isDone() {
            return true;
        }

        public T get() throws InterruptedException, ExecutionException {
            return m_result;
        }

        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return get();
        }
    }

    private class ExceptionFuture<T> extends ImmediateFuture<T> {
        private ExecutionException m_exception;

        public ExceptionFuture(Exception e) {
            super(null);
            m_exception = new ExecutionException(e);
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            throw m_exception;
        }

    }
}
