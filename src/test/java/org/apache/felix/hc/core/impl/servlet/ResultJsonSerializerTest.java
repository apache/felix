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
package org.apache.felix.hc.core.impl.servlet;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Arrays;

import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.api.execution.HealthCheckMetadata;
import org.apache.felix.hc.core.impl.executor.ExecutionResult;
import org.apache.felix.hc.util.FormattingResultLog;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class ResultJsonSerializerTest {

    @Mock
    private ServiceReference<HealthCheck> serviceReference;

    ResultJsonSerializer resultJsonSerializer = new ResultJsonSerializer();

    @Before
    public void setup() {
        initMocks(this);

        when(serviceReference.getProperty(HealthCheck.NAME)).thenReturn("Test");
        when(serviceReference.getProperty(HealthCheck.TAGS)).thenReturn(new String[] { "tag1", "tag2" });
        when(serviceReference.getProperty(Constants.SERVICE_ID)).thenReturn(1L);
    }

    @Test
    public void testJsonSerialisation() {

        FormattingResultLog log = new FormattingResultLog();
        log.info("test message");
        Result result = new Result(log);
        HealthCheckMetadata hcMetadata = new HealthCheckMetadata(serviceReference);
        HealthCheckExecutionResult executionResult = new ExecutionResult(hcMetadata, result, 100);
        Result overallResult = new Result(Result.Status.OK, "Overall status");

        String json = resultJsonSerializer.serialize(overallResult, Arrays.asList(executionResult), null, false);
        assertThat(json, containsString("\"overallResult\":\"OK\""));
        assertThat(json, containsString("\"tags\":[\"tag1\",\"tag2\"]"));
        assertThat(json, containsString("\"messages\":[{\"status\":\"OK\",\"message\":\"test message\"}]"));
    }

}
