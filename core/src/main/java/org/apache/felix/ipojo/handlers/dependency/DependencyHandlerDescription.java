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
package org.apache.felix.ipojo.handlers.dependency;

import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.dependency.impl.ServiceReferenceManager;
import org.apache.felix.ipojo.dependency.interceptors.ServiceTrackingInterceptor;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.DependencyModel;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import java.util.List;
import java.util.Map;

/**
 * Dependency Handler Description.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DependencyHandlerDescription extends HandlerDescription {

    /**
     * Dependencies managed by the dependency handler.
     */
    private DependencyDescription[] m_dependencies = new DependencyDescription[0];

    /**
     * Creates the Dependency Handler description.
     * @param handler the Dependency Handler.
     * @param deps the Dependencies
     */
    public DependencyHandlerDescription(DependencyHandler handler, Dependency[] deps) {
        super(handler);
        m_dependencies = new DependencyDescription[deps.length];
        for (int i = 0; i < m_dependencies.length; i++) {
            m_dependencies[i] = new DependencyDescription(deps[i]);
        }
    }

    /**
     * Get dependencies description.
     * @return the dependencies list.
     */
    public DependencyDescription[] getDependencies() {
        return m_dependencies;
    }

    /**
     * Builds the Dependency Handler description.
     * @return the handler description.
     * @see org.apache.felix.ipojo.architecture.HandlerDescription#getHandlerInfo()
     */
    public Element getHandlerInfo() {
        Element deps = super.getHandlerInfo();
        for (DependencyDescription dependency : m_dependencies) {
            String state = "resolved";
            if (dependency.getState() == DependencyModel.UNRESOLVED) {
                state = "unresolved";
            }
            if (dependency.getState() == DependencyModel.BROKEN) {
                state = "broken";
            }
            Element dep = new Element("Requires", "");
            dep.addAttribute(new Attribute("Specification", dependency.getInterface()));
            dep.addAttribute(new Attribute("Id", dependency.getId()));

            if (dependency.getFilter() != null) {
                dep.addAttribute(new Attribute("Filter", dependency.getFilter()));
            }

            if (dependency.isOptional()) {
                dep.addAttribute(new Attribute("Optional", "true"));
                if (dependency.supportsNullable()) {
                    dep.addAttribute(new Attribute("Nullable", "true"));
                }
                if (dependency.getDefaultImplementation() != null) {
                    dep.addAttribute(new Attribute("Default-Implementation", dependency.getDefaultImplementation()));
                }
            } else {
                dep.addAttribute(new Attribute("Optional", "false"));
            }

            if (dependency.isMultiple()) {
                dep.addAttribute(new Attribute("Aggregate", "true"));
            } else {
                dep.addAttribute(new Attribute("Aggregate", "false"));
            }

            if (dependency.isProxy()) {
                dep.addAttribute(new Attribute("Proxy", "true"));
            } else {
                dep.addAttribute(new Attribute("Proxy", "false"));
            }

            String policy = "dynamic";
            if (dependency.getPolicy() == DependencyModel.STATIC_BINDING_POLICY) {
                policy = "static";
            } else if (dependency.getPolicy() == DependencyModel.DYNAMIC_PRIORITY_BINDING_POLICY) {
                policy = "dynamic-priority";
            }
            dep.addAttribute(new Attribute("Binding-Policy", policy));

            if (dependency.getComparator() != null) {
                dep.addAttribute(new Attribute("Comparator", dependency.getComparator()));
            }

            dep.addAttribute(new Attribute("State", state));
            List<ServiceReference> set = dependency.getUsedServices();
            if (set != null) {
                for (ServiceReference ref : set) {
                    Element use = new Element("Uses", "");
                    computeServiceReferenceDescription(ref, use);
                    dep.addElement(use);
                }
            }

            set = dependency.getServiceReferences();
            if (set != null) {
                for (ServiceReference ref : set) {
                    Element use = new Element("Selected", "");
                    computeServiceReferenceDescription(ref, use);
                    dep.addElement(use);
                }
            }

            final ServiceReferenceManager serviceReferenceManager = dependency.getDependency()
                    .getServiceReferenceManager();
            if (serviceReferenceManager == null) {
                // Exit here, cannot compute anything else.
                deps.addElement(dep);
                continue;
            }

            set = serviceReferenceManager.getMatchingServices();
            if (set != null) {
                for (ServiceReference ref : set) {
                    Element use = new Element("Matches", "");
                    computeServiceReferenceDescription(ref, use);
                    dep.addElement(use);
                }
            }

            // Add interceptors to the description
            List<ServiceReference> interceptors = serviceReferenceManager.getTrackingInterceptorReferences();
            for (ServiceReference ref : interceptors) {
                Element itcp = new Element("ServiceTrackingInterceptor", "");
                computeInterceptorDescription(ref, itcp);
                dep.addElement(itcp);
            }

            ServiceReference ref = serviceReferenceManager.getRankingInterceptorReference();
            if (ref != null) {
                Element itcp = new Element("ServiceRankingInterceptor", "");
                computeInterceptorDescription(ref, itcp);
                dep.addElement(itcp);
            }

            interceptors = serviceReferenceManager.getBindingInterceptorReferences();
            for (ServiceReference rf : interceptors) {
                Element itcp = new Element("ServiceBindingInterceptor", "");
                computeInterceptorDescription(rf, itcp);
                dep.addElement(itcp);
            }

            deps.addElement(dep);
        }
        return deps;
    }

    private void computeServiceReferenceDescription(ServiceReference ref, Element use) {
        use.addAttribute(new Attribute(Constants.SERVICE_ID, ref.getProperty(Constants.SERVICE_ID).toString()));
        String instance = (String) ref.getProperty(Factory.INSTANCE_NAME_PROPERTY);
        if (instance != null) {
            use.addAttribute(new Attribute(Factory.INSTANCE_NAME_PROPERTY, instance));
        }
    }

    private void computeInterceptorDescription(ServiceReference ref, Element itcp) {
        itcp.addAttribute(new Attribute(Constants.SERVICE_ID, ref.getProperty(Constants.SERVICE_ID).toString()));
        itcp.addAttribute(new Attribute("bundle.id", Long.toString(ref.getBundle().getBundleId())));
        String instance = (String) ref.getProperty(Factory.INSTANCE_NAME_PROPERTY);
        if (instance != null) {
            itcp.addAttribute(new Attribute(Factory.INSTANCE_NAME_PROPERTY, instance));
        }
        itcp.addAttribute(new Attribute("target", ref.getProperty(ServiceTrackingInterceptor.TARGET_PROPERTY)
                .toString()));
    }

}
