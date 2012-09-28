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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.useradmin.BackendException;
import org.apache.felix.useradmin.RoleFactory;
import org.apache.felix.useradmin.RoleRepositoryStore;
import org.apache.felix.useradmin.impl.role.RoleImpl;
import org.osgi.framework.Filter;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.UserAdminPermission;

/**
 * Provides a manager and entry-point for accessing {@link Role}s.
 */
public final class RoleRepository {

    /**
     * Hands off all obtained role change event to a local set of listeners.
     */
    final class RoleChangeReflector implements RoleChangeListener {
        /**
         * {@inheritDoc}
         */
        public void roleAdded(Role role) {
            Iterator iterator = createListenerIterator();
            while (iterator.hasNext()) {
                ((RoleChangeListener) iterator.next()).roleAdded(role);
            }
        }
        
        /**
         * {@inheritDoc}
         */
        public void roleRemoved(Role role) {
            Iterator iterator = createListenerIterator();
            while (iterator.hasNext()) {
                ((RoleChangeListener) iterator.next()).roleRemoved(role);
            }
        }
        
        /**
         * {@inheritDoc}
         */
        public void propertyAdded(Role role, Object key, Object value) {
            Iterator iterator = createListenerIterator();
            while (iterator.hasNext()) {
                ((RoleChangeListener) iterator.next()).propertyAdded(role, key, value);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void propertyRemoved(Role role, Object key) {
            Iterator iterator = createListenerIterator();
            while (iterator.hasNext()) {
                ((RoleChangeListener) iterator.next()).propertyRemoved(role, key);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void propertyChanged(Role role, Object key, Object oldValue, Object newValue) {
            Iterator iterator = createListenerIterator();
            while (iterator.hasNext()) {
                ((RoleChangeListener) iterator.next()).propertyChanged(role, key, oldValue, newValue);
            }
        }
    }

    /** The single predefined role. */
    public static final Role USER_ANYONE = RoleFactory.createRole(Role.ROLE, Role.USER_ANYONE);

    private final RoleRepositoryStore m_store;
    private final CopyOnWriteArrayList m_listeners;
    private final RoleChangeReflector m_roleChangeReflector;
    
    /**
     * Creates a new {@link RoleRepository} instance.
     * 
     * @param store the {@link RoleRepositoryStore} to use, cannot be <code>null</code>.
     */
    public RoleRepository(RoleRepositoryStore store) {
        m_store = store;
        
        m_listeners = new CopyOnWriteArrayList();
        m_roleChangeReflector = new RoleChangeReflector();
    }

    /**
     * Adds a given role to this manager.
     * 
     * @param role the role to add, cannot be <code>null</code>. If it is already contained by this manager, this method will not do anything.
     * @return the given role if added, <code>null</code> otherwise.
     */
    public Role addRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null!");
        }
        if (!(role instanceof RoleImpl)) {
            throw new IllegalArgumentException("Invalid role type!");
        }

        checkPermissions();

        try {
            if (m_store.addRole(role)) {
                m_roleChangeReflector.roleAdded(role);
                return wireChangeListener(role);
            }

            return null;
        }
        catch (IOException e) {
            throw new BackendException("Adding role " + role.getName() + " failed!", e);
        }
    }

    /**
     * Adds the given role change listener to be called for upcoming changes in roles.
     * 
     * @param listener the listener to register, cannot be <code>null</code>.
     * @throws IllegalArgumentException in case the given listener was <code>null</code>.
     */
    public void addRoleChangeListener(RoleChangeListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("RoleChangeListener cannot be null!");
        }

        m_listeners.addIfAbsent(listener);
    }

    /**
     * Returns the by its given name.
     * 
     * @param roleName the name of the role to return, cannot be <code>null</code>.
     * @return the role matching the given name, or <code>null</code> if no role matched the given name.
     */
    public Role getRoleByName(String roleName) {
        try {
            return wireChangeListener(m_store.getRoleByName(roleName));
        }
        catch (IOException e) {
            throw new BackendException("Failed to get role by name: " + roleName + "!", e);
        }
    }

    /**
     * Returns a collection with all roles matching a given filter.
     * 
     * @param filter the filter to match the individual roles against, can be <code>null</code> if all roles should be returned.
     * @return a list with all matching roles, can be empty, but never <code>null</code>.
     */
    public List getRoles(Filter filter) {
        List matchingRoles = new ArrayList();

        try {
            Role[] roles = m_store.getAllRoles();
            for (int i = 0; i < roles.length; i++) {
                Role role = roles[i];
                if (!isPredefinedRole(role) && ((filter == null) || filter.match(role.getProperties()))) {
                    matchingRoles.add(wireChangeListener(role));
                }
            }
        }
        catch (IOException e) {
            throw new BackendException("Failed to get roles!", e);
        }

        return matchingRoles;
    }

