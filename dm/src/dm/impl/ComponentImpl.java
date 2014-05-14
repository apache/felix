package dm.impl;

import static dm.ComponentState.INACTIVE;
import static dm.ComponentState.INSTANTIATED_AND_WAITING_FOR_REQUIRED;
import static dm.ComponentState.TRACKING_OPTIONAL;
import static dm.ComponentState.WAITING_FOR_REQUIRED;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import dm.Component;
import dm.ComponentDeclaration;
import dm.ComponentDependencyDeclaration;
import dm.ComponentState;
import dm.ComponentStateListener;
import dm.Dependency;
import dm.DependencyManager;
import dm.context.ComponentContext;
import dm.context.DependencyContext;
import dm.context.Event;

public class ComponentImpl implements Component, ComponentContext, ComponentDeclaration {
	private static final ServiceRegistration NULL_REGISTRATION = (ServiceRegistration) Proxy
			.newProxyInstance(ComponentImpl.class.getClassLoader(),
					new Class[] { ServiceRegistration.class },
					new DefaultNullObject());
    private static final Class[] VOID = new Class[] {};
	private volatile Executor m_executor = new SerialExecutor(new Logger(null));
	private ComponentState m_state = ComponentState.INACTIVE;
	private final CopyOnWriteArrayList<DependencyContext> m_dependencies = new CopyOnWriteArrayList<>();
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
    private final long m_id;
    private static AtomicLong m_idGenerator = new AtomicLong();
    private final Map<DependencyContext, ConcurrentSkipListSet<Event>> m_dependencyEvents = new HashMap<>();

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
    private boolean m_handlingChange;

    static class SCDImpl implements ComponentDependencyDeclaration {
        private final String m_name;
        private final int m_state;
        private final String m_type;

        public SCDImpl(String name, int state, String type) {
            m_name = name;
            m_state = state;
            m_type = type;
        }

        public String getName() {
            return m_name;
        }

        public int getState() {
            return m_state;
        }

