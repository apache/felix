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
package org.apache.felix.hc.jmx.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.felix.hc.api.ResultLog;
import org.apache.felix.hc.api.execution.HealthCheckExecutionResult;
import org.apache.felix.hc.core.impl.executor.ExtendedHealthCheckExecutor;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/** A {@link DynamicMBean} used to execute a {@link HealthCheck} service */
public class HealthCheckMBean implements DynamicMBean {

    private static final String HC_OK_ATTRIBUTE_NAME = "ok";
    private static final String HC_STATUS_ATTRIBUTE_NAME = "status";
    private static final String HC_LOG_ATTRIBUTE_NAME = "log";
    private static final String HC_TIMED_OUT_ATTRIBUTE_NAME = "timedOut";
    private static final String HC_ELAPSED_TIMED_ATTRIBUTE_NAME = "elapsedTime";
    private static final String HC_FINISHED_AT_ATTRIBUTE_NAME = "finishedAt";
    private static CompositeType LOG_ROW_TYPE;
    private static TabularType LOG_TABLE_TYPE;

    private static final String INDEX_COLUMN = "index";
    private static final String LEVEL_COLUMN = "level";
    private static final String MESSAGE_COLUMN = "message";

    /** The health check service to call. */
    private final ServiceReference<HealthCheck> healthCheckRef;

    /** The executor service. */
    private final ExtendedHealthCheckExecutor executor;

    /** The mbean info. */
    private final MBeanInfo mbeanInfo;

    /** The default attributes. */
    private final Map<String, Object> defaultAttributes;

    static {
        try {
            // Define the log row and table types
            LOG_ROW_TYPE = new CompositeType(
                    "LogLine",
                    "A line in the result log",
                    new String[] { INDEX_COLUMN, LEVEL_COLUMN, MESSAGE_COLUMN },
                    new String[] { "log line index", "log level", "log message" },
                    new OpenType[] { SimpleType.INTEGER, SimpleType.STRING, SimpleType.STRING });
            final String[] indexes = { INDEX_COLUMN };
            LOG_TABLE_TYPE = new TabularType("LogTable", "Result log messages", LOG_ROW_TYPE, indexes);
        } catch (Exception ignore) {
            // row or table type will be null if this happens
        }
    }

    public HealthCheckMBean(final ServiceReference<HealthCheck> ref, final ExtendedHealthCheckExecutor executor) {
        this.healthCheckRef = ref;
        this.executor = executor;
        this.mbeanInfo = this.createMBeanInfo(ref);
        this.defaultAttributes = this.createDefaultAttributes(ref);
    }

    @Override
    public Object getAttribute(final String attribute)
            throws AttributeNotFoundException, MBeanException, ReflectionException {
        // we should call getAttributes - and not vice versa to have the result
        // of a single check call - and not do a check call for each attribute
        final AttributeList result = this.getAttributes(new String[] { attribute });
        if (result.size() == 0) {
            throw new AttributeNotFoundException(attribute);
        }
        final Attribute attr = (Attribute) result.get(0);
        return attr.getValue();
    }

    private TabularData logData(final Result er) throws OpenDataException {
        final TabularDataSupport result = new TabularDataSupport(LOG_TABLE_TYPE);
        int i = 1;
        for (final ResultLog.Entry e : er) {
            final Map<String, Object> data = new HashMap<String, Object>();
            data.put(INDEX_COLUMN, i++);
            data.put(LEVEL_COLUMN, e.getStatus().toString());
            data.put(MESSAGE_COLUMN, e.getMessage());

            result.put(new CompositeDataSupport(LOG_ROW_TYPE, data));
        }
        return result;
    }

    @Override
    public AttributeList getAttributes(final String[] attributes) {
        final AttributeList result = new AttributeList();
        if (attributes != null) {
            HealthCheckExecutionResult hcResult = null;
            for (final String key : attributes) {
                final Object defaultValue = this.defaultAttributes.get(key);
                if (defaultValue != null) {
                    result.add(new Attribute(key, defaultValue));
                } else {
                    // we assume that a valid attribute name is used
                    // which is requesting a hc result
                    if (hcResult == null) {
                        hcResult = this.getHealthCheckResult();
                    }

                    if (HC_OK_ATTRIBUTE_NAME.equals(key)) {
                        result.add(new Attribute(key, hcResult.getHealthCheckResult().isOk()));
                    } else if (HC_LOG_ATTRIBUTE_NAME.equals(key)) {
                        try {
                            result.add(new Attribute(key, logData(hcResult.getHealthCheckResult())));
                        } catch (final OpenDataException ignore) {
                            // we ignore this and simply don't add the attribute
                        }
                    } else if (HC_STATUS_ATTRIBUTE_NAME.equals(key)) {
                        result.add(new Attribute(key, hcResult.getHealthCheckResult().getStatus().toString()));
                    } else if (HC_ELAPSED_TIMED_ATTRIBUTE_NAME.equals(key)) {
                        result.add(new Attribute(key, hcResult.getElapsedTimeInMs()));
                    } else if (HC_FINISHED_AT_ATTRIBUTE_NAME.equals(key)) {
                        result.add(new Attribute(key, hcResult.getFinishedAt()));
                    } else if (HC_TIMED_OUT_ATTRIBUTE_NAME.equals(key)) {
                        result.add(new Attribute(key, hcResult.hasTimedOut()));
                    }
                }
            }
        }

        return result;
    }

