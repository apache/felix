package org.apache.felix.dm.index;

import java.util.Map.Entry;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;

public class FilterIndexBundleContext extends BundleContextInterceptorBase {
    public FilterIndexBundleContext(BundleContext context) {
        super(context);
    }

    public void serviceChanged(ServiceEvent event) {
        Entry[] entries = synchronizeCollection();
        for (int i = 0; i < entries.length; i++) {
            Entry serviceListenerFilterEntry = entries[i];
            ServiceListener serviceListener = (ServiceListener) serviceListenerFilterEntry.getKey();
            String filter = (String) serviceListenerFilterEntry.getValue();
            if (filter == null) {
                serviceListener.serviceChanged(event);
            }
            else {
                // call service changed on the listener if the filter matches the event
                // TODO review if we can be smarter here
                try {
                    if ("(objectClass=*)".equals(filter)) {
                        serviceListener.serviceChanged(event);
                    }
                    else {
                        if (m_context.createFilter(filter).match(event.getServiceReference())) {
                            serviceListener.serviceChanged(event);
                        }
                    }
                }
                catch (InvalidSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
