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

import static org.apache.felix.hc.core.impl.executor.HealthCheckExecutorImplConfiguration.LONGRUNNING_FUTURE_THRESHOLD_CRITICAL_DEFAULT_MS;
import static org.apache.felix.hc.core.impl.executor.HealthCheckExecutorImplConfiguration.RESULT_CACHE_TTL_DEFAULT_MS;
import static org.apache.felix.hc.core.impl.executor.HealthCheckExecutorImplConfiguration.TIMEOUT_DEFAULT_MS;
import static org.apache.felix.hc.util.FormattingResultLog.msHumanReadable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog;
import org.apache.felix.hc.api.execution.HealthCheckExecutionOptions;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.api.execution.HealthCheckExecutor;
import org.apache.felix.hc.api.execution.HealthCheckSelector;
import org.apache.felix.hc.core.impl.executor.async.AsyncHealthCheckExecutor;
import org.apache.felix.hc.util.FormattingResultLog;
import org.apache.felix.hc.util.HealthCheckFilter;
import org.apache.felix.hc.util.HealthCheckMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Runs health checks for a given list of tags in parallel. */
@Component(service = { HealthCheckExecutor.class, ExtendedHealthCheckExecutor.class }, immediate = true // immediate = true to keep the
                                                                                                        // cache!
)
@Designate(ocd = HealthCheckExecutorImplConfiguration.class)
public class HealthCheckExecutorImpl implements ExtendedHealthCheckExecutor, ServiceListener {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private long timeoutInMs;

    private long longRunningFutureThresholdForRedMs;

    private long resultCacheTtlInMs;

    private String[] defaultTags;

    private HealthCheckResultCache healthCheckResultCache = new HealthCheckResultCache();

    private final Map<HealthCheckMetadata, HealthCheckFuture> stillRunningFutures = new HashMap<HealthCheckMetadata, HealthCheckFuture>();

    // optional dependency on quartz - if not present checks are just executed synchronously
    @Reference
    private AsyncHealthCheckExecutor asyncHealthCheckExecutor;

    @Reference
    HealthCheckExecutorThreadPool healthCheckExecutorThreadPool;

    private BundleContext bundleContext;

    @Activate
    protected final void activate(final HealthCheckExecutorImplConfiguration configuration, final BundleContext bundleContext) {
        this.bundleContext = bundleContext;

        configure(configuration);

        try {
            this.bundleContext.addServiceListener(this, "("
                    + Constants.OBJECTCLASS + "=" + HealthCheck.class.getName() + ")");
        } catch (final InvalidSyntaxException ise) {
            // this should really never happen as the expression above is constant
            throw new RuntimeException("Unexpected exception occured.", ise);
        }
    }

    @Modified
    protected final void modified(final HealthCheckExecutorImplConfiguration configuration) {
        configure(configuration);
    }

    @Deactivate
    protected final void deactivate() {
        this.bundleContext.removeServiceListener(this);
        this.bundleContext = null;
        this.healthCheckResultCache.clear();
    }

    protected final void configure(final HealthCheckExecutorImplConfiguration configuration) {
        this.timeoutInMs = configuration.timeoutInMs();
        if (this.timeoutInMs <= 0L) {
            this.timeoutInMs = TIMEOUT_DEFAULT_MS;
        }

        this.longRunningFutureThresholdForRedMs = configuration.longRunningFutureThresholdForCriticalMs();
        if (this.longRunningFutureThresholdForRedMs <= 0L) {
            this.longRunningFutureThresholdForRedMs = LONGRUNNING_FUTURE_THRESHOLD_CRITICAL_DEFAULT_MS;
        }

        this.resultCacheTtlInMs = configuration.resultCacheTtlInMs();
        if (this.resultCacheTtlInMs <= 0L) {
            this.resultCacheTtlInMs = RESULT_CACHE_TTL_DEFAULT_MS;
        }
        
        this.defaultTags = configuration.defaultTags();

    }

    @Override
    public void serviceChanged(final ServiceEvent event) {
        if (event.getType() == ServiceEvent.UNREGISTERING) {
            final Long serviceId = (Long) event.getServiceReference().getProperty(Constants.SERVICE_ID);
            this.healthCheckResultCache.removeCachedResult(serviceId);
        }
    }

    @Override
    public List<HealthCheckExecutionResult> execute(HealthCheckSelector selector) {
        return execute(selector, new HealthCheckExecutionOptions());
    }

