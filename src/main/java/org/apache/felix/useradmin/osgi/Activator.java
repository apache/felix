/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.felix.useradmin.osgi;

import org.apache.felix.useradmin.impl.EventDispatcher;
import org.apache.felix.useradmin.impl.RoleRepository;
import org.apache.felix.useradmin.impl.UserAdminImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.useradmin.UserAdmin;

/**
 * Provides the bundle activator for the UserAdmin service.
 */
public class Activator implements BundleActivator {

    private volatile ServiceContext m_context;

    /**
     * {@inheritDoc}
     */
    public void start(BundleContext context) throws Exception {
        m_context = createServiceContext(context);
        
        // The actual service itself...
        UserAdminImpl service = new UserAdminImpl(m_context.m_roleRepository, m_context.m_eventDispatcher);
        
        // Register the actual service...
        context.registerService(UserAdmin.class.getName(), service, null);
        
        // Start/open all helper classes...
        m_context.start();
    }

    /**
     * {@inheritDoc}
     */
    public void stop(BundleContext context) throws Exception {
        if (m_context != null) {
            // Stop/close all helper classes...
            m_context.stop();
        }
    }
    
    /**
     * Creates a new service context in which all helper classes are kept.
     * 
     * @param context the bundle context to use, cannot be <code>null</code>.
     * @return a new, initialized, ServiceContext instance, never <code>null</code>.
     */
    private ServiceContext createServiceContext(BundleContext context) {
        // Create all services...
        EventAdminHelper eventAdmin = new EventAdminHelper(context);
        UserAdminListenerListHelper listenerList = new UserAdminListenerListHelper(context);
        EventDispatcher eventDispatcher = new EventDispatcher(eventAdmin, listenerList);
        RoleRepositoryStoreHelper store = new RoleRepositoryStoreHelper(context);

        RoleRepository roleRepository = new RoleRepository(store);

        return new ServiceContext(eventAdmin, listenerList, eventDispatcher, roleRepository, store);
    }
}
