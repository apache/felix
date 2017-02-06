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

import java.util.Dictionary;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.apache.felix.dm.Logger;
import org.apache.felix.dm.context.ComponentContext;
import org.apache.felix.dm.context.Event;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * An event for a service dependency.
 * Not thread safe, but this class is assumed to be used under the protection of the component serial queue.
 */
public class ServiceEventImpl extends Event {
    /**
     * The service reference on which a service dependency depends on
     */
	private final ServiceReference<?> m_reference; 
	
    /**
     * The bundle context of the bundle which has created the service dependency. If not null, 
     * will be used in close method when ugetting the service reference of the dependency.
     */
	private final BundleContext m_bundleContext;
	
    /**
     * The bundle which has created the service dependency.
     */
	private final Bundle m_bundle;
	
	/**
	 * Protects in case close is called twice.
	 */
	private final AtomicBoolean m_closed = new AtomicBoolean(false); 
	
	/**
	 * Our logger.
	 */
	private final Logger m_logger;

	/**
	 * The actual service.
	 */
	private volatile Object m_service;
	
    public ServiceEventImpl(ComponentContext ctx, ServiceReference<?> reference, Object service) {
	    super(service);
		m_service = service;
	    m_bundle = ctx.getBundle();
	    m_bundleContext = ctx.getBundleContext();
		m_reference = reference;
		m_logger = ctx.getLogger();
    }

    /**
     * Returns the actual service, or null if the service reference is invalid.
     * @return the service or null if the service is not available anymore.
     */
    @SuppressWarnings("unchecked")
	@Override
    public <T> T getEvent() {
        if (m_service == null) {
        	try {
    		    m_service = m_bundleContext.getService(m_reference);
    		    if (m_service == null) {
            		debug(() -> "Service " + m_reference + " unavailable");
    		    }
        	} catch (Exception t) {
        		error(() -> "Could not get service from service reference " + m_reference, t);
        	}
        }
        return (T) m_service;
    }

	/**
	 * Returns the bundle which has declared a service dependency.
	 */
	public Bundle getBundle() {
	    return m_bundle;
	}
	
    /**
     * Returns the context of the bundle which has declared a service dependency.
     */
	public BundleContext getBundleContext() {
	    return m_bundleContext;
	}

	/**
	 * Returns the reference service dependency.
	 */
	public ServiceReference<?> getReference() {
		return m_reference;
	}
		    
    @SuppressWarnings("unchecked")
	@Override
    public Dictionary<String, Object> getProperties() {
        return ServiceUtil.propertiesToDictionary(m_reference);
    }
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ServiceEventImpl) {
			return getReference().equals(((ServiceEventImpl) obj).getReference());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return getReference().hashCode();
	}

    @Override
    public int compareTo(Event b) {
    	return getReference().compareTo(((ServiceEventImpl) b).getReference());
    }
        
    @Override
    public String toString() {
    	return getEvent().toString();
    }

    @Override
    public void close() {
        if (m_closed.compareAndSet(false, true)) {
        	if (m_service != null) {
        		try {
            		m_bundleContext.ungetService(m_reference);
        		} catch (IllegalStateException e) {}
        	}
        }
    }
        
    private void error(Supplier<String> msg, Exception err) {
    	if (m_logger != null) {
    		m_logger.err("%s", err, msg.get());
    	}
    }
    
    private void debug(Supplier<String> msg) {
    	if (m_logger != null) {
    		m_logger.debug("%s", msg.get());
    	}
    }
}