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
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentDependencyDeclaration;
import org.apache.felix.dm.ResourceDependency;
import org.apache.felix.dm.ResourceHandler;
import org.apache.felix.dm.context.AbstractDependency;
import org.apache.felix.dm.context.DependencyContext;
import org.apache.felix.dm.context.Event;
import org.apache.felix.dm.context.EventType;
import org.osgi.framework.ServiceRegistration;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ResourceDependencyImpl extends AbstractDependency<ResourceDependency> implements ResourceDependency, ResourceHandler, ComponentDependencyDeclaration {
    private volatile ServiceRegistration m_registration;
    private volatile String m_resourceFilter;
    private volatile URL m_trackedResource;

    public ResourceDependencyImpl() {
    }
    
    public ResourceDependencyImpl(ResourceDependencyImpl prototype) {
        super(prototype);
        m_resourceFilter = prototype.m_resourceFilter;
        m_trackedResource = prototype.m_trackedResource;
    }
    
    @Override
    public DependencyContext createCopy() {
        return new ResourceDependencyImpl(this);
    }
    
    @Override
    public void start() {
        Dictionary<String, Object> props = null;
        if (m_trackedResource != null) {
            props = new Hashtable<>();
            props.put(ResourceHandler.URL, m_trackedResource);
        } else {
            if (m_resourceFilter != null) {
                props = new Hashtable<>();
                props.put(ResourceHandler.FILTER, m_resourceFilter);
            }
        }
        m_registration = m_component.getBundleContext().registerService(ResourceHandler.class.getName(), this, props);
        super.start();
    }

    @Override
    public void stop() {
        m_registration.unregister();
        m_registration = null;
        super.stop();
    }

    public void added(URL resource) {
        if (m_trackedResource == null || m_trackedResource.equals(resource)) {
            getComponentContext().handleEvent(this, EventType.ADDED, new ResourceEventImpl(resource, null));
        }
    }
    
    public void added(URL resource, Dictionary<?, ?> resourceProperties) {
        if (m_trackedResource == null || m_trackedResource.equals(resource)) {
            getComponentContext().handleEvent(this, EventType.ADDED, new ResourceEventImpl(resource, resourceProperties));
        }
    }
        
    public void changed(URL resource) {
        if (m_trackedResource == null || m_trackedResource.equals(resource)) {
            m_component.handleEvent(this, EventType.CHANGED, new ResourceEventImpl(resource, null));
        }
    }
    
    public void changed(URL resource, Dictionary<?, ?> resourceProperties) {
        if (m_trackedResource == null || m_trackedResource.equals(resource)) {
            m_component.handleEvent(this, EventType.CHANGED, new ResourceEventImpl(resource, resourceProperties));
        }
    }

    public void removed(URL resource) {
        if (m_trackedResource == null || m_trackedResource.equals(resource)) {
            m_component.handleEvent(this, EventType.REMOVED, new ResourceEventImpl(resource, null));
        }
    }
    
    public void removed(URL resource, Dictionary<?, ?> resourceProperties) {
        if (m_trackedResource == null || m_trackedResource.equals(resource)) {
            m_component.handleEvent(this, EventType.REMOVED, new ResourceEventImpl(resource, resourceProperties));
        }
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
        ResourceEventImpl re = (ResourceEventImpl) e;
        URL serviceInstance = re.getResource();
        Dictionary<?,?> resourceProperties = re.getProperties();
       
        m_component.invokeCallbackMethod(getInstances(), method,
            new Class[][] {
                    { Component.class, URL.class, Dictionary.class }, 
                    { Component.class, URL.class },
                    { Component.class },  
                    { URL.class, Dictionary.class }, 
                    { URL.class },
                    { Object.class }, 
                    {}},
            new Object[][] {
                    { m_component, serviceInstance, resourceProperties }, 
                    { m_component, serviceInstance }, 
                    { m_component }, 
                    { serviceInstance, resourceProperties },
                    { serviceInstance },
                    { serviceInstance }, 
                    {}}
        );

    }  
                    
    public ResourceDependency setResource(URL resource) {
        m_trackedResource = resource;
        return this;
    }
    
    public ResourceDependency setFilter(String resourceFilter) {
        ensureNotActive();
        m_resourceFilter = resourceFilter;
        return this;
    }
    
    public ResourceDependency setFilter(String resourceFilter, String resourcePropertiesFilter) {
        ensureNotActive();
        m_resourceFilter = resourceFilter;
        return this;
    }
    
    @Override
    public Class<?> getAutoConfigType() {
        return URL.class;
    }
        
    @SuppressWarnings("unchecked")
    public Dictionary<String, Object> getProperties() {
        ResourceEventImpl re = (ResourceEventImpl) m_component.getDependencyEvent(this);
        if (re != null) {
            URL resource = re.getResource();
            Dictionary<String, Object> resourceProperties = re.getProperties();
            if (m_propagateCallbackInstance != null && m_propagateCallbackMethod != null) {
                try {
                    CallbackTypeDef callbackInfo = new CallbackTypeDef(URL.class, resource);
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
                Hashtable<String, Object> props = new Hashtable<>();
                props.put(ResourceHandler.HOST, resource.getHost());
                props.put(ResourceHandler.PATH, resource.getPath());
                props.put(ResourceHandler.PROTOCOL, resource.getProtocol());
                props.put(ResourceHandler.PORT, Integer.toString(resource.getPort()));
                // add the custom resource properties
                if (resourceProperties != null) {
                    Enumeration<String> properyKeysEnum = resourceProperties.keys(); 
                    while (properyKeysEnum.hasMoreElements()) {
                        String key = properyKeysEnum.nextElement();
                        if (!key.equals(ResourceHandler.HOST) &&
                                !key.equals(ResourceHandler.PATH) &&
                                !key.equals(ResourceHandler.PROTOCOL) &&
                                !key.equals(ResourceHandler.PORT)) {
                            props.put(key, resourceProperties.get(key).toString());
                        } else {
                            m_component.getLogger().warn(
                                "Custom resource property is overlapping with the default resource property for key: %s",
                                key);
                        }
                    }
                }
                return props;
            }
        }
        else {
            throw new IllegalStateException("cannot find resource");
        }
    }
    
    @Override
    public String getName() {
        StringBuilder sb = new StringBuilder();
        if (m_trackedResource != null) {
            sb.append(m_trackedResource.toString());
        }
        if (m_resourceFilter != null) {
            sb.append(m_resourceFilter);
        }
        return sb.toString();
    }
    
    @Override
    public String getSimpleName() {
        return m_trackedResource != null ? m_trackedResource.toString() : null;
    }

    @Override
    public String getFilter() {
        return m_resourceFilter;
    }

    @Override
    public String getType() {
        return "resource";
    }
}
