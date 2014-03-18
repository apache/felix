package dm.impl;

import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.osgi.framework.Bundle;

import dm.Component;
import dm.ComponentStateListener;
import dm.Dependency;
import dm.DependencyManager;
import dm.context.DependencyContext;

/**
 * Bundle Adapter Service implementation. This class extends the FilterService in order to catch
 * some Service methods for configuring actual adapter service implementation.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class BundleAdapterImpl extends FilterComponent
{
    /**
     * Creates a new Bundle Adapter Service implementation.
     */
    public BundleAdapterImpl(DependencyManager dm, int bundleStateMask, String bundleFilter, boolean propagate)
    {
        super(dm.createComponent()); // This service will be filtered by our super class, allowing us to take control.
        m_component.setImplementation(new BundleAdapterDecorator(bundleStateMask, bundleFilter, propagate))
                 .add(dm.createBundleDependency()
                      .setFilter(bundleFilter)
                      .setStateMask(bundleStateMask)
                      .setCallbacks("added", "removed"))
                 .setCallbacks("init", null, "stop", null);
    }

    public class BundleAdapterDecorator extends AbstractDecorator {
        private final boolean m_propagate;
        private final int m_bundleStateMask;
        private final String m_bundleFilter;

        public BundleAdapterDecorator(int bundleStateMask, String bundleFilter, boolean propagate) {
            m_bundleStateMask = bundleStateMask;
            m_bundleFilter = bundleFilter;
            m_propagate = propagate;
        }
        
        public Component createService(Object[] properties) {
            Bundle bundle = (Bundle) properties[0];
            Properties props = new Properties();
            if (m_serviceProperties != null) {
                Enumeration e = m_serviceProperties.keys();
                while (e.hasMoreElements()) {
                    Object key = e.nextElement();
                    props.put(key, m_serviceProperties.get(key));
                }
            }
            List<DependencyContext> dependencies = m_component.getDependencies();
            // the first dependency is always the dependency on the bundle, which
            // will be replaced with a more specific dependency below
            dependencies.remove(0);
            Component service = m_manager.createComponent()
                .setInterface(m_serviceInterfaces, props)
                .setImplementation(m_serviceImpl)
                .setFactory(m_factory, m_factoryCreateMethod) // if not set, no effect
                .setComposition(m_compositionInstance, m_compositionMethod) // if not set, no effect
                .setCallbacks(m_callbackObject, m_init, m_start, m_stop, m_destroy) // if not set, no effect
                .add(m_manager.createBundleDependency()
                    .setBundle(bundle)
                    .setStateMask(m_bundleStateMask)
                    .setPropagate(m_propagate)
                    .setCallbacks(null, "changed", null)
                    .setAutoConfig(true)
                    .setRequired(true));

            for (DependencyContext dc : dependencies) {
                service.add((Dependency) dc.createCopy());
            }

            for (ComponentStateListener stateListener : m_stateListeners) {
                service.add(stateListener);
            }
            configureAutoConfigState(service, m_component);
            return service;
        }
    }
}
