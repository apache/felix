package dm.impl;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;

public class BundleEventImpl extends EventImpl implements Comparable {
    final Bundle m_bundle;
    final BundleEvent m_event;
    
    public BundleEventImpl(Bundle bundle, BundleEvent event) {
        m_bundle = bundle;
        m_event = event;
    }
    
    public Bundle getBundle() {
        return m_bundle;
    }
    
    public BundleEvent getBundleEvent() {
        return m_event;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BundleEventImpl) {
            return getBundle().getBundleId() == ((BundleEventImpl) obj).getBundle().getBundleId();
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return getBundle().hashCode();
    }

    @Override
    public int compareTo(Object b) {
        return Long.compare(getBundle().getBundleId(), ((BundleEventImpl) b).getBundle().getBundleId());
    }
}
