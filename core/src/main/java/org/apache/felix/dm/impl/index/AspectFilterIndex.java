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
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
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

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class AspectFilterIndex implements FilterIndex, ServiceTrackerCustomizer {
    private static final String FILTER_START = "(&(" + Constants.OBJECTCLASS + "=";
    private static final String FILTER_SUBSTRING_0 = ")(&(|(!(" + Constants.SERVICE_RANKING + "=*))(" + Constants.SERVICE_RANKING + "<=";
    private static final String FILTER_SUBSTRING_1 = "))(|(" + Constants.SERVICE_ID + "=";
    private static final String FILTER_SUBSTRING_2 = ")(" + DependencyManager.ASPECT + "=";
    private static final String FILTER_END = "))))";
    private final Object m_lock = new Object();
    private ServiceTracker m_tracker;
    private BundleContext m_context;
    private final Map /* <Long, SortedSet<ServiceReference>> */ m_sidToServiceReferencesMap = new HashMap();
    private final Map /* <Long, SortedMap<Integer, ServiceListener>> */ m_sidToRankingToListenersMap = new HashMap();
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
        // (&(objectClass=foo.Bar)(&(|(!(service.ranking=*))(service.ranking<=9))(|(service.id=37)(org.apache.felix.dependencymanager.aspect=37))))
        if ((filter != null)
            && (filter.startsWith(FILTER_START))
            && (filter.endsWith(FILTER_END))
            ) {
            int i0 = filter.indexOf(FILTER_SUBSTRING_0);
            if (i0 == -1) {
                return null;
            }
            int i1 = filter.indexOf(FILTER_SUBSTRING_1);
            if (i1 == -1 || i1 <= i0) {
                return null;
            }
            int i2 = filter.indexOf(FILTER_SUBSTRING_2);
            if (i2 == -1 || i2 <= i1) {
                return null;
            }
            long sid = Long.parseLong(filter.substring(i1 + FILTER_SUBSTRING_1.length(), i2));
            long sid2 = Long.parseLong(filter.substring(i2 + FILTER_SUBSTRING_2.length(), filter.length() - FILTER_END.length()));
            if (sid != sid2) {
                return null;
            }
            FilterData result = new FilterData();
            result.className = filter.substring(FILTER_START.length(), i0);
            result.serviceId = sid;
            result.ranking = Integer.parseInt(filter.substring(i0 + FILTER_SUBSTRING_0.length(), i1));
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
        				ServiceReference reference = (ServiceReference) iterator.next();
        				if (ServiceUtil.getRanking(reference) <= data.ranking) {
        					result.add(reference);
        				}
        			}
        		}
			}
        }
        return result;
    }

    public void serviceChanged(ServiceEvent event) {
        List list = new ArrayList();
        ServiceReference reference = event.getServiceReference();
        Long sid = ServiceUtil.getServiceIdObject(reference);
        int ranking = ServiceUtil.getRanking(reference);
        synchronized (m_sidToRankingToListenersMap) {
            SortedMap /* <Integer, ServiceListener> */ map = (SortedMap) m_sidToRankingToListenersMap.get(sid);
            if (map != null) {
                Iterator iterator = map.entrySet().iterator();
                while (iterator.hasNext()) {
                    Entry entry = (Entry) iterator.next();
                    if (ranking <= ((Integer) entry.getKey()).intValue()) {
                        list.add((ServiceListener) entry.getValue());
                    }
                }
            }
        }
        Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
            ServiceListener listener = (ServiceListener) iterator.next();
            listener.serviceChanged(event);
        }
    }

    public void addServiceListener(ServiceListener listener, String filter) {
        FilterData data = getFilterData(null, filter);
        if (data != null) {
            Long sidObject = Long.valueOf(data.serviceId);
            synchronized (m_sidToRankingToListenersMap) {
                SortedMap /* <Integer, ServiceListener> */ rankingToListenersMap = (SortedMap) m_sidToRankingToListenersMap.get(sidObject);
                if (rankingToListenersMap == null) {
                    rankingToListenersMap = new TreeMap();
                    m_sidToRankingToListenersMap.put(sidObject, rankingToListenersMap);
                }
                rankingToListenersMap.put(Integer.valueOf(data.ranking), listener);
                m_listenerToFilterMap.put(listener, filter);
            }
        }
    }

    public void removeServiceListener(ServiceListener listener) {
        synchronized (m_sidToRankingToListenersMap) {
            String filter = (String) m_listenerToFilterMap.remove(listener);
            if (filter != null) {
            	// the listener does exist
	            FilterData data = getFilterData(null, filter);
	            if (data != null) {
	                synchronized (m_sidToRankingToListenersMap) {
	                    SortedMap /* <Integer, ServiceListener> */ rankingToListenersMap = (SortedMap) m_sidToRankingToListenersMap.get(Long.valueOf(data.serviceId));
	                    if (rankingToListenersMap != null) {
	                        rankingToListenersMap.remove(Integer.valueOf(data.ranking));
	                    }
	                }
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
        sb.append("AspectFilterIndex[");
        sb.append("S2R2L: " + m_sidToRankingToListenersMap.size());
        sb.append(", S2SR: " + m_sidToServiceReferencesMap.size());
        sb.append(", L2F: " + m_listenerToFilterMap.size());
        sb.append("]");
        return sb.toString();
    }

    /** Structure to hold internal filter data. */
    private static class FilterData {
        public String className;
        public long serviceId;
        public int ranking;
    }
}