    @Override
    public List<HealthCheckExecutionResult> execute(HealthCheckSelector selector, HealthCheckExecutionOptions options) {
        logger.debug("Starting executing checks for filter selector {} and execution options {}", selector, options);

        if(ArrayUtils.isEmpty(selector.tags())) {
            logger.debug("Using default tags");
            selector.withTags(defaultTags);
        }
        final HealthCheckFilter filter = new HealthCheckFilter(this.bundleContext);
        try {
            final ServiceReference<HealthCheck>[] healthCheckReferences = filter.getHealthCheckServiceReferences(selector, options.isCombineTagsWithOr());

            return this.execute(healthCheckReferences, options);
        } finally {
            filter.dispose();
        }
    }

    /** @see org.apache.felix.hc.core.impl.executor.ExtendedHealthCheckExecutor#execute(org.osgi.framework.ServiceReference) */
    @Override
    public HealthCheckExecutionResult execute(final ServiceReference<HealthCheck> ref) {
        final HealthCheckMetadata metadata = this.getHealthCheckMetadata(ref);
        return createResultsForDescriptor(metadata);
    }

    /** Execute a set of health checks */
    private List<HealthCheckExecutionResult> execute(final ServiceReference<HealthCheck>[] healthCheckReferences,
            HealthCheckExecutionOptions options) {
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final List<HealthCheckExecutionResult> results = new ArrayList<HealthCheckExecutionResult>();
        final List<HealthCheckMetadata> healthCheckDescriptors = getHealthCheckMetadata(healthCheckReferences);

        createResultsForDescriptors(healthCheckDescriptors, results, options);

        stopWatch.stop();
        if (logger.isDebugEnabled()) {
            logger.debug("Time consumed for all checks: {}", msHumanReadable(stopWatch.getTime()));
        }
        // sort result
        Collections.sort(results, new Comparator<HealthCheckExecutionResult>() {

            @Override
            public int compare(final HealthCheckExecutionResult arg0,
                    final HealthCheckExecutionResult arg1) {
                return ((ExecutionResult) arg0).compareTo((ExecutionResult) arg1);
            }

        });
        return results;
    }

    private void createResultsForDescriptors(final List<HealthCheckMetadata> healthCheckDescriptors,
            final List<HealthCheckExecutionResult> results, HealthCheckExecutionOptions options) {
        // -- All methods below check if they can transform a healthCheckDescriptor into a result
        // -- if yes the descriptor is removed from the list and the result added

        // get async results
        if (!options.isForceInstantExecution()) {
            if (asyncHealthCheckExecutor != null) {
                asyncHealthCheckExecutor.collectAsyncResults(healthCheckDescriptors, results, healthCheckResultCache);
            } else {
                for (HealthCheckMetadata hcDescriptior : healthCheckDescriptors) {
                    if (StringUtils.isNotBlank(hcDescriptior.getAsyncCronExpression())) {
                        logger.warn(
                                "Health check '{}' is configured for asynchronous execution, but async executor is not available (quartz bundle missing)",
                                hcDescriptior.getName());
                    }
                }
            }
        }

        // reuse cached results where possible
        if (!options.isForceInstantExecution()) {
            healthCheckResultCache.useValidCacheResults(healthCheckDescriptors, results, resultCacheTtlInMs);
        }

        // everything else is executed in parallel via futures
        List<HealthCheckFuture> futures = createOrReuseFutures(healthCheckDescriptors);

        // wait for futures at most until timeout (but will return earlier if all futures are finished)
        waitForFuturesRespectingTimeout(futures, options);
        collectResultsFromFutures(futures, results);

        // respect sticky results if configured via HealthCheck.WARNINGS_STICK_FOR_MINUTES
        appendStickyResultLogIfConfigured(results);

    }

    private void appendStickyResultLogIfConfigured(List<HealthCheckExecutionResult> results) {
        ListIterator<HealthCheckExecutionResult> resultsIt = results.listIterator();
        while (resultsIt.hasNext()) {
            HealthCheckExecutionResult result = resultsIt.next();
            Long warningsStickForMinutes = result.getHealthCheckMetadata().getWarningsStickForMinutes();
            if (warningsStickForMinutes != null && warningsStickForMinutes > 0) {
                result = healthCheckResultCache.createExecutionResultWithStickyResults(result);
                resultsIt.set(result);
            }
        }
    }

