/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.base.internal.context;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.util.MimeTypes;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

@SuppressWarnings("deprecation")
public final class ServletContextImpl implements ExtServletContext
{
    private final Bundle bundle;
    private final ServletContext context;
    private final HttpContext httpContext;
    private final Map<String, Object> attributes;
    private final ServletContextAttributeListener attributeListener;

    public ServletContextImpl(Bundle bundle, ServletContext context, HttpContext httpContext, ServletContextAttributeListener attributeListener, boolean sharedAttributes)
    {
        this.bundle = bundle;
        this.context = context;
        this.httpContext = httpContext;
        this.attributeListener = attributeListener;
        this.attributes = sharedAttributes ? null : new ConcurrentHashMap<String, Object>();
    }

    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> type)
    {
        throw new UnsupportedOperationException();
    }

    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter)
    {
        throw new UnsupportedOperationException();
    }

    public FilterRegistration.Dynamic addFilter(String filterName, String className)
    {
        throw new UnsupportedOperationException();
    }

    public void addListener(Class<? extends EventListener> type)
    {
        throw new UnsupportedOperationException();
    }

    public void addListener(String className)
    {
        throw new UnsupportedOperationException();
    }

    public <T extends EventListener> void addListener(T listener)
    {
        throw new UnsupportedOperationException();
    }

    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> type)
    {
        throw new UnsupportedOperationException();
    }

    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet)
    {
        throw new UnsupportedOperationException();
    }

    public ServletRegistration.Dynamic addServlet(String servletName, String className)
    {
        throw new UnsupportedOperationException();
    }

    public <T extends Filter> T createFilter(Class<T> type) throws ServletException
    {
        throw new UnsupportedOperationException();
    }

    public <T extends EventListener> T createListener(Class<T> type) throws ServletException
    {
        throw new UnsupportedOperationException();
    }

    public <T extends Servlet> T createServlet(Class<T> type) throws ServletException
    {
        throw new UnsupportedOperationException();
    }

    public void declareRoles(String... roleNames)
    {
        this.context.declareRoles(roleNames);
    }

    public Object getAttribute(String name)
    {
        return (this.attributes != null) ? this.attributes.get(name) : this.context.getAttribute(name);
    }

    public Enumeration getAttributeNames()
    {
        return (this.attributes != null) ? Collections.enumeration(this.attributes.keySet()) : this.context.getAttributeNames();
    }

    public ClassLoader getClassLoader()
    {
        return bundle.getClass().getClassLoader();
    }

    public ServletContext getContext(String uri)
    {
        return this.context.getContext(uri);
    }

    public String getContextPath()
    {
        return this.context.getContextPath();
    }

    public Set<SessionTrackingMode> getDefaultSessionTrackingModes()
    {
        return this.context.getDefaultSessionTrackingModes();
    }

    public int getEffectiveMajorVersion()
    {
        return this.context.getEffectiveMajorVersion();
    }

    public int getEffectiveMinorVersion()
    {
        return this.context.getEffectiveMinorVersion();
    }

    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes()
    {
        return this.context.getEffectiveSessionTrackingModes();
    }

    public FilterRegistration getFilterRegistration(String filterName)
    {
        return this.context.getFilterRegistration(filterName);
    }

    public Map<String, ? extends FilterRegistration> getFilterRegistrations()
    {
        return this.context.getFilterRegistrations();
    }

    public String getInitParameter(String name)
    {
        return this.context.getInitParameter(name);
    }

    public Enumeration getInitParameterNames()
    {
        return this.context.getInitParameterNames();
    }

    public JspConfigDescriptor getJspConfigDescriptor()
    {
        throw new UnsupportedOperationException();
    }

    public int getMajorVersion()
    {
        return this.context.getMajorVersion();
    }

    public String getMimeType(String file)
    {
        String type = this.httpContext.getMimeType(file);
        if (type != null)
        {
            return type;
        }

        return MimeTypes.get().getByFile(file);
    }

    public int getMinorVersion()
    {
        return this.context.getMinorVersion();
    }

    public RequestDispatcher getNamedDispatcher(String name)
    {
        return this.context.getNamedDispatcher(name);
    }

    public String getRealPath(String name)
    {
        URL url = getResource(normalizePath(name));
        if (url == null) {
            return null;
        }
        return url.toExternalForm();
    }

    public RequestDispatcher getRequestDispatcher(String uri)
    {
        return this.context.getRequestDispatcher(uri);
    }

    public URL getResource(String path)
    {
        return this.httpContext.getResource(normalizePath(path));
    }

    public InputStream getResourceAsStream(String path)
    {
        URL res = getResource(path);
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

    public Set getResourcePaths(String path)
    {
        Enumeration paths = this.bundle.getEntryPaths(normalizePath(path));
        if ((paths == null) || !paths.hasMoreElements())
        {
            return null;
        }

        Set<String> set = new HashSet<String>();
        while (paths.hasMoreElements())
        {
            set.add((String) paths.nextElement());
        }

        return set;
    }

    public String getServerInfo()
    {
        return this.context.getServerInfo();
    }

    public Servlet getServlet(String name) throws ServletException
    {
        return this.context.getServlet(name);
    }

    public String getServletContextName()
    {
        return this.context.getServletContextName();
    }

    public Enumeration getServletNames()
    {
        return this.context.getServletNames();
    }

    public ServletRegistration getServletRegistration(String servletName)
    {
        return this.context.getServletRegistration(servletName);
    }

    public Map<String, ? extends ServletRegistration> getServletRegistrations()
    {
        return this.context.getServletRegistrations();
    }

    public Enumeration getServlets()
    {
        return this.context.getServlets();
    }

    public SessionCookieConfig getSessionCookieConfig()
    {
        return this.context.getSessionCookieConfig();
    }

    public boolean handleSecurity(HttpServletRequest req, HttpServletResponse res) throws IOException
    {
        return this.httpContext.handleSecurity(req, res);
    }

    public void log(Exception cause, String message)
    {
        SystemLogger.error(message, cause);
    }

    public void log(String message)
    {
        SystemLogger.info(message);
    }

    public void log(String message, Throwable cause)
    {
        SystemLogger.error(message, cause);
    }

    public void removeAttribute(String name)
    {
        Object oldValue;
        if (this.attributes != null)
        {
            oldValue = this.attributes.remove(name);
        }
        else
        {
            oldValue = this.context.getAttribute(name);
            this.context.removeAttribute(name);
        }

        if (oldValue != null)
        {
            attributeListener.attributeRemoved(new ServletContextAttributeEvent(this, name, oldValue));
        }
    }

    public void setAttribute(String name, Object value)
    {
        if (value == null)
        {
            this.removeAttribute(name);
        }
        else if (name != null)
        {
            Object oldValue;
            if (this.attributes != null)
            {
                oldValue = this.attributes.put(name, value);
            }
            else
            {
                oldValue = this.context.getAttribute(name);
                this.context.setAttribute(name, value);
            }

            if (oldValue == null)
            {
                attributeListener.attributeAdded(new ServletContextAttributeEvent(this, name, value));
            }
            else
            {
                attributeListener.attributeReplaced(new ServletContextAttributeEvent(this, name, oldValue));
            }
        }
    }

    public boolean setInitParameter(String name, String value)
    {
        return this.context.setInitParameter(name, value);
    }

    public void setSessionTrackingModes(Set<SessionTrackingMode> modes)
    {
        this.context.setSessionTrackingModes(modes);
    }

    private String normalizePath(String path)
    {
        if (path == null)
        {
            return null;
        }

        String normalizedPath = path.trim().replaceAll("/+", "/");
        if (normalizedPath.startsWith("/") && (normalizedPath.length() > 1))
        {
            normalizedPath = normalizedPath.substring(1);
        }

        return normalizedPath;
    }
}
