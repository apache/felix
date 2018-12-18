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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.Result.Status;
import org.apache.felix.hc.api.ResultLog;
import org.apache.felix.hc.api.ResultLog.Entry;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.util.HealthCheckMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Caches health check results. */
public class HealthCheckResultCache {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final List<Status> NOT_OK_STATUS_VALUES = Arrays.asList(Status.WARN, Status.CRITICAL, Status.HEALTH_CHECK_ERROR);

    /** The map holding the cached results. */
    private final Map<Long, HealthCheckExecutionResult> cache = new ConcurrentHashMap<Long, HealthCheckExecutionResult>();

    @SuppressWarnings("serial")
    private final Map<Result.Status, Map<Long, HealthCheckExecutionResult>> cacheOfNotOkResults = new ConcurrentHashMap<Result.Status, Map<Long, HealthCheckExecutionResult>>() {
        {
            for (Status status : NOT_OK_STATUS_VALUES) {
                put(status, new ConcurrentHashMap<Long, HealthCheckExecutionResult>());
            }
        }
    };

    /** Update the cache with the result */
    public void updateWith(HealthCheckExecutionResult result) {
        final ExecutionResult executionResult = (ExecutionResult) result;
        cache.put(executionResult.getServiceId(), result);

        Status status = executionResult.getHealthCheckResult().getStatus();
        if (status.ordinal() >= Result.Status.WARN.ordinal()) {
            logger.debug("Caching {} result for HC {}", status, executionResult.getServiceId());
            cacheOfNotOkResults.get(status).put(executionResult.getServiceId(), result);
        }
    }

    /** Get the valid cache results */
    public void useValidCacheResults(final List<HealthCheckMetadata> metadatas,
            final Collection<HealthCheckExecutionResult> results,
            final long resultCacheTtlInMs) {
        final Set<HealthCheckExecutionResult> cachedResults = new TreeSet<HealthCheckExecutionResult>();
        final Iterator<HealthCheckMetadata> checksIt = metadatas.iterator();
        while (checksIt.hasNext()) {
            final HealthCheckMetadata md = checksIt.next();
            final HealthCheckExecutionResult result = getValidCacheResult(md, resultCacheTtlInMs);
            if (result != null) {
                cachedResults.add(result);
                checksIt.remove();
            }
        }
        logger.debug("Adding {} results from cache", cachedResults.size());
        results.addAll(cachedResults);
    }

    /** Return the cached result if it's still valid. */
    public HealthCheckExecutionResult getValidCacheResult(final HealthCheckMetadata metadata,
            final long resultCacheTtlInMs) {
        return get(metadata, resultCacheTtlInMs);
    }

    private HealthCheckExecutionResult get(final HealthCheckMetadata metadata, final long globalResultCacheTtlInMs) {
        final Long key = metadata.getServiceId();
        final HealthCheckExecutionResult cachedResult = cache.get(key);
        if (cachedResult != null) {
            Date finishedAt = cachedResult.getFinishedAt();
            if (finishedAt == null) {
                // never cache without proper meta data -> remove it
                cache.remove(key);
                return null;
            }

            long effectiveTtl = getEffectiveTtl(metadata, globalResultCacheTtlInMs);
            long validUntilLong = finishedAt.getTime() + effectiveTtl;
            if (validUntilLong < 0) { // if Long.MAX_VALUE is configured, this can become negative
                validUntilLong = Long.MAX_VALUE;
            }
            Date validUntil = new Date(validUntilLong);
            Date now = new Date();
            if (validUntil.after(now)) {
                logger.debug("Cache hit: validUntil={} cachedResult={}", validUntil, cachedResult);
                return cachedResult;
            } else {
                logger.debug("Outdated result: validUntil={} cachedResult={}", validUntil, cachedResult);
                // not removing result for key as out-dated results are shown for timed out checks if available
            }
        }

        // null => no cache hit
        return null;
    }

