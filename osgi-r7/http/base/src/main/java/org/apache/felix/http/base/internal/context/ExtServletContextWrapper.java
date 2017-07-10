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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

/**
 * Wrapper of an {code ExtServletContex}.
 * This implementation simply forwards to the delegate.
 */
public abstract class ExtServletContextWrapper implements ExtServletContext
{
    private final ExtServletContext delegate;

	public ExtServletContextWrapper(final ExtServletContext delegate)
    {
    	this.delegate = delegate;
    }

	@Override
    public boolean handleSecurity(final HttpServletRequest req,
	        final HttpServletResponse res) throws IOException
	{
		return delegate.handleSecurity(req, res);
	}

	@Override
    public HttpSessionAttributeListener getHttpSessionAttributeListener()
	{
		return delegate.getHttpSessionAttributeListener();
	}

	@Override
    public HttpSessionListener getHttpSessionListener()
	{
		return delegate.getHttpSessionListener();
	}

	@Override
    public ServletRequestListener getServletRequestListener()
	{
		return delegate.getServletRequestListener();
	}

	@Override
    public ServletRequestAttributeListener getServletRequestAttributeListener()
	{
		return delegate.getServletRequestAttributeListener();
	}

	@Override
    public String getContextPath()
	{
		return delegate.getContextPath();
	}

	@Override
    public ServletContext getContext(final String uripath)
	{
		return delegate.getContext(uripath);
	}

	@Override
    public int getMajorVersion()
	{
		return delegate.getMajorVersion();
	}

	@Override
    public int getMinorVersion()
	{
		return delegate.getMinorVersion();
	}

	@Override
    public int getEffectiveMajorVersion()
	{
		return delegate.getEffectiveMajorVersion();
	}

	@Override
    public int getEffectiveMinorVersion()
	{
		return delegate.getEffectiveMinorVersion();
	}

	@Override
    public String getMimeType(final String file)
	{
		return delegate.getMimeType(file);
	}

	@Override
    public Set<String> getResourcePaths(final String path)
	{
		return delegate.getResourcePaths(path);
	}

	@Override
    public URL getResource(final String path) throws MalformedURLException
	{
		return delegate.getResource(path);
	}

	@Override
    public InputStream getResourceAsStream(final String path)
	{
		return delegate.getResourceAsStream(path);
	}

	@Override
    public RequestDispatcher getRequestDispatcher(final String path)
	{
		return delegate.getRequestDispatcher(path);
	}

	@Override
    public RequestDispatcher getNamedDispatcher(final String name)
	{
		return delegate.getNamedDispatcher(name);
	}

	@Override
    @SuppressWarnings("deprecation")
    public Servlet getServlet(final String name) throws ServletException
	{
		return delegate.getServlet(name);
	}

	@Override
    @SuppressWarnings("deprecation")
    public Enumeration<Servlet> getServlets()
    {
		return delegate.getServlets();
	}

	@Override
    @SuppressWarnings("deprecation")
    public Enumeration<String> getServletNames()
    {
		return delegate.getServletNames();
	}

	@Override
    public void log(final String msg)
	{
		delegate.log(msg);
	}

	@Override
    @SuppressWarnings("deprecation")
    public void log(final Exception exception, final String msg)
	{
		delegate.log(exception, msg);
	}

	@Override
    public void log(final String message, final Throwable throwable)
	{
		delegate.log(message, throwable);
	}

	@Override
    public String getRealPath(final String path)
	{
		return delegate.getRealPath(path);
	}

	@Override
    public String getServerInfo()
	{
		return delegate.getServerInfo();
	}

	@Override
    public String getInitParameter(final String name)
	{
		return delegate.getInitParameter(name);
	}

	@Override
    public Enumeration<String> getInitParameterNames()
	{
		return delegate.getInitParameterNames();
	}

	@Override
    public boolean setInitParameter(final String name, final String value)
	{
		return delegate.setInitParameter(name, value);
	}

	@Override
    public Object getAttribute(final String name)
	{
		return delegate.getAttribute(name);
	}

	@Override
    public Enumeration<String> getAttributeNames()
	{
		return delegate.getAttributeNames();
	}

	@Override
    public void setAttribute(final String name, final Object object)
	{
		delegate.setAttribute(name, object);
	}

	@Override
    public void removeAttribute(final String name)
	{
		delegate.removeAttribute(name);
	}

	@Override
    public String getServletContextName() {
		return delegate.getServletContextName();
	}

	@Override
    public Dynamic addServlet(final String servletName, final String className)
	{
		return delegate.addServlet(servletName, className);
	}

	@Override
    public Dynamic addServlet(final String servletName, final Servlet servlet)
	{
		return delegate.addServlet(servletName, servlet);
	}

	@Override
    public Dynamic addServlet(final String servletName,
	        final Class<? extends Servlet> servletClass)
	{
		return delegate.addServlet(servletName, servletClass);
	}

	@Override
    public <T extends Servlet> T createServlet(final Class<T> clazz)
			throws ServletException
	{
		return delegate.createServlet(clazz);
	}

	@Override
    public ServletRegistration getServletRegistration(final String servletName)
	{
		return delegate.getServletRegistration(servletName);
	}

	@Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations()
	{
		return delegate.getServletRegistrations();
	}

	@Override
    public javax.servlet.FilterRegistration.Dynamic addFilter(
	        final String filterName, final String className)
	{
		return delegate.addFilter(filterName, className);
	}

	@Override
    public javax.servlet.FilterRegistration.Dynamic addFilter(
	        final String filterName, final Filter filter)
	{
		return delegate.addFilter(filterName, filter);
	}

	@Override
    public javax.servlet.FilterRegistration.Dynamic addFilter(
	        final String filterName, final Class<? extends Filter> filterClass)
	{
		return delegate.addFilter(filterName, filterClass);
	}

	@Override
    public <T extends Filter> T createFilter(final Class<T> clazz)
			throws ServletException
	{
		return delegate.createFilter(clazz);
	}

	@Override
    public FilterRegistration getFilterRegistration(final String filterName)
	{
		return delegate.getFilterRegistration(filterName);
	}

	@Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations()
	{
		return delegate.getFilterRegistrations();
	}

	@Override
    public SessionCookieConfig getSessionCookieConfig()
	{
		return delegate.getSessionCookieConfig();
	}

	@Override
    public void setSessionTrackingModes(
	        final Set<SessionTrackingMode> sessionTrackingModes)
	{
		delegate.setSessionTrackingModes(sessionTrackingModes);
	}

	@Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes()
	{
		return delegate.getDefaultSessionTrackingModes();
	}

	@Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes()
	{
		return delegate.getEffectiveSessionTrackingModes();
	}

	@Override
    public void addListener(final String className)
	{
		delegate.addListener(className);
	}

	@Override
    public <T extends EventListener> void addListener(final T t)
	{
		delegate.addListener(t);
	}

	@Override
    public void addListener(final Class<? extends EventListener> listenerClass)
	{
		delegate.addListener(listenerClass);
	}

	@Override
    public <T extends EventListener> T createListener(final Class<T> clazz)
			throws ServletException
	{
		return delegate.createListener(clazz);
	}

	@Override
    public JspConfigDescriptor getJspConfigDescriptor()
	{
		return delegate.getJspConfigDescriptor();
	}

	@Override
    public ClassLoader getClassLoader()
	{
		return delegate.getClassLoader();
	}

	@Override
    public void declareRoles(final String... roleNames)
	{
		delegate.declareRoles(roleNames);
	}

	@Override
    public String getVirtualServerName()
	{
		return delegate.getVirtualServerName();
	}
}
