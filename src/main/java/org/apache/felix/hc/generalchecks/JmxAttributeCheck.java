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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.hc.api.FormattingResultLog;
import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog;
import org.apache.felix.hc.generalchecks.util.SimpleConstraintChecker;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@link HealthCheck} that checks one (or multiple) JMX attribute(s). */
@Component(service = HealthCheck.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = JmxAttributeCheck.Config.class, factory = true)
public class JmxAttributeCheck implements HealthCheck {

    private static final Logger LOG = LoggerFactory.getLogger(JmxAttributeCheck.class);

    public static final String HC_NAME = "JMX Attribute";
    public static final String HC_LABEL = "Health Check: " + HC_NAME;

    private Result.Status statusForFailedContraint;

    @ObjectClassDefinition(name = HC_LABEL, description = "Checks the value of a single JMX attribute.")
    @interface Config {

        @AttributeDefinition(name = "Name", description = "Name of this health check.")
        String hc_name() default HC_NAME;

        @AttributeDefinition(name = "Tags", description = "List of tags for this health check, used to select subsets of health checks for execution e.g. by a composite health check.")
        String[] hc_tags() default {};

        @AttributeDefinition(name = "MBean Name", description = "The name of the MBean to retrieve the attribute to be checked from.")
        String mbean_name() default "";

        @AttributeDefinition(name = "Attribute Name", description = "The name of the MBean attribute to check against the constraint.")
        String attribute_name() default "";
 
        @AttributeDefinition(name = "Attribute Constraint", description = "Constraint on the MBean attribute value. If simple value, uses equals. For strings constraints like 'CONTAINS mystr', 'STARTS_WITH mystr' or 'ENDS_WITH mystr' can be used, for numbers constraints like '> 4', '= 7', '< 9' or 'between 3 and 7' work.")
        String attribute_value_constraint() default "";

        @AttributeDefinition(name = "Status for failed constraint", description = "Status to fail with if the constraint check fails")
        Result.Status statusForFailedContraint() default Result.Status.WARN;
        
        @AttributeDefinition
        String webconsole_configurationFactory_nameHint() default "JMX MBean {mbean.name} Attribute '{attribute.name}' constraint: {attribute.value.constraint}";
    }

    private List<AttributeConstraintConfig> attributeConstraintConfigs;
    
    @Activate
    protected void activate(final Config config, final Map<String,Object> rawConfig) {
        statusForFailedContraint = config.statusForFailedContraint();
        attributeConstraintConfigs = AttributeConstraintConfig.load(config, rawConfig);
        
        LOG.info("Activated JMX Attribute HC with statusForFailedContraint={} and attribute constraint config(s):", statusForFailedContraint);
        for (AttributeConstraintConfig attributeConstraintConfig : attributeConstraintConfigs) {
            LOG.info(attributeConstraintConfig.toString());
        }
    }


    @Override
    public Result execute() {
        FormattingResultLog resultLog = new FormattingResultLog();
        for (AttributeConstraintConfig attributeConstraintConfig : attributeConstraintConfigs) {
            checkAttributeConstraint(resultLog, attributeConstraintConfig);
        }
        return new Result(resultLog);
    }

