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
import org.osgi.service.useradmin.UserAdmin;

/**
 * Provides a convenience class for all helpers that are used for this
 * implementation of the {@link UserAdmin} service.
 */
final class ServiceContext {

    final EventAdminHelper m_eventAdmin;
    final UserAdminListenerListHelper m_listenerList;
    final EventDispatcher m_eventDispatcher;
    final RoleRepository m_roleRepository;
    final RoleRepositoryStoreHelper m_store;

    /**
     * Creates a new ServiceContext instance.
     */
    public ServiceContext(EventAdminHelper eventAdmin, UserAdminListenerListHelper listenerList, EventDispatcher eventDispatcher, RoleRepository roleRepository, RoleRepositoryStoreHelper store) {
        m_eventAdmin = eventAdmin;
        m_listenerList = listenerList;
        m_eventDispatcher = eventDispatcher;
        m_roleRepository = roleRepository;
        m_store = store;
    }

    /**
     * Starts/opens all helpers.
     */
    public void start() {
        m_eventAdmin.open();
        m_listenerList.open(true /* trackAllServices */);
        m_eventDispatcher.start();
        m_store.open();
    }
    
    /**
     * Stops/closes all helpers.
     */
    public void stop() {
        m_eventDispatcher.stop();
        m_listenerList.close();
        m_eventAdmin.close();
        m_store.close();
    }
}
