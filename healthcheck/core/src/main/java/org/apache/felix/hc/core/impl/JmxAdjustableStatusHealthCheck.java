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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ReflectionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.Result.Status;
import org.apache.felix.hc.api.ResultLog.Entry;
import org.apache.felix.hc.util.FormattingResultLog;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Allows to dynamically add a health check that returns WARN or CRITICAL for certain tags for testing purposes or go-live sequences. Uses an MBean to
 * add/remove the DynamicTestingHealthCheck dynamically. */
@Component
public class JmxAdjustableStatusHealthCheck {
    private static final Logger LOG = LoggerFactory.getLogger(JmxAdjustableStatusHealthCheck.class);

    public static final String OBJECT_NAME = "org.apache.felix.healthcheck:type=AdjustableStatusHealthCheck";

    private BundleContext bundleContext;

    private ServiceRegistration mbeanRegistration = null;
    private ServiceRegistration healthCheckRegistration = null;

    @Activate
    protected final void activate(final ComponentContext context) {
        this.bundleContext = context.getBundleContext();
        registerMbean();
    }

    @Deactivate
    protected final void deactivate(final ComponentContext context) {
        unregisterMbean();
        unregisterDynamicHealthCheck();
    }

    private void registerMbean() {
        final Dictionary<String, String> mbeanProps = new Hashtable<String, String>();
        mbeanProps.put("jmx.objectname", OBJECT_NAME);
        AdjustableHealthCheckStatusMBean adjustableHealthCheckStatusMBean = new AdjustableHealthCheckStatusMBean();
        this.mbeanRegistration = bundleContext.registerService(DynamicMBean.class.getName(), adjustableHealthCheckStatusMBean, mbeanProps);
        LOG.debug("Registered mbean {} as {}", adjustableHealthCheckStatusMBean, OBJECT_NAME);
    }

    private void unregisterMbean() {
        if (this.mbeanRegistration != null) {
            this.mbeanRegistration.unregister();
            this.mbeanRegistration = null;
            LOG.debug("Unregistered mbean AdjustableHealthCheckStatusMBean");
        }
    }

    /* synchronized as potentially multiple users can run JMX operations */
    private synchronized void registerDynamicHealthCheck(Result.Status status, String[] tags) {
        unregisterDynamicHealthCheck();
        HealthCheck healthCheck = new AdhocStatusOnlyHealthCheck(status);
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(HealthCheck.NAME, "JMX Adhoc Result");
        props.put(HealthCheck.TAGS, tags);

        healthCheckRegistration = bundleContext.registerService(HealthCheck.class.getName(), healthCheck, props);

    }

    /* synchronized as potentially multiple users can run JMX operations */
    private synchronized void unregisterDynamicHealthCheck() {
        if (this.healthCheckRegistration != null) {
            this.healthCheckRegistration.unregister();
            this.healthCheckRegistration = null;
            LOG.debug("Unregistered DynamicTestingHealthCheck");
        }
    }

    class AdhocStatusOnlyHealthCheck implements HealthCheck {

        private final Result.Status status;

        AdhocStatusOnlyHealthCheck(Result.Status status) {
            this.status = status;
        }

        @Override
        public Result execute() {
            FormattingResultLog resultLog = new FormattingResultLog();
            resultLog.add(
                    new Entry(status, "Set dynamically via JMX bean " + OBJECT_NAME));
            return new Result(resultLog);
        }

    }

    private class AdjustableHealthCheckStatusMBean implements DynamicMBean {

        private static final String OP_RESET = "reset";
        private static final String OP_ADD_WARN_RESULT_FOR_TAGS = "addWarnResultForTags";
        private static final String OP_ADD_TEMPORARILY_UNAVAILABLE_RESULT_FOR_TAGS = "addTemporarilyUnavailableResultForTags";
        private static final String OP_ADD_CRITICAL_RESULT_FOR_TAGS = "addCriticalResultForTags";

        private static final String ATT_TAGS = "tags";
        private static final String ATT_STATUS = "status";

        private static final String STATUS_INACTIVE = "INACTIVE";

        /** The mbean info. */
        private final MBeanInfo mbeanInfo;

        private List<String> tags = new ArrayList<String>();
        private String status = STATUS_INACTIVE;

        public AdjustableHealthCheckStatusMBean() {
            this.mbeanInfo = this.createMBeanInfo();
        }

