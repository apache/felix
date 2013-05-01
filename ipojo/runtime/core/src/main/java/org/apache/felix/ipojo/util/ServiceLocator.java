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
package org.apache.felix.ipojo.util;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * A simple utility class to retrive services from the service registry.
 */
public class ServiceLocator<T> {

    private final BundleContext m_context;
    private final Class<T> m_clazz;

    private ServiceReference m_reference;
    private T m_service;

    public ServiceLocator(Class<T> clazz, BundleContext context) {
        m_clazz = clazz;
        m_context = context;
    }

    public synchronized T get() {
         if (m_service != null) {
                return m_service;
         }

        // We can't use the generic version, as KF does not support it yet.
        m_reference = m_context.getServiceReference(m_clazz.getName());
        if (m_reference == null) {
            return null;
        }

        m_service = (T) m_context.getService(m_reference);

        return m_service;
    }

    public synchronized void unget() {
        m_service = null;
        if (m_reference != null) {
            m_context.ungetService(m_reference);
            m_reference = null;
        }
    }
}
