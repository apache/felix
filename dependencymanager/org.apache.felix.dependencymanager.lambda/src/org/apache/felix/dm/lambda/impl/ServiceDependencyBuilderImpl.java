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
package org.apache.felix.dm.lambda.impl;

import java.util.Dictionary;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.ServiceDependency;
import org.apache.felix.dm.lambda.ServiceDependencyBuilder;
import org.osgi.framework.ServiceReference;

public class ServiceDependencyBuilderImpl<S> extends ServiceCallbacksBuilderImpl<S, ServiceDependencyBuilder<S>> implements ServiceDependencyBuilder<S> {
    private final Class<S> m_serviceIface;
    private final Component m_component;
    private String m_filter;
    private ServiceReference<S> m_ref;
    private boolean m_required;
    private String m_debug;
    private boolean m_propagate;
    private Object m_propagateInstance;
    private String m_propagateMethod;
    private Object m_defaultImpl;
    private long m_timeout = -1;
    private boolean m_requiredSet;

    public ServiceDependencyBuilderImpl(Component component, Class<S> service) {
        super(service);
        m_serviceIface = service;
        m_component = component;
    }

    public ServiceDependencyBuilder<S> filter(String filter) {
        m_filter = filter;
        return this;
    }

    public ServiceDependencyBuilder<S> ref(ServiceReference<S> ref) {
        m_ref = ref;
        return this;
    }

    public ServiceDependencyBuilder<S> optional() {
        return required(false);
    }

    public ServiceDependencyBuilder<S> required() {
        return required(true);
    }

    public ServiceDependencyBuilder<S> required(boolean required) {
        m_required = required;
        m_requiredSet = true;
        return this;
    }

    public ServiceDependencyBuilder<S> debug(String label) {
        m_debug = label;
        return this;
    }

    public ServiceDependencyBuilder<S> propagate() {
        return propagate(true);
    }

    public ServiceDependencyBuilder<S> propagate(boolean propagate) {
        m_propagate = propagate;
        return this;
    }

    public ServiceDependencyBuilder<S> propagate(Object instance, String method) {
        m_propagateInstance = instance;
        m_propagateMethod = method;
        return this;
    }
    
    public ServiceDependencyBuilder<S> propagate(Function<ServiceReference<S>, Dictionary<String, Object>> propagate) {
        Object wrappedCallback = new Object() {
            @SuppressWarnings("unused")
            Dictionary<String, Object> propagate(ServiceReference<S> ref) {
                return propagate.apply(ref);
            }
        };
        propagate(wrappedCallback, "propagate");
        return this;
    }    

    public ServiceDependencyBuilder<S> propagate(BiFunction<ServiceReference<S>, S, Dictionary<String, Object>> propagate) {
        Object wrappedCallback = new Object() {
            @SuppressWarnings("unused")
            Dictionary<String, Object> propagate(ServiceReference<S> ref, S service) {
                return propagate.apply(ref, service);
            }
        };
        propagate(wrappedCallback, "propagate");
        return this;
    }    

    public ServiceDependencyBuilder<S> defImpl(Object defaultImpl) {
        m_defaultImpl = defaultImpl;
        return this;
    }

    public ServiceDependencyBuilder<S> timeout(long timeout) {
        m_timeout = timeout;
        required();
        return this;
    }

   	// Build final ServiceDependency object.
    @Override
    public ServiceDependency build() {
        DependencyManager dm = m_component.getDependencyManager();
        if (m_ref != null && m_filter != null) {
            throw new IllegalArgumentException("Can not set ref and filter at the same time");
        }
        if (m_serviceIface == null && (m_ref == null || m_filter == null)) {
            throw new IllegalArgumentException("service interface not specified, and no service reference or service filter specified.");
        }
        ServiceDependency sd = m_timeout > -1 ? dm.createTemporalServiceDependency(m_timeout) : dm.createServiceDependency();
        if (m_ref != null) {
            sd.setService(m_serviceIface, m_ref);
        } else {
            sd.setService(m_serviceIface, m_filter);
        }
        if (! m_requiredSet) {
            m_required = Helpers.isDependencyRequiredByDefault(m_component);
        }
        sd.setRequired(m_required);
        sd.setDefaultImplementation(m_defaultImpl);
        if (m_debug != null) {
            sd.setDebug(m_debug);
        }
        if (m_propagate) {
            sd.setPropagate(true);
        } else if (m_propagateInstance != null) {
            if (m_propagateMethod == null) {
                throw new IllegalArgumentException("propagate instance can't be null");
            }
            sd.setPropagate(m_propagateInstance, m_propagateMethod);
        }
        if (hasCallbacks()) {
            sd.setCallbacks(m_callbackInstance, m_added, m_changed, m_removed, m_swapped);
        } else if (hasRefs()) {
            Object cb = createCallbackInstance();
            sd.setCallbacks(cb, "add", "change", "remove", m_swapRefs.size() > 0 ? "swap" : null);
        }
        
        if (m_autoConfigField != null) {
            sd.setAutoConfig(m_autoConfigField);
        } else {
            sd.setAutoConfig(m_autoConfig);
        }
        return sd;
    }
    
}