    /** Obtains the effective TTL for a given Metadata descriptor.
     *
     * @param metadata Metadata descriptor of health check
     * @param globalTtl TTL from service configuration of health check executor (used as default)
     * @return effective TTL */
    private long getEffectiveTtl(HealthCheckMetadata metadata, long globalTtl) {
        final long ttl;
        Long hcTtl = metadata.getResultCacheTtlInMs();
        if (hcTtl != null && hcTtl > 0) {
            ttl = hcTtl;
        } else {
            ttl = globalTtl;
        }
        return ttl;
    }

    /** Creates a new execution result
     * 
     * @param origResult
     * @return */
    public HealthCheckExecutionResult createExecutionResultWithStickyResults(HealthCheckExecutionResult origResult) {
        HealthCheckExecutionResult result = origResult;

        HealthCheckMetadata healthCheckMetadata = origResult.getHealthCheckMetadata();
        Long warningsStickForMinutes = healthCheckMetadata.getWarningsStickForMinutes();
        if (warningsStickForMinutes != null) {
            logger.debug("Taking into account sticky results (up to {} min old) for health check ", warningsStickForMinutes,
                    healthCheckMetadata.getName());
            List<HealthCheckExecutionResult> nonOkResultsFromPast = new ArrayList<HealthCheckExecutionResult>();
            long cutOffTime = System.currentTimeMillis() - (warningsStickForMinutes * 60 * 1000);
            for (Status status : NOT_OK_STATUS_VALUES) {
                long hcServiceId = ((ExecutionResult) origResult).getServiceId();
                HealthCheckExecutionResult nonOkResultFromPast = cacheOfNotOkResults.get(status).get(hcServiceId);
                if (nonOkResultFromPast == null) {
                    logger.debug("no sticky result in cache for HC {}", hcServiceId);
                    continue;
                }
                if (nonOkResultFromPast == origResult) {
                    logger.debug("result already in cache: {} for HC {}, not adding sticky result", origResult, hcServiceId);
                    continue;
                }
                long pastHcTime = nonOkResultFromPast.getFinishedAt().getTime();
                logger.debug("Time of old {} result: {}", status, pastHcTime);
                logger.debug("Cut off time: {}", cutOffTime);
                if (pastHcTime > cutOffTime) {
                    logger.debug("Found sticky result: {}", nonOkResultFromPast);
                    nonOkResultsFromPast.add(nonOkResultFromPast);
                }
            }

            if (!nonOkResultsFromPast.isEmpty()) {
                ResultLog resultLog = new ResultLog();
                resultLog.add(new Entry(Result.Status.OK, "*** Current Result ***"));
                for (ResultLog.Entry entry : origResult.getHealthCheckResult()) {
                    resultLog.add(entry);
                }
                DateFormat df = new SimpleDateFormat("HH:mm:ss.SSS");
                for (HealthCheckExecutionResult nonOkResultFromPast : nonOkResultsFromPast) {
                    Status status = nonOkResultFromPast.getHealthCheckResult().getStatus();
                    resultLog.add(
                            new Entry(Result.Status.WARN,
                                    "*** Sticky Result " + status + " from " + df.format(nonOkResultFromPast.getFinishedAt()) + " ***"));
                    for (ResultLog.Entry entry : nonOkResultFromPast.getHealthCheckResult()) {
                        resultLog.add(entry);
                    }
                }
                result = new ExecutionResult(healthCheckMetadata, new Result(resultLog), origResult.getElapsedTimeInMs());
            }
        }

        return result;
    }

    /** Clear the whole cache */
    public void clear() {
        this.cache.clear();
        for (Status status : NOT_OK_STATUS_VALUES) {
            cacheOfNotOkResults.get(status).clear();
        }
    }

    /** Remove entry from cache */
    public void removeCachedResult(final Long serviceId) {
        this.cache.remove(serviceId);
        for (Status status : NOT_OK_STATUS_VALUES) {
            cacheOfNotOkResults.get(status).remove(serviceId);
        }
    }

    @Override
    public String toString() {
        return "[HealthCheckResultCache size=" + cache.size() + "]";
    }

}
