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

package org.apache.felix.ipojo.extender.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.Dictionary;

/**
 * Common code wrapping an OSGi service.
 */
public abstract class AbstractService implements Lifecycle {

    /**
     * The bundle context.
     * To let sub-classes retrieve the used bundle context, this member is {@literal protected}.
     */
    protected final BundleContext m_bundleContext;
    /**
     * The service specification.
     */
    private final Class<?> m_type;
    /**
     * The service registration.
     */
    private ServiceRegistration<?> m_registration;


    /**
     * Constructor.
     * This constructor checks that the current class and the service specification are compatible.
     *
     * @param bundleContext the bundle context
     * @param type          the specification
     */
    protected AbstractService(BundleContext bundleContext, Class<?> type) {
        m_bundleContext = bundleContext;
        if (!type.isAssignableFrom(getClass())) {
            throw new IllegalArgumentException("This object is not an instance of " + type.getName());
        }
        m_type = type;
    }

    /**
     * On start, registers the service.
     */
    public void start() {
        m_registration = registerService();
    }

    protected ServiceRegistration<?> registerService() {
        return m_bundleContext.registerService(m_type.getName(), this, getServiceProperties());
    }

    /**
     * On stop, un-registers the service.
     */
    public void stop() {
        if (m_registration != null) {
            m_registration.unregister();
            m_registration = null;
        }
    }

    /**
     * @return the service properties, {@literal null} by default.
     */
    protected Dictionary<String, ?> getServiceProperties() {
        return null;
    }

    protected BundleContext getBundleContext() {
        return m_bundleContext;
    }

    protected ServiceRegistration<?> getRegistration() {
        return m_registration;
    }

    /**
     * Is this service registered or not ?
     */
    public boolean isRegistered() {
        return m_registration != null;
    }
}