        public String getType() {
            return m_type;
        }
    }

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
        m_id = m_idGenerator.getAndIncrement();
    }

	public Executor getExecutor() {
		return m_executor;
	}
	
	public Component setExecutor(Executor executor) {
	    m_executor = executor;
	    return this;
	}
	
	@Override
	public Component add(final Dependency[] dependencies) {
		getExecutor().execute(new Runnable() {
			@Override
			public void run() {
				List<DependencyContext> instanceBoundDeps = new ArrayList();
				for (Dependency d : dependencies) {
					DependencyContext dc = (DependencyContext) d;
					m_dependencyEvents.put(dc,  new ConcurrentSkipListSet<Event>());
					m_dependencies.add(dc);
					dc.add(ComponentImpl.this);
					if (!(m_state == ComponentState.INACTIVE)) {
						dc.setInstanceBound(true);
						instanceBoundDeps.add(dc);
					}
				}
				startDependencies(instanceBoundDeps);
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
                m_dependencyEvents.remove(d);
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
	public void handleAdded(DependencyContext dc, Event e) {
	    Set<Event> dependencyEvents = m_dependencyEvents.get(dc);
	    dependencyEvents.add(e);
	    dc.setAvailable(true);
	    
        switch (m_state) {
        case WAITING_FOR_REQUIRED:
            if (dc.isRequired())
                handleChange();
            break;
        case INSTANTIATED_AND_WAITING_FOR_REQUIRED:
            if (!dc.isInstanceBound()) {
                if (dc.isRequired()) {
                    dc.invokeAdd(e);
                }
                updateInstance(dc);
            }
            else {
                if (dc.isRequired())
                    handleChange();
            }
            break;
        case TRACKING_OPTIONAL:
            dc.invokeAdd(e);
            updateInstance(dc);
            break;
        default:
        }
	}
	
    @Override
    public void handleChanged(DependencyContext dc, Event e) {
        Set<Event> dependencyEvents = m_dependencyEvents.get(dc);
        dependencyEvents.remove(e);
        dependencyEvents.add(e);
        
        switch (m_state) {
        case TRACKING_OPTIONAL:
            dc.invokeChange(e);
            updateInstance(dc);
            break;

        case INSTANTIATED_AND_WAITING_FOR_REQUIRED:
            if (!dc.isInstanceBound()) {
                dc.invokeChange(e);
                updateInstance(dc);
            }
            break;
        }
    }

    @Override
    public void handleRemoved(DependencyContext dc, Event e) {
        Set<Event> dependencyEvents = m_dependencyEvents.get(dc);
        int size = dependencyEvents.size();
        if (dependencyEvents.contains(e)) {
            size --; // the dependency is currently registered and is about to be removed.
        }
        dc.setAvailable(size > 0);
        handleChange();
        
        // Now, really remove the dependency event, because next, we'll possibly invoke updateInstance, which will
        // trigger getAutoConfigInstance, and at this point, we don't want to return the removed service, which might
        // be the highest ranked service.
        dependencyEvents.remove(e);
        
        // Depending on the state, we possible have to invoke the callbacks and update the component instance.        
        switch (m_state) {
        case INSTANTIATED_AND_WAITING_FOR_REQUIRED:
            if (! dc.isInstanceBound()) {
                if (dc.isRequired()) {            
                    dc.invokeRemove(e);
                }
                updateInstance(dc);
            }
            break;
        case TRACKING_OPTIONAL:
            dc.invokeRemove(e);
            updateInstance(dc);
            break;
        default:
        }
    }
	       
    @Override
    public Event getDependencyEvent(DependencyContext dc) {
        ConcurrentSkipListSet<Event> events = m_dependencyEvents.get(dc);
        return events.size() > 0 ? events.last() : null;
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

    private void handleChange() {
        // At this point, our component is starting, stopping, or a dependency is being added/changed/removed. 
        // So, we have to calculate a new state change for this component.
        // Now, if we decide to call the component's init method, then at this point, if the component adds
        // some additional instance-bound (and *available*) dependencies, then this will trigger a recursive call to 
        // our handleChange method, which we are currently executing. Since this would mess around with the execution of 
        // our current handleChange method execution , we are using a special "m_handlingChange" flag, which avoids this 
        // kind of problem. 
        
        if (! m_handlingChange) {
            try {
                m_handlingChange = true;
                ComponentState oldState;
                ComponentState newState;
                do {
                    oldState = m_state;
                    newState = calculateNewState(oldState);
                    m_state = newState;
                }
                while (performTransition(oldState, newState));
            } finally {
                m_handlingChange = false;
            }
        }
    }
    
	/** Based on the current state, calculate the new state. */
	private ComponentState calculateNewState(ComponentState currentState) {
		if (currentState == INACTIVE) {
			if (m_isStarted) {
				return WAITING_FOR_REQUIRED;
			}
		}
		if (currentState == WAITING_FOR_REQUIRED) {
			if (!m_isStarted) {
				return INACTIVE;
			}
			if (allRequiredAvailable()) {
				return INSTANTIATED_AND_WAITING_FOR_REQUIRED;
			}
		}
		if (currentState == INSTANTIATED_AND_WAITING_FOR_REQUIRED) {
			if (m_isStarted && allRequiredAvailable()) {
				if (allInstanceBoundAvailable()) {
					return TRACKING_OPTIONAL;
				}
				return currentState;
			}
			return WAITING_FOR_REQUIRED;
		}
		if (currentState == TRACKING_OPTIONAL) {
			if (m_isStarted && allRequiredAvailable() && allInstanceBoundAvailable()) {
				return currentState;
			}
			return INSTANTIATED_AND_WAITING_FOR_REQUIRED;
		}
		return currentState;
	}

	/** Perform all the actions associated with state transitions. Returns true if a transition was performed. */
	private boolean performTransition(ComponentState oldState, ComponentState newState) {
//		System.out.println("transition from " + oldState + " to " + newState);
		if (oldState == ComponentState.INACTIVE && newState == ComponentState.WAITING_FOR_REQUIRED) {
		    startDependencies(m_dependencies);
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
		    invokeRemoveOptionalDependencies();
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
		    stopDependencies();
            destroyComponent();
			notifyListeners(newState);
			return true;
		}
		return false;
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

    private void updateInstance(DependencyContext d) {
        if (d.isAutoConfig()) {
            configureImplementation(d.getAutoConfigType(), d.getAutoConfigInstance(), d.getAutoConfigName());
        }
        if (d.isPropagated() && m_registration != null) {
            m_registration.setProperties(calculateServiceProperties());
        }
    }
    
    private void startDependencies(List<DependencyContext> dependencies) {
        // Start first optional dependencies first.
        List<DependencyContext> requiredDeps = new ArrayList();
        for (DependencyContext d : dependencies) {
            if (d.isRequired()) {
                requiredDeps.add(d);
                continue;
            }
            if (d.needsInstance()) {
                instantiateComponent();
            }
            d.start();
        }
        // now, start required dependencies.
        for (DependencyContext d : requiredDeps) {
            if (d.needsInstance()) {
                instantiateComponent();
            }
            d.start();
        }
    }
    
    private void stopDependencies() {
        for (DependencyContext d : m_dependencies) {
            d.stop();
        }
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
    
    private boolean hasSomePropagateDependencies() {
		for (int i = 0; i < m_dependencies.size(); i++) {
			DependencyContext d = (DependencyContext) m_dependencies.get(i);
			if (d.isPropagated()) {
				return true;
			}
		}
		return false;
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
                    m_logger.log(Logger.LOG_ERROR, "Could not instantiate class " + m_componentDefinition, e);
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
			    for (Event e : m_dependencyEvents.get(d)) {
			        d.invokeAdd(e);
			    }
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
	             for (Event e : m_dependencyEvents.get(d)) {
	                 d.invokeAdd(e);
	             }
			}
		}
	}
	
    private void invokeAddOptionalDependencies() {
        for (DependencyContext d : m_dependencies) {
            if (! d.isRequired()) {
                for (Event e : m_dependencyEvents.get(d)) {
                    d.invokeAdd(e);
                }
            }
        }
    }

    private void invokeRemoveRequiredDependencies() { 
		for (DependencyContext d : m_dependencies) {
			if (!d.isInstanceBound() && d.isRequired()) {
                for (Event e : m_dependencyEvents.get(d)) {
                    d.invokeRemove(e);
                }
			}
		}
	}

    private void invokeRemoveOptionalDependencies() { 
        for (DependencyContext d : m_dependencies) {
            if (! d.isRequired()) {
                for (Event e : m_dependencyEvents.get(d)) {
                    d.invokeRemove(e);
                }
            }
        }
    }

	private void invokeRemoveInstanceBoundDependencies() {
		for (DependencyContext d : m_dependencies) {
			if (d.isInstanceBound()) {
                for (Event e : m_dependencyEvents.get(d)) {
                    d.invokeRemove(e);
                }
			}
		}
	}

	private void invoke(String name) {
        if (name != null) {
            // if a callback instance was specified, look for the method there, if not,
            // ask the service for its composition instances
            Object[] instances = m_callbackInstance != null ? new Object[] { m_callbackInstance } : getCompositionInstances();

            invokeCallbackMethod(instances, name, 
                new Class[][] {{ Component.class }, {}}, 
                new Object[][] {{ this }, {}});
        }
    }
    
    public Object[] getInstances() {
    	return getCompositionInstances();
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
			l.changed(this, state);
		}
	}
	
	private void notifyListener(ComponentStateListener l, ComponentState state) {
	    l.changed(this, state);
	}

	public boolean isAvailable() {
		return m_state == TRACKING_OPTIONAL;
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
		return (List<DependencyContext>) m_dependencies.clone();
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
	
    public ComponentDependencyDeclaration[] getComponentDependencies() {
        List deps = getDependencies();
        if (deps != null) {
            ComponentDependencyDeclaration[] result = new ComponentDependencyDeclaration[deps.size()];
            for (int i = 0; i < result.length; i++) {
                DependencyContext dep = (DependencyContext) deps.get(i);
                if (dep instanceof ComponentDependencyDeclaration) {
                    result[i] = (ComponentDependencyDeclaration) dep;
                }
                else {
                    result[i] = new SCDImpl(dep.toString(), (dep.isAvailable() ? 1 : 0) + (dep.isRequired() ? 2 : 0), dep.getClass().getName());
                }
            }
            return result;
        }
        return null;
    }
    
    public String getName() {
        StringBuffer sb = new StringBuffer();
        Object serviceName = m_serviceName;
        if (serviceName instanceof String[]) {
            String[] names = (String[]) serviceName;
            for (int i = 0; i < names.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(names[i]);
            }
            appendProperties(sb);
        } else if (serviceName instanceof String) {
            sb.append(serviceName.toString());
            appendProperties(sb);
        } else {
            Object implementation = m_componentDefinition;
            if (implementation != null) {
                if (implementation instanceof Class) {
                    sb.append(((Class) implementation).getName());
                } else {
                    // If the implementation instance does not override "toString", just display
                    // the class name, else display the component using its toString method
                    try {
                	Method m = implementation.getClass().getMethod("toString", new Class[0]);
                        if (m.getDeclaringClass().equals(Object.class)) {
                            sb.append(implementation.getClass().getName());
                        } else {
                            sb.append(implementation.toString());
                        }
                    }  catch (java.lang.NoSuchMethodException e) {
                        // Just display the class name
                        sb.append(implementation.getClass().getName());
                    }
                }
            } else {
                sb.append(super.toString());
            }
        }
        return sb.toString();
    }
    
    private void appendProperties(StringBuffer result) {
        Dictionary properties = calculateServiceProperties();
        if (properties != null) {
            result.append("(");
            Enumeration enumeration = properties.keys();
            while (enumeration.hasMoreElements()) {
                Object key = enumeration.nextElement();
                result.append(key.toString());
                result.append('=');
                Object value = properties.get(key);
                if (value instanceof String[]) {
                    String[] values = (String[]) value;
                    result.append('{');
                    for (int i = 0; i < values.length; i++) {
                        if (i > 0) {
                            result.append(',');
                        }
                        result.append(values[i].toString());
                    }
                    result.append('}');
                }
                else {
                    result.append(value.toString());
                }
                if (enumeration.hasMoreElements()) {
                    result.append(',');
                }
            }
            result.append(")");
        }
    }
    
    public BundleContext getBundleContext() {
        return m_context;
    }
    
    public long getId() {
        return m_id;
    }
    
    public String getClassName() {
        Object serviceInstance = m_componentInstance;
        if (serviceInstance != null) {
            return serviceInstance.getClass().getName();
        } 
        
        Object implementation = m_componentDefinition;
        if (implementation != null) {
            if (implementation instanceof Class) {
                return ((Class<?>) implementation).getName();
            }
            return implementation.getClass().getName();
        } 
        
        Object instanceFactory = m_instanceFactory;
        if (instanceFactory != null) {
            return instanceFactory.getClass().getName();
        } else {
            // Unexpected ...
            return getClass().getName();
        }
    }
    
    public synchronized String[] getServices() {
        if (m_serviceName instanceof String[]) {
            return (String[]) m_serviceName;
        } else if (m_serviceName instanceof String) {
            return new String[] { (String) m_serviceName };
        } else {
            return null;
        }
    }
    
    public int getState() {
        return (isAvailable() ? ComponentDeclaration.STATE_REGISTERED : ComponentDeclaration.STATE_UNREGISTERED);
    }

    public void ensureNotActive() {
        // TODO Auto-generated method stub
    }
    
    public ComponentDeclaration getComponentDeclaration() {
        return this;
    }
}
