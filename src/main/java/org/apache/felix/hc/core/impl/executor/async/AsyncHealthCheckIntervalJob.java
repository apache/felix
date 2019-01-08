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

import java.util.concurrent.ScheduledFuture;

import org.apache.felix.hc.core.impl.executor.HealthCheckExecutorThreadPool;
import org.apache.felix.hc.util.HealthCheckMetadata;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Runs health checks that are configured with an interval (ScheduledThreadPoolExecutor.scheduleAtFixedRate()) for asynchronous execution.  */
public class AsyncHealthCheckIntervalJob extends AsyncHealthCheckJob implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(AsyncHealthCheckExecutor.class);

    private final HealthCheckExecutorThreadPool healthCheckExecutorThreadPool;
    private ScheduledFuture<?> scheduleFuture = null;

    public AsyncHealthCheckIntervalJob(HealthCheckMetadata healthCheckDescriptor, AsyncHealthCheckExecutor asyncHealthCheckExecutor,
            BundleContext bundleContext, HealthCheckExecutorThreadPool healthCheckExecutorThreadPool) {
        super(healthCheckDescriptor, asyncHealthCheckExecutor, bundleContext);
        this.healthCheckExecutorThreadPool = healthCheckExecutorThreadPool;
    }

    public boolean schedule() {
        Long asyncIntervalInSec = healthCheckDescriptor.getAsyncIntervalInSec();
        scheduleFuture = healthCheckExecutorThreadPool.scheduleAtFixedRate(this, asyncIntervalInSec);
        LOG.info("Scheduled job {} for execution every {}sec", this, asyncIntervalInSec);
        return true;
    }

    @Override
    public boolean unschedule() {

        if (scheduleFuture != null) {
            LOG.debug("Unscheduling async job for {}", healthCheckDescriptor);
            return scheduleFuture.cancel(false);
        } else {
            LOG.debug("No scheduled future for {} exists", healthCheckDescriptor);
            return false;
        }
    }

}