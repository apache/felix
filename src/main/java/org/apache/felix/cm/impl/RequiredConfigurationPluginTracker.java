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
package org.apache.felix.cm.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationPlugin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * This tracker tracks existence of configuration plugins which have a
 * {@link #PROPERTY_NAME} property. The tracker checks just for existence of a
 * plugin and tries to get it. However, this is not a guarantee that the plugin
 * can and actually is used by the configuration admin as the admin has a
 * separate tracker.
 */
public class RequiredConfigurationPluginTracker
        implements ServiceTrackerCustomizer<ConfigurationPlugin, ConfigurationPlugin> {

    public static final String PROPERTY_NAME = "config.plugin.id";

    /** Tracker for the configuration plugins. */
    private final ServiceTracker<ConfigurationPlugin, ConfigurationPlugin> pluginTracker;

    private final BundleContext bundleContext;

    private final Map<String, AtomicInteger> serviceMap = new HashMap<>();

    private final Map<Long, String> idToNameMap = new ConcurrentHashMap<>();

    private final ConfigurationAdminStarter starter;

    public RequiredConfigurationPluginTracker(final BundleContext bundleContext, final ActivatorWorkerQueue workerQueue,
            final ConfigurationAdminStarter starter,
            final String[] pluginNames) throws BundleException, InvalidSyntaxException {
        this.starter = starter;
        for (final String name : pluginNames) {
            serviceMap.put(name, new AtomicInteger(0));
        }
        this.bundleContext = bundleContext;
        pluginTracker = new ServiceTracker<>(bundleContext, ConfigurationPlugin.class, this);
        pluginTracker.open();
    }

    /**
     * Stop the tracker
     */
    public void stop() {
        if (this.pluginTracker != null) {
            this.pluginTracker.close();
        }
    }

    private boolean hasRequiredPlugins() {
        int pluginCount = 0;
        for (final AtomicInteger entry : this.serviceMap.values()) {
            if (entry.get() > 0) {
                pluginCount++;
            }
        }
        return pluginCount == this.serviceMap.size();
    }

    @Override
    public ConfigurationPlugin addingService(final ServiceReference<ConfigurationPlugin> reference) {
        final Object nameObj = reference.getProperty(PROPERTY_NAME);
        if (nameObj != null) {
            final String name = nameObj.toString();
            final AtomicInteger counter = this.serviceMap.get(name);
            if (counter != null) {
                final ConfigurationPlugin plugin = bundleContext.getService(reference);
                if (plugin != null) {
                    final boolean checkActivate = counter.getAndIncrement() == 0;
                    idToNameMap.put((Long) reference.getProperty(Constants.SERVICE_ID), name);
                    if (checkActivate && hasRequiredPlugins()) {
                        starter.updatePluginsSet(true);
                    }
                    return plugin;
                }
            }
        }
        return null;
    }

    @Override
    public void modifiedService(final ServiceReference<ConfigurationPlugin> reference,
            ConfigurationPlugin service) {
        removedService(reference, service);
        addingService(reference);
    }

    @Override
    public void removedService(final ServiceReference<ConfigurationPlugin> reference,
            ConfigurationPlugin service) {
        final String name = idToNameMap.remove(reference.getProperty(Constants.SERVICE_ID));
        if (name != null) {
            final AtomicInteger counter = this.serviceMap.get(name);
            if (counter != null) {
                bundleContext.ungetService(reference);
                final boolean deactivate = counter.decrementAndGet() == 0;
                if (deactivate) {
                    starter.updatePluginsSet(false);
                }
            }
        }
    }
}
