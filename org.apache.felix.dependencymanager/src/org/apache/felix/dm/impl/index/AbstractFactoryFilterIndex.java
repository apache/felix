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
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.felix.dm.impl.ServiceUtil;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class AbstractFactoryFilterIndex {
	protected final Map<Long, SortedSet<ServiceReference>> m_sidToServiceReferencesMap = new HashMap<>();
	protected final Map <ServiceListener, String> m_listenerToFilterMap = new HashMap<>();

    public void addedService(ServiceReference reference, Object service) {
        add(reference);
    }

    public void modifiedService(ServiceReference reference, Object service) {
        modify(reference);
    }

    public void removedService(ServiceReference reference, Object service) {
        remove(reference);
    }
    
	public void swappedService(ServiceReference reference, Object service,
			ServiceReference newReference, Object newService) {
		addedService(newReference, newService);
		removedService(reference, service);
	}
    
	public void add(ServiceReference reference) {
        Long sid = ServiceUtil.getServiceIdObject(reference);
        synchronized (m_sidToServiceReferencesMap) {
            SortedSet<ServiceReference> list = m_sidToServiceReferencesMap.get(sid);
            if (list == null) {
                list = new TreeSet<ServiceReference>();
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
            SortedSet<ServiceReference> list = m_sidToServiceReferencesMap.get(sid);
            if (list != null) {
                list.remove(reference);
            }
        }
    }
    
    protected boolean referenceMatchesObjectClass(ServiceReference ref, String objectClass) {
    	boolean matches = false;
    	Object value = ref.getProperty(Constants.OBJECTCLASS);
    	matches = Arrays.asList((String[])value).contains(objectClass);
    	return matches;
    }
    
    /** Structure to hold internal filter data. */
    protected static class FilterData {
        public long m_serviceId;
        public String m_objectClass;
        public int m_ranking;

		public String toString() {
			return "FilterData [serviceId=" + m_serviceId + "]";
		}
    }
}
