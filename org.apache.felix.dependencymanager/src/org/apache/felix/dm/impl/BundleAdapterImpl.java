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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentStateListener;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.context.DependencyContext;
import org.osgi.framework.Bundle;

/**
 * Bundle Adapter Service implementation. This class extends the FilterService in order to catch
 * some Service methods for configuring actual adapter service implementation.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class BundleAdapterImpl extends FilterComponent
{
	/**
     * Creates a new Bundle Adapter Service implementation.
     */
    public BundleAdapterImpl(DependencyManager dm, int bundleStateMask, String bundleFilter, boolean propagate)
    {
    	this(dm, bundleStateMask, bundleFilter, propagate, null, null, null, null);
    }

	public BundleAdapterImpl(DependencyManager dm, int stateMask, String filter, boolean propagate, Object cbInstance, String add, String change, String remove) {
        super(dm.createComponent()); // This service will be filtered by our super class, allowing us to take control.
        m_component
        	.setImplementation(new BundleAdapterDecorator(stateMask, propagate, cbInstance, add, change, remove))
        	.add(dm.createBundleDependency()
        		   .setFilter(filter)
        		   .setStateMask(stateMask)
        		   .setCallbacks("added", "removed"))
        	.setCallbacks("init", null, "stop", null);
	}

	public class BundleAdapterDecorator extends AbstractDecorator {
        private final boolean m_propagate;
        private final int m_bundleStateMask;
        public final Object m_cbInstance;
    	public final String m_add;
    	public final String m_change;
    	public final String m_remove;

        public BundleAdapterDecorator(int bundleStateMask, boolean propagate) {
            this(bundleStateMask, propagate, null, null, null, null);
        }
        
		public BundleAdapterDecorator(int bundleStateMask, boolean propagate, Object callbackInstance, String add, String change, String remove) {
            m_bundleStateMask = bundleStateMask;
            m_propagate = propagate;
            m_cbInstance = callbackInstance;
            m_add = add;
            m_change = change;
            m_remove = remove;
        }

		public Component createService(Object[] properties) {
            Bundle bundle = (Bundle) properties[0];
            Hashtable<String, Object> props = new Hashtable<>();
            if (m_serviceProperties != null) {
                Enumeration<String> e = m_serviceProperties.keys();
                while (e.hasMoreElements()) {
                    String key = e.nextElement();
                    props.put(key, m_serviceProperties.get(key));
                }
            }
            List<DependencyContext> dependencies = m_component.getDependencies();
            // the first dependency is always the dependency on the bundle, which
            // will be replaced with a more specific dependency below
            dependencies.remove(0);
            Component service = m_manager.createComponent()
                .setInterface(m_serviceInterfaces, props)
                .setImplementation(m_serviceImpl)
                .setFactory(m_factory, m_factoryCreateMethod) // if not set, no effect
                .setComposition(m_compositionInstance, m_compositionMethod) // if not set, no effect
                .setCallbacks(m_callbackObject, m_init, m_start, m_stop, m_destroy) // if not set, no effect
                .add(m_manager.createBundleDependency()
                    .setBundle(bundle)
                    .setStateMask(m_bundleStateMask)
                    .setPropagate(m_propagate)
                    .setCallbacks(m_cbInstance, m_add, m_change, m_remove) // if no callbacks, autoconfig is enabled
                    .setRequired(true));

            copyDependencies(dependencies, service);

            for (ComponentStateListener stateListener : m_stateListeners) {
                service.add(stateListener);
            }
            configureAutoConfigState(service, m_component);
            return service;
        }
    }
}
