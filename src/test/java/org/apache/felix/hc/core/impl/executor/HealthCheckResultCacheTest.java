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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.api.execution.HealthCheckMetadata;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class HealthCheckResultCacheTest {

    private static final int HC_TIMEOUT_NOT_SET = -1;
    private static final int DUR_1_MIN = 60 * 1000;
    private static final int DUR_2_MIN = 2 * DUR_1_MIN;
    private static final int DUR_3_MIN = 3 * DUR_1_MIN;
    private static final int DUR_4_MIN = 4 * DUR_1_MIN;

    private HealthCheckResultCache healthCheckResultCache = new HealthCheckResultCache();

    @Mock
    ServiceReference serviceRef;

    @Before
    public void setup() {
        initMocks(this);
    }

    private HealthCheckMetadata setupHealthCheckMetadata(long id, long ttl) {
        reset(serviceRef);
        doReturn(id).when(serviceRef).getProperty(Constants.SERVICE_ID);
        doReturn(ttl).when(serviceRef).getProperty(HealthCheck.RESULT_CACHE_TTL_IN_MS);
        doReturn("HC id=" + id).when(serviceRef).getProperty(HealthCheck.NAME);
        return new HealthCheckMetadata(serviceRef);
    }

    @Test
    public void testHealthCheckResultCache() {

        HealthCheckMetadata hc1 = setupHealthCheckMetadata(1, HC_TIMEOUT_NOT_SET);
        ExecutionResult executionResult1 = spy(new ExecutionResult(hc1, new Result(Result.Status.OK, "result for hc1"), 1));
        doReturn(new Date(new Date().getTime() - DUR_1_MIN)).when(executionResult1).getFinishedAt();
        healthCheckResultCache.updateWith(executionResult1);

        HealthCheckMetadata hc2 = setupHealthCheckMetadata(2, HC_TIMEOUT_NOT_SET);
        ExecutionResult executionResult2 = spy(new ExecutionResult(hc2, new Result(Result.Status.OK, "result for hc2"), 1));
        doReturn(new Date(new Date().getTime() - DUR_3_MIN)).when(executionResult2).getFinishedAt();
        healthCheckResultCache.updateWith(executionResult2);

        HealthCheckMetadata hc3 = setupHealthCheckMetadata(3, DUR_4_MIN);
        ExecutionResult executionResult3 = spy(new ExecutionResult(hc3, new Result(Result.Status.OK, "result for hc3"), 1));
        doReturn(new Date(new Date().getTime() - DUR_3_MIN)).when(executionResult3).getFinishedAt();
        healthCheckResultCache.updateWith(executionResult3);

        HealthCheckMetadata hc4 = setupHealthCheckMetadata(4, HC_TIMEOUT_NOT_SET);
        // no result for this yet

        List<HealthCheckMetadata> hcList = new ArrayList<HealthCheckMetadata>(Arrays.asList(hc1, hc2, hc3, hc4));
        List<HealthCheckExecutionResult> results = new ArrayList<HealthCheckExecutionResult>();

        healthCheckResultCache.useValidCacheResults(hcList, results, DUR_2_MIN);

        assertTrue(hcList.contains(hc2)); // result too old, left in hcList for later execution
        assertTrue(hcList.contains(hc4)); // no result was added to cache via updateWith()

        assertTrue(results.contains(executionResult1)); // true <= result one min old, global timeout 2min
        assertFalse(results.contains(executionResult2)); // false <= result three min old, global timeout 2min
        assertTrue(results.contains(executionResult3)); // true <= result one three old, HC timeout 4min

        // values not found in cache are left in hcList
        assertEquals(2, hcList.size());
        assertEquals(2, results.size());

    }

    @Test
    public void testHealthCheckResultCacheTtl() {

        // -- test cache miss due to HC TTL
        HealthCheckMetadata hcWithTtl = setupHealthCheckMetadata(1, DUR_1_MIN);
        ExecutionResult executionResult = spy(new ExecutionResult(hcWithTtl, new Result(Result.Status.OK, "result for hc"), 1));
        doReturn(new Date(new Date().getTime() - DUR_2_MIN)).when(executionResult).getFinishedAt();
        healthCheckResultCache.updateWith(executionResult);

        HealthCheckExecutionResult result = healthCheckResultCache.getValidCacheResult(hcWithTtl, DUR_3_MIN);
        assertNull(result); // even though global timeout would be ok (2min<3min, the hc timeout of 1min invalidates the result)

        // -- test cache hit due to HC TTL
        hcWithTtl = setupHealthCheckMetadata(2, DUR_3_MIN);
        executionResult = spy(new ExecutionResult(hcWithTtl, new Result(Result.Status.OK, "result for hc"), 1));
        doReturn(new Date(new Date().getTime() - DUR_2_MIN)).when(executionResult).getFinishedAt();
        healthCheckResultCache.updateWith(executionResult);

        result = healthCheckResultCache.getValidCacheResult(hcWithTtl, DUR_1_MIN);
        assertEquals(executionResult, result); // even though global timeout would invalidate this result (1min<2min, the hc timeout of 3min
                                               // allows the result)

        // -- test Long.MAX_VALUE
        hcWithTtl = setupHealthCheckMetadata(3, Long.MAX_VALUE);
        executionResult = spy(new ExecutionResult(hcWithTtl, new Result(Result.Status.OK, "result for hc"), 1));
        doReturn(new Date(new Date().getTime() - DUR_4_MIN)).when(executionResult).getFinishedAt();
        healthCheckResultCache.updateWith(executionResult);

        result = healthCheckResultCache.getValidCacheResult(hcWithTtl, DUR_1_MIN);
        assertEquals(executionResult, result);

    }

    private HealthCheckMetadata setupHealthCheckMetadataWithStickyResults(long id, long nonOkStickyForSec) {
        reset(serviceRef);
        doReturn(id).when(serviceRef).getProperty(Constants.SERVICE_ID);
        doReturn(nonOkStickyForSec).when(serviceRef).getProperty(HealthCheck.KEEP_NON_OK_RESULTS_STICKY_FOR_SEC);
        doReturn("HC id=" + id).when(serviceRef).getProperty(HealthCheck.NAME);
        return new HealthCheckMetadata(serviceRef);
    }

    @Test
    public void testCreateExecutionResultWithStickyResults() {

        HealthCheckMetadata hcWithStickyResultsSet = setupHealthCheckMetadataWithStickyResults(1, 120 /* 2 minutes */);
        ExecutionResult currentResult = spy(new ExecutionResult(hcWithStickyResultsSet, new Result(Result.Status.OK, "result for hc"), 1));
        HealthCheckExecutionResult overallResultWithStickyResults = healthCheckResultCache
                .createExecutionResultWithStickyResults(currentResult);
        assertTrue("Exact same result is expected if no history exists", currentResult == overallResultWithStickyResults);

        // add 4 minutes old WARN to cache
        ExecutionResult oldWarnResult = spy(
                new ExecutionResult(hcWithStickyResultsSet, new Result(Result.Status.WARN, "result for hc"), 1));
        doReturn(new Date(System.currentTimeMillis() - DUR_4_MIN)).when(oldWarnResult).getFinishedAt();
        healthCheckResultCache.updateWith(oldWarnResult);

        // check that it is not used
        currentResult = new ExecutionResult(hcWithStickyResultsSet, new Result(Result.Status.OK, "result for hc"), 1);
        overallResultWithStickyResults = healthCheckResultCache.createExecutionResultWithStickyResults(currentResult);
        assertTrue("Exact same result is expected if WARN HC Result is too old", currentResult == overallResultWithStickyResults);

        // change WARN to 1 minute age
        doReturn(new Date(System.currentTimeMillis() - DUR_1_MIN)).when(oldWarnResult).getFinishedAt();
        overallResultWithStickyResults = healthCheckResultCache.createExecutionResultWithStickyResults(currentResult);
        assertTrue("Expect newly created result as sticky result should be taken into account",
                currentResult != overallResultWithStickyResults);
        assertEquals("Expect status to be taken over from old, sticky WARN", Result.Status.WARN,
                overallResultWithStickyResults.getHealthCheckResult().getStatus());
        assertEquals("Expect 4 entries, two each for current and WARN", 4, getLogMsgCount(overallResultWithStickyResults));

        // add 1 minutes old CRITICAL to cache
        ExecutionResult oldCriticalResult = spy(
                new ExecutionResult(hcWithStickyResultsSet, new Result(Result.Status.CRITICAL, "result for hc"), 1));
        doReturn(new Date(System.currentTimeMillis() - DUR_1_MIN)).when(oldCriticalResult).getFinishedAt();
        healthCheckResultCache.updateWith(oldCriticalResult);

        overallResultWithStickyResults = healthCheckResultCache.createExecutionResultWithStickyResults(currentResult);
        assertTrue("Expect newly created result as sticky result should be taken into account",
                currentResult != overallResultWithStickyResults);
        assertEquals("Expect status to be taken over from old, sticky CRITICAL", Result.Status.CRITICAL,
                overallResultWithStickyResults.getHealthCheckResult().getStatus());
        assertEquals("Expect six entries, two each for current, WARN and CRITICAL result", 6,
                getLogMsgCount(overallResultWithStickyResults));

    }

    private int getLogMsgCount(HealthCheckExecutionResult result) {
        int count = 0;
        for (ResultLog.Entry entry : result.getHealthCheckResult()) {
            count++;
        }
        return count;
    }

}
