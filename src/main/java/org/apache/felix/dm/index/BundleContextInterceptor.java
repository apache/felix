package org.apache.felix.dm.index;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class BundleContextInterceptor extends BundleContextInterceptorBase {
    private final ServiceRegistryCache m_cache;

    public BundleContextInterceptor(ServiceRegistryCache cache, BundleContext context) {
        super(context);
        m_cache = cache;
    }

    public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
        FilterIndex filterIndex = m_cache.hasFilterIndexFor(null, filter);
        if (filterIndex != null) {
            filterIndex.addServiceListener(listener, filter);
        }
        else {
            m_context.addServiceListener(listener, filter);
//            super.addServiceListener(listener, filter);
        }
    }

    public void addServiceListener(ServiceListener listener) {
        FilterIndex filterIndex = m_cache.hasFilterIndexFor(null, null);
        if (filterIndex != null) {
            filterIndex.addServiceListener(listener, null);
        }
        else {
            m_context.addServiceListener(listener);
//            super.addServiceListener(listener);
        }
    }

    public void removeServiceListener(ServiceListener listener) {
        FilterIndex filterIndex = m_cache.hasFilterIndexFor(null, null);
        if (filterIndex != null) {
            filterIndex.removeServiceListener(listener);
        }
        else {
            m_context.removeServiceListener(listener);
//            super.removeServiceListener(listener);
        }
    }

    public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        // first we ask the cache if there is an index for our request (class and filter combination)
        FilterIndex filterIndex = m_cache.hasFilterIndexFor(clazz, filter);
        if (filterIndex != null) {
            ServiceReference[] result = filterIndex.getAllServiceReferences(clazz, filter);
            // TODO filter for assignability
            return result;
        }
        else {
            // if they don't know, we ask the real bundle context instead
            return m_context.getServiceReferences(clazz, filter);
        }
        
    }

    public ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        // TODO implement
        return m_context.getAllServiceReferences(clazz, filter);
    }

    public ServiceReference getServiceReference(String clazz) {
        // TODO implement
        return m_context.getServiceReference(clazz);
    }

    public void serviceChanged(ServiceEvent event) {
        m_cache.serviceChangedForFilterIndices(event);
//        Entry[] entries = synchronizeCollection();
//        for (int i = 0; i < entries.length; i++) {
//            Entry serviceListenerFilterEntry = entries[i];
//            ServiceListener serviceListener = (ServiceListener) serviceListenerFilterEntry.getKey();
//            String filter = (String) serviceListenerFilterEntry.getValue();
//            if (filter == null) {
//                serviceListener.serviceChanged(event);
//            }
//            else {
//                try {
//                    if (m_context.createFilter(filter).match(event.getServiceReference())) {
//                        serviceListener.serviceChanged(event);
//                    }
//                }
//                catch (InvalidSyntaxException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
    }
}
