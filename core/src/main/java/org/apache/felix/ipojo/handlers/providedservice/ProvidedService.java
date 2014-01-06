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
package org.apache.felix.ipojo.handlers.providedservice;

import org.apache.felix.ipojo.*;
import org.apache.felix.ipojo.util.Callback;
import org.apache.felix.ipojo.util.Property;
import org.apache.felix.ipojo.util.SecurityHelper;
import org.osgi.framework.*;
import org.osgi.service.cm.ConfigurationAdmin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * Provided Service represent a provided service by the component.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ProvidedService implements ServiceFactory {

    /**
     * Service State : REGISTRED.
     */
    public static final int REGISTERED = 1;

    /**
     * Service State : UNREGISTRED.
     */
    public static final int UNREGISTERED = 0;

    /**
     * Factory Policy : SINGLETON_FACTORY.
     */
    public static final int SINGLETON_STRATEGY = 0;

    /**
     * Factory policy : SERVICE_FACTORY.
     */
    public static final int SERVICE_STRATEGY = 1;

    /**
     * Factory policy : STATIC_FACTORY.
     */
    public static final int STATIC_STRATEGY = 2;

    /**
     * Factory policy : INSTANCE.
     * Creates one service object per instance consuming the service.
     */
    public static final int INSTANCE_STRATEGY = 3;

    /**
     * Factory policy : CUSTOMIZED.
     * Custom creation strategy
     */
    public static final int CUSTOM_STRATEGY = -1;
    public static final String ALL_SPECIFICATIONS_FOR_CONTROLLERS = "ALL";

    /**
     * At this time, it is only the java interface full name.
     */
    private String[] m_serviceSpecifications = new String[0];

    /**
     * The service registration. is null when the service is not registered.
     * m_serviceRegistration : ServiceRegistration
     */
    private ServiceRegistration m_serviceRegistration;

    /**
     * Link to the owner handler. m_handler : Provided Service Handler
     */
    private ProvidedServiceHandler m_handler;

    /**
     * The map of properties.
     */
    private Map<String, Property> m_properties = new TreeMap<String, Property>();

    /**
     * Service providing policy.
     */
    private final int m_policy;

    /**
     * Service Object creation policy.
     */
    private final CreationStrategy m_strategy;

    /**
     * Were the properties updated during the processing.
     */
    private volatile boolean m_wasUpdated;

    /**
     * Service Controller.
     */
    private Map<String, ServiceController> m_controllers = new HashMap<String, ServiceController>();

    /**
     * Post-Registration callback.
     */
    private Callback m_postRegistration;

    /**
     * Post-Unregistration callback.
     */
    private Callback m_postUnregistration;

    /**
     * The published properties.
     */
    private Dictionary m_publishedProperties = new Properties();

    /**
     * The provided service listeners.
     */
    private List<ProvidedServiceListener> m_listeners = new ArrayList<ProvidedServiceListener>();

    /**
     * Creates a provided service object.
     *
     * @param handler               the the provided service handler.
     * @param specification         the specifications provided by this provided service
     * @param factoryPolicy         the service providing policy
     * @param creationStrategyClass the customized service object creation strategy.
     * @param conf                  the instance configuration.
     */
    public ProvidedService(ProvidedServiceHandler handler, String[] specification, int factoryPolicy, Class creationStrategyClass, Dictionary conf) {
        CreationStrategy strategy;
        m_handler = handler;

        m_serviceSpecifications = specification;

        if (creationStrategyClass == null) {
            m_policy = factoryPolicy;
        } else {
            m_policy = CUSTOM_STRATEGY;
        }

        // Add instance name, factory name and factory version is set.
        try {
            addProperty(new Property(Factory.INSTANCE_NAME_PROPERTY, null, null, handler.getInstanceManager().getInstanceName(), String.class.getName(), handler.getInstanceManager(), handler));
            addProperty(new Property("factory.name", null, null, handler.getInstanceManager().getFactory().getFactoryName(), String.class.getName(), handler.getInstanceManager(), handler));

            if (handler.getInstanceManager().getFactory().getVersion() != null) {
                addProperty(new Property(Factory.FACTORY_VERSION_PROPERTY, null, null, handler.getInstanceManager().getFactory().getVersion(), String.class.getName(), handler.getInstanceManager(), handler));
            }

            // Add the service.* if defined
            if (conf.get(Constants.SERVICE_PID) != null) {
                addProperty(new Property(Constants.SERVICE_PID, null, null, (String) conf.get(Constants.SERVICE_PID), String.class.getName(), handler.getInstanceManager(), handler));
            }
            if (conf.get(Constants.SERVICE_RANKING) != null) {
                addProperty(new Property(Constants.SERVICE_RANKING, null, null, (String) conf.get(Constants.SERVICE_RANKING), "int", handler.getInstanceManager(), handler));
            }
            if (conf.get(Constants.SERVICE_VENDOR) != null) {
                addProperty(new Property(Constants.SERVICE_VENDOR, null, null, (String) conf.get(Constants.SERVICE_VENDOR), String.class.getName(), handler.getInstanceManager(), handler));
            }
            if (conf.get(Constants.SERVICE_DESCRIPTION) != null) {
                addProperty(new Property(Constants.SERVICE_DESCRIPTION, null, null, (String) conf.get(Constants.SERVICE_DESCRIPTION), String.class.getName(), handler.getInstanceManager(), handler));
            }

        } catch (ConfigurationException e) {
            m_handler.error("An exception occurs when adding instance.name and factory.name property : " + e.getMessage());
        }

        if (creationStrategyClass != null) {
            try {
                strategy = (CreationStrategy) creationStrategyClass.newInstance();
            } catch (IllegalAccessException e) {
                strategy = null;
                m_handler.error("["
                        + m_handler.getInstanceManager().getInstanceName()
                        + "] The customized service object creation policy "
                        + "(" + creationStrategyClass.getName() + ") is not accessible: "
                        + e.getMessage(), e);
                getInstanceManager().stop();
            } catch (InstantiationException e) {
                strategy = null;
                m_handler.error("["
                        + m_handler.getInstanceManager().getInstanceName()
                        + "] The customized service object creation policy "
                        + "(" + creationStrategyClass.getName() + ") cannot be instantiated: "
                        + e.getMessage(), e);
                getInstanceManager().stop();
            }
        } else {
            switch (factoryPolicy) {
                case SINGLETON_STRATEGY:
                    strategy = new SingletonStrategy();
                    break;
                case SERVICE_STRATEGY:
                case STATIC_STRATEGY:
                    // In this case, we need to try to create a new pojo object,
                    // the factory method will handle the creation.
                    strategy = new FactoryStrategy();
                    break;
                case INSTANCE_STRATEGY:
                    strategy = new PerInstanceStrategy();
                    break;
                // Other policies:
                // Thread : one service object per asking thread
                // Consumer : one service object per consumer
                default:
                    strategy = null;
                    List specs = Arrays.asList(m_serviceSpecifications);
                    m_handler.error("["
                            + m_handler.getInstanceManager().getInstanceName()
                            + "] Unknown creation policy for " + specs + " : "
                            + factoryPolicy);
                    getInstanceManager().stop();
                    break;
            }
        }
        m_strategy = strategy;
    }

    /**
     * Add properties to the provided service.
     *
     * @param props : the properties to attached to the service registration
     */
    protected void setProperties(Property[] props) {
        for (Property prop : props) {
            addProperty(prop);
        }
    }

    /**
     * Add the given property to the property list.
     *
     * @param prop : the element to add
     */
    private synchronized void addProperty(Property prop) {
        m_properties.put(prop.getName(), prop);
    }

    /**
     * Remove a property.
     *
     * @param name : the property to remove
     * @return <code>true</code> if the property was removed,
     * <code>false</code> otherwise.
     */
    private synchronized boolean removeProperty(String name) {
        return m_properties.remove(name) != null;
    }

    /**
     * Get the service reference of the service registration.
     *
     * @return the service reference of the provided service (null if the
     * service is not published).
     */
    public ServiceReference getServiceReference() {
        if (m_serviceRegistration == null) {
            return null;
        } else {
            return m_serviceRegistration.getReference();
        }
    }

    /**
     * Returns a service object for the dependency.
     *
     * @param bundle       : the bundle
     * @param registration : the service registration of the registered service
     * @return a new service object or a already created service object (in the case of singleton) or <code>null</code>
     * if the instance is no more valid.
     * @see org.osgi.framework.ServiceFactory#getService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration)
     */
    public Object getService(Bundle bundle, ServiceRegistration registration) {
        if (getInstanceManager().getState() == InstanceManager.VALID) {
            return m_strategy.getService(bundle, registration);
        } else {
            return null;
        }
    }

    /**
     * The unget method.
     *
     * @param bundle       : bundle
     * @param registration : service registration
     * @param service      : service object
     * @see org.osgi.framework.ServiceFactory#ungetService(org.osgi.framework.Bundle,
     * org.osgi.framework.ServiceRegistration, java.lang.Object)
     */
    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
        m_strategy.ungetService(bundle, registration, service);
    }

    /**
     * Registers the service. The service object must be able to serve this
     * service.
     * This method also notifies the creation strategy of the publication.
     */
    public void registerService() {
        ServiceRegistration reg = null;
        Properties serviceProperties = null;
        // Do not have to be in the synchronized block, immutable.
        final BundleContext bc = m_handler.getInstanceManager().getContext();

        synchronized (this) {
            if (m_serviceRegistration != null) {
                return;
            } else {
                if (m_handler.getInstanceManager().getState() == ComponentInstance.VALID && isAtLeastAServiceControllerValid()) {
                    // Security check
                    if (SecurityHelper.hasPermissionToRegisterServices(
                            m_serviceSpecifications, bc) && SecurityHelper.canRegisterService(bc)) {
                        serviceProperties = getServiceProperties();
                    } else {
                        throw new SecurityException("The bundle "
                                + bc.getBundle().getBundleId()
                                + " does not have the"
                                + " permission to register the services "
                                + Arrays.asList(m_serviceSpecifications));
                    }
                } else {
                    // We don't have to do anything.
                    return;
                }
            }
        }

        // Registration must be done outside of the synchronized block.
        m_strategy.onPublication(getInstanceManager(),
                getServiceSpecificationsToRegister(),
                serviceProperties);
        reg = bc.registerService(
                getServiceSpecificationsToRegister(), this,
                (Dictionary) serviceProperties);

        boolean update = false;
        synchronized (this) {
            if (m_serviceRegistration != null) {
                // Oh oh the service was registered twice. Unregister the last one
                reg.unregister();
                return;
            } else {
                m_serviceRegistration = reg;
            }

            // An update may happen during the registration, re-check and apply.
            // This must be call outside the synchronized block.
            // If the registration is null, the security helper returns false.
            if (m_wasUpdated && SecurityHelper.canUpdateService(reg)) {
                serviceProperties = getServiceProperties();
                update = true;
            }
        }

        if (update) {
            reg.setProperties(serviceProperties);
        }

        synchronized (this) {
            m_publishedProperties = serviceProperties;
            m_wasUpdated = false;

            // Call the post-registration callback in the same thread holding
            // the monitor lock.
            // This allows to be sure that the callback is called once per
            // registration.
            // But the callback must take care to not create a deadlock
            if (m_postRegistration != null) {
                try {
                    m_postRegistration
                            .call(new Object[]{m_serviceRegistration
                                    .getReference()});
                } catch (Exception e) {
                    m_handler.error(
                            "Cannot invoke the post-registration callback "
                                    + m_postRegistration.getMethod(), e);
                }
            }
        }

        // Notify: ProvidedServiceListeners.serviceRegistered()
        notifyListeners(+1);

    }

    /**
     * Withdraws the service from the service registry.
     */
    public void unregisterService() {
        ServiceReference ref = null;
        synchronized (this) {
            // Create a copy of the service reference in the case we need
            // to inject it to the post-unregistration callback.
            if (m_serviceRegistration != null) {
                ref = m_serviceRegistration.getReference();
                m_serviceRegistration.unregister();
                m_serviceRegistration = null;
            }

            m_strategy.onUnpublication();

            // Call the post-unregistration callback in the same thread holding the monitor lock.
            // This allows to be sure that the callback is called once per unregistration.
            // But the callback must take care to not create a deadlock
            if (m_postUnregistration != null && ref != null) {
                try {
                    m_postUnregistration.call(new Object[]{ref});
                } catch (Exception e) {
                    m_handler.error("Cannot invoke the post-unregistration callback " + m_postUnregistration.getMethod(), e);
                }
            }
        }

        // Notify: ProvidedServiceListeners.serviceUnregistered()
        if (ref != null) {
            notifyListeners(-1);
        }

    }

    /**
     * Get the current provided service state.
     *
     * @return The state of the provided service.
     */
    public int getState() {
        if (m_serviceRegistration == null) {
            return UNREGISTERED;
        } else {
            return REGISTERED;
        }
    }

    protected InstanceManager getInstanceManager() {
        return m_handler.getInstanceManager();
    }

    /**
     * Return the list of properties attached to this service. This list
     * contains only property where a value are assigned.
     * <p/>
     * This method is called while holding the monitor lock.
     *
     * @return the properties attached to the provided service.
     */
    private Properties getServiceProperties() {
        // Build the service properties list
        Properties serviceProperties = new Properties();
        for (Property p : m_properties.values()) {
            final Object value = p.getValue();
            if (value != null && value != Property.NO_VALUE) {
                serviceProperties.put(p.getName(), value);
            }
        }
        return serviceProperties;
    }

    /**
     * Get the list of properties attached to the service registration.
     *
     * @return the properties attached to the provided service.
     */
    public synchronized Property[] getProperties() {
        return m_properties.values().toArray(new Property[m_properties.size()]);
    }

    /**
     * Update the service properties. The new list of properties is sent to
     * the service registry.
     */
    public void update() {
        boolean doCallListener = false;
        synchronized (this) {
            // Update the service registration
            if (m_serviceRegistration != null) {
                Properties updated = getServiceProperties();
                Dictionary oldProps = (Dictionary) ((Properties) m_publishedProperties).clone();
                Dictionary newProps = (Dictionary) (updated.clone());

                // Remove keys that must not be compared
                newProps.remove(Factory.INSTANCE_NAME_PROPERTY);
                oldProps.remove(Factory.INSTANCE_NAME_PROPERTY);
                newProps.remove(Constants.SERVICE_ID);
                oldProps.remove(Constants.SERVICE_ID);
                newProps.remove(Constants.SERVICE_PID);
                oldProps.remove(Constants.SERVICE_PID);
                newProps.remove("factory.name");
                oldProps.remove("factory.name");
                newProps.remove(ConfigurationAdmin.SERVICE_FACTORYPID);
                oldProps.remove(ConfigurationAdmin.SERVICE_FACTORYPID);

                // Trigger the update only if the properties have changed.

                // First check, are the size equals
                if (oldProps.size() != newProps.size()) {
                    if (SecurityHelper.canUpdateService(m_serviceRegistration)) {
                        m_handler.info("Updating Registration : " + oldProps.size() + " / " + newProps.size());
                        m_publishedProperties = updated;
                        m_serviceRegistration.setProperties(updated);
                        doCallListener = true;
                    }
                } else {
                    // Check changes
                    Enumeration keys = oldProps.keys();
                    boolean hasChanged = false;
                    while (!hasChanged && keys.hasMoreElements()) {
                        String k = (String) keys.nextElement();
                        Object val = oldProps.get(k);
                        if (!val.equals(updated.get(k))) {
                            hasChanged = true;
                        }
                    }
                    if (hasChanged && SecurityHelper.canUpdateService(m_serviceRegistration)) {
                        m_handler.info("Updating Registration : " + updated);
                        m_publishedProperties = updated;
                        m_serviceRegistration.setProperties(updated);
                        doCallListener = true;
                    }
                }
            } else {
                // Need to be updated later.
                m_wasUpdated = true;
            }
        }
        if (doCallListener) {
            // Notify: ProvidedServiceListeners.serviceUpdated()
            notifyListeners(0);
        }
    }

    /**
     * Add properties to the list.
     *
     * @param props : properties to add
     */
    protected void addProperties(Dictionary props) {
        Enumeration keys = props.keys();
        boolean updated = false;
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            Object value = props.get(key);

            // m_properties can be modified by another thread, we need to make a stack confinement
            Property property;
            synchronized (this) {
                property = m_properties.get(key);
            }

            if (property != null) {
                // Already existing property
                if (property.getValue() == null || !value.equals(property.getValue())) {
                    property.setValue(value);
                    updated = true;
                }
            } else {
                try {
                    // Create the property.
                    property = new Property(key, null, null, value, getInstanceManager(), m_handler);
                    addProperty(property);
                    updated = true;
                } catch (ConfigurationException e) {
                    m_handler.error("The propagated property " + key + " cannot be created correctly : " + e.getMessage());
                }
            }
        }

        if (updated) {
            m_handler.info("Update triggered by adding properties " + props);
            update();
        }
    }

    /**
     * Remove properties from the list.
     *
     * @param props : properties to remove
     */
    protected void deleteProperties(Dictionary props) {
        Enumeration keys = props.keys();
        boolean mustUpdate = false;
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            mustUpdate = mustUpdate || removeProperty(key);
        }

        if (mustUpdate) {
            m_handler.info("Update triggered when removing properties : " + props);
            update();
        }
    }

    /**
     * Get the published service specifications.
     *
     * @return the list of provided service specifications (i.e. java
     * interface).
     */
    public String[] getServiceSpecifications() {
        return m_serviceSpecifications;
    }

    /**
     * Get the service registration.
     *
     * @return the service registration of this service.
     */
    public ServiceRegistration getServiceRegistration() {
        return m_serviceRegistration;
    }

    /**
     * Sets the service controller on this provided service.
     *
     * @param field         the field attached to this controller
     * @param value         the value the initial value
     * @param specification the target specification, if <code>null</code>
     *                      affect all specifications.
     */
    public void setController(String field, boolean value, String specification) {
        if (specification == null) {
            m_controllers.put(ALL_SPECIFICATIONS_FOR_CONTROLLERS, new ServiceController(field, value));
        } else {
            m_controllers.put(specification, new ServiceController(field, value));

        }
    }

    /**
     * Gets the service controller attached to the given field.
     *
     * @param field the field name
     * @return the service controller, {@code null} if there is no service controller attached to the given field
     * name.
     */
    public ServiceController getController(String field) {
        for (ServiceController controller : m_controllers.values()) {
            if (field.equals(controller.m_field)) {
                return controller;
            }
        }
        return null;
    }

    /**
     * Gets the service controller handling the service publishing the given specification.
     *
     * @param spec the specification (qualified class name)
     * @return the service controller, {@code null} if there is no service controller handling the service publishing
     * the given service specification
     */
    public ServiceController getControllerBySpecification(String spec) {
        return m_controllers.get(spec);
    }

    /**
     * Checks if at least one service controller is valid.
     *
     * @return <code>true</code> if one service controller at least
     * is valid.
     */
    private boolean isAtLeastAServiceControllerValid() {
        // No controller
        if (m_controllers.isEmpty()) {
            return true;
        }

        for (ServiceController controller : m_controllers.values()) {
            if (controller.getValue()) {
                return true;
            }
        }
        return false;
    }

    private String[] getServiceSpecificationsToRegister() {
        if (m_controllers.isEmpty()) {
            return m_serviceSpecifications;
        }

        ArrayList<String> l = new ArrayList<String>();
        if (m_controllers.containsKey(ALL_SPECIFICATIONS_FOR_CONTROLLERS)) {
            ServiceController ctrl = m_controllers.get(ALL_SPECIFICATIONS_FOR_CONTROLLERS);
            if (ctrl.m_value) {
                l.addAll(Arrays.asList(m_serviceSpecifications));
            }
        }

        for (String spec : m_controllers.keySet()) {
            ServiceController ctrl = m_controllers.get(spec);
            if (ctrl.m_value) {
                if (!ALL_SPECIFICATIONS_FOR_CONTROLLERS.equals(spec)) { // Already added.
                    if (!l.contains(spec)) {
                        l.add(spec);
                    }
                }
            } else {
                l.remove(spec);
            }
        }

        return l.toArray(new String[l.size()]);

    }

    public void setPostRegistrationCallback(Callback cb) {
        m_postRegistration = cb;
    }

    public void setPostUnregistrationCallback(Callback cb) {
        m_postUnregistration = cb;
    }

    public int getPolicy() {
        return m_policy;
    }

    public Class<? extends CreationStrategy> getCreationStrategy() {
        return m_strategy.getClass();
    }

    /**
     * Add the given listener to the provided service handler's list of listeners.
     *
     * @param listener the {@code ProvidedServiceListener} object to be added
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void addListener(ProvidedServiceListener listener) {
        if (listener == null) {
            throw new NullPointerException("null listener");
        }
        synchronized (m_listeners) {
            m_listeners.add(listener);
        }
    }

    /**
     * Remove the given listener from the provided service handler's list of listeners.
     *
     * @param listener the {@code ProvidedServiceListener} object to be removed
     * @throws NullPointerException   if {@code listener} is {@code null}
     * @throws NoSuchElementException if {@code listener} wasn't present the in provided service handler's list of listeners
     */
    public void removeListener(ProvidedServiceListener listener) {
        if (listener == null) {
            throw new NullPointerException("null listener");
        }
        synchronized (m_listeners) {
            // We definitely cannot rely on listener's equals method...
            // ...so we need to manually search for the listener, using ==.
            int i = -1;
            for (int j = m_listeners.size() - 1; j >= 0; j--) {
                if (m_listeners.get(j) == listener) {
                    // Found!
                    i = j;
                    break;
                }
            }
            if (i != -1) {
                m_listeners.remove(i);
            } else {
                throw new NoSuchElementException("no such listener");
            }
        }
    }

    /**
     * Notify all listeners that a change has occurred in this provided service.
     *
     * @param direction the "direction" of the change (+1:registration, 0:update, -1:unregistration)
     */
    private void notifyListeners(int direction) {
        // Get a snapshot of the listeners
        List<ProvidedServiceListener> tmp;
        synchronized (m_listeners) {
            tmp = new ArrayList<ProvidedServiceListener>(m_listeners);
        }
        // Do notify, outside the m_listeners lock
        for (ProvidedServiceListener l : tmp) {
            try {
                if (direction > 0) {
                    l.serviceRegistered(m_handler.getInstanceManager(), this);
                } else if (direction < 0) {
                    l.serviceUnregistered(m_handler.getInstanceManager(), this);
                } else {
                    l.serviceModified(m_handler.getInstanceManager(), this);
                }
            } catch (Throwable e) {
                // Put a warning on the logger, and continue
                m_handler.warn(
                        String.format(
                                "[%s] A ProvidedServiceListener has failed: %s",
                                m_handler.getInstanceManager().getInstanceName(),
                                e.getMessage())
                        , e);
            }
        }
    }

    /**
     * Removes all the listeners from this provided service before it gets disposed.
     */
    public void cleanup() {
        synchronized (m_listeners) {
            m_listeners.clear();
        }
    }

    /**
     * Service Controller.
     */
    class ServiceController {
        /**
         * The controller value.
         */
        private boolean m_value;
        /**
         * The field attached to this controller.
         */
        private final String m_field;

        /**
         * Creates a ServiceController.
         *
         * @param field the field
         * @param value the initial value
         */
        public ServiceController(String field, boolean value) {
            m_field = field;
            m_value = value;
        }

        public String getField() {
            return m_field;
        }

        /**
         * Gets the value.
         *
         * @return the value
         */
        public boolean getValue() {
            synchronized (ProvidedService.this) {
                return m_value;
            }
        }

        /**
         * Sets the value.
         *
         * @param value the value
         */
        public void setValue(Boolean value) {
            synchronized (ProvidedService.this) {
                if (value != m_value) {
                    // If there is a change to the ServiceController value then
                    // we will
                    // need to modify the registrations.
                    m_value = value;
                    unregisterService();
                    if (getServiceSpecificationsToRegister().length != 0) {
                        registerService();
                    }
                }
            }
        }

    }

    /**
     * Singleton creation strategy.
     * This strategy just creates one service object and
     * returns always the same.
     */
    private class SingletonStrategy extends CreationStrategy {

        /**
         * The service is going to be registered.
         *
         * @param instance   the instance manager
         * @param interfaces the published interfaces
         * @param props      the properties
         * @see org.apache.felix.ipojo.handlers.providedservice.CreationStrategy#onPublication(InstanceManager, java.lang.String[], java.util.Properties)
         */
        public void onPublication(InstanceManager instance, String[] interfaces,
                                  Properties props) {
        }

        /**
         * The service was unpublished.
         *
         * @see org.apache.felix.ipojo.handlers.providedservice.CreationStrategy#onUnpublication()
         */
        public void onUnpublication() {
        }

        /**
         * A service object is required.
         *
         * @param arg0 the bundle requiring the service object.
         * @param arg1 the service registration.
         * @return the first pojo object.
         * @see org.osgi.framework.ServiceFactory#getService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration)
         */
        public Object getService(Bundle arg0, ServiceRegistration arg1) {
            return m_handler.getInstanceManager().getPojoObject();
        }

        /**
         * A service object is released.
         *
         * @param arg0 the bundle
         * @param arg1 the service registration
         * @param arg2 the get service object.
         * @see org.osgi.framework.ServiceFactory#ungetService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration, java.lang.Object)
         */
        public void ungetService(Bundle arg0, ServiceRegistration arg1,
                                 Object arg2) {
        }

    }

    /**
     * Service object creation policy following the OSGi Service Factory
     * policy {@link ServiceFactory}.
     */
    private class FactoryStrategy extends CreationStrategy {

        /**
         * The service is going to be registered.
         *
         * @param instance   the instance manager
         * @param interfaces the published interfaces
         * @param props      the properties
         * @see org.apache.felix.ipojo.handlers.providedservice.CreationStrategy#onPublication(InstanceManager, java.lang.String[], java.util.Properties)
         */
        public void onPublication(InstanceManager instance, String[] interfaces,
                                  Properties props) {
        }

        /**
         * The service is unpublished.
         *
         * @see org.apache.felix.ipojo.handlers.providedservice.CreationStrategy#onUnpublication()
         */
        public void onUnpublication() {
        }

        /**
         * OSGi Service Factory getService method.
         * Returns a new service object per asking bundle.
         * This object is then cached by the framework.
         *
         * @param arg0 the bundle requiring the service
         * @param arg1 the service registration
         * @return the service object for the asking bundle
         * @see org.osgi.framework.ServiceFactory#getService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration)
         */
        public Object getService(Bundle arg0, ServiceRegistration arg1) {
            return m_handler.getInstanceManager().createPojoObject();
        }

        /**
         * OSGi Service Factory unget method.
         * Deletes the created object for the asking bundle.
         *
         * @param arg0 the asking bundle
         * @param arg1 the service registration
         * @param arg2 the created service object returned for this bundle
         * @see org.osgi.framework.ServiceFactory#ungetService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration, java.lang.Object)
         */
        public void ungetService(Bundle arg0, ServiceRegistration arg1,
                                 Object arg2) {
            m_handler.getInstanceManager().deletePojoObject(arg2);
        }
    }


    /**
     * Service object creation policy creating a service object per asking iPOJO component
     * instance. This creation policy follows the iPOJO Service Factory interaction pattern
     * and does no support 'direct' invocation.
     */
    private class PerInstanceStrategy extends CreationStrategy implements IPOJOServiceFactory, InvocationHandler {
        /**
         * Map [ComponentInstance->ServiceObject] storing created service objects.
         */
        private Map/*<ComponentInstance, ServiceObject>*/ m_instances = new HashMap();

        /**
         * A method is invoked on the proxy object.
         * If the method is the {@link IPOJOServiceFactory#getService(ComponentInstance)}
         * method, this method creates a service object if not already created for the asking
         * component instance.
         * If the method is {@link IPOJOServiceFactory#ungetService(ComponentInstance, Object)}
         * the service object is unget (i.e. removed from the map and deleted).
         * In all other cases, a {@link UnsupportedOperationException} is thrown as this policy
         * requires to use  the {@link IPOJOServiceFactory} interaction pattern.
         *
         * @param arg0 the proxy object
         * @param arg1 the called method
         * @param arg2 the arguments
         * @return the service object attached to the asking instance for 'get',
         * <code>null</code> for 'unget',
         * a {@link UnsupportedOperationException} for all other methods.
         * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
         */
        public Object invoke(Object arg0, Method arg1, Object[] arg2) {
            if (isGetServiceMethod(arg1)) {
                return getService((ComponentInstance) arg2[0]);
            }

            if (isUngetServiceMethod(arg1)) {
                ungetService((ComponentInstance) arg2[0], arg2[1]);
                return null;
            }

            // Regular methods from java.lang.Object : equals and hashCode
            if (arg1.getName().equals("equals") && arg2 != null && arg2.length == 1) {
                return this.equals(arg2[0]);
            }

            if (arg1.getName().equals("hashCode")) {
                return this.hashCode();
            }

            throw new UnsupportedOperationException("This service requires an advanced creation policy. "
                    + "Before calling the service, call the getService(ComponentInstance) method to get "
                    + "the service object. - Method called: " + arg1.getName());
        }

        /**
         * A service object is required.
         * This policy returns a service object per asking instance.
         *
         * @param instance the instance requiring the service object
         * @return the service object for this instance
         * @see org.apache.felix.ipojo.IPOJOServiceFactory#getService(org.apache.felix.ipojo.ComponentInstance)
         */
        public Object getService(ComponentInstance instance) {
            Object obj = m_instances.get(instance);
            if (obj == null) {
                obj = m_handler.getInstanceManager().createPojoObject();
                m_instances.put(instance, obj);
            }
            return obj;
        }

        /**
         * A service object is unget.
         * The service object is removed from the map and deleted.
         *
         * @param instance  the instance releasing the service
         * @param svcObject the service object
         * @see org.apache.felix.ipojo.IPOJOServiceFactory#ungetService(org.apache.felix.ipojo.ComponentInstance, java.lang.Object)
         */
        public void ungetService(ComponentInstance instance, Object svcObject) {
            Object pojo = m_instances.remove(instance);
            m_handler.getInstanceManager().deletePojoObject(pojo);
        }

        /**
         * The service is going to be registered.
         *
         * @param instance   the instance manager
         * @param interfaces the published interfaces
         * @param props      the properties
         * @see org.apache.felix.ipojo.handlers.providedservice.CreationStrategy#onPublication(InstanceManager, java.lang.String[], java.util.Properties)
         */
        public void onPublication(InstanceManager instance, String[] interfaces,
                                  Properties props) {
        }

        /**
         * The service is going to be unregistered.
         * The instance map is cleared. Created object are disposed.
         *
         * @see org.apache.felix.ipojo.handlers.providedservice.CreationStrategy#onUnpublication()
         */
        public void onUnpublication() {
            Collection col = m_instances.values();
            Iterator it = col.iterator();
            while (it.hasNext()) {
                m_handler.getInstanceManager().deletePojoObject(it.next());
            }
            m_instances.clear();
        }

        /**
         * OSGi Service Factory getService method.
         *
         * @param arg0 the asking bundle
         * @param arg1 the service registration
         * @return a proxy implementing the {@link IPOJOServiceFactory}
         * @see org.osgi.framework.ServiceFactory#getService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration)
         */
        public Object getService(Bundle arg0, ServiceRegistration arg1) {
            Object proxy = Proxy.newProxyInstance(getInstanceManager().getClazz().getClassLoader(),
                    getSpecificationsWithIPOJOServiceFactory(m_serviceSpecifications, m_handler.getInstanceManager().getContext()), this);
            return proxy;
        }

        /**
         * OSGi Service factory unget method.
         * Does nothing.
         *
         * @param arg0 the asking bundle
         * @param arg1 the service registration
         * @param arg2 the service object created for this bundle.
         * @see org.osgi.framework.ServiceFactory#ungetService(org.osgi.framework.Bundle, org.osgi.framework.ServiceRegistration, java.lang.Object)
         */
        public void ungetService(Bundle arg0, ServiceRegistration arg1,
                                 Object arg2) {
        }

        /**
         * Utility method returning the class array of provided service
         * specification and the {@link IPOJOServiceFactory} interface.
         *
         * @param specs the published service interface
         * @param bc    the bundle context, used to load classes
         * @return the class array containing provided service specification and
         * the {@link IPOJOServiceFactory} class.
         */
        private Class[] getSpecificationsWithIPOJOServiceFactory(String[] specs, BundleContext bc) {
            Class[] classes = new Class[specs.length + 1];
            int i = 0;
            for (i = 0; i < specs.length; i++) {
                try {
                    classes[i] = bc.getBundle().loadClass(specs[i]);
                } catch (ClassNotFoundException e) {
                    // Should not happen.
                }
            }
            classes[i] = IPOJOServiceFactory.class;
            return classes;
        }


    }

}
