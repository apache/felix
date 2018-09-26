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

import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.dm.BundleComponent;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentStateListener;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ResourceComponent;
import org.apache.felix.dm.ResourceDependency;
import org.apache.felix.dm.context.DependencyContext;

/**
 * Resource adapter service implementation. This class extends the FilterService in order to catch
 * some Service methods for configuring actual resource adapter service implementation.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ResourceAdapterImpl extends FilterComponent<ResourceComponent> implements ResourceComponent {
    private volatile Object m_callbackInstance;
    private volatile String m_callbackChanged;
    private volatile String m_callbackAdded;
    private volatile String m_resourceFilter;
    private volatile boolean m_propagate = true;
    private volatile Object m_propagateCallbackInstance;
    private volatile String m_propagateCallbackMethod;

    public ResourceAdapterImpl(DependencyManager dm) {
        super(dm.createComponent()); // This service will be filtered by our super class, allowing us to take control.
    }

	public ResourceComponent setResourceFilter(String filter) {
		m_resourceFilter = filter;
		return this;
	}
            
	public ResourceComponent setPropagate(boolean propagate) {
		m_propagate = propagate;
		return this;
	}
	
	public ResourceComponent setPropagate(Object propagateCbInstance, String propagateCbMethod) {
		m_propagateCallbackInstance = propagateCbInstance;
		m_propagateCallbackMethod = propagateCbMethod;
		return this;
	}

	public ResourceComponent setBundleCallbacks(String add, String change) {
		m_callbackAdded = add;
		m_callbackChanged = change;
		return this;
	}

	public ResourceComponent setBundleCallbackInstance(Object callbackInstance) {
		m_callbackInstance = callbackInstance;
		return this;
	}

    @Override
    protected void startInitial() {
        DependencyManager dm = getDependencyManager();
        m_component.setImplementation(new ResourceAdapterDecorator())
        .add(dm.createResourceDependency()
             .setFilter(m_resourceFilter)
             .setAutoConfig(false)
             .setCallbacks("added", "removed"))
        .setCallbacks("init", null, "stop", null);
    }
        
    public String getName() {
        return "Resource Adapter" + ((m_resourceFilter != null) ? " with filter " + m_resourceFilter : "");
    }

    public class ResourceAdapterDecorator extends AbstractDecorator {
        
        public Component createService(Object[] properties) {
            URL resource = (URL) properties[0]; 
            Hashtable<String, Object> props = new Hashtable<>();
            if (m_serviceProperties != null) {
                Enumeration<String> e = m_serviceProperties.keys();
                while (e.hasMoreElements()) {
                    String key = e.nextElement();
                    props.put(key, m_serviceProperties.get(key));
                }
            }
            List<DependencyContext> dependencies = m_component.getDependencies();
            // the first dependency is always the dependency on the resource, which
            // will be replaced with a more specific dependency below
            dependencies.remove(0);
            ResourceDependency resourceDependency = m_manager.createResourceDependency()
                 .setResource(resource)
                 .setCallbacks(m_callbackInstance, m_callbackAdded, m_callbackChanged, null)
                 .setAutoConfig(m_callbackAdded == null)
                 .setRequired(true);
            if (m_propagateCallbackInstance != null && m_propagateCallbackMethod != null) {
                resourceDependency.setPropagate(m_propagateCallbackInstance, m_propagateCallbackMethod);
            } else {
                resourceDependency.setPropagate(m_propagate);
            }
            Component<?> service = m_manager.createComponent()
                .setInterface(m_serviceInterfaces, props)
                .setImplementation(m_serviceImpl)
                .setFactory(m_factory, m_factoryCreateMethod) // if not set, no effect
                .setComposition(m_compositionInstance, m_compositionMethod) // if not set, no effect
                .setCallbacks(m_callbackObject, m_init, m_start, m_stop, m_destroy) // if not set, no effect
                .setScope(m_scope)
                .add(resourceDependency);
            
            configureAutoConfigState(service, m_component);

            copyDependencies(dependencies, service);

            for (ComponentStateListener stateListener : m_stateListeners) {
                service.add(stateListener);
            }
            return service;
        }
    }
}
