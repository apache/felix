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
import java.io.InputStream;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.apache.felix.httplite.osgi.Logger;
import org.apache.felix.httplite.osgi.ServiceRegistration;
import org.apache.felix.httplite.osgi.ServiceRegistrationHandler;
import org.apache.felix.httplite.servlet.HttpConstants;
import org.apache.felix.httplite.servlet.HttpServletRequestImpl;
import org.apache.felix.httplite.servlet.HttpServletResponseImpl;
import org.osgi.service.http.HttpContext;

/**
 * Handles resource processing.  
 * 
 * Encapsulates the logic in OSGI Service Platform Release 4 Compendium Version 4.2 Section 102.3
 *
 */
public class ResourceHandler implements ServiceRegistrationHandler
{

    private static final String INDEX_HTML = "index.html";
	private final HttpServletRequestImpl m_request;
    private final HttpServletResponseImpl m_response;

    private final HttpContext m_httpContext;
    private final String m_name;
    private final String m_alias;
	private final Logger m_logger;
    private final int m_aliasIndex;

    /**
     * @param req HttpRequest
     * @param res HttpResponse
     * @param resource ServiceRegistration
     * @param logger Log reference
     */
    public ResourceHandler(final HttpServletRequestImpl req, final HttpServletResponseImpl res, final ServiceRegistration resource, final Logger logger)
    {
        if (resource.isServlet())
        {
            throw new IllegalStateException(
                "Invalid state, ResourceHandler constructed with a Servlet.");
        }

        this.m_request = req;
        this.m_response = res;
        this.m_httpContext = resource.getContext();
        this.m_name = resource.getName();
        this.m_alias = resource.getAlias();  
        this.m_aliasIndex = m_alias.length();
        this.m_logger = logger;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.http.lightweight.osgi.ServiceRegistrationHandler#process(boolean)
     */
    public void handle(final boolean close) throws IOException
    {
        if (!m_request.getMethod().equals(HttpConstants.GET_REQUEST)
            && !m_request.getMethod().equals(HttpConstants.HEAD_REQUEST))
        {

            //POST, PUT, DELETE operations not valid on resources.
        	m_logger.log(Logger.LOG_WARNING, "Ignored client " + m_request.getMethod() + " on static resource.");
            return;
        }

        if (m_httpContext.handleSecurity(m_request, m_response))
        {
            String resourceName = getResourceName(m_request.getRequestURI());
            
            URL resource = null;
            
            if (resourceName.endsWith("/"))
            {
            	m_logger.log(Logger.LOG_DEBUG, "Appending " + INDEX_HTML + " to request " + resourceName);
            	resource = m_httpContext.getResource(resourceName + INDEX_HTML);
            } 
            else 
            {
            	resource = m_httpContext.getResource(resourceName);	
            }
           
            if (resource == null)
            {
            	m_logger.log(Logger.LOG_INFO, "Returning HTTP 404 for request for " + resourceName);
            	m_response.sendError(HttpServletResponse.SC_NOT_FOUND);
            	return;
            }

            InputStream inputStream = resource.openStream();
            m_response.setContentType(m_httpContext.getMimeType(resourceName));

            m_response.writeToOutputStream(inputStream, close);
        }
    }

    /**
     * @param path String
     * @return resource name at given path.
     */
    private String getResourceName(final String path)
    {
        if (path.startsWith( "/" )) 
        {
            return  m_name + path.substring(m_aliasIndex);
        } else 
        {
            return m_name + "/" + path.substring(m_aliasIndex);
        }       
    }
}
