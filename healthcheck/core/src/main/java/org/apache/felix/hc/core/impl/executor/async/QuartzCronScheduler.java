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
package org.apache.felix.hc.core.impl.executor.async;

import org.apache.felix.hc.core.impl.executor.HealthCheckExecutorThreadPool;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.simpl.RAMJobStore;
import org.quartz.spi.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuartzCronScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(QuartzCronScheduler.class);

    private static final String HC_SCHEDULER_NAME = "quartz.hc.scheduler_name";

    private Scheduler scheduler;

    public QuartzCronScheduler(HealthCheckExecutorThreadPool healthCheckExecutorThreadPool) {
        try {
            DirectSchedulerFactory schedulerFactory = DirectSchedulerFactory.getInstance();
            ThreadPool threadPool = new QuartzThreadPool(healthCheckExecutorThreadPool);
            schedulerFactory.createScheduler(HC_SCHEDULER_NAME, "id_" + System.currentTimeMillis(), threadPool, new RAMJobStore());
            scheduler = schedulerFactory.getScheduler(HC_SCHEDULER_NAME);
            scheduler.start();
            LOG.debug("Started quartz scheduler {}", scheduler);
        } catch (SchedulerException e) {
            throw new IllegalStateException("Could not initialise/start quartz scheduler " + HC_SCHEDULER_NAME, e);
        }
    }

    public void shutdown() {
        if (scheduler != null) {
            try {
                scheduler.shutdown(false);
                LOG.debug("Shutdown of quartz scheduler finished: {}", scheduler);
            } catch (SchedulerException e) {
                throw new IllegalStateException("Could not shutdown quartz scheduler " + HC_SCHEDULER_NAME, e);
            }
        }
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public class QuartzThreadPool implements ThreadPool {

        private final HealthCheckExecutorThreadPool healthCheckExecutorThreadPool;

        public QuartzThreadPool(HealthCheckExecutorThreadPool healthCheckExecutorThreadPool) {
            this.healthCheckExecutorThreadPool = healthCheckExecutorThreadPool;
        }

        /** @see org.quartz.spi.QuartzThreadPool#getPoolSize() */
        @Override
        public int getPoolSize() {
            return healthCheckExecutorThreadPool.getPoolSize();
        }

        /** @see org.quartz.spi.QuartzThreadPool#initialize() */
        @Override
        public void initialize() {
            // nothing to do
        }

        /** @see org.quartz.spi.ThreadPool#setInstanceId(java.lang.String) */
        @Override
        public void setInstanceId(final String id) {
            // we ignore this
        }

        /** @see org.quartz.spi.ThreadPool#setInstanceName(java.lang.String) */
        @Override
        public void setInstanceName(final String name) {
            // we ignore this
        }

        /** @see org.quartz.spi.QuartzThreadPool#runInThread(java.lang.Runnable) */
        @Override
        public boolean runInThread(final Runnable job) {
            healthCheckExecutorThreadPool.execute(job);
            return true;
        }

        /** @see org.quartz.spi.ThreadPool#blockForAvailableThreads() */
        @Override
        public int blockForAvailableThreads() {
            return healthCheckExecutorThreadPool.getMaxCurrentlyAvailableThreads();
        }

        /** @see org.quartz.spi.QuartzThreadPool#shutdown(boolean) */
        @Override
        public void shutdown(final boolean waitForJobsToComplete) {
            // this executor is bound to the SCR lifecycle of HealthCheckExecutorThreadPool
        }
    }

}
