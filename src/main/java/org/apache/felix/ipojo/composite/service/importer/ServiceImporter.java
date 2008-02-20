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
package org.apache.felix.ipojo.composite.service.importer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import org.apache.felix.ipojo.PolicyServiceContext;
import org.apache.felix.ipojo.util.AbstractServiceDependency;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Import a service form the parent to the internal service registry.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceImporter extends AbstractServiceDependency {

    /**
     * Reference on the handler.
     */
    private ImportHandler m_handler;
    
    private BundleContext m_origin;

    private class Record implements ServiceListener {
        /**
         * External Reference.
         */
        private ServiceReference m_ref;
        /**
         * Internal Registration.
         */
        private ServiceRegistration m_reg;
        /**
         * Exposed Object.
         */
        private Object m_svcObject;
        
        private Record(ServiceReference ref) {
            m_ref = ref;
            try {
                m_origin.addServiceListener(this, "(" + Constants.SERVICE_ID + "=" + ref.getProperty(Constants.SERVICE_ID) + ")");
            } catch (InvalidSyntaxException e) {
                // Nothing to do.
            }
        }
        
        private void register() {
            m_svcObject = getService(m_ref);
            m_reg = m_handler.getCompositeManager().getServiceContext().registerService(getSpecification().getName(), m_svcObject, getProps(m_ref));
        }
        
        private void dispose() {
            m_origin.removeServiceListener(this);
            if (m_reg != null) {
                m_reg.unregister();
                m_svcObject = null;
                m_reg = null;
            }
            m_ref = null;
        }
        
        /**
         * Test object equality.
         * @param o : object to confront against the current object.
         * @return true if the two objects are equals (same service reference).
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object o) {
            if (o instanceof Record) {
                Record rec = (Record) o;
                return rec.m_ref == m_ref;
            }
            return false;
        }

        public synchronized void serviceChanged(ServiceEvent evt) {
            // In case of modification, modify the service imported service registration.
            if (m_reg != null && evt.getType() == ServiceEvent.MODIFIED) {
                      m_reg.setProperties(getProps(evt.getServiceReference()));
              }
            }
            
    }

    /**
     * List of managed records.
     */
    private List/*<Record>*/m_records = new ArrayList()/* <Record> */;

    /**
     * Requirement Id.
     */
    private String m_id;

    /**
     * Is this requirement attached to a service-level requirement.
     */
    private boolean m_isServiceLevelRequirement;
    
    private boolean m_isFrozen;

    /**
     * Constructor.
     * 
     * @param specification : targeted specification
     * @param filter : LDAP filter
     * @param multiple : should the importer imports several services ?
     * @param optional : is the import optional ?
     * @param from : parent context
     * @param to : internal context
     * @param policy : resolving policy
     * @param id : requirement id (may be null)
     * @param in : handler
     */
    public ServiceImporter(Class specification, Filter filter, boolean multiple, boolean optional, Comparator cmp, int policy, BundleContext bc, String id, ImportHandler in) {
        super(specification, multiple, optional, filter, cmp, policy, bc);
        
        m_origin = bc;
        
        this.m_handler = in;
        
        if (m_id == null) {
            m_id = super.getSpecification().getName();
        } else {
            m_id = id;
        }
    }

    /**
     * Get the properties for the exposed service from the given reference.
     * 
     * @param ref : the reference.
     * @return the property dictionary
     */
    private static Dictionary getProps(ServiceReference ref) {
        Properties prop = new Properties();
        String[] keys = ref.getPropertyKeys();
        for (int i = 0; i < keys.length; i++) {
            prop.put(keys[i], ref.getProperty(keys[i]));
        }
        return prop;
    }
    
    public boolean isStatic() {
        return getBindingPolicy() == STATIC_BINDING_POLICY;
    }
    
    public void freeze() {
        m_isFrozen = true;
    }
    
    public boolean isFrozen() {
        return m_isFrozen;
    }

    /**
     * Stop the management of the import.
     */
    public void stop() {

        super.stop();

        for (int i = 0; i < m_records.size(); i++) {
            Record rec = (Record) m_records.get(i);
            rec.dispose();
        }
        
        m_records.clear();

    }

    /**
     * Get the record list using the given reference.
     * 
     * @param ref : the reference
     * @return the list containing all record using the given reference
     */
    private List/* <Record> */getRecordsByRef(ServiceReference ref) {
        List l = new ArrayList();
        for (int i = 0; i < m_records.size(); i++) {
            Record rec = (Record) m_records.get(i);
            if (rec.m_ref == ref) {
                l.add(rec);
            }
        }
        return l;
    }

    /**
     * Build the list of imported service provider.
     * @return the list of all imported services.
     */
    protected List getProviders() {
        List l = new ArrayList();
        for (int i = 0; i < m_records.size(); i++) {
            l.add((((Record) m_records.get(i)).m_ref).getProperty("instance.name"));
        }
        return l;
    }
    
    /**
     * Set that this dependency is a service level dependency.
     * This forces the scoping policy to be STRICT. 
     * @param b
     */
    public void setServiceLevelDependency() {
        m_isServiceLevelRequirement = true;
        PolicyServiceContext bc = new PolicyServiceContext(m_handler.getCompositeManager().getGlobalContext(), m_handler.getCompositeManager().getParentServiceContext(), PolicyServiceContext.LOCAL);
        setBundleContext(bc);
    }

    public String getId() {
        return m_id;
    }
    
    public boolean isServiceLevelRequirement() {
        return m_isServiceLevelRequirement;
    }

    public void invalidate() {
        m_handler.invalidating(this);
    }

    public void onDependencyReconfiguration(ServiceReference[] departs, ServiceReference[] arrivals) {
        throw new UnsupportedOperationException("Service import does not support dependency reconfiguration");
    }

    public void onServiceArrival(ServiceReference ref) {
        Record rec = new Record(ref);
        m_records.add(rec);
        // Always register the reference, as the method is call only when needed. 
        rec.register();
    }

    public void onServiceDeparture(ServiceReference ref) {
        List l = getRecordsByRef(ref);
        for (int i = 0; i < l.size(); i++) { // Stop the implied record
            Record rec = (Record) l.get(i);
            rec.dispose();
            
        }
        m_records.removeAll(l);
    }

    public void validate() {
        m_handler.validating(this);
    }

}
