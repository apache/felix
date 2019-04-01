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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;

import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.api.execution.HealthCheckMetadata;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class TempUnavailableGracePeriodEvaluatorTest {

    private TempUnavailableGracePeriodEvaluator tempUnavailableGracePeriodEvaluator;
    
    @Mock
    private ServiceReference<?> hcServiceRef;

    @Before
    public void setup() {
        initMocks(this);
    }
    
    @Test
    public void testGracePeriodIsNotExceeded() {
        
        tempUnavailableGracePeriodEvaluator = new TempUnavailableGracePeriodEvaluator(60*1000 /*1 min */);
        
        HealthCheckExecutionResult result = createResult(1, Result.Status.TEMPORARILY_UNAVAILABLE);
        
        List<HealthCheckExecutionResult> results = asList(result, createResult(2, Result.Status.OK), createResult(3, Result.Status.WARN));
        
        tempUnavailableGracePeriodEvaluator.evaluateGracePeriodForTemporarilyUnavailableResults(results);
        
        assertEquals(Result.Status.TEMPORARILY_UNAVAILABLE, results.get(0).getHealthCheckResult().getStatus());
        assertEquals(Result.Status.OK, results.get(1).getHealthCheckResult().getStatus());
        assertEquals(Result.Status.WARN, results.get(2).getHealthCheckResult().getStatus());
    }



    @Test
    public void testGracePeriodIsExceeded() throws InterruptedException {
        
        tempUnavailableGracePeriodEvaluator = new TempUnavailableGracePeriodEvaluator(20 /* 20ms only */);
        
        HealthCheckExecutionResult result = createResult(1, Result.Status.TEMPORARILY_UNAVAILABLE);
        
        Thread.sleep(50 /* 50ms */);
        
        result = createResult(1, Result.Status.TEMPORARILY_UNAVAILABLE);
        
        List<HealthCheckExecutionResult> results = asList(result, createResult(2, Result.Status.OK), createResult(3, Result.Status.WARN));
        
        tempUnavailableGracePeriodEvaluator.evaluateGracePeriodForTemporarilyUnavailableResults(results);
        
        // overall result has to be CRITICAL now
        assertEquals(Result.Status.CRITICAL, results.get(0).getHealthCheckResult().getStatus());
        assertEquals(Result.Status.OK, results.get(1).getHealthCheckResult().getStatus());
        assertEquals(Result.Status.WARN, results.get(2).getHealthCheckResult().getStatus());
        
        
        // one intermediate OK result followed by yet another TEMPORARILY_UNAVAILABLE has to send it back to TEMPORARILY_UNAVAILABLE 
        // (not CRITICAL during grace period of 20ms)
        result = createResult(1, Result.Status.OK);
        result = createResult(1, Result.Status.TEMPORARILY_UNAVAILABLE);
        results.set(0, result);
        
        tempUnavailableGracePeriodEvaluator.evaluateGracePeriodForTemporarilyUnavailableResults(results);
        // overall result has to be back to TEMPORARILY_UNAVAILABLE now
        assertEquals(Result.Status.TEMPORARILY_UNAVAILABLE, results.get(0).getHealthCheckResult().getStatus());
        assertEquals(Result.Status.OK, results.get(1).getHealthCheckResult().getStatus());
        assertEquals(Result.Status.WARN, results.get(2).getHealthCheckResult().getStatus());
    }

    private HealthCheckExecutionResult createResult(long serviceId, Result.Status status) {
        doReturn(serviceId).when(hcServiceRef).getProperty(Constants.SERVICE_ID);
        HealthCheckExecutionResult result = new ExecutionResult(new HealthCheckMetadata(hcServiceRef), new Result(status, "Result of status "+status), 1);
        tempUnavailableGracePeriodEvaluator.updateTemporarilyUnavailableTimestampWith(result);
        return result;
    }


}