    private void checkAttributeConstraint(final FormattingResultLog resultLog, AttributeConstraintConfig attributeConstraintConfig) {
        resultLog.debug("Checking {}", attributeConstraintConfig);
        try {
            final MBeanServer jmxServer = ManagementFactory.getPlatformMBeanServer();
            final ObjectName objectName = new ObjectName(attributeConstraintConfig.mbeanName);
            if (jmxServer.queryNames(objectName, null).size() == 0) {
                resultLog.warn("MBean not found: {}", objectName);
            } else {
                final Object value = jmxServer.getAttribute(objectName, attributeConstraintConfig.attributeName);
                resultLog.debug("{} {} returns {}", attributeConstraintConfig.mbeanName, attributeConstraintConfig.attributeName, value);
                boolean matches = new SimpleConstraintChecker().check(value, attributeConstraintConfig.attributeValueConstraint);
                String baseMsg = "JMX attribute "+attributeConstraintConfig.mbeanName+" -> '"+attributeConstraintConfig.attributeName+"': Value [" + value + "] ";
                if (matches) {
                    resultLog.add(new ResultLog.Entry(Result.Status.OK, baseMsg+"matches constraint [" + attributeConstraintConfig.attributeValueConstraint + "]"));
                } else {
                    resultLog.add(new ResultLog.Entry( statusForFailedContraint, baseMsg+"does not match constraint [" + attributeConstraintConfig.attributeValueConstraint + "]"));
                }
            }
        } catch (Exception e) {
            LOG.warn("JMX check {} failed: {}", attributeConstraintConfig, e.getMessage(), e);
            resultLog.healthCheckError("JMX attribute check failed: {}", attributeConstraintConfig, e);
        }
    }
    
    
    private static class AttributeConstraintConfig {
        
        public static final String PROP_MBEAN = "mbean";
        public static final String PROP_ATTRIBUTE = "attribute";

        public static final String SUFFIX_NAME = ".name";
        public static final String SUFFIX_VALUE_CONSTRAINT = ".value.constraint";

        private static List<AttributeConstraintConfig> load(final Config config, final Map<String, Object> rawConfig) {
            List<AttributeConstraintConfig> attributeConstraintConfigs = new ArrayList<AttributeConstraintConfig>();
            
            // first attribute via metatype
            attributeConstraintConfigs.add(new AttributeConstraintConfig(config.mbean_name(), config.attribute_name(),config.attribute_value_constraint()));

            // additional attributes possible via naming scheme "mbean2.name" / "attribute2.name" ...
            int attributeCounter = 2;
            while(AttributeConstraintConfig.hasConfig(rawConfig, attributeCounter)) {
                attributeConstraintConfigs.add(new AttributeConstraintConfig(rawConfig, attributeCounter));
                attributeCounter++;
            }
            return attributeConstraintConfigs;
        }

        private static String getAttributePropName(int attributeCounter) {
            return PROP_ATTRIBUTE + attributeCounter + SUFFIX_NAME;
        }
        
        private static boolean hasConfig(Map<String,Object> rawConfig, int attributeCounter) {
            return rawConfig.containsKey(getAttributePropName(attributeCounter));
        }
        
        final String mbeanName;
        
        final String attributeName;
        final String attributeValueConstraint;
        
        public AttributeConstraintConfig(String mbeanName, String attributeName, String attributeValueConstraint) {
            this.mbeanName = mbeanName;
            this.attributeName = attributeName;
            this.attributeValueConstraint = attributeValueConstraint;
        }

        public AttributeConstraintConfig(Map<String,Object> rawConfig, int attributeCounter) {
            String propNameAttribute = getAttributePropName(attributeCounter);
            String defaultMBeanName = (String) rawConfig.get(PROP_MBEAN + SUFFIX_NAME);
            String mBeanName = (String) rawConfig.get(PROP_MBEAN + attributeCounter + SUFFIX_NAME);
            this.mbeanName = StringUtils.defaultIfBlank(mBeanName, defaultMBeanName);
            this.attributeName = (String) rawConfig.get(propNameAttribute);
            this.attributeValueConstraint = (String) rawConfig.get(PROP_ATTRIBUTE + attributeCounter + SUFFIX_VALUE_CONSTRAINT);
            if(StringUtils.isAnyBlank(mbeanName, attributeName, attributeValueConstraint)) {
                throw new IllegalArgumentException("Invalid JmxAttributeCheck config for property "+mbeanName+" -> "+propNameAttribute+": "+toString());
            }
        }
        
        @Override
        public String toString() {
            return "JMX attribute "+mbeanName+" -> '"+attributeName+"': Constraint: "+attributeValueConstraint;
        };
    }
}