        @Override
        public Object getAttribute(final String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {

            if (ATT_TAGS.equals(attribute)) {
                return StringUtils.join(tags, ",");
            } else if (ATT_STATUS.equals(attribute)) {
                return status.toString();
            } else {
                throw new AttributeNotFoundException("Attribute " + attribute + " not found.");
            }
        }

        @Override
        public AttributeList getAttributes(final String[] attributes) {
            final AttributeList result = new AttributeList();
            for (String att : attributes) {
                try {
                    result.add(new Attribute(att, getAttribute(att)));
                } catch (Exception e) {
                    throw new IllegalArgumentException(e.getMessage(), e);
                }
            }
            return result;
        }

        private MBeanInfo createMBeanInfo() {
            final List<MBeanAttributeInfo> attrs = new ArrayList<MBeanAttributeInfo>();

            attrs.add(new MBeanAttributeInfo(ATT_TAGS, String.class.getName(), "Tags", true, false, false));
            attrs.add(new MBeanAttributeInfo(ATT_STATUS, String.class.getName(), "Status", true, false, false));

            MBeanParameterInfo[] params = new MBeanParameterInfo[] {
                    new MBeanParameterInfo(ATT_TAGS, "java.lang.String", "Comma separated list of tags") };

            final List<MBeanOperationInfo> ops = new ArrayList<MBeanOperationInfo>();
            ops.add(new MBeanOperationInfo(OP_RESET, "Resets this testing mechanism and removes the failing HC",
                    new MBeanParameterInfo[0], "java.lang.String", MBeanOperationInfo.ACTION));
            ops.add(new MBeanOperationInfo(OP_ADD_TEMPORARILY_UNAVAILABLE_RESULT_FOR_TAGS, "Adds a TEMPORARILY_UNAVAILABLE result for the given tags",
                    params, "java.lang.String", MBeanOperationInfo.ACTION));
            ops.add(new MBeanOperationInfo(OP_ADD_CRITICAL_RESULT_FOR_TAGS, "Adds a CRITICAL result for the given tags",
                    params, "java.lang.String", MBeanOperationInfo.ACTION));
            ops.add(new MBeanOperationInfo(OP_ADD_WARN_RESULT_FOR_TAGS, "Adds a WARN result for the given tags",
                    params, "java.lang.String", MBeanOperationInfo.ACTION));

            return new MBeanInfo(this.getClass().getName(),
                    "Adjustable Health Check", attrs.toArray(new MBeanAttributeInfo[attrs.size()]), null,
                    ops.toArray(new MBeanOperationInfo[ops.size()]), null);
        }

        @Override
        public MBeanInfo getMBeanInfo() {
            return this.mbeanInfo;
        }

        @Override
        public Object invoke(final String actionName, final Object[] params, final String[] signature)
                throws MBeanException, ReflectionException {
            
            Status newStatus = null;
            
            tags = params.length > 0 ? Arrays.asList(params[0].toString().split("[,; ]+")) : Arrays.asList("");
            
            if (OP_RESET.equals(actionName)) {
                unregisterDynamicHealthCheck();
                LOG.info("JMX-adjustable Health Check was reset");
                status = STATUS_INACTIVE;
                return "Reset successful";
            } else if (OP_ADD_TEMPORARILY_UNAVAILABLE_RESULT_FOR_TAGS.equals(actionName)) {
                newStatus =  Result.Status.TEMPORARILY_UNAVAILABLE;
            } else if (OP_ADD_CRITICAL_RESULT_FOR_TAGS.equals(actionName)) {
                newStatus =  Result.Status.CRITICAL;
            } else if (OP_ADD_WARN_RESULT_FOR_TAGS.equals(actionName)) {
                newStatus =  Result.Status.WARN;
            } else {
                throw new MBeanException(
                        new UnsupportedOperationException(getClass().getSimpleName() + " does not support operation " + actionName));
            }
            
            status = newStatus.toString();
            registerDynamicHealthCheck(newStatus, tags.toArray(new String[tags.size()]));
            LOG.info("Activated JMX-adjustable Health Check with status "+newStatus+" and tags " + StringUtils.join(tags, ","));
            return "Added check with result "+newStatus;
        
        }

        @Override
        public void setAttribute(final Attribute attribute)
                throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException,
                ReflectionException {
            throw new MBeanException(
                    new UnsupportedOperationException(getClass().getSimpleName() + " does not support setting attributes."));
        }

        @Override
        public AttributeList setAttributes(final AttributeList attributes) {
            return new AttributeList();
        }
    }

}
