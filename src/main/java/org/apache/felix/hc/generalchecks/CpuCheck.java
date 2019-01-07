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

import java.lang.management.ManagementFactory;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

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

@HealthCheckService(name = CpuCheck.HC_NAME)
@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = CpuCheck.Config.class, factory = false)
public class CpuCheck implements HealthCheck {

    private static final Logger LOG = LoggerFactory.getLogger(CpuCheck.class);

    public static final String HC_NAME = "CPU";
    public static final String HC_LABEL = "Health Check: " + HC_NAME;

    @ObjectClassDefinition(name = HC_LABEL, description = "Checks for high CPU load")
    public @interface Config {
        @AttributeDefinition(name = "Name", description = "Name of this health check")
        String hc_name() default HC_NAME;

        @AttributeDefinition(name = "Tags", description = "List of tags for this health check, used to select subsets of health checks for execution e.g. by a composite health check.")
        String[] hc_tags() default {};

        @AttributeDefinition(name = "CPU usage threshold for WARN", description = "in percent, if CPU usage is over this limit the result is WARN")
        long cpuPercentageThresholdWarn() default 95;

    }

    private long cpuPercentageThresholdWarn;

    @Activate
    protected void activate(final Config config) {
        cpuPercentageThresholdWarn = config.cpuPercentageThresholdWarn();
        LOG.info("Activated CPU HC: cpuPercentageThresholdWarn={}%", cpuPercentageThresholdWarn);
    }

    @Override
    public Result execute() {

        FormattingResultLog log = new FormattingResultLog();

        double processCpuLoad = Double.NaN;
        try {
            processCpuLoad = getProcessCpuLoad();
        } catch (Exception e) {
            log.add(new ResultLog.Entry(Result.Status.HEALTH_CHECK_ERROR, "Could not get process CPU load: " + e, e));
        }

        if (Double.isNaN(processCpuLoad)) {
            log.info("No CPU load available yet");
        } else {
            String loadStr = String.format("%.1f", processCpuLoad);
            Result.Status status = processCpuLoad < cpuPercentageThresholdWarn ? Result.Status.OK : Result.Status.WARN;
            log.add(new ResultLog.Entry(status, "Process CPU Usage: " + loadStr + "%"));
        }
        return new Result(log);

    }

    public double getProcessCpuLoad() throws MalformedObjectNameException, ReflectionException, InstanceNotFoundException {

        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName name = ObjectName.getInstance("java.lang:type=OperatingSystem");
        AttributeList list = mbs.getAttributes(name, new String[] { "ProcessCpuLoad" });

        if (list.isEmpty()) {
            return Double.NaN;
        }

        Attribute att = (Attribute) list.get(0);
        Double value = (Double) att.getValue();

        if (value == -1.0) {
            return Double.NaN;
        }

        return value * 100;
    }

}
