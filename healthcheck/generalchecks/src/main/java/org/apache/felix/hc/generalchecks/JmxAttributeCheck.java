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

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog;
import org.apache.felix.hc.generalchecks.util.SimpleConstraintChecker;
import org.apache.felix.hc.util.FormattingResultLog;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link HealthCheck} that checks a single JMX attribute */
@Component(service = HealthCheck.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = JmxAttributeCheck.Config.class, factory = true)
public class JmxAttributeCheck implements HealthCheck {

    private static final Logger LOG = LoggerFactory.getLogger(JmxAttributeCheck.class);

    public static final String HC_NAME = "JMX Attribute";
    public static final String HC_LABEL = "Health Check: " + HC_NAME;

    
    private String mbeanName;
    private String attributeName;
    private String constraint;
    private Result.Status statusForFailedContraint;

    @ObjectClassDefinition(name = HC_LABEL, description = "Checks the value of a single JMX attribute.")
    @interface Config {

        @AttributeDefinition(name = "Name", description = "Name of this health check.")
        String hc_name() default HC_NAME;

        @AttributeDefinition(name = "Tags", description = "List of tags for this health check, used to select subsets of health checks for execution e.g. by a composite health check.")
        String[] hc_tags() default {};

        @AttributeDefinition(name = "MBean Name", description = "The name of the MBean to retrieve the attribute to be checked from.")
        String mbean_name() default "";

        @AttributeDefinition(name = "Attribute Name", description = "The name of the MBean attribute to check against the constraing.")
        String attribute_name() default "";
 
        @AttributeDefinition(name = "Attribute Constraint", description = "Constraint on the MBean attribute value. If simple value, uses equals. For strings constraints like 'CONTAINS mystr', 'STARTS_WITH mystr' or 'ENDS_WITH mystr' can be used, for numbers constraints like '> 4', '= 7', '< 9' or 'between 3 and 7' work.")
        String attribute_value_constraint() default "";

        @AttributeDefinition(name = "Status for failed constraint", description = "Status to fail with if the constraint check fails")
        Result.Status statusForFailedContraint() default Result.Status.WARN;
        
        @AttributeDefinition
        String webconsole_configurationFactory_nameHint() default "JMX MBean {mbean.name} Attribute '{attribute.name}' constraint: {attribute.value.constraint}";
    }

    
    @Activate
    protected void activate(final Config config) {
        mbeanName = config.mbean_name();
        attributeName = config.attribute_name();
        constraint = config.attribute_value_constraint();
        statusForFailedContraint = config.statusForFailedContraint();
        LOG.info("Activated JMX Attribute HC for mbeanName {} attributeName={} constraint={} statusForFailedContraint={}", mbeanName, attributeName, constraint, statusForFailedContraint);
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
                boolean matches = new SimpleConstraintChecker().check(value, constraint);
                String baseMsg = "JMX attribute "+mbeanName+" -> '"+attributeName+"': Value [" + value + "] ";
                if (matches) {
                    resultLog.add(new ResultLog.Entry(Result.Status.OK, baseMsg+"matches constraint [" + constraint + "]"));
                } else {
                    resultLog.add(new ResultLog.Entry( statusForFailedContraint, baseMsg+"does not match constraint [" + constraint + "]"));
                }
            }
        } catch (Exception e) {
            LOG.warn("JMX attribute {}.{} check failed: {}", mbeanName, attributeName, e.getMessage(), e);
            resultLog.healthCheckError("JMX attribute check failed: {}", e);
        }
        return new Result(resultLog);
    }
}
