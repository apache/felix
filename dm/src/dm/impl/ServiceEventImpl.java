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
			ServiceEventImpl sdi = (ServiceEventImpl) obj;
			return sdi.getReference().equals(getReference());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return getReference().hashCode();
	}

    @Override
    public int compareTo(Object b) {
        ServiceReference ra = (ServiceReference) getReference(), rb = ((ServiceEventImpl) b).getReference();
        int ranka = getRank(ra);
        int rankb = getRank(rb);
        if (ranka < rankb) {
            return -1;
        } else if (ranka > rankb) {
            return 1;
        }
        return 0;
    }
    
    private int getRank(ServiceReference ref) {
        Object ranking = ref.getProperty(Constants.SERVICE_RANKING);
        if (ranking != null && (ranking instanceof Integer)) {
            return ((Integer) ranking).intValue();
        }
        return 0;
    }
}