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
import org.apache.felix.http.base.internal.whiteboard.HttpContextManager.HttpContextHolder;
import org.apache.felix.http.base.internal.whiteboard.tracker.FilterTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ServletContextHelperTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ServletTracker;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.context.ServletContextHelper;
import org.osgi.service.http.runtime.dto.ResourceDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;

@SuppressWarnings({ "deprecation" })
public final class ExtenderManager
{
    static final String TYPE_FILTER = "f";
    static final String TYPE_SERVLET = "s";
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

    private void addInitParams(ServiceReference ref, AbstractMapping mapping)
    {
        for (String key : ref.getPropertyKeys())
        {
            String prefixKey = null;

            if (mapping instanceof FilterMapping && key.startsWith(FILTER_INIT_PREFIX))
            {
                prefixKey = FILTER_INIT_PREFIX;
            }
            else if (mapping instanceof ServletMapping && key.startsWith(SERVLET_INIT_PREFIX))
            {
                prefixKey = SERVLET_INIT_PREFIX;
            }

            if (prefixKey != null)
            {
                String paramKey = key.substring(prefixKey.length());
                String paramValue = getStringProperty(ref, key);

                if (paramValue != null)
                {
                    mapping.getInitParams().put(paramKey, paramValue);
                }
            }
        }
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

    private HttpContext getHttpContext(AbstractMapping mapping, ServiceReference ref)
    {
        Bundle bundle = ref.getBundle();
        String contextName = getStringProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT);
        if (!isEmpty(contextName))
        {
            return this.contextManager.getHttpContext(bundle, contextName, mapping, true);
        }
        return this.contextManager.getHttpContext(bundle, null, mapping);
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
        FilterInfo filterInfo = new FilterInfo();
        filterInfo.name = getStringProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_NAME);
        if ( filterInfo.name == null || filterInfo.name.isEmpty() )
        {
            filterInfo.name = service.getClass().getName();
        }
        filterInfo.asyncSupported = getBooleanProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_ASYNC_SUPPORTED);
        filterInfo.servletNames = getStringArrayProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_SERVLET);
        filterInfo.patterns = getStringArrayProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN);
        filterInfo.ranking = getIntProperty(ref, Constants.SERVICE_RANKING, 0);

        String[] dispatcherNames = getStringArrayProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_DISPATCHER);
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
            SystemLogger.debug("Ignoring Filter Service " + ref + ", " + HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_PATTERN +
                    " is missing or empty");
            return;
        }

        FilterMapping mapping = new FilterMapping(ref.getBundle(), service, filterInfo);
        filterInfo.context = getHttpContext(mapping, ref); // XXX
        addInitParams(ref, mapping);
        addMapping(TYPE_FILTER, ref, mapping);
    }

    public void add(Servlet service, ServiceReference ref)
    {
        ServletInfo servletInfo = new ServletInfo();
        servletInfo.name = getStringProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_NAME);
        if ( servletInfo.name == null || servletInfo.name.isEmpty() )
        {
            servletInfo.name = service.getClass().getName();
        }
        servletInfo.errorPage = getStringArrayProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ERROR_PAGE);
        servletInfo.patterns = getStringArrayProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN);
        servletInfo.asyncSupported = getBooleanProperty(ref, HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_ASYNC_SUPPORTED);

        if (isEmpty(servletInfo.patterns))
        {
            SystemLogger.debug("Ignoring Servlet Service " + ref + ", " + HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN +
                    "is missing or empty");
            return;
        }

        final ServletMapping mapping = new ServletMapping(ref.getBundle(), service, servletInfo);
        servletInfo.context = getHttpContext(mapping, ref); // XXX
        addInitParams(ref, mapping);
        addMapping(TYPE_SERVLET, ref, mapping);
    }

    public void removeFilter(ServiceReference ref)
    {
        removeMapping(TYPE_FILTER, ref);
    }

    public void removeServlet(ServiceReference ref)
    {
        removeMapping(TYPE_SERVLET, ref);
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
