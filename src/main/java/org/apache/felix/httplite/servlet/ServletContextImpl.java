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
package org.apache.felix.httplite.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.felix.httplite.osgi.Logger;
import org.osgi.service.http.HttpContext;

/**
 * ServletContext implementation.
 *
 */
public class ServletContextImpl implements ServletContext
{

    private final HttpContext m_httpContext;
    private final Logger m_logger;
    private final Dictionary m_initparams;
    private Map m_attributes;
    private final String m_name;

    /**
     * @param name Name of Servlet Context
     * @param httpContext HttpContext
     * @param initparams Dictionary
     * @param logger Logger
     */
    public ServletContextImpl(final String name, final HttpContext httpContext, final Dictionary initparams, final Logger logger)
    {
        this.m_name = name;
        this.m_httpContext = httpContext;
        this.m_initparams = initparams;
        this.m_logger = logger;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getContext(java.lang.String)
     */
	public ServletContext getContext(String uripath)
    {
        return null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getMajorVersion()
     */
	public int getMajorVersion()
    {
        return 2;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getMinorVersion()
     */
	public int getMinorVersion()
    {
        return 4;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getMimeType(java.lang.String)
     */
	public String getMimeType(String file)
    {
        return m_httpContext.getMimeType(file);
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getResourcePaths(java.lang.String)
     */
	public Set getResourcePaths(String path)
    {
        return null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getResource(java.lang.String)
     */
	public URL getResource(String path) throws MalformedURLException
    {
        return m_httpContext.getResource(path);
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getResourceAsStream(java.lang.String)
     */
	public InputStream getResourceAsStream(String path)
    {
        try
        {
            return m_httpContext.getResource(path).openStream();
        }
        catch (IOException e)
        {
            m_logger.log(Logger.LOG_ERROR, "Unable to open stream on resource: " + path,
                e);
            return null;
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getRequestDispatcher(java.lang.String)
     */
	public RequestDispatcher getRequestDispatcher(String path)
    {
        return null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getNamedDispatcher(java.lang.String)
     */

	public RequestDispatcher getNamedDispatcher(String name)
    {
        return null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getServlet(java.lang.String)
     */
	public Servlet getServlet(String name) throws ServletException
    {
        return null;
    }


	public Enumeration getServlets()
    {
        return null;
    }


	public Enumeration getServletNames()
    {
        return null;
    }


	public void log(String msg)
    {
        m_logger.log(Logger.LOG_INFO, msg);
    }


	public void log(Exception exception, String msg)
    {
        m_logger.log(Logger.LOG_ERROR, msg, exception);
    }


	public void log(String message, Throwable throwable)
    {
        m_logger.log(Logger.LOG_ERROR, message, throwable);
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getRealPath(java.lang.String)
     */
	public String getRealPath(String path)
    {
        return path;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getServerInfo()
     */
	public String getServerInfo()
    {
        return HttpConstants.SERVER_INFO;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getInitParameter(java.lang.String)
     */
	public String getInitParameter(String name)
    {
        if (m_initparams != null)
        {
            Object o = m_initparams.get(name);

            if (o != null)
            {
                return o.toString();
            }
        }

        return null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getInitParameterNames()
     */
	public Enumeration getInitParameterNames()
    {
        if (m_initparams != null)
        {
            return m_initparams.keys();
        }

        return HttpConstants.EMPTY_ENUMERATION;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getAttribute(java.lang.String)
     */
	public Object getAttribute(String name)
    {
        if (m_attributes != null)
        {
            return m_attributes.get(name);
        }

        return null;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getAttributeNames()
     */
	public Enumeration getAttributeNames()
    {
        if (m_attributes != null)
        {
            return Collections.enumeration(m_attributes.keySet());
        }

        return HttpConstants.EMPTY_ENUMERATION;
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#setAttribute(java.lang.String, java.lang.Object)
     */
	public void setAttribute(String name, Object object)
    {
        if (m_attributes == null)
        {
            m_attributes = new HashMap();
        }

        m_attributes.put(name, object);
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#removeAttribute(java.lang.String)
     */
	public void removeAttribute(String name)
    {
        if (m_attributes != null)
        {
            m_attributes.remove(name);
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getServletContextName()
     */
	public String getServletContextName()
    {
        return m_name;
    }

	/* (non-Javadoc)
	 * @see javax.servlet.ServletContext#getContextPath()
	 */
	/**
	 * @return the context path.
	 */
	public String getContextPath() {	
		return m_name;
	}
}