    private HealthCheckExecutionResult createResultsForDescriptor(final HealthCheckMetadata metadata) {
        // create result for a single descriptor

        // reuse cached results where possible
        HealthCheckExecutionResult result;

        result = healthCheckResultCache.getValidCacheResult(metadata, resultCacheTtlInMs);

        if (result == null) {
            final HealthCheckFuture future;
            synchronized (this.stillRunningFutures) {
                future = createOrReuseFuture(metadata);
            }

            // wait for futures at most until timeout (but will return earlier if all futures are finished)
            waitForFuturesRespectingTimeout(Collections.singletonList(future), null);
            result = collectResultFromFuture(future);
        }

        return result;
    }

    /** Create the health check meta data */
    private List<HealthCheckMetadata> getHealthCheckMetadata(final ServiceReference... healthCheckReferences) {
        final List<HealthCheckMetadata> descriptors = new LinkedList<HealthCheckMetadata>();
        for (final ServiceReference serviceReference : healthCheckReferences) {
            final HealthCheckMetadata descriptor = getHealthCheckMetadata(serviceReference);

            descriptors.add(descriptor);
        }

        return descriptors;
    }

    /** Create the health check meta data */
    private HealthCheckMetadata getHealthCheckMetadata(final ServiceReference healthCheckReference) {
        final HealthCheckMetadata descriptor = new HealthCheckMetadata(healthCheckReference);
        return descriptor;
    }

    /** Create or reuse future for the list of health checks */
    private List<HealthCheckFuture> createOrReuseFutures(final List<HealthCheckMetadata> healthCheckDescriptors) {
        final List<HealthCheckFuture> futuresForResultOfThisCall = new LinkedList<HealthCheckFuture>();

        synchronized (this.stillRunningFutures) {
            for (final HealthCheckMetadata md : healthCheckDescriptors) {

                futuresForResultOfThisCall.add(createOrReuseFuture(md));

            }
        }
        return futuresForResultOfThisCall;
    }

