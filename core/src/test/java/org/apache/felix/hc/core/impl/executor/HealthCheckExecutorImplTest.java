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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.Result.Status;
import org.apache.felix.hc.api.ResultLog.Entry;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.util.HealthCheckMetadata;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

public class HealthCheckExecutorImplTest {

    @InjectMocks
    private HealthCheckExecutorImpl healthCheckExecutorImpl = new HealthCheckExecutorImpl();;

    @Mock
    private HealthCheckFuture future;

    @Mock
    private HealthCheckMetadata HealthCheckMetadata;

    @Spy
    private HealthCheckResultCache healthCheckResultCache = new HealthCheckResultCache();

    @Before
    public void setup() {
        initMocks(this);

        when(future.getHealthCheckMetadata()).thenReturn(HealthCheckMetadata);
        when(HealthCheckMetadata.getTitle()).thenReturn("Test Check");

        // 2 sec normal timeout
        healthCheckExecutorImpl.setTimeoutInMs(2000L);
        // 10 sec timeout for critical
        healthCheckExecutorImpl.setLongRunningFutureThresholdForRedMs(10000L);
    }

    @Test
    public void testCollectResultsFromFutures() throws Exception {

        List<HealthCheckFuture> futures = new LinkedList<HealthCheckFuture>();
        futures.add(future);
        Collection<HealthCheckExecutionResult> results = new TreeSet<HealthCheckExecutionResult>();

        when(future.isDone()).thenReturn(true);
        ExecutionResult testResult = new ExecutionResult(HealthCheckMetadata, new Result(Result.Status.OK, "test"), 10L);
        when(future.get()).thenReturn(testResult);

        healthCheckExecutorImpl.collectResultsFromFutures(futures, results);

        verify(future, times(1)).get();

        assertEquals(1, results.size());
        assertTrue(results.contains(testResult));
    }

    @Test
    public void testCollectResultsFromFuturesTimeout() throws Exception {

        // add an earlier result with status ok (that will be shown as part of the log)
        addResultToCache(Status.OK);

        List<HealthCheckFuture> futures = new LinkedList<HealthCheckFuture>();
        futures.add(future);
        Set<HealthCheckExecutionResult> results = new TreeSet<HealthCheckExecutionResult>();

        when(future.isDone()).thenReturn(false);
        // simulating a future that was created 5sec ago
        when(future.getCreatedTime()).thenReturn(new Date(new Date().getTime() - 1000 * 5));

        healthCheckExecutorImpl.collectResultsFromFutures(futures, results);

        verify(future, times(0)).get();

        assertEquals(1, results.size());
        HealthCheckExecutionResult result = results.iterator().next();

        assertEquals(Result.Status.WARN, result.getHealthCheckResult().getStatus());

        // 3 because previous result exists and is part of log
        assertEquals(3, getLogEntryCount(result));
    }

    @Test
    public void testCollectResultsFromFuturesCriticalTimeout() throws Exception {

        List<HealthCheckFuture> futures = new LinkedList<HealthCheckFuture>();
        futures.add(future);
        Set<HealthCheckExecutionResult> results = new TreeSet<HealthCheckExecutionResult>();

        when(future.isDone()).thenReturn(false);

        // use an old date now (simulating a future that has run for an hour)
        when(future.getCreatedTime()).thenReturn(new Date(new Date().getTime() - 1000 * 60 * 60));

        healthCheckExecutorImpl.collectResultsFromFutures(futures, results);
        assertEquals(1, results.size());
        HealthCheckExecutionResult result = results.iterator().next();

        verify(future, times(0)).get();

        assertEquals(Result.Status.CRITICAL, result.getHealthCheckResult().getStatus());
        assertEquals(1, getLogEntryCount(result));
    }

    @Test
    public void testCollectResultsFromFuturesWarnTimeoutWithPreviousCritical() throws Exception {

        // an earlier result with critical
        addResultToCache(Status.CRITICAL);

        List<HealthCheckFuture> futures = new LinkedList<HealthCheckFuture>();
        futures.add(future);
        Set<HealthCheckExecutionResult> results = new TreeSet<HealthCheckExecutionResult>();

        when(future.isDone()).thenReturn(false);
        // simulating a future that was created 5sec ago
        when(future.getCreatedTime()).thenReturn(new Date(new Date().getTime() - 1000 * 5));

        healthCheckExecutorImpl.collectResultsFromFutures(futures, results);
        assertEquals(1, results.size());
        HealthCheckExecutionResult result = results.iterator().next();

        verify(future, times(0)).get();

        // expect CRITICAL because previous result (before timeout) was CRITICAL (and not only WARN)
        assertEquals(Result.Status.CRITICAL, result.getHealthCheckResult().getStatus());
        assertEquals(3, getLogEntryCount(result));
    }

    private int getLogEntryCount(HealthCheckExecutionResult result) {
        int logEntryCount = 0;
        final Iterator<Entry> it = result.getHealthCheckResult().iterator();
        while (it.hasNext()) {
            it.next();
            logEntryCount++;
        }
        return logEntryCount;
    }

    private void addResultToCache(Status status) {
        healthCheckResultCache.updateWith(new ExecutionResult(HealthCheckMetadata, new Result(status, "Status " + status), 1000));
    }
}
