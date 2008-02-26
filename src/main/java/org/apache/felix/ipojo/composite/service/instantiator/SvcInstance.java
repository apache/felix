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
package org.apache.felix.ipojo.composite.service.instantiator;

import java.util.Comparator;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.util.AbstractServiceDependency;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

/**
 * Manage a service instantiation. This service create component instance providing the required service specification.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class SvcInstance extends AbstractServiceDependency {

    /**
     * Configuration to push to the instance.
     */
    private Dictionary m_configuration;

    /**
     * Handler creating the service instance.
     */
    private ServiceDependencyHandler m_handler;

    /**
     * Map of matching factories Service Reference => instance or null (null if the service reference is not actually used).
     */
    private Map /* <ServiceReference, Instance> */m_matchingFactories = new HashMap();

    /**
     * Required specification.
     */
    private String m_specification;

    /**
     * Is the service provider frozen ? (Is used for static biding policy)
     */
    private boolean m_isFrozen;

    /**
     * Constructor.
     * @param h : the handler.
     * @param spec : required specification.
     * @param conf : instance configuration.
     * @param isAgg : is the service instance an aggregate service ?
     * @param isOpt : is the service instance optional ?
     * @param filt : LDAP filter
     * @param cmp : comparator to use for the tracking
     * @param policy : binding policy
     * @throws ConfigurationException : an attribute cannot be parsed correctly, or is incorrect.
     */
    public SvcInstance(ServiceDependencyHandler h, String spec, Dictionary conf, boolean isAgg, boolean isOpt, Filter filt, Comparator cmp, int policy) throws ConfigurationException {
        super(Factory.class, isAgg, isOpt, filt, cmp, policy, null, h);

        m_specification = spec;

        m_handler = h;
        setBundleContext(m_handler.getCompositeManager().getServiceContext());

        m_configuration = conf;
    }

    /**
     * Stop the service instance.
     */
    public void stop() {
        super.stop();

        Set keys = m_matchingFactories.keySet();
        Iterator it = keys.iterator();
        while (it.hasNext()) {
            ServiceReference ref = (ServiceReference) it.next();
            Object o = m_matchingFactories.get(ref);
            if (o != null) {
                ((ComponentInstance) o).dispose();
            }
        }

        m_matchingFactories.clear();

    }

    public boolean isFrozen() {
        return m_isFrozen;
    }

    /**
     * Freeze the set of used provider.
     * This method is when the static binding policy is applied.
     */
    public void freeze() {
        m_isFrozen = true;
    }

    /**
     * Create an instance for the given reference. The instance is not added inside the map.
     * @param factory : the factory from which we need to create the instance.
     * @return the created component instance.
     * @throws ConfigurationException : the instance cannot be configured correctly.
     * @throws MissingHandlerException : the factory is invalid.
     * @throws UnacceptableConfiguration : the given configuration is invalid for the given factory.
     */
    private ComponentInstance createInstance(Factory factory) throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        // Recreate the configuration to avoid sharing.
        Properties p = new Properties();
        Enumeration kk = m_configuration.keys();
        while (kk.hasMoreElements()) {
            String k = (String) kk.nextElement();
            p.put(k, m_configuration.get(k));
        }
        ComponentInstance instance = null;
        instance = factory.createComponentInstance(p);
        return instance;
    }

    /**
     * Does the service instance match with the given factory ?
     * @param fact : the factory to test.
     * @return true if the factory match, false otherwise.
     */
    public boolean match(ServiceReference fact) {
        // Check if the factory can provide the specification
        ComponentTypeDescription desc = (ComponentTypeDescription) fact.getProperty("component.description");
        if (desc == null) { 
            return false; // No component type description.
        }

        String[] provides = desc.getprovidedServiceSpecification();
        for (int i = 0; provides != null && i < provides.length; i++) {
            if (provides[i].equals(m_specification)) {
                // Check that the factory needs every properties contained in
                // the configuration
                org.apache.felix.ipojo.architecture.PropertyDescription[] props = desc.getProperties();
                Enumeration e = m_configuration.keys();
                while (e.hasMoreElements()) {
                    String k = (String) e.nextElement();
                    if (!containsProperty(k, props)) { return false; }
                }

                Properties p = new Properties();
                Enumeration keys = m_configuration.keys();
                while (keys.hasMoreElements()) {
                    String k = (String) keys.nextElement();
                    p.put(k, m_configuration.get(k));
                }

                Factory factory = (Factory) getService(fact);
                return factory.isAcceptable(p);
            }
        }
        return false;
    }

    /**
     * Does the factory support the given property ? This method check if the property is contained in the given property description array.
     * @param name : name of the property
     * @param props : list of property description
     * @return true if the factory support this property
     */
    private boolean containsProperty(String name, org.apache.felix.ipojo.architecture.PropertyDescription[] props) {
        for (int i = 0; props != null && i < props.length; i++) {
            if (props[i].getName().equalsIgnoreCase(name)) { return true; }
        }
        if (name.equalsIgnoreCase("name")) { return true; } // Skip the name property
        return false;
    }

    /**
     * Get the required specification.
     * @return the required specification.
     */
    public String getServiceSpecification() {
        return m_specification;
    }

    /**
     * Get the map of used references [reference, component instance].
     * @return the map of used references.
     */
    protected Map getMatchingFactories() {
        return m_matchingFactories;
    }

    /**
     * On Dependency Reconfiguration notification method.
     * @param departs : leaving service references.
     * @param arrivals : new injected service references.
     * @see org.apache.felix.ipojo.util.AbstractServiceDependency#onDependencyReconfiguration(org.osgi.framework.ServiceReference[], org.osgi.framework.ServiceReference[])
     */
    public void onDependencyReconfiguration(ServiceReference[] departs, ServiceReference[] arrivals) {
        for (int i = 0; departs != null && i < departs.length; i++) {
            onServiceDeparture(departs[i]);
        }
        
        for (int i = 0; arrivals != null && i < arrivals.length; i++) {
            onServiceArrival(arrivals[i]);
        }
    }

    /**
     * A new service is injected.
     * This method create the sub-service instance in the composite.
     * @param ref : service reference.
     * @see org.apache.felix.ipojo.util.AbstractServiceDependency#onServiceArrival(org.osgi.framework.ServiceReference)
     */
    public void onServiceArrival(ServiceReference ref) {
        // The given factory matches.
        try {
            Factory fact = (Factory) getService(ref);
            ComponentInstance ci = createInstance(fact);
            m_matchingFactories.put(ref, ci);
        } catch (UnacceptableConfiguration e) {
            m_handler.error("A matching factory refuse the actual configuration : " + e.getMessage());
            m_handler.getCompositeManager().stop();
        } catch (MissingHandlerException e) {
            m_handler.error("A matching factory is no more valid : " + e.getMessage());
            m_handler.getCompositeManager().stop();
        } catch (ConfigurationException e) {
            m_handler.error("A matching configuration is refuse by the instance : " + e.getMessage());
            m_handler.getCompositeManager().stop();
        }

    }

    
    /**
     * A used service is leaving.
     * This method dispose the created instance.
     * @param ref : leaving service reference.
     * @see org.apache.felix.ipojo.util.AbstractServiceDependency#onServiceDeparture(org.osgi.framework.ServiceReference)
     */
    public void onServiceDeparture(ServiceReference ref) {
        // Remove the reference is contained
        Object o = m_matchingFactories.remove(ref);
        if (o != null) {
            ((ComponentInstance) o).dispose();
        }
    }

}
