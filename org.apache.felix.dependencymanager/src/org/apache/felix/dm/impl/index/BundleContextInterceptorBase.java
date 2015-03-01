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
package org.apache.felix.dm.impl.index;

import java.io.File;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Base class for bundle context interceptors that keep track of service listeners and delegate incoming changes to them.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class BundleContextInterceptorBase implements BundleContext, ServiceListener {
    protected final BundleContext m_context;
    /** Keeps track of all service listeners and their optional filters. */
	private final Map<ServiceListener, String> m_serviceListenerFilterMap = new HashMap<>();
    private long m_currentVersion = 0;
    private long m_entryVersion = -1;
	private Entry<ServiceListener, String>[] m_serviceListenerFilterMapEntries;

    public BundleContextInterceptorBase(BundleContext context) {
        m_context = context;
    }

    public String getProperty(String key) {
        return m_context.getProperty(key);
    }

    public Bundle getBundle() {
        return m_context.getBundle();
    }

    public Bundle installBundle(String location) throws BundleException {
        return m_context.installBundle(location);
    }

    public Bundle installBundle(String location, InputStream input) throws BundleException {
        return m_context.installBundle(location, input);
    }

    public Bundle getBundle(long id) {
        return m_context.getBundle(id);
    }

    public Bundle[] getBundles() {
        return m_context.getBundles();
    }

	public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
        synchronized (m_serviceListenerFilterMap) {
            m_serviceListenerFilterMap.put(listener, filter);
            m_currentVersion++;
        }
    }

	public void addServiceListener(ServiceListener listener) {
        synchronized (m_serviceListenerFilterMap) {
            m_serviceListenerFilterMap.put(listener, null);
            m_currentVersion++;
        }
    }

    public void removeServiceListener(ServiceListener listener) {
        synchronized (m_serviceListenerFilterMap) {
            m_serviceListenerFilterMap.remove(listener);
            m_currentVersion++;
        }
    }

    public void addBundleListener(BundleListener listener) {
        m_context.addBundleListener(listener);
    }

    public void removeBundleListener(BundleListener listener) {
        m_context.removeBundleListener(listener);
    }

    public void addFrameworkListener(FrameworkListener listener) {
        m_context.addFrameworkListener(listener);
    }

    public void removeFrameworkListener(FrameworkListener listener) {
        m_context.removeFrameworkListener(listener);
    }

    public ServiceRegistration registerService(String[] clazzes, Object service, @SuppressWarnings("rawtypes") Dictionary properties) {
        return m_context.registerService(clazzes, service, properties);
    }

    public ServiceRegistration registerService(String clazz, Object service, @SuppressWarnings("rawtypes") Dictionary properties) {
        return m_context.registerService(clazz, service, properties);
    }

    public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        return m_context.getServiceReferences(clazz, filter);
    }

    public ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        return m_context.getAllServiceReferences(clazz, filter);
    }

    public ServiceReference getServiceReference(String clazz) {
        return m_context.getServiceReference(clazz);
    }

    public Object getService(ServiceReference reference) {
        return m_context.getService(reference);
    }

    public boolean ungetService(ServiceReference reference) {
        return m_context.ungetService(reference);
    }

    public File getDataFile(String filename) {
        return m_context.getDataFile(filename);
    }

    public Filter createFilter(String filter) throws InvalidSyntaxException {
        return m_context.createFilter(filter);
    }

	@SuppressWarnings("unchecked")
	protected Entry<ServiceListener, String>[] synchronizeCollection() {
        // lazy copy on write: we make a new copy only if writes have changed the collection
        synchronized (m_serviceListenerFilterMap) {
            if (m_currentVersion != m_entryVersion) {
                m_serviceListenerFilterMapEntries = (Entry<ServiceListener, String>[]) m_serviceListenerFilterMap.entrySet().toArray(new Entry[m_serviceListenerFilterMap.size()]);
                m_entryVersion = m_currentVersion;
            }
        }
        return m_serviceListenerFilterMapEntries;
    }
}