    /** Create or reuse future for the health check This method must be synchronized by the caller(!) on stillRunningFutures */
    private HealthCheckFuture createOrReuseFuture(final HealthCheckMetadata metadata) {
        HealthCheckFuture future = this.stillRunningFutures.get(metadata);
        if (future != null) {
            logger.debug("Found a future that is still running for {}", metadata);
        } else {
            logger.debug("Creating future for {}", metadata);
            future = new HealthCheckFuture(metadata, bundleContext, new HealthCheckFuture.Callback() {

                @Override
                public void finished(final HealthCheckExecutionResult result) {
                    healthCheckResultCache.updateWith(result);
                    asyncHealthCheckExecutor.updateWith(result);
                    synchronized (stillRunningFutures) {
                        stillRunningFutures.remove(metadata);
                    }
                }
            });
            this.stillRunningFutures.put(metadata, future);

            final HealthCheckFuture newFuture = future;

            healthCheckExecutorThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    newFuture.run();
                    synchronized (stillRunningFutures) {
                        // notify executor threads that newFuture is finished. Wrapping it in another runnable
                        // ensures that newFuture.isDone() will return true (if e.g. done in callback above, there are
                        // still a few lines of code until the future is really done and hence then the executor thread
                        // is sometime notified a bit too early, still receives the result isDone()=false and then waits
                        // for another 50ms, even though the future was about to be done one ms later)
                        stillRunningFutures.notifyAll();
                    }
                }
            });
        }

        return future;
    }

    /** Wait for the futures until the timeout is reached */
    private void waitForFuturesRespectingTimeout(final List<HealthCheckFuture> futuresForResultOfThisCall,
            HealthCheckExecutionOptions options) {
        final StopWatch callExcutionTimeStopWatch = new StopWatch();
        callExcutionTimeStopWatch.start();
        boolean allFuturesDone;

        long effectiveTimeout = this.timeoutInMs;
        if (options != null && options.getOverrideGlobalTimeout() > 0) {
            effectiveTimeout = options.getOverrideGlobalTimeout();
        }

        if (futuresForResultOfThisCall.isEmpty()) {
            return; // nothing to wait for (usually because of cached results)
        }

        do {
            try {
                synchronized (stillRunningFutures) {
                    stillRunningFutures.wait(50); // wait for notifications of callbacks of HealthCheckFutures
                }
            } catch (final InterruptedException ie) {
                logger.warn("Unexpected InterruptedException while waiting for healthCheckContributors", ie);
            }

            allFuturesDone = true;
            for (final HealthCheckFuture healthCheckFuture : futuresForResultOfThisCall) {
                allFuturesDone &= healthCheckFuture.isDone();
            }
        } while (!allFuturesDone && callExcutionTimeStopWatch.getTime() < effectiveTimeout);
    }

    /** Collect the results from all futures
     * 
     * @param futuresForResultOfThisCall The list of futures
     * @param results The result collection */
    void collectResultsFromFutures(final List<HealthCheckFuture> futuresForResultOfThisCall,
            final Collection<HealthCheckExecutionResult> results) {

        final Set<HealthCheckExecutionResult> resultsFromFutures = new HashSet<HealthCheckExecutionResult>();

        final Iterator<HealthCheckFuture> futuresIt = futuresForResultOfThisCall.iterator();
        while (futuresIt.hasNext()) {
            final HealthCheckFuture future = futuresIt.next();
            final HealthCheckExecutionResult result = this.collectResultFromFuture(future);

            resultsFromFutures.add(result);
            futuresIt.remove();
        }

        logger.debug("Adding {} results from futures", resultsFromFutures.size());
        results.addAll(resultsFromFutures);
    }

    /** Collect the result from a single future
     * 
     * @param future The future
     * @return The execution result or a result for a reached timeout */
    HealthCheckExecutionResult collectResultFromFuture(final HealthCheckFuture future) {

        HealthCheckExecutionResult result;
        HealthCheckMetadata hcMetadata = future.getHealthCheckMetadata();
        if (future.isDone()) {
            logger.debug("Health Check is done: {}", hcMetadata);

            try {
                result = future.get();
            } catch (final Exception e) {
                logger.warn("Unexpected Exception during future.get(): " + e, e);
                long futureElapsedTimeMs = new Date().getTime() - future.getCreatedTime().getTime();
                result = new ExecutionResult(hcMetadata, Result.Status.HEALTH_CHECK_ERROR,
                        "Unexpected Exception during future.get(): " + e, futureElapsedTimeMs, false);
            }

        } else {
            logger.debug("Health Check timed out: {}", hcMetadata);
            // Futures must not be cancelled as interrupting a health check might leave the system in invalid state
            // (worst case could be a corrupted repository index if using write operations)

            // normally we turn the check into WARN (normal timeout), but if the threshold time for CRITICAL is reached for a certain
            // future we turn the result CRITICAL
            long futureElapsedTimeMs = new Date().getTime() - future.getCreatedTime().getTime();
            FormattingResultLog resultLog = new FormattingResultLog();
            if (futureElapsedTimeMs < this.longRunningFutureThresholdForRedMs) {
                resultLog.warn("Timeout: Check still running after " + msHumanReadable(futureElapsedTimeMs));
            } else {
                resultLog.critical("Timeout: Check still running after " + msHumanReadable(futureElapsedTimeMs)
                        + " (exceeding the configured threshold for CRITICAL: "
                        + msHumanReadable(this.longRunningFutureThresholdForRedMs) + ")");
            }

            // add logs from previous, cached result if exists (using a 1 year TTL)
            HealthCheckExecutionResult lastCachedResult = healthCheckResultCache.getValidCacheResult(hcMetadata, 1000 * 60 * 60 * 24 * 365);
            if (lastCachedResult != null) {
                DateFormat df = new SimpleDateFormat("HH:mm:ss.SSS");
                resultLog.info("*** Result log of last execution finished at {} after {} ***",
                        df.format(lastCachedResult.getFinishedAt()),
                        FormattingResultLog.msHumanReadable(lastCachedResult.getElapsedTimeInMs()));
                for (ResultLog.Entry entry : lastCachedResult.getHealthCheckResult()) {
                    resultLog.add(entry);
                }
            }

            result = new ExecutionResult(hcMetadata, new Result(resultLog), futureElapsedTimeMs, true);

        }

        return result;
    }

    public void setTimeoutInMs(final long timeoutInMs) {
        this.timeoutInMs = timeoutInMs;
    }

    public void setLongRunningFutureThresholdForRedMs(
            final long longRunningFutureThresholdForRedMs) {
        this.longRunningFutureThresholdForRedMs = longRunningFutureThresholdForRedMs;
    }
}
