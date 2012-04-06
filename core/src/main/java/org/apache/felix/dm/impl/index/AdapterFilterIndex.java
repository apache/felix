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
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.FilterIndex;
import org.apache.felix.dm.ServiceUtil;
import org.apache.felix.dm.tracker.ServiceTracker;
import org.apache.felix.dm.tracker.ServiceTrackerCustomizer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import quicktime.std.music.ToneDescription;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AdapterFilterIndex implements FilterIndex, ServiceTrackerCustomizer {
	// (&(objectClass=foo.Bar)(|(service.id=18233)(org.apache.felix.dependencymanager.aspect=18233)))
    private static final String FILTER_START = "(&(" + Constants.OBJECTCLASS + "=";
    private static final String FILTER_SUBSTRING_0 = ")(|(" + Constants.SERVICE_ID + "=";
    private static final String FILTER_SUBSTRING_1 = ")(" + DependencyManager.ASPECT + "=";
    private static final String FILTER_END = ")))";
    private final Object m_lock = new Object();
    private ServiceTracker m_tracker;
    private BundleContext m_context;
    private final Map /* <Long, SortedSet<ServiceReference>> */ m_sidToServiceReferencesMap = new HashMap();
    private final Map /* <String, List<ServiceListener>> */ m_sidToListenersMap = new HashMap();
    private final Map /* <ServiceListener, String> */ m_listenerToFilterMap = new HashMap();

    public void open(BundleContext context) {
        synchronized (m_lock) {
            if (m_context != null) {
                throw new IllegalStateException("Filter already open.");
            }
            try {
                m_tracker = new ServiceTracker(context, context.createFilter("(" + Constants.OBJECTCLASS + "=*)"), this);
            }
            catch (InvalidSyntaxException e) {
                throw new Error();
            }
            m_context = context;
        }
        m_tracker.open(true, true);
    }

    public void close() {
        ServiceTracker tracker;
        synchronized (m_lock) {
            if (m_context == null) {
                throw new IllegalStateException("Filter already closed.");
            }
            tracker = m_tracker;
            m_tracker = null;
            m_context = null;
        }
        tracker.close();
    }

    public boolean isApplicable(String clazz, String filter) {
        return getFilterData(clazz, filter) != null;
    }

    /** Returns a value object with the relevant filter data, or <code>null</code> if this filter was not valid. */
    private FilterData getFilterData(String clazz, String filter) {
        // something like:
    	// (&(objectClass=foo.Bar)(|(service.id=18233)(org.apache.felix.dependencymanager.aspect=18233)))    	
        if ((filter != null)
            && (filter.startsWith(FILTER_START))
            && (filter.endsWith(FILTER_END))
            ) {
        	// service-id = 
            int i0 = filter.indexOf(FILTER_SUBSTRING_0);
            if (i0 == -1) {
                return null;
            }
            // org.apache.felix.dependencymanager.aspect =
            int i1 = filter.indexOf(FILTER_SUBSTRING_1);
            if (i1 == -1 || i1 <= i0) {
                return null;
            }
            long sid = Long.parseLong(filter.substring(i0 + FILTER_SUBSTRING_0.length(), i1));
            long sid2 = Long.parseLong(filter.substring(i1 + FILTER_SUBSTRING_1.length(), filter.length() - FILTER_END.length()));
            if (sid != sid2) {
                return null;
            }
            FilterData result = new FilterData();
            result.serviceId = sid;
            return result;
        }
        return null;
    }

    public List getAllServiceReferences(String clazz, String filter) {
        List /* <ServiceReference> */ result = new ArrayList();
        FilterData data = getFilterData(clazz, filter);
        if (data != null) {
        	SortedSet /* <ServiceReference> */ list = null;
        	synchronized (m_sidToServiceReferencesMap) {
        		list = (SortedSet) m_sidToServiceReferencesMap.get(Long.valueOf(data.serviceId));
        		if (list != null) {
        			Iterator iterator = list.iterator();
        			while (iterator.hasNext()) {
        				result.add((ServiceReference) iterator.next());
        			}
        		}
			}
        }
        return result;
    }

    public void serviceChanged(ServiceEvent event) {
        ServiceReference reference = event.getServiceReference();
        Long sid = ServiceUtil.getServiceIdObject(reference);
        List /* <ServiceListener> */ notificationList = new ArrayList();
        synchronized (m_sidToListenersMap) {
            List /* <ServiceListener> */ list = (ArrayList) m_sidToListenersMap.get(sid);
            if (list != null) {
                notificationList.addAll(list);
            }
        }
        // notify
        Iterator iterator = notificationList.iterator();
        while (iterator.hasNext()) {
        	ServiceListener listener = (ServiceListener) iterator.next();
        	listener.serviceChanged(event);
        }
    }

    public void addServiceListener(ServiceListener listener, String filter) {
        FilterData data = getFilterData(null, filter);
        if (data != null) {
            Long sidObject = Long.valueOf(data.serviceId);
            synchronized (m_sidToListenersMap) {
            	List /* <ServiceListener> */ listeners = (List) m_sidToListenersMap.get(sidObject);
            	if (listeners == null) {
            		listeners = new ArrayList();
            		m_sidToListenersMap.put(sidObject, listeners);
            	}
            	listeners.add(listener);
            }
        }
    }

    public void removeServiceListener(ServiceListener listener) {
        synchronized (m_sidToListenersMap) {
            String filter = (String) m_listenerToFilterMap.remove(listener);
            FilterData data = getFilterData(null, filter);
            if (data != null) {
            	Long sidObject = Long.valueOf(data.serviceId);
            	List /* ServiceListener */ listeners = (List) m_sidToListenersMap.get(sidObject);
            	if (listeners != null) {
            		listeners.remove(listener);
            	}
            }
        }
    }

    public Object addingService(ServiceReference reference) {
        BundleContext context;
        synchronized (m_lock) {
            context = m_context;
        }
        if (context != null) {
            return context.getService(reference);
        }
        else {
            throw new IllegalStateException("No valid bundle context.");
        }
    }

    public void addedService(ServiceReference reference, Object service) {
        add(reference);
    }

    public void modifiedService(ServiceReference reference, Object service) {
        modify(reference);
    }

    public void removedService(ServiceReference reference, Object service) {
        remove(reference);
    }

    public void add(ServiceReference reference) {
        Long sid = ServiceUtil.getServiceIdObject(reference);
        synchronized (m_sidToServiceReferencesMap) {
            Set list = (Set) m_sidToServiceReferencesMap.get(sid);
            if (list == null) {
                list = new TreeSet();
                m_sidToServiceReferencesMap.put(sid, list);
            }
            list.add(reference);
        }
    }

    public void modify(ServiceReference reference) {
        remove(reference);
        add(reference);
    }

    public void remove(ServiceReference reference) {
        Long sid = ServiceUtil.getServiceIdObject(reference);
        synchronized (m_sidToServiceReferencesMap) {
            Set list = (Set) m_sidToServiceReferencesMap.get(sid);
            if (list != null) {
                list.remove(reference);
            }
        }
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("AdapterFilterIndex[");
        sb.append("S2L: " + m_sidToListenersMap.size());
        sb.append(", S2SR: " + m_sidToServiceReferencesMap.size());
        sb.append(", L2F: " + m_listenerToFilterMap.size());
        sb.append("]");
        return sb.toString();
    }

    /** Structure to hold internal filter data. */
    private static class FilterData {
        public long serviceId;
    }

}
