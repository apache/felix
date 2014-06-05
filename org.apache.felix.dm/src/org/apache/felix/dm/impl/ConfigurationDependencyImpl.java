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

import org.apache.felix.dm.ConfigurationDependency;
import org.apache.felix.dm.PropertyMetaData;
import org.apache.felix.dm.context.DependencyContext;
import org.apache.felix.dm.context.Event;
import org.apache.felix.dm.impl.metatype.MetaTypeProviderImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

public class ConfigurationDependencyImpl extends DependencyImpl<ConfigurationDependency> implements ConfigurationDependency, ManagedService {
    private Dictionary<?,?> m_settings;
    private String m_callback = "updated";
	private final Logger m_logger;
	private String m_pid;
	private ServiceRegistration m_registration;
    private MetaTypeProviderImpl m_metaType;
	private final AtomicBoolean m_updateInvokedCache = new AtomicBoolean();

    public ConfigurationDependencyImpl() {
    	this(null, null);
    }
    
    public ConfigurationDependencyImpl(BundleContext context, Logger logger) {
    	super(false /* not autoconfig */, context);
    	m_logger = logger;
        setRequired(true);
    }
    
	public ConfigurationDependencyImpl(ConfigurationDependencyImpl prototype) {
	    super(prototype);
	    m_pid = prototype.m_pid;
	    m_callback = prototype.m_callback;
	    m_logger = prototype.m_logger;
	}
	
	@Override
	public DependencyContext createCopy() {
	    return new ConfigurationDependencyImpl(this);
	}

    public ConfigurationDependencyImpl setCallback(String callback) {
        m_callback = callback;
        return this;
    }

    @Override
    public boolean needsInstance() {
        return true;
    }

    @Override
    public void start() {
        super.start();
        if (m_context != null) { // If null, we are in a test environment
	        Properties props = new Properties();
	        props.put(Constants.SERVICE_PID, m_pid);
	        ManagedService ms = this;
	        if (m_metaType != null) {
	            ms = m_metaType;
	        }
	        m_registration = m_context.registerService(ManagedService.class.getName(), ms, props);
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (m_registration != null) {
        	m_registration.unregister();
        	m_registration = null;
        }
    }
    
	public ConfigurationDependency setPid(String pid) {
		// ensureNotActive(); TODO
		m_pid = pid;
		return this;
	}
		
    public String toString() {
    	return "ConfigurationDependency[" + m_pid + "]";
    }
    
    public String getName() {
        return m_pid;
    }
    
    public String getType() {
        return "configuration";
    }

    @Override
    protected Object getService() {
        return m_settings;
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
    
	@Override
	public Dictionary getProperties() {
		if (m_settings == null) {
            throw new IllegalStateException("cannot find configuration");
		}
		return m_settings;
	}
    
    @Override
    public void updated(Dictionary settings) throws ConfigurationException {
    	m_updateInvokedCache.set(false);
        Dictionary<?,?> oldSettings = null;
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
        if (settings != null) {
            Object[] instances = m_component.getInstances();
            if (instances != null) {
                invokeUpdated(settings);
            }
        }
        
        // At this point, we have accepted the configuration.
        synchronized (this) {
            m_settings = settings;
        }

        if ((oldSettings == null) && (settings != null)) {
            // Notify the component that our dependency is available.
            add(new EventImpl());
        }
        else if ((oldSettings != null) && (settings != null)) {
            // Notify the component that our dependency has changed.
            change(new EventImpl());
        }
        else if ((oldSettings != null) && (settings == null)) {
            // Notify the component that our dependency has been removed.
            // Notice that the component will be stopped, and then all required dependencies will be unbound
            // (including our configuration dependency).
            remove(new EventImpl());
        }
    }

    @Override
    public void invokeAdd(Event event) {
		try {
			invokeUpdated(m_settings);
		} catch (ConfigurationException e) {
			e.printStackTrace(); // FIXME use a LogService
		}
    }

    @Override
    public void invokeChange(Event event) {
        // We already did that synchronously, from our updated method
    }

    @Override
    public void invokeRemove(Event event) {
        // The state machine is stopping us. We have to invoke updated(null).
        try {
        	m_updateInvokedCache.set(false);
            invokeUpdated(null);
        } catch (ConfigurationException e) {
            e.printStackTrace(); // FIXME use a LogService
        } finally {
        	// Reset for the next time the state machine calls invokeAdd
        	m_updateInvokedCache.set(false);
        }
    }
    
    private void invokeUpdated(Dictionary<?,?> settings) throws ConfigurationException {
    	if (m_updateInvokedCache.compareAndSet(false, true)) {
			Object[] instances = m_component.getInstances();
			if (instances != null) {
				for (int i = 0; i < instances.length; i++) {
					try {
						InvocationUtil.invokeCallbackMethod(instances[i],
								m_callback, new Class[][] {
										{ Dictionary.class }, {} },
								new Object[][] { { settings }, {} });
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
            m_metaType = new MetaTypeProviderImpl(getName(), m_context, m_logger, this, null);
        }
    }
}
