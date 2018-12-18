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
package org.apache.felix.hc.webconsole.impl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.core.impl.executor.ExecutionResult;
import org.apache.felix.hc.util.FormattingResultLog;
import org.apache.felix.hc.util.HealthCheckMetadata;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class HealthCheckWebconsolePluginTest {

    @Mock
    ServiceReference<HealthCheck> serviceRef;
    
    private HealthCheckWebconsolePlugin webConsolePlugin = new HealthCheckWebconsolePlugin();


    @Before
    public void setup() {
        initMocks(this);
        
        doReturn("Test HC <should-be-escaped>").when(serviceRef).getProperty(HealthCheck.NAME);
        doReturn(1L).when(serviceRef).getProperty(Constants.SERVICE_ID);

    }

    
    @Test
    public void testRenderResultNoDebug() throws IOException {

        StringWriter writer = new StringWriter();
        
        webConsolePlugin.renderResult(new PrintWriter(writer), getExecutionResult(), false);
        
        String resultStr = writer.toString();
        assertThat(resultStr, containsString("Test HC &lt;should-be-escaped&gt;"));
        assertThat(resultStr, containsString("<div class='logOK'>OK HC log with level 'info'</div>"));
        assertThat(resultStr, not(containsString("<div class='logDEBUG'>OK HC log with level 'debug'</div>")));

    }

    @Test
    public void testRenderResultWithDebug() throws IOException {

        StringWriter writer = new StringWriter();
        
        webConsolePlugin.renderResult(new PrintWriter(writer), getExecutionResult(), true);
        
        String resultStr = writer.toString();
        assertThat(resultStr, containsString("<div class='logDEBUG'>OK HC log with level 'debug'</div>"));

    }

    private ExecutionResult getExecutionResult() {
        FormattingResultLog resultLog = new FormattingResultLog();
        resultLog.info("HC log with level 'info'");
        resultLog.debug("HC log with level 'debug'");

        HealthCheckMetadata metadata = new HealthCheckMetadata(serviceRef);
        Result result = new Result(resultLog);
        return new ExecutionResult(metadata, result, 777);
    }
    
}
