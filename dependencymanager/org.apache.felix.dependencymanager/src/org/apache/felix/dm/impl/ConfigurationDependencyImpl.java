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
import java.util.Dictionary;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.dm.Component;
import org.apache.felix.dm.ConfigurationDependency;
import org.apache.felix.dm.Logger;
import org.apache.felix.dm.PropertyMetaData;
import org.apache.felix.dm.context.AbstractDependency;
import org.apache.felix.dm.context.DependencyContext;
import org.apache.felix.dm.context.Event;
import org.apache.felix.dm.context.EventType;
import org.apache.felix.dm.impl.metatype.MetaTypeProviderImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * Implementation for a configuration dependency.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ConfigurationDependencyImpl extends AbstractDependency<ConfigurationDependency> implements ConfigurationDependency, ManagedService {
    private Dictionary<String, Object> m_settings;
	private String m_pid;
	private ServiceRegistration m_registration;
    private MetaTypeProviderImpl m_metaType;
	private final AtomicBoolean m_updateInvokedCache = new AtomicBoolean();
	private final Logger m_logger;
	private final BundleContext m_context;

    public ConfigurationDependencyImpl() {
        this(null, null);
    }
	
    public ConfigurationDependencyImpl(BundleContext context, Logger logger) {
        m_context = context;
    	m_logger = logger;
        setRequired(true);
        setCallback("updated");
    }
    
	public ConfigurationDependencyImpl(ConfigurationDependencyImpl prototype) {
	    super(prototype);
	    m_context = prototype.m_context;
	    m_pid = prototype.m_pid;
	    m_logger = prototype.m_logger;
        m_metaType = prototype.m_metaType != null ? new MetaTypeProviderImpl(prototype.m_metaType, this, null) : null;
	}
	
    @Override
    public Class<?> getAutoConfigType() {
        return null; // we don't support auto config mode.
    }

	@Override
	public DependencyContext createCopy() {
	    return new ConfigurationDependencyImpl(this);
	}

    public ConfigurationDependencyImpl setCallback(String callback) {
        super.setCallbacks(callback, null);
        return this;
    }
    
    public ConfigurationDependencyImpl setCallback(Object instance, String callback) {
        super.setCallbacks(instance, callback, null);
        return this;
    }

    @Override
    public boolean needsInstance() {
        return m_callbackInstance == null;
    }

    @Override
    public void start() {
        BundleContext context = m_component.getBundleContext();
        if (context != null) { // If null, we are in a test environment
	        Properties props = new Properties();
	        props.put(Constants.SERVICE_PID, m_pid);
	        ManagedService ms = this;
	        if (m_metaType != null) {
	            ms = m_metaType;
	        }
	        m_registration = context.registerService(ManagedService.class.getName(), ms, props);
        }
        super.start();
    }

    @Override
    public void stop() {
        if (m_registration != null) {
            try {
                m_registration.unregister();
            } catch (IllegalStateException e) {}
        	m_registration = null;
        }
        super.stop();
    }
    
	public ConfigurationDependency setPid(String pid) {
		ensureNotActive();
		m_pid = pid;
		return this;
	}
		
    @Override
    public String getSimpleName() {
        return m_pid;
    }
    
    @Override
    public String getFilter() {
        return null;
    }

    public String getType() {
        return "configuration";
    }
            
    public ConfigurationDependency add(PropertyMetaData properties)
    {
        createMetaTypeImpl();
        m_metaType.add(properties);
        return this;
    }

    public ConfigurationDependency setDescription(String description)
    {
        createMetaTypeImpl();
        m_metaType.setDescription(description);
        return this;
    }

    public ConfigurationDependency setHeading(String heading)
    {
        createMetaTypeImpl();
        m_metaType.setName(heading);
        return this;
    }
    
    public ConfigurationDependency setLocalization(String path)
    {
        createMetaTypeImpl();
        m_metaType.setLocalization(path);
        return this;
    }
    
	@SuppressWarnings("unchecked")
	@Override
	public Dictionary<String, Object> getProperties() {
		if (m_settings == null) {
            throw new IllegalStateException("cannot find configuration");
		}
		return m_settings;
	}
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void updated(Dictionary settings) throws ConfigurationException {
    	m_updateInvokedCache.set(false);
        Dictionary<String, Object> oldSettings = null;
        synchronized (this) {
            oldSettings = m_settings;
        }

        if (oldSettings == null && settings == null) {
            // CM has started but our configuration is not still present in the CM database: ignore
            return;
        }

        // If this is initial settings, or a configuration update, we handle it synchronously.
        // We'll conclude that the dependency is available only if invoking updated did not cause
        // any ConfigurationException.
        Object[] instances = m_component.getInstances();
        if (instances != null) {
            try {
                invokeUpdated(settings);
            } catch (ConfigurationException e) {
                logConfigurationException(e);
                throw e;
            }
        }
        
        // At this point, we have accepted the configuration.
        synchronized (this) {
            m_settings = settings;
        }

        if ((oldSettings == null) && (settings != null)) {
            // Notify the component that our dependency is available.
            m_component.handleEvent(this, EventType.ADDED, new ConfigurationEventImpl(m_pid, settings));
        }
        else if ((oldSettings != null) && (settings != null)) {
            // Notify the component that our dependency has changed.
            m_component.handleEvent(this, EventType.CHANGED, new ConfigurationEventImpl(m_pid, settings));
        }
        else if ((oldSettings != null) && (settings == null)) {
            // Notify the component that our dependency has been removed.
            // Notice that the component will be stopped, and then all required dependencies will be unbound
            // (including our configuration dependency).
            m_component.handleEvent(this, EventType.REMOVED, new ConfigurationEventImpl(m_pid, oldSettings));
        }
    }

    @Override
    public void invokeCallback(EventType type, Event ... event) {
        switch (type) {
        case ADDED:
            try {
                invokeUpdated(m_settings);
            } catch (ConfigurationException e) {
                logConfigurationException(e);
            }
            break;
        case CHANGED:
            // We already did that synchronously, from our updated method
            break;
        case REMOVED:
            // The state machine is stopping us. We have to invoke updated(null).
            // Reset for the next time the state machine calls invokeAdd
            m_updateInvokedCache.set(false);
            break;
        default:
            break;
        }
    }
    
    private void invokeUpdated(Dictionary<?,?> settings) throws ConfigurationException {
    	if (m_updateInvokedCache.compareAndSet(false, true)) {
			Object[] instances = super.getInstances(); // either the callback instance or the component instances
			if (instances != null) {
				for (int i = 0; i < instances.length; i++) {
					try {
						InvocationUtil.invokeCallbackMethod(instances[i],
								m_add, new Class[][] {
										{ Dictionary.class },
										{ Component.class, Dictionary.class },
										{} },
								new Object[][] { 
						            { settings }, 
						            { m_component, settings },
						            {} });
					}

					catch (InvocationTargetException e) {
						// The component has thrown an exception during it's
						// callback invocation.
						if (e.getTargetException() instanceof ConfigurationException) {
							// the callback threw an OSGi
							// ConfigurationException: just re-throw it.
							throw (ConfigurationException) e
									.getTargetException();
						} else {
							// wrap the callback exception into a
							// ConfigurationException.
							throw new ConfigurationException(null,
									"Configuration update failed",
									e.getTargetException());
						}
					} catch (NoSuchMethodException e) {
						// if the method does not exist, ignore it
					} catch (Throwable t) {
						// wrap any other exception as a ConfigurationException.
						throw new ConfigurationException(null,
								"Configuration update failed", t);
					}
				}
			}
    	}
    }
    
    private synchronized void createMetaTypeImpl() {
        if (m_metaType == null) {
            m_metaType = new MetaTypeProviderImpl(m_pid, m_context, m_logger, this, null);
        }
    }
    
    private void logConfigurationException(ConfigurationException e) {
        if (m_logger != null) {
            m_logger.log(Logger.LOG_ERROR, "Got exception while handling configuration update for pid " + m_pid, e);
        }
    }
}
