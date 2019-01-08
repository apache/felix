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

import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.core.impl.executor.HealthCheckFuture;
import org.apache.felix.hc.core.impl.executor.HealthCheckFuture.Callback;
import org.apache.felix.hc.util.HealthCheckMetadata;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Abstract class for async execution variants cron/interval. */
public abstract class AsyncHealthCheckJob implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(AsyncHealthCheckJob.class);

    protected final HealthCheckMetadata healthCheckDescriptor;
    protected final AsyncHealthCheckExecutor asyncHealthCheckExecutor;
    protected final BundleContext bundleContext;

    public AsyncHealthCheckJob(HealthCheckMetadata healthCheckDescriptor, AsyncHealthCheckExecutor asyncHealthCheckExecutor,
            BundleContext bundleContext) {
        this.healthCheckDescriptor = healthCheckDescriptor;
        this.asyncHealthCheckExecutor = asyncHealthCheckExecutor;
        this.bundleContext = bundleContext;
    }

    @Override
    public void run() {

        LOG.debug("Running job {}", this);
        HealthCheckFuture healthCheckFuture = new HealthCheckFuture(healthCheckDescriptor, bundleContext, new Callback() {

            @Override
            public void finished(HealthCheckExecutionResult result) {
                asyncHealthCheckExecutor.updateWith(result);
            }
        });

        // run future in same thread (as we are already async via scheduler)
        healthCheckFuture.run();

    }

    public abstract boolean schedule();

    public abstract boolean unschedule();

    @Override
    public String toString() {
        return "[Async job for " + this.healthCheckDescriptor + "]";
    }
}