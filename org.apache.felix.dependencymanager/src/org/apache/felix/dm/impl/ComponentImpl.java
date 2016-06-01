/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.dm.impl;

import static org.apache.felix.dm.ComponentState.INACTIVE;
import static org.apache.felix.dm.ComponentState.INSTANTIATED_AND_WAITING_FOR_REQUIRED;
import static org.apache.felix.dm.ComponentState.TRACKING_OPTIONAL;
import static org.apache.felix.dm.ComponentState.WAITING_FOR_REQUIRED;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentDeclaration;
import org.apache.felix.dm.ComponentDependencyDeclaration;
import org.apache.felix.dm.ComponentExecutorFactory;
import org.apache.felix.dm.ComponentState;
import org.apache.felix.dm.ComponentStateListener;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.Logger;
import org.apache.felix.dm.context.ComponentContext;
import org.apache.felix.dm.context.DependencyContext;
import org.apache.felix.dm.context.Event;
import org.apache.felix.dm.context.EventType;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

/**
 * Dependency Manager Component implementation.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ComponentImpl implements Component, ComponentContext, ComponentDeclaration {
    /**
     * NullObject ServiceRegistration that is injected in components that don't provide any services. 
     */
	private static final ServiceRegistration NULL_REGISTRATION = (ServiceRegistration) Proxy
			.newProxyInstance(ComponentImpl.class.getClassLoader(),
					new Class[] { ServiceRegistration.class },
					new DefaultNullObject());
	
	/**
	 * Constant Used to get empty constructor by reflection. 
	 */
    private static final Class<?>[] VOID = new Class[] {};
    
    /**
     * Default Component Executor, which is by default single threaded. The first thread which schedules a task
     * is the master thread and will execute all tasks that are scheduled by other threads at the time the master
     * thread is executing. Internal tasks scheduled by the master thread are executed immediately (inline execution).
     * 
     * If a ComponentExecutorFactory is provided in the OSGI registry, then this executor will be replaced by the
     * executor returned by the ComponentExecutorFactory (however, the same semantic of the default executor is used: 
     * all tasks are serially executed).
     * 
     * @see @link {@link ComponentExecutorFactory}
     */
	private volatile Executor m_executor = new SerialExecutor(new Logger(null));
	
	/**
	 * The current state of the component state machine.
	 */
	private ComponentState m_state = ComponentState.INACTIVE;
	
    /**
     * Indicates that the handleChange method is currently being executed.
     */
    private boolean m_handlingChange;
    
    /**
     * List of dependencies. We use a COW list in order to avoid ConcurrentModificationException while iterating on the 
     * list and while a component synchronously add more dependencies from one of its callback method.
     */
	private final CopyOnWriteArrayList<DependencyContext> m_dependencies = new CopyOnWriteArrayList<>();
	
	/**
	 * List of Component state listeners. We use a COW list in order to avoid ConcurrentModificationException while iterating on the 
     * list and while a component synchronously add more listeners from one of its callback method.
	 */
	private final List<ComponentStateListener> m_listeners = new CopyOnWriteArrayList<>();
	
	/**
	 * Is the component active ?
	 */
	private boolean m_isStarted;
	
	/**
	 * The Component logger.
	 */
    private final Logger m_logger;
    
    /**
     * The Component bundle context.
     */
    private final BundleContext m_context;
    
    /**
     * The DependencyManager object that has created this component.
     */
    private final DependencyManager m_manager;
    
    /**
     * The object used to create the component. Can be a class name, or the component implementation instance.
     */
    private volatile Object m_componentDefinition;
    
    /**
     * The component instance.
     */
	private Object m_componentInstance;
	
	/**
	 * The service(s) provided by this component. Can be a String, or a String array.
	 */
    private volatile Object m_serviceName;
    
    /**
     * The service properties, if this component is providing a service.
     */
    private volatile Dictionary<Object, Object> m_serviceProperties;
    
    /**
     * The component service registration. Can be a NullObject in case the component does not provide a service.
     */
    private volatile ServiceRegistration m_registration;
    
    /**
     * Map of auto configured fields (BundleContext, ServiceRegistration, DependencyManager, or Component).
     * By default, all fields mentioned above are auto configured (injected in class fields having the same type).
     */
    private final Map<Class<?>, Boolean> m_autoConfig = new ConcurrentHashMap<>();
    
    /**
     * Map of auto configured instance fields that will be used when injected auto configured fields.
     * @see #m_autoConfig
     */
    private final Map<Class<?>, String> m_autoConfigInstance = new ConcurrentHashMap<>();
    
    /**
     * Data structure used to record the elapsed time used by component lifecycle callbacks.
     * Key = callback name ("init", "start", "stop", "destroy").
     * Value = elapsed time in nanos.
     */
    private final Map<String, Long> m_stopwatch = new ConcurrentHashMap<>();
    
    /**
     * Unique component id.
     */
    private final long m_id;
    
    /**
     * Unique ID generator.
     */
    private final static AtomicLong m_idGenerator = new AtomicLong();
    
    /**
     * Holds all the services of a given dependency context. Caution: the last entry in the skiplist is the highest 
     * ranked service.
     */
    private final Map<DependencyContext, ConcurrentSkipListSet<Event>> m_dependencyEvents = new HashMap<>();
    
    /**
     * Flag used to check if this component has been added in a DependencyManager object.
     */
    private final AtomicBoolean m_active = new AtomicBoolean(false);
        
    /**
     * Init lifecycle callback. From that method, component are expected to add more extra dependencies.
     * When this callback is invoked, all required dependencies have been injected. 
     */
    private volatile String m_callbackInit;
    
    /**
     * Start lifecycle callback. When this method is called, all required + all extra required dependencies defined in the
     * init callback have been injected. The component may then perform its initialization.
     */
    private volatile String m_callbackStart;
    
    /**
     * Stop callback. When this method is called, the component has been unregistered (if it provides any services),
     * and all optional dependencies have been unbound.
     */
    private volatile String m_callbackStop;
    
    /**
     * Destroy callback. When this method is called, all required dependencies defined in the init method have been unbound.
     * After this method is called, then all required dependencies defined in the Activator will be unbound.
     */
    private volatile String m_callbackDestroy;
    
    /**
     * By default, the init/start/stop/destroy callbacks are invoked on the component instance(s).
     * But you can specify a separate callback instance.
     */
    private volatile Object m_callbackInstance;
    
    /**
     * Component Factory instance object, that can be used to instantiate the component instance.
     */
	private volatile Object m_instanceFactory;
	
	/**
	 * Name of the Factory method to call.
	 */
	private volatile String m_instanceFactoryCreateMethod;
	
	/**
	 * Composition Manager that can be used to create a graph of objects that are used to implement the component.
	 */
	private volatile Object m_compositionManager;
	
	/**
	 * Name of the method used to invoke in order to get the list of component instance objects.
	 */
	private volatile String m_compositionManagerGetMethod;
	
	/**
	 * The composition manager instance object, if specified.
	 */
	private volatile Object m_compositionManagerInstance;
	
	/**
	 * The Component bundle.
	 */
    private final Bundle m_bundle;
        
    /**
     * Cache of callback invocation used to avoid calling the same callback twice.
     * This situation may sometimes happen when the state machine triggers a lifecycle callback ("bind" call), and
     * when the bind method registers a service which is tracked by another optional component dependency.
     * 
     * @see org.apache.felix.dm.itest.api.FELIX4913_OptionalCallbackInvokedTwiceTest which reproduces the use case.
     */
    private final Map<Event, Event> m_invokeCallbackCache = new IdentityHashMap<>();

    /**
     * Flag used to check if the start callback has been invoked.
     * We use this flag to ensure that we only inject optional dependencies after the start callback has been called. 
     */
	private boolean m_startCalled;
	
    /**
     * Default component declaration implementation.
     */
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
        
        public String getSimpleName() {
            return m_name;
        }
        
        public String getFilter() {
            return null;
        }

        public int getState() {
            return m_state;
        }

        public String getType() {
            return m_type;
        }
    }

    /**
     * Constructor. Only used for tests.
     */
    public ComponentImpl() {
	    this(null, null, new Logger(null));
	}
	
    /**
     * Constructor
     * @param context the component bundle context 
     * @param manager the manager used to create the component
     * @param logger the logger to use
     */
    public ComponentImpl(BundleContext context, DependencyManager manager, Logger logger) {
        m_context = context;
        m_bundle = context != null ? context.getBundle() : null;
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

    @Override
    public <T> T createConfigurationType(Class<T> type, Dictionary<?, ?> config) {
        return Configurable.create(type,  config);
    }
    
    @Override
    public Executor getExecutor() {
        return m_executor;
    }

    @Override
    public Component setDebug(String debugKey) {
        // Force debug level in our logger
        m_logger.setEnabledLevel(LogService.LOG_DEBUG);
        m_logger.setDebugKey(debugKey);
        return this;
    }

	@Override
	public Component add(final Dependency ... dependencies) {
		getExecutor().execute(() -> {
            List<DependencyContext> instanceBoundDeps = new ArrayList<>();
            for (Dependency d : dependencies) {
                DependencyContext dc = (DependencyContext) d;
                if (dc.getComponentContext() != null) {
                    m_logger.err("%s can't be added to %s (dependency already added to another component).", dc, ComponentImpl.this);
                    continue;
                }
                m_dependencyEvents.put(dc, new ConcurrentSkipListSet<Event>());
                m_dependencies.add(dc);
                dc.setComponentContext(ComponentImpl.this);
                if (!(m_state == ComponentState.INACTIVE)) {
                    dc.setInstanceBound(true);
                    instanceBoundDeps.add(dc);
                }
            }
            startDependencies(instanceBoundDeps);
            handleChange();
		});
		return this;
	}

	@Override
	public Component remove(final Dependency d) {
		getExecutor().execute(() -> {
		    DependencyContext dc = (DependencyContext) d;
		    // First remove this dependency from the dependency list
		    m_dependencies.remove(d);
		    // Now we can stop the dependency (our component won't be deactivated, it will only be unbound with
		    // the removed dependency).
		    if (!(m_state == ComponentState.INACTIVE)) {
		        dc.stop();
		    }
		    // Finally, cleanup the dependency events.
		    m_dependencyEvents.remove(d);
		    handleChange();
		});
		return this;
	}

	@Override
	public void start() {
	    if (m_active.compareAndSet(false, true)) {
            getExecutor().execute(() -> {
                m_isStarted = true;
                handleChange();
            });
	    }
	}
	
	@Override
	public void stop() {           
	    if (m_active.compareAndSet(true, false)) {
	        Executor executor = getExecutor();

	        // First, declare the task that will stop our component in our executor.
	        final Runnable stopTask = () -> {
	            m_isStarted = false;
	            handleChange();
	        };
            
            // Now, we have to schedule our stopTask in our component executor. But we have to handle a special case:
            // if the component bundle is stopping *AND* if the executor is a parallel dispatcher, then we want 
            // to invoke our stopTask synchronously, in order to make sure that the bundle context is valid while our 
            // component is being deactivated (if we stop the component asynchronously, the bundle context may be invalidated
            // before our component is stopped, and we don't want to be in this situation).
            
            boolean stopping = m_bundle != null /* null only in tests env */ && m_bundle.getState() == Bundle.STOPPING;
            if (stopping && executor instanceof DispatchExecutor) {
            	((DispatchExecutor) executor).execute(stopTask, false /* try to  execute synchronously, not using threadpool */);
            } else {
            	executor.execute(stopTask);
            }
	    }
	}

	@SuppressWarnings("unchecked")
    @Override
	public Component setInterface(String serviceName, Dictionary<?, ?> properties) {
		ensureNotActive();
	    m_serviceName = serviceName;
	    m_serviceProperties = (Dictionary<Object, Object>) properties;
	    return this;
	}

	@SuppressWarnings("unchecked")
    @Override
	public Component setInterface(String[] serviceName, Dictionary<?, ?> properties) {
	    ensureNotActive();
	    m_serviceName = serviceName;
	    m_serviceProperties = (Dictionary<Object, Object>) properties;
	    return this;
	}
	
	@Override
    public void handleEvent(final DependencyContext dc, final EventType type, final Event... event) {
        // since this method can be invoked by anyone from any thread, we need to
        // pass on the event to a runnable that we execute using the component's
        // executor
        getExecutor().execute(() -> {
                try {
                    switch (type) {
                    case ADDED:
                        handleAdded(dc, event[0]);
                        break;
                    case CHANGED:
                        handleChanged(dc, event[0]);
                        break;
                    case REMOVED:
                        handleRemoved(dc, event[0]);
                        break;
                    case SWAPPED:
                        handleSwapped(dc, event[0], event[1]);
                        break;
                    }
                } finally {
                	// Clear cache of component callbacks invocation, except if we are currently called from handleChange().
                	// (See FELIX-4913).
                    clearInvokeCallbackCache();
                }
            });        
	}

    @Override
    public Event getDependencyEvent(DependencyContext dc) {
        ConcurrentSkipListSet<Event> events = m_dependencyEvents.get(dc);
        return events.size() > 0 ? events.last() : null;
    }
    
    @Override
    public Set<Event> getDependencyEvents(DependencyContext dc) {
        return m_dependencyEvents.get(dc);
    }

    @Override
    public Component setAutoConfig(Class<?> clazz, boolean autoConfig) {
        m_autoConfig.put(clazz, Boolean.valueOf(autoConfig));
        return this;
    }
    
    @Override
    public Component setAutoConfig(Class<?> clazz, String instanceName) {
        m_autoConfig.put(clazz, Boolean.valueOf(instanceName != null));
        m_autoConfigInstance.put(clazz, instanceName);
        return this;
    }
    
    @Override
    public boolean getAutoConfig(Class<?> clazz) {
        Boolean result = (Boolean) m_autoConfig.get(clazz);
        return (result != null && result.booleanValue());
    }
    
    @Override
    public String getAutoConfigInstance(Class<?> clazz) {
        return (String) m_autoConfigInstance.get(clazz);
    }

    @SuppressWarnings("unchecked")
    public <T> T getInstance() {     
        Object[] instances  = getCompositionInstances();
        return instances.length == 0 ? null : (T) instances[0]; 
    }

    public Object[] getInstances() {
        return getCompositionInstances();
    }
    
    public void invokeCallbackMethod(Object[] instances, String methodName, Class<?>[][] signatures, Object[][] parameters) {
        invokeCallbackMethod(instances, methodName, signatures, parameters, true);
    }

    public void invokeCallbackMethod(Object[] instances, String methodName, Class<?>[][] signatures,
        Object[][] parameters, boolean logIfNotFound) {
        boolean callbackFound = false;
        for (int i = 0; i < instances.length; i++) {
            try {
                InvocationUtil.invokeCallbackMethod(instances[i], methodName, signatures, parameters);
                callbackFound |= true;
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
        
        // If the callback is not found, we don't log if the method is on an AbstractDecorator.
        // (Aspect or Adapter are not interested in user dependency callbacks)        
        if (logIfNotFound && ! callbackFound && ! (getInstance() instanceof AbstractDecorator)) {
            if (m_logger == null) {
                System.out.println("\"" + methodName + "\" callback not found on component instances "
                    + Arrays.toString(instances));
            } else {
                m_logger.log(LogService.LOG_ERROR, "\"" + methodName + "\" callback not found on component instances "
                    + Arrays.toString(instances));
            }

        }
    }

    @Override
    public boolean isAvailable() {
        return m_state == TRACKING_OPTIONAL;
    }
    
    @Override
    public boolean isActive() {
        return m_active.get();
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

    @SuppressWarnings("unchecked")
    @Override
    public List<DependencyContext> getDependencies() {
        return (List<DependencyContext>) m_dependencies.clone();
    }

    @Override
    public Component setImplementation(Object implementation) {
        m_componentDefinition = implementation;
        return this;
    }
    
    @Override
    public ServiceRegistration getServiceRegistration() {
        return m_registration;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K,V> Dictionary<K, V> getServiceProperties() {
        if (m_serviceProperties != null) {
            // Applied patch from FELIX-4304
            Hashtable<Object, Object> serviceProperties = new Hashtable<>();
            addTo(serviceProperties, m_serviceProperties);
            return (Dictionary<K, V>) serviceProperties;
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Component setServiceProperties(final Dictionary<?, ?> serviceProperties) {
        getExecutor().execute(() -> {
            Dictionary<Object, Object> properties = null;
            m_serviceProperties = (Dictionary<Object, Object>) serviceProperties;
            if ((m_registration != null) && (m_serviceName != null)) {
                properties = calculateServiceProperties();
                m_registration.setProperties(properties);
            }
        });
        return this;
    }
    
    public Component setCallbacks(String init, String start, String stop, String destroy) {
        ensureNotActive();
        m_callbackInit = init;
        m_callbackStart = start;
        m_callbackStop = stop;
        m_callbackDestroy = destroy;
        return this;
    }
    
    public Component setCallbacks(Object instance, String init, String start, String stop, String destroy) {
        ensureNotActive();
        m_callbackInstance = instance;
        m_callbackInit = init;
        m_callbackStart = start;
        m_callbackStop = stop;
        m_callbackDestroy = destroy;
        return this;
    }

    @Override
    public Component setFactory(Object factory, String createMethod) {
        ensureNotActive();
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
        ensureNotActive();
        m_compositionManager = instance;
        m_compositionManagerGetMethod = getMethod;
        return this;
    }

    @Override
    public Component setComposition(String getMethod) {
        return setComposition(null, getMethod);
    }

    @Override
    public DependencyManager getDependencyManager() {
        return m_manager;
    }
    
    public ComponentDependencyDeclaration[] getComponentDependencies() {
        List<DependencyContext> deps = getDependencies();
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
        // If the component provides service(s), return the services as the component name.
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
            // The component does not provide a service, use the component definition as the name.
            Object componentDefinition = m_componentDefinition;
            if (componentDefinition != null) {
                sb.append(toString(componentDefinition));
            } else { 
                // No component definition means we are using a factory. If the component instance is available use it as the component name,
                // alse use teh factory object as the component name.
                Object componentInstance = m_componentInstance;
                if (componentInstance != null) {
                    sb.append(componentInstance.getClass().getName());
                } else {
                    // Check if a factory is set.
                    Object instanceFactory = m_instanceFactory;
                    if (instanceFactory != null) {
                        sb.append(toString(instanceFactory));
                    } else {
                        sb.append(super.toString());
                    }
                }
            }
        }
        return sb.toString();
    }
    
    private String toString(Object implementation) {
        if (implementation instanceof Class) {
            return (((Class<?>) implementation).getName());
        } else {
            // If the implementation instance does not override "toString", just display
            // the class name, else display the component using its toString method
            try {
            Method m = implementation.getClass().getMethod("toString", new Class[0]);
                if (m.getDeclaringClass().equals(Object.class)) {
                    return implementation.getClass().getName();
                } else {
                    return implementation.toString();
                }
            }  catch (java.lang.NoSuchMethodException e) {
                // Just display the class name
                return implementation.getClass().getName();
            }
        }
    }
    
    @Override
    public BundleContext getBundleContext() {
        return m_context;
    }
    
    @Override
    public Bundle getBundle() {
        return m_bundle;
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
            return toString(instanceFactory);
        } else {
            // unexpected.
            return ComponentImpl.class.getName();
        }
    }
    
    public String[] getServices() {
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
        if (m_active.get()) {
            throw new IllegalStateException("Can't modify an already started component.");
        }
    }
    
    public ComponentDeclaration getComponentDeclaration() {
        return this;
    }
    
    @Override
    public String toString() {
        if (m_logger.getDebugKey() != null) {
            return m_logger.getDebugKey();
        }
        return getClassName();
    }
    
    @Override
    public void setThreadPool(Executor threadPool) {
        ensureNotActive();
        m_executor = new DispatchExecutor(threadPool, m_logger);
    }
    
    @Override
    public Logger getLogger() {
        return m_logger;
    }

    @Override
    public Map<String, Long> getCallbacksTime() {
        return m_stopwatch;
    }
    
    // ---------------------- Package/Private methods ---------------------------
    
    void instantiateComponent() {
        m_logger.debug("instantiating component.");

        // TODO add more complex factory instantiations of one or more components in a composition here
        if (m_componentInstance == null) {
            if (m_componentDefinition instanceof Class) {
                try {
                    m_componentInstance = createInstance((Class<?>) m_componentDefinition);
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
                                factory = createInstance((Class<?>) m_instanceFactory);
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
                            m_componentInstance = InvocationUtil.invokeMethod(factory, 
                                factory.getClass(), m_instanceFactoryCreateMethod, 
                                new Class[][] {{}, {Component.class}}, new Object[][] {{}, {this}}, false);
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
            autoConfigureImplementation(BundleContext.class, m_context);
            autoConfigureImplementation(ServiceRegistration.class, NULL_REGISTRATION);
            autoConfigureImplementation(DependencyManager.class, m_manager);
            autoConfigureImplementation(Component.class, this);
        }
    }    
    
    /**
     * Runs the state machine, to see if a change event has to trigger some component state transition.
     */
    private void handleChange() {
        m_logger.debug("handleChanged");
    	handlingChange(true);
        try {
            ComponentState oldState;
            ComponentState newState;
            do {
                oldState = m_state;
                newState = calculateNewState(oldState);
                m_logger.debug("%s -> %s", oldState, newState);
                m_state = newState;
            } while (performTransition(oldState, newState));
        } finally {
        	handlingChange(false);
            clearInvokeCallbackCache();
            m_logger.debug("end handling change.");
        }
    }
    
    /** 
     * Based on the current state, calculate the new state. 
     */
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

    /** 
     * Perform all the actions associated with state transitions. 
     * @returns true if a transition was performed.
     **/
    private boolean performTransition(ComponentState oldState, ComponentState newState) {
        if (oldState == ComponentState.INACTIVE && newState == ComponentState.WAITING_FOR_REQUIRED) {
            startDependencies(m_dependencies);
            notifyListeners(newState);
            return true;
        }
        if (oldState == ComponentState.WAITING_FOR_REQUIRED && newState == ComponentState.INSTANTIATED_AND_WAITING_FOR_REQUIRED) {
            instantiateComponent();
            invokeAutoConfigDependencies();
            invokeAddRequiredDependencies();
			ComponentState stateBeforeCallingInit = m_state;
            invoke(m_callbackInit); 
	        if (stateBeforeCallingInit == m_state) {
	            notifyListeners(newState); // init did not change current state, we can notify about this new state
	        }
            return true;
        }
        if (oldState == ComponentState.INSTANTIATED_AND_WAITING_FOR_REQUIRED && newState == ComponentState.TRACKING_OPTIONAL) {
            invokeAutoConfigInstanceBoundDependencies();
            invokeAddRequiredInstanceBoundDependencies();
            invokeStart();
            invokeAddOptionalDependencies();
            registerService();
            notifyListeners(newState);
            return true;
        }
        if (oldState == ComponentState.TRACKING_OPTIONAL && newState == ComponentState.INSTANTIATED_AND_WAITING_FOR_REQUIRED) {
            unregisterService();
            invokeRemoveOptionalDependencies();
            invokeStop();
            invokeRemoveInstanceBoundDependencies();
            notifyListeners(newState);
            return true;
        }
        if (oldState == ComponentState.INSTANTIATED_AND_WAITING_FOR_REQUIRED && newState == ComponentState.WAITING_FOR_REQUIRED) {
            invoke(m_callbackDestroy);
            removeInstanceBoundDependencies();
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
    
	private void invokeStart() {
        invoke(m_callbackStart);
        m_startCalled = true;
	}

    private void invokeStop() {
        invoke(m_callbackStop);
        m_startCalled = false;
	}

	/**
     * Sets the m_handlingChange flag that indicates if the state machine is currently running the handleChange method.
     */
    private void handlingChange(boolean transiting) {
        m_handlingChange = transiting;
    }
    
    /**
     * Are we currently running the handleChange method ?
     */
    private boolean isHandlingChange() {
    	return m_handlingChange;
    }

    /**
     * Then handleEvent calls this method when a dependency service is being added.
     */
    private void handleAdded(DependencyContext dc, Event e) {
        if (! m_isStarted) {
            return;
        }
        m_logger.debug("handleAdded %s", e);
        
        Set<Event> dependencyEvents = m_dependencyEvents.get(dc);
        dependencyEvents.add(e);        
        dc.setAvailable(true);
                  
        // In the following switch block, we sometimes only recalculate state changes 
        // if the dependency is fully started. If the dependency is not started,
        // it means it is actually starting (the service tracker is executing the open method). 
        // And in this case, depending on the state, we don't recalculate state changes now. 
        // 
        // All this is done for two reasons:
        // 1- optimization: it is preferable to recalculate state changes once we know about all currently 
        //    available dependency services (after the tracker has returned from its open method).
        // 2- This also allows to determine the list of currently available dependency services before calling
        //    the component start() callback.
        
        switch (m_state) {
        case WAITING_FOR_REQUIRED:            
            if (dc.isStarted() && dc.isRequired()) {
                handleChange();
            }
            break;
        case INSTANTIATED_AND_WAITING_FOR_REQUIRED:
            if (!dc.isInstanceBound()) {
                if (dc.isRequired()) {
                    invokeCallbackSafe(dc, EventType.ADDED, e);
                }
                updateInstance(dc, e, false, true);
            } else {
                if (dc.isStarted() && dc.isRequired()) {
                    handleChange();
                }
            }
            break;
        case TRACKING_OPTIONAL:
            invokeCallbackSafe(dc, EventType.ADDED, e);
            updateInstance(dc, e, false, true);
            break;
        default:
        }
    }       
    
    /**
     * Then handleEvent calls this method when a dependency service is being changed.
     */
    private void handleChanged(final DependencyContext dc, final Event e) {
        if (! m_isStarted) {
            return;
        }
        Set<Event> dependencyEvents = m_dependencyEvents.get(dc);
        dependencyEvents.remove(e);
        dependencyEvents.add(e);
                
        switch (m_state) {
        case TRACKING_OPTIONAL:
            invokeCallbackSafe(dc, EventType.CHANGED, e);
            updateInstance(dc, e, true, false);
            break;

        case INSTANTIATED_AND_WAITING_FOR_REQUIRED:
            if (!dc.isInstanceBound()) {
                invokeCallbackSafe(dc, EventType.CHANGED, e);
                updateInstance(dc, e, true, false);
            }
            break;
        default:
            // noop
        }
    }
    
    /**
     * Then handleEvent calls this method when a dependency service is being removed.
     */
    private void handleRemoved(DependencyContext dc, Event e) {
        if (! m_isStarted) {
            return;
        }
        // Check if the dependency is still available.
        Set<Event> dependencyEvents = m_dependencyEvents.get(dc);
        int size = dependencyEvents.size();
        if (dependencyEvents.contains(e)) {
            size--; // the dependency is currently registered and is about to be removed.
        }
        dc.setAvailable(size > 0);
        
        // If the dependency is now unavailable, we have to recalculate state change. This will result in invoking the
        // "removed" callback with the removed dependency (which we have not yet removed from our dependency events list.).
        // But we don't recalculate the state if the dependency is not started (if not started, it means that it is currently starting,
        // and the tracker is detecting a removed service).
        if (size == 0 && dc.isStarted()) {
            handleChange();
        }
        
        // Now, really remove the dependency event.
        dependencyEvents.remove(e);    
        
        // Depending on the state, we possible have to invoke the callbacks and update the component instance.        
        switch (m_state) {
        case INSTANTIATED_AND_WAITING_FOR_REQUIRED:
            if (!dc.isInstanceBound()) {
                if (dc.isRequired()) {
                    invokeCallbackSafe(dc, EventType.REMOVED, e);
                }
                updateInstance(dc, e, false, false);
            }
            break;
        case TRACKING_OPTIONAL:
            invokeCallbackSafe(dc, EventType.REMOVED, e);
            updateInstance(dc, e, false, false);
            break;
        default:
        }
    }
    
    private void handleSwapped(DependencyContext dc, Event oldEvent, Event newEvent) {
        if (! m_isStarted) {
            return;
        }
        Set<Event> dependencyEvents = m_dependencyEvents.get(dc);        
        dependencyEvents.remove(oldEvent);
        dependencyEvents.add(newEvent);
                
        // Depending on the state, we possible have to invoke the callbacks and update the component instance.        
        switch (m_state) {
        case WAITING_FOR_REQUIRED:
            // No need to swap, we don't have yet injected anything
            break;
        case INSTANTIATED_AND_WAITING_FOR_REQUIRED:
            // Only swap *non* instance-bound dependencies
            if (!dc.isInstanceBound()) {
                if (dc.isRequired()) {
                    dc.invokeCallback(EventType.SWAPPED, oldEvent, newEvent);
                }
            }
            break;
        case TRACKING_OPTIONAL:
            dc.invokeCallback(EventType.SWAPPED, oldEvent, newEvent);
            break;
        default:
        }
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
    
    /**
     * Updates the component instance(s).
     * @param dc the dependency context for the updating dependency service
     * @param event the event holding the updating service (service + properties)
     * @param update true if dependency service properties are updating, false if not. If false, it means
     *        that a dependency service is being added or removed. (see the "add" flag).
     * @param add true if the dependency service has been added, false if it has been removed. This flag is 
     *        ignored if the "update" flag is true (because the dependency properties are just being updated).
     */
    private void updateInstance(DependencyContext dc, Event event, boolean update, boolean add) {
        if (dc.isAutoConfig()) {
            updateImplementation(dc.getAutoConfigType(), dc, dc.getAutoConfigName(), event, update, add);
        }
        if (dc.isPropagated() && m_registration != null) {
            m_registration.setProperties(calculateServiceProperties());
        }
    }
    
    private void startDependencies(List<DependencyContext> dependencies) {
        // Start first optional dependencies first.
        m_logger.debug("startDependencies.");
        List<DependencyContext> requiredDeps = new ArrayList<>();
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
            autoConfigureImplementation(ServiceRegistration.class, m_registration);
            
            // service name can either be a string or an array of strings
            ServiceRegistration registration;

            // determine service properties
            Dictionary<?,?> properties = calculateServiceProperties();

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
            try {
                if (m_bundle != null && (m_bundle.getState() == Bundle.STARTING || m_bundle.getState() == Bundle.ACTIVE || m_bundle.getState() == Bundle.STOPPING)) {
                    m_registration.unregister();
                }
            } catch (IllegalStateException e) { /* Should we really log this ? */}
            autoConfigureImplementation(ServiceRegistration.class, NULL_REGISTRATION);
            m_registration = null;
        }
    }
    
    private Dictionary<Object, Object> calculateServiceProperties() {
		Dictionary<Object, Object> properties = new Hashtable<>();
		for (int i = 0; i < m_dependencies.size(); i++) {
			DependencyContext d = (DependencyContext) m_dependencies.get(i);
			if (d.isPropagated() && d.isAvailable()) {
				Dictionary<Object, Object> dict = d.getProperties();
				addTo(properties, dict);
			}
		}
		// our service properties must not be overriden by propagated dependency properties, so we add our service
		// properties after having added propagated dependency properties.
		addTo(properties, m_serviceProperties);
		if (properties.size() == 0) {
			properties = null;
		}
		return properties;
	}

	private void addTo(Dictionary<Object, Object> properties, Dictionary<Object, Object> additional) {
		if (properties == null) {
			throw new IllegalArgumentException("Dictionary to add to cannot be null.");
		}
		if (additional != null) {
			Enumeration<Object> e = additional.keys();
			while (e.hasMoreElements()) {
				Object key = e.nextElement();
				properties.put(key, additional.get(key));
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
			        invokeCallbackSafe(d, EventType.ADDED, e);
			    }
			}
		}
	}
	
    private void invokeAutoConfigDependencies() {
        for (DependencyContext d : m_dependencies) {
            if (d.isAutoConfig() && !d.isInstanceBound()) {
                configureImplementation(d.getAutoConfigType(), d, d.getAutoConfigName());
            }
        }
    }
    
    private void invokeAutoConfigInstanceBoundDependencies() {
        for (DependencyContext d : m_dependencies) {
            if (d.isAutoConfig() && d.isInstanceBound()) {
                configureImplementation(d.getAutoConfigType(), d, d.getAutoConfigName());
            }
        }
    }
	
	private void invokeAddRequiredInstanceBoundDependencies() {
		for (DependencyContext d : m_dependencies) {
			if (d.isRequired() && d.isInstanceBound()) {
	             for (Event e : m_dependencyEvents.get(d)) {
	                 invokeCallbackSafe(d, EventType.ADDED, e);
	             }
			}
		}
	}
	
    private void invokeAddOptionalDependencies() {
        for (DependencyContext d : m_dependencies) {
            if (! d.isRequired()) {
                for (Event e : m_dependencyEvents.get(d)) {
                    invokeCallbackSafe(d, EventType.ADDED, e);
                }
            }
        }
    }

    private void invokeRemoveRequiredDependencies() { 
		for (DependencyContext d : m_dependencies) {
			if (!d.isInstanceBound() && d.isRequired()) {
                for (Event e : m_dependencyEvents.get(d)) {
                    invokeCallbackSafe(d, EventType.REMOVED, e);
                }
			}
		}
	}

    private void invokeRemoveOptionalDependencies() { 
        for (DependencyContext d : m_dependencies) {
            if (! d.isRequired()) {
                for (Event e : m_dependencyEvents.get(d)) {
                    invokeCallbackSafe(d, EventType.REMOVED, e);
                }
            }
        }
    }

	private void invokeRemoveInstanceBoundDependencies() {
		for (DependencyContext d : m_dependencies) {
			if (d.isInstanceBound()) {
                for (Event e : m_dependencyEvents.get(d)) {
                    invokeCallbackSafe(d, EventType.REMOVED, e);
                }
			}
		}
	}
	
	/**
	 * This method ensures that a dependency callback is invoked only one time;
	 * It also ensures that if the dependency callback is optional, then we only
	 * invoke the bind method if the component start callback has already been called. 
	 */
	private void invokeCallbackSafe(DependencyContext dc, EventType type, Event event) {
		if (! dc.isRequired() && ! m_startCalled) {
			return;
		}
		if (m_invokeCallbackCache.put(event, event) == null) {
		    // FELIX-5155: we must not invoke callbacks on our special internal components (adapters/aspects) if the dependency is not the first one, or 
		    // if the internal component is a Factory Pid Adapter.
		    // For aspects/adapters, the first dependency only need to be injected, not the other extra dependencies added by user.
		    // (in fact, we do this because extra dependencies (added by user) may contain a callback instance, and we really don't want to invoke the callbacks twice !		    
		    Object mainComponentImpl = getInstance();
		    if (mainComponentImpl instanceof AbstractDecorator) {
		        if (mainComponentImpl instanceof FactoryConfigurationAdapterImpl.AdapterImpl || dc != m_dependencies.get(0)) {
		            return;
		        }
		    }
			dc.invokeCallback(type, event);
		}		
	}
	
	/**
	 * Removes and closes all instance bound dependencies.
	 * This method is called when a component is destroyed.
	 */
    private void removeInstanceBoundDependencies() {
    	for (DependencyContext dep : m_dependencies) {
    		if (dep.isInstanceBound()) {
    			m_dependencies.remove(dep);
    			dep.stop();
    		}
    	}
    }

	/**
	 * Clears the cache of invoked components callbacks.
	 * We only clear the cache when the state machine is not running.
	 * The cache is used to avoid calling the same bind callback twice.
	 * See FELIX-4913.
	 */
	private void clearInvokeCallbackCache() {
	    if (! isHandlingChange()) {
	    	m_invokeCallbackCache.clear();
	    }
	}

	private void invoke(String name) {
        if (name != null) {
            // if a callback instance was specified, look for the method there, if not,
            // ask the service for its composition instances
            Object[] instances = m_callbackInstance != null ? new Object[] { m_callbackInstance } : getCompositionInstances();

            long t1 = System.nanoTime();
            try {
                invokeCallbackMethod(instances, name, 
                    new Class[][] {{ Component.class }, {}}, 
                    new Object[][] {{ this }, {}},
                    false);
            } finally {
                long t2 = System.nanoTime();
                m_stopwatch.put(name, t2 - t1);
            }
        }
    }
    
    private Object createInstance(Class<?> clazz) throws SecurityException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Constructor<?> constructor = clazz.getConstructor(VOID);
		constructor.setAccessible(true);
        return constructor.newInstance();
    }

	private void notifyListeners(ComponentState state) {
		for (ComponentStateListener l : m_listeners) {
			l.changed(this, state);
		}
	}
	
    private void autoConfigureImplementation(Class<?> clazz, Object instance) {
        if (((Boolean) m_autoConfig.get(clazz)).booleanValue()) {
            configureImplementation(clazz, instance, (String) m_autoConfigInstance.get(clazz));
        }
    }
    
   /**
     * Configure a field in the service implementation. The service implementation
     * is searched for fields that have the same type as the class that was specified
     * and for each of these fields, the specified instance is filled in.
     *
     * @param clazz the class to search for
     * @param instance the object to fill in the implementation class(es) field
     * @param instanceName the name of the instance to fill in, or <code>null</code> if not used
     */
    private void configureImplementation(Class<?> clazz, Object instance, String fieldName) {
        Object[] targets = getInstances();
        if (! FieldUtil.injectField(targets, fieldName, clazz, instance, m_logger) && fieldName != null) {
            // If the target is an abstract decorator (i.e: an adapter, or an aspect), we must not log warnings
            // if field has not been injected.
            if (! (getInstance() instanceof AbstractDecorator)) {
                m_logger.log(Logger.LOG_ERROR, "Could not inject " + instance + " to field \"" + fieldName
                    + "\" at any of the following component instances: " + Arrays.toString(targets));
            }
        }
    }

    private void configureImplementation(Class<?> clazz, DependencyContext dc, String fieldName) {
        Object[] targets = getInstances();
        if (! FieldUtil.injectDependencyField(targets, fieldName, clazz, dc, m_logger) && fieldName != null) {
            // If the target is an abstract decorator (i.e: an adapter, or an aspect), we must not log warnings
            // if field has not been injected.
            if (! (getInstance() instanceof AbstractDecorator)) {
                m_logger.log(Logger.LOG_ERROR, "Could not inject dependency " + clazz.getName() + " to field \""
                    + fieldName + "\" at any of the following component instances: " + Arrays.toString(targets));
            }
        }
    }

    /**
     * Update the component instances.
     *
     * @param clazz the class of the dependency service to inject in the component instances
     * @param dc the dependency context for the updating dependency service
     * @param fieldName the component instances fieldname to fill in with the updated dependency service
     * @param event the event holding the updating service (service + properties)
     * @param update true if dependency service properties are updating, false if not. If false, it means
     *        that a dependency service is being added or removed. (see the "add" flag).
     * @param add true if the dependency service has been added, false if it has been removed. This flag is 
     *        ignored if the "update" flag is true (because the dependency properties are just being updated).
     */
    private void updateImplementation(Class<?> clazz, DependencyContext dc, String fieldName, Event event, boolean update,
        boolean add)
    {
        Object[] targets = getInstances();
        FieldUtil.updateDependencyField(targets, fieldName, update, add, clazz, event, dc, m_logger);
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

    private void appendProperties(StringBuffer result) {
        Dictionary<Object, Object> properties = calculateServiceProperties();
        if (properties != null) {
            result.append("(");
            Enumeration<?> enumeration = properties.keys();
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
}
