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
package org.apache.felix.http.base.internal.whiteboard;

import static org.apache.felix.http.base.internal.util.UriUtils.decodePath;
import static org.apache.felix.http.base.internal.util.UriUtils.removeDotSegments;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
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

import org.apache.felix.http.base.internal.dispatch.RequestDispatcherImpl;
import org.apache.felix.http.base.internal.dispatch.RequestInfo;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.registry.PathResolution;
import org.apache.felix.http.base.internal.registry.PerContextHandlerRegistry;
import org.apache.felix.http.base.internal.registry.ServletResolution;
import org.apache.felix.http.base.internal.util.UriUtils;

/**
 * This servlet context implementation represents the shared
 * part for a servlet context backed by a servlet context helper.
 *
 * For each using bundle, a {@link PerBundleServletContextImpl} is created.
 */
public class SharedServletContextImpl implements ServletContext
{

    private final ServletContext context;
    private final Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();
    private final String contextPath;
    private final String name;
    private final PerContextHandlerRegistry registry;
    private final ServletContextAttributeListener attributeListener;
    private final Map<String, String> initParameters = new HashMap<String, String>();

    public SharedServletContextImpl(final ServletContext webContext,
            final String name,
            final String path,
            final Map<String, String> initParameters,
            final PerContextHandlerRegistry registry)
    {
        this.context = webContext;
        if ( path.equals("/") )
        {
            this.contextPath = webContext.getContextPath();
        }
        else
        {
            this.contextPath = webContext.getContextPath() + path;
        }
        this.name = name;
        if ( initParameters != null )
        {
            this.initParameters.putAll(initParameters);
        }
        this.attributeListener = registry.getEventListenerRegistry();
        this.registry = registry;
    }

    @Override
    public ClassLoader getClassLoader()
    {
        // is implemented by {@link PerBundleServletContextImpl}.
        return null;
    }

    /**
     * @see javax.servlet.ServletContext#getResource(java.lang.String)
     */
    @Override
    public URL getResource(String path)
    {
        // is implemented by {@link PerBundleServletContextImpl}.
        return null;
    }

    @Override
    public String getMimeType(String file)
    {
        // is implemented by {@link PerBundleServletContextImpl}.
        return null;
    }

    @Override
    public String getRealPath(String path)
    {
        // is implemented by {@link PerBundleServletContextImpl}.
        return null;
    }

