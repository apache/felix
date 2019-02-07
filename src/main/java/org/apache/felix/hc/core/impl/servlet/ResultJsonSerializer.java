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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.utils.json.JSONWriter;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Serializes health check results into json format. */
@Component(service = ResultJsonSerializer.class)
public class ResultJsonSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(ResultJsonSerializer.class);

    static final String OVERALL_RESULT_KEY = "OverallResult";

    public String serialize(final Result overallResult, final List<HealthCheckExecutionResult> executionResults, final String jsonpCallback,
            boolean includeDebug) {

        LOG.debug("Sending json response... ");

        StringWriter writer = new StringWriter();
        try {
            JSONWriter jsonWriter = new JSONWriter(writer);
            jsonWriter.object();
            jsonWriter.key("overallResult");
            jsonWriter.value(overallResult.getStatus().toString());
            jsonWriter.key("results");
            jsonWriter.array();
            for (HealthCheckExecutionResult healthCheckResult : executionResults) {
                writeResult(healthCheckResult, includeDebug, jsonWriter);
            }
            jsonWriter.endArray();
            jsonWriter.endObject();
        } catch(IOException e) {
            LOG.error("Could not serialise health check result: e="+e, e);
            writer.write("{error:'"+e.getMessage()+"'}");
        }
        String resultStr = writer.toString();

        if (StringUtils.isNotBlank(jsonpCallback)) {
            resultStr = jsonpCallback + "(" + resultStr + ");";
        }

        return resultStr;

    }

    private void writeResult(final HealthCheckExecutionResult healthCheckResult, boolean includeDebug, JSONWriter jsonWriter) throws IOException {

        jsonWriter.object()
            .key("name").value(healthCheckResult.getHealthCheckMetadata().getTitle())
            .key("status").value(healthCheckResult.getHealthCheckResult().getStatus().toString()) 
            .key("timeInMs").value(healthCheckResult.getElapsedTimeInMs())
            .key("finishedAt").value(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(healthCheckResult.getFinishedAt())) ;
        
        jsonWriter.key("tags").array();
        for(String tag: healthCheckResult.getHealthCheckMetadata().getTags()) {
            jsonWriter.value(tag);
        }
        jsonWriter.endArray();
        
        jsonWriter.key("messages").array();
        for (ResultLog.Entry entry : healthCheckResult.getHealthCheckResult()) {
            if (!includeDebug && entry.isDebug()) {
                continue;
            }
            jsonWriter.object()
                .key("status").value(entry.getStatus().toString())
                .key("message").value(entry.getMessage());
            
            Exception exception = entry.getException();
            if (exception != null) {
                StringWriter stringWriter = new StringWriter();
                exception.printStackTrace(new PrintWriter(stringWriter));
                jsonWriter.key("exception").value(stringWriter.toString());
            }
            jsonWriter.endObject();
        }
        jsonWriter.endArray();
        
        
        jsonWriter.endObject();
    }

}
