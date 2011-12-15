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
package org.apache.felix.httplite.server;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.felix.httplite.osgi.Logger;
import org.apache.felix.httplite.osgi.ServiceRegistration;
import org.apache.felix.httplite.osgi.ServiceRegistrationHandler;
import org.apache.felix.httplite.servlet.HttpServletRequestImpl;
import org.apache.felix.httplite.servlet.HttpServletResponseImpl;

/**
 *  Handles servlet processing.  
 *  This class encapsulates the work done on a servlet given a request and response.
 */
public class ServletHandler implements ServiceRegistrationHandler
{

    private final HttpServletRequestImpl m_request;
    private final HttpServletResponseImpl m_response;
    private final ServiceRegistration m_servletElement;
    private final Logger m_logger;

    /**
     * @param request Http Request
     * @param response Http Response
     * @param element Servlet Registration
     * @param m_logger Logger
     */
    public ServletHandler(final HttpServletRequestImpl request, final HttpServletResponseImpl response, final ServiceRegistration element, final Logger m_logger)
    {
        this.m_request = request;
        this.m_response = response;
        this.m_servletElement = element;
        this.m_logger = m_logger;
    }

    /**
     * Process the servlet.
     * 
     * @param close true if not keep-alive connection.
     * @throws ServletException
     * @throws IOException
     */
    public void handle(final boolean close) throws ServletException, IOException
    {
        //Check to see if the Servlet has been initialized, if not initialize it and set the flag.
        synchronized (m_servletElement)
        {
            if (!m_servletElement.hasBeenInitialized())
            {
                m_logger.log(Logger.LOG_DEBUG,
                    "Initializing servlet " + m_servletElement.getAlias());
                m_servletElement.getServlet().init(m_servletElement.getServletConfig());
                m_servletElement.setInitialized();
            }
        }

        if (m_servletElement.getContext().handleSecurity(m_request, m_response))
        {
            m_servletElement.getServlet().service(m_request, m_response);
        }

        if (!m_response.isCommitted())
        {
            m_response.flushBuffer();
        }
    }
}
