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
package org.apache.felix.ipojo;

import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.extender.internal.linker.InstanceBundleContextAware;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.util.Logger;
import org.apache.felix.ipojo.util.Property;
import org.osgi.framework.BundleContext;

import java.lang.reflect.*;
import java.util.*;

/**
 * This class defines the container of primitive instances. It manages content initialization
 * and handlers cooperation.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class InstanceManager implements ComponentInstance, InstanceStateListener, InstanceBundleContextAware {
    /**
     * The name of the component instance.
     */
    protected String m_name;

    /**
     * The name of the component type implementation class.
     */
    protected String m_className;

    /**
     * The handler object list.
     */
    protected final HandlerManager[] m_handlers;

    /**
     * The current instance state ({@link ComponentInstance#STOPPED} at the beginning).
     * Possible value are
     * <li>{@link ComponentInstance#INVALID}</li>
     * <li>{@link ComponentInstance#VALID}</li>
     * <li>{@link ComponentInstance#DISPOSED}</li>
     * <li>{@link ComponentInstance#STOPPED}</li>
     */
    protected int m_state = STOPPED;

    /**
     * The instance state listener list.
     *
     * @see InstanceStateListener
     */
    protected List m_listeners = null;

    /**
     * The content of the current instance.
     */
    protected List m_pojoObjects;

    /**
     * The instance factory.
     */
    private final ComponentFactory m_factory;

    /**
     * The instance logger.
     */
    private final Logger m_logger;

    /**
     * The instance description.
     */
    private final PrimitiveInstanceDescription m_description;

    /**
     * The bundle context of the instance.
     */
    private final BundleContext m_context;

    /**
     * The map [field, {@link FieldInterceptor} list] storing interceptors monitoring fields.
     * Once configured, this map can't change.
     */
    private Map m_fieldRegistration;

    /**
     * the map [method identifier, {@link MethodInterceptor} list] interested
     * by the method.
     * Once configured, this map can't change.
     */
    private Map m_methodRegistration;

    /**
     * the map (sorted by parameter index) or {@link ConstructorInjector} interested by
     * injecting constructor parameter.
     * Once configured, this list can't change.
     */
    private Map m_constructorRegistration;

    /**
     * The manipulated class.
     * Once set, this field doesn't change.
     */
    private Class m_clazz;

    /**
     * The factory method used to create content objects.
     * If <code>null</code>, the regular constructor is used.
     * Once set, this field is immutable.
     */
    private String m_factoryMethod = null;

    /**
     * Is the component instance state changing?
     */
    private boolean m_inTransition = false;

    /**
     * The queue of stored state changed.
     */
    private List m_stateQueue = new ArrayList();

    /**
     * The map of [field, value], storing POJO managed
     * field value.
     */
    private Map m_fields = new HashMap();

    /**
     * The Map storing the Method objects by ids.
     * [id=>{@link Method}].
     */
    private Map m_methods =  Collections.synchronizedMap(new HashMap());

    /**
     * The instance's bundle context.
     */
    private BundleContext m_instanceContext;


    /**
     * Creates a new Component Manager.
     * The instance is not initialized.
     *
     * @param factory  the factory managing the instance manager
     * @param context  the bundle context to give to the instance
     * @param handlers handler object array
     */
    public InstanceManager(ComponentFactory factory, BundleContext context, HandlerManager[] handlers) {
        m_factory = factory;
        m_context = context;
        m_handlers = handlers;
        m_description = new PrimitiveInstanceDescription(m_factory.getComponentDescription(), this);
        m_logger = new Logger(m_context, this);
    }

    /**
     * Sets the instance bundle context.
     *
     * @param context the bundle context of the bundle having declared the instance
     * @since 1.11.2
     */
    public void setInstanceBundleContext(BundleContext context) {
        m_instanceContext = context;
    }

    /**
     * The instance logger.
     *
     * @return the logger
     */
    public Logger getLogger() {
        return m_logger;
    }

    /**
     * Configures the instance manager.
     * Sets the class name, and the instance name as well as the factory method.
     * Initializes handlers.
     *
     * @param metadata      the component type metadata
     * @param configuration the configuration of the instance
     * @throws ConfigurationException if the metadata are not correct
     */
    public void configure(Element metadata, Dictionary configuration) throws ConfigurationException {
        m_className = metadata.getAttribute("classname");

        // Add the name
        m_name = (String) configuration.get(Factory.INSTANCE_NAME_PROPERTY);

        // Check if an object is injected in the instance
        Object obj = configuration.get("instance.object");
        if (obj != null) {
            m_pojoObjects = new ArrayList(1);
            m_pojoObjects.add(obj);
        }

        // Get the factory method if presents.
        m_factoryMethod = (String) metadata.getAttribute("factory-method");

        // Check if we have an instance bundle context given as property, it should not be used,
        // but it's a way to provide the instance bundle context when using the Factory service.
        // The instance context is set only if it was not already set.
        if (m_instanceContext == null) {
            m_instanceContext = (BundleContext) configuration.get("instance.bundle.context");
        }

        // Create the standard handlers and add these handlers to the list
        for (HandlerManager handler : m_handlers) {
            handler.init(this, metadata, configuration);
        }


        // Fix for Felix-3576
        handleBCInjections();


        // Check that the constructor parameter are continuous.
        if (m_constructorRegistration != null) {
            for (int i = 0; i < m_constructorRegistration.size(); i++) {
                if (!m_constructorRegistration.containsKey(i)) {
                    throw new ConfigurationException("The constructor parameter " + i + " is not managed");
                }
            }
        }
    }

    /**
     * BundleContext injection is not registered with the InstanceManager.
     * We're iterating through factory's all constructors and register first
     * BundleContext parameter as constructor injection. So rest of the code
     * don't have to do anything to handle BundleContext mixed with other
     * injections.
     *
     * @throws ConfigurationException
     */
    private void handleBCInjections() throws ConfigurationException {
        MethodMetadata[] constructors = getFactory().getPojoMetadata().getConstructors();
        for (MethodMetadata constructor : constructors) {
            String[] ctorArguments = constructor.getMethodArguments();

            for (int index = 0; index < ctorArguments.length; index++) {
                if (ctorArguments[index].equals(BundleContext.class.getName())
                        && (m_constructorRegistration == null || !m_constructorRegistration.containsKey(index))) {
                    //Check if its used with only other injections.
                    boolean injectionsConsistent = true;
                    for (int siblingIndex = 0; siblingIndex < ctorArguments.length; siblingIndex++) {
                        if (siblingIndex == index) {
                            continue;
                        }

                        String injectionType = ctorArguments[siblingIndex];
                        if (m_constructorRegistration != null && m_constructorRegistration.containsKey(siblingIndex)) {
                            ConstructorInjector siblingInjector =
                                    (ConstructorInjector) m_constructorRegistration.get(siblingIndex);
                            Class injectorClass = siblingInjector.getConstructorParameterType(siblingIndex);

                            if (injectorClass != null && !injectorClass.getName().equals(injectionType)) {
                                injectionsConsistent = false;
                                break;
                            }
                        } else {
                            injectionsConsistent = false;
                            break;
                        }
                    }

                    if (injectionsConsistent) {
                        Property contextInjection =
                                new Property("__context", null, null, index, null,
                                        BundleContext.class.getName(), this, null);

                        contextInjection.setValue(getContext());
                        register(index, contextInjection);

                        // We register the first valid BC injection.
                        break;
                    }
                }
            }
        }
    }

    /**
     * Gets the description of the current instance.
     *
     * @return the instance description.
     * @see org.apache.felix.ipojo.ComponentInstance#getInstanceDescription()
     */
    public InstanceDescription getInstanceDescription() {
        return m_description;
    }

    /**
     * Gets the list of handlers plugged (i.e. attached) on the instance.
     * This method does not need a synchronized block as the handler set is constant.
     *
     * @return the handler array of plugged handlers.
     */
    public Handler[] getRegisteredHandlers() {
        Handler[] handler = new Handler[m_handlers.length];
        for (int i = 0; i < m_handlers.length; i++) {
            handler[i] = m_handlers[i].getHandler();
        }
        return handler;
    }

    /**
     * Returns a specified handler.
     * This method allows cross-handler interactions.
     * This must does not need a synchronized block as the handler set is constant.
     *
     * @param name the class name of the handler to find or its qualified name (namespace:name)
     * @return the handler, or null if not found
     */
    public Handler getHandler(String name) {
        for (int i = 0; i < m_handlers.length; i++) {
            HandlerFactory fact = (HandlerFactory) m_handlers[i].getHandler().getHandlerManager().getFactory();
            if (fact.getHandlerName().equals(name)) {
                return m_handlers[i].getHandler();
            }
        }
        return null;
    }

    /**
     * Gives access to a field value of the first created pojo.
     * This method processes by analyzing both managed fields and pojo fields (by reflection).
     * If no pojo were already created try only on managed fields.
     *
     * @param fieldName the field name.
     * @return the field value, <code>null</code> is returned if the value is managed and not already set.
     */
    public synchronized Object getFieldValue(String fieldName) {
        if (m_pojoObjects == null) {
            return getFieldValue(fieldName, null);
        } else {
            return getFieldValue(fieldName, m_pojoObjects.get(0)); // Use the first pojo.
        }
    }

    /**
     * Gives access to a field value to the given created pojo.
     * This method processes by analyzing both managed fields and pojo fields (by reflection).
     * If the given pojo is <code>null</code>, tries only on managed fields.
     *
     * @param fieldName the field name.
     * @param pojo      the pojo on which computing field value.
     * @return the field value, <code>null</code> is returned if the value is managed and not already set.
     */
    public synchronized Object getFieldValue(String fieldName, Object pojo) {
        Object setByContainer = null;

        if (m_fields != null) {
            setByContainer = m_fields.get(fieldName);
        }

        if (setByContainer == null && pojo != null) { // In the case of no given pojo, return null.
            // If null either the value was not already set or has the null value.
            try {
                Field field = pojo.getClass().getDeclaredField(fieldName);
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                return field.get(pojo);
            } catch (SecurityException e) {
                m_logger.log(Logger.ERROR, "Cannot reflect on field " + fieldName + " to obtain the value : " + e.getMessage());
            } catch (NoSuchFieldException e) {
                m_logger.log(Logger.ERROR, "Cannot reflect on field " + fieldName + " to obtain the value : " + e.getMessage());
            } catch (IllegalArgumentException e) {
                m_logger.log(Logger.ERROR, "Cannot reflect on field " + fieldName + " to obtain the value : " + e.getMessage());
            } catch (IllegalAccessException e) {
                m_logger.log(Logger.ERROR, "Cannot reflect on field " + fieldName + " to obtain the value : " + e.getMessage());
            }
            return null;
        } else {
            return setByContainer;
        }
    }

    /**
     * Starts the instance manager.
     * This method activates plugged handlers,
     * and computes the initial instance state.
     */
    public void start() {
        synchronized (this) {
            if (m_state != STOPPED) { // Instance already started
                return;
            } else {
                m_state = -2; // Temporary state.
            }
        }

        // Plug handler descriptions
        Handler[] handlers = getRegisteredHandlers();
        for (int i = 0; i < handlers.length; i++) {
            m_description.addHandler(handlers[i].getDescription());
        }

        for (int i = 0; i < m_handlers.length; i++) {
            m_handlers[i].addInstanceStateListener(this);
            try {
                m_handlers[i].start();
            } catch (IllegalStateException e) {
                m_logger.log(Logger.ERROR, e.getMessage());
                stop();
                throw e;
            }
        }

        // Is an object already contained (i.e. injected)
        if (m_pojoObjects != null && !m_pojoObjects.isEmpty()) {
            managedInjectedObject();
        }

        for (int i = 0; i < m_handlers.length; i++) {
            if (m_handlers[i].getState() != VALID) {
                setState(INVALID);
                return;
            }
        }
        setState(VALID);
    }

    /**
     * Stops the instance manager.
     * This methods sets the instance state to {@link ComponentInstance#STOPPED},
     * disables attached handlers, and notifies listeners ({@link InstanceStateListener})
     * of the instance stopping process.
     */
    public void stop() {
        List listeners = null;
        synchronized (this) {
            if (m_state == STOPPED) { // Instance already stopped
                return;
            }
            m_stateQueue.clear();
            m_inTransition = false;
        }

        setState(INVALID); // Must be called outside a synchronized block.

        // Stop all the handlers
        for (int i = m_handlers.length - 1; i > -1; i--) {
            m_handlers[i].removeInstanceStateListener(this);
            m_handlers[i].stop();
        }

        synchronized (this) {
            m_state = STOPPED;
            if (m_listeners != null) {
                listeners = new ArrayList(m_listeners); // Stack confinement
            }
            m_pojoObjects = null;
        }

        if (listeners != null) {
            for (int i = 0; i < listeners.size(); i++) {
                ((InstanceStateListener) listeners.get(i)).stateChanged(this, STOPPED);
            }
        }
    }

    /**
     * Disposes the instance.
     * This method does the following process:
     * <li>Stop the instance is not {@link ComponentInstance#STOPPED}</li>
     * <li>Notifies listeners {@link InstanceStateListener} of the destruction</li>
     * <li>Disposes attached handlers</li>
     * <li>Clears structures</li>
     *
     * @see org.apache.felix.ipojo.ComponentInstance#dispose()
     */
    public void dispose() {
        List listeners = null;
        int state; // Will be confined in stack.
        synchronized (this) {
            state = m_state; // Stack confinement
            if (m_listeners != null) {
                listeners = new ArrayList(m_listeners); // Stack confinement
            }
            m_listeners = null;
        }

        if (state > STOPPED) { // Valid or invalid
            stop(); // Does not hold the lock.
        }

        synchronized (this) {
            m_state = DISPOSED;
        }

        for (int i = 0; listeners != null && i < listeners.size(); i++) {
            ((InstanceStateListener) listeners.get(i)).stateChanged(this, DISPOSED);
        }

        for (int i = m_handlers.length - 1; i > -1; i--) {
            m_handlers[i].dispose();
        }

        synchronized (this) {
            m_factory.disposed(this);
            m_clazz = null;
            // Do not clean registration map, so injection still works
            // after disposal for late callbacks.
        }
    }

    /**
     * Sets the state of the component instance.
     * If the state changes, calls the {@link PrimitiveHandler#stateChanged(int)} method on the attached handlers.
     * This method has a reentrant mechanism. If in the flow of the first call the method is called another times,
     * the second call is stored and executed after the first one finished.
     *
     * @param state the new state
     */
    public void setState(int state) {
        int originalState = -2;
        List listeners = null;
        synchronized (this) {
            if (m_inTransition) {
                m_stateQueue.add(new Integer(state));
                return;
            }

            if (m_state != state) {
                m_inTransition = true;
                originalState = m_state; // Stack confinement.
                m_state = state;
                if (m_listeners != null) {
                    listeners = new ArrayList(m_listeners); // Stack confinement.
                }
            }
        }

        // This section can be executed only by one thread at the same time. The m_inTransition pseudo semaphore block access to this section.
        if (m_inTransition) { // Check that we are really changing.
            if (state > originalState) {
                // The state increases (Stopped = > IV, IV => V) => invoke handlers from the higher priority to the lower
                try {
                    for (int i = 0; i < m_handlers.length; i++) {
                        m_handlers[i].getHandler().stateChanged(state);
                    }
                } catch (IllegalStateException e) {
                    // When an illegal state exception happens, the instance manager must be stopped immediately.
                    stop();
                    m_logger.log(Logger.ERROR, e.getMessage(), e);
                    return;
                }
            } else {
                // The state decreases (V => IV, IV = > Stopped, Stopped => Disposed)
                try {
                    for (int i = m_handlers.length - 1; i > -1; i--) {
                        m_handlers[i].getHandler().stateChanged(state);
                    }
                } catch (IllegalStateException e) {
                    // When an illegal state exception happens, the instance manager must be stopped immediately.
                    stop();
                    m_logger.log(Logger.ERROR, e.getMessage());
                    return;
                }
            }
        }

        if (listeners != null) {
            for (int i = 0; i < listeners.size(); i++) {
                ((InstanceStateListener) listeners.get(i)).stateChanged(this, state);
            }
        }

        synchronized (this) {
            m_inTransition = false;
            if (!m_stateQueue.isEmpty()) {
                int newState = ((Integer) (m_stateQueue.remove(0))).intValue();
                setState(newState);
            }
        }
    }

    /**
     * Gets the actual state of the instance.
     * Possible values are:
     * <li>{@link ComponentInstance#INVALID}</li>
     * <li>{@link ComponentInstance#VALID}</li>
     * <li>{@link ComponentInstance#DISPOSED}</li>
     * <li>{@link ComponentInstance#STOPPED}</li>
     *
     * @return the actual state of the component instance.
     * @see org.apache.felix.ipojo.ComponentInstance#getState()
     */
    public synchronized int getState() {
        return m_state;
    }

    /**
     * Checks if the instance is started.
     * An instance is started if the state is either
     * {@link ComponentInstance#VALID} or {@link ComponentInstance#INVALID}.
     *
     * @return <code>true</code> if the instance is started.
     * @see org.apache.felix.ipojo.ComponentInstance#isStarted()
     */
    public synchronized boolean isStarted() {
        return m_state > STOPPED;
    }

    /**
     * Registers an instance state listener.
     *
     * @param listener the listener to register.
     * @see org.apache.felix.ipojo.ComponentInstance#addInstanceStateListener(org.apache.felix.ipojo.InstanceStateListener)
     */
    public synchronized void addInstanceStateListener(InstanceStateListener listener) {
        if (m_listeners == null) {
            m_listeners = new ArrayList();
        }
        m_listeners.add(listener);
    }

    /**
     * Unregisters an instance state listener.
     *
     * @param listener the listener to unregister.
     * @see org.apache.felix.ipojo.ComponentInstance#removeInstanceStateListener(org.apache.felix.ipojo.InstanceStateListener)
     */
    public synchronized void removeInstanceStateListener(InstanceStateListener listener) {
        if (m_listeners != null) {
            m_listeners.remove(listener);
            if (m_listeners.isEmpty()) {
                m_listeners = null;
            }
        }
    }

    /**
     * Gets the factory which has created the current instance.
     *
     * @return the factory of the component
     * @see org.apache.felix.ipojo.ComponentInstance#getFactory()
     */
    public ComponentFactory getFactory() {
        return m_factory;
    }

    /**
     * Loads the manipulated class.
     */
    protected void load() {
        try {
            m_clazz = m_factory.loadClass(m_className);
        } catch (ClassNotFoundException e) {
            m_logger.log(Logger.ERROR, "[" + m_name + "] Class not found during the loading phase : " + e.getMessage(), e);
            stop();
            return;
        }
    }

    /**
     * Gets the object array created by the instance.
     *
     * @return the created content objects of the component instance.
     */
    public synchronized Object[] getPojoObjects() {
        if (m_pojoObjects == null) {
            return null;
        }
        return m_pojoObjects.toArray(new Object[m_pojoObjects.size()]);
    }

    /**
     * Creates a POJO objects.
     * This method is not synchronized and does not require any locks.
     * If a {@link InstanceManager#m_factoryMethod} is specified,
     * this method called this static method to creates the object.
     * Otherwise, the methods uses the regular constructor.
     * All those methods can receive the {@link BundleContext} in
     * argument.
     *
     * @return the created object or <code>null</code> if an error
     * occurs during the creation.
     */
    protected Object createObject() {
        if (m_clazz == null) {
            load();
        }

        // The following code doesn't need to be synchronized as is deal only with immutable fields.
        Object instance = null;
        if (m_factoryMethod == null) {
            // No factory-method, we use the constructor.
            try {
                // Try to find the correct constructor.
                if (m_constructorRegistration != null) {
                    // Initialize the injected values and types
                    // We have the IM first.
                    Object[] values = new Object[m_constructorRegistration.size() + 1];
                    Class[] types = new Class[m_constructorRegistration.size() + 1];
                    values[0] = this;
                    types[0] = InstanceManager.class;

                    // Iterate over the constructor injector
                    for (int i = 0; i < m_constructorRegistration.size(); i++) {
                        ConstructorInjector injector = (ConstructorInjector)
                                m_constructorRegistration.get(new Integer(i));
                        Object v = injector.getConstructorParameter(i);
                        if (v != null) {
                            values[i + 1] = v;
                            Class t = injector.getConstructorParameterType(i);
                            if (t == null) {
                                t = v.getClass();
                            }
                            types[i + 1] = t;
                        }
                    }
                    // Find the constructor.
                    Constructor cst = m_clazz.getDeclaredConstructor(types);
                    if (!cst.isAccessible()) {
                        cst.setAccessible(true);
                    }
                    String methodId = MethodMetadata.computeMethodId(cst);
                    onEntry(null, methodId, values);
                    instance = cst.newInstance(values);
                    onExit(instance, methodId, instance);
                } else {
                    // Old semantic
                    // Try to find if there is a constructor with a bundle context as parameter :
                    try {
                        Constructor cst = m_clazz.getDeclaredConstructor(new Class[]{InstanceManager.class, BundleContext.class});
                        if (!cst.isAccessible()) {
                            cst.setAccessible(true);
                        }
                        Object[] args = new Object[]{this, m_context};
                        onEntry(null, MethodMetadata.BC_CONSTRUCTOR_ID, new Object[]{m_context});
                        instance = cst.newInstance(args);
                        onExit(instance, MethodMetadata.BC_CONSTRUCTOR_ID, instance);
                    } catch (NoSuchMethodException e) {
                        // Create an instance if no instance are already created with <init>()BundleContext
                        if (instance == null) {
                            Constructor cst = m_clazz.getDeclaredConstructor(new Class[]{InstanceManager.class});
                            if (!cst.isAccessible()) {
                                cst.setAccessible(true);
                            }
                            Object[] args = new Object[]{this};
                            onEntry(null, MethodMetadata.EMPTY_CONSTRUCTOR_ID, new Object[0]);
                            instance = cst.newInstance(args);
                            onExit(instance, MethodMetadata.EMPTY_CONSTRUCTOR_ID, instance);
                        }
                    }
                }

            } catch (IllegalAccessException e) {
                m_logger.log(Logger.ERROR,
                        "[" + m_name + "] createInstance -> The POJO constructor is not accessible : " + e.getMessage(), e);
                stop();
                throw new RuntimeException("Cannot create a POJO instance, the POJO constructor is not accessible", e);
            } catch (SecurityException e) {
                m_logger.log(
                        Logger.ERROR,
                        "["
                                + m_name
                                + "] createInstance -> The POJO constructor is not accessible (security reason) : "
                                + e.getMessage(), e
                );
                stop();
                throw new RuntimeException("Cannot create a POJO instance, the POJO constructor is not accessible", e);
            } catch (InvocationTargetException e) {
                m_logger.log(
                        Logger.ERROR,
                        "["
                                + m_name
                                + "] createInstance -> Cannot invoke the constructor method - the constructor throws an exception : "
                                + e.getTargetException().getMessage(), e.getTargetException()
                );
                onError(null, m_className, e.getTargetException());
                stop();
                throw new RuntimeException("Cannot create a POJO instance, the POJO constructor has thrown an exception", e.getTargetException());
            } catch (NoSuchMethodException e) {
                // Improve the log message because of FELIX-4455, we will see if we get better feedback.
                m_logger.log(Logger.ERROR,
                        "[" + m_name + "] iPOJO did not find a suitable constructor to create the " +
                                "object: " + e.getMessage(), e);
                stop();
                throw new RuntimeException("Cannot create a POJO instance, the POJO constructor cannot be found", e);
            } catch (Throwable e) {
                // Catch every other possible error and runtime exception.
                m_logger.log(Logger.ERROR,
                        "[" + m_name + "] createInstance -> The POJO constructor invocation failed : " + e.getMessage(), e);
                stop();
                throw new RuntimeException("Cannot create a POJO instance, the POJO constructor invocation has thrown an exception", e);
            }
        } else {
            try {
                // Build the pojo object with the factory-method.
                Method factory = null;
                // Try with the bundle context
                try {
                    factory = m_clazz.getDeclaredMethod(m_factoryMethod, new Class[]{BundleContext.class});
                    if (!factory.isAccessible()) {
                        factory.setAccessible(true);
                    }
                    Object[] args = new Object[]{m_context};
                    onEntry(null, m_className, args);
                    instance = factory.invoke(null, new Object[]{m_context});
                } catch (NoSuchMethodException e1) {
                    // Try without the bundle context
                    try {
                        factory = m_clazz.getDeclaredMethod(m_factoryMethod, new Class[0]);
                        if (!factory.isAccessible()) {
                            factory.setAccessible(true);
                        }
                        Object[] args = new Object[0];
                        onEntry(null, m_className, args);
                        instance = factory.invoke(null, args);
                    } catch (NoSuchMethodException e2) {
                        // Error : factory-method not found
                        m_logger.log(
                                Logger.ERROR,
                                "["
                                        + m_name
                                        + "] createInstance -> Cannot invoke the factory-method (method not found) : "
                                        + e2.getMessage(), e2
                        );
                        stop();
                        throw new RuntimeException("Cannot create a POJO instance, the factory-method cannot be found", e2);
                    }
                }

                // Now call the setInstanceManager method.
                // Find declaring super class.
                Class declaringClass = instance.getClass();
                Method method = null;
                while (declaringClass != null && method == null) {
                    try {
                        method = declaringClass.getDeclaredMethod("_setInstanceManager",
                                new Class[]{InstanceManager.class});
                    } catch (NoSuchMethodException e) {
                        //Do nothing
                    }

                    declaringClass = declaringClass.getSuperclass();
                }

                if (method == null) {
                    // Error : _setInstanceManager method is missing
                    m_logger
                            .log(
                                    Logger.ERROR,
                                    "["
                                            + m_name
                                            + "] createInstance -> Cannot invoke the factory-method (the _setInstanceManager method does not exist"
                            );
                    stop();
                    throw new RuntimeException("Cannot create a POJO instance, the factory-method cannot be found");
                }

                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
                method.invoke(instance, new Object[]{this});
                onExit(null, m_className, instance);

            } catch (InvocationTargetException e) {
                // Error : invocation failed
                m_logger.log(Logger.ERROR,
                        "[" + m_name + "] createInstance -> The factory-method throws an exception : " + e.getTargetException(), e.getTargetException());
                onError(null, m_className, e.getTargetException());
                stop();
                throw new RuntimeException("Cannot create a POJO instance, the factory-method has thrown an exception", e.getTargetException());
            } catch (Throwable e) {
                // Catch every other possible error and runtime exception.
                m_logger.log(Logger.ERROR,
                        "[" + m_name + "] createInstance -> The factory-method invocation failed : " + e.getMessage(), e);
                stop();
                throw new RuntimeException("Cannot create a POJO instance, the factory-method invocation has thrown an exception", e);
            }
        }
        return instance;
    }

    /**
     * Creates an instance of the content.
     * This method needs to be called once only for singleton provided service.
     * This methods call the {@link InstanceManager#createObject()} method, and adds
     * the created object to the {@link InstanceManager#m_pojoObjects} list. Then,
     * it calls the {@link PrimitiveHandler#onCreation(Object)} methods on attached
     * handlers.
     *
     * @return a new instance or <code>null</code> if an error occurs during the
     * creation.
     */
    public Object createPojoObject() {
        Object instance = createObject();

        // Add the new instance in the instance list.
        synchronized (this) {
            if (m_pojoObjects == null) {
                m_pojoObjects = new ArrayList(1);
            }
            m_pojoObjects.add(instance);
        }
        // Call createInstance on Handlers :
        for (int i = 0; i < m_handlers.length; i++) {
            // This methods must be call without the monitor lock.
            ((PrimitiveHandler) m_handlers[i].getHandler()).onCreation(instance);
        }

        return instance;
    }

    /**
     * Deletes a POJO object.
     *
     * @param pojo the pojo to remove from the list of created pojos.
     */
    public synchronized void deletePojoObject(Object pojo) {
        if (m_pojoObjects != null) {
            m_pojoObjects.remove(pojo);
        }
    }

    /**
     * Gets the first object created by the instance.
     * If no object created, creates and returns a POJO object.
     * This methods call the {@link InstanceManager#createObject()} method, and adds
     * the created object to the {@link InstanceManager#m_pojoObjects} list. Then,
     * it calls the {@link PrimitiveHandler#onCreation(Object)} methods on attached
     * handlers.
     * <br/>
     * <p>
     * <b>TODO</b> this method has a potential race condition if two threads require a pojo
     * object at the same time. Only one object will be created, but the second thread
     * can receive the created object before the {@link PrimitiveHandler#onCreation(Object)}
     * calls.
     * </p>
     *
     * @return the pojo object of the component instance to use for singleton component
     */
    public Object getPojoObject() {
        Object pojo = null;
        boolean newPOJO = false;
        synchronized (this) {
            if (m_pojoObjects != null) {
                pojo = m_pojoObjects.get(0); // Stack confinement
            } else {
                pojo = createObject();  // Stack confinement
                if (m_pojoObjects == null) {
                    m_pojoObjects = new ArrayList(1);
                }
                m_pojoObjects.add(pojo);
                newPOJO = true;
            }
        }

        // Call createInstance on Handlers :
        for (int i = 0; newPOJO && i < m_handlers.length; i++) {
            ((PrimitiveHandler) m_handlers[i].getHandler()).onCreation(pojo);
        }
        //NOTE this method allows returning a POJO object before calling the onCreation on handler:
        // a second thread get the created object before the first one (which created the object),
        // call onCreation.

        return pojo;
    }

    /**
     * Gets the manipulated class.
     * The method does not need to be synchronized.
     * Reassigning the internal class will use the same class object.
     *
     * @return the manipulated class
     */
    public Class getClazz() {
        if (m_clazz == null) {
            load();
        }
        return m_clazz;
    }

    /**
     * Configures an injected object in this container.
     */
    private void managedInjectedObject() {
        Object obj = m_pojoObjects.get(0); // Get first object.

        if (!(obj instanceof Pojo)) {
            // Error, the injected object is not a POJO.
            throw new RuntimeException("The injected object in " + m_name + " is not a POJO");
        }

        load(); // Load the class.

        if (!m_clazz.isInstance(obj)) {
            throw new RuntimeException("The injected object in " + m_name + " is not an instance of " + m_className);
        }

        // Call _setInstanceManager
        try {
            Method setIM = m_clazz.getDeclaredMethod("_setInstanceManager", new Class[]{this.getClass()});
            setIM.setAccessible(true); // Necessary as the method is private
            setIM.invoke(obj, new Object[]{this});
        } catch (Exception e) {
            // If anything wrong happened...
            throw new RuntimeException("Cannot attach the injected object with the container of " + m_name, e);
        }

        // Call createInstance on Handlers :
        for (int i = 0; i < m_handlers.length; i++) {
            // This methods must be call without the monitor lock.
            ((PrimitiveHandler) m_handlers[i].getHandler()).onCreation(obj);
        }


    }

    /**
     * Registers an handler.
     * This methods is called by handler wanting to monitor
     * fields and/or methods of the implementation class.
     *
     * @param handler the handler to register
     * @param fields  the field metadata list
     * @param methods the method metadata list
     * @deprecated use {@link InstanceManager#register(FieldMetadata, FieldInterceptor)}
     * and {@link InstanceManager#register(MethodMetadata, MethodInterceptor)} instead.
     */
    public void register(PrimitiveHandler handler, FieldMetadata[] fields, MethodMetadata[] methods) {
        for (int i = 0; fields != null && i < fields.length; i++) {
            register(fields[i], handler);
        }
        for (int i = 0; methods != null && i < methods.length; i++) {
            register(methods[i], handler);
        }

    }

    /**
     * Registers a field interceptor.
     * A field interceptor will be notified of field access of the
     * implementation class. Note that handlers are field interceptors.
     *
     * @param field       the field to monitor
     * @param interceptor the field interceptor object
     */
    public void register(FieldMetadata field, FieldInterceptor interceptor) {
        if (m_fieldRegistration == null) {
            m_fieldRegistration = new HashMap();
            m_fieldRegistration.put(field.getFieldName(), new FieldInterceptor[]{interceptor});
        } else {
            FieldInterceptor[] list = (FieldInterceptor[]) m_fieldRegistration.get(field.getFieldName());
            if (list == null) {
                m_fieldRegistration.put(field.getFieldName(), new FieldInterceptor[]{interceptor});
            } else {
                for (int j = 0; j < list.length; j++) {
                    if (list[j] == interceptor) {
                        return;
                    }
                }
                FieldInterceptor[] newList = new FieldInterceptor[list.length + 1];
                System.arraycopy(list, 0, newList, 0, list.length);
                newList[list.length] = interceptor;
                m_fieldRegistration.put(field.getFieldName(), newList);
            }
        }
    }

    /**
     * Registers a method interceptor.
     * A method interceptor will be notified of method entries, exits
     * and errors. Note that handlers are method interceptors.
     *
     * @param method      the field to monitor
     * @param interceptor the field interceptor object
     */
    public void register(MethodMetadata method, MethodInterceptor interceptor) {
        if (m_methodRegistration == null) {
            m_methodRegistration = new HashMap();
            m_methodRegistration.put(method.getMethodIdentifier(), new MethodInterceptor[]{interceptor});
        } else {
            MethodInterceptor[] list = (MethodInterceptor[]) m_methodRegistration.get(method.getMethodIdentifier());
            if (list == null) {
                m_methodRegistration.put(method.getMethodIdentifier(), new MethodInterceptor[]{interceptor});
            } else {
                for (int j = 0; j < list.length; j++) {
                    if (list[j] == interceptor) {
                        return;
                    }
                }
                MethodInterceptor[] newList = new MethodInterceptor[list.length + 1];
                System.arraycopy(list, 0, newList, 0, list.length);
                newList[list.length] = interceptor;
                m_methodRegistration.put(method.getMethodIdentifier(), newList);
            }
        }
    }

    /**
     * Registers a method interceptor on a methods from an inner class.
     * A method interceptor will be notified of method entries, exits
     * and errors. Note that handlers are method interceptors.
     *
     * @param method      the field to monitor
     * @param innerClass  the inner class name
     * @param interceptor the field interceptor object
     */
    public void register(MethodMetadata method, String innerClass, MethodInterceptor interceptor) {
        if (m_methodRegistration == null) {
            m_methodRegistration = new HashMap();
            m_methodRegistration.put(innerClass + "___" + method.getMethodIdentifier(),
                    new MethodInterceptor[]{interceptor});
        } else {
            MethodInterceptor[] list = (MethodInterceptor[]) m_methodRegistration.get(method.getMethodIdentifier());
            if (list == null) {
                m_methodRegistration.put(innerClass + "___" + method.getMethodIdentifier(),
                        new MethodInterceptor[]{interceptor});
            } else {
                for (int j = 0; j < list.length; j++) {
                    if (list[j] == interceptor) {
                        return;
                    }
                }
                MethodInterceptor[] newList = new MethodInterceptor[list.length + 1];
                System.arraycopy(list, 0, newList, 0, list.length);
                newList[list.length] = interceptor;
                m_methodRegistration.put(innerClass + "___" + method.getMethodIdentifier(), newList);
            }
        }
    }

    /**
     * Registers a constructor injector.
     * The constructor injector will be called when a pojo object is going to be
     * created.
     *
     * @param index    the index of the parameter. Only one injector per index can
     *                 be registered.
     * @param injector the injector object.
     * @throws ConfigurationException if the given index is already injected by another
     *                                injector
     */
    public void register(int index, ConstructorInjector injector) throws ConfigurationException {
        if (m_constructorRegistration == null) {
            m_constructorRegistration = new HashMap();
        }
        if (!m_constructorRegistration.containsKey(index)) {
            m_constructorRegistration.put(index, injector);
        } else {
            throw new ConfigurationException("Another constructor injector " +
                    "manages the parameter " + index + " : " + m_constructorRegistration.get(index) + ", so cannot " +
                    "register " + injector);
        }
    }

    /**
     * This method is called by the manipulated class each time that a GETFIELD instruction is executed.
     * The method asks to each attached handler monitoring this field which value need
     * to be injected (i.e. returned) by invoking the {@link PrimitiveHandler#onGet(Object, String, Object)}
     * method. If the field value changes, this method call the {@link PrimitiveHandler#onSet(Object, String, Object)}
     * method on each field interceptor monitoring the field in order to advertize the new value.
     *
     * @param pojo      the pojo object on which the field was get
     * @param fieldName the field name on which the GETFIELD instruction is called
     * @return the value decided by the last asked handler (throws a warning if two fields decide two different values)
     */
    public Object onGet(Object pojo, String fieldName) {
        Object initialValue = null;
        synchronized (this) { // Stack confinement.
            initialValue = m_fields.get(fieldName);
        }
        Object result = initialValue;
        boolean hasChanged = false;
        // Get the list of registered handlers
        FieldInterceptor[] list = (FieldInterceptor[]) m_fieldRegistration.get(fieldName); // Immutable list.
        for (int i = 0; list != null && i < list.length; i++) {
            // Call onGet outside of a synchronized block.
            Object handlerResult = list[i].onGet(pojo, fieldName, initialValue);
            if (handlerResult == initialValue) {
                continue; // Non-binding case (default implementation).
            } else {
                if (result != initialValue) {
                    //TODO analyze impact of removing conflict detection
                    if ((handlerResult != null && !handlerResult.equals(result)) || (result != null && handlerResult == null)) {
                        m_logger.log(
                                Logger.WARNING,
                                "A conflict was detected on the injection of "
                                        + fieldName
                        );
                    }
                }
                result = handlerResult;
                hasChanged = true;
            }
        }
        if (hasChanged) {
            // A change occurs => notify the change
            //TODO consider just changing the reference, however multiple thread can be an issue
            synchronized (this) {
                m_fields.put(fieldName, result);
            }
            // Call onset outside of a synchronized block.
            for (int i = 0; list != null && i < list.length; i++) {
                list[i].onSet(pojo, fieldName, result);
            }
        }
        return result;
    }

    /**
     * Dispatches entry method events on registered method interceptors.
     * This method calls the {@link MethodInterceptor#onEntry(Object, java.lang.reflect.Member, Object[])}
     * methods on method interceptors monitoring the method.
     *
     * @param pojo     the pojo object on which method is invoked.
     * @param methodId the method id used to compute the {@link Method} object.
     * @param args     the argument array
     */
    public void onEntry(Object pojo, String methodId, Object[] args) {
        if (m_methodRegistration == null) { // Immutable field.
            return;
        }

        MethodInterceptor[] list = (MethodInterceptor[]) m_methodRegistration.get(methodId);
        Member method = getMethodById(methodId);
        // We can't find the member object of anonymous methods.

        // In case of a constructor, the method is null, and the list is null too.
        for (int i = 0; list != null && i < list.length; i++) {
            list[i].onEntry(pojo, method, args); // Outside a synchronized block.
        }
    }

    /**
     * Dispatches exit method events on registered method interceptors.
     * The given returned object is an instance of {@link Exception} if the method thrown an
     * exception. If the given object is <code>null</code>, either the method returns <code>void</code>,
     * or the method has returned <code>null</code>
     * This method calls the {@link MethodInterceptor#onExit(Object, java.lang.reflect.Member, Object)} and the
     * {@link MethodInterceptor#onFinally(Object, java.lang.reflect.Member)} methods on method interceptors
     * monitoring the method.
     *
     * @param pojo     the pojo object on which method was invoked.
     * @param methodId the method id used to compute the {@link Method} object.
     * @param result   the returned object.
     */
    public void onExit(Object pojo, String methodId, Object result) {
        if (m_methodRegistration == null) {
            return;
        }
        MethodInterceptor[] list = (MethodInterceptor[]) m_methodRegistration.get(methodId);
        Member method = getMethodById(methodId);
        for (int i = 0; list != null && i < list.length; i++) {
            list[i].onExit(pojo, method, result);
        }
        for (int i = 0; list != null && i < list.length; i++) {
            list[i].onFinally(pojo, method);
        }
    }

    /**
     * Dispatches error method events on registered method interceptors.
     * or the method has returned <code>null</code>
     * This method calls the {@link MethodInterceptor#onError(Object, java.lang.reflect.Member, Throwable)} and the
     * {@link MethodInterceptor#onFinally(Object, java.lang.reflect.Member)} methods on method interceptors monitoring
     * the method.
     *
     * @param pojo     the pojo object on which the method was invoked
     * @param methodId the method id used to compute the {@link Method} object.
     * @param error    the Throwable object.
     */
    public void onError(Object pojo, String methodId, Throwable error) {
        if (m_methodRegistration == null) {
            return;
        }
        MethodInterceptor[] list = (MethodInterceptor[]) m_methodRegistration.get(methodId);
        Member method = getMethodById(methodId);
        for (int i = 0; list != null && i < list.length; i++) {
            list[i].onError(pojo, method, error);
        }
        for (int i = 0; list != null && i < list.length; i++) {
            list[i].onFinally(pojo, method);
        }
    }

    /**
     * Computes the {@link Method} object from the given id.
     * Once computes, a map is used as a cache to avoid to recompute for
     * the same id.
     *
     * @param methodId the method id
     * @return the method object or <code>null</code> if the method cannot be found.
     */
    private Member getMethodById(String methodId) {
        // Used a synchronized map.
        Member member = (Member) m_methods.get(methodId);
        if (!m_methods.containsKey(methodId) && m_clazz != null) {
            // Is it a inner class method
            if (methodId.contains("___")) { // Mark to detect a inner class method.
                String[] split = methodId.split("___");
                if (split.length != 2) {
                    m_logger.log(Logger.INFO, "A methodID cannot be associated with a method from the POJO class: " + methodId);
                    return null;
                } else {
                    String innerClassName = split[0];
                    methodId = split[1];

                    // We can't find the member objects from anonymous methods, identified by their numeric name
                    // Just escaping in this case.
                    if (innerClassName.matches("-?\\d+")) {
                        m_methods.put(methodId, null);
                        return null;
                    }

                    for (Class c : m_clazz.getDeclaredClasses()) {
                        if (innerClassName.equals(c.getSimpleName())) {
                            Method[] mets = c.getDeclaredMethods();
                            for (Method met : mets) {
                                if (MethodMetadata.computeMethodId(met).equals(methodId)) {
                                    // Store the new methodId
                                    m_methods.put(methodId, met);
                                    return met;
                                }
                            }
                        }
                        m_logger.log(Logger.INFO, "Cannot find the member associated to " + methodId + " - reason: " +
                                "cannot find the class " + innerClassName + " declared in " + m_clazz.getName());
                    }
                }
            }


            // First try on methods.
            Method[] mets = m_clazz.getDeclaredMethods();
            for (int i = 0; i < mets.length; i++) {
                if (MethodMetadata.computeMethodId(mets[i]).equals(methodId)) {
                    // Store the new methodId
                    m_methods.put(methodId, mets[i]);
                    return mets[i];
                }
            }

            // If not found, it is a constructor, return the constructor object in this case.
            if (methodId.startsWith(MethodMetadata.CONSTRUCTOR_PREFIX)) {
                Constructor[] constructors = m_clazz.getDeclaredConstructors();
                for (int i = 0; i < constructors.length; i++) {
                    // Check if the constructor was not already computed. If not, compute the Id and check.
                    if (MethodMetadata.computeMethodId(constructors[i]).equals(methodId)) {
                        // Store the new methodId
                        m_methods.put(methodId, constructors[i]);
                        return constructors[i];
                    }
                }
            }

            // Should not happen
            m_logger.log(Logger.INFO, "A methodID cannot be associated with a method from the POJO class: " + methodId);
            return null;
        } else {
            return member;
        }
    }

    /**
     * This method is called by the manipulated class each time that a PUTFIELD instruction is executed.
     * The method calls the {@link PrimitiveHandler#onSet(Object, String, Object)} method on each field
     * interceptors monitoring this field.
     * This method can be invoked with a <code>null</code> pojo argument when the changes comes from another
     * handler.
     *
     * @param pojo        the pojo object on which the field was set
     * @param fieldName   the field name on which the PUTFIELD instruction is called
     * @param objectValue the new value of the field
     */
    public void onSet(final Object pojo, final String fieldName, final Object objectValue) {
        synchronized (this) {
            // First, store the new value.
            // This must be done in a synchronized block to avoid
            // concurrent modification
            m_fields.put(fieldName, objectValue);
        }
        // The registrations cannot be modified, so we can directly access
        // the interceptor list.
        FieldInterceptor[] list = (FieldInterceptor[]) m_fieldRegistration
                .get(fieldName);
        for (int i = 0; list != null && i < list.length; i++) {
            // The callback must be call outside the synchronization block.
            list[i].onSet(pojo, fieldName, objectValue);
        }
    }


    /**
     * Gets the bundle context used by this component instance.
     *
     * @return the context of the component.
     * @see org.apache.felix.ipojo.ComponentInstance#getContext()
     */
    public BundleContext getContext() {
        return m_context; // Immutable
    }

    /**
     * Gets the global bundle context. This is the bundle context
     * of the bundle declaring the component type.
     *
     * @return the bundle context of the bundle declaring the component
     * type.
     */
    public BundleContext getGlobalContext() {
        return ((IPojoContext) m_context).getGlobalContext(); // Immutable
    }


    /**
     * Gets the local service context. This service context gives
     * access to the 'local' service registry (the composite one).
     * If the instance lives in the global (i.e. OSGi) context,
     * this method returns <code>null</code>
     *
     * @return the local service context or <code>null</code> if the
     * instance doesn't live in a composite.
     */
    public ServiceContext getLocalServiceContext() {
        return ((IPojoContext) m_context).getServiceContext(); // Immutable
    }

    /**
     * Gets the instance name.
     *
     * @return the instance name.
     * @see org.apache.felix.ipojo.ComponentInstance#getInstanceName()
     */
    public String getInstanceName() {
        return m_name; // Immutable
    }

    /**
     * Reconfigures the current instance.
     * Reconfiguring an instance means re-injecting a new
     * instance configuration. Some properties are immutable
     * such as the instance name.
     * This methods calls the {@link PrimitiveHandler#reconfigure(Dictionary)}
     * methods on each attached handler, and then recompute the instance
     * state. Note that the reconfiguration process does not deactivate the
     * instance.
     *
     * @param configuration the new configuration to push
     * @see org.apache.felix.ipojo.ComponentInstance#reconfigure(java.util.Dictionary)
     */
    public void reconfigure(Dictionary configuration) {
        m_logger.log(Logger.INFO, "Reconfiguring " + getInstanceName());
        for (int i = 0; i < m_handlers.length; i++) {
            m_handlers[i].getHandler().reconfigure(configuration);
        }
        // We synchronized the state computation.
        synchronized (this) {
            if (m_state == STOPPED) {
                m_logger.log(Logger.INFO, "Instance stopped during reconfiguration - Try to restart");
                start();
            } else if (m_state == INVALID) {
                m_logger.log(Logger.INFO, "Instance invalid during reconfiguration - Recompute state");
                // Try to revalidate the instance after reconfiguration
                for (int i = 0; i < m_handlers.length; i++) {
                    if (m_handlers[i].getState() != VALID) {
                        return;
                    }
                }
                setState(VALID);
            }
        }
    }

    /**
     * Gets the implementation class of the component type.
     * This method does not need to be synchronized as the
     * class name is constant once set.
     *
     * @return the class name of the component implementation.
     */
    public String getClassName() {
        return m_className;
    }

    /**
     * State Change listener callback.
     * This method is called every time that a plugged handler becomes valid or invalid.
     * This method computes the new instance state and applies it (by calling the
     * {@link InstanceManager#setState(int)} method.
     *
     * @param instance the handler becoming valid or invalid
     * @param newState the new state of the handler
     * @see org.apache.felix.ipojo.InstanceStateListener#stateChanged(org.apache.felix.ipojo.ComponentInstance, int)
     */
    public void stateChanged(ComponentInstance instance, int newState) {
        int state;
        synchronized (this) {
            if (m_state <= STOPPED) {
                return;
            } else {
                state = m_state; // Stack confinement
            }
        }

        // Update the component state if necessary
        if (newState == INVALID && state == VALID) {
            // Need to update the state to UNRESOLVED
            setState(INVALID);
            return;
        }
        if (newState == VALID && state == INVALID) {
            // An handler becomes valid => check if all handlers are valid
            for (int i = 0; i < m_handlers.length; i++) {
                if (m_handlers[i].getState() != VALID) {
                    return;
                }
            }
            setState(VALID);
            return;
        }
    }

    /**
     * Gets the list of registered fields (containing field names).
     * This method is invoked by the POJO itself during
     * its initialization.
     *
     * @return the set of registered fields.
     */
    public Set getRegistredFields() {
        // IMPORTANT - method used by the manipulator
        if (m_fieldRegistration == null) {
            return null;
        }
        return m_fieldRegistration.keySet();
    }

    /**
     * Gets the list of registered methods (containing method ids).
     * This method is invoked by the POJO itself during its
     * initialization.
     *
     * @return the set of registered methods.
     */
    public Set getRegistredMethods() {
        // IMPORTANT - method used by the manipulator
        if (m_methodRegistration == null) {
            return null;
        } else {
            return m_methodRegistration.keySet();
        }
    }

    /**
     * Gets the bundle context of the instance, i.e. the bundle context of the bundle having declared this instance.
     *
     * @return the bundle context of the instance.
     * @since 1.11.2
     */
    public BundleContext getInstanceContext() {
        return m_instanceContext;
    }
}
