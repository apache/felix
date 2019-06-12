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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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

    private final ConcurrentHashMap<String, AtomicInteger> serviceMap = new ConcurrentHashMap<>();

    private final Map<Long, String> idToNameMap = new ConcurrentHashMap<>();

    private final ConfigurationAdminStarter starter;

    private final Set<String> requiredNames = new HashSet<>();

    private final Set<String> registeredPluginNames = new TreeSet<>();

    private final ActivatorWorkerQueue workerQueue;

    public RequiredConfigurationPluginTracker(final BundleContext bundleContext,
            final ActivatorWorkerQueue workerQueue,
            final ConfigurationAdminStarter starter,
            final String[] pluginNames) throws BundleException, InvalidSyntaxException {
        this.workerQueue = workerQueue;
        this.starter = starter;
        for (final String name : pluginNames) {
            requiredNames.add(name);
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
        for (final String name : this.requiredNames) {
            final AtomicInteger v = this.serviceMap.get(name);
            if (v == null || v.get() == 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ConfigurationPlugin addingService(final ServiceReference<ConfigurationPlugin> reference) {
        ConfigurationPlugin plugin = null;
        final Object nameObj = reference.getProperty(PROPERTY_NAME);
        if (nameObj != null) {
            final String name = nameObj.toString();
            idToNameMap.put((Long) reference.getProperty(Constants.SERVICE_ID), name);
            this.serviceMap.putIfAbsent(name, new AtomicInteger());
            final AtomicInteger counter = this.serviceMap.get(name);
            final boolean checkActivate = counter.getAndIncrement() == 0;
            boolean activate = false;
            if (this.requiredNames.contains(name)) {
                plugin = bundleContext.getService(reference);
                if (plugin != null) {
                    activate = checkActivate && hasRequiredPlugins();
                }
            }
            final boolean activateCA = activate;
            this.workerQueue.enqueue(new Runnable() {

                @Override
                public void run() {
                    if (activateCA) {
                        starter.updatePluginsSet(true);
                    }
                    registeredPluginNames.add(name);
                    updateRegisteredConfigurationPlugins();
                }
            });
        }
        return plugin;
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
            final boolean deactivate = counter.decrementAndGet() == 0;
            if (this.requiredNames.contains(name)) {
                bundleContext.ungetService(reference);
            }
            if (deactivate) {
                this.workerQueue.enqueue(new Runnable() {

                    @Override
                    public void run() {
                        if (!hasRequiredPlugins()) {
                            starter.updatePluginsSet(false);
                        }
                        registeredPluginNames.remove(name);
                        updateRegisteredConfigurationPlugins();
                    }
                });
            }
        }
    }

    private void updateRegisteredConfigurationPlugins() {
        final String propValue;
        if (this.registeredPluginNames.isEmpty()) {
            propValue = "";
        } else {
            final StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (final String name : this.registeredPluginNames) {
                if (first) {
                    first = false;
                } else {
                    sb.append(",");
                }
                sb.append(name);
            }
            propValue = sb.toString();
        }
        starter.updateRegisteredConfigurationPlugins(propValue);
    }

}
