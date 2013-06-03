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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.dm.FilterIndex;
import org.apache.felix.dm.impl.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceRegistration;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceRegistryCache implements ServiceListener/*, CommandProvider*/ {
    private static final String INDEX_PERFLOG = "org.apache.felix.dependencymanager.index.logmissingindices";
	private final List /* <FilterIndex> */ m_filterIndexList = new CopyOnWriteArrayList();
    private final BundleContext m_context;
    private final FilterIndexBundleContext m_filterIndexBundleContext;
    private final Map /* <BundleContext, BundleContextInterceptor> */ m_bundleContextInterceptorMap = new HashMap();
    private long m_currentVersion = 0;
    private long m_arrayVersion = -1;
    private BundleContextInterceptor[] m_interceptors = null;
    private ServiceRegistration m_registration;
    private boolean m_dumpUnIndexedFilters = "true".equals(System.getProperty(INDEX_PERFLOG));
    private List m_unindexedFilters = new ArrayList();
	private Logger m_logger;
    
    public ServiceRegistryCache(BundleContext context) {
        m_context = context;
        // only obtain the logservice when we actually want to log something.
        if (System.getProperty(INDEX_PERFLOG) != null) {
        	m_logger = new Logger(context);
        }
        m_filterIndexBundleContext = new FilterIndexBundleContext(m_context);
    }
    
    public void open() {
        m_context.addServiceListener(this);
//        m_registration = m_context.registerService(CommandProvider.class.getName(), this, null);
    }
    
    public void close() {
//        m_registration.unregister();
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
                m_interceptors = (BundleContextInterceptor[]) m_bundleContextInterceptorMap.values().toArray(new BundleContextInterceptor[m_bundleContextInterceptorMap.size()]);
                m_arrayVersion = m_currentVersion;
            }
        }
        
        serviceChangedForFilterIndices(event);
    }
    
    /** Creates an interceptor for a bundle context that uses our cache. */
    public BundleContext createBundleContextInterceptor(BundleContext context) {
        synchronized (m_bundleContextInterceptorMap) {
            BundleContextInterceptor bundleContextInterceptor = (BundleContextInterceptor) m_bundleContextInterceptorMap.get(context);
            if (bundleContextInterceptor == null) {
                bundleContextInterceptor = new BundleContextInterceptor(this, context, m_logger);
                m_bundleContextInterceptorMap.put(context, bundleContextInterceptor);
                m_currentVersion++;
                // TODO figure out a good way to clean up bundle contexts that are no longer valid so they can be garbage collected
            }
            return bundleContextInterceptor;
        }
    }

    public FilterIndex hasFilterIndexFor(String clazz, String filter) {
        Iterator iterator = m_filterIndexList.iterator();
        while (iterator.hasNext()) {
            FilterIndex filterIndex = (FilterIndex) iterator.next();
            if (filterIndex.isApplicable(clazz, filter)) {
                return filterIndex;
            }
        }
        if (m_dumpUnIndexedFilters) {
        	String filterStr = clazz + ":" + filter;
	        if (!m_unindexedFilters.contains(filterStr)) {
	        	m_unindexedFilters.add(filterStr);
	        	m_logger.log(Logger.LOG_DEBUG, "No filter index for " + filterStr);
	        }
        }
        return null;
    }

    public void serviceChangedForFilterIndices(ServiceEvent event) {
        Iterator iterator = m_filterIndexList.iterator();
        while (iterator.hasNext()) {
            FilterIndex filterIndex = (FilterIndex) iterator.next();
            filterIndex.serviceChanged(event);
        }
    }

//    public void _sc(CommandInterpreter ci) {
//        ci.println(toString());
//    }
//    
//    public void _fi(CommandInterpreter ci) {
//        String arg = ci.nextArgument();
//        if (arg != null) {
//            int x = Integer.parseInt(arg);
//            FilterIndex filterIndex = (FilterIndex) m_filterIndexList.get(x);
//            String a1 = ci.nextArgument();
//            String a2 = null;
//            if (a1 != null) {
//                if ("-".equals(a1)) {
//                    a1 = null;
//                }
//                a2 = ci.nextArgument();
//            }
//            if (filterIndex.isApplicable(a1, a2)) {
//                List /* <ServiceReference> */ references = filterIndex.getAllServiceReferences(a1, a2);
//                ci.println("Found " + references.size() + " references:");
//                for (int i = 0; i < references.size(); i++) {
//                    ci.println("" + i + " - " + references.get(i));
//                }
//            }
//            else {
//                ci.println("Filter not applicable.");
//            }
//        }
//        else {
//            ci.println("FilterIndices:");
//            Iterator iterator = m_filterIndexList.iterator();
//            int index = 0;
//            while (iterator.hasNext()) {
//                FilterIndex filterIndex = (FilterIndex) iterator.next();
//                ci.println("" + index + " " + filterIndex);
//                index++;
//            }
//        }
//    }
//    
//    public String getHelp() {
//        return "I'm not going to help you!";
//    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("ServiceRegistryCache[");
        sb.append("FilterIndices: " + m_filterIndexList.size());
        sb.append(", BundleContexts intercepted: " + m_bundleContextInterceptorMap.size());
        sb.append("]");
        return sb.toString();
    }

	public List getFilterIndices() {
		return m_filterIndexList;
	}
}