    /**
     * Returns a collection with all roles matching a given key-value pair.
     * 
     * @param key the key to search for;
     * @param value the value to search for.
     * @return a list with all matching roles, can be empty, but never <code>null</code>.
     */
    public List getRoles(String key, String value) {
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null!");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null!");
        }

        List matchingRoles = new ArrayList();

        try {
            Role[] roles = m_store.getAllRoles();
            for (int i = 0; i < roles.length; i++) {
                Role role = roles[i];
                Dictionary dict = role.getProperties();
                if (!isPredefinedRole(role) && value.equals(dict.get(key))) {
                    matchingRoles.add(wireChangeListener(role));
                }
            }
        }
        catch (IOException e) {
            throw new BackendException("Failed to get roles!", e);
        }

        return matchingRoles;
    }

    /**
     * Removes a given role from this manager.
     * 
     * @param role the role to remove, cannot be <code>null</code>.
     * @return <code>true</code> if the role was removed (i.e., it was managed by this manager), or <code>false</code> if it was not found.
     */
    public boolean removeRole(Role role) {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null!");
        }
        if (!(role instanceof RoleImpl)) {
            throw new IllegalArgumentException("Invalid role type!");
        }

        checkPermissions();

        // Cannot remove predefined roles...
        if (isPredefinedRole(role)) {
            return false;
        }

        try {
            if (m_store.removeRole(role)) {
                unwireChangeListener(role);
                m_roleChangeReflector.roleRemoved(role);
                
                return true;
            }

            return false;
        }
        catch (IOException e) {
            throw new BackendException("Failed to remove role " + role.getName() + "!", e);
        }
    }

    /**
     * Removes the given role change listener from be called for changes in roles.
     * 
     * @param listener the listener to unregister, cannot be <code>null</code>.
     * @throws IllegalArgumentException in case the given listener was <code>null</code>.
     */
    public void removeRoleChangeListener(RoleChangeListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("RoleChangeListener cannot be null!");
        }

        m_listeners.remove(listener);
    }

    /**
     * Starts this repository.
     */
    public void start() {
        try {
            // The sole predefined role we've got...
            m_store.addRole(USER_ANYONE);

            m_store.initialize();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Stops this repository, allowing it to clean up.
     */
    public void stop() {
        try {
            m_store.close();
        }
        catch (IOException e) {
            // Ignore; nothing we can do about this here...
        }
    }

    /**
     * Creates a new iterator for iterating over all listeners.
     * 
     * @return a new {@link Iterator} instance, never <code>null</code>. 
     */
    final Iterator createListenerIterator() {
        return m_listeners.iterator();
    }

    /**
     * Verifies whether the caller has the right permissions to add or remove roles.
     * 
     * @throws SecurityException in case the caller has not the right permissions to perform the action.
     */
    private void checkPermissions() throws SecurityException {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new UserAdminPermission(UserAdminPermission.ADMIN, null));
        }
    }
    
    /**
     * Returns whether or not the given role is a predefined role.
     * <p>
     * Currently, there's only a single predefined role: {@link Role#USER_ANYONE}.
     * </p>
     * 
     * @param role the role to check, may be <code>null</code>.
     * @return <code>true</code> if the given role is predefined, <code>false</code> otherwise.
     */
    private boolean isPredefinedRole(Role role) {
        return Role.USER_ANYONE.equals(role.getName());
    }

    /**
     * Wires the given role to this repository so it can listen for its changes.
     * 
     * @param role the role to listen for its changes, cannot be <code>null</code>.
     * @return the given role.
     * @throws IllegalArgumentException in case the given object was not a {@link RoleImpl} instance.
     */
    private Role wireChangeListener(Object role) {
        RoleImpl result = (RoleImpl) role;
        if (result != null) {
            result.setRoleChangeListener(m_roleChangeReflector);
        }
        return result;
    }

    /**
     * Unwires the given role to this repository so it no longer listens for its changes.
     * 
     * @param role the role to unwire, cannot be <code>null</code>.
     * @throws IllegalArgumentException in case the given object was not a {@link RoleImpl} instance.
     */
    private void unwireChangeListener(Object role) {
        RoleImpl result = (RoleImpl) role;
        result.setRoleChangeListener(null);
    }
}
