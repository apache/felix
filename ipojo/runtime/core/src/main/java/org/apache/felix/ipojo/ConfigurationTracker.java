/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo;

import org.apache.felix.ipojo.extender.internal.Extender;
import org.apache.felix.ipojo.util.Log;
import org.apache.felix.ipojo.util.Logger;
import org.apache.felix.ipojo.util.ServiceLocator;
import org.osgi.framework.*;
import org.osgi.service.cm.*;
import org.osgi.service.cm.ConfigurationException;

import java.io.IOException;
import java.util.*;

/**
 * An object tracking configuration from the configuration admin. It delegates to the underlying factories or
 * component instance the action.
 * <p/>
 * This class implements a Configuration Listener, so events are received asynchronously.
 */
public class ConfigurationTracker implements ConfigurationListener {

    /**
     * The tracker instance.
     */
    private static ConfigurationTracker m_singleton;
    private final ServiceRegistration m_registration;
    private final BundleContext m_context;
    private final Logger m_logger;
    private Map<String, IPojoFactory> m_factories = new HashMap<String, IPojoFactory>();

    public ConfigurationTracker() {
        m_context = Extender.getIPOJOBundleContext();
        m_logger = new Logger(m_context, "iPOJO Configuration Admin listener", Log.INFO);
        // register as listener for configurations
        Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_DESCRIPTION, "iPOJO Configuration Admin Listener");
        props.put(Constants.SERVICE_VENDOR, "The Apache Software Foundation");
        m_registration = m_context.registerService(ConfigurationListener.class.getName(), this, props);
    }

    public static void initialize() {
        synchronized (ConfigurationTracker.class) {
            if (m_singleton == null) {
                m_singleton = new ConfigurationTracker();
            }
        }
    }

    public static void shutdown() {
        m_singleton.dispose();
        m_singleton = null;
    }

    public static ConfigurationTracker get() {
        return m_singleton;
    }

    /**
     * This method must be called by the iPOJO System itself, and only once.
     */
    public synchronized void dispose() {
        if (m_registration != null) {
            m_registration.unregister();
        }
        m_factories.clear();
    }

    public synchronized void registerFactory(IPojoFactory factory) {
        m_factories.put(factory.getFactoryName(), factory);

        ServiceLocator<ConfigurationAdmin> locator = new ServiceLocator<ConfigurationAdmin>(ConfigurationAdmin
                .class, m_context);
        final ConfigurationAdmin admin = locator.get();
        if (admin == null) {
            return;
        }

        List<Configuration> configurations = findFactoryConfiguration(admin, factory);
        for (Configuration configuration : configurations) {
            try {
                factory.updated(configuration.getPid(), configuration.getProperties());
            } catch (ConfigurationException e) {
                m_logger.log(Log.ERROR, "Cannot reconfigure instance " + configuration.getPid() + " from " +
                        configuration.getFactoryPid() + " with the configuration : " + configuration.getProperties(),
                        e);
            }
        }
    }

    public synchronized void instanceCreated(ComponentInstance instance) {
        ServiceLocator<ConfigurationAdmin> locator = new ServiceLocator<ConfigurationAdmin>(ConfigurationAdmin
                .class, m_context);
        final ConfigurationAdmin admin = locator.get();
        if (admin == null) {
            return;
        }

        Configuration configuration = findSingletonConfiguration(admin, instance.getInstanceName());
        if (configuration != null) {
            Hashtable<String, Object> conf = copyConfiguration(configuration);
            if (!conf.containsKey(Factory.INSTANCE_NAME_PROPERTY)) {
                conf.put(Factory.INSTANCE_NAME_PROPERTY, configuration.getPid());
            }
            try {
                instance.getFactory().reconfigure(conf);
            } catch (UnacceptableConfiguration unacceptableConfiguration) {
                m_logger.log(Log.ERROR, "Cannot reconfigure the instance " + configuration.getPid() + " - the " +
                        "configuration is unacceptable", unacceptableConfiguration);
            } catch (MissingHandlerException e) {
                m_logger.log(Log.ERROR, "Cannot reconfigure the instance " + configuration.getPid() + " - factory is " +
                        "invalid", e);
            }
        }
    }

    public synchronized void unregisterFactory(IPojoFactory factory) {
        m_factories.remove(factory.getFactoryName());
    }

    public void configurationEvent(ConfigurationEvent event) {
        String pid = event.getPid();
        String factoryPid = event.getFactoryPid();

        if (factoryPid == null) {
            ComponentInstance instance = retrieveInstance(pid);
            if (instance != null) {
                manageConfigurationEventForSingleton(instance, event);
            }
        } else {
            IPojoFactory factory = retrieveFactory(factoryPid);
            if (factory != null) {
                manageConfigurationEventForFactory(factory, event);
            }
            // Else the factory is unknown, do nothing.
        }

    }

    private void manageConfigurationEventForFactory(final IPojoFactory factory, final ConfigurationEvent event) {
        ServiceLocator<ConfigurationAdmin> locator = new ServiceLocator<ConfigurationAdmin>(ConfigurationAdmin
                .class, m_context);

        switch (event.getType()) {
            case ConfigurationEvent.CM_DELETED:
                factory.deleted(event.getPid());
                break;
            case ConfigurationEvent.CM_UPDATED:
                final ConfigurationAdmin admin = locator.get();
                if (admin == null) {
                    break;
                }
                final Configuration config = getConfiguration(admin, event.getPid(),
                        factory.getBundleContext().getBundle());
                if (config != null) {
                    try {
                        factory.updated(event.getPid(), config.getProperties());
                    } catch (org.osgi.service.cm.ConfigurationException e) {
                        m_logger.log(Log.ERROR, "Cannot reconfigure instance " + event.getPid() + " with the new " +
                                "configuration " + config.getProperties(), e);
                    }
                }
            default:
                // To nothing.
        }

        locator.unget();
    }

    private void manageConfigurationEventForSingleton(final ComponentInstance instance,
                                                      final ConfigurationEvent event) {
        ServiceLocator<ConfigurationAdmin> locator = new ServiceLocator<ConfigurationAdmin>(ConfigurationAdmin
                .class, m_context);

        switch (event.getType()) {
            case ConfigurationEvent.CM_DELETED:
                instance.dispose();
                break;
            case ConfigurationEvent.CM_UPDATED:
                final ConfigurationAdmin admin = locator.get();
                if (admin == null) {
                    break;
                }
                final Configuration config = getConfiguration(admin, event.getPid(),
                        instance.getFactory().getBundleContext().getBundle());
                if (config != null) {
                    Hashtable<String, Object> conf = copyConfiguration(config);
                    if (!conf.containsKey(Factory.INSTANCE_NAME_PROPERTY)) {
                        conf.put(Factory.INSTANCE_NAME_PROPERTY, event.getPid());
                    }
                    try {
                        instance.getFactory().reconfigure(conf);
                    } catch (UnacceptableConfiguration unacceptableConfiguration) {
                        m_logger.log(Log.ERROR, "Cannot reconfigure the instance " + event.getPid() + " - the " +
                                "configuration is unacceptable", unacceptableConfiguration);
                    } catch (MissingHandlerException e) {
                        m_logger.log(Log.ERROR, "Cannot reconfigure the instance " + event.getPid() + " - factory is " +
                                "invalid", e);
                    }
                }
            default:
                // To nothing.
        }

        locator.unget();
    }

    private Hashtable<String, Object> copyConfiguration(Configuration config) {
        Hashtable<String, Object> conf = new Hashtable<String, Object>();
        // Copy configuration
        Enumeration keys = config.getProperties().keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            conf.put(key, config.getProperties().get(key));
        }
        return conf;
    }

    private IPojoFactory retrieveFactory(String factoryPid) {
        synchronized (this) {
            return m_factories.get(factoryPid);
        }
    }

    private ComponentInstance retrieveInstance(String instanceName) {
        Collection<IPojoFactory> factories;
        synchronized (this) {
            factories = m_factories.values();
        }
        for (IPojoFactory factory : factories) {
            ComponentInstance instance = factory.getInstanceByName(instanceName);
            if (instance != null) {
                return instance;
            }
        }
        return null;
    }

    private Configuration getConfiguration(final ConfigurationAdmin admin, final String pid,
                                           final Bundle bundle) {
        if (admin == null) {
            return null;
        }

        try {
            // Even if it is possible, we don't build the filter with bundle.location to detect the case where the
            // configuration exists but can't be managed by iPOJO.
            final Configuration cfg = admin.getConfiguration(pid);
            final String bundleLocation = bundle.getLocation();
            if (cfg.getBundleLocation() == null || bundleLocation.equals(cfg.getBundleLocation())
                    || m_context.getBundle().getLocation().equals(cfg.getBundleLocation())) {
                cfg.setBundleLocation(bundleLocation);
                return cfg;
            }

            // Multi-location
            if (cfg.getBundleLocation().startsWith("?")) {
                if (bundle.hasPermission(new ConfigurationPermission(cfg.getBundleLocation(), "target"))) {
                    return cfg;
                }
            }

            // configuration belongs to another bundle, cannot be used here
            m_logger.log(Log.ERROR, "Cannot use configuration pid=" + pid + " for bundle "
                    + bundleLocation + " because it belongs to bundle " + cfg.getBundleLocation());
        } catch (IOException ioe) {
            m_logger.log(Log.WARNING, "Failed reading configuration for pid=" + pid, ioe);
        }

        return null;
    }

    public List<Configuration> findFactoryConfiguration(final ConfigurationAdmin admin, final IPojoFactory factory) {
        final String filter = "(service.factoryPid=" + factory.getFactoryName() + ")";
        return findConfigurations(admin, filter);
    }

    public Configuration findSingletonConfiguration(final ConfigurationAdmin admin, final String pid) {
        final String filter = "(service.pid=" + pid + ")";
        List<Configuration> list = findConfigurations(admin, filter);
        if (list.isEmpty()) {
            return null;
        } else {
            return list.get(0);
        }
    }

    private List<Configuration> findConfigurations(final ConfigurationAdmin admin, final String filter) {
        List<Configuration> configurations = Collections.emptyList();
        if (admin == null) {
            return configurations;
        }

        try {
            Configuration[] list = admin.listConfigurations(filter);
            if (list == null) {
                return configurations;
            } else {
                return Arrays.asList(list);
            }
        } catch (InvalidSyntaxException e) {
            m_logger.log(Log.ERROR, "Invalid Configuration selection filter " + filter, e);
        } catch (IOException e) {
            m_logger.log(Log.ERROR, "Error when retrieving configurations for filter=" + filter, e);
        }
        return configurations;
    }

}
