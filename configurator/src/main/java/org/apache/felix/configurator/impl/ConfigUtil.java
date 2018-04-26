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
package org.apache.felix.configurator.impl;

import java.io.IOException;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Utilities for configuration handling
 */
public abstract class ConfigUtil {

    /**
     * Encode the value for the LDAP filter: \, *, (, and ) should be escaped.
     */
    private static String encode(final String value) {
        return value.replace("\\", "\\\\")
                .replace("*", "\\*")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    /**
     * Get or create a configuration
     * @param ca  The configuration admin
     * @param pid The pid
     * @param createIfNeeded If {@code true} the configuration is created if it doesn't exists
     * @return The configuration or {@code null}.
     * @throws IOException If anything goes wrong
     * @throws InvalidSyntaxException If the filter syntax is invalid (very unlikely)
     */
    public static Configuration getOrCreateConfiguration(final ConfigurationAdmin ca,
            final String pid,
            final boolean createIfNeeded)
                    throws IOException, InvalidSyntaxException {
        final String filter = "(" + Constants.SERVICE_PID + "=" + encode(pid) + ")";
        final Configuration[] configs = ca.listConfigurations(filter);
        if (configs != null && configs.length > 0) {
            return configs[0];
        }
        if ( !createIfNeeded ) {
            return null;
        }

        final int pos = pid.indexOf('~');
        if ( pos != -1 ) {
            final String factoryPid = pid.substring(0, pos);
            final String alias = pid.substring(pos + 1);

            return ca.getFactoryConfiguration(factoryPid, alias, "?");
        } else {
            return ca.getConfiguration(pid, "?");
        }
    }
}
