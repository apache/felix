package dm.impl;

import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

// Todo implements some methods from DependencyImpl (createCopy, etc ...)
public class ConfigurationDependencyImpl extends DependencyImpl implements ManagedService {
    private Dictionary m_settings;
    private String m_callback = "updated";

    public ConfigurationDependencyImpl() {
        setRequired(true);
    }

    public ConfigurationDependencyImpl setCallback(String callback) {
        m_callback = callback;
        return this;
    }

    @Override
    public boolean needsInstance() {
        return true;
    }

    @Override
    public void start() {
        super.start();
        // Register our managed service in the service registry
    }

    @Override
    public void stop() {
        super.stop();
        // Unregister our managed service from the service registry
    }

    @Override
    public void updated(Dictionary settings) throws ConfigurationException {
        Dictionary<?,?> oldSettings = null;
        synchronized (this) {
            oldSettings = m_settings;
        }

        if (oldSettings == null && settings == null) {
            // CM has started but our configuration is not still present in the CM database: ignore
            return;
        }

        // If this is initial settings, or a configuration update, we handle it synchronously.
        // We'll conclude that the dependency is available only if invoking updated did not cause
        // any ConfigurationException.
        if (settings != null) {
            Object[] instances = m_component.getInstances();
            if (instances != null) {
                invokeUpdated(settings);
            }
        }
        
        // At this point, we have accepted the configuration.
        synchronized (this) {
            m_settings = settings;
        }

        if ((oldSettings == null) && (settings != null)) {
            // Notify the component that our dependency is available.
            add(new EventImpl());
        }
        else if ((oldSettings != null) && (settings != null)) {
            // Notify the component that our dependency has changed.
            change(new EventImpl());
        }
        else if ((oldSettings != null) && (settings == null)) {
            // Notify the component that our dependency has been removed.
            // Notice that the component will be stopped, and then all required dependencies will be unbound
            // (including our configuration dependency).
            remove(new EventImpl());
        }
    }

    public void invokeAdd() {
        // We already did that synchronously, from our updated method
    }

    public void invokeChange() {
        // We already did that synchronously, from our updated method
    }

    public void invokeRemove() {
        // The configuration has gone, so, the state machine has stopped the component,
        // and all required dependencies must now be removed, including our configuration 
        // dependency.
        try {
            invokeUpdated(null);
        } catch (ConfigurationException e) {
            e.printStackTrace(); // FIXME use a LogService
        }
    }
    
    private void invokeUpdated(Dictionary settings) throws ConfigurationException {
        Object[] instances = m_component.getInstances();
        if (instances != null) {
            for (int i = 0; i < instances.length; i++) {
                try {
                    InvocationUtil.invokeCallbackMethod(instances[i], m_callback, 
                        new Class[][] { { Dictionary.class }, {} }, 
                        new Object[][] { { settings }, {} });
                }

                catch (InvocationTargetException e) {
                    // The component has thrown an exception during it's callback invocation.
                    if (e.getTargetException() instanceof ConfigurationException) {
                        // the callback threw an OSGi ConfigurationException: just re-throw it.
                        throw (ConfigurationException) e.getTargetException();
                    }
                    else {
                        // wrap the callback exception into a ConfigurationException.
                        throw new ConfigurationException(null, "Configuration update failed", e.getTargetException());
                    }
                }
                catch (NoSuchMethodException e) {
                    // if the method does not exist, ignore it
                }
                catch (Throwable t) {
                    // wrap any other exception as a ConfigurationException.
                    throw new ConfigurationException(null, "Configuration update failed", t);
                }
            }
        }
    }
}