    /** Create the mbean info */
    private MBeanInfo createMBeanInfo(final ServiceReference<HealthCheck> serviceReference) {
        final List<MBeanAttributeInfo> attrs = new ArrayList<MBeanAttributeInfo>();

        // add relevant service properties
        if (serviceReference.getProperty(HealthCheck.NAME) != null) {
            attrs.add(new MBeanAttributeInfo(HealthCheck.NAME, String.class.getName(), "The name of the health check service.", true, false,
                    false));
        }
        if (serviceReference.getProperty(HealthCheck.TAGS) != null) {
            attrs.add(new MBeanAttributeInfo(HealthCheck.TAGS, String.class.getName(), "The tags of the health check service.", true, false,
                    false));
        }

        // add standard attributes
        attrs.add(new MBeanAttributeInfo(HC_OK_ATTRIBUTE_NAME, Boolean.class.getName(), "The health check result", true, false, false));
        attrs.add(new MBeanAttributeInfo(HC_STATUS_ATTRIBUTE_NAME, String.class.getName(), "The health check status", true, false, false));
        attrs.add(new MBeanAttributeInfo(HC_ELAPSED_TIMED_ATTRIBUTE_NAME, Long.class.getName(), "The elapsed time in miliseconds", true,
                false, false));
        attrs.add(new MBeanAttributeInfo(HC_FINISHED_AT_ATTRIBUTE_NAME, Date.class.getName(), "The date when the execution finished", true,
                false, false));
        attrs.add(new MBeanAttributeInfo(HC_TIMED_OUT_ATTRIBUTE_NAME, Boolean.class.getName(), "Indicates of the execution timed out", true,
                false, false));
        attrs.add(new OpenMBeanAttributeInfoSupport(HC_LOG_ATTRIBUTE_NAME, "The health check result log", LOG_TABLE_TYPE, true, false,
                false));

        final String description;
        if (serviceReference.getProperty(Constants.SERVICE_DESCRIPTION) != null) {
            description = serviceReference.getProperty(Constants.SERVICE_DESCRIPTION).toString();
        } else {
            description = "Health check";
        }
        return new MBeanInfo(this.getClass().getName(),
                description,
                attrs.toArray(new MBeanAttributeInfo[attrs.size()]), null, null, null);
    }

    /** Create the default attributes. */
    private Map<String, Object> createDefaultAttributes(final ServiceReference<HealthCheck> serviceReference) {
        final Map<String, Object> list = new HashMap<String, Object>();
        if (serviceReference.getProperty(HealthCheck.NAME) != null) {
            list.put(HealthCheck.NAME, serviceReference.getProperty(HealthCheck.NAME).toString());
        }
        if (serviceReference.getProperty(HealthCheck.TAGS) != null) {
            final Object value = serviceReference.getProperty(HealthCheck.TAGS);
            if (value instanceof String[]) {
                list.put(HealthCheck.TAGS, Arrays.toString((String[]) value));
            } else {
                list.put(HealthCheck.TAGS, value.toString());
            }
        }

        return list;
    }

    @Override
    public MBeanInfo getMBeanInfo() {
        return this.mbeanInfo;
    }

    @Override
    public Object invoke(final String actionName, final Object[] params, final String[] signature)
            throws MBeanException, ReflectionException {
        throw new MBeanException(new UnsupportedOperationException(getClass().getSimpleName() + " does not support operations."));
    }

    @Override
    public void setAttribute(final Attribute attribute)
            throws AttributeNotFoundException, InvalidAttributeValueException,
            MBeanException, ReflectionException {
        throw new MBeanException(new UnsupportedOperationException(getClass().getSimpleName() + " does not support setting attributes."));
    }

    @Override
    public AttributeList setAttributes(final AttributeList attributes) {
        return new AttributeList();
    }

    @Override
    public String toString() {
        return "HealthCheckMBean [healthCheck=" + this.healthCheckRef + "]";
    }

    private HealthCheckExecutionResult getHealthCheckResult() {
        return this.executor.execute(this.healthCheckRef);
    }
}