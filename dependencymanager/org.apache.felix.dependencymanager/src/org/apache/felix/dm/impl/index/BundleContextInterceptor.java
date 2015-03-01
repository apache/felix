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
package org.apache.felix.dm.impl.index;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.felix.dm.FilterIndex;
import org.apache.felix.dm.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class BundleContextInterceptor extends BundleContextInterceptorBase {
	protected static final String INDEX_LOG_TRESHOLD = "org.apache.felix.dm.index.log.treshold";
    private final ServiceRegistryCache m_cache;
    private final boolean m_perfmon;
	private Logger m_logger;
	private long m_threshold;

    public BundleContextInterceptor(ServiceRegistryCache cache, BundleContext context) {
        super(context);
        m_cache = cache;
        m_perfmon = context.getProperty(INDEX_LOG_TRESHOLD) != null;
		if (m_perfmon) {
			m_threshold = Long.parseLong(context.getProperty(INDEX_LOG_TRESHOLD));
			m_logger = new Logger(context);
		}
    }

    public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
        FilterIndex filterIndex = m_cache.hasFilterIndexFor(null, filter);
        if (filterIndex != null) {
            filterIndex.addServiceListener(listener, filter);
        }
        else {
            m_context.addServiceListener(listener, filter);
        }
    }

    public void addServiceListener(ServiceListener listener) {
        FilterIndex filterIndex = m_cache.hasFilterIndexFor(null, null);
        if (filterIndex != null) {
            filterIndex.addServiceListener(listener, null);
        }
        else {
            m_context.addServiceListener(listener);
        }
    }

	public void removeServiceListener(ServiceListener listener) {
    	// remove servicelistener. although it would be prettier to find the correct filterindex first it's
    	// probaby faster to do a brute force removal.
    	Iterator<FilterIndex> filterIndexIterator = m_cache.getFilterIndices().iterator();
    	while (filterIndexIterator.hasNext()) {
    		filterIndexIterator.next().removeServiceListener(listener);
    	}
    	m_context.removeServiceListener(listener);
    }

	public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
    	long start = 0L;
    	if (m_perfmon) {
    		start = System.currentTimeMillis();
    	}
        // first we ask the cache if there is an index for our request (class and filter combination)
        FilterIndex filterIndex = m_cache.hasFilterIndexFor(clazz, filter);
        if (filterIndex != null) {
            List<ServiceReference> result = filterIndex.getAllServiceReferences(clazz, filter);
            Iterator<ServiceReference> iterator = result.iterator();
            while (iterator.hasNext()) {
                ServiceReference reference = iterator.next();
                String[] list = (String[]) reference.getProperty(Constants.OBJECTCLASS);
                for (int i = 0; i < list.length; i++) {
                    if (!reference.isAssignableTo(m_context.getBundle(), list[i])) {
                        iterator.remove();
                        break;
                    }
                }
            }
            if (m_perfmon) {
	        	long duration = System.currentTimeMillis() - start;
	        	if (duration > m_threshold) {
	        		m_logger.log(Logger.LOG_DEBUG, "Indexed filter exceeds lookup time threshold (" + duration + " ms): " + clazz + " " + filter);
	        	}
            }
            if (result.size() == 0) {
                return null;
            }
            return (ServiceReference[]) result.toArray(new ServiceReference[result.size()]);
        }
        else {
            // if they don't know, we ask the real bundle context instead
            ServiceReference[] serviceReferences = m_context.getServiceReferences(clazz, filter);
            if (m_perfmon) {
	        	long duration = System.currentTimeMillis() - start;
	        	if (duration > m_threshold) {
	        		m_logger.log(Logger.LOG_DEBUG, "Unindexed filter exceeds lookup time threshold (" + duration + " ms): " + clazz + " " + filter);
	        	}
            }
        	return serviceReferences;
        }
    }

	public ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        // first we ask the cache if there is an index for our request (class and filter combination)
        FilterIndex filterIndex = m_cache.hasFilterIndexFor(clazz, filter);
        if (filterIndex != null) {
            List<ServiceReference> result = filterIndex.getAllServiceReferences(clazz, filter);
            if (result == null || result.size() == 0) {
                return null;
            }
            return (ServiceReference[]) result.toArray(new ServiceReference[result.size()]);
        }
        else {
            // if they don't know, we ask the real bundle context instead
            return m_context.getAllServiceReferences(clazz, filter);
        }
    }

    public ServiceReference getServiceReference(String clazz) {
        ServiceReference[] references;
        try {
            references = getServiceReferences(clazz, null);
            if (references == null || references.length == 0) {
                return null;
            }
            Arrays.sort(references);
            return references[references.length - 1];
        }
        catch (InvalidSyntaxException e) {
            throw new Error("Invalid filter syntax thrown for null filter.", e);
        }
    }

    public void serviceChanged(ServiceEvent event) {
        m_cache.serviceChangedForFilterIndices(event);
    }
}
