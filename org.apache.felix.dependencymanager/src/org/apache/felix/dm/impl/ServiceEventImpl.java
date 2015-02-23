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

import org.apache.felix.dm.context.Event;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceEventImpl extends Event {
    /**
     * The service reference on which a service dependency depends on
     */
	private final ServiceReference m_reference; 
		
    /**
     * The bundle context of the bundle which has created the service dependency. If not null, 
     * will be used in close method when ugetting the service reference of the dependency.
     */
	private final BundleContext m_bundleContext;
	
    /**
     * The bundle which has created the service dependency. If not null, will be used to ensure that the bundle is still active before
     * ungetting the service reference of the dependency. (ungetting a service reference on a bundle which is not
     * active triggers an exception, and this may degrade performance, especially when doing some benchmarks).
     */
	private final Bundle m_bundle;
	
	public ServiceEventImpl(ServiceReference reference, Object service) {
	    this(null, null, reference, service);
	}

    public ServiceEventImpl(Bundle bundle, BundleContext bundleContext, ServiceReference reference, Object service) {
	    super(service);
	    m_bundle = bundle;
	    m_bundleContext = bundleContext;
		m_reference = reference;
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
	public ServiceReference getReference() {
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
        if (m_bundleContext != null) {
            try {
                // Optimization: don't call ungetService if the bundle referring to the service is not active. 
                // This optim is important when doing benchmarks where the referring bundle is being stopped 
                // while some dependencies are lost concurrently (here we want to avoid having many exception thrown).
                if (m_bundle == null || m_bundle.getState() == Bundle.ACTIVE) {
                    m_bundleContext.ungetService(m_reference);
                }
            } catch (IllegalStateException e) {}
        }
    }
}