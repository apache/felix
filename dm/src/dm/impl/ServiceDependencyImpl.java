package dm.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.AbstractMap;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

import tracker.ServiceTracker;
import tracker.ServiceTrackerCustomizer;
import dm.Component;
import dm.ServiceDependency;
import dm.context.DependencyContext;
import dm.context.Event;

public class ServiceDependencyImpl extends DependencyImpl implements ServiceDependency, ServiceTrackerCustomizer {
	private volatile ServiceTracker m_tracker;
    private final BundleContext m_context;
    private final Logger m_logger;
    protected volatile Class<?> m_trackedServiceName;
    private volatile String m_trackedServiceFilter;
    private volatile String m_trackedServiceFilterUnmodified;
    private volatile ServiceReference m_trackedServiceReference;
    private volatile boolean m_propagate;
    private volatile Object m_propagateCallbackInstance;
    private volatile String m_propagateCallbackMethod;
    private volatile Object m_defaultImplementation;
    private volatile Object m_defaultImplementationInstance;
    private volatile Object m_nullObject;

    /**
     * Entry to wrap service properties behind a Map.
     */
    private static final class ServicePropertiesMapEntry implements Map.Entry {
        private final String m_key;
        private Object m_value;

        public ServicePropertiesMapEntry(String key, Object value) {
            m_key = key;
            m_value = value;
        }

        public Object getKey() {
            return m_key;
        }

        public Object getValue() {
            return m_value;
        }

        public String toString() {
            return m_key + "=" + m_value;
        }

        public Object setValue(Object value) {
            Object oldValue = m_value;
            m_value = value;
            return oldValue;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return false;
            }
            Map.Entry e = (Map.Entry) o;
            return eq(m_key, e.getKey()) && eq(m_value, e.getValue());
        }

        public int hashCode() {
            return ((m_key == null) ? 0 : m_key.hashCode()) ^ ((m_value == null) ? 0 : m_value.hashCode());
        }

