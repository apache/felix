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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Dictionary;

import org.apache.felix.dm.BundleDependency;
import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentDependencyDeclaration;
import org.apache.felix.dm.context.AbstractDependency;
import org.apache.felix.dm.context.DependencyContext;
import org.apache.felix.dm.context.Event;
import org.apache.felix.dm.context.EventType;
import org.apache.felix.dm.tracker.BundleTracker;
import org.apache.felix.dm.tracker.BundleTrackerCustomizer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class BundleDependencyImpl extends AbstractDependency<BundleDependency> implements BundleDependency, BundleTrackerCustomizer, ComponentDependencyDeclaration {
    private BundleTracker m_tracker;
    private volatile int m_stateMask = Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE;
    private Bundle m_bundleInstance;
    private volatile Filter m_filter;
    private volatile long m_bundleId = -1;
    private Object m_nullObject;
    private volatile boolean m_propagate;
    private volatile Object m_propagateCallbackInstance;
    private volatile String m_propagateCallbackMethod;

    public BundleDependencyImpl() {
    }
    
    public BundleDependencyImpl(BundleDependencyImpl prototype) {
        super(prototype);
        m_stateMask = prototype.m_stateMask;
        m_nullObject = prototype.m_nullObject;
        m_bundleInstance = prototype.m_bundleInstance;
        m_filter = prototype.m_filter;
        m_bundleId = prototype.m_bundleId;
        m_propagate = prototype.m_propagate;
        m_propagateCallbackInstance = prototype.m_propagateCallbackInstance;
        m_propagateCallbackMethod = prototype.m_propagateCallbackMethod;       
    }
    
    @Override
    public DependencyContext createCopy() {
        return new BundleDependencyImpl(this);
    }
    
    @Override
    public void start() {
        m_tracker = new BundleTracker(m_component.getBundleContext(), m_stateMask, this);
        m_tracker.open();
        super.start();
    }

    @Override
    public void stop() {
        m_tracker.close();
        m_tracker = null;
        super.stop();
    }
    
    @Override
    public String getName() {
        StringBuilder sb = new StringBuilder();
        getSimpleName(sb);
        if (m_filter != null) {
            sb.append(" ");
            sb.append(m_filter.toString());
        }
        if (m_bundleId != -1) {
            sb.append("{bundle.id=" + m_bundleId + "}");
        }
        return sb.toString();
    }

    @Override
    public String getSimpleName() {
        // Return the state mask, but don't include the filter or bundle id.
        StringBuilder sb = new StringBuilder();
        if ((m_stateMask & Bundle.ACTIVE) != 0) {
            sb.append("active");
        }
        if ((m_stateMask & Bundle.INSTALLED) != 0) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append("installed");
        }
        if ((m_stateMask & Bundle.RESOLVED) != 0) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append("resolved");
        }
        return sb.toString();
    }
    
    @Override
    public String getFilter() {
        if (m_filter != null || m_bundleId != -1) {
            StringBuilder sb = new StringBuilder();
            if (m_filter != null) {
                sb.append(m_filter.toString());
            }
            if (m_bundleId != -1) {
                sb.append("{bundle.id=" + m_bundleId + "}");
            }
            return sb.toString();
        }
        return null;
    }

    @Override
    public String getType() {
        return "bundle";
    }

    public Object addingBundle(Bundle bundle, BundleEvent event) {
        // if we don't like a bundle, we could reject it here by returning null
        long bundleId = bundle.getBundleId();
        if (m_bundleId >= 0 && m_bundleId != bundleId) {
            return null;
        }
        Filter filter = m_filter;
        if (filter != null) {
            Dictionary<?,?> headers = bundle.getHeaders();
            if (!m_filter.match(headers)) {
                return null;
            }
        }
        return bundle;
    }
    
    public void addedBundle(Bundle bundle, BundleEvent event, Object object) {
        m_component.handleEvent(this, EventType.ADDED, new BundleEventImpl(bundle, event));
    }
        
    public void modifiedBundle(Bundle bundle, BundleEvent event, Object object) {
        m_component.handleEvent(this, EventType.CHANGED, new BundleEventImpl(bundle, event));
    }

    public void removedBundle(Bundle bundle, BundleEvent event, Object object) {
        m_component.handleEvent(this, EventType.REMOVED, new BundleEventImpl(bundle, event));
    }
    
    @Override
    public void invokeCallback(EventType type, Event ... e) {
        switch (type) {
        case ADDED:
            if (m_add != null) {
                invoke(m_add, e[0]);
            }
            break;
        case CHANGED:
            if (m_change != null) {
                invoke (m_change, e[0]);
            }
            break;
        case REMOVED:
            if (m_remove != null) {
                invoke (m_remove, e[0]);
            }
            break;
        default:
            break;
        }
    }
            
    private void invoke(String method, Event e) {
        BundleEventImpl be = (BundleEventImpl) e;
        m_component.invokeCallbackMethod(getInstances(), method,
            new Class[][] {{Bundle.class}, {Object.class}, {Component.class, Bundle.class}, {}},             
            new Object[][] {{be.getBundle()}, {be.getBundle()}, {m_component, be.getBundle()}, {}});
    }
    
    public BundleDependency setBundle(Bundle bundle) {
        m_bundleId = bundle.getBundleId();
        return this;
    }

    public BundleDependency setFilter(String filter) throws IllegalArgumentException {
        if (filter != null) {
            try {
                m_filter = FrameworkUtil.createFilter(filter);
            } 
            catch (InvalidSyntaxException e) {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
        return this;
    }
    
    public BundleDependency setStateMask(int mask) {
        m_stateMask = mask;
        return this;
    }
    
    @Override
    public Class<?> getAutoConfigType() {
        return Bundle.class;
    }
        
    @SuppressWarnings("unchecked")
    @Override
    public Dictionary<String, Object> getProperties() {
        Event event = getService();
        if (event != null) {
            Bundle bundle = (Bundle) event.getEvent();
            if (m_propagateCallbackInstance != null && m_propagateCallbackMethod != null) {
                try {
                    CallbackTypeDef callbackInfo = new CallbackTypeDef(Bundle.class, bundle);
                    return (Dictionary<String, Object>) InvocationUtil.invokeCallbackMethod(m_propagateCallbackInstance, m_propagateCallbackMethod, callbackInfo.m_sigs, callbackInfo.m_args);
                }
                catch (InvocationTargetException e) {
                    m_component.getLogger().warn("Exception while invoking callback method", e.getCause());
                }
                catch (Throwable e) {
                    m_component.getLogger().warn("Exception while trying to invoke callback method", e);
                }
                throw new IllegalStateException("Could not invoke callback");
            }
            else {
                return (Dictionary<String, Object>) bundle.getHeaders();
            }
        }
        else {
            throw new IllegalStateException("cannot find bundle");
        }
    }
    
    @Override
    public Object getDefaultService(boolean nullObject) {
        Object service = null;
        if (isAutoConfig()) {
            // TODO does it make sense to add support for custom bundle impls?
            // service = getDefaultImplementation();
            if (service == null && nullObject) {
                service = getNullObject();
            }
        }
        return service;
    }

    private Bundle getNullObject() {
        if (m_nullObject == null) {
            try {
                m_nullObject = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] { Bundle.class }, new DefaultNullObject()); 
            }
            catch (Throwable e) {
                m_component.getLogger().err("Could not create null object for Bundle.", e);
            }
        }
        return (Bundle) m_nullObject;
    }
    
    private void getSimpleName(StringBuilder sb) {
        if ((m_stateMask & Bundle.ACTIVE) != 0) {
            sb.append("active");
        }
        if ((m_stateMask & Bundle.INSTALLED) != 0) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append("installed");
        }
        if ((m_stateMask & Bundle.RESOLVED) != 0) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append("resolved");
        }
    }
}