    @Override
    public Set<String> getResourcePaths(final String path)
    {
        // is implemented by {@link PerBundleServletContextImpl}.
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(final String filterName, final Class<? extends Filter> type)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterRegistration.Dynamic addFilter(final String filterName, final Filter filter)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public FilterRegistration.Dynamic addFilter(final String filterName, final String className)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addListener(final Class<? extends EventListener> type)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addListener(final String className)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends EventListener> void addListener(final T listener)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletRegistration.Dynamic addServlet(final String servletName, final Class<? extends Servlet> type)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletRegistration.Dynamic addServlet(final String servletName, final Servlet servlet)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServletRegistration.Dynamic addServlet(final String servletName, final String className)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Filter> T createFilter(final Class<T> type) throws ServletException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends EventListener> T createListener(final Class<T> type) throws ServletException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Servlet> T createServlet(final Class<T> type) throws ServletException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void declareRoles(final String... roleNames)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getVirtualServerName() {
        return context.getVirtualServerName();
    }

    @Override
    public Object getAttribute(final String name)
    {
        return this.attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames()
    {
        return Collections.enumeration(this.attributes.keySet());
    }

    @Override
    public ServletContext getContext(final String uri)
    {
        return this.context.getContext(uri);
    }

    @Override
    public String getContextPath()
    {
        return this.contextPath;
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
    public FilterRegistration getFilterRegistration(final String filterName)
    {
        return this.context.getFilterRegistration(filterName);
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations()
    {
        return this.context.getFilterRegistrations();
    }

    @Override
    public String getInitParameter(final String name)
    {
        return this.initParameters.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames()
    {
        return Collections.enumeration(this.initParameters.keySet());
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor()
    {
        return null;
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
    public RequestDispatcher getNamedDispatcher(final String name)
    {
        if (name == null)
        {
            return null;
        }

        final RequestDispatcher dispatcher;
        final ServletHandler servletHandler = this.registry.resolveServletByName(name);
        if ( servletHandler != null )
        {
        	final ServletResolution resolution = new ServletResolution();
        	resolution.handler = servletHandler;
            resolution.handlerRegistry = this.registry;
            // TODO - what is the path of a named servlet?
            final RequestInfo requestInfo = new RequestInfo("", null, null, null);
            dispatcher = new RequestDispatcherImpl(resolution, requestInfo);
        }
        else
        {
        	dispatcher = null;
        }
        return dispatcher;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path)
    {
        // See section 9.1 of Servlet 3.x specification...
        if (path == null || (!path.startsWith("/") && !"".equals(path)))
        {
            return null;
        }

        String query = null;
        int q = 0;
        if ((q = path.indexOf('?')) > 0)
        {
            query = path.substring(q + 1);
            path = path.substring(0, q);
        }
        // TODO remove path parameters...
        final String encodedRequestURI = path == null ? "" : removeDotSegments(path);
        final String requestURI = decodePath(encodedRequestURI);

        final RequestDispatcher dispatcher;
        final PathResolution pathResolution = this.registry.resolve(requestURI);
        if ( pathResolution != null )
        {
        	final ServletResolution resolution = new ServletResolution();
        	resolution.handler = pathResolution.handler;
            resolution.handlerRegistry = this.registry;
            final RequestInfo requestInfo = new RequestInfo(pathResolution.servletPath, pathResolution.pathInfo, query,
                    UriUtils.concat(this.contextPath, encodedRequestURI));
            dispatcher = new RequestDispatcherImpl(resolution, requestInfo);
        }
        else
        {
        	dispatcher = null;
        }
        return dispatcher;
    }

    @Override
    public InputStream getResourceAsStream(final String path)
    {
        // is implemented by {@link PerBundleServletContextImpl}.
        return null;
    }

    @Override
    public String getServerInfo()
    {
        return this.context.getServerInfo();
    }

    @SuppressWarnings("deprecation")
    @Override
    public Servlet getServlet(final String name) throws ServletException
    {
        return this.context.getServlet(name);
    }

    @Override
    public String getServletContextName()
    {
        return this.name;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Enumeration<String> getServletNames()
    {
        return this.context.getServletNames();
    }

    @Override
    public ServletRegistration getServletRegistration(final String servletName)
    {
        return this.context.getServletRegistration(servletName);
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations()
    {
        return this.context.getServletRegistrations();
    }

    @SuppressWarnings("deprecation")
    @Override
    public Enumeration<Servlet> getServlets()
    {
        return this.context.getServlets();
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig()
    {
        return this.context.getSessionCookieConfig();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void log(final Exception cause, final String message)
    {
        SystemLogger.error(message, cause);
    }

    @Override
    public void log(final String message)
    {
        SystemLogger.info(message);
    }

    @Override
    public void log(final String message, final Throwable cause)
    {
        SystemLogger.error(message, cause);
    }

    @Override
    public void removeAttribute(final String name)
    {
        final Object oldValue = this.attributes.remove(name);

        if (oldValue != null)
        {
            this.attributeListener.attributeRemoved(new ServletContextAttributeEvent(this, name, oldValue));
        }
    }

    @Override
    public void setAttribute(final String name, final Object value)
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
                this.attributeListener.attributeAdded(new ServletContextAttributeEvent(this, name, value));
            }
            else
            {
                this.attributeListener.attributeReplaced(new ServletContextAttributeEvent(this, name, oldValue));
            }
        }
    }

    @Override
    public boolean setInitParameter(final String name, final String value)
    {
        throw new IllegalStateException();
    }

    @Override
    public void setSessionTrackingModes(final Set<SessionTrackingMode> modes)
    {
        throw new IllegalStateException();
    }
}
