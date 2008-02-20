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

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.PolicyServiceContext;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.composite.CompositeHandler;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.AbstractServiceDependency;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

/**
 * This handler manages the import and the export of services from /
 * to the parent context.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ImportHandler extends CompositeHandler {

    /**
     * List of importers.
     */
    private List m_importers = new ArrayList();
    
    /**
     * Flag to check if the start method has finished.
     */
    private boolean m_isStarted;
    

    /**
     * Configure the handler.
     * 
     * @param metadata : the metadata of the component
     * @param conf : the instance configuration
     * @throws ConfigurationException : the specification attribute is missing. 
     * @see org.apache.felix.ipojo.CompositeHandler#configure(org.apache.felix.ipojo.CompositeManager,
     * org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(Element metadata, Dictionary conf) throws ConfigurationException {
        Element[] imp = metadata.getElements("requires");
        
        // Get instance filters
        Dictionary filtersConfiguration = null;
        if (conf.get("requires.filters") != null) {
            filtersConfiguration = (Dictionary) conf.get("requires.filters");
        }

        for (int i = 0; imp != null && i < imp.length; i++) {
            boolean optional = false;
            boolean aggregate = false;
            String specification = imp[i].getAttribute("specification");

            if (specification != null) {                
                String opt = imp[i].getAttribute("optional");
                optional = opt != null && opt.equalsIgnoreCase("true");

                String agg = imp[i].getAttribute("aggregate");
                aggregate = agg != null && agg.equalsIgnoreCase("true");

                String filter_orig = "(&(objectClass=" + specification + ")(!(instance.name=" + getCompositeManager().getInstanceName() + ")))"; // Cannot import yourself
                String filter = filter_orig;
                String f = imp[i].getAttribute("filter");
                if (f != null) {
                    filter = "(&" + filter + f + ")";
                }
                
                String id = imp[i].getAttribute("id");
                
                String scope = imp[i].getAttribute("scope");
                BundleContext bc = getCompositeManager().getGlobalContext(); // Get the default bundle context.
                if (scope != null) {
                    if (scope.equalsIgnoreCase("global")) {
                        bc = new PolicyServiceContext(getCompositeManager().getGlobalContext(), getCompositeManager().getParentServiceContext(), PolicyServiceContext.GLOBAL);
                    } else if (scope.equalsIgnoreCase("composite")) {
                        bc = new PolicyServiceContext(getCompositeManager().getGlobalContext(), getCompositeManager().getParentServiceContext(), PolicyServiceContext.LOCAL);
                    } else if (scope.equalsIgnoreCase("composite+global")) {
                        bc = new PolicyServiceContext(getCompositeManager().getGlobalContext(), getCompositeManager().getParentServiceContext(), PolicyServiceContext.LOCAL_AND_GLOBAL);
                    }
                }
                
                // Configure instance filter if available
                if (filtersConfiguration != null && id != null && filtersConfiguration.get(id) != null) {
                    filter = "(&" + filter_orig + (String) filtersConfiguration.get(id) + ")";
                }
                
                Filter fil = null;
                if (filter != null) {
                    try {
                        fil = getCompositeManager().getGlobalContext().createFilter(filter);
                    } catch (InvalidSyntaxException e) {
                        throw new ConfigurationException("A required filter " + filter + " is malformed : " + e.getMessage());
                    }
                }
                
                Comparator cmp = AbstractServiceDependency.getComparator(imp[i], getCompositeManager().getGlobalContext());
                Class spec = AbstractServiceDependency.loadSpecification(specification, getCompositeManager().getGlobalContext());
                int policy = AbstractServiceDependency.getPolicy(imp[i]);
                
                ServiceImporter si = new ServiceImporter(spec, fil, aggregate, optional, cmp, policy, bc, id, this);
                m_importers.add(si);
            } else { // Malformed import
                error( "Malformed imports : the specification attribute is mandatory");
                throw new ConfigurationException("Malformed imports : the specification attribute is mandatory");
            }
        }
    }

    /**
     * Start the handler.
     * Start importers and exporters.
     * @see org.apache.felix.ipojo.CompositeHandler#start()
     */
    public void start() {
        for (int i = 0; i < m_importers.size(); i++) {
            ServiceImporter si = (ServiceImporter) m_importers.get(i);
            si.start();
        }
        isHandlerValid();
        m_isStarted = true;
    }

    /**
     * Stop the handler.
     * Stop all importers and exporters.
     * @see org.apache.felix.ipojo.CompositeHandler#stop()
     */
    public void stop() {
        for (int i = 0; i < m_importers.size(); i++) {
            ServiceImporter si = (ServiceImporter) m_importers.get(i);
            si.stop();
        }
        m_isStarted = false;
    }

    /**
     * Check the handler validity.
     * @return true if all importers and exporters are valid
     * @see org.apache.felix.ipojo.CompositeHandler#isValid()
     */
    public void isHandlerValid() {
        for (int i = 0; i < m_importers.size(); i++) {
            ServiceImporter si = (ServiceImporter) m_importers.get(i);
            if (si.getState() != AbstractServiceDependency.RESOLVED) {
                setValidity(false);
                return;
            }
        }
        setValidity(true);
    }
    
    public void stateChanged(int newState) {
        // If we are becoming valid and started, check if we need to freeze importers.
        if (m_isStarted && newState == ComponentInstance.VALID) { 
            for (int i = 0; i < m_importers.size(); i++) {
                ServiceImporter si = (ServiceImporter) m_importers.get(i);
                if (si.isStatic()) {
                    si.freeze();
                }
            }
        }
    }

    /**
     * Notify the handler that an importer is no more valid.
     * 
     * @param importer : the implicated importer.
     */
    protected void invalidating(ServiceImporter importer) {
        // An import is no more valid
        if (getValidity()) {
            setValidity(false);
        }
    }

    /**
     * Notify the handler that an importer becomes valid. 
     * @param importer : the implicated importer.
     */
    protected void validating(ServiceImporter importer) {
        // An import becomes valid
        if (!getValidity()) {
            isHandlerValid();
        }
    }

    /**
     * Get the import / export handler description.
     * @return the handler description
     * @see org.apache.felix.ipojo.CompositeHandler#getDescription()
     */
    public HandlerDescription getDescription() {
        return new ImportDescription(this, m_importers);
    }
    
    public List getRequirements() {
        return m_importers;
    }
}
