package dm.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import dm.Component;
import dm.ComponentStateListener;
import dm.Dependency;
import dm.DependencyManager;
import dm.context.ComponentContext;
import dm.context.ComponentState;
import dm.context.DependencyContext;

public class ComponentImpl implements Component, ComponentContext {
	private static final ServiceRegistration NULL_REGISTRATION = (ServiceRegistration) Proxy
			.newProxyInstance(ComponentImpl.class.getClassLoader(),
					new Class[] { ServiceRegistration.class },
					new DefaultNullObject());
    private static final Class[] VOID = new Class[] {};
	private final SerialExecutor m_executor = new SerialExecutor(new Logger(null));
	private ComponentState m_state = ComponentState.INACTIVE;
	private final List<DependencyContext> m_dependencies = new CopyOnWriteArrayList<>();
	private final List<ComponentStateListener> m_listeners = new CopyOnWriteArrayList<>();
	private boolean m_isStarted;
    private final Logger m_logger;
    private final BundleContext m_context;
    private final DependencyManager m_manager;
    private final Object SYNC = new Object();
	private Object m_componentDefinition;
	private Object m_componentInstance;
    private volatile Object m_serviceName;
    private volatile Dictionary m_serviceProperties;
    private volatile ServiceRegistration m_registration;
    private final Map m_autoConfig = new ConcurrentHashMap();
    private final Map m_autoConfigInstance = new ConcurrentHashMap();

    // configuration (static)
    private volatile String m_callbackInit;
    private volatile String m_callbackStart;
    private volatile String m_callbackStop;
    private volatile String m_callbackDestroy;
    private volatile Object m_callbackInstance;
    
    // instance factory
	private volatile Object m_instanceFactory;
	private volatile String m_instanceFactoryCreateMethod;
	
	// composition manager
	private volatile Object m_compositionManager;
	private volatile String m_compositionManagerGetMethod;
	private volatile Object m_compositionManagerInstance;

	public ComponentImpl() {
	    this(null, null, new Logger(null));
	}
	
    public ComponentImpl(BundleContext context, DependencyManager manager, Logger logger) {
        m_context = context;
        m_manager = manager;
        m_logger = logger;
        m_autoConfig.put(BundleContext.class, Boolean.TRUE);
        m_autoConfig.put(ServiceRegistration.class, Boolean.TRUE);
        m_autoConfig.put(DependencyManager.class, Boolean.TRUE);
        m_autoConfig.put(Component.class, Boolean.TRUE);
        m_callbackInit = "init";
        m_callbackStart = "start";
        m_callbackStop = "stop";
        m_callbackDestroy = "destroy";
    }

	public SerialExecutor getExecutor() {
		return m_executor;
	}
	
