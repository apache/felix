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
package org.apache.felix.dm.itest.api;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.apache.felix.dm.ResourceHandler;
import org.apache.felix.dm.ResourceUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
class ResourceProvider {
	final URL[] m_resources;
    final BundleContext m_context;
    final Map<ResourceHandler, Filter> m_handlers = new HashMap<>();

	ResourceProvider(BundleContext ctx, URL ... resources) {
		m_context = ctx;
		m_resources = resources;
	}
	
    public void change() {
        for (int i = 0; i < m_resources.length; i++) {
        	change(i);
        }    	
    }
    
    @SuppressWarnings("deprecation")
	public void change(int resourceIndex) {
        Map<ResourceHandler, Filter> handlers = new HashMap<>();
        synchronized (m_handlers) {
            handlers.putAll(m_handlers);
        }
        for (Map.Entry<ResourceHandler, Filter> e : handlers.entrySet()) {
        	ResourceHandler handler = e.getKey();
        	Filter filter = e.getValue();
        	if (filter == null || filter.match(ResourceUtil.createProperties(m_resources[resourceIndex]))) {
        		handler.changed(m_resources[resourceIndex]);
            }
        }
    }

    @SuppressWarnings("deprecation")
	public void add(ServiceReference ref, ResourceHandler handler) {
        String filterString = (String) ref.getProperty("filter");
        Filter filter = null;
        if (filterString != null) {
            try {
                filter = m_context.createFilter(filterString);
            }
            catch (InvalidSyntaxException e) {
                Assert.fail("Could not create filter for resource handler: " + e);
                return;
            }
        }
        for (int i = 0; i < m_resources.length; i++) {
            if (filter == null || filter.match(ResourceUtil.createProperties(m_resources[i]))) {
                synchronized (m_handlers) {
                    m_handlers.put(handler, filter);
                }
                handler.added(m_resources[i]);
            }
        }
    }

    public void remove(ServiceReference ref, ResourceHandler handler) {
        Filter filter;
        synchronized (m_handlers) {
            filter = (Filter) m_handlers.remove(handler);
        }
        if (filter != null) {
        	removeResources(handler, filter);
        }
    }

    @SuppressWarnings("deprecation")
	private void removeResources(ResourceHandler handler, Filter filter) {
            for (int i = 0; i < m_resources.length; i++) {
                if (filter == null || filter.match(ResourceUtil.createProperties(m_resources[i]))) {
                    handler.removed(m_resources[i]);
                }
            }
        }

    public void destroy() {
        Map<ResourceHandler, Filter> handlers = new HashMap<>();
        synchronized (m_handlers) {
            handlers.putAll(m_handlers);
        }

        for (Map.Entry<ResourceHandler, Filter> e : handlers.entrySet()) {
            ResourceHandler handler = e.getKey();
            Filter filter = e.getValue();
            removeResources(handler, filter);
        }
    }
}
