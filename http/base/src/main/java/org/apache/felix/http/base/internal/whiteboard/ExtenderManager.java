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
package org.apache.felix.http.base.internal.whiteboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.runtime.ServletInfo;
import org.apache.felix.http.base.internal.service.HttpServiceImpl;
import org.apache.felix.http.base.internal.whiteboard.HttpContextManager.HttpContextHolder;
import org.apache.felix.http.base.internal.whiteboard.tracker.FilterTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ServletContextHelperTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ServletTracker;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.dto.ResourceDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;

@SuppressWarnings({ "deprecation" })
public final class ExtenderManager
{
    static final String TYPE_RESOURCE = "r";

    /**
     * Properties starting with this prefix are passed as servlet init parameters to the
     * {@code init()} method of the servlet.
     */
    public static final String SERVLET_INIT_PREFIX = "servlet.init.";

    /**
     * Properties starting with this prefix are passed as filter
     * init parameters to the {@code init()} method of the filter.
     */
    public static final String FILTER_INIT_PREFIX = "filter.init.";

    private final Map<String, AbstractMapping> mapping;
    private final HttpContextManager contextManager;

    private final HttpService httpService;

    private final ArrayList<ServiceTracker> trackers = new ArrayList<ServiceTracker>();

    public ExtenderManager(final HttpService httpService, final BundleContext bundleContext)
    {
        this.mapping = new HashMap<String, AbstractMapping>();
        this.contextManager = new HttpContextManager();
        this.httpService = httpService;
        addTracker(new FilterTracker(bundleContext, this));
        addTracker(new ServletTracker(bundleContext, this));
        addTracker(new ServletContextHelperTracker(bundleContext, this));
    }

    public void close()
    {
        for(final ServiceTracker t : this.trackers)
        {
            t.close();
        }
        this.trackers.clear();
        this.unregisterAll();
    }

    private void addTracker(ServiceTracker tracker)
    {
        this.trackers.add(tracker);
        tracker.open();
    }

    static boolean isEmpty(final String value)
    {
        return value == null || value.length() == 0;
    }

    static boolean isEmpty(final String[] value)
    {
        return value == null || value.length == 0;
    }

    private String getStringProperty(ServiceReference ref, String key)
    {
        Object value = ref.getProperty(key);
        return (value instanceof String) ? (String) value : null;
    }

    private String[] getStringArrayProperty(ServiceReference ref, String key)
    {
        Object value = ref.getProperty(key);

        if (value instanceof String)
        {
            return new String[] { (String) value };
        }
        else if (value instanceof String[])
        {
            return (String[]) value;
        }
        else if (value instanceof Collection<?>)
        {
            Collection<?> collectionValues = (Collection<?>) value;
            String[] values = new String[collectionValues.size()];

            int i = 0;
            for (Object current : collectionValues)
            {
                values[i++] = current != null ? String.valueOf(current) : null;
            }

            return values;
        }

        return null;
    }

    private boolean getBooleanProperty(ServiceReference ref, String key)
    {
        Object value = ref.getProperty(key);
        if (value instanceof String)
        {
            return Boolean.valueOf((String) value);
        }
        else if (value instanceof Boolean)
        {
            return ((Boolean) value).booleanValue();
        }
        return false;
    }

    private int getIntProperty(ServiceReference ref, String key, int defValue)
    {
        Object value = ref.getProperty(key);
        if (value == null)
        {
            return defValue;
        }

        try
        {
            return Integer.parseInt(value.toString());
        }
        catch (Exception e)
        {
            return defValue;
        }
    }

    /**
     * Get the init parameters.
     */
    private Map<String, String> getInitParams(final ServiceReference<?> ref, final String prefix)
    {
        Map<String, String> result = null;
        for (final String key : ref.getPropertyKeys())
        {
            if ( key.startsWith(prefix))
            {
                final String paramKey = key.substring(prefix.length());
                final String paramValue = getStringProperty(ref, key);

                if (paramValue != null)
                {
                    if ( result == null )
                    {
                        result = new HashMap<String, String>();
                    }
                    result.put(paramKey, paramValue);
                }
            }
        }
        return result;
    }

