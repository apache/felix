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

import static org.apache.felix.hc.api.FormattingResultLog.msHumanReadable;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.felix.hc.api.FormattingResultLog;
import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.api.execution.HealthCheckMetadata;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Future to be able to schedule a health check for parallel execution. */
public class HealthCheckFuture extends FutureTask<ExecutionResult> {

    public interface Callback {
        public void finished(final HealthCheckExecutionResult result);
    }

    private final static Logger LOG = LoggerFactory.getLogger(HealthCheckFuture.class);

    private final HealthCheckMetadata metadata;
    private final Date createdTime;

    public HealthCheckFuture(final HealthCheckMetadata metadata, final BundleContext bundleContext, final Callback callback) {
        super(new Callable<ExecutionResult>() {
            @Override
            public ExecutionResult call() throws Exception {
                Thread.currentThread().setName("HealthCheck " + metadata.getTitle());
                LOG.debug("Starting check {}", metadata);

                final StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                Result resultFromHealthCheck = null;
                ExecutionResult executionResult = null;

                Object healthCheck = bundleContext.getService(metadata.getServiceReference());
                try {
                    if (healthCheck != null) {
                        if ((healthCheck instanceof HealthCheck)) {
                            resultFromHealthCheck = ((HealthCheck) healthCheck).execute();
                        } else {
                            resultFromHealthCheck = executeLegacyHc(healthCheck);
                        }
                    } else {
                        throw new IllegalStateException("Service for " + metadata + " is gone");
                    }

                } catch (final Exception e) {
                    resultFromHealthCheck = new Result(Result.Status.CRITICAL,
                            "Exception during execution of '" + metadata.getName() + "': " + e, e);
                } finally {
                    // unget service ref
                    bundleContext.ungetService(metadata.getServiceReference());

                    // update result with information about this run
                    stopWatch.stop();
                    long elapsedTime = stopWatch.getTime();
                    if (resultFromHealthCheck != null) {
                        // wrap the result in an execution result
                        executionResult = new ExecutionResult(metadata, resultFromHealthCheck, elapsedTime);
                    }
                    LOG.debug("Time consumed for {}: {}", metadata, msHumanReadable(elapsedTime));
                }

                callback.finished(executionResult);
                Thread.currentThread().setName("HealthCheck-idle");
                return executionResult;
            }

        });
        this.createdTime = new Date();
        this.metadata = metadata;

    }

    Date getCreatedTime() {
        return this.createdTime;
    }

    public HealthCheckMetadata getHealthCheckMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        return "[Future for " + this.metadata + ", createdTime=" + this.createdTime + "]";
    }

    @SuppressWarnings("rawtypes")
    private static Result executeLegacyHc(Object healthCheck) {

        FormattingResultLog log = new FormattingResultLog();
        log.debug("Running legacy HC {}, please convert to new interface org.apache.felix.hc.api.HealthCheck!",
                healthCheck.getClass().getName());
        try {
            Object result = MethodUtils.invokeMethod(healthCheck, "execute");
            Object resultLog = FieldUtils.readField(result, "resultLog", true);

            List entries = (List) FieldUtils.readField(resultLog, "entries", true);
            for (Object object : entries) {
                String statusLegacy = String.valueOf(FieldUtils.readField(object, "status", true));
                String message = (String) FieldUtils.readField(object, "message", true);
                Exception exception = (Exception) FieldUtils.readField(object, "exception", true);
                if(statusLegacy.equals("DEBUG")) {
                    log.add(new ResultLog.Entry(message, true, exception));
                } else {
                    statusLegacy = statusLegacy.replace("INFO", "OK");
                    log.add(new ResultLog.Entry(Result.Status.valueOf(statusLegacy), message, exception));
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.healthCheckError("Could call and convert Sling HC {} for Felix Runtime", healthCheck.getClass().getName());
        }
        return new Result(log);
    }
}
