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
package org.apache.felix.http.whiteboard.internal.manager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.apache.felix.http.api.ExtHttpService;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.apache.felix.http.whiteboard.HttpWhiteboardConstants;
import org.apache.felix.http.whiteboard.internal.manager.HttpContextManager.HttpContextHolder;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;

public final class ExtenderManager
{
    static final String TYPE_FILTER = "f";
    static final String TYPE_SERVLET = "s";

    private HttpService httpService;
    private final Map<String, AbstractMapping> mapping;
    private final HttpContextManager contextManager;

    public ExtenderManager()
    {
        this.mapping = new HashMap<String, AbstractMapping>();
        this.contextManager = new HttpContextManager();
    }

    static boolean isEmpty(final String value)
    {
        return value == null || value.length() == 0;
    }

    private String getStringProperty(ServiceReference ref, String key)
    {
        Object value = ref.getProperty(key);
        return (value instanceof String) ? (String)value : null;
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
        if (value == null) {
            return defValue;
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defValue;
        }
    }

    private void addInitParams(ServiceReference ref, AbstractMapping mapping)
    {
        for (String key : ref.getPropertyKeys()) {
            if (key.startsWith(HttpWhiteboardConstants.INIT_PREFIX)) {
                String paramKey = key.substring(HttpWhiteboardConstants.INIT_PREFIX.length());
                String paramValue = getStringProperty(ref, key);

                if (paramValue != null) {
                    mapping.getInitParams().put(paramKey, paramValue);
                }
            }
        }
    }

    public void add(HttpContext service, ServiceReference ref)
    {
        String contextId = getStringProperty(ref, HttpWhiteboardConstants.CONTEXT_ID);
        if (!isEmpty(contextId))
        {
            boolean shared = getBooleanProperty(ref, HttpWhiteboardConstants.CONTEXT_SHARED);
            Bundle bundle = shared ? null : ref.getBundle();
            Collection<AbstractMapping> mappings = this.contextManager.addHttpContext(bundle, contextId, service);
            for (AbstractMapping mapping : mappings)
            {
                registerMapping(mapping);
            }
        }
        else
        {
            SystemLogger.debug("Ignoring HttpContext Service " + ref + ", " + HttpWhiteboardConstants.CONTEXT_ID
                + " is missing or empty");
        }
    }

    public void remove(HttpContext service)
    {
        Collection<AbstractMapping> mappings = this.contextManager.removeHttpContext(service);
        if (mappings != null)
        {
            for (AbstractMapping mapping : mappings)
            {
                unregisterMapping(mapping);
            }
        }
    }

    private void getHttpContext(AbstractMapping mapping, ServiceReference ref)
    {
        Bundle bundle = ref.getBundle();
        String contextId = getStringProperty(ref, HttpWhiteboardConstants.CONTEXT_ID);
        this.contextManager.getHttpContext(bundle, contextId, mapping);
    }

    private void ungetHttpContext(AbstractMapping mapping, ServiceReference ref)
    {
        Bundle bundle = ref.getBundle();
        String contextId = getStringProperty(ref, HttpWhiteboardConstants.CONTEXT_ID);
        this.contextManager.ungetHttpContext(bundle, contextId, mapping);
    }

    public void add(Filter service, ServiceReference ref)
    {
        int ranking = getIntProperty(ref, Constants.SERVICE_RANKING, 0);
        String pattern = getStringProperty(ref, HttpWhiteboardConstants.PATTERN);

        if (isEmpty(pattern)) {
            SystemLogger.debug("Ignoring Filter Service " + ref + ", " + HttpWhiteboardConstants.PATTERN
                + " is missing or empty");
            return;
        }

        FilterMapping mapping = new FilterMapping(ref.getBundle(), service, pattern, ranking);
        getHttpContext(mapping, ref);
        addInitParams(ref, mapping);
        addMapping(TYPE_FILTER, ref, mapping);
    }

    public void add(Servlet service, ServiceReference ref)
    {
        String alias = getStringProperty(ref, HttpWhiteboardConstants.ALIAS);
        if (isEmpty(alias))
        {
            SystemLogger.debug("Ignoring Servlet Service " + ref + ", " + HttpWhiteboardConstants.ALIAS
                + " is missing or empty");
            return;
        }

        ServletMapping mapping = new ServletMapping(ref.getBundle(), service, alias);
        getHttpContext(mapping, ref);
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

    public synchronized void setHttpService(HttpService service)
    {
        this.httpService = service;
        if (this.httpService instanceof ExtHttpService) {
            SystemLogger.info("Detected extended HttpService. Filters enabled.");
        } else {
            SystemLogger.info("Detected standard HttpService. Filters disabled.");
        }

        registerAll();
    }

    public synchronized void unsetHttpService()
    {
        unregisterAll();
        this.httpService = null;
    }

    public synchronized void unregisterAll()
    {
    	AbstractMapping[] mappings = null;
    	HttpService service;
    	synchronized (this) {
			service = this.httpService;
			if (service != null) {
    			Collection<AbstractMapping> values = this.mapping.values();
    			mappings = values.toArray(new AbstractMapping[values.size()]);
    		}
    	}
    	if (mappings != null) {
    		for (AbstractMapping mapping : mappings) {
    			mapping.unregister(service);
    		}
    	}
    }

    private synchronized void registerAll()
    {
    	AbstractMapping[] mappings = null;
    	HttpService service;
    	synchronized (this) {
			service = this.httpService;
			if (service != null) {
    			Collection<AbstractMapping> values = this.mapping.values();
    			mappings = values.toArray(new AbstractMapping[values.size()]);
    		}
    	}
    	if (mappings != null) {
    		for (AbstractMapping mapping : mappings) {
    			mapping.register(service);
    		}
    	}
    }

    private synchronized void addMapping(final String servType, ServiceReference ref, AbstractMapping mapping)
    {
        this.mapping.put(ref.getProperty(Constants.SERVICE_ID).toString() + servType, mapping);
        this.registerMapping(mapping);
    }

    private synchronized void removeMapping(final String servType, ServiceReference ref)
    {
        AbstractMapping mapping = this.mapping.remove(ref.getProperty(Constants.SERVICE_ID).toString() + servType);
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
     * {@link org.apache.felix.http.whiteboard.internal.manager.HttpContextManager.HttpContextHolder}
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
