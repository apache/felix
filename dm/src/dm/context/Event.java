package dm.context;

import org.osgi.framework.BundleContext;

/** 
 * An event holds all data that belongs to some external event as it comes in via
 * the 'changed' callback of a dependency.
 */
public interface Event extends Comparable {
    /**
     * Release the resources this event is holding (like service reference for example).
     * @param m_context the bundle context possibly used to clean some service references.s
     */
    public void close(BundleContext context);
}