	@Override
	public Component add(final Dependency[] dependencies) {
		getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				List<DependencyContext> instanceBoundDeps = new ArrayList();
				for (Dependency d : dependencies) {
					DependencyContext dc = (DependencyContext) d;
					m_dependencies.add(dc);
					dc.add(ComponentImpl.this);
					if (!(m_state == ComponentState.INACTIVE)) {
						dc.setInstanceBound(true);
						instanceBoundDeps.add(dc);
					}
				}
				for (DependencyContext dc : instanceBoundDeps) {
					dc.start();
				}
				handleChange();
			}
		});
		return this;
	}

	@Override
	public Component remove(final Dependency d) {
		getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				DependencyContext dc = (DependencyContext) d;
				if (!(m_state == ComponentState.INACTIVE)) {
					dc.stop();
				}
				m_dependencies.remove(d);
				dc.remove(ComponentImpl.this);
				handleChange();
			}
		});
		return this;
	}

	public void start() {
		getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				m_isStarted = true;
				handleChange();
			}
		});
		
	}
	
	public void stop() {
		getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				m_isStarted = false;
				handleChange();
			}
		});
	}

	@Override
	public Component setInterface(String serviceName, Dictionary properties) {
		// ensureNotActive(); // TODO
	    m_serviceName = serviceName;
	    m_serviceProperties = properties;
	    return this;
	}

	@Override
	public Component setInterface(String[] serviceName, Dictionary properties) {
	    // ensureNotActive(); // TODO
	    m_serviceName = serviceName;
	    m_serviceProperties = properties;
	    return this;
	}

	@Override
	public void handleChange() {
		ComponentState oldState;
		ComponentState newState;
		do {
			oldState = m_state;
			newState = calculateNewState(oldState);
			m_state = newState;
		}
		while (performTransition(oldState, newState));
	}
	
    public Component setAutoConfig(Class clazz, boolean autoConfig) {
        m_autoConfig.put(clazz, Boolean.valueOf(autoConfig));
        return this;
    }
    
    public Component setAutoConfig(Class clazz, String instanceName) {
        m_autoConfig.put(clazz, Boolean.valueOf(instanceName != null));
        m_autoConfigInstance.put(clazz, instanceName);
        return this;
    }
    
    public boolean getAutoConfig(Class clazz) {
        Boolean result = (Boolean) m_autoConfig.get(clazz);
        return (result != null && result.booleanValue());
    }
    
    public String getAutoConfigInstance(Class clazz) {
        return (String) m_autoConfigInstance.get(clazz);
    }

	/** Based on the current state, calculate the new state. */
	private ComponentState calculateNewState(ComponentState currentState) {
		if (currentState == ComponentState.INACTIVE) {
			if (m_isStarted) {
				return ComponentState.WAITING_FOR_REQUIRED;
			}
		}
		if (currentState == ComponentState.WAITING_FOR_REQUIRED) {
			if (!m_isStarted) {
				return ComponentState.INACTIVE;
			}
			if (allRequiredAvailable()) {
				return ComponentState.INSTANTIATED_AND_WAITING_FOR_REQUIRED;
			}
		}
		if (currentState == ComponentState.INSTANTIATED_AND_WAITING_FOR_REQUIRED) {
			if (m_isStarted && allRequiredAvailable()) {
				if (allInstanceBoundAvailable()) {
					return ComponentState.TRACKING_OPTIONAL;
				}
				return currentState;
			}
			return ComponentState.WAITING_FOR_REQUIRED;
		}
		if (currentState == ComponentState.TRACKING_OPTIONAL) {
			if (m_isStarted && allRequiredAvailable() && allInstanceBoundAvailable()) {
				return currentState;
			}
			return ComponentState.INSTANTIATED_AND_WAITING_FOR_REQUIRED;
		}
		return currentState;
	}

	private boolean allRequiredAvailable() {
		boolean available = true;
		for (DependencyContext d : m_dependencies) {
			if (d.isRequired() && !d.isInstanceBound()) {
				if (!d.isAvailable()) {
					available = false;
					break;
				}
			}
		}
		return available;
	}

	private boolean allInstanceBoundAvailable() {
		boolean available = true;
		for (DependencyContext d : m_dependencies) {
			if (d.isRequired() && d.isInstanceBound()) {
				if (!d.isAvailable()) {
					available = false;
					break;
				}
			}
		}
		return available;
	}

    private boolean someDependenciesNeedInstance() {
        for (DependencyContext d : m_dependencies) {
            if (d.needsInstance()) {
                return true;
            }
        }
        return false;
    }

	/** Perform all the actions associated with state transitions. Returns true if a transition was performed. */
	private boolean performTransition(ComponentState oldState, ComponentState newState) {
//		System.out.println("transition from " + oldState + " to " + newState);
		if (oldState == ComponentState.INACTIVE && newState == ComponentState.WAITING_FOR_REQUIRED) {
			for (DependencyContext d : m_dependencies) {
			    if (d.needsInstance()) {
			        instantiateComponent();
			    }
				d.start();
			}
			notifyListeners(newState);
			return true;
		}
		if (oldState == ComponentState.WAITING_FOR_REQUIRED && newState == ComponentState.INSTANTIATED_AND_WAITING_FOR_REQUIRED) {
			instantiateComponent();
			invokeAddRequiredDependencies();
			invokeAutoConfigDependencies();
	        invoke(m_callbackInit);
			notifyListeners(newState);
			return true;
		}
		if (oldState == ComponentState.INSTANTIATED_AND_WAITING_FOR_REQUIRED && newState == ComponentState.TRACKING_OPTIONAL) {
			invokeAddRequiredInstanceBoundDependencies();
			invokeAutoConfigInstanceBoundDependencies();
			invoke(m_callbackStart);
			invokeAddOptionalDependencies();
			registerService();
			notifyListeners(newState);
			return true;
		}
		if (oldState == ComponentState.TRACKING_OPTIONAL && newState == ComponentState.INSTANTIATED_AND_WAITING_FOR_REQUIRED) {
			unregisterService();
			invoke(m_callbackStop);
			invokeRemoveInstanceBoundDependencies();
			notifyListeners(newState);
			return true;
		}
		if (oldState == ComponentState.INSTANTIATED_AND_WAITING_FOR_REQUIRED && newState == ComponentState.WAITING_FOR_REQUIRED) {
			invoke(m_callbackDestroy);
			invokeRemoveRequiredDependencies();
			notifyListeners(newState);
			if (! someDependenciesNeedInstance()) {
                destroyComponent();
            }
			return true;
		}
		if (oldState == ComponentState.WAITING_FOR_REQUIRED && newState == ComponentState.INACTIVE) {
			for (DependencyContext d : m_dependencies) {
				d.stop();
			}
            destroyComponent();
			notifyListeners(newState);
			return true;
		}
		return false;
	}
	
    private void registerService() {
        if (m_context != null && m_serviceName != null) {
            ServiceRegistrationImpl wrapper = new ServiceRegistrationImpl();
            m_registration = wrapper;
            if (((Boolean) m_autoConfig.get(ServiceRegistration.class)).booleanValue()) {
                configureImplementation(ServiceRegistration.class, m_registration, (String) m_autoConfigInstance.get(ServiceRegistration.class));
            }
            
            // service name can either be a string or an array of strings
            ServiceRegistration registration;

            // determine service properties
            Dictionary properties = calculateServiceProperties();

            // register the service
            try {
                if (m_serviceName instanceof String) {
                    registration = m_context.registerService((String) m_serviceName, m_componentInstance, properties);
                }
                else {
                    registration = m_context.registerService((String[]) m_serviceName, m_componentInstance, properties);
                }
                wrapper.setServiceRegistration(registration);
            }
            catch (IllegalArgumentException iae) {
                m_logger.log(Logger.LOG_ERROR, "Could not register service " + m_componentInstance, iae);
                // set the registration to an illegal state object, which will make all invocations on this
                // wrapper fail with an ISE (which also occurs when the SR becomes invalid)
                wrapper.setIllegalState();
            }
        }
    }

    private void unregisterService() {
        if (m_serviceName != null && m_registration != null) {
            m_registration.unregister();
            configureImplementation(ServiceRegistration.class, NULL_REGISTRATION);
            m_registration = null;
        }
    }

    private Dictionary calculateServiceProperties() {
		Dictionary properties = new Properties();
		addTo(properties, m_serviceProperties);
		for (int i = 0; i < m_dependencies.size(); i++) {
			DependencyContext d = (DependencyContext) m_dependencies.get(i);
			if (d.isPropagated() && d.isAvailable()) {
				Dictionary dict = d.getProperties();
				addTo(properties, dict);
			}
		}
		if (properties.size() == 0) {
			properties = null;
		}
		return properties;
	}

	private void addTo(Dictionary properties, Dictionary additional) {
		if (properties == null) {
			throw new IllegalArgumentException("Dictionary to add to cannot be null.");
		}
		if (additional != null) {
			Enumeration e = additional.keys();
			while (e.hasMoreElements()) {
				Object key = e.nextElement();
				properties.put(key, additional.get(key));
			}
		}
	}

	private void instantiateComponent() {
		// TODO add more complex factory instantiations of one or more components in a composition here
	    if (m_componentInstance == null) {
            if (m_componentDefinition instanceof Class) {
                try {
                    m_componentInstance = createInstance((Class) m_componentDefinition);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {
	        	if (m_instanceFactoryCreateMethod != null) {
	        		Object factory = null;
		        	if (m_instanceFactory != null) {
		        		if (m_instanceFactory instanceof Class) {
		        			try {
								factory = createInstance((Class) m_instanceFactory);
							}
		                    catch (Exception e) {
		                        m_logger.log(Logger.LOG_ERROR, "Could not create factory instance of class " + m_instanceFactory + ".", e);
		                    }
		        		}
		        		else {
		        			factory = m_instanceFactory;
		        		}
		        	}
		        	else {
		        		// TODO review if we want to try to default to something if not specified
		        	    // for now the JavaDoc of setFactory(method) reflects the fact that we need
		        	    // to review it
		        	}
		        	if (factory == null) {
                        m_logger.log(Logger.LOG_ERROR, "Factory cannot be null.");
		        	}
		        	else {
    		        	try {
    		        		m_componentInstance = InvocationUtil.invokeMethod(factory, factory.getClass(), m_instanceFactoryCreateMethod, new Class[][] {{}}, new Object[][] {{}}, false);
    					}
    		        	catch (Exception e) {
    	                    m_logger.log(Logger.LOG_ERROR, "Could not create service instance using factory " + factory + " method " + m_instanceFactoryCreateMethod + ".", e);
    					}
		        	}
	        	}
            }
            
            if (m_componentInstance == null) {
                m_componentInstance = m_componentDefinition;
            }
            
	        // configure the bundle context
	        if (((Boolean) m_autoConfig.get(BundleContext.class)).booleanValue()) {
	            configureImplementation(BundleContext.class, m_context, (String) m_autoConfigInstance.get(BundleContext.class));
	        }
            if (((Boolean) m_autoConfig.get(ServiceRegistration.class)).booleanValue()) {
                configureImplementation(ServiceRegistration.class, NULL_REGISTRATION, (String) m_autoConfigInstance.get(ServiceRegistration.class));
            }
            if (((Boolean) m_autoConfig.get(DependencyManager.class)).booleanValue()) {
                configureImplementation(DependencyManager.class, m_manager, (String) m_autoConfigInstance.get(DependencyManager.class));
            }
            if (((Boolean) m_autoConfig.get(Component.class)).booleanValue()) {
                configureImplementation(Component.class, this, (String) m_autoConfigInstance.get(Component.class));
            }
	    }
	}
	
	private void destroyComponent() {
		m_componentInstance = null;
	}
	
	private void invokeAddRequiredDependencies() {
		for (DependencyContext d : m_dependencies) {
			if (d.isRequired() && !d.isInstanceBound()) {
				d.invokeAdd();
			}
		}
	}
	
    private void invokeAutoConfigDependencies() {
        for (DependencyContext d : m_dependencies) {
            if (d.isAutoConfig() && !d.isInstanceBound()) {
                configureImplementation(d.getAutoConfigType(), d.getAutoConfigInstance(), d.getAutoConfigName());
            }
        }
    }
    
    private void invokeAutoConfigInstanceBoundDependencies() {
        for (DependencyContext d : m_dependencies) {
            if (d.isAutoConfig() && d.isInstanceBound()) {
                configureImplementation(d.getAutoConfigType(), d.getAutoConfigInstance(), d.getAutoConfigName());
            }
        }
    }
	
	private void invokeAddRequiredInstanceBoundDependencies() {
		for (DependencyContext d : m_dependencies) {
			if (d.isRequired() && d.isInstanceBound()) {
				d.invokeAdd();
			}
		}
	}
	
    private void invokeAddOptionalDependencies() {
        for (DependencyContext d : m_dependencies) {
            if (! d.isRequired()) {
                d.invokeAdd();
            }
        }
    }

	private void invokeRemoveRequiredDependencies() {
		for (DependencyContext d : m_dependencies) {
			if (!d.isInstanceBound()) {
				d.invokeRemove();
			}
		}
	}

	private void invokeRemoveInstanceBoundDependencies() {
		for (DependencyContext d : m_dependencies) {
			if (d.isInstanceBound()) {
				d.invokeRemove();
			}
		}
	}

	private void invoke(String name) {
        if (name != null) {
            // if a callback instance was specified, look for the method there, if not,
            // ask the service for its composition instances
        	
            invokeCallbackMethod(getInstances(), name, 
                new Class[][] {{ Component.class }, {}}, 
                new Object[][] {{ this }, {}});
        }
    }
    
    public Object[] getInstances() {
    	Object[] instances = m_callbackInstance != null ? new Object[] { m_callbackInstance } : getCompositionInstances();
    	return instances;
    }
    
    public void invokeCallbackMethod(Object[] instances, String methodName, Class[][] signatures, Object[][] parameters) {
        for (int i = 0; i < instances.length; i++) {
            try {
                InvocationUtil.invokeCallbackMethod(instances[i], methodName, signatures, parameters);
            }
            catch (NoSuchMethodException e) {
                // if the method does not exist, ignore it
            }
            catch (InvocationTargetException e) {
                // the method itself threw an exception, log that
                m_logger.log(Logger.LOG_ERROR, "Invocation of '" + methodName + "' failed.", e.getCause());
            }
            catch (Throwable e) {
                m_logger.log(Logger.LOG_ERROR, "Could not invoke '" + methodName + "'.", e);
            }
        }
    }

    private Object createInstance(Class clazz) throws SecurityException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Constructor constructor = clazz.getConstructor(VOID);
		constructor.setAccessible(true);
        return constructor.newInstance(null);
    }

	private void notifyListeners(ComponentState state) {
		for (ComponentStateListener l : m_listeners) {
			l.changed(state);
		}
	}
	
	private void notifyListener(ComponentStateListener l, ComponentState state) {
	    l.changed(state);
	}

	public boolean isAvailable() {
		return m_state == ComponentState.TRACKING_OPTIONAL;
	}

	@Override
	public Component add(final ComponentStateListener l) {
	    m_listeners.add(l);
	    return this;
	}

	@Override
	public Component remove(ComponentStateListener l) {
	    m_listeners.remove(l);
	    return this;
	}

	@Override
	public List<DependencyContext> getDependencies() {
		return m_dependencies;
	}

	@Override
	public Component setImplementation(Object implementation) {
		m_componentDefinition = implementation;
		return this;
	}
	
    private void configureImplementation(Class clazz, Object instance) {
        configureImplementation(clazz, instance, null);
    }

    /**
     * Configure a field in the service implementation. The service implementation
     * is searched for fields that have the same type as the class that was specified
     * and for each of these fields, the specified instance is filled in.
     *
     * @param clazz the class to search for
     * @param instance the instance to fill in
     * @param instanceName the name of the instance to fill in, or <code>null</code> if not used
     */
    private void configureImplementation(Class clazz, Object instance, String instanceName) {
        Object[] instances = getInstances();
        if (instances != null && instance != null && clazz != null) {
            for (int i = 0; i < instances.length; i++) {
                Object serviceInstance = instances[i];
                Class serviceClazz = serviceInstance.getClass();
                if (Proxy.isProxyClass(serviceClazz)) {
                    serviceInstance = Proxy.getInvocationHandler(serviceInstance);
                    serviceClazz = serviceInstance.getClass();
                }
                while (serviceClazz != null) {
                    Field[] fields = serviceClazz.getDeclaredFields();
                    for (int j = 0; j < fields.length; j++) {
                        Field field = fields[j];
                        Class type = field.getType();
                        if ((instanceName == null && type.equals(clazz)) 
                            || (instanceName != null && field.getName().equals(instanceName) && type.isAssignableFrom(clazz))) {
                            try {
                                field.setAccessible(true);
                                // synchronized makes sure the field is actually written to immediately
                                synchronized (SYNC) {
                                    field.set(serviceInstance, instance);
                                }
                            }
                            catch (Exception e) {
                                m_logger.log(Logger.LOG_ERROR, "Could not set field " + field, e);
                                return;
                            }
                        }
                    }
                    serviceClazz = serviceClazz.getSuperclass();
                }
            }
        }
    }

	@Override
	public ServiceRegistration getServiceRegistration() {
        return m_registration;
	}

	@Override
	public Object getService() {
		return m_componentInstance;
	}

	@Override
	public Dictionary getServiceProperties() {
		if (m_serviceProperties != null) {
			// Applied patch from FELIX-4304
			Hashtable serviceProperties = new Hashtable();
			addTo(serviceProperties, m_serviceProperties);
			return serviceProperties;
		}
		return null;
	}

	@Override
	public Component setServiceProperties(final Dictionary serviceProperties) {
	    getExecutor().execute(new Runnable() {
			@Override
			public void run() {
			    Dictionary properties = null;
		        m_serviceProperties = serviceProperties;
		        if ((m_registration != null) && (m_serviceName != null)) {
		            properties = calculateServiceProperties();
			        m_registration.setProperties(properties);
		        }
			}
		});
	    return this;
	}
	
	public Component setCallbacks(String init, String start, String stop, String destroy) {
	    // ensureNotActive(); // TODO
	    m_callbackInit = init;
	    m_callbackStart = start;
	    m_callbackStop = stop;
	    m_callbackDestroy = destroy;
	    return this;
	}
	
    public synchronized Component setCallbacks(Object instance, String init, String start, String stop, String destroy) {
	    // ensureNotActive(); // TODO
        m_callbackInstance = instance;
        m_callbackInit = init;
        m_callbackStart = start;
        m_callbackStop = stop;
        m_callbackDestroy = destroy;
        return this;
    }

	@Override
	public Component setFactory(Object factory, String createMethod) {
	    // ensureNotActive(); // TODO
		m_instanceFactory = factory;
		m_instanceFactoryCreateMethod = createMethod;
		return this;
	}

	@Override
	public Component setFactory(String createMethod) {
		return setFactory(null, createMethod);
	}

	@Override
	public Component setComposition(Object instance, String getMethod) {
	    // ensureNotActive(); // TODO
		m_compositionManager = instance;
		m_compositionManagerGetMethod = getMethod;
		return this;
	}

	@Override
	public Component setComposition(String getMethod) {
		return setComposition(null, getMethod);
	}

	private Object[] getCompositionInstances() {
        Object[] instances = null;
        if (m_compositionManagerGetMethod != null) {
            if (m_compositionManager != null) {
                m_compositionManagerInstance = m_compositionManager;
            }
            else {
                m_compositionManagerInstance = m_componentInstance;
            }
            if (m_compositionManagerInstance != null) {
                try {
                    instances = (Object[]) InvocationUtil.invokeMethod(m_compositionManagerInstance, m_compositionManagerInstance.getClass(), m_compositionManagerGetMethod, new Class[][] {{}}, new Object[][] {{}}, false);
                }
                catch (Exception e) {
                    m_logger.log(Logger.LOG_ERROR, "Could not obtain instances from the composition manager.", e);
                    instances = m_componentInstance == null ? new Object[] {} : new Object[] { m_componentInstance };
                }
            }
        }
        else {
            instances = m_componentInstance == null ? new Object[] {} : new Object[] { m_componentInstance };
        }
        return instances;
	}

	@Override
	public DependencyManager getDependencyManager() {
        return m_manager;
	}
}
