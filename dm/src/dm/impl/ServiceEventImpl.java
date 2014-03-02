package dm.impl;

import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class ServiceEventImpl extends EventImpl implements Comparable {
	private final ServiceReference m_reference;
	private final Object m_service;

	public ServiceEventImpl(ServiceReference reference, Object service) {
		m_reference = reference;
		m_service = service;
	}
	
	public ServiceReference getReference() {
		return m_reference;
	}
	
	public Object getService() {
		return m_service;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ServiceEventImpl) {
			return getReference().equals(((ServiceEventImpl) obj).getReference());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return getReference().hashCode();
	}

    @Override
    public int compareTo(Object b) {
    	return getReference().compareTo(((ServiceEventImpl) b).getReference());
    }
        
    public String toString() {
    	return m_service.toString();
    }
}