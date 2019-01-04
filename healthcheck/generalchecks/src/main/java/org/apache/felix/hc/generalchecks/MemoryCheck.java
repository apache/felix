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
package org.apache.felix.hc.generalchecks;

import static org.apache.felix.hc.generalchecks.util.UnitsUtil.formatBytes;

import org.apache.felix.hc.annotation.HealthCheckService;
import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog.Entry;
import org.apache.felix.hc.util.FormattingResultLog;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@HealthCheckService(name = MemoryCheck.HC_NAME)
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = MemoryCheck.Config.class, factory = false)
public class MemoryCheck implements HealthCheck {

    private static final Logger LOG = LoggerFactory.getLogger(MemoryCheck.class);

    public static final String HC_NAME = "Memory";
    public static final String HC_LABEL = "Health Check: " + HC_NAME;

    @ObjectClassDefinition(name = HC_LABEL, description = "Checks for high CPU load")
    public @interface Config {
        @AttributeDefinition(name = "Name", description = "Name of this health check")
        String hc_name() default HC_NAME;

        @AttributeDefinition(name = "Tags", description = "List of tags for this health check, used to select subsets of health checks for execution e.g. by a composite health check.")
        String[] hc_tags() default {};

        @AttributeDefinition(name = "Heap used threshold for WARN", description = "in percent, if heap usage is over this limit the result is WARN")
        long heapUsedPercentageThresholdWarn() default 90;

        @AttributeDefinition(name = "Heap used threshold for CRITICAL", description = "in percent, if heap usage is over this limit the result is CRITICAL")
        long heapUsedPercentageThresholdCritical() default 99;
    }

    private long heapUsedPercentageThresholdWarn;
    private long heapUsedPercentageThresholdCritical;

    @Activate
    protected void activate(final Config config) {
        heapUsedPercentageThresholdWarn = config.heapUsedPercentageThresholdWarn();
        heapUsedPercentageThresholdCritical = config.heapUsedPercentageThresholdCritical();
        LOG.info("Activated Memory HC: heapUsedPercentageThresholdWarn={}% heapUsedPercentageThresholdCritical={}%", heapUsedPercentageThresholdWarn, heapUsedPercentageThresholdCritical);
    }

    @Override
    public Result execute() {
        FormattingResultLog log = new FormattingResultLog();

        Runtime runtime = Runtime.getRuntime();

        long freeMemory = runtime.freeMemory();
        log.debug("Free memory: {}", formatBytes(freeMemory));
        long currentlyAllocatedByJVM = runtime.totalMemory();
        log.debug("Currently allocated memory: {}", formatBytes(currentlyAllocatedByJVM));
        long usedMemory = currentlyAllocatedByJVM - freeMemory;
        log.debug("Used memory: {}", formatBytes(usedMemory));
        long maxMemoryAvailableToJVM = runtime.maxMemory();
        
        double memoryUsedPercentage = ((double) usedMemory / maxMemoryAvailableToJVM * 100d);

        Result.Status status =
                memoryUsedPercentage < this.heapUsedPercentageThresholdWarn ? Result.Status.OK :
                        memoryUsedPercentage < this.heapUsedPercentageThresholdCritical ? Result.Status.WARN
                                : Result.Status.CRITICAL;

        String message = String.format("Memory Usage: %.1f%% of %s maximal heap used", memoryUsedPercentage, formatBytes(maxMemoryAvailableToJVM));

        log.add(new Entry(status, message));

        return new Result(log);
    }

}
