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
package org.apache.felix.ipojo.handlers.configuration;

import org.apache.felix.ipojo.*;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandler;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.parser.PojoMetadata;
import org.apache.felix.ipojo.util.Callback;
import org.apache.felix.ipojo.util.Log;
import org.apache.felix.ipojo.util.Property;
import org.apache.felix.ipojo.util.SecurityHelper;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;

import java.util.*;

/**
 * Handler managing the Configuration Admin.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ConfigurationHandler extends PrimitiveHandler implements ManagedService {

    public static final String MANAGED_SERVICE_PID = "managed.service.pid";
    /**
     * List of the configurable fields.
     */
    private List<Property> m_configurableProperties = new ArrayList<Property>(1);
    /**
     * ProvidedServiceHandler of the component. It is useful to propagate
     * properties to service registrations.
     */
    private ProvidedServiceHandler m_providedServiceHandler;
    /**
     * Properties propagated during the last instance "update".
     */
    private Dictionary m_propagatedFromInstance = new Properties();
    /**
     * Properties to propagate.
     */
    private Dictionary<String, Object> m_toPropagate = new Hashtable<String, Object>();
    /**
     * Properties propagated from the configuration admin.
     */
    private Dictionary m_propagatedFromCA;
    /**
     * Check if the instance was already reconfigured by the configuration admin.
     */
    private boolean m_configurationAlreadyPushed;
    /**
     * should the component propagate configuration ?
     */
    private boolean m_mustPropagate;
    /**
     * Service Registration to publish the service registration.
     */
    private ServiceRegistration m_sr;
    /**
     * Managed Service PID.
     * This PID must be different from the instance name if the instance was created
     * with the Configuration Admin.
     */
    private String m_managedServicePID;
    /**
     * the handler description.
     */
    private ConfigurationHandlerDescription m_description;
    /**
     * Updated method.
     * This method is called when a reconfiguration is completed.
     */
    private Callback m_updated;
    /**
     * The configuration listeners.
     */
    private final Set<ConfigurationListener> m_listeners = new LinkedHashSet<ConfigurationListener>();
    /**
     * The last configuration sent to listeners.
     */
    private Map<String, Object> m_lastConfiguration;

    /**
     * Initialize the component type.
     *
     * @param desc     : component type description to populate.
     * @param metadata : component type metadata.
     * @throws ConfigurationException : metadata are incorrect.
     * @see org.apache.felix.ipojo.Handler#initializeComponentFactory(org.apache.felix.ipojo.architecture.ComponentTypeDescription, org.apache.felix.ipojo.metadata.Element)
     */
    public void initializeComponentFactory(ComponentTypeDescription desc, Element metadata) throws ConfigurationException {
        Element[] confs = metadata.getElements("Properties", "");
        if (confs == null) {
            return;
        }
        Element[] configurables = confs[0].getElements("Property");
        for (int i = 0; configurables != null && i < configurables.length; i++) {
            String fieldName = configurables[i].getAttribute("field");
            String methodName = configurables[i].getAttribute("method");
            String paramIndex = configurables[i].getAttribute("constructor-parameter");

            if (fieldName == null && methodName == null && paramIndex == null) {
                throw new ConfigurationException("Malformed property : The property needs to contain" +
                        " at least a field, a method or a constructor-parameter");
            }

            String name = configurables[i].getAttribute("name");
            if (name == null) {
                if (fieldName == null && methodName != null) {
                    name = methodName;
                } else if (fieldName == null && paramIndex != null) {
                    // Extract the name from the arguments.
                    MethodMetadata[] constructors = getFactory().getPojoMetadata().getConstructors();
                    if (constructors.length != 1) {
                        throw new ConfigurationException("Cannot infer the property name injected in the constructor " +
                                "parameter #" + paramIndex + " - add the `name` attribute");
                    } else {
                        int idx = Integer.valueOf(paramIndex);
                        if (constructors[0].getMethodArgumentNames().length > idx) {
                            name = constructors[0].getMethodArgumentNames()[idx];
                        } else {
                            throw new ConfigurationException("Cannot infer the property name injected in the constructor " +
                                    "parameter #" + paramIndex + " - not enough argument in the constructor :" +
                                    constructors[0].getArguments());
                        }
                    }
                } else {
                    name = fieldName;
                }
                configurables[i].addAttribute(new Attribute("name", name)); // Add the type to avoid configure checking
            }

            String value = configurables[i].getAttribute("value");

            // Detect the type of the property
            PojoMetadata manipulation = getFactory().getPojoMetadata();
            String type = null;
            if (methodName != null) {
                MethodMetadata[] method = manipulation.getMethods(methodName);
                if (method.length == 0) {
                    type = configurables[i].getAttribute("type");
                    if (type == null) {
                        throw new ConfigurationException("Malformed property : The type of the property cannot be discovered, add a 'type' attribute");
                    }
                } else {
                    if (method[0].getMethodArguments().length != 1) {
                        throw new ConfigurationException("Malformed property :  The method " + methodName + " does not have one argument");
                    }
                    type = method[0].getMethodArguments()[0];
                    configurables[i].addAttribute(new Attribute("type", type)); // Add the type to avoid configure checking
                }
            } else if (fieldName != null) {
                FieldMetadata field = manipulation.getField(fieldName);
                if (field == null) {
                    throw new ConfigurationException("Malformed property : The field " + fieldName + " does not exist in the implementation class");
                }
                type = field.getFieldType();
                configurables[i].addAttribute(new Attribute("type", type)); // Add the type to avoid configure checking
            } else if (paramIndex != null) {
                int index = Integer.parseInt(paramIndex);
                type = configurables[i].getAttribute("type");
                MethodMetadata[] cts = manipulation.getConstructors();
                // If we don't have a type, try to get the first constructor and get the type of the parameter
                // we the index 'index'.
                if (type == null && cts.length > 0 && cts[0].getMethodArguments().length > index) {
                    type = cts[0].getMethodArguments()[index];
                } else if (type == null) { // Applied only if type was not determined.
                    throw new ConfigurationException("Cannot determine the type of the property " + index +
                            ", please use the type attribute");
                }
                configurables[i].addAttribute(new Attribute("type", type));
            }

            // Is the property set to immutable
            boolean immutable = false;
            String imm = configurables[i].getAttribute("immutable");
            immutable = imm != null && imm.equalsIgnoreCase("true");

            boolean mandatory = false;
            String man = configurables[i].getAttribute("mandatory");
            mandatory = man != null && man.equalsIgnoreCase("true");

            PropertyDescription pd;
            if (value == null) {
                pd = new PropertyDescription(name, type, null, false); // Cannot be immutable if we have no value.
            } else {
                pd = new PropertyDescription(name, type, value, immutable);
            }

            if (mandatory) {
                pd.setMandatory();
            }

            desc.addProperty(pd);
        }

    }

    /**
     * Configures the handler.
     * Access to field does not require synchronization as this method is executed
     * before any thread access to this object.
     *
     * @param metadata      the metadata of the component
     * @param configuration the instance configuration
     * @throws ConfigurationException one property metadata is not correct
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(Element metadata, Dictionary configuration) throws ConfigurationException {
        // Build the map
        Element[] confs = metadata.getElements("Properties", "");
        Element[] configurables = confs[0].getElements("Property");

        // Check if the component is dynamically configurable
        // Propagation enabled by default.
        m_mustPropagate = true;
        // We must create a copy as the Config Admin dictionary has some limitations
        m_toPropagate = new Hashtable<String, Object>();
        if (configuration != null) {
            Enumeration keys = configuration.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                // To conform with 'Property Propagation 104.4.4 (Config Admin spec)',
                // we don't propagate properties starting with .
                if (!excluded(key)) {
                    m_toPropagate.put(key, configuration.get(key));
                }
            }
        }

        String propa = confs[0].getAttribute("propagation");
        if (propa != null && propa.equalsIgnoreCase("false")) {
            m_mustPropagate = false;
            m_toPropagate = null;
        }

        // Check if the component support ConfigurationADmin reconfiguration
        m_managedServicePID = confs[0].getAttribute("pid"); // Look inside the component type description
        String instanceMSPID = (String) configuration.get(MANAGED_SERVICE_PID); // Look inside the instance configuration.
        if (instanceMSPID != null) {
            m_managedServicePID = instanceMSPID;
        }

        // updated method
        String upd = confs[0].getAttribute("updated");
        if (upd != null) {
            MethodMetadata method = getPojoMetadata().getMethod(upd);
            if (method == null) {
                throw new ConfigurationException("The updated method is not found in the class "
                        + getInstanceManager().getClassName());
            } else if (method.getMethodArguments().length == 0) {
                m_updated = new Callback(upd, new Class[0], false, getInstanceManager());
            } else if (method.getMethodArguments().length == 1
                    && method.getMethodArguments()[0].equals(Dictionary.class.getName())) {
                m_updated = new Callback(upd, new Class[]{Dictionary.class}, false, getInstanceManager());
            } else {
                throw new ConfigurationException("The updated method is found in the class "
                        + getInstanceManager().getClassName() + " must have either no argument or a Dictionary");
            }
        }

        for (int i = 0; configurables != null && i < configurables.length; i++) {
            String fieldName = configurables[i].getAttribute("field");
            String methodName = configurables[i].getAttribute("method");
            String paramIndex = configurables[i].getAttribute("constructor-parameter");
            int index = -1;

            String name = configurables[i].getAttribute("name"); // The initialize method has fixed the property name.
            String value = configurables[i].getAttribute("value");

            String type = configurables[i].getAttribute("type"); // The initialize method has fixed the property name.

            Property prop;
            if (paramIndex == null) {
                prop = new Property(name, fieldName, methodName, value, type, getInstanceManager(), this);
            } else {
                index = Integer.parseInt(paramIndex);
                prop = new Property(name, fieldName, methodName, index,
                        value, type, getInstanceManager(), this);
            }
            addProperty(prop);

            // Check if the instance configuration contains value for the current property :
            if (configuration.get(name) == null) {
                if (fieldName != null && configuration.get(fieldName) != null) {
                    prop.setValue(configuration.get(fieldName));
                }
            } else {
                prop.setValue(configuration.get(name));
            }

            if (fieldName != null) {
                FieldMetadata field = new FieldMetadata(fieldName, type);
                getInstanceManager().register(field, prop);
            }

            if (index != -1) {
                getInstanceManager().register(index, prop);
            }
        }

        m_description = new ConfigurationHandlerDescription(this, m_configurableProperties, m_managedServicePID);

    }

    /**
     * Stop method.
     * This method is synchronized to avoid the configuration admin pushing a configuration during the un-registration.
     * Do nothing.
     *
     * @see org.apache.felix.ipojo.Handler#stop()
     */
    public synchronized void stop() {
        if (m_sr != null) {
            m_sr.unregister();
            m_sr = null;
        }
        m_lastConfiguration = Collections.emptyMap();
    }

    /**
     * Start method.
     * This method is synchronized to avoid the config admin pushing a configuration before ending the method.
     * Propagate properties if the propagation is activated.
     *
     * @see org.apache.felix.ipojo.Handler#start()
     */
    public synchronized void start() {
        // Get the provided service handler :
        m_providedServiceHandler = (ProvidedServiceHandler) getHandler(HandlerFactory.IPOJO_NAMESPACE + ":provides");


        // Propagation
        if (m_mustPropagate) {
            for (Property prop : m_configurableProperties) {
                if (prop.getValue() != Property.NO_VALUE && prop.getValue() != null) { // No injected value, or null
                    m_toPropagate.put(prop.getName(), prop.getValue());
                }
            }

            // We cannot use the reconfigure method directly, as there are no real changes.
            Properties extra = reconfigureProperties(m_toPropagate);
            propagate(extra, m_propagatedFromInstance);
            m_propagatedFromInstance = extra;

            if (getInstanceManager().getPojoObjects() != null) {
                try {
                    notifyUpdated(null);
                } catch (Throwable e) {
                    error("Cannot call the updated method : " + e.getMessage(), e);
                }
            }
        }


        // Give initial values and reset the 'invoked' flag.
        for (Property prop : m_configurableProperties) {
            prop.reset(); // Clear the invoked flag.
            if (prop.hasField() && prop.getValue() != Property.NO_VALUE && prop.getValue() != null) {
                getInstanceManager().onSet(null, prop.getField(), prop.getValue());
            }
        }

        if (m_managedServicePID != null && m_sr == null) {
            Hashtable<String, Object> props = new Hashtable<String, Object>();
            props.put(Constants.SERVICE_PID, m_managedServicePID);
            props.put(Factory.INSTANCE_NAME_PROPERTY, getInstanceManager().getInstanceName());
            props.put("factory.name", getInstanceManager().getFactory().getFactoryName());

            // Security Check
            if (SecurityHelper.hasPermissionToRegisterService(ManagedService.class.getName(),
                    getInstanceManager().getContext()) && SecurityHelper.canRegisterService
                    (getInstanceManager().getContext())) {
                m_sr = getInstanceManager().getContext().registerService(ManagedService.class.getName(), this, props);
            } else {
                error("Cannot register the ManagedService - The bundle "
                        + getInstanceManager().getContext().getBundle().getBundleId()
                        + " does not have the permission to register the service");
            }
        }
    }

    /**
     * Adds the given property metadata to the property metadata list.
     *
     * @param prop : property metadata to add
     */
    protected void addProperty(Property prop) {
        m_configurableProperties.add(prop);
    }

    /**
     * Reconfigure the component instance.
     * Check if the new configuration modifies the current configuration.
     * Invokes the updated method if needed.
     *
     * @param configuration : the new configuration
     * @see org.apache.felix.ipojo.Handler#reconfigure(java.util.Dictionary)
     */
    public void reconfigure(Dictionary configuration) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        boolean changed = false;
        synchronized (this) {
            info(getInstanceManager().getInstanceName() + " is reconfiguring the properties : " + configuration);

            // Is there any changes ?
            changed = detectConfigurationChanges(configuration);
            if (changed) {
                Properties extra = reconfigureProperties(configuration);
                propagate(extra, m_propagatedFromInstance);
                m_propagatedFromInstance = extra;

                if (getInstanceManager().getPojoObjects() != null) {
                    try {
                        notifyUpdated(null);
                    } catch (Throwable e) {
                        error("Cannot call the updated method : " + e.getMessage(), e);
                    }
                }
                // Make a snapshot of the current configuration
                for (Property p : m_configurableProperties) {
                    map.put(p.getName(), p.getValue());
                }
            }
        }

        if (changed) {
            notifyListeners(map);
        }
    }

    private boolean detectConfigurationChanges(Dictionary configuration) {
        Enumeration keysEnumeration = configuration.keys();
        while (keysEnumeration.hasMoreElements()) {
            String name = (String) keysEnumeration.nextElement();
            Object value = configuration.get(name);

            // Some properties are skipped
            if (name.equals(Factory.INSTANCE_NAME_PROPERTY)
                    || name.equals(Constants.SERVICE_PID)
                    || name.equals(MANAGED_SERVICE_PID)) {
                continue;
            }
            // Do we have a property.
            Property p = getPropertyByName(name);
            if (p != null) {
                // Change detection based on the value.
                if (p.getValue() == null) {
                    return true;
                } else if (! p.getValue().equals(value)) {
                    return true;
                }
            } else {
                // Was it propagated ?
                if (m_propagatedFromCA != null) {
                    Object v = m_propagatedFromCA.get(name);
                    if (v == null  || ! v.equals(value)) {
                        return true;
                    }
                }
                if (m_propagatedFromInstance != null) {
                    Object v = m_propagatedFromInstance.get(name);
                    if (v == null  || ! v.equals(value)) {
                        return true;
                    }
                }
            }
        }

        // A propagated property may have been removed.
        if (m_propagatedFromCA != null) {
            Enumeration enumeration = m_propagatedFromCA.keys();
            while (enumeration.hasMoreElements()) {
                String k = (String) enumeration.nextElement();
                if (configuration.get(k)  == null) {
                    return true;
                }
            }
        }
        if (m_propagatedFromInstance != null) {
            Enumeration enumeration = m_propagatedFromInstance.keys();
            while (enumeration.hasMoreElements()) {
                String k = (String) enumeration.nextElement();
                if (configuration.get(k)  == null) {
                    return true;
                }
            }
        }

        return false;
    }

    private Property getPropertyByName(String name) {
        for (Property p : m_configurableProperties) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Reconfigured configuration properties and returns non matching properties.
     * When called, it must hold the monitor lock.
     *
     * @param configuration : new configuration
     * @return the properties that does not match with configuration properties
     */
    private Properties reconfigureProperties(Dictionary configuration) {
        Properties toPropagate = new Properties();
        Enumeration keysEnumeration = configuration.keys();
        while (keysEnumeration.hasMoreElements()) {
            String name = (String) keysEnumeration.nextElement();
            Object value = configuration.get(name);
            boolean found = false;
            // Check if the name is a configurable property
            for (Property prop : m_configurableProperties) {
                if (prop.getName().equals(name)) {
                    Object v = reconfigureProperty(prop, value);
                    found = true;
                    if (m_mustPropagate && ! excluded(name)) {
                        toPropagate.put(name, v);
                    }
                    break; // Exit the search loop
                }
            }

            if (!found && m_mustPropagate && ! excluded(name)) {
                toPropagate.put(name, value);
            }
        }

        // Every removed configurable property gets reset to its default value
        for (Property prop : m_configurableProperties) {
            if (configuration.get(prop.getName()) == null) {
                reconfigureProperty(prop, prop.getDefaultValue());
            }
        }
        return toPropagate;

    }

    /**
     * Checks whether the property with this given name must not be propagated.
     * @param name the name of the property
     * @return {@code true} if the property must not be propagated
     */
    private boolean excluded(String name) {
        return name.startsWith(".")
                || Factory.INSTANCE_NAME_PROPERTY.equals(name)
                || Factory.FACTORY_VERSION_PROPERTY.equals(name)
                || "factory.name".equals(name);
    }

    /**
     * Reconfigures the given property with the given value.
     * This methods handles {@link org.apache.felix.ipojo.InstanceManager#onSet(Object, String, Object)}
     * call and the callback invocation.
     * The reconfiguration occurs only if the value changes.
     *
     * @param prop  the property object to reconfigure
     * @param value the new value.
     * @return the new property value
     */
    public Object reconfigureProperty(Property prop, Object value) {
        if (prop.getValue() == null || !prop.getValue().equals(value)) {
            prop.setValue(value);
            if (prop.hasField()) {
                getInstanceManager().onSet(null, prop.getField(), prop.getValue()); // Notify other handler of the field value change.
            }
            if (prop.hasMethod()) {
                if (getInstanceManager().getPojoObjects() != null) {
                    prop.invoke(null); // Call on all created pojo objects.
                }
            }
        }
        return prop.getValue();
    }

    /**
     * Removes the old properties from the provided services and propagate new properties.
     *
     * @param newProps : new properties to propagate
     * @param oldProps : old properties to remove
     */
    private void propagate(Dictionary newProps, Dictionary oldProps) {
        if (m_mustPropagate && m_providedServiceHandler != null) {
            if (oldProps != null) {
                m_providedServiceHandler.removeProperties(oldProps);
            }

            if (newProps != null) {
                // Remove the name, the pid and the managed service pid props
                newProps.remove(Factory.INSTANCE_NAME_PROPERTY);
                newProps.remove(MANAGED_SERVICE_PID);
                newProps.remove(Constants.SERVICE_PID);

                // Remove all properties starting with . (config admin specification)
                Enumeration<String> keys = newProps.keys();
                List<String> propertiesStartingWithDot = new ArrayList<String>();
                while (keys.hasMoreElements()) {
                    String key = keys.nextElement();
                    if (key.startsWith(".")) {
                        propertiesStartingWithDot.add(key);
                    }
                }
                for (String k : propertiesStartingWithDot) {
                    newProps.remove(k);
                }

                // Propagation of the properties to service registrations :
                m_providedServiceHandler.addProperties(newProps);
            }
        }
    }

    /**
     * Handler createInstance method.
     * This method is override to allow delayed callback invocation.
     * Invokes the updated method if needed.
     *
     * @param instance : the created object
     * @see org.apache.felix.ipojo.PrimitiveHandler#onCreation(Object)
     */
    public void onCreation(Object instance) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        for (Property prop : m_configurableProperties) {
            if (prop.hasMethod()) {
                prop.invoke(instance);
            }
            // Fill the snapshot copy while calling callbacks
            map.put(prop.getName(), prop.getValue());
        }

        try {
            notifyUpdated(instance);
        } catch (Throwable e) {
            error("Cannot call the updated method : " + e.getMessage(), e);
        }
        notifyListeners(map);
    }

    /**
     * Invokes the updated method.
     * This method build the dictionary containing all valued properties,
     * as well as properties propagated to the provided service handler (
     * only if the propagation is enabled).
     *
     * @param instance the instance on which the callback must be called.
     *                 If <code>null</code> the callback is called on all the existing
     *                 object.
     */
    private void notifyUpdated(Object instance) {
        if (m_updated == null) {
            return;
        }

        if (m_updated.getArguments().length == 0) {
            // We don't have to compute the properties,
            // we just call the callback.
            try {
                if (instance == null) {
                    m_updated.call(new Object[0]);
                } else {
                    m_updated.call(instance, new Object[0]);
                }
            } catch (Exception e) {
                error("Cannot call the updated method " + m_updated.getMethod() + " : " + e.getMessage());
            }
            return;
        }

        // Else we must compute the properties.
        Properties props = new Properties();
        for (Property property : m_configurableProperties) {
            String n = property.getName();
            Object v = property.getValue();
            if (v != Property.NO_VALUE) {
                props.put(n, v);
            }
        }
        // add propagated properties to the list if propagation is enabled
        if (m_mustPropagate) {
            // Start by properties from the configuration admin,
            if (m_propagatedFromCA != null) {

                Enumeration e = m_propagatedFromCA.keys();
                while (e.hasMoreElements()) {
                    String k = (String) e.nextElement();
                    if (!k.equals(Factory.INSTANCE_NAME_PROPERTY)) {
                        props.put(k, m_propagatedFromCA.get(k));
                    }
                }
            }
            // Do also the one from the instance configuration
            if (m_propagatedFromInstance != null) {
                Enumeration e = m_propagatedFromInstance.keys();
                while (e.hasMoreElements()) {
                    String k = (String) e.nextElement();
                    if (!k.equals(Factory.INSTANCE_NAME_PROPERTY)) { // Skip instance.name
                        props.put(k, m_propagatedFromInstance.get(k));
                    }
                }
            }
        }

        try {
            if (instance == null) {
                m_updated.call(new Object[]{props});
            } else {
                m_updated.call(instance, new Object[]{props});
            }
        } catch (Exception e) {
            error("Cannot call the updated method " + m_updated.getMethod() + " : " + e.getMessage());
        }
    }

    /**
     * Managed Service method.
     * This method is called when the instance is reconfigured by the ConfigurationAdmin.
     * When called, it must hold the monitor lock.
     *
     * @param conf : pushed configuration.
     * @throws org.osgi.service.cm.ConfigurationException
     *          the reconfiguration failed.
     * @see org.osgi.service.cm.ManagedService#updated(java.util.Dictionary)
     */
    public void updated(Dictionary conf) throws org.osgi.service.cm.ConfigurationException {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        synchronized (this) {
            if (conf == null && !m_configurationAlreadyPushed) {
                return; // First call
            } else if (conf != null) { // Configuration push
                Properties props = reconfigureProperties(conf);
                propagate(props, m_propagatedFromCA);
                m_propagatedFromCA = props;
                m_configurationAlreadyPushed = true;
            } else if (m_configurationAlreadyPushed) { // Configuration deletion
                propagate(null, m_propagatedFromCA);
                m_propagatedFromCA = null;
                m_configurationAlreadyPushed = false;
            }

            if (getInstanceManager().getPojoObjects() != null) {
                try {
                    notifyUpdated(null);
                } catch (Throwable e) {
                    error("Cannot call the updated method : " + e.getMessage(), e);
                }
            }
            // Make a snapshot of the current configuration
            for (Property p : m_configurableProperties) {
                map.put(p.getName(), p.getValue());
            }
        }
        notifyListeners(map);
    }

    /**
     * Gets the configuration handler description.
     *
     * @return the configuration handler description.
     * @see org.apache.felix.ipojo.Handler#getDescription()
     */
    public HandlerDescription getDescription() {
        return m_description;
    }

    /**
     * Add the given listener to the configuration handler's list of listeners.
     *
     * @param listener the {@code ConfigurationListener} object to be added
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    public void addListener(ConfigurationListener listener) {
        if (listener == null) {
            throw new NullPointerException("null listener");
        }
        synchronized (m_listeners) {
            m_listeners.add(listener);
        }
    }

    /**
     * Remove the given listener from the configuration handler's list of listeners.
     * If the listeners is not registered, this method does nothing.
     *
     * @param listener the {@code ConfigurationListener} object to be removed
     * @throws NullPointerException   if {@code listener} is {@code null}
     */
    public void removeListener(ConfigurationListener listener) {
        if (listener == null) {
            throw new NullPointerException("The list of listener is null");
        }
        synchronized (m_listeners) {
            // We definitely cannot rely on listener's equals method...
            // ...so we need to manually search for the listener, using ==.
            ConfigurationListener found = null;
            for (ConfigurationListener l : m_listeners) {
                if (l == listener) {
                    found = l;
                    break;
                }
            }
            if (found != null) {
                m_listeners.remove(found);
            }
        }
    }

    /**
     * Notify all listeners that a reconfiguration has occurred.
     *
     * @param map the new configuration of the component instance.
     */
    private void notifyListeners(Map<String, Object> map) {

        // Get a snapshot of the listeners
        // and check if we had a change in the map.
        List<ConfigurationListener> tmp;
        synchronized (m_listeners) {
            tmp = new ArrayList<ConfigurationListener>(m_listeners);

            if (map == null) {
                if (m_lastConfiguration == null) {
                    // No change.
                    return;
                }
                // Else trigger the change.
            } else {
                if (m_lastConfiguration != null && m_lastConfiguration.size() == map.size()) {
                    // Must compare key by key
                    boolean diff = false;
                    for (String k : map.keySet()) {
                        if (! map.get(k).equals(m_lastConfiguration.get(k))) {
                            // Difference found, break;
                            diff = true;
                            break;
                        }
                    }
                    if (! diff) {
                        // no difference found, skip notification
                        return;
                    }
                }
                // Else difference found, triggers the change
            }

            if (map == null) {
                m_lastConfiguration = Collections.emptyMap();
            } else {
                m_lastConfiguration = Collections.unmodifiableMap(map);
            }

        }
        if (! tmp.isEmpty()) {
            getLogger().log(Log.DEBUG, String.format(
                    "[%s] Notifying configuration listener: %s", getInstanceManager().getInstanceName(), tmp));
        }
        // Protect the map.
        // Do notify, outside any lock
        for (ConfigurationListener l : tmp) {
            try {
                l.configurationChanged(getInstanceManager(), m_lastConfiguration);
            } catch (Throwable e) {
                // Failure inside a listener: put a warning on the logger, and continue
                warn(String.format(
                        "[%s] A ConfigurationListener has failed: %s",
                        getInstanceManager().getInstanceName(),
                        e.getMessage())
                        , e);
            }
        }
    }

    @Override
    public void stateChanged(int state) {
        if (state == ComponentInstance.DISPOSED) {
            // Clean up the list of listeners
            synchronized (m_listeners) {
                m_listeners.clear();
            }
        }
        super.stateChanged(state);
    }
}
