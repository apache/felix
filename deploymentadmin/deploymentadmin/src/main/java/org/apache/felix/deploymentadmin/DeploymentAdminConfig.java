/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.deploymentadmin;

import java.util.Dictionary;

import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.deploymentadmin.DeploymentAdmin;

/**
 * Provides the configuration options for this implementation of {@link DeploymentAdmin}.
 */
public class DeploymentAdminConfig {
    /** Configuration key used to stop only bundles mentioned in a DP instead of all bundles. */
    static final String KEY_STOP_UNAFFECTED_BUNDLE = "stopUnaffectedBundle";
    /** Configuration key used to allow usage of customizers outside a DP. */
    static final String KEY_ALLOW_FOREIGN_CUSTOMIZERS = "allowForeignCustomizers";

    static final boolean DEFAULT_STOP_UNAFFECTED_BUNDLE = true;
    static final boolean DEFAULT_ALLOW_FOREIGN_CUSTOMIZERS = false;

    private final BundleContext m_context;
    private final Boolean m_stopUnaffectedBundles;
    private final Boolean m_allowForeignCustomizers;

    /**
     * Creates a new {@link DeploymentAdminConfig} instance with the default settings.
     */
    public DeploymentAdminConfig(BundleContext context) {
        m_context = context;
        m_stopUnaffectedBundles = null;
        m_allowForeignCustomizers = null;
    }

    /**
     * Creates a new {@link DeploymentAdminConfig} instance with the given configuration properties.
     */
    public DeploymentAdminConfig(BundleContext context, Dictionary properties) throws ConfigurationException {
        Boolean stopUnaffectedBundles = null;
        Boolean allowForeignCustomizers = null;

        if (properties != null) {
            stopUnaffectedBundles = getMandatoryValue(properties, KEY_STOP_UNAFFECTED_BUNDLE);
            allowForeignCustomizers = getMandatoryValue(properties, KEY_ALLOW_FOREIGN_CUSTOMIZERS);
        }

        m_context = context;
        m_stopUnaffectedBundles = stopUnaffectedBundles;
        m_allowForeignCustomizers = allowForeignCustomizers;
    }

    /**
     * Creates a new {@link DeploymentAdminConfig} instance as copy of the given configuration.
     */
    public DeploymentAdminConfig(DeploymentAdminConfig configuration) {
        m_context = configuration.m_context;
        m_stopUnaffectedBundles = configuration.m_stopUnaffectedBundles;
        m_allowForeignCustomizers = configuration.m_allowForeignCustomizers;
    }

    private static Boolean getMandatoryValue(Dictionary dict, String key) throws ConfigurationException {
        Object value = dict.get(key);
        if (value == null || !(value instanceof String || value instanceof Boolean)) {
            throw new ConfigurationException(key, "missing or invalid value!");
        }
        return parseBoolean(value);
    }

    private static Boolean parseBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return value != null ? Boolean.valueOf(value.toString()) : null;
    }

    /**
     * @return <code>true</code> if foreign customizers (that are not part of a DP) are allowed, <code>false</code> if all customizers should be provided by this or an earlier DP.
     */
    public boolean isAllowForeignCustomizers() {
        Boolean result = m_allowForeignCustomizers;
        if (result == null) {
            String prop = getFrameworkProperty(KEY_ALLOW_FOREIGN_CUSTOMIZERS);
            if (prop != null) {
                result = Boolean.valueOf(prop);
            }
        }
        return (result == null) ? DEFAULT_ALLOW_FOREIGN_CUSTOMIZERS : result.booleanValue();
    }

    /**
     * @return <code>true</code> if all bundles should be stopped during the installation of a DP, <code>false</code> if only affected bundles should be stopped.
     */
    public boolean isStopUnaffectedBundles() {
        Boolean result = m_stopUnaffectedBundles;
        if (result == null) {
            String prop = getFrameworkProperty(KEY_STOP_UNAFFECTED_BUNDLE);
            if (prop != null) {
                result = Boolean.valueOf(prop);
            }
        }
        return (result == null) ? DEFAULT_STOP_UNAFFECTED_BUNDLE : result.booleanValue();
    }

    private String getFrameworkProperty(String key) {
        String prop = m_context.getProperty(DeploymentAdminImpl.PID + "." + key);
        if (prop == null) {
            prop = m_context.getProperty(DeploymentAdminImpl.PID + "." + key.toLowerCase());
        }
        return prop;
    }
}
