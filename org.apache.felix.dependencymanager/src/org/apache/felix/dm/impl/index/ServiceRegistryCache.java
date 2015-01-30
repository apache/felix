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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.dm.FilterIndex;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceRegistryCache implements ServiceListener/*, CommandProvider*/ {
	private final List<FilterIndex> m_filterIndexList = new CopyOnWriteArrayList<>();
    private final BundleContext m_context;
    private final FilterIndexBundleContext m_filterIndexBundleContext;
	private final Map<BundleContext, BundleContextInterceptor> m_bundleContextInterceptorMap = new HashMap<>();
    private long m_currentVersion = 0;
    private long m_arrayVersion = -1;
    
    public ServiceRegistryCache(BundleContext context) {
        m_context = context;
        m_filterIndexBundleContext = new FilterIndexBundleContext(m_context);
    }
    
    public void open() {
        m_context.addServiceListener(this);
    }
    
    public void close() {
        m_context.removeServiceListener(this);
    }
    
    public void addFilterIndex(FilterIndex index) {
        m_filterIndexList.add(index);
        index.open(m_filterIndexBundleContext);
    }
    
    public void removeFilterIndex(FilterIndex index) {
        index.close();
        m_filterIndexList.remove(index);
    }

    public void serviceChanged(ServiceEvent event) {
        // any incoming event is first dispatched to the list of filter indices
        m_filterIndexBundleContext.serviceChanged(event);
        // and then all the other listeners can access it
        synchronized (m_bundleContextInterceptorMap) {
            if (m_currentVersion != m_arrayVersion) {
                // if our copy is out of date, we make a new one
                m_arrayVersion = m_currentVersion;
            }
        }
        
        serviceChangedForFilterIndices(event);
    }
    
    /** Creates an interceptor for a bundle context that uses our cache. */
    public BundleContext createBundleContextInterceptor(BundleContext context) {
        synchronized (m_bundleContextInterceptorMap) {
            BundleContextInterceptor bundleContextInterceptor = m_bundleContextInterceptorMap.get(context);
            if (bundleContextInterceptor == null) {
                bundleContextInterceptor = new BundleContextInterceptor(this, context);
                m_bundleContextInterceptorMap.put(context, bundleContextInterceptor);
                m_currentVersion++;
                // TODO figure out a good way to clean up bundle contexts that are no longer valid so they can be garbage collected
            }
            return bundleContextInterceptor;
        }
    }

    public FilterIndex hasFilterIndexFor(String clazz, String filter) {
        Iterator<FilterIndex> iterator = m_filterIndexList.iterator();
        while (iterator.hasNext()) {
            FilterIndex filterIndex = iterator.next();
            if (filterIndex.isApplicable(clazz, filter)) {
                return filterIndex;
            }
        }
        return null;
    }

    public void serviceChangedForFilterIndices(ServiceEvent event) {
        Iterator<FilterIndex> iterator = m_filterIndexList.iterator();
        while (iterator.hasNext()) {
            FilterIndex filterIndex = iterator.next();
            filterIndex.serviceChanged(event);
        }
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("ServiceRegistryCache[");
        sb.append("FilterIndices: " + m_filterIndexList.size());
        sb.append(", BundleContexts intercepted: " + m_bundleContextInterceptorMap.size());
        sb.append("]");
        return sb.toString();
    }

	public List<FilterIndex> getFilterIndices() {
		return m_filterIndexList;
	}
}
