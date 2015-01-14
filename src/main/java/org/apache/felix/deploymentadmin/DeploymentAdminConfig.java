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

import org.osgi.framework.BundleContext;

/**
 * Provides the configuration options for this DeploymentAdmin implementation.
 */
public class DeploymentAdminConfig {
    /** Prefix used for the configuration properties of DA. */
    private static final String PREFIX = "org.apache.felix.deploymentadmin.";

    /** 
     * Configuration key used to stop only bundles mentioned in a DP instead of all bundles.
     * @deprecated incorrect name append the 's'
     */
    static final String KEY_STOP_UNAFFECTED_BUNDLE = PREFIX.concat("stopUnaffectedBundle");
    /** Configuration key used to stop only bundles mentioned in a DP instead of all bundles. */
    static final String KEY_STOP_UNAFFECTED_BUNDLES = PREFIX.concat("stopUnaffectedBundles");
    /** Configuration key used to allow usage of customizers outside a DP. */
    static final String KEY_ALLOW_FOREIGN_CUSTOMIZERS = PREFIX.concat("allowForeignCustomizers");

    static final boolean DEFAULT_STOP_UNAFFECTED_BUNDLES = true;
    static final boolean DEFAULT_ALLOW_FOREIGN_CUSTOMIZERS = false;

    private final boolean m_stopUnaffectedBundles;
    private final boolean m_allowForeignCustomizers;

    /**
     * Creates a new {@link DeploymentAdminConfig} instance with the default settings.
     */
    public DeploymentAdminConfig(BundleContext context) {
        // Allow the constant to be used in singular or plural form...
        String value = getFrameworkProperty(context, KEY_STOP_UNAFFECTED_BUNDLE);
        if (value == null) {
            value = getFrameworkProperty(context, KEY_STOP_UNAFFECTED_BUNDLES);
        }
        m_stopUnaffectedBundles = parseBoolean(value, DEFAULT_STOP_UNAFFECTED_BUNDLES);

        value = getFrameworkProperty(context, KEY_ALLOW_FOREIGN_CUSTOMIZERS);
        m_allowForeignCustomizers = parseBoolean(value, DEFAULT_ALLOW_FOREIGN_CUSTOMIZERS);
    }

    /**
     * @return <code>true</code> if foreign customizers (that are not part of a DP) are allowed, <code>false</code> if
     *         all customizers should be provided by this or an earlier DP.
     */
    public boolean isAllowForeignCustomizers() {
        return m_allowForeignCustomizers;
    }

    /**
     * @return <code>true</code> if all bundles should be stopped during the installation of a DP, <code>false</code> if
     *         only affected bundles should be stopped.
     */
    public boolean isStopUnaffectedBundles() {
        return m_stopUnaffectedBundles;
    }

    private static boolean parseBoolean(String value, boolean dflt) {
        if (value == null || "".equals(value.trim())) {
            return dflt;
        }
        return Boolean.parseBoolean(value);
    }

    private static String getFrameworkProperty(BundleContext context, String key) {
        String prop = context.getProperty(key);
        if (prop == null) {
            // be lenient wrt the naming...
            prop = context.getProperty(key.toLowerCase());
        }
        return prop;
    }
}
