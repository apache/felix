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
package org.apache.felix.log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.log.LogLevel;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class ConfigurationListenerImpl {
    private static final String CONFIGURATION_CLASS = "org.osgi.service.cm.Configuration";
    private static final String CONFIGURATION_ADMIN_CLASS = "org.osgi.service.cm.ConfigurationAdmin";
    private static final String CONFIGURATION_EVENT_CLASS = "org.osgi.service.cm.ConfigurationEvent";
    private static final String CONFIGURATION_LISTENER_CLASS = "org.osgi.service.cm.ConfigurationListener";

    /** ConfigurationAdmin tracker */
    final ServiceTracker<?, ?> m_cmtracker;
    final BundleContext m_context;
    final Log m_log;
    final LoggerAdminImpl m_loggerAdmin;

    public ConfigurationListenerImpl(final BundleContext context, final Log log, final LoggerAdminImpl loggerAdmin) throws Exception {
        m_context = context;
        m_log = log;
        m_loggerAdmin = loggerAdmin;

        Filter filter = context.createFilter(String.format("(%s=%s)", Constants.OBJECTCLASS, CONFIGURATION_ADMIN_CLASS));

        m_cmtracker = new ServiceTracker<>(m_context, filter, new CLCustomizer());
        m_cmtracker.open();
    }

    public void close() {
        m_cmtracker.close();
    }

    class CLProxy {
        private final Object m_ca;
        private final ServiceReference<?> m_caReference;
        private final ServiceRegistration<?> m_registration;
        private final Class<?> m_caClass;
        private final Class<?> m_ceClass;
        private final Class<?> m_clClass;
        private final Class<?> m_configurationClass;
        private final Method m_caGetConfiguration;
        private final Method m_caListConfigurations;
        private final Method m_ceGetPid;
        private final Method m_ceGetType;
        private final Method m_clConfigurationEvent;
        private final Method m_configurationGetProperties;
        private final Method m_configurationGetProcessedProperties;

        public CLProxy(ServiceReference<?> caReference) {
            m_caReference = caReference;
            m_ca = m_context.getService(m_caReference);

            try {
                m_caClass = m_caReference.getBundle().loadClass(CONFIGURATION_ADMIN_CLASS);
                m_caGetConfiguration = m_caClass.getMethod("getConfiguration", String.class, String.class);
                m_caListConfigurations = m_caClass.getMethod("listConfigurations", String.class);

                m_configurationClass = m_caReference.getBundle().loadClass(CONFIGURATION_CLASS);
                m_configurationGetProperties = m_configurationClass.getMethod("getProperties");
                Method configurationGetProcessedProperties = null;
                try {
                    // try for the 1.6 method
                    configurationGetProcessedProperties = m_configurationClass.getMethod("getProcessedProperties", ServiceReference.class);
                }
                catch (NoSuchMethodException nsme) {
                    // ignore
                }
                m_configurationGetProcessedProperties = configurationGetProcessedProperties;

                m_ceClass = m_caReference.getBundle().loadClass(CONFIGURATION_EVENT_CLASS);
                m_ceGetPid = m_ceClass.getMethod("getPid");
                m_ceGetType = m_ceClass.getMethod("getType");

                m_clClass = m_caReference.getBundle().loadClass(CONFIGURATION_LISTENER_CLASS);
                m_clConfigurationEvent = m_clClass.getMethod("configurationEvent", m_ceClass);
            }
            catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
                throw new IllegalStateException(
                    "Failure reflecting over API from Configuration Admin service bundle", e);
            }

            m_registration = m_context.registerService(CONFIGURATION_LISTENER_CLASS, configurationListenerProxy(), null);
        }

        private Object configurationListenerProxy() {
            ClassLoader classLoader = m_caReference.getBundle().adapt(BundleWiring.class).getClassLoader();

            return Proxy.newProxyInstance(
                classLoader, new Class<?>[] {m_clClass},
                new InvocationHandler() {

                    @SuppressWarnings("unchecked")
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.equals(m_clConfigurationEvent)) {
                            String pid = (String)m_ceGetPid.invoke(args[0]);
                            String configName = null;
                            String location = "?";
                            if (pid.startsWith("org.osgi.service.log.admin|")) {
                                configName = pid.substring("org.osgi.service.log.admin|".length());
                                if (configName.contains("|") && (configName.split("|").length == 3)) {
                                    String[] parts = configName.split("|");
                                    location = parts[2];
                                }
                            }

                            switch ((int)m_ceGetType.invoke(args[0])) {
                                case 2: // CM_DELETED
                                    m_loggerAdmin.updateConfiguration(configName, null);
                                    break;
                                default:
                                    Object configObj = m_caGetConfiguration.invoke(m_ca, pid, location);
                                    Object propertiesObj = getProperties(configObj);
                                    m_loggerAdmin.updateConfiguration(configName, (Dictionary<String, Object>)propertiesObj);
                            }
                        }

                        return null;
                    }
                }
            );
        }

        @SuppressWarnings("unchecked")
        private Dictionary<String, Object> getProperties(Object configObj) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            if (m_configurationGetProcessedProperties != null) {
                return (Dictionary<String, Object>) m_configurationGetProcessedProperties.invoke(configObj, m_caReference);
            }
            return (Dictionary<String, Object>) m_configurationGetProperties.invoke(configObj);
        }

        @SuppressWarnings("rawtypes")
        private List<Dictionary> listConfigurations(String filter) {
            try {
                Object result = m_caListConfigurations.invoke(m_ca, filter);
                if (result != null) {
                    List<Dictionary> dictionaries = new ArrayList<>();
                    for (Object configObj : (Object[])result) {
                        dictionaries.add(getProperties(configObj));
                    }
                    return dictionaries;
                }
            }
            catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                if (e instanceof InvocationTargetException) {
                    m_log.log(getClass().getName(), m_context.getBundle(), null, LogLevel.ERROR, "An error occured reflecting on ConfigurationAdmin.", ((InvocationTargetException)e).getTargetException());
                }
                else {
                    m_log.log(getClass().getName(), m_context.getBundle(), null, LogLevel.ERROR, "An error occured reflecting on ConfigurationAdmin.", e);
                }
            }
            return Collections.emptyList();
        }

        private void unregister() {
            m_registration.unregister();
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private void updateContext(String name, String pid, boolean delete) {
            List<Dictionary> configurations = listConfigurations(String.format("(%s=%s)", Constants.SERVICE_PID, pid));

            if (!configurations.isEmpty()) {
                m_loggerAdmin.updateConfiguration(name, delete ? null : configurations.get(0));
            }
        }

    }

    class CLCustomizer implements ServiceTrackerCustomizer<Object, CLProxy> {

        @Override
        public CLProxy addingService(ServiceReference<Object> reference) {
            CLProxy clProxy = new CLProxy(reference);

            // configure ROOT context
            clProxy.updateContext(null, "org.osgi.service.log.admin", false);

            // configure bundle contexts
            for (String name : m_loggerAdmin.getLoggerContextNames()) {
                String pid = "org.osgi.service.log.admin|" + name;
                clProxy.updateContext(name, pid, false);
            }

            return clProxy;
        }

        @Override
        public void modifiedService(ServiceReference<Object> reference, CLProxy clProxy) {
        }

        @Override
        public void removedService(ServiceReference<Object> reference, CLProxy clProxy) {
            // un-configure bundle contexts
            for (String name : m_loggerAdmin.getLoggerContextNames()) {
                String pid = "org.osgi.service.log.admin|" + name;
                clProxy.updateContext(name, pid, true);
            }

            // un-configure ROOT context
            clProxy.updateContext(null, "org.osgi.service.log.admin", true);

            // un-register the configuration listener
            clProxy.unregister();
        }

    }

}
