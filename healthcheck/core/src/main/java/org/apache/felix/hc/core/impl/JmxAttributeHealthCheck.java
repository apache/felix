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
package org.apache.felix.hc.core.impl;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.util.FormattingResultLog;
import org.apache.felix.hc.util.SimpleConstraintChecker;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link HealthCheck} that checks a single JMX attribute */
@Component(service = HealthCheck.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = JmxAttributeHealthCheckConfiguration.class, factory = true)
public class JmxAttributeHealthCheck implements HealthCheck {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private String mbeanName;
    private String attributeName;
    private String constraint;

    @Activate
    protected void activate(final JmxAttributeHealthCheckConfiguration configuration) {
        mbeanName = configuration.mbean_name();
        attributeName = configuration.attribute_name();
        constraint = configuration.attribute_value_constraint();

        log.debug("Activated with HealthCheck name={}, objectName={}, attribute={}, constraint={}",
                new Object[] { configuration.hc_name(), mbeanName, attributeName, constraint });
    }

    @Override
    public Result execute() {
        final FormattingResultLog resultLog = new FormattingResultLog();
        resultLog.debug("Checking {} / {} with constraint {}", mbeanName, attributeName, constraint);
        try {
            final MBeanServer jmxServer = ManagementFactory.getPlatformMBeanServer();
            final ObjectName objectName = new ObjectName(mbeanName);
            if (jmxServer.queryNames(objectName, null).size() == 0) {
                resultLog.warn("MBean not found: {}", objectName);
            } else {
                final Object value = jmxServer.getAttribute(objectName, attributeName);
                resultLog.debug("{} {} returns {}", mbeanName, attributeName, value);
                new SimpleConstraintChecker().check(value, constraint, resultLog);
            }
        } catch (final Exception e) {
            log.warn("JMX attribute {}/{} check failed: {}", new Object[] { mbeanName, attributeName, e });
            resultLog.healthCheckError("JMX attribute check failed: {}", e);
        }
        return new Result(resultLog);
    }
}
