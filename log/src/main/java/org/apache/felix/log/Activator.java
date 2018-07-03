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

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;
import org.osgi.service.log.LoggerFactory;
import org.osgi.service.log.admin.LoggerAdmin;
import org.osgi.service.log.admin.LoggerContext;

/**
 * The bundle activator for the OSGi log service (see section 101 of the service
 * compendium).
 * <p>
 * The log service provides a general purpose message logger for the OSGi service
 * platform.  It consists of two services, one for logging information and another
 * for retrieving current or previously recorded log information.
 * <p>
 * The service knows about the following properties which are read at bundle
 * startup:
 * <dl>
 *   <dt>org.apache.felix.log.maxSize</dt>
 *   <dd>Determines the maximum size of the log used to maintain historic
 *       log information.  A value of -1 means the log has no maximum size;
 *       a value of 0 means that no historic log information will be maintained.
 *       The default value is 100.</dd>
 *
 *   <dt>org.apache.felix.log.storeDebug</dt>
 *   <dd>Determines whether or not debug messages will be stored as part of
 *       the historic log information. The default value is false.</dd>
 * </dl>
 */
public final class Activator implements BundleActivator
{
    /** The name of the property that defines the maximum size of the log. */
    private static final String MAX_SIZE_PROPERTY = "org.apache.felix.log.maxSize";
    /** The default value for the maximum size property. */
    private static final int DEFAULT_MAX_SIZE = 100;
    /** The name of the property that defines whether debug messages are stored. */
    private static final String STORE_DEBUG_PROPERTY = "org.apache.felix.log.storeDebug";
    /** The default value for the store debug property. */
    private static final boolean DEFAULT_STORE_DEBUG = false;
    /** The log. */
    private Log m_log;
    /** The LoggerAdmin. */
    private LoggerAdminImpl m_loggerAdmin;
    /** The Configuration listener. */
    private ConfigurationListenerImpl m_configurationListener;

    /**
     * Returns the maximum size for the log.
     * @param context the bundle context (used to look up a property)
     * @return the maximum size for the log
     */
    private static int getMaxSize(final BundleContext context)
    {
        int maxSize = DEFAULT_MAX_SIZE;

        String maxSizePropValue = context.getProperty(MAX_SIZE_PROPERTY);
        if (maxSizePropValue != null)
        {
            try
            {
                maxSize = Integer.parseInt(maxSizePropValue);
            }
            catch (NumberFormatException e)
            {
                // the property value is invalid - ignore
            }
        }

        return maxSize;
    }

    /**
     * Returns whether or not to store debug messages.
     * @param context the bundle context (used to look up a property)
     * @return whether or not to store debug messages
     */
    private static boolean getStoreDebug(final BundleContext context)
    {
        boolean storeDebug = DEFAULT_STORE_DEBUG;

        String storeDebugPropValue = context.getProperty(STORE_DEBUG_PROPERTY);
        if (storeDebugPropValue != null)
        {
            storeDebug = Boolean.valueOf(storeDebugPropValue).booleanValue();
        }

        return storeDebug;
    }

    /**
     * Return the default log level.
     * @param context
     * @return the default log level
     */
    private static String getDefaultLogLevel(final BundleContext context) {
        return context.getProperty(LoggerContext.LOGGER_CONTEXT_DEFAULT_LOGLEVEL);
    }

    /**
     * Called by the OSGi framework when the bundle is started.
     * Used to register the service implementations with the framework.
     * @param context the bundle context
     * @throws Exception if an error occurs
     */
    public void start(final BundleContext context) throws Exception
    {
        // create the log instance
        m_log = new Log(getMaxSize(context), getStoreDebug(context));
        // create the LoggerAdmin instance
        m_loggerAdmin = new LoggerAdminImpl(getDefaultLogLevel(context), m_log);

        // register the listeners
        context.addBundleListener(m_log);
        context.addFrameworkListener(m_log);
        context.addServiceListener(m_log);

        // register the services with the framework
        ServiceRegistration<?> serviceRegistration = context.registerService(
            new String[] {LogService.class.getName(), LoggerFactory.class.getName()},
            new LogServiceFactory(m_loggerAdmin), null);

        context.registerService(LogReaderService.class.getName(),
            new LogReaderServiceFactory(m_log), null);

        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(
            LoggerAdmin.LOG_SERVICE_ID,
            serviceRegistration.getReference().getProperty(Constants.SERVICE_ID));
        context.registerService(LoggerAdmin.class.getName(), m_loggerAdmin, properties);

        try {
            m_configurationListener = new ConfigurationListenerImpl(context, m_log, m_loggerAdmin);
        } catch (Exception e) {
            m_log.log(getClass().getName(), context.getBundle(), null, LogLevel.ERROR, "An error occured while setting up the configuration listener.", e);
        }
    }

    /**
     * Called by the OSGi framework when the bundle is stopped.
     * @param context the bundle context
     * @throws Exception if an error occurs
     */
    public void stop(final BundleContext context) throws Exception
    {
        // close the configuration listener
        if (m_configurationListener != null) {
            m_configurationListener.close();
        }
        // close the log
        m_log.close();
    }

}