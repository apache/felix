package org.apache.felix.dm.index;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * A filter index is an interface you can implement to create your own, optimized index for specific filter expressions.
 */
public interface FilterIndex {
    /** Opens this filter index. */
    public void open(BundleContext context);
    /** Closes this filter index. */
    public void close();
    /** Determines if the combination of class and filter is applicable for this filter index. */
    public boolean isApplicable(String clazz, String filter);
    /** Returns all service references that match the specified class and filter. */
    public ServiceReference[] getAllServiceReferences(String clazz, String filter);
    /** Invoked whenever a service event occurs. */
    public void serviceChanged(ServiceEvent event);
    /** Adds a service listener to this filter index. */
    public void addServiceListener(ServiceListener listener, String filter);
    /** Removes a service listener from this filter index. */
    public void removeServiceListener(ServiceListener listener);
}
