/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.base.internal.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.osgi.framework.Bundle;
import org.osgi.service.http.context.ServletContextHelper;

public class ServletContextImpl implements ExtServletContext {

    private final ServletContextHelper delegatee;

    private final Bundle bundle;
    private final ServletContext context;
    private final Map<String, Object> attributes;

    public ServletContextImpl(final Bundle bundle,
            final ServletContext context,
            final ServletContextHelper delegatee)
    {
        this.bundle = bundle;
        this.context = context;
        this.delegatee = delegatee;
        this.attributes = new ConcurrentHashMap<String, Object>();
    }

    /**
     * @see org.apache.felix.http.base.internal.context.ExtServletContext#handleSecurity(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        return delegatee.handleSecurity(request, response);
    }

    /**
     * @see javax.servlet.ServletContext#getResource(java.lang.String)
     */
    @Override
    public URL getResource(String path)
    {
        return delegatee.getResource(path);
    }

    @Override
    public String getMimeType(String file)
    {
        return delegatee.getMimeType(file);
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> type)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, String className)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addListener(Class<? extends EventListener> type)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addListener(String className)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends EventListener> void addListener(T listener)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> type)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, String className)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> type) throws ServletException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> type) throws ServletException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> type) throws ServletException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void declareRoles(String... roleNames)
    {
        this.context.declareRoles(roleNames);
    }

    @Override
    public String getVirtualServerName() {
        return context.getVirtualServerName();
    }

    @Override
    public Object getAttribute(String name)
    {
        return this.attributes.get(name);
    }

    @Override
    public Enumeration getAttributeNames()
    {
        return Collections.enumeration(this.attributes.keySet());
    }

    @Override
    public ClassLoader getClassLoader()
    {
        return bundle.getClass().getClassLoader();
    }

    @Override
    public ServletContext getContext(String uri)
    {
        // TODO
        return this.context.getContext(uri);
    }

    @Override
    public String getContextPath()
    {
        // TODO
        return this.context.getContextPath();
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes()
    {
        return this.context.getDefaultSessionTrackingModes();
    }

    @Override
    public int getEffectiveMajorVersion()
    {
        return this.context.getEffectiveMajorVersion();
    }

    @Override
    public int getEffectiveMinorVersion()
    {
        return this.context.getEffectiveMinorVersion();
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes()
    {
        return this.context.getEffectiveSessionTrackingModes();
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName)
    {
        return this.context.getFilterRegistration(filterName);
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations()
    {
        return this.context.getFilterRegistrations();
    }

    @Override
    public String getInitParameter(String name)
    {
        return this.context.getInitParameter(name);
    }

    @Override
    public Enumeration getInitParameterNames()
    {
        return this.context.getInitParameterNames();
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getMajorVersion()
    {
        return this.context.getMajorVersion();
    }

    @Override
    public int getMinorVersion()
    {
        return this.context.getMinorVersion();
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name)
    {
        return this.context.getNamedDispatcher(name);
    }

    @Override
    public String getRealPath(String path)
    {
        return this.delegatee.getRealPath(path);
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String uri)
    {
        return this.context.getRequestDispatcher(uri);
    }

    @Override
    public InputStream getResourceAsStream(String path)
    {
        final URL res = getResource(path);
        if (res != null)
        {
            try
            {
                return res.openStream();
            }
            catch (IOException e)
            {
                // Do nothing
            }
        }
        return null;
    }

    @Override
    public Set<String> getResourcePaths(final String path)
    {
        return this.delegatee.getResourcePaths(path);
    }

    @Override
    public String getServerInfo()
    {
        return this.context.getServerInfo();
    }

    @Override
    public Servlet getServlet(String name) throws ServletException
    {
        return this.context.getServlet(name);
    }

    @Override
    public String getServletContextName()
    {
        return this.context.getServletContextName();
    }

    @Override
    public Enumeration getServletNames()
    {
        return this.context.getServletNames();
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName)
    {
        return this.context.getServletRegistration(servletName);
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations()
    {
        return this.context.getServletRegistrations();
    }

    @Override
    public Enumeration getServlets()
    {
        return this.context.getServlets();
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig()
    {
        return this.context.getSessionCookieConfig();
    }

    @Override
    public void log(Exception cause, String message)
    {
        SystemLogger.error(message, cause);
    }

    @Override
    public void log(String message)
    {
        SystemLogger.info(message);
    }

    @Override
    public void log(String message, Throwable cause)
    {
        SystemLogger.error(message, cause);
    }

    @Override
    public void removeAttribute(String name)
    {
        Object oldValue = this.attributes.remove(name);

        if (oldValue != null)
        {
            //this.attributeListener.attributeRemoved(new ServletContextAttributeEvent(this, name, oldValue));
        }
    }

    @Override
    public void setAttribute(String name, Object value)
    {
        if (value == null)
        {
            this.removeAttribute(name);
        }
        else if (name != null)
        {
            Object oldValue = this.attributes.put(name, value);

            if (oldValue == null)
            {
                //this.attributeListener.attributeAdded(new ServletContextAttributeEvent(this, name, value));
            }
            else
            {
                //this.attributeListener.attributeReplaced(new ServletContextAttributeEvent(this, name, oldValue));
            }
        }
    }

    @Override
    public boolean setInitParameter(String name, String value)
    {
        return this.context.setInitParameter(name, value);
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> modes)
    {
        this.context.setSessionTrackingModes(modes);
    }
}
