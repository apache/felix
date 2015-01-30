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

import org.apache.felix.http.base.internal.logger.SystemLogger;
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

    private final Map<String, AbstractMapping> mapping;

    private final ServletContextHelperManager contextManager;

    private final HttpService httpService;

    private final ArrayList<ServiceTracker<?, ?>> trackers = new ArrayList<ServiceTracker<?, ?>>();

    public ExtenderManager(final HttpServiceImpl httpService, final BundleContext bundleContext)
    {
        this.mapping = new HashMap<String, AbstractMapping>();
        this.contextManager = new ServletContextHelperManager(bundleContext, httpService);
        this.httpService = httpService;
        addTracker(new FilterTracker(bundleContext, contextManager));
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
