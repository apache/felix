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

import java.io.File;
import java.util.Arrays;

import org.apache.felix.hc.annotation.HealthCheckService;
import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog;
import org.apache.felix.hc.util.FormattingResultLog;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@HealthCheckService(name = DiskSpaceCheck.HC_NAME)
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = DiskSpaceCheck.Config.class, factory = true)
public class DiskSpaceCheck implements HealthCheck {

    private static final Logger LOG = LoggerFactory.getLogger(DiskSpaceCheck.class);

    public static final String HC_NAME = "Disk Space";
    public static final String HC_LABEL = "Health Check: " + HC_NAME;

    @ObjectClassDefinition(name = HC_LABEL, description = "Checks the configured path(s) against the given thresholds")
    public @interface Config {
        @AttributeDefinition(name = "Name", description = "Name of this health check")
        String hc_name() default HC_NAME;

        @AttributeDefinition(name = "Tags", description = "List of tags for this health check, used to select subsets of health checks for execution e.g. by a composite health check.")
        String[] hc_tags() default {};

        @AttributeDefinition(name = "Disk used threshold for WARN", description = "in percent, if disk usage is over this limit the result is WARN")
        long diskUsedThresholdWarn() default 90;

        @AttributeDefinition(name = "Disk used threshold for CRITICAL", description = "in percent, if disk usage is over this limit the result is CRITICAL")
        long diskUsedThresholdCritical() default 97;

        @AttributeDefinition(name = "Paths to check for disk usage", description = "Paths that is checked for free space according the configured thresholds")
        String[] diskPaths() default { "." };

        @AttributeDefinition
        String webconsole_configurationFactory_nameHint() default "{hc.name}: {diskPaths} used>{diskUsedThresholdWarn}% -> WARN  used>{diskUsedThresholdCritical}% -> CRITICAL";
    }

    private long diskUsedThresholdWarn;
    private long diskUsedThresholdCritical;
    private String[] diskPaths;

    @Activate
    protected void activate(final Config config) {
        diskUsedThresholdWarn = config.diskUsedThresholdWarn();
        diskUsedThresholdCritical = config.diskUsedThresholdCritical();
        diskPaths = config.diskPaths();

        LOG.info("Activated disk usage HC for path(s) {} diskUsedThresholdWarn={}% diskUsedThresholdCritical={}%", Arrays.asList(diskPaths),
                diskUsedThresholdWarn, diskUsedThresholdCritical);
    }

    @Override
    public Result execute() {

        FormattingResultLog log = new FormattingResultLog();

        for (String diskPath : diskPaths) {

            File diskPathFile = new File(diskPath);

            if (!diskPathFile.exists()) {
                log.warn("Directory '{}' does not exist", diskPathFile);
                continue;
            } else if (!diskPathFile.isDirectory()) {
                log.warn("Directory '{}' is not a directory", diskPathFile);
                continue;
            }

            double total = diskPathFile.getTotalSpace();
            double free = diskPathFile.getUsableSpace();
            double usedPercentage = (total - free) / total * 100d;

            String totalStr = formatBytes(total);
            String freeStr = formatBytes(free);
            String msg = String.format("Disk Usage %s: %.1f%% of %s used / %s free", diskPathFile.getAbsolutePath(),
                    usedPercentage,
                    totalStr, freeStr);

            Result.Status status = usedPercentage > this.diskUsedThresholdCritical ? Result.Status.CRITICAL
                    : usedPercentage > this.diskUsedThresholdWarn ? Result.Status.WARN
                            : Result.Status.OK;

            log.add(new ResultLog.Entry(status, msg));

        }

        return new Result(log);
    }


}
