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

import javax.servlet.DispatcherType;
import javax.servlet.Filter;

import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.base.internal.runtime.FilterInfo;
import org.apache.felix.http.base.internal.service.HttpServiceImpl;
import org.apache.felix.http.base.internal.whiteboard.tracker.FilterTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ServletContextHelperTracker;
import org.apache.felix.http.base.internal.whiteboard.tracker.ServletTracker;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.runtime.dto.ResourceDTO;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.util.tracker.ServiceTracker;

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

    private final ServletContextHelperManager contextManager;

    private final HttpService httpService;

    private final ArrayList<ServiceTracker<?, ?>> trackers = new ArrayList<ServiceTracker<?, ?>>();

    public ExtenderManager(final HttpServiceImpl httpService, final BundleContext bundleContext)
    {
        this.mapping = new HashMap<String, AbstractMapping>();
        this.contextManager = new ServletContextHelperManager(bundleContext, httpService);
        this.httpService = httpService;
        addTracker(new FilterTracker(bundleContext, this));
        addTracker(new ServletTracker(bundleContext, this.contextManager));
        addTracker(new ServletContextHelperTracker(bundleContext, this.contextManager));
    }

    public void close()
    {
        for(final ServiceTracker<?, ?> t : this.trackers)
        {
            t.close();
        }
        this.trackers.clear();
        this.unregisterAll();
        this.contextManager.close();
    }

    private void addTracker(ServiceTracker<?, ?> tracker)
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

    public void removeFilter(final Filter service, ServiceReference<Filter> ref)
    {
        final FilterInfo filterInfo = createFilterInfo(ref, false);
        if ( filterInfo != null )
        {
            ((HttpServiceImpl)this.httpService).unregisterFilter(service, filterInfo);
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
 }
