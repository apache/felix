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
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.FilterIndex;
import org.apache.felix.dm.impl.ServiceUtil;
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
public class AdapterFilterIndex extends AbstractFactoryFilterIndex implements FilterIndex, ServiceTrackerCustomizer {
	// (&(objectClass=foo.Bar)(|(service.id=18233)(org.apache.felix.dependencymanager.aspect=18233)))
	private static final String FILTER_REGEXP = "\\(&\\(" + Constants.OBJECTCLASS + "=([a-zA-Z\\.\\$0-9]*)\\)\\(\\|\\(" 
									+ Constants.SERVICE_ID + "=([0-9]*)\\)\\(" 
									+ DependencyManager.ASPECT + "=([0-9]*)\\)\\)\\)";
	private static final Pattern PATTERN = Pattern.compile(FILTER_REGEXP);
    private final Object m_lock = new Object();
    private ServiceTracker m_tracker;
    private BundleContext m_context;
	private final Map<Object, List<ServiceListener>> m_sidToListenersMap = new HashMap<>();
	protected final Map<ServiceListener, String> m_listenerToObjectClassMap = new HashMap<>();

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
    	FilterData resultData = null;
    	if (filter != null) {
	    	Matcher matcher = PATTERN.matcher(filter);
	    	if (matcher.matches()) {
	    		String sid = matcher.group(2);
	    		String sid2 = matcher.group(3);
	    		if (sid.equals(sid2)) {
	    			resultData = new FilterData();
	    			resultData.m_serviceId = Long.parseLong(sid);
	    		}
	    	}
    	}
    	return resultData;
    }

	public List<ServiceReference> getAllServiceReferences(String clazz, String filter) {
		List<ServiceReference> result = new ArrayList<>();
		Matcher matcher = PATTERN.matcher(filter);
		if (matcher.matches()) {
			FilterData data = getFilterData(clazz, filter);
			if (data != null) {
				SortedSet<ServiceReference> list = null;
				synchronized (m_sidToServiceReferencesMap) {
					list = m_sidToServiceReferencesMap.get(Long.valueOf(data.m_serviceId));
					if (list != null) {
						Iterator<ServiceReference> iterator = list.iterator();
						while (iterator.hasNext()) {
							ServiceReference ref = (ServiceReference) iterator.next();
							String objectClass = matcher.group(1);
							if (referenceMatchesObjectClass(ref, objectClass)) {
								result.add(ref);
							}
						}
					}
				}
			}
		}
		return result;
	}
    
	public void serviceChanged(ServiceEvent event) {
        ServiceReference reference = event.getServiceReference();
        Long sid = ServiceUtil.getServiceIdObject(reference);
        List<ServiceListener> notificationList = new ArrayList<>();
        synchronized (m_sidToListenersMap) {
            List<ServiceListener> list = m_sidToListenersMap.get(sid);
            if (list != null) {
            	Iterator<ServiceListener> iterator = list.iterator();
            	while (iterator.hasNext()) {
                	ServiceListener listener = (ServiceListener) iterator.next();
                	String objectClass = m_listenerToObjectClassMap.get(listener);
                	if (referenceMatchesObjectClass(reference, objectClass)) {
                		notificationList.add(listener);
                	} 
            	}
            }
        }
        // notify
        Iterator<ServiceListener> iterator = notificationList.iterator();
        while (iterator.hasNext()) {
        	ServiceListener listener = (ServiceListener) iterator.next();
        	listener.serviceChanged(event);
        }
    }

	public void addServiceListener(ServiceListener listener, String filter) {
        FilterData data = getFilterData(null, filter);
        if (data != null) {
            Long sidObject = Long.valueOf(data.m_serviceId);
            synchronized (m_sidToListenersMap) {
            	List<ServiceListener> listeners = m_sidToListenersMap.get(sidObject);
            	if (listeners == null) {
            		listeners = new ArrayList<>();
            		m_sidToListenersMap.put(sidObject, listeners);
            	}
            	listeners.add(listener);
            	m_listenerToFilterMap.put(listener, filter);
        		Matcher matcher = PATTERN.matcher(filter);
        		if (matcher.matches()) {
        			String objectClass = matcher.group(1);
        			m_listenerToObjectClassMap.put(listener, objectClass);
        		} else {
        			throw new IllegalArgumentException("Filter string does not match index pattern");
        		}

            }
        }
    }

	public void removeServiceListener(ServiceListener listener) {
        synchronized (m_sidToListenersMap) {
        	m_listenerToObjectClassMap.remove(listener);
            String filter = (String) m_listenerToFilterMap.remove(listener);
            if (filter != null) {
            	// the listener does exist
            	FilterData data = getFilterData(null, filter);
            	if (data != null) {
            		Long sidObject = Long.valueOf(data.m_serviceId);
            		List<ServiceListener> listeners = m_sidToListenersMap.get(sidObject);
            		if (listeners != null) {
            			listeners.remove(listener);
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

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("AdapterFilterIndex[");
        sb.append("S2L: " + m_sidToListenersMap.size());
        sb.append(", S2SR: " + m_sidToServiceReferencesMap.size());
        sb.append(", L2F: " + m_listenerToFilterMap.size());
        sb.append("]");
        return sb.toString();
    }
}
