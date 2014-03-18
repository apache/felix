package dm.impl;

import java.net.URL;
import java.util.Dictionary;

public class ResourceEventImpl extends EventImpl implements Comparable {
    final URL m_resource;
    final Dictionary<?, ?> m_resourceProperties;
    
    public ResourceEventImpl(URL resource, Dictionary<?,?> resourceProperties) {
        m_resource = resource;
        m_resourceProperties = resourceProperties;
    }
    
    public URL getResource() {
        return m_resource;
    }
    
    public Dictionary<?, ?> getResourceProperties() {
        return m_resourceProperties;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ResourceEventImpl) {
            ResourceEventImpl r1 = this;
            ResourceEventImpl r2 = (ResourceEventImpl) obj;
            boolean match = r1.getResource().equals(r2.getResource());
            if (match) {
                Dictionary<?,?> d1 = getResourceProperties();
                Dictionary<?,?> d2 = r2.getResourceProperties();

                if (d1 == null && d2 == null) {
                    return match;
                }
                
                if (d1 == null && d2 != null) {
                    return false;
                }
                
                if (d1 != null && d2 == null) {
                    return false;
                }
                return d1.equals(d2);
            }
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getResource().hashCode();
        result = prime * result + ((getResourceProperties() == null) ? 0 : getResourceProperties().hashCode());
        return result;
    }

    @Override
    public int compareTo(Object that) {
        if (this.equals(that)) {
            return 0;
        }
        
        // Sort by resource name.
        return getResource().toString().compareTo(that.toString());
    }
}
