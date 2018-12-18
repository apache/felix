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

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Creates a thread pool via standard java.util.concurrent package to be used for parallel execution of health checks in
 * HealthCheckExecutorImpl and AsyncHealthCheckExecutor */
@Component(service = { HealthCheckExecutorThreadPool.class })
@Designate(ocd = HealthCheckExecutorThreadPoolConfiguration.class)
public class HealthCheckExecutorThreadPool {
    private final static Logger LOG = LoggerFactory.getLogger(HealthCheckExecutorThreadPool.class);

    private int threadPoolSize;

    private ScheduledThreadPoolExecutor executor;

    @Activate
    protected final void activate(final HealthCheckExecutorThreadPoolConfiguration configuration, final BundleContext bundleContext) {

        this.threadPoolSize = configuration.threadPoolSize();

        executor = new ScheduledThreadPoolExecutor(threadPoolSize, new HcThreadFactory(), new HcRejectedExecutionHandler());

        LOG.info("Created HC Thread Pool: threadPoolSize={}", threadPoolSize);

    }

    @Deactivate
    protected final void deactivate() {
        executor.shutdown();
    }

    // Method called by HealthCheckExecutorImpl (regular synchronous checks)
    public void execute(final Runnable job) {
        this.executor.execute(job);
    }

    // used for interval execution (asynchronous checks)
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable job, long intervalInSec) {
        ScheduledFuture<?> scheduleFuture = executor.scheduleAtFixedRate(job, 0, intervalInSec, TimeUnit.SECONDS);
        return scheduleFuture;
    }

    // methods below are used by AsyncHealthCheckExecutor.QuartzThreadPool
    public int getPoolSize() {
        return this.executor.getPoolSize();
    }

    public int getMaxCurrentlyAvailableThreads() {
        return this.threadPoolSize - executor.getQueue().size();
    }

    static class HcThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        HcThreadFactory() {
            group = Thread.currentThread().getThreadGroup();
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, "hc-thread-" + threadNumber.getAndIncrement());
            t.setDaemon(true); // using daemon thread to not delay JVM shutdown (HC status is non-transactional and only in memory)
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

    private final class HcRejectedExecutionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            LOG.warn("Thread Pool {} rejected to run runnable {}", executor, r);
        }
    }

}
