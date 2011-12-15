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
import java.io.OutputStream;
import java.net.Socket;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.felix.httplite.server.ResourceHandler;
import org.apache.felix.httplite.server.Server;
import org.apache.felix.httplite.server.ServletHandler;
import org.apache.felix.httplite.servlet.HttpServletRequestImpl;
import org.apache.felix.httplite.servlet.HttpServletResponseImpl;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

/**
 * The HTTP Service implementation that also implements RegistrationResolver to
 * provide internal server classes with OSGi service registration data.
 */
public class HttpServiceImpl implements HttpService, ServiceRegistrationResolver
{

    /**
     * Socket server reference.
     */
    private final Server m_server;
    /**
     * Map of registered servlets.
     */
    private final Map m_servletMap;
    /**
     * Logger reference.
     */
    private final Logger m_logger;
    /**
     * Client bundle reference.
     */
    private final Bundle m_bundle;

    /**
     * @param server
     *            Map of <String, String> of configuration properties for the
     *            HTTP server.
     * @param bundle
     *            Bundle that registered with the service
     * @param logger
     *            instance of Logger
     * @param servletMap Map of servlet instances.
     * @throws IOException
     */
    public HttpServiceImpl(final Bundle bundle, final Server server, final Logger logger, Map servletMap) throws IOException
    {
        this.m_bundle = bundle;
        this.m_logger = logger;
        this.m_server = server;
        this.m_servletMap = servletMap;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.osgi.service.http.HttpService#registerResources(java.lang.String,
     * java.lang.String, org.osgi.service.http.HttpContext)
     */
    public void registerResources(final String alias, final String name,
        final HttpContext context) throws NamespaceException
    {
        validateAlias(alias);
        
        synchronized (m_servletMap)
        {
            if (m_servletMap.containsKey(alias))
            {
                throw new NamespaceException("Alias " + alias
                    + " has already been registered.");
            }
        }

        if (context == null)
        {
            m_servletMap.put(alias, new ServiceRegistration(alias, name,
                createDefaultHttpContext(), m_logger));
        }
        else
        {
            m_servletMap.put(alias, new ServiceRegistration(alias, name, context,
                m_logger));
        }

        m_logger.log(Logger.LOG_DEBUG, "Registered resource for alias: " + alias);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.http.HttpService#unregister(java.lang.String)
     */
    public void unregister(String alias)
    {
        ServiceRegistration reg = null;
        synchronized (m_servletMap)
        {
            reg = (ServiceRegistration) m_servletMap.get(alias);
            if (reg != null)
            {
                m_servletMap.remove(alias);
                m_logger.log(Logger.LOG_DEBUG, "Unregistered resource for alias: "
                    + alias);
            }
        }

        if (reg != null && reg.isServlet() && reg.hasBeenInitialized())
        {
            reg.getServlet().destroy();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.http.HttpService#createDefaultHttpContext()
     */
    public HttpContext createDefaultHttpContext()
    {
        return new DefaultContextImpl(m_bundle);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.service.http.HttpService#registerServlet(java.lang.String,
     * javax.servlet.Servlet, java.util.Dictionary,
     * org.osgi.service.http.HttpContext)
     */
    public void registerServlet(final String alias, final Servlet servlet,
        final Dictionary initparams, final HttpContext context) throws ServletException,
        NamespaceException
    {
        validateAlias(alias);
        
        if (m_servletMap.containsKey(alias))
        {
            throw new NamespaceException("Alias " + alias
                + " has already been registered.");
        }

        if (context == null)
        {
            m_servletMap.put(alias, new ServiceRegistration(alias, servlet, initparams,
                new DefaultContextImpl(m_bundle), m_logger));
        }
        else
        {
            m_servletMap.put(alias, new ServiceRegistration(alias, servlet, initparams,
                context, m_logger));
        }

        m_logger.log(Logger.LOG_DEBUG, "Registered servlet for alias: " + alias);
    }

    /**
     * Start the HTTP server.
     * 
     * @throws IOException on I/O error
     */
    protected final void start() throws IOException
    {
        if (m_server.getState() != Server.INACTIVE_STATE)
        {
            throw new IllegalStateException("Attempted to start already-running server.");
        }

        m_server.start(this);
    }

    /**
     * Stop the HTTP server
     * 
     * @throws InterruptedException on thread interruption.
     */
    protected final void stop() throws InterruptedException
    {
        if (m_server.getState() != Server.ACTIVE_STATE)
        {
            throw new IllegalStateException("Attempted to stop an inactive server.");
        }

        for (Iterator i = m_servletMap.values().iterator(); i.hasNext();)
        {
            ServiceRegistration sr = (ServiceRegistration) i.next();

            try
            {
                m_logger.log(Logger.LOG_DEBUG, "Cleaning up servlet " + sr.getAlias());
                sr.getServlet().destroy();
            }
            catch (Exception e)
            {
                m_logger.log(Logger.LOG_ERROR,
                    "Servlet threw exception during destroy(): " + sr.getAlias());
            }
        }

        m_server.stop();
    }

    /**
     * Iterate through all service registrations and return the registration
     * which matches the longest alias, or null if no matches are found.
     * 
     * TODO: consider caching if a lot of time is spent resolving registrations.
     * 
     * @param requestPath the URI of the request
     * @return the service registration with the deepest match to the request
     *         path.
     */
    public final ServiceRegistration getServiceRegistration(final String requestPath)
    {
        ServiceRegistration sr = null;
        int maxLength = 0;
        synchronized (m_servletMap)
        {
            for (Iterator i = m_servletMap.keySet().iterator(); i.hasNext();)
            {
                String alias = (String) i.next();

                if (requestPath.startsWith(alias))
                {
                    if (sr == null)
                    {
                        sr = (ServiceRegistration) m_servletMap.get(alias);
                        maxLength = alias.length();
                    }
                    else if (alias.length() > maxLength)
                    {
                        sr = (ServiceRegistration) m_servletMap.get(alias);
                        maxLength = alias.length();
                    }
                }
            }
        }

        return sr;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.felix.http.lightweight.osgi.RegistrationResolver#getProcessor
     * (org.apache.felix.http.lightweight.http.HttpRequest,
     * org.apache.felix.http.lightweight.http.HttpResponse, java.lang.String)
     */
    public ServiceRegistrationHandler getProcessor(final HttpServletRequestImpl request,
        final HttpServletResponseImpl response, final String requestPath)
    {
        ServiceRegistration element = getServiceRegistration(requestPath);

        if (element != null)
        {
            if (element.isServlet())
            {
                return new ServletHandler(request, response, element, m_logger);
            }
            else
            {
                return new ResourceHandler(request, response, element, m_logger);
            }
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.felix.http.lightweight.osgi.ServiceRegistrationResolver#
     * getServletRequest(java.net.Socket)
     */
    public HttpServletRequestImpl getServletRequest(final Socket socket)
    {
        return new HttpServletRequestImpl(socket, this, m_logger);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.felix.http.lightweight.osgi.ServiceRegistrationResolver#
     * getServletResponse(org.apache.felix.http.lightweight.servlet.HttpRequest,
     * java.io.OutputStream)
     */
    public HttpServletResponseImpl getServletResponse(final OutputStream output)
    {
        return new HttpServletResponseImpl(output);
    }

    /**
     * Validate that a given alias is legal.
     * 
     * @param alias input alias
     * @throws NamespaceException is thrown if alias is illegal
     */
    private void validateAlias( String alias ) throws NamespaceException
    {
        if (alias == null)
        {
            throw new NamespaceException( "Alias is null." );
        }
        
        if (alias.trim().length() == 0)
        {
            throw new NamespaceException( "Alias is an empty string." );
        }
        
        if (!alias.startsWith( "/" )) 
        {
            throw new NamespaceException( "Alias must begin with '/'." );
        }
    }
}
