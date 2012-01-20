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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.felix.httplite.server.Server;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * HttpServiceFactory creates a separate HttpService instance for every requester, so that
 * the requester (client) bundle has access to it's own class loader for getting resources.
 *
 */
public class HttpServiceFactoryImpl implements ServiceFactory
{
    /**
     * Logger instance
     */
    private final Logger m_logger;
    /**
     * Socket server reference
     */
    private final Server m_server;
    /**
     * List of service registrations, both Resource and Servlet.
     */
    private List m_registrations;
	/**
	 * Map to store the servlet and resource registrations.
	 */
	private final HashMap m_servletMap;

    /**
     * @param logger
     * @param m_server
     */
    public HttpServiceFactoryImpl(final Logger logger, final Server m_server)
    {
        this.m_logger = logger;
        this.m_server = m_server;
        this.m_servletMap = new HashMap();
    }

    /* (non-Javadoc)
     * @see org.osgi.framework.ServiceFactory#getService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration)
     */
    public Object getService(final Bundle bundle, final ServiceRegistration registration)
    {
        HttpServiceImpl httpService = null;
        try
        {
            httpService = new HttpServiceImpl(bundle, m_server, m_logger, m_servletMap);

            if (m_server.getState() != Server.ACTIVE_STATE)
            {
                m_logger.log(Logger.LOG_INFO, "Starting http server.");
                httpService.start();
            }

            if (m_registrations == null)
            {
                m_registrations = new ArrayList();
            }

            m_registrations.add(serializeRegistration(bundle, registration));
        }
        catch (IOException e)
        {
            m_logger.log(Logger.LOG_ERROR, "Unable to create Http Service.", e);
            return null;
        }

        return httpService;
    }

    /**
     * Provide a unique string to represent a given registration.
     * 
     * @param bundle
     * @param registration
     * @return
     */
    private String serializeRegistration(final Bundle bundle,
        final ServiceRegistration registration)
    {
        return registration.toString() + bundle.getBundleId();
    }

    /* (non-Javadoc)
     * @see org.osgi.framework.ServiceFactory#ungetService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration, java.lang.Object)
     */
    public void ungetService(final Bundle bundle, final ServiceRegistration registration,
        final Object service)
    {
        if (m_registrations == null)
        {
            throw new IllegalStateException("m_registrations has not been initialized.");
        }

        String key = serializeRegistration(bundle, registration);

        if (!m_registrations.contains(key))
        {
            throw new IllegalStateException("Untracked service registration.");
        }

        m_registrations.remove(key);

        if (m_registrations.size() == 0 && m_server.getState() == Server.ACTIVE_STATE)
        {
            (new Thread(new Runnable()
            {                
                public void run()
                {
                    try
                    {
                        Thread.sleep( 1000 * 30 );
                        
                        if (m_registrations == null || m_server == null || Thread.interrupted())
                        {
                            return;
                        }
                        
                        if (m_registrations.size() == 0 && m_server.getState() == Server.ACTIVE_STATE) 
                        {
                            m_logger.log(Logger.LOG_INFO,
                                "Stopping http server since no clients are registered.");
                            m_server.setStopping();
                            m_server.stop();
                        }
                    }
                    catch (InterruptedException e)
                    {
                        return;
                    }
                    
                }
            })).start();            
        }
    }
}
