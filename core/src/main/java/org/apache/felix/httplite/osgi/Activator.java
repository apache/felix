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
package org.apache.felix.httplite.osgi;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.httplite.server.Server;
import org.apache.felix.httplite.servlet.HttpConstants;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;

/**
 * Activator for org.apache.felix.http.lightweight HTTP Service implementation.
 * 
 * The activator will read in system properties that are relevant to the service
 * and register the HttpService in the service registry.
 * 
**/
public class Activator implements BundleActivator
{
    /**
     * Felix-specific log level setting as system property.
     */
    private static final String FELIX_LOG_LEVEL = "felix.log.level";
    /**
     * HTTP Service registration.
     */
    private ServiceRegistration m_httpServiceReg;
    /**
     * Reference to socket server.
     */
    private Server m_server;

    /* (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     */
	public void start(BundleContext context) throws Exception
    {
        Logger logger = createLogger(context);

        //Set the internal logger's log level if specified.
        if (context.getProperty(FELIX_LOG_LEVEL) != null)
        {
            logger.setLogLevel(Integer.parseInt(context.getProperty(FELIX_LOG_LEVEL)));
        }

        //Only enable the HTTPService if the HTTP_ENABLE property is set or undefined.
        if (isPropertyTrue(context, Server.CONFIG_PROPERTY_HTTP_ENABLE, true))
        {
            Map config = createConfigMap(context);
            m_server = new Server(config, logger);
            m_httpServiceReg = context.registerService(HttpService.class.getName(),
                new HttpServiceFactoryImpl(logger, m_server),
                createHttpServiceProperties(Server.getConfiguredPort(config)));
        }

        //Warn user that HTTPS is not supported if it is specifically enabled.
        if (isPropertyTrue(context, Server.CONFIG_PROPERTY_HTTPS_ENABLE, false))
        {
            logger.log(Logger.LOG_WARNING, Server.CONFIG_PROPERTY_HTTPS_ENABLE
                + " is not implemented in this http service.");
        }
    }

    /**
     * Create a Dictionary intended to be used with Http Service registration.
     * 
     * @param port Port number to add to the properties.
     * @return A dictionary of OSGi service properties associate with the HTTP service.
     */
    private Dictionary createHttpServiceProperties(final int port)
    {
        Dictionary props = new Properties();

        props.put(HttpConstants.SERVICE_PROPERTY_KEY_HTTP_ENABLE, "true");
        props.put(HttpConstants.SERVICE_PROPERTY_KEY_HTTPS_ENABLE, "false");
        props.put(HttpConstants.SERVICE_PROPERTY_KEY_HTTP_PORT, Integer.toString(port));

        return props;
    }

    /**
     * Create a Map of configuration name/value pairs that the socket server requires to start.
     * 
     * @param context BundleContext
     * @return Map of configuration name/value pairs that the socket server requires to start.
     */
    private Map createConfigMap(final BundleContext context)
    {
        Map config = new HashMap();

        config.put(Server.CONFIG_PROPERTY_HTTP_PORT,
            context.getProperty(Server.CONFIG_PROPERTY_HTTP_PORT));
        config.put(Server.CONFIG_PROPERTY_HTTP_HOST,
            context.getProperty(Server.CONFIG_PROPERTY_HTTP_HOST));
        config.put(Server.CONFIG_PROPERTY_HTTP_ENABLE,
            context.getProperty(Server.CONFIG_PROPERTY_HTTP_ENABLE));
        config.put(Server.CONFIG_PROPERTY_HTTPS_ENABLE,
            context.getProperty(Server.CONFIG_PROPERTY_HTTPS_ENABLE));
        config.put(Server.CONFIG_PROPERTY_THREADPOOL_LIMIT_PROP,
            context.getProperty(Server.CONFIG_PROPERTY_THREADPOOL_LIMIT_PROP));
        config.put(Server.CONFIG_PROPERTY_THREADPOOL_TIMEOUT_PROP,
            context.getProperty(Server.CONFIG_PROPERTY_THREADPOOL_TIMEOUT_PROP));
        config.put(Server.CONFIG_PROPERTY_CONNECTION_REQUESTLIMIT_PROP,
            context.getProperty(Server.CONFIG_PROPERTY_CONNECTION_REQUESTLIMIT_PROP));
        config.put(Server.CONFIG_PROPERTY_CONNECTION_TIMEOUT_PROP,
            context.getProperty(Server.CONFIG_PROPERTY_CONNECTION_TIMEOUT_PROP));

        return config;
    }

    /**
     * @param context BundleContext
     * @return Logger instance
     */
    private Logger createLogger(final BundleContext context)
    {
        Logger logger = new Logger();
        logger.setSystemBundleContext(context);

        return logger;
    }

    /* (non-Javadoc)
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
	public void stop(final BundleContext context) throws Exception
    {
		m_server.setStopping();
		
        if (m_httpServiceReg != null)
        {
            m_httpServiceReg.unregister();
        }

        if (m_server.getState() == Server.ACTIVE_STATE)
        {
            m_server.stop();
        }
    }

    /**
     * Convenience method that returns true if Bundle property exists and is true, false if false, and defaultValue otherwise.
     * @param context BundleContext
     * @param name Property name
     * @param defaultValue default value for case that the key does not exist.
     * @return true if Bundle property exists and is true, false if false, and defaultValue otherwise.
     */
    private static boolean isPropertyTrue(final BundleContext context, final String name,
        final boolean defaultValue)
    {
        String value = context.getProperty(name);

        if (value == null)
        {
            return defaultValue;
        }

        return Boolean.valueOf(value).booleanValue();
    }
}
