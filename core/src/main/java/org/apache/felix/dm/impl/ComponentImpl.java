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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentDeclaration;
import org.apache.felix.dm.ComponentDependencyDeclaration;
import org.apache.felix.dm.ComponentStateListener;
import org.apache.felix.dm.Dependency;
import org.apache.felix.dm.DependencyActivation;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.DependencyService;
import org.apache.felix.dm.InvocationUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Component implementation.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ComponentImpl implements Component, DependencyService, ComponentDeclaration, Comparable {
    private static final Class[] VOID = new Class[] {};
	private static final ServiceRegistration NULL_REGISTRATION;
    private static final ComponentStateListener[] SERVICE_STATE_LISTENER_TYPE = new ComponentStateListener[] {};
    private static long HIGHEST_ID = 0;

    private final Object SYNC = new Object();
    private final BundleContext m_context;
    private final DependencyManager m_manager;
    private final long m_id;

    // configuration (static)
    private String m_callbackInit;
    private String m_callbackStart;
    private String m_callbackStop;
    private String m_callbackDestroy;
    private volatile Object m_serviceName;
    private volatile Object m_implementation;
    private volatile Object m_callbackInstance;

    // configuration (dynamic, but does not affect state)
    private volatile Dictionary m_serviceProperties;

    // configuration (dynamic, and affects state)
    private final ArrayList m_dependencies = new ArrayList();

    // runtime state (calculated from dependencies)
    private State m_state;

    // runtime state (changes because of state changes)
    private volatile Object m_serviceInstance;
    private volatile ServiceRegistration m_registration;
    private volatile boolean m_isBound;
    private volatile boolean m_isInstantiated;

    // service state listeners
    private final List m_stateListeners = new ArrayList();

    // work queue
    private final SerialExecutor m_executor = new SerialExecutor();

    // instance factory
	private volatile Object m_instanceFactory;
	private volatile String m_instanceFactoryCreateMethod;

	// composition manager
	private volatile Object m_compositionManager;
	private volatile String m_compositionManagerGetMethod;
	private volatile Object m_compositionManagerInstance;
	
	// internal logging
    private final Logger m_logger;
    
    private final Map m_autoConfig = new HashMap();
    private final Map m_autoConfigInstance = new HashMap();
    
    private boolean m_isStarted = false;

    public ComponentImpl(BundleContext context, DependencyManager manager, Logger logger) {
        synchronized (VOID) {
            m_id = HIGHEST_ID++;
        }
    	m_logger = logger;
        m_state = new State((List) m_dependencies.clone(), false, false, false);
        m_context = context;
        m_manager = manager;
        m_callbackInit = "init";
        m_callbackStart = "start";
        m_callbackStop = "stop";
        m_callbackDestroy = "destroy";
        m_implementation = null;
        m_autoConfig.put(BundleContext.class, Boolean.TRUE);
        m_autoConfig.put(ServiceRegistration.class, Boolean.TRUE);
        m_autoConfig.put(DependencyManager.class, Boolean.TRUE);
        m_autoConfig.put(Component.class, Boolean.TRUE);
    }

    private void calculateStateChanges() {
        // see if any of the things we did caused a further change of state
        State oldState, newState;
        synchronized (m_dependencies) {
            oldState = m_state;
            newState = new State((List) m_dependencies.clone(), !oldState.isInactive(), m_isInstantiated, m_isBound);
            m_state = newState;
        }
        calculateStateChanges(oldState, newState);
    }
    
    private void calculateStateChanges(final State oldState, final State newState) {
        if (oldState.isInactive() && (newState.isTrackingOptional())) {
            m_executor.enqueue(new Runnable() {
                public void run() {
                    activateService(newState);
                }});
        }
        if (oldState.isInactive() && (newState.isWaitingForRequired())) {
            m_executor.enqueue(new Runnable() {
                public void run() {
                    startTrackingRequired(newState);
                }});
        }
        if (oldState.isWaitingForRequired() && newState.isTrackingOptional()) {
            m_executor.enqueue(new Runnable() {
                public void run() {
                    activateService(newState);
                }});
        }
        if ((oldState.isWaitingForRequired()) && newState.isInactive()) {
            m_executor.enqueue(new Runnable() {
                public void run() {
                    stopTrackingRequired(oldState);
                }});
        }
        if (oldState.isTrackingOptional() && newState.isWaitingForRequiredInstantiated()) {
            m_executor.enqueue(new Runnable() {
                public void run() {
                    // TODO as far as I can see there is nothing left to do here
                    // unbindService(newState);
                }});
        }
        if (oldState.isTrackingOptional() && newState.isWaitingForRequired()) {
            m_executor.enqueue(new Runnable() {
                public void run() {
                    deactivateService(oldState);
                }});
        }
        if (oldState.isTrackingOptional() && newState.isBound()) {
            m_executor.enqueue(new Runnable() {
                public void run() {
                    bindService(newState);
                }});
        }
        if (oldState.isTrackingOptional() && newState.isInactive()) {
            m_executor.enqueue(new Runnable() {
                public void run() {
                    deactivateService(oldState);
                    stopTrackingRequired(oldState);
                }});
        }
        if (oldState.isWaitingForRequiredInstantiated() && newState.isWaitingForRequired()) {
            m_executor.enqueue(new Runnable() {
                public void run() {
                    deactivateService(oldState);
                }});
        }
        if (oldState.isWaitingForRequiredInstantiated() && newState.isInactive()) {
            m_executor.enqueue(new Runnable() {
                public void run() {
                    deactivateService(oldState);
                    stopTrackingRequired(oldState);
                }});
        }
        if (oldState.isWaitingForRequiredInstantiated() && newState.isBound()) {
            m_executor.enqueue(new Runnable() {
                public void run() {
                    bindService(newState);
                }});
        }
        if (oldState.isBound() && newState.isWaitingForRequiredInstantiated()) {
            m_executor.enqueue(new Runnable() {
                public void run() {
                    unbindService(oldState);
                }});
        }
        if (oldState.isBound() && newState.isWaitingForRequired()) {
            m_executor.enqueue(new Runnable() {
                public void run() {
                    unbindService(oldState);
                    deactivateService(oldState);
                }});
        }
        if (oldState.isBound() && newState.isInactive()) {
            m_executor.enqueue(new Runnable() {
                public void run() {
                    unbindService(oldState);
                    deactivateService(oldState);
                    stopTrackingRequired(oldState);
                }});
        }
        m_executor.execute();
    }
    
    // TODO fix code duplication between add(Dependency) and add(List)
    public Component add(final Dependency dependency) {
    	State oldState, newState;
        synchronized (m_dependencies) {
        	oldState = m_state;
            m_dependencies.add(dependency);
        }
        
        // if we're inactive, don't do anything, otherwise we might want to start
        // the dependency
        if (!oldState.isInactive()) {
            // if the dependency is required, it should be started regardless of the state
            // we're in
            if (dependency.isRequired()) {
                ((DependencyActivation) dependency).start(this);
            }
            else {
                // if the dependency is optional, it should only be started if we're in
                // bound state
                if (oldState.isBound()) {
                    ((DependencyActivation) dependency).start(this);
                }
            }
        }

        synchronized (m_dependencies) {
            oldState = m_state;
            // starting the dependency above might have triggered another state change, so
            // we have to fetch the current state again
            newState = new State((List) m_dependencies.clone(), !oldState.isInactive(), m_isInstantiated, m_isBound);
            m_state = newState;
        }
        calculateStateChanges(oldState, newState);
        return this;
    }
    
    public Component add(List dependencies) {
        State oldState, newState;
        synchronized (m_dependencies) {
            oldState = m_state;
            for (int i = 0; i < dependencies.size(); i++) {
                m_dependencies.add(dependencies.get(i));
            }
        }
        
        // if we're inactive, don't do anything, otherwise we might want to start
        // the dependencies
        if (!oldState.isInactive()) {
            for (int i = 0; i < dependencies.size(); i++) {
                Dependency dependency = (Dependency) dependencies.get(i);
                // if the dependency is required, it should be started regardless of the state
                // we're in
                if (dependency.isRequired()) {
                    ((DependencyActivation) dependency).start(this);
                }
                else {
                    // if the dependency is optional, it should only be started if we're in
                    // bound state
                    if (oldState.isBound()) {
                        ((DependencyActivation) dependency).start(this);
                    }
                }
            }
        }

        synchronized (m_dependencies) {
            oldState = m_state;
            // starting the dependency above might have triggered another state change, so
            // we have to fetch the current state again
            newState = new State((List) m_dependencies.clone(), !oldState.isInactive(), m_isInstantiated, m_isBound);
            m_state = newState;
        }
        calculateStateChanges(oldState, newState);
        return this;
    }

    public Component remove(Dependency dependency) {
    	State oldState, newState;
        synchronized (m_dependencies) {
        	oldState = m_state;
            m_dependencies.remove(dependency);
        }
        if (oldState.isAllRequiredAvailable() || ((oldState.isWaitingForRequired() || oldState.isWaitingForRequiredInstantiated()) && dependency.isRequired())) {
        	((DependencyActivation) dependency).stop(this);
        }
        synchronized (m_dependencies) {
            // starting the dependency above might have triggered another state change, so
            // we have to fetch the current state again
            oldState = m_state;
            newState = new State((List) m_dependencies.clone(), !oldState.isInactive(), m_isInstantiated, m_isBound);
            m_state = newState;
        }
        calculateStateChanges(oldState, newState);
        return this;
    }

    public List getDependencies() {
        synchronized (m_dependencies) {
            return (List) m_dependencies.clone();
        }
    }

    public ServiceRegistration getServiceRegistration() {
        return m_registration;
    }

    public Object getService() {
        return m_serviceInstance;
    }
    
    public Component getServiceInterface() {
        return this;
    }

    public void dependencyAvailable(final Dependency dependency) {
    	State oldState, newState;
        synchronized (m_dependencies) {
        	oldState = m_state;
            newState = new State((List) m_dependencies.clone(), !oldState.isInactive(), m_isInstantiated, m_isBound);
            m_state = newState;
        }
        if (newState.isAllRequiredAvailable() || newState.isWaitingForRequiredInstantiated()) {
        	updateInstance(dependency);
        }
        calculateStateChanges(oldState, newState);
    }

    public void dependencyChanged(final Dependency dependency) {
    	State state;
        synchronized (m_dependencies) {
        	state = m_state;
        }
        if (state.isAllRequiredAvailable()) {
        	updateInstance(dependency);
        }
    }

    public void dependencyUnavailable(final Dependency dependency) {
    	State oldState, newState;
        synchronized (m_dependencies) {
        	oldState = m_state;
            newState = new State((List) m_dependencies.clone(), !oldState.isInactive(), m_isInstantiated, m_isBound);
            m_state = newState;
        }
        if (newState.isAllRequiredAvailable()) {
        	updateInstance(dependency);
        }
        calculateStateChanges(oldState, newState);
    }

    public void start() {
        boolean needsStarting = false;
        synchronized (this) {
            if (!m_isStarted) {
                m_isStarted = true;
                needsStarting = true;
            }
        }
        if (needsStarting) {
	        State oldState, newState;
	        synchronized (m_dependencies) {
	            oldState = m_state;
	            newState = new State((List) m_dependencies.clone(), true, m_isInstantiated, m_isBound);
	            m_state = newState;
	        }
	        calculateStateChanges(oldState, newState);
    	}
    }

    public void stop() {
        boolean needsStopping = false;
        synchronized (this) {
            if (m_isStarted) {
                m_isStarted = false;
                needsStopping = true;
            }
        }
        if (needsStopping) {
	        State oldState, newState;
	        synchronized (m_dependencies) {
	            oldState = m_state;
	            newState = new State((List) m_dependencies.clone(), false, m_isInstantiated, m_isBound);
	            m_state = newState;
	        }
	        calculateStateChanges(oldState, newState);
    	}
    }

    public synchronized Component setInterface(String serviceName, Dictionary properties) {
	    ensureNotActive();
	    m_serviceName = serviceName;
	    m_serviceProperties = properties;
	    return this;
	}

	public synchronized Component setInterface(String[] serviceName, Dictionary properties) {
	    ensureNotActive();
	    m_serviceName = serviceName;
	    m_serviceProperties = properties;
	    return this;
	}

	public synchronized Component setCallbacks(String init, String start, String stop, String destroy) {
	    ensureNotActive();
	    m_callbackInit = init;
	    m_callbackStart = start;
	    m_callbackStop = stop;
	    m_callbackDestroy = destroy;
	    return this;
	}
	
    public synchronized Component setCallbacks(Object instance, String init, String start, String stop, String destroy) {
        ensureNotActive();
        m_callbackInstance = instance;
        m_callbackInit = init;
        m_callbackStart = start;
        m_callbackStop = stop;
        m_callbackDestroy = destroy;
        return this;
    }
	
	public synchronized Component setImplementation(Object implementation) {
	    ensureNotActive();
	    m_implementation = implementation;
	    return this;
	}

	public synchronized Component setFactory(Object factory, String createMethod) {
	    ensureNotActive();
		m_instanceFactory = factory;
		m_instanceFactoryCreateMethod = createMethod;
		return this;
	}

	public synchronized Component setFactory(String createMethod) {
		return setFactory(null, createMethod);
	}

	public synchronized Component setComposition(Object instance, String getMethod) {
	    ensureNotActive();
		m_compositionManager = instance;
		m_compositionManagerGetMethod = getMethod;
		return this;
	}

	public synchronized Component setComposition(String getMethod) {
		return setComposition(null, getMethod);
	}

	public String toString() {
	    return this.getClass().getSimpleName() + "[" + m_serviceName + " " + m_implementation + "]";
	}

	public synchronized Dictionary getServiceProperties() {
	    if (m_serviceProperties != null) {
	        return (Dictionary) ((Hashtable) m_serviceProperties).clone();
	    }
	    return null;
	}

	public Component setServiceProperties(Dictionary serviceProperties) {
	    boolean needsProperties = false;
	    Dictionary properties = null;
	    synchronized (this) {
	        m_serviceProperties = serviceProperties;
	        if ((m_registration != null) && (m_serviceName != null)) {
	            properties = calculateServiceProperties();
	            needsProperties = true;
	        }
	    }
	    if (needsProperties) {
	        m_registration.setProperties(properties);
	    }
	    return this;
	}

	// service state listener methods
	public void addStateListener(ComponentStateListener listener) {
    	synchronized (m_stateListeners) {
		    m_stateListeners.add(listener);
    	}
    	// when we register as a listener and the service is already started
    	// make sure we invoke the right callbacks so the listener knows
    	State state;
    	synchronized (m_dependencies) {
    		state = m_state;
    	}
    	if (state.isBound()) {
    		listener.starting(this);
    		listener.started(this);
    	}
	}

	public void removeStateListener(ComponentStateListener listener) {
    	synchronized (m_stateListeners) {
    		m_stateListeners.remove(listener);
    	}
	}

	public void removeStateListeners() {
    	synchronized (m_stateListeners) {
    		m_stateListeners.clear();
    	}
	}

	private void stateListenersStarting() {
		ComponentStateListener[] list = getListeners();
		for (int i = 0; i < list.length; i++) {
		    try {
		        list[i].starting(this);
		    }
		    catch (Throwable t) {
		        m_logger.log(Logger.LOG_ERROR, "Error invoking listener starting method.", t);
		    }
		}
	}

	private void stateListenersStarted() {
        ComponentStateListener[] list = getListeners();
        for (int i = 0; i < list.length; i++) {
            try {
                list[i].started(this);
            }
            catch (Throwable t) {
                m_logger.log(Logger.LOG_ERROR, "Error invoking listener started method.", t);
            }
        }
    }

    private void stateListenersStopping() {
        ComponentStateListener[] list = getListeners();
        for (int i = 0; i < list.length; i++) {
            try {
                list[i].stopping(this);
            }
            catch (Throwable t) {
                m_logger.log(Logger.LOG_ERROR, "Error invoking listener stopping method.", t);
            }
        }
    }

    private void stateListenersStopped() {
        ComponentStateListener[] list = getListeners();
        for (int i = 0; i < list.length; i++) {
            try {
                list[i].stopped(this);
            }
            catch (Throwable t) {
                m_logger.log(Logger.LOG_ERROR, "Error invoking listener stopped method.", t);
            }
        }
    }

	private ComponentStateListener[] getListeners() {
		synchronized (m_stateListeners) {
			return (ComponentStateListener[]) m_stateListeners.toArray(SERVICE_STATE_LISTENER_TYPE);
		}
	}

    private void activateService(State state) {
        String init;
        synchronized (this) {
            init = m_callbackInit;
        }
        // service activation logic, first we initialize the service instance itself
        // meaning it is created if necessary and the bundle context is set
        initService();
        // now is the time to configure the service, meaning all required
        // dependencies will be set and any callbacks called
        configureService(state);
        // flag that our instance has been created
        m_isInstantiated = true;
        // then we invoke the init callback so the service can further initialize
        // itself
        invoke(init);
        // see if any of this caused further state changes
        calculateStateChanges();
    }

    private void bindService(State state) {
        String start;
        synchronized (this) {
            start = m_callbackStart;
        }
        
        // configure service with extra-dependencies which might have been added from init() method.
        configureServiceWithExtraDependencies(state);
        // inform the state listeners we're starting
        stateListenersStarting();
        // invoke the start callback, since we're now ready to be used
        invoke(start);
        // start tracking optional services
        startTrackingOptional(state);
        // register the service in the framework's service registry
        registerService();
        // inform the state listeners we've started
        stateListenersStarted();
    }
    
    private void configureServiceWithExtraDependencies(State state) {
        Iterator i = state.getDependencies().iterator();
        while (i.hasNext()) {
            Dependency dependency = (Dependency) i.next();
            if (dependency.isAutoConfig() && dependency.isInstanceBound()) {
                configureImplementation(dependency.getAutoConfigType(), dependency.getAutoConfigInstance(), dependency.getAutoConfigName());
            }
        }
    }

    private void unbindService(State state) {
        String stop;
        synchronized (this) {
            stop = m_callbackStop;
        }
        // service deactivation logic, first inform the state listeners
        // we're stopping
        stateListenersStopping();
        // then, unregister the service from the framework
        unregisterService();
        // stop tracking optional services
        stopTrackingOptional(state);
        // invoke the stop callback
        invoke(stop);
        // inform the state listeners we've stopped
        stateListenersStopped();
    }

    private void deactivateService(State state) {
        String destroy;
        synchronized (this) {
            destroy = m_callbackDestroy;
        }
        // flag that our instance was destroyed
        m_isInstantiated = false;
        // invoke the destroy callback
        invoke(destroy);
        // destroy the service instance
        destroyService(state);
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
                m_logger.log(Logger.LOG_WARNING, "Invocation of '" + methodName + "' failed.", e.getCause());
            }
            catch (Exception e) {
                m_logger.log(Logger.LOG_WARNING, "Could not invoke '" + methodName + "'.", e);
            }
        }
    }

    private void startTrackingOptional(State state) {
        Iterator i = state.getDependencies().iterator();
        while (i.hasNext()) {
            Dependency dependency = (Dependency) i.next();
            if (!dependency.isRequired()) {
                ((DependencyActivation) dependency).start(this);
            }
        }
    }
    
    private void stopTrackingOptional(State state) {
        Iterator i = state.getDependencies().iterator();
        while (i.hasNext()) {
            Dependency dependency = (Dependency) i.next();
            if (!dependency.isRequired()) {
                ((DependencyActivation) dependency).stop(this);
            }
        }
    }

    private void startTrackingRequired(State state) {
        Iterator i = state.getDependencies().iterator();
        while (i.hasNext()) {
            Dependency dependency = (Dependency) i.next();
            if (dependency.isRequired()) {
                ((DependencyActivation) dependency).start(this);
            }
        }
    }

    private void stopTrackingRequired(State state) {
        Iterator i = state.getDependencies().iterator();
        while (i.hasNext()) {
            Dependency dependency = (Dependency) i.next();
            if (dependency.isRequired()) {
                ((DependencyActivation) dependency).stop(this);
            }
        }
    }

    private Object createInstance(Class clazz) throws SecurityException, NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Constructor constructor = clazz.getConstructor(VOID);
		constructor.setAccessible(true);
        return constructor.newInstance(null);
    }

    public void initService() {
    	if (m_serviceInstance == null) {
	        if (m_implementation instanceof Class) {
	            // instantiate
	            try {
	            	m_serviceInstance = createInstance((Class) m_implementation);
	            }
	            catch (Exception e) {
	                m_logger.log(Logger.LOG_ERROR, "Could not create service instance of class " + m_implementation + ".", e);
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
    						m_serviceInstance = InvocationUtil.invokeMethod(factory, factory.getClass(), m_instanceFactoryCreateMethod, new Class[][] {{}}, new Object[][] {{}}, false);
    					}
    		        	catch (Exception e) {
    	                    m_logger.log(Logger.LOG_ERROR, "Could not create service instance using factory " + factory + " method " + m_instanceFactoryCreateMethod + ".", e);
    					}
		        	}
	        	}
	        	if (m_serviceInstance == null) {
	        	    if (m_implementation == null) {
	        	        m_logger.log(Logger.LOG_ERROR, "Implementation cannot be null.");
	        	    }
	        	    m_serviceInstance = m_implementation;
	        	}
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

    public synchronized Component setAutoConfig(Class clazz, boolean autoConfig) {
        m_autoConfig.put(clazz, Boolean.valueOf(autoConfig));
        return this;
    }
    
    public synchronized Component setAutoConfig(Class clazz, String instanceName) {
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
    
    private void configureService(State state) {
        // configure all services (the optional dependencies might be configured
        // as null objects but that's what we want at this point)
        configureServices(state);
    }

    private void destroyService(State state) {
        unconfigureServices(state);
        m_serviceInstance = null;
    }

    private void registerService() {
        if (m_serviceName != null) {
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
                    registration = m_context.registerService((String) m_serviceName, m_serviceInstance, properties);
                }
                else {
                    registration = m_context.registerService((String[]) m_serviceName, m_serviceInstance, properties);
                }
                wrapper.setServiceRegistration(registration);
            }
            catch (IllegalArgumentException iae) {
                m_logger.log(Logger.LOG_ERROR, "Could not register service " + m_serviceInstance, iae);
                // set the registration to an illegal state object, which will make all invocations on this
                // wrapper fail with an ISE (which also occurs when the SR becomes invalid)
                wrapper.setIllegalState();
            }
        }
        m_isBound = true;
    }

	private Dictionary calculateServiceProperties() {
		Dictionary properties = new Properties();
		addTo(properties, m_serviceProperties);
		for (int i = 0; i < m_dependencies.size(); i++) {
			Dependency d = (Dependency) m_dependencies.get(i);
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

    private void unregisterService() {
        m_isBound = false;
        if (m_serviceName != null) {
            m_registration.unregister();
            configureImplementation(ServiceRegistration.class, NULL_REGISTRATION);
            m_registration = null;
        }
    }

    private void updateInstance(Dependency dependency) {
        if (dependency.isAutoConfig()) {
            configureImplementation(dependency.getAutoConfigType(), dependency.getAutoConfigInstance(), dependency.getAutoConfigName());
        }
        if (dependency.isPropagated() && m_registration != null) {
            m_registration.setProperties(calculateServiceProperties());
        }
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
    	Object[] instances = getCompositionInstances();
    	if (instances != null) {
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
    
    public Object[] getCompositionInstances() {
        Object[] instances = null;
        if (m_compositionManagerGetMethod != null) {
            if (m_compositionManager != null) {
                m_compositionManagerInstance = m_compositionManager;
            }
            else {
                m_compositionManagerInstance = m_serviceInstance;
            }
            if (m_compositionManagerInstance != null) {
                try {
                    instances = (Object[]) InvocationUtil.invokeMethod(m_compositionManagerInstance, m_compositionManagerInstance.getClass(), m_compositionManagerGetMethod, new Class[][] {{}}, new Object[][] {{}}, false);
                }
                catch (Exception e) {
                    m_logger.log(Logger.LOG_ERROR, "Could not obtain instances from the composition manager.", e);
                    instances = m_serviceInstance == null ? new Object[] {} : new Object[] { m_serviceInstance };
                }
            }
        }
        else {
            instances = m_serviceInstance == null ? new Object[] {} : new Object[] { m_serviceInstance };
        }
        return instances;
    }

    private void configureImplementation(Class clazz, Object instance) {
        configureImplementation(clazz, instance, null);
    }

    private void configureServices(State state) {
        Iterator i = state.getDependencies().iterator();
        while (i.hasNext()) {
            Dependency dependency = (Dependency) i.next();
            if (dependency.isAutoConfig()) {
                configureImplementation(dependency.getAutoConfigType(), dependency.getAutoConfigInstance(), dependency.getAutoConfigName());
            }
            if (dependency.isRequired()) {
                dependency.invokeAdded(this);
            }
        }
    }

    private void unconfigureServices(State state) {
        Iterator i = state.getDependencies().iterator();
        while (i.hasNext()) {
            Dependency dependency = (Dependency) i.next();
            if (dependency.isRequired()) {
                dependency.invokeRemoved(this);
            }
        }
    }

    protected void ensureNotActive() {
    	State state;
    	synchronized (m_dependencies) {
    		state = m_state;
    	}
    	if (!state.isInactive()) {
            throw new IllegalStateException("Cannot modify state while active.");
        }
    }
    
    public boolean isRegistered() {
    	State state;
    	synchronized (m_dependencies) {
    		state = m_state;
    	}
        return (state.isAllRequiredAvailable());
    }
    
    public boolean isInstantiated() {
        State state;
        synchronized (m_dependencies) {
            state = m_state;
        }
        return (state.isTrackingOptional() || state.isBound() || state.isWaitingForRequiredInstantiated());
    }
    
    // ServiceComponent interface
    
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
    
    public ComponentDependencyDeclaration[] getComponentDependencies() {
        List deps = getDependencies();
        if (deps != null) {
            ComponentDependencyDeclaration[] result = new ComponentDependencyDeclaration[deps.size()];
            for (int i = 0; i < result.length; i++) {
                Dependency dep = (Dependency) deps.get(i);
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
            Object implementation = m_implementation;
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
    
    public String getClassName() {
        Object serviceInstance = m_serviceInstance;
        if (serviceInstance != null) {
            return serviceInstance.getClass().getName();
        } 
        
        Object implementation = m_implementation;
        if (implementation != null) {
            if (implementation instanceof Class) {
                return ((Class) implementation).getName();
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

    public int getState() {
        return (isRegistered() ? 1 : 0);
    }
    
    public long getId() {
        return m_id;
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
    
    public DependencyManager getDependencyManager() {
        return m_manager;
    }
    
    static {
        NULL_REGISTRATION = (ServiceRegistration) Proxy.newProxyInstance(ComponentImpl.class.getClassLoader(), new Class[] {ServiceRegistration.class}, new DefaultNullObject());
    }

    public BundleContext getBundleContext() {
        return m_context;
    }

    public int compareTo(Object object) {
        if (object instanceof ComponentImpl) {
            ComponentImpl other = (ComponentImpl) object;
            long id1 = this.getBundleContext().getBundle().getBundleId();
            long id2 = ((ComponentImpl) other).getBundleContext().getBundle().getBundleId();
            if (id1 == id2) {
                return (int)(this.m_id - other.m_id);
            }
            return (int)(id1 - id2);
        }
        return -1;
    }
}
