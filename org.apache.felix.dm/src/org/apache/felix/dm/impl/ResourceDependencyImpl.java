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
import java.util.Properties;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ComponentDependencyDeclaration;
import org.apache.felix.dm.ResourceDependency;
import org.apache.felix.dm.ResourceHandler;
import org.apache.felix.dm.context.DependencyContext;
import org.apache.felix.dm.context.Event;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ResourceDependencyImpl extends DependencyImpl<ResourceDependency> implements ResourceDependency, ResourceHandler, ComponentDependencyDeclaration {
    private volatile ServiceRegistration m_registration;
    private volatile String m_resourceFilter;
    private volatile URL m_trackedResource;
    private final Logger m_logger;

    public ResourceDependencyImpl(BundleContext context, Logger logger) {
        super(true /* autoconfig */, context);
        m_logger = logger;
    }
    
    public ResourceDependencyImpl(ResourceDependencyImpl prototype) {
        super(prototype);
        m_resourceFilter = prototype.m_resourceFilter;
        m_trackedResource = prototype.m_trackedResource;
        m_logger = prototype.m_logger;
    }
    
    @Override
    public DependencyContext createCopy() {
        return new ResourceDependencyImpl(this);
    }
    
    @Override
    public void start() {
        boolean wasStarted = isStarted();
        super.start();
        if (!wasStarted) {
            Dictionary props = null;
            if (m_trackedResource != null) {
                props = new Properties();
                props.put(ResourceHandler.URL, m_trackedResource);
            }
            else { 
                if (m_resourceFilter != null) {
                    props = new Properties();
                    props.put(ResourceHandler.FILTER, m_resourceFilter);
                }
            }
            m_registration = m_context.registerService(ResourceHandler.class.getName(), this, props);
        }
    }

    @Override
    public void stop() {
        boolean wasStarted = isStarted();
        super.stop();
        if (wasStarted) {
            m_registration.unregister();
            m_registration = null;
        }
    }

    public void added(URL resource) {
        if (m_trackedResource == null || m_trackedResource.equals(resource)) {
            add(new ResourceEventImpl(resource, null));
        }
    }
    
    public void added(URL resource, Dictionary resourceProperties) {
        if (m_trackedResource == null || m_trackedResource.equals(resource)) {
            add(new ResourceEventImpl(resource, resourceProperties));
        }
    }
        
    public void changed(URL resource) {
        if (m_trackedResource == null || m_trackedResource.equals(resource)) {
            change(new ResourceEventImpl(resource, null));
        }
    }
    
    public void changed(URL resource, Dictionary resourceProperties) {
        if (m_trackedResource == null || m_trackedResource.equals(resource)) {
            change(new ResourceEventImpl(resource, resourceProperties));
        }
    }

    public void removed(URL resource) {
        if (m_trackedResource == null || m_trackedResource.equals(resource)) {
            remove(new ResourceEventImpl(resource, null));
        }
    }
    
    public void removed(URL resource, Dictionary resourceProperties) {
        if (m_trackedResource == null || m_trackedResource.equals(resource)) {
            remove(new ResourceEventImpl(resource, resourceProperties));
        }
    }
    
    @Override
    public void invoke(String method, Event e) {
        ResourceEventImpl re = (ResourceEventImpl) e;
        URL serviceInstance = re.getResource();
        Dictionary<?,?> resourceProperties = re.getResourceProperties();
       
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
    protected Object getService() {
        ResourceEventImpl re = (ResourceEventImpl) m_component.getDependencyEvent(this);
        return re != null ? re.getResource() : null;
    }
            
    @Override
    public Class<?> getAutoConfigType() {
        return URL.class;
    }
        
    public Dictionary<?,?> getProperties() {
        ResourceEventImpl re = (ResourceEventImpl) m_component.getDependencyEvent(this);
        if (re != null) {
            URL resource = re.getResource();
            Dictionary<?,?> resourceProperties = re.getResourceProperties();
            if (m_propagateCallbackInstance != null && m_propagateCallbackMethod != null) {
                try {
                    return (Dictionary<?,?>) InvocationUtil.invokeCallbackMethod(m_propagateCallbackInstance, m_propagateCallbackMethod, new Class[][] {{ URL.class }}, new Object[][] {{ resource }});
                }
                catch (InvocationTargetException e) {
                    m_logger.log(LogService.LOG_WARNING, "Exception while invoking callback method", e.getCause());
                }
                catch (Exception e) {
                    m_logger.log(LogService.LOG_WARNING, "Exception while trying to invoke callback method", e);
                }
                throw new IllegalStateException("Could not invoke callback");
            }
            else {
                Properties props = new Properties();
                props.setProperty(ResourceHandler.HOST, resource.getHost());
                props.setProperty(ResourceHandler.PATH, resource.getPath());
                props.setProperty(ResourceHandler.PROTOCOL, resource.getProtocol());
                props.setProperty(ResourceHandler.PORT, Integer.toString(resource.getPort()));
                // add the custom resource properties
                if (resourceProperties != null) {
                    Enumeration properyKeysEnum = resourceProperties.keys(); 
                    while (properyKeysEnum.hasMoreElements()) {
                        String key = (String) properyKeysEnum.nextElement();
                        if (!key.equals(ResourceHandler.HOST) &&
                                !key.equals(ResourceHandler.PATH) &&
                                !key.equals(ResourceHandler.PROTOCOL) &&
                                !key.equals(ResourceHandler.PORT)) {
                            props.setProperty(key, resourceProperties.get(key).toString());
                        } else {
                            m_logger.log(LogService.LOG_WARNING, "Custom resource property is overlapping with the default resource property for key: " + key);
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
        if (m_resourceFilter != null) {
            sb.append(m_resourceFilter);
        }
        if (m_trackedResource != null) {
            sb.append(m_trackedResource.toString());
        }
        return sb.toString();
    }

    @Override
    public String getType() {
        return "resource";
    }
}