        private static final boolean eq(Object o1, Object o2) {
            return (o1 == null ? o2 == null : o1.equals(o2));
        }
    }

    /**
     * Wraps service properties behind a Map.
     */
    private final static class ServicePropertiesMap extends AbstractMap {
        private final ServiceReference m_ref;

        public ServicePropertiesMap(ServiceReference ref) {
            m_ref = ref;
        }

        public Object get(Object key) {
            return m_ref.getProperty(key.toString());
        }

        public int size() {
            return m_ref.getPropertyKeys().length;
        }

        public Set entrySet() {
            Set set = new HashSet();
            String[] keys = m_ref.getPropertyKeys();
            for (int i = 0; i < keys.length; i++) {
                set.add(new ServicePropertiesMapEntry(keys[i], m_ref.getProperty(keys[i])));
            }
            return set;
        }
    }
    
	public ServiceDependencyImpl(BundleContext ctx, Logger logger) {
		super(true /* autoconfig */);
	    m_context = ctx;
	    m_logger = logger;
	}
	
	public ServiceDependencyImpl(ServiceDependencyImpl prototype) {
		super(prototype);
		m_logger = prototype.m_logger;
		m_context = prototype.m_context;	
        m_trackedServiceName = prototype.m_trackedServiceName;
        m_nullObject = prototype.m_nullObject;
        m_trackedServiceFilter = prototype.m_trackedServiceFilter;
        m_trackedServiceFilterUnmodified = prototype.m_trackedServiceFilterUnmodified;
        m_trackedServiceReference = prototype.m_trackedServiceReference;
        m_autoConfigInstance = prototype.m_autoConfigInstance;
        m_defaultImplementation = prototype.m_defaultImplementation;
        m_autoConfig = prototype.m_autoConfig;
	}
	    	    
    // --- CREATION
    
	public ServiceDependency setCallbacks(String add, String remove) {
		super.setCallbacks(add,  remove);
		return this;
	}
	
	public ServiceDependency setCallbacks(String add, String change, String remove) {
		super.setCallbacks(add, change, remove);
		return this;
	}
	
    public ServiceDependency setCallbacks(String added, String changed, String removed, String swapped) {
        setCallbacks(added, changed, removed);
        // TODO handle the swapped parameter.
        return this;
    }
	
	public ServiceDependency setCallbacks(Object instance, String add, String remove) {
		super.setCallbacks(instance, add, remove);
		return this;
	}
	
	public ServiceDependency setCallbacks(Object instance, String add, String change, String remove) {
		super.setCallbacks(instance, add, change, remove);
		return this;
	}
	
    public ServiceDependency setCallbacks(Object instance, String added, String changed, String removed, String swapped) {
        setCallbacks(instance, added, changed, removed);
        // TODO handle the swapped parameter
        return this;
    }

    public ServiceDependency setRequired(boolean required) {
		super.setRequired(required);
		return this;
	}
	
	public ServiceDependency setAutoConfig(boolean autoConfig) {
		super.setAutoConfig(autoConfig);
		return this;
	}
	
    public ServiceDependency setAutoConfig(String instanceName) {
    	super.setAutoConfig(instanceName);
    	return this;
    }

    @Override
    public ServiceDependency setDefaultImplementation(Object implementation) {
        ensureNotActive();
        m_defaultImplementation = implementation;
        return this;
    }   

    @Override
   	public ServiceDependency setService(Class serviceName) {
        setService(serviceName, null, null);
        return this;
    }

    public ServiceDependency setService(Class serviceName, String serviceFilter) {
        setService(serviceName, null, serviceFilter);
        return this;
    }

    public ServiceDependency setService(String serviceFilter) {
        if (serviceFilter == null) {
            throw new IllegalArgumentException("Service filter cannot be null.");
        }
        setService(null, null, serviceFilter);
        return this;
    }

    public ServiceDependency setService(Class serviceName, ServiceReference serviceReference) {
        setService(serviceName, serviceReference, null);
        return this;
    }

	@Override
	public void start() {
		boolean wasStarted = isStarted();
		super.start();

        if (!wasStarted) {
            if (m_trackedServiceName != null) {
                if (m_trackedServiceFilter != null) {
                    try {
                        m_tracker = new ServiceTracker(m_context, m_context.createFilter(m_trackedServiceFilter), this);
                    } catch (InvalidSyntaxException e) {
                        throw new IllegalStateException("Invalid filter definition for dependency: " + m_trackedServiceFilter);
                    }
                } else if (m_trackedServiceReference != null) {
                    m_tracker = new ServiceTracker(m_context, m_trackedServiceReference, this);
                } else {
                    m_tracker = new ServiceTracker(m_context, m_trackedServiceName.getName(), this);
                }
            } else {
                throw new IllegalStateException("Could not create tracker for dependency, no service name specified.");
            }
            m_tracker.open();
        }
	}
	
	@Override
	public void stop() {
		boolean wasStarted = isStarted();
		super.stop();
		if (wasStarted) {
			m_tracker.close();
			m_tracker = null;
		}
	}

	@Override
	public Object addingService(ServiceReference reference) {
		Object service = getBundleContext().getService(reference);
		return service;
	}

	@Override
	public void addedService(ServiceReference reference, Object service) {
		add(new ServiceEventImpl(reference, service));
	}

	@Override
	public void modifiedService(ServiceReference reference, Object service) {
		change(new ServiceEventImpl(reference, service));
	}

	@Override
	public void removedService(ServiceReference reference, Object service) {
		remove(new ServiceEventImpl(reference, service));
	}
	
	@Override
	public void invoke(String method, Event e) {
		ServiceEventImpl se = (ServiceEventImpl) e;
		m_component.invokeCallbackMethod(getInstances(), method,
		    new Class[][]{
            {Component.class, ServiceReference.class, m_trackedServiceName},
            {Component.class, ServiceReference.class, Object.class}, 
            {Component.class, ServiceReference.class},
            {Component.class, m_trackedServiceName}, 
            {Component.class, Object.class}, 
            {Component.class},
            {Component.class, Map.class, m_trackedServiceName},
            {ServiceReference.class, m_trackedServiceName},
            {ServiceReference.class, Object.class}, 
            {ServiceReference.class},
            {m_trackedServiceName}, 
            {Object.class}, 
            {},
            {Map.class, m_trackedServiceName}}, 
            
            new Object[][]{
		    {m_component, se.getReference(), se.getService()},
            {m_component, se.getReference(), se.getService()}, 
            {m_component, se.getReference()}, 
            {m_component, se.getService()},
            {m_component, se.getService()},
            {m_component},
            {m_component, new ServicePropertiesMap(se.getReference()), se.getService()},
            {se.getReference(), se.getService()},
            {se.getReference(), se.getService()}, 
            {se.getReference()}, 
            {se.getService()}, 
            {se.getService()}, 
            {},
            {new ServicePropertiesMap(se.getReference()), se.getService()}}
		);
	}
	
	@Override
    public Class<?> getAutoConfigType() {
        return m_trackedServiceName;
    }
	
    @Override
    public DependencyContext createCopy() {
        return new ServiceDependencyImpl(this);
    }
    
    @Override
    public synchronized String toString() {
        return "ServiceDependency[" + m_trackedServiceName + " " + m_trackedServiceFilterUnmodified + "]";
    }

    @Override
    public String getName() {
        StringBuilder sb = new StringBuilder();
        if (m_trackedServiceName != null) {
            sb.append(m_trackedServiceName.getName());
            if (m_trackedServiceFilterUnmodified != null) {
                sb.append(' ');
                sb.append(m_trackedServiceFilterUnmodified);
            }
        }
        if (m_trackedServiceReference != null) {
            sb.append("{service.id=" + m_trackedServiceReference.getProperty(Constants.SERVICE_ID) + "}");
        }
        return sb.toString();
    }
    
    @Override
    public String getType() {
        return "service";
    }

	@Override
	public ServiceDependency setPropagate(boolean propagate) {
        ensureNotActive();
        m_propagate = propagate;
        return this;
	}

	@Override
	public ServiceDependency setPropagate(Object instance, String method) {
        setPropagate(instance != null && method != null);
        m_propagateCallbackInstance = instance;
        m_propagateCallbackMethod = method;
        return this;
	}
	
	@Override
    public Dictionary getProperties() {
        Object service = null;
        ServiceEventImpl se = m_dependencies.size() > 0 ? (ServiceEventImpl) m_dependencies.first() : null;
        if (se != null) {
            if (m_propagateCallbackInstance != null && m_propagateCallbackMethod != null) {
                try {
                    return (Dictionary) InvocationUtil.invokeCallbackMethod(m_propagateCallbackInstance, m_propagateCallbackMethod,
                            new Class[][]{{ServiceReference.class, Object.class}, {ServiceReference.class}}, new Object[][]{
                                    {se.getReference(), se.getService()}, {se.getReference()}});
                } catch (InvocationTargetException e) {
                    m_logger.log(LogService.LOG_WARNING, "Exception while invoking callback method", e.getCause());
                } catch (Exception e) {
                    m_logger.log(LogService.LOG_WARNING, "Exception while trying to invoke callback method", e);
                }
                throw new IllegalStateException("Could not invoke callback");
            } else {
                Properties props = new Properties();
                String[] keys = se.getReference().getPropertyKeys();
                for (int i = 0; i < keys.length; i++) {
                    if (!(keys[i].equals(Constants.SERVICE_ID) || keys[i].equals(Constants.SERVICE_PID))) {
                        props.put(keys[i], se.getReference().getProperty(keys[i]));
                    }
                }
                return props;
            }
        } else {
            throw new IllegalStateException("cannot find service reference");
        }
    }	

    @Override
    public boolean isPropagated() {
        return m_propagate;
    }

    private BundleContext getBundleContext() {
        return m_context;
    }
    
    /** Internal method to set the name, service reference and/or filter. */
    private void setService(Class serviceName, ServiceReference serviceReference, String serviceFilter) {
        ensureNotActive();
        if (serviceName == null) {
            m_trackedServiceName = Object.class;
        }
        else {
            m_trackedServiceName = serviceName;
        }
        if (serviceFilter != null) {
            m_trackedServiceFilterUnmodified = serviceFilter;
            if (serviceName == null) {
                m_trackedServiceFilter = serviceFilter;
            }
            else {
                m_trackedServiceFilter = "(&(" + Constants.OBJECTCLASS + "=" + serviceName.getName() + ")"
                    + serviceFilter + ")";
            }
        }
        else {
            m_trackedServiceFilterUnmodified = null;
            m_trackedServiceFilter = null;
        }
        if (serviceReference != null) {
            m_trackedServiceReference = serviceReference;
            if (serviceFilter != null) {
                throw new IllegalArgumentException("Cannot specify both a filter and a service reference.");
            }
        }
        else {
            m_trackedServiceReference = null;
        }
    }
    
    @Override
    protected Object getService() {
        Object service = null;
        ServiceEventImpl se = m_dependencies.size() > 0 ? (ServiceEventImpl) m_dependencies.last() : null;
        if (se != null) {
            service = se.getService();
        } else if (isAutoConfig()) {
            service = getDefaultImplementation();
            if (service == null) {
                service = getNullObject();
            }
        }
        return service;
    }
    
    private Object getNullObject() {
        if (m_nullObject == null) {
            Class trackedServiceName;
            synchronized (this) {
                trackedServiceName = m_trackedServiceName;
            }
            try {
                m_nullObject = Proxy.newProxyInstance(trackedServiceName.getClassLoader(),
                    new Class[] { trackedServiceName }, new DefaultNullObject());
            }
            catch (Exception e) {
                m_logger.log(Logger.LOG_ERROR, "Could not create null object for " + trackedServiceName + ".", e);
            }
        }
        return m_nullObject;
    }

    private Object getDefaultImplementation() {
        if (m_defaultImplementation != null) {
            if (m_defaultImplementation instanceof Class) {
                try {
                    m_defaultImplementationInstance = ((Class) m_defaultImplementation).newInstance();
                }
                catch (Exception e) {
                    m_logger.log(Logger.LOG_ERROR, "Could not create default implementation instance of class "
                        + m_defaultImplementation + ".", e);
                }
            }
            else {
                m_defaultImplementationInstance = m_defaultImplementation;
            }
        }
        return m_defaultImplementationInstance;
    }

    private void ensureNotActive() {
        if (m_tracker != null) {
            throw new IllegalStateException("Cannot modify state while active.");
        }
    }
}
