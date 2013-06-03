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

import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.extender.internal.Extender;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Logger;
import org.apache.felix.ipojo.util.SecurityHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedServiceFactory;

import java.util.*;

/**
 * This class defines common mechanisms of iPOJO component factories
 * (i.e. component type).
 *
 * The factory is also tracking Factory configuration from the configuration admin to created / delete and update
 * instances from this factory.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class IPojoFactory implements Factory {
    /*
     * TODO there is potentially an issue when calling FactoryStateListener callbacks with the lock
     * It should be called by a separate thread dispatching events to listeners.
     */

    /**
     * The list of the managed instance name.
     * This list is shared by all factories and is used to assert name uniqueness.
     */
    protected static final List<String> INSTANCE_NAME = Collections.synchronizedList(new ArrayList<String>());

    /**
     * The component type description exposed by the {@link Factory} service.
     */
    protected ComponentTypeDescription m_componentDesc;

    /**
     * The list of the managed instance managers.
     * The key of this map is the name (i.e. instance names) of the created instance
     */
    protected final Map<String, ComponentInstance> m_componentInstances = new HashMap<String, ComponentInstance>();

    /**
     * The component type metadata.
     */
    protected final Element m_componentMetadata;

    /**
     * The bundle context reference.
     */
    protected final BundleContext m_context;

    /**
     * The factory name.
     * Could be the component class name if the factory name is not set.
     * Immutable once set.
     */
    protected String m_factoryName;

    /**
     * The list of required handlers.
     */
    protected final List<RequiredHandler> m_requiredHandlers = new ArrayList<RequiredHandler>();

    /**
     * The list of factory state listeners.
     * @see FactoryStateListener
     */
    protected List<FactoryStateListener> m_listeners = new ArrayList<FactoryStateListener>(1);

    /**
     * The logger for the factory.
     */
    protected final Logger m_logger;

    /**
     * Is the factory public (exposed as services).
     */
    protected final boolean m_isPublic;

    /**
     * The version of the component type.
     */
    protected final String m_version;

    /**
     * The service registration of this factory (Factory & ManagedServiceFactory).
     * @see ManagedServiceFactory
     * @see Factory
     */
    protected ServiceRegistration m_sr;

    /**
     * The factory state.
     * Can be:
     * <li>{@link Factory#INVALID}</li>
     * <li>{@link Factory#VALID}</li>
     * The factory is invalid at the beginning.
     * A factory becomes valid if every required handlers
     * are available (i.e. can be created).
     */
    protected int m_state = Factory.INVALID;

    /**
     * The flag indicating if this factory has already a
     * computed description or not.
     */
    private boolean m_described;

    /**
     * Generates a unique instance name if not provided by the configuration.
     */
    private NameGenerator m_generator = new UniquenessNameGenerator(new SwitchNameGenerator());

    /**
     * Creates an iPOJO Factory.
     * At the end of this method, the required set of handler is computed.
     * But the result is computed by a sub-class.
     * @param context the bundle context of the bundle containing the factory.
     * @param metadata the description of the component type.
     * @throws ConfigurationException if the element describing the factory is malformed.
     */
    public IPojoFactory(BundleContext context, Element metadata) throws ConfigurationException {
        m_context = context;
        m_componentMetadata = metadata;
        m_factoryName = getFactoryName();
        String fac = metadata.getAttribute("public");
        m_isPublic = fac == null || !fac.equalsIgnoreCase("false");
        m_logger = new Logger(m_context, m_factoryName);

        // Compute the component type version.
        String version = metadata.getAttribute("version");
        if ("bundle".equalsIgnoreCase(version)) { // Handle the "bundle" constant: use the bundle version.
            // The cast is necessary in KF.
            m_version = (String) m_context.getBundle().getHeaders().get(Constants.BUNDLE_VERSION);
        } else {
            m_version = version;
        }

        m_requiredHandlers.addAll(getRequiredHandlerList()); // Call sub-class to get the list of required handlers.

        m_logger.log(Logger.INFO, "New factory created : " + m_factoryName);
    }

    /**
     * Gets the component type description.
     * @return the component type description
     */
    public ComponentTypeDescription getComponentTypeDescription() {
        return new ComponentTypeDescription(this);
    }

    /**
     * Adds a factory listener.
     * @param listener the factory listener to add.
     * @see org.apache.felix.ipojo.Factory#addFactoryStateListener(org.apache.felix.ipojo.FactoryStateListener)
     */
    public void addFactoryStateListener(FactoryStateListener listener) {
        synchronized (this) {
            m_listeners.add(listener);
        }
    }

    /**
     * Gets the logger used by instances created by the current factory.
     * @return the factory logger.
     */
    public Logger getLogger() {
        return m_logger;
    }

    /**
     * Computes the factory name.
     * Each sub-type must override this method.
     * @return the factory name.
     */
    public abstract String getFactoryName();

    /**
     * Computes the required handler list.
     * Each sub-type must override this method.
     * @return the required handler list
     */
    public abstract List<RequiredHandler> getRequiredHandlerList();

    /**
     * Creates an instance.
     * This method is called with the monitor lock.
     * @param config the instance configuration
     * @param context the iPOJO context to use
     * @param handlers the handler array to use
     * @return the new component instance.
     * @throws ConfigurationException if the instance creation failed during the configuration process.
     */
    public abstract ComponentInstance createInstance(Dictionary config, IPojoContext context, HandlerManager[] handlers)
        throws ConfigurationException;

    /**
     * Creates an instance.
     * This method creates the instance in the global context.
     * @param configuration the configuration of the created instance.
     * @return the created component instance.
     * @throws UnacceptableConfiguration if the given configuration is not consistent with the component type of this factory.
     * @throws MissingHandlerException if an handler is unavailable when the instance is created.
     * @throws org.apache.felix.ipojo.ConfigurationException if the instance or type configuration are not correct.
     * @see org.apache.felix.ipojo.Factory#createComponentInstance(java.util.Dictionary)
     */
    public ComponentInstance createComponentInstance(Dictionary configuration) throws UnacceptableConfiguration, MissingHandlerException,
            ConfigurationException {
        return createComponentInstance(configuration, null);
    }

    /**
     * Creates an instance in the specified service context.
     * This method is synchronized to assert the validity of the factory during the creation.
     * Callbacks to sub-class and  created instances need to be aware that they are holding the monitor lock.
     * This method call the override {@link IPojoFactory#createInstance(Dictionary, IPojoContext, HandlerManager[])
     * method.
     * @param configuration the configuration of the created instance.
     * @param serviceContext the service context to push for this instance.
     * @return the created component instance.
     * @throws UnacceptableConfiguration if the given configuration is not consistent with the component type of this factory.
     * @throws MissingHandlerException if an handler is unavailable when creating the instance.
     * @throws org.apache.felix.ipojo.ConfigurationException if the instance configuration failed.
     * @see org.apache.felix.ipojo.Factory#createComponentInstance(java.util.Dictionary)
     */
    public synchronized ComponentInstance createComponentInstance(Dictionary configuration, ServiceContext serviceContext) throws UnacceptableConfiguration, // NOPMD
            MissingHandlerException, ConfigurationException {
        if (configuration == null) {
            configuration = new Properties();
        }

        IPojoContext context;
        if (serviceContext == null) {
            context = new IPojoContext(m_context);
        } else {
            context = new IPojoContext(m_context, serviceContext);
        }

        try {
            checkAcceptability(configuration);
        } catch (UnacceptableConfiguration e) {
            m_logger.log(Logger.ERROR, "The configuration is not acceptable : " + e.getMessage());
            throw new UnacceptableConfiguration("The configuration "
                    + configuration + " is not acceptable for " + m_factoryName
                    , e);
        }

        // Find name in the configuration
        String name;
        if (configuration.get(Factory.INSTANCE_NAME_PROPERTY) == null && configuration.get("name") == null) {
            // No name provided
            name = null;
        } else {
            // Support both instance.name & name
            name = (String) configuration.get(Factory.INSTANCE_NAME_PROPERTY);
            if (name == null) {
                name = (String) configuration.get("name");
                getLogger().log(Logger.WARNING, "The 'name' (" + name + ") attribute, used as the instance name, is deprecated, please use the 'instance.name' attribute");
            }
        }


        // Generate a unique name if required and verify uniqueness
        // We extract the version from the configuration because it may help to compute a unique name by appending
        // the version to the given name.
        String version = (String) configuration.get(Factory.FACTORY_VERSION_PROPERTY);

        // If the extracted version is null, we use the current factory version (as we were called)
        if (version == null) {
            version = m_version;
        }
        name = m_generator.generate(name, version);
        configuration.put(Factory.INSTANCE_NAME_PROPERTY, name);

        // Here we are sure to be valid until the end of the method.
        HandlerManager[] handlers = new HandlerManager[m_requiredHandlers.size()];
        for (int i = 0; i < handlers.length; i++) {
            handlers[i] = getHandler(m_requiredHandlers.get(i), serviceContext);
        }

        try {
            ComponentInstance instance = createInstance(configuration, context, handlers);
            m_componentInstances.put(name, instance);
            m_logger.log(Logger.INFO, "Instance " + name + " from factory " + m_factoryName + " created");
            // Register the instance on the ConfigurationTracker to be updated if needed.
            ConfigurationTracker.get().instanceCreated(instance);
            return instance;
        } catch (ConfigurationException e) {
            INSTANCE_NAME.remove(name);
            m_logger.log(Logger.ERROR, e.getMessage());
            throw new ConfigurationException(e.getMessage(), e, m_factoryName);
        }


    }

    /**
     * Gets the bundle context of the factory.
     * @return the bundle context of the factory.
     * @see org.apache.felix.ipojo.Factory#getBundleContext()
     */
    public BundleContext getBundleContext() {
        return m_context;
    }

    /**
     * Gets the factory class name.
     * @return the factory class name.
     * @see org.apache.felix.ipojo.Factory#getClassName()
     */
    public abstract String getClassName();

    /**
     * Gets the component type description.
     * @return the component type description object. <code>Null</code> if not already computed.
     */
    public synchronized ComponentTypeDescription getComponentDescription() {
        return m_componentDesc;
    }

    /**
     * Gets the component type description (Element-Attribute form).
     * @return the component type description.
     * @see org.apache.felix.ipojo.Factory#getDescription()
     */
    public synchronized Element getDescription() {
        // Can be null, if not already computed.
        if (m_componentDesc == null) {
            return new Element("No description available for " + m_factoryName, "");
        }
        return m_componentDesc.getDescription();
    }

    /**
     * Gets the component metadata.
     * @return the component metadata
     * @see org.apache.felix.ipojo.Factory#getComponentMetadata()
     */
    public Element getComponentMetadata() {
        return m_componentMetadata;
    }

    /**
     * Computes the list of missing handlers.
     * @return the list of missing handlers.
     * @see org.apache.felix.ipojo.Factory#getMissingHandlers()
     */
    public List<String> getMissingHandlers() {
        List<String> list = new ArrayList<String>();
        for (RequiredHandler req : m_requiredHandlers) {
            if (req.getReference() == null) {
                list.add(req.getFullName());
            }
        }
        return list;
    }

    /**
     * Gets the factory name.
     * This name is immutable once set.
     * @return the factory name.
     * @see org.apache.felix.ipojo.Factory#getName()
     */
    public String getName() {
        return m_factoryName;
    }

    /**
     * Gets the list of required handlers.
     * The required handler list cannot change.
     * @return the list of required handlers.
     * @see org.apache.felix.ipojo.Factory#getRequiredHandlers()
     */
    public List<String> getRequiredHandlers() {
        List<String> list = new ArrayList<String>();
        for (RequiredHandler req : m_requiredHandlers) {
            list.add(req.getFullName());
        }
        return list;
    }

    /**
     * Gets the actual factory state.
     * Must be synchronized as this state is dependent of handler availability.
     * @return the actual factory state.
     * @see org.apache.felix.ipojo.Factory#getState()
     */
    public synchronized int getState() {
        return m_state;
    }

    /**
     * Gets a component instance created by the current factory.
     * @param name the instance name
     * @return the component instance, {@literal null} if not found
     */
    public ComponentInstance getInstanceByName(String name) {
        synchronized (this) {
            return m_componentInstances.get(name);
        }
    }

    /**
     * Checks if the configuration is acceptable.
     * @param conf the configuration to test.
     * @return <code>true</code> if the configuration is acceptable.
     * @see org.apache.felix.ipojo.Factory#isAcceptable(java.util.Dictionary)
     */
    public boolean isAcceptable(Dictionary conf) {
        try {
            checkAcceptability(conf);
        } catch (MissingHandlerException e) {
            return false;
        } catch (UnacceptableConfiguration e) {
            return false;
        }
        return true;
    }

    /**
     * Checks if the configuration is acceptable.
     * This method checks the following assertions:
     * <li>All handlers can be creates</li>
     * <li>The configuration does not override immutable properties</li>
     * <li>The configuration contains a value for every unvalued property</li>
     * @param conf the configuration to test.
     * @throws UnacceptableConfiguration if the configuration is unacceptable.
     * @throws MissingHandlerException if an handler is missing.
     */
    public void checkAcceptability(Dictionary<String, ?> conf) throws UnacceptableConfiguration,
            MissingHandlerException {
        PropertyDescription[] props;
        synchronized (this) {
            if (m_state == Factory.INVALID) {
                throw new MissingHandlerException(getMissingHandlers());
            }
            props = m_componentDesc.getProperties(); // Stack confinement.
            // The property list is up to date, as the factory is valid.
        }

        // Check that the configuration does not override immutable properties.

        for (PropertyDescription prop : props) {
            // Is the property immutable
            if (prop.isImmutable() && conf.get(prop.getName()) != null) {
                throw new UnacceptableConfiguration("The property " + prop + " cannot be overridden : immutable " +
                        "property"); // The instance configuration tries to override an immutable property.
            }
            // Is the property required ?
            if (prop.isMandatory() && prop.getValue() == null && conf.get(prop.getName()) == null) {
                throw new UnacceptableConfiguration("The mandatory property " + prop.getName() + " is missing"); // The property must be set.
            }
        }
    }

    /**
     * Reconfigures an existing instance.
     * The acceptability of the configuration is checked before the reconfiguration. Moreover,
     * the configuration must contain the 'instance.name' property specifying the instance
     * to reconfigure.
     * This method is synchronized to assert the validity of the factory during the reconfiguration.
     * @param properties the new configuration to push.
     * @throws UnacceptableConfiguration if the new configuration is not consistent with the component type.
     * @throws MissingHandlerException if the current factory is not valid.
     * @see org.apache.felix.ipojo.Factory#reconfigure(java.util.Dictionary)
     */
    public synchronized void reconfigure(Dictionary properties) throws UnacceptableConfiguration, MissingHandlerException {
        if (properties == null || (properties.get(Factory.INSTANCE_NAME_PROPERTY) == null && properties.get("name") == null)) { // Support both instance.name and name
            throw new UnacceptableConfiguration("The configuration does not contains the \"instance.name\" property");
        }

        String name = (String) properties.get(Factory.INSTANCE_NAME_PROPERTY);
        if (name == null) {
            name = (String) properties.get("name");
        }

        ComponentInstance instance = m_componentInstances.get(name);
        if (instance == null) { // The instance does not exists.
            return;
        }

        checkAcceptability(properties); // Test if the configuration is acceptable
        instance.reconfigure(properties); // re-configure the instance
    }

    /**
     * Removes a factory listener.
     * @param listener the factory listener to remove.
     * @see org.apache.felix.ipojo.Factory#removeFactoryStateListener(org.apache.felix.ipojo.FactoryStateListener)
     */
    public void removeFactoryStateListener(FactoryStateListener listener) {
        synchronized (this) {
            m_listeners.remove(listener);
        }
    }

    /**
     * Stopping method.
     * This method is call when the factory is stopping.
     * This method is called when holding the lock on the factory.
     */
    public abstract void stopping();

    /**
     * Stops all the instance managers.
     * This method calls the {@link IPojoFactory#stopping()} method,
     * notifies listeners, and disposes created instances. Moreover,
     * if the factory is public, services are also unregistered.
     *
     */
    public synchronized void stop() {
        ComponentInstance[] instances;
        if (m_sr != null) {
            m_sr.unregister();
            m_sr = null;
        }

        ConfigurationTracker.get().unregisterFactory(this);

        stopping(); // Method called when holding the lock.
        int oldState = m_state; // Create a variable to store the old state. Using a variable is important as
                                // after the next instruction, the getState() method must return INVALID.
        m_state = INVALID; // Set here to avoid to create instances during the stops.

        Set<String> col = m_componentInstances.keySet();
        Iterator<String> it = col.iterator();
        instances = new ComponentInstance[col.size()]; // Stack confinement
        int index = 0;
        while (it.hasNext()) {
            instances[index] = m_componentInstances.get(it.next());
            index++;
        }

        if (oldState == VALID) { // Check if the old state was valid.
            for (FactoryStateListener listener : m_listeners) {
                listener.stateChanged(this, INVALID);
            }
        }

        // Dispose created instances.
        for (ComponentInstance instance : instances) {
            if (instance.getState() != ComponentInstance.DISPOSED) {
                instance.dispose();
            }
        }

        // Release each handler
        for (RequiredHandler req : m_requiredHandlers) {
            req.unRef();
        }

        m_described = false;
        m_componentDesc = null;
        m_componentInstances.clear();

        m_logger.log(Logger.INFO, "Factory " + m_factoryName + " stopped");

    }

    /**
     * Destroys the factory.
     * The factory cannot be restarted. Only the {@link Extender} can call this method.
     */
    synchronized void dispose() {
        stop(); // Does not hold the lock.
        m_requiredHandlers.clear();
        m_listeners = null;
    }

    /**
     * Starting method.
     * This method is called when the factory is starting.
     * This method is <strong>not</strong> called when holding the lock on the factory.
     */
    public abstract void starting();

    /**
     * Starts the factory.
     * Tries to compute the component type description,
     * calls the {@link IPojoFactory#starting()} method,
     * and published services if the factory is public.
     */
    public void start() {
        synchronized (this) {
            if (m_described) { // Already started.
                return;
            }
        }

        m_componentDesc = getComponentTypeDescription();

        starting();

        synchronized (this) {
            computeFactoryState();
        }

        if (m_isPublic) {
            // Exposition of the factory service
            if (m_componentDesc == null) {
                m_logger.log(Logger.ERROR, "Unexpected state, the description of " + m_factoryName + "  is null");
                return;
            }
            BundleContext bc = SecurityHelper.selectContextToRegisterServices(m_componentDesc.getFactoryInterfacesToPublish(),
                    m_context, getIPOJOBundleContext());
            m_sr =
                    bc.registerService(m_componentDesc.getFactoryInterfacesToPublish(), this, m_componentDesc
                            .getPropertiesToPublish());
        }

        m_logger.log(Logger.INFO, "Factory " + m_factoryName + " started");

    }

    /**
     * For testing purpose <b>ONLY</b>.
     * This method recomputes the required handler list.
     */
    public void restart() {
    	// Call sub-class to get the list of required handlers.
        m_requiredHandlers.clear();
        m_requiredHandlers.addAll(getRequiredHandlerList());
    }

    /**
     * Gets the iPOJO Bundle Context.
     * @return the iPOJO Bundle Context
     */
    protected final BundleContext getIPOJOBundleContext() {
        return Extender.getIPOJOBundleContext();
    }

    /**
     * Creates or updates an instance.
     * This method is used from the configuration tracker.
     * @param name the name of the instance
     * @param properties the new configuration of the instance
     */
    public void updated(String name, Dictionary properties) throws org.osgi.service.cm
            .ConfigurationException {
        ComponentInstance instance;
        synchronized (this) {
            instance = m_componentInstances.get(name);
        }

        if (instance == null) {
            try {
                properties.put(Factory.INSTANCE_NAME_PROPERTY, name); // Add the name in the configuration
                // If an instance with this name was created before, this creation will failed.
                createComponentInstance(properties);
            } catch (UnacceptableConfiguration e) {
                m_logger.log(Logger.ERROR, "The configuration is not acceptable : " + e.getMessage());
                throw new org.osgi.service.cm.ConfigurationException(properties.toString(), e.getMessage(), e);
            } catch (MissingHandlerException e) {
                m_logger.log(Logger.ERROR, "Handler not available : " + e.getMessage());
                throw new org.osgi.service.cm.ConfigurationException(properties.toString(), e.getMessage(), e);
            } catch (ConfigurationException e) {
                m_logger.log(Logger.ERROR, "The Component Type metadata are not correct : " + e.getMessage());
                throw new org.osgi.service.cm.ConfigurationException(properties.toString(), e.getMessage(), e);
            }
        } else {
            try {
                properties.put(Factory.INSTANCE_NAME_PROPERTY, name); // Add the name in the configuration
                reconfigure(properties); // re-configure the component
            } catch (UnacceptableConfiguration e) {
                m_logger.log(Logger.ERROR, "The configuration is not acceptable : " + e.getMessage());
                throw new org.osgi.service.cm.ConfigurationException(properties.toString(), e.getMessage(), e);
            } catch (MissingHandlerException e) {
                m_logger.log(Logger.ERROR, "The factory is not valid, at least one handler is missing : " + e.getMessage());
                throw new org.osgi.service.cm.ConfigurationException(properties.toString(), e.getMessage(), e);
            }
        }
    }

    /**
     * Deletes an instance.
     * @param name the name of the instance to delete
     */
    public synchronized void deleted(String name) {
        INSTANCE_NAME.remove(name);
        ComponentInstance instance = m_componentInstances.remove(name);
        if (instance != null) {
            instance.dispose();
        }
    }

    /**
     * Callback called by instance when disposed.
     * @param instance the destroyed instance
     */
    public void disposed(ComponentInstance instance) {
        String name = instance.getInstanceName();
        synchronized (this) {
            m_componentInstances.remove(name);
        }
        INSTANCE_NAME.remove(name);
    }

    /**
     * Computes the component type description.
     * To do this, it creates a 'ghost' instance of the handler
     * and calls the {@link Handler#initializeComponentFactory(ComponentTypeDescription, Element)}
     * method. The handler instance is then deleted.
     * The factory must be valid when calling this method.
     * This method is called with the lock.
     */
    protected void computeDescription() {
        for (RequiredHandler req : m_requiredHandlers) {
            if (getHandler(req, null) == null) {
                m_logger.log(Logger.ERROR, "Cannot extract handler object from " + m_factoryName + " " + req
                        .getFullName());
            } else {
                Handler handler = getHandler(req, null).getHandler();
                try {
                    handler.setFactory(this);
                    handler.initializeComponentFactory(m_componentDesc, m_componentMetadata);
                    ((Pojo) handler).getComponentInstance().dispose();
                } catch (ConfigurationException e) {
                    ((Pojo) handler).getComponentInstance().dispose();
                    m_logger.log(Logger.ERROR, e.getMessage());
                    stop();
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    /**
     * Computes factory state.
     * The factory is valid if every required handler are available.
     * If the factory becomes valid for the first time, the component
     * type description is computed.
     * This method is called when holding the lock on the current factory.
     */
    protected void computeFactoryState() {
        boolean isValid = true;
        for (RequiredHandler req : m_requiredHandlers) {
            if (req.getReference() == null) {
                isValid = false;
                break;
            }

        }

        if (isValid) {
            if (m_state == INVALID) {

                if (!m_described) {
                    computeDescription();
                    m_described = true;
                }

                m_state = VALID;
                if (m_sr != null) {
                    m_sr.setProperties(m_componentDesc.getPropertiesToPublish());
                }

                // Register the factory on the ConfigurationTracker
                ConfigurationTracker.get().registerFactory(this);

                for (FactoryStateListener listener : m_listeners) {
                    listener.stateChanged(this, VALID);
                }
            }
        } else {
            if (m_state == VALID) {
                m_state = INVALID;

                // Un-register the factory on the ConfigurationTracker
                ConfigurationTracker.get().unregisterFactory(this);

                // Notify listeners.
                for (FactoryStateListener listener : m_listeners) {
                    listener.stateChanged(this, INVALID);
                }

                // Dispose created instances.
                // We must create a copy to avoid concurrent exceptions
                Set<? extends String> keys = new HashSet<String>(m_componentInstances.keySet());
                for (String key : keys) {
                    ComponentInstance instance = m_componentInstances.get(key);
                    if (instance.getState() != ComponentInstance.DISPOSED) {
                        instance.dispose();
                    }
                    INSTANCE_NAME.remove(instance.getInstanceName());
                }

                m_componentInstances.clear();

                if (m_sr != null) {
                    m_sr.setProperties(m_componentDesc.getPropertiesToPublish());
                }
            }
        }
    }

    /**
     * Checks if the given handler identifier and the service reference match.
     * Does not need to be synchronized as the method does not use any fields.
     * @param req the handler identifier.
     * @param ref the service reference.
     * @return <code>true</code> if the service reference can fulfill the handler requirement
     */
    protected boolean match(RequiredHandler req, ServiceReference<?> ref) {
        String name = (String) ref.getProperty(Handler.HANDLER_NAME_PROPERTY);
        String namespace = (String) ref.getProperty(Handler.HANDLER_NAMESPACE_PROPERTY);
        if (HandlerFactory.IPOJO_NAMESPACE.equals(namespace)) {
            return name.equalsIgnoreCase(req.getName()) && req.getNamespace() == null;
        }
        return name.equalsIgnoreCase(req.getName()) && namespace.equalsIgnoreCase(req.getNamespace());
    }

    /**
     * Returns the handler object for the given required handler.
     * The handler is instantiated in the given service context.
     * This method is called with the lock.
     * @param req the handler to create.
     * @param context the service context in which the handler is created (same as the instance context).
     * @return the handler object.
     */
    protected HandlerManager getHandler(RequiredHandler req, ServiceContext context) {
        try {
            return (HandlerManager) req.getFactory().createComponentInstance(null, context);
        } catch (MissingHandlerException e) {
            m_logger.log(Logger.ERROR, "The creation of the handler " + req.getFullName() + " has failed: " + e.getMessage());
            stop();
            return null;
        } catch (UnacceptableConfiguration e) {
            m_logger.log(Logger.ERROR, "The creation of the handler "
                    + req.getFullName()
                    + " has failed (UnacceptableConfiguration): "
                    + e.getMessage());
            stop();
            return null;
        } catch (org.apache.felix.ipojo.ConfigurationException e) {
            m_logger.log(Logger.ERROR, "The configuration of the handler "
                    + req.getFullName()
                    + " has failed (ConfigurationException): "
                    + e.getMessage());
            stop();
            return null;
        }
    }

    /**
     * Structure storing required handlers.
     * Access to this class must mostly be with the lock on the factory.
     * (except to access final fields)
     */
    protected class RequiredHandler implements Comparable {
        /**
         * The factory to create this handler.
         */
        private HandlerFactory m_factory;

        /**
         * The handler name.
         */
        private final String m_name;

        /**
         * The handler start level.
         */
        private int m_level = Integer.MAX_VALUE;

        /**
         * The handler namespace.
         */
        private final String m_namespace;

        /**
         * The Service Reference of the handler factory.
         */
        private ServiceReference<? extends HandlerFactory> m_reference;

        /**
         * Crates a Required Handler.
         * @param name the handler name.
         * @param namespace the handler namespace.
         */
        public RequiredHandler(String name, String namespace) {
            m_name = name;
            m_namespace = namespace;
        }

        /**
         * Equals method.
         * Two handlers are equals if they have same name and namespace or they share the same service reference.
         * @param object the object to compare to the current object.
         * @return <code>true</code> if the two compared object are equals
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object object) {
            if (object instanceof RequiredHandler) {
                RequiredHandler req = (RequiredHandler) object;
                if (m_namespace == null) {
                    return req.m_name.equalsIgnoreCase(m_name) && req.m_namespace == null;
                } else {
                    return req.m_name.equalsIgnoreCase(m_name) && m_namespace.equalsIgnoreCase(req.m_namespace);
                }
            } else {
                return false;
            }

        }

        /**
         * Hashcode method.
         * This method delegates to the {@link Object#hashCode()}.
         * @return the object hashcode.
         * @see java.lang.Object#hashCode()
         */
        public int hashCode() {
            return super.hashCode();
        }

        /**
         * Gets the factory object used for this handler.
         * The object is get when used for the first time.
         * This method is called with the lock avoiding concurrent modification and on a valid factory.
         * @return the factory object.
         */
        public HandlerFactory getFactory() {
            if (m_reference == null) {
                return null;
            }
            if (m_factory == null) {
                m_factory = m_context.getService(getReference());
            }
            return m_factory;
        }

        /**
         * Gets the handler qualified name (<code>namespace:name</code>).
         * @return the handler full name
         */
        public String getFullName() {
            if (m_namespace == null) {
                return HandlerFactory.IPOJO_NAMESPACE + ":" + m_name;
            } else {
                return m_namespace + ":" + m_name;
            }
        }

        public String getName() {
            return m_name;
        }

        public String getNamespace() {
            return m_namespace;
        }

        public ServiceReference<? extends HandlerFactory> getReference() {
            return m_reference;
        }

        public int getLevel() {
            return m_level;
        }

        /**
         * Releases the reference of the used factory.
         * This method is called with the lock on the current factory.
         */
        public void unRef() {
            if (m_reference != null) {
                m_factory = null;
                m_reference = null;
            }
        }

        /**
         * Sets the service reference. If the new service reference is <code>null</code>, it ungets the used factory (if already get).
         * This method is called with the lock on the current factory.
         * @param ref the new service reference.
         */
        public void setReference(ServiceReference<? extends HandlerFactory> ref) {
            m_reference = ref;
            Integer level = (Integer) m_reference.getProperty(Handler.HANDLER_LEVEL_PROPERTY);
            if (level != null) {
                m_level = level;
            }
        }

        /**
         * Start level Comparison.
         * This method is used to sort the handler array.
         * This method is called with the lock.
         * @param object the object on which compare.
         * @return <code>-1</code>, <code>0</code>, <code>+1</code> according to the comparison of their start levels.
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(Object object) {
            if (object instanceof RequiredHandler) {
                RequiredHandler req = (RequiredHandler) object;
                if (this.m_level == req.m_level) {
                    return 0;
                } else if (this.m_level < req.m_level) {
                    return -1;
                } else {
                    return +1;
                }
            }
            return 0;
        }
    }

    /**
     * Generate a unique name for a component instance.
     */
    private interface NameGenerator {

        /**
         * @return a unique name.
         * @param name The user provided name (may be null)
         * @param version the factory version if set. Instances can specify the version of the factory they are bound
         *                to. This parameter may be used to avoid name conflicts.
         */
        String generate(String name, String version) throws UnacceptableConfiguration;
    }

    /**
     * This generator implements the default naming strategy.
     * The name is composed of the factory name suffixed with a unique number identifier (starting from 0).
     */
    private class DefaultNameGenerator implements NameGenerator {
        private long m_nextId = 0;

        /**
         * This method has to be synchronized to ensure name uniqueness.
         * @param name The user provided name (may be null)
         * @param version ignored.
         */
        public synchronized String generate(String name, String version) throws UnacceptableConfiguration
        {
            // Note: This method is overridden by handlers to get the full name.
            return IPojoFactory.this.getFactoryName() + "-" + m_nextId++;
        }
    }

    /**
     * This generator implements the naming strategy when client provides the instance name value.
     */
    private static class UserProvidedNameGenerator implements NameGenerator {

        /**
         * @param name The user provided name (not null)
         * @param  version ignored.
         */
        public String generate(String name, String version) throws UnacceptableConfiguration
        {
            return name;
        }
    }

    /**
     * This generator ensure that the returned name is globally unique.
     */
    private class UniquenessNameGenerator implements NameGenerator {

        private NameGenerator delegate;

        private UniquenessNameGenerator(NameGenerator delegate)
        {
            this.delegate = delegate;
        }

        /**
         * This method has to be synchronized to ensure name uniqueness.
         * @param name The user provided name (may be null)
         */
        public String generate(String name, String version) throws UnacceptableConfiguration
        {
            // Produce the name
            String name2 = delegate.generate(name, version);

            // Needs to be in a synchronized block because we want to ensure that the name is unique
            synchronized (INSTANCE_NAME) {
                // Verify uniqueness
                if (INSTANCE_NAME.contains(name2)  && version != null) {
                    // Named already used, try to append the version.
                    name2 = name2 + "-" + version;
                    if (INSTANCE_NAME.contains(name2)) {
                        m_logger.log(Logger.ERROR, "The configuration is not acceptable : Name already used");
                        throw new UnacceptableConfiguration(getFactoryName() + " : Name already used : " + name2);
                    }
                } else if (INSTANCE_NAME.contains(name2)) {
                    m_logger.log(Logger.ERROR, "The configuration is not acceptable : Name already used");
                    throw new UnacceptableConfiguration(getFactoryName() + " : Name already used : " + name2);
                }
                // reserve the name
                INSTANCE_NAME.add(name2);
            }
            return name2;
        }
    }

    /**
     * This generator choose the right NameGenerator.
     */
    private class SwitchNameGenerator implements NameGenerator {
        private NameGenerator computed = new DefaultNameGenerator();
        private NameGenerator userProvided = new UserProvidedNameGenerator();

        public String generate(String name, String version) throws UnacceptableConfiguration
        {
            if (name == null) {
                return computed.generate(null, null);
            }
            return userProvided.generate(name, version);
        }
    }


}
