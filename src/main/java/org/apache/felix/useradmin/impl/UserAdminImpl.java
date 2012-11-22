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

package org.apache.felix.useradmin.impl;

import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;
import org.osgi.service.useradmin.UserAdminEvent;

/**
 * Provides the implementation for {@link UserAdmin}.
 */
public class UserAdminImpl implements ServiceFactory, UserAdmin, RoleChangeListener {
    
    private final RoleRepository m_roleRepository;
    private final EventDispatcher m_eventDispatcher;

    private volatile ServiceReference m_serviceRef;
    
    /**
     * Creates a new {@link UserAdminImpl} implementation.
     * 
     * @param roleRepository the repository with roles to use for this service;
     * @param eventDispatcher the event dispatcher to use for this service.
     * 
     * @throws IllegalArgumentException in case one of the given parameters was <code>null</code>.
     */
    public UserAdminImpl(RoleRepository roleRepository, EventDispatcher eventDispatcher) {
        if (roleRepository == null) {
            throw new IllegalArgumentException("RoleRepository cannot be null!");
        }
        if (eventDispatcher == null) {
            throw new IllegalArgumentException("EventDispatcher cannot be null!");
        }

        m_roleRepository = roleRepository;
        m_eventDispatcher = eventDispatcher;

        m_roleRepository.addRoleChangeListener(this);
    }

    /**
     * {@inheritDoc}
     */
    public Role createRole(String name, int type) {
        return m_roleRepository.addRole(name, type);
    }

    /**
     * {@inheritDoc}
     */
    public Authorization getAuthorization(User user) {
        return new AuthorizationImpl(user, m_roleRepository);
    }

    /**
     * {@inheritDoc}
     */
    public Role getRole(String name) {
        return m_roleRepository.getRoleByName(name);
    }

    /**
     * {@inheritDoc}
     */
    public Role[] getRoles(String filter) throws InvalidSyntaxException {
        // Do a sanity check on the given filter...
        if (filter != null && !"".equals(filter.trim())) {
            FrameworkUtil.createFilter(filter);
        }

        List roles = m_roleRepository.getRoles(filter);
        if (roles.isEmpty()) {
            return null;
        }
        return (Role[]) roles.toArray(new Role[roles.size()]);
    }
    
    /**
     * {@inheritDoc}
     * 
     * <p>Overridden in order to get hold of our service reference.</p>
     */
    public Object getService(Bundle bundle, ServiceRegistration registration) {
        m_serviceRef = registration.getReference();
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public User getUser(String key, String value) {
        User result = null;
        List roles = m_roleRepository.getRoles(key, value);
        if (roles.size() == 1) {
            Role foundRole = (Role) roles.get(0);
            if (foundRole.getType() == Role.USER) {
                result = (User) foundRole;
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public void propertyAdded(Role role, Object key, Object value) {
        m_eventDispatcher.dispatch(createUserAdminEvent(UserAdminEvent.ROLE_CHANGED, role));
    }
    
    /**
     * {@inheritDoc}
     */
    public void propertyChanged(Role role, Object key, Object oldValue, Object newValue) {
        m_eventDispatcher.dispatch(createUserAdminEvent(UserAdminEvent.ROLE_CHANGED, role));
    }

    /**
     * {@inheritDoc}
     */
    public void propertyRemoved(Role role, Object key) {
        m_eventDispatcher.dispatch(createUserAdminEvent(UserAdminEvent.ROLE_CHANGED, role));
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean removeRole(String name) {
        return m_roleRepository.removeRole(name);
    }

    /**
     * {@inheritDoc}
     */
    public void roleAdded(Role role) {
        m_eventDispatcher.dispatch(createUserAdminEvent(UserAdminEvent.ROLE_CREATED, role));
    }

    /**
     * {@inheritDoc}
     */
    public void roleRemoved(Role role) {
        m_eventDispatcher.dispatch(createUserAdminEvent(UserAdminEvent.ROLE_REMOVED, role));
    }

    /**
     * {@inheritDoc}
     */
    public void ungetService(Bundle bundle, ServiceRegistration registration, Object service) {
        // Nop; we leave the service as-is...
    }

    /**
     * Creates a new {@link UserAdminEvent} instance for the given type and role.
     * 
     * @param type the type of event to create;
     * @param role the role to create the event for.
     * @return a new {@link UserAdminEvent} instance, never <code>null</code>.
     */
    private UserAdminEvent createUserAdminEvent(int type, Role role) {
        return new UserAdminEvent(m_serviceRef, type, role);
    }
}
