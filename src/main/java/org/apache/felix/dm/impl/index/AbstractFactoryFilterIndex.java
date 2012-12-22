package org.apache.felix.dm.impl.index;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.felix.dm.ServiceUtil;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public abstract class AbstractFactoryFilterIndex {

	protected final Map /* <Long, SortedSet<ServiceReference>> */ m_sidToServiceReferencesMap = new HashMap();
	protected final Map /* <ServiceListener, String> */ m_listenerToFilterMap = new HashMap();

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
    
    protected boolean referenceMatchesObjectClass(ServiceReference ref, String objectClass) {
    	boolean matches = false;
    	Object value = ref.getProperty(Constants.OBJECTCLASS);
    	matches = Arrays.asList((String[])value).contains(objectClass);
    	return matches;
    }
    
    /** Structure to hold internal filter data. */
    protected static class FilterData {
        public long serviceId;
        public String objectClass;
        public int ranking;

		public String toString() {
			return "FilterData [serviceId=" + serviceId + "]";
		}
    }
}
