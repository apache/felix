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

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;

import org.apache.felix.httplite.servlet.ServletConfigImpl;
import org.apache.felix.httplite.servlet.ServletContextImpl;
import org.osgi.service.http.HttpContext;

/**
 * This class stores the state from a client that registered a Servlet
 * or resource with the HTTP Service.
 */
public class ServiceRegistration
{
    /**
     * Servlet instance
     */
    private final Servlet m_servlet;
    /**
     * Dictionary of parameters
     */
    private final Dictionary m_initparams;
    /**
     * Reference to HttpContext
     */
    private final HttpContext m_context;
    /**
     * Alias
     */
    private final String m_alias;
    /**
     * Logger instance
     */
    private final Logger m_logger;
    /**
     * true if the servlet has been initialized.  This should happen on the first request only.
     */
    private boolean m_initialized;
    /**
     * Servlet instance
     */
    private boolean m_isServlet;
    /**
     * Name of registration
     */
    private final String m_name;
    private ServletConfigImpl m_servletConfigImpl;

    /**
     * @param alias Alias that the service is registered with.
     * @param servlet Instance of servlet corresponding to alias.
     * @param initparams initial configuration parameters
     * @param context HTTP context
     * @param logger Logger instance
     */
    public ServiceRegistration(final String alias, final Servlet servlet, final Dictionary initparams, final HttpContext context, final Logger logger)
    {
        this.m_alias = alias;
        this.m_name = null;
        this.m_servlet = servlet;
        this.m_initparams = initparams;
        this.m_context = context;
        this.m_logger = logger;
        this.m_initialized = false;
        this.m_isServlet = true;
    }

    /**
      * @param alias Alias that the service is registered with.
     * @param name name of the resource
     * @param context HTTP context
     * @param logger Logger instance
     */
    public ServiceRegistration(final String alias, final String name, final HttpContext context, final Logger logger)
    {
        this.m_alias = alias;
        this.m_name = name;
        this.m_logger = logger;
        this.m_servlet = null;
        this.m_initparams = null;
        this.m_context = context;
        this.m_isServlet = false;
    }

    /**
     * @return true if this registration represents a servlet, false for a resource.
     */
    public final boolean isServlet()
    {
        return m_isServlet;
    }

    /**
     * @return Alias of resource
     */
    public final String getAlias()
    {
        return m_alias;
    }

    /**
     * @return Name of resource
     */
    public final String getName()
    {
        return m_name;
    }

    /**
     * @return true if the init() method has been called
     * on the Servlet at some point in the past, false otherwise.
     */
    public final boolean hasBeenInitialized()
    {
        return m_initialized;
    }

    /**
     * Set the initialized flat to true.  Will throw
     * IllegalStateException() if called multiple times on the same instance.
     */
    public final void setInitialized()
    {
        if (m_initialized)
        {
            throw new IllegalStateException("Servlet has already been initialized.");
        }

        m_initialized = true;
    }

    /**
     * @return the Servlet instance.
     */
    public final Servlet getServlet()
    {
        return m_servlet;
    }

    /**
     * @return Dictionary of init params.
     */
    public final Dictionary getInitparams()
    {
        return m_initparams;
    }

    /**
     * @return HttpContext
     */
    public final HttpContext getContext()
    {
        return m_context;
    }

    /**
     * @return the ServletConfig for this servlet.
     */
    public final ServletConfig getServletConfig()
    {
        if (m_servletConfigImpl == null)
        {
            m_servletConfigImpl = new ServletConfigImpl(m_alias, m_initparams,
                new ServletContextImpl(m_alias, m_context, m_initparams, m_logger));
        }

        return m_servletConfigImpl;
    }
}