    public void add(ServletContextHelper service, ServiceReference ref)
    {
        String name = getStringProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME);
        String path = getStringProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_PATH);

        // TODO - check if name and path are valid values
        if (!isEmpty(name) && !isEmpty(path) )
        {
            Collection<AbstractMapping> mappings = this.contextManager.addContextHelper(ref.getBundle(), name, path, service);
            for (AbstractMapping mapping : mappings)
            {
                registerMapping(mapping);
            }
        }
        else
        {
            SystemLogger.debug("Ignoring ServletContextHelper Service " + ref + ", " + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + " is missing or empty");
        }
    }

    public void addResource(final ServiceReference ref)
    {
        final String[] pattern = getStringArrayProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN);
        final String prefix = getStringProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX);

        if (!isEmpty(pattern))
        {
            if ( !isEmpty(prefix))
            {
                for(final String p : pattern)
                {
                    // TODO : check if p is empty - and then log?
                    final ResourceDTO resourceDTO = new ResourceDTO();
                    resourceDTO.patterns = new String[] {p};
                    resourceDTO.prefix = prefix;
                    final ResourceMapping mapping = new ResourceMapping(ref.getBundle(), resourceDTO);
                    this.addMapping(TYPE_RESOURCE, ref, mapping);
                }
            }
            else
            {
                SystemLogger.debug("Ignoring Resource Service " + ref + ", " + HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PREFIX + " is missing or empty");
            }
        }
        else
        {
            SystemLogger.debug("Ignoring Resource Service " + ref + ", " + HttpWhiteboardConstants.HTTP_WHITEBOARD_RESOURCE_PATTERN + " is missing or empty");
        }
    }

    public void removeResource(final ServiceReference ref)
    {
        this.removeMapping(TYPE_RESOURCE, ref);
    }

    public void remove(ServletContextHelper service)
    {
        Collection<AbstractMapping> mappings = this.contextManager.removeContextHelper(service);
        if (mappings != null)
        {
            for (AbstractMapping mapping : mappings)
            {
                unregisterMapping(mapping);
            }
        }
    }

    private void ungetHttpContext(AbstractMapping mapping, ServiceReference ref)
    {
        Bundle bundle = ref.getBundle();
        String contextName = getStringProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT);
        if (!isEmpty(contextName))
        {
            this.contextManager.ungetHttpContext(bundle, contextName, mapping, true);
            return;
        }
        this.contextManager.ungetHttpContext(bundle, null, mapping);
    }

    public void add(final Filter service, final ServiceReference ref)
    {
        final FilterInfo filterInfo = createFilterInfo(ref, true);
        if ( filterInfo != null )
        {
            ((HttpServiceImpl)this.httpService).registerFilter(service, filterInfo);
        }
    }

    private FilterInfo createFilterInfo(final ServiceReference<?> filterRef, final boolean log)
    {
        final FilterInfo filterInfo = new FilterInfo();
        filterInfo.name = getStringProperty(filterRef, HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME);
        filterInfo.asyncSupported = getBooleanProperty(filterRef, HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_ASYNC_SUPPORTED);
        filterInfo.servletNames = getStringArrayProperty(filterRef, HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_SERVLET);
        filterInfo.patterns = getStringArrayProperty(filterRef, HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN);
        filterInfo.ranking = getIntProperty(filterRef, Constants.SERVICE_RANKING, 0);
        filterInfo.serviceId = (Long)filterRef.getProperty(Constants.SERVICE_ID);
        filterInfo.initParams = getInitParams(filterRef, FILTER_INIT_PREFIX);
        String[] dispatcherNames = getStringArrayProperty(filterRef, HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER);
        if (dispatcherNames != null && dispatcherNames.length > 0)
        {
            DispatcherType[] dispatchers = new DispatcherType[dispatcherNames.length];
            for (int i = 0; i < dispatchers.length; i++)
            {
                dispatchers[i] = DispatcherType.valueOf(dispatcherNames[i].toUpperCase());
            }
            filterInfo.dispatcher = dispatchers;
        }

        if (isEmpty(filterInfo.patterns))
        {
            if ( log )
            {
                SystemLogger.debug("Ignoring Filter Service " + filterRef + ", " + HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN +
                        " is missing or empty");
            }
            return null;
        }

        return filterInfo;
    }

    private ServletInfo createServletInfo(final ServiceReference<?> servletRef, final boolean log)
    {
        final ServletInfo servletInfo = new ServletInfo();
        servletInfo.name = getStringProperty(servletRef, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME);
        servletInfo.errorPage = getStringArrayProperty(servletRef, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE);
        servletInfo.patterns = getStringArrayProperty(servletRef, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN);
        servletInfo.asyncSupported = getBooleanProperty(servletRef, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED);
        servletInfo.initParams = getInitParams(servletRef, SERVLET_INIT_PREFIX);
        servletInfo.ranking = getIntProperty(servletRef, Constants.SERVICE_RANKING, 0);
        servletInfo.serviceId = (Long)servletRef.getProperty(Constants.SERVICE_ID);

        if (isEmpty(servletInfo.patterns))
        {
            if ( log ) {
                SystemLogger.debug("Ignoring Servlet Service " + servletRef + ", " + HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN +
                        "is missing or empty");
            }
            return null;
        }
        return servletInfo;
    }

    public void add(Servlet service, ServiceReference<?> ref)
    {
        final ServletInfo servletInfo = createServletInfo(ref, true);
        if ( servletInfo != null )
        {
            ((HttpServiceImpl)this.httpService).registerServlet(service, servletInfo);
        }
    }

    public void removeFilter(final Filter service, ServiceReference ref)
    {
        final FilterInfo filterInfo = createFilterInfo(ref, false);
        if ( filterInfo != null )
        {
            ((HttpServiceImpl)this.httpService).unregisterFilter(service, filterInfo);
        }
    }

    public void removeServlet(Servlet service, ServiceReference ref)
    {
        final ServletInfo servletInfo = createServletInfo(ref, false);
        if ( servletInfo != null )
        {
            ((HttpServiceImpl)this.httpService).unregisterServlet(service, servletInfo);
        }
    }

    private synchronized void unregisterAll()
    {
        AbstractMapping[] mappings = null;
        HttpService service;
        synchronized (this)
        {
            service = this.httpService;
            if (service != null)
            {
                Collection<AbstractMapping> values = this.mapping.values();
                mappings = values.toArray(new AbstractMapping[values.size()]);
            }
        }
        if (mappings != null)
        {
            for (AbstractMapping mapping : mappings)
            {
                mapping.unregister(service);
            }
        }
    }

    private synchronized void addMapping(final String servType, ServiceReference ref, AbstractMapping mapping)
    {
        this.mapping.put(ref.getProperty(Constants.SERVICE_ID).toString().concat(servType), mapping);
        this.registerMapping(mapping);
    }

    private synchronized void removeMapping(final String servType, ServiceReference ref)
    {
        AbstractMapping mapping = this.mapping.remove(ref.getProperty(Constants.SERVICE_ID).toString().concat(servType));
        if (mapping != null)
        {
            ungetHttpContext(mapping, ref);
            unregisterMapping(mapping);
        }
    }

    private void registerMapping(AbstractMapping mapping)
    {
        HttpService httpService = this.httpService;
        if (httpService != null)
        {
            mapping.register(httpService);
        }
    }

    private void unregisterMapping(AbstractMapping mapping)
    {
        HttpService httpService = this.httpService;
        if (httpService != null)
        {
            mapping.unregister(httpService);
        }
    }

    /**
     * Returns
     * {@link org.apache.felix.http.base.internal.whiteboard.whiteboard.internal.manager.HttpContextManager.HttpContextHolder}
     * instances of HttpContext services.
     *
     * @return
     */
    Map<String, HttpContextHolder> getHttpContexts()
    {
        return this.contextManager.getHttpContexts();
    }

    /**
     * Returns {@link AbstractMapping} instances for which there is no
     * registered HttpContext as desired by the context ID.
     */
    Map<String, Set<AbstractMapping>> getOrphanMappings()
    {
        return this.contextManager.getOrphanMappings();
    }

    /**
     * Returns mappings indexed by there owning OSGi service.
     */
    Map<String, AbstractMapping> getMappings()
    {
        synchronized (this)
        {
            return new HashMap<String, AbstractMapping>(this.mapping);
        }
    }
}
