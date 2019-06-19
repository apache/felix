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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.felix.useradmin.BackendException;
import org.apache.felix.useradmin.RoleFactory;
import org.apache.felix.useradmin.RoleRepositoryStore;
import org.apache.felix.useradmin.impl.role.ObservableRole;
import org.osgi.service.useradmin.Group;
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
        public void propertyAdded(Role role, Object key, Object value) {
            Iterator iterator = createListenerIterator();
            while (iterator.hasNext()) {
                ((RoleChangeListener) iterator.next()).propertyAdded(role, key, value);
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
    }

    /** The single predefined role. */
    private static final Role USER_ANYONE = RoleFactory.createRole(Role.USER_ANYONE);

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
    public Role addRole(String name, int type) {
        if ((name == null) || "".equals(name.trim())) {
            throw new IllegalArgumentException("Name cannot be null or empty!");
        }
        if (type != Role.GROUP && type != Role.USER) {
            throw new IllegalArgumentException("Invalid role type!");
        }

        checkPermissions();

        try {
            Role result = m_store.addRole(name, type);
            if (result != null) {
                result = wireChangeListener(result);
                m_roleChangeReflector.roleAdded(result);
            }

            return result;
        }
        catch (Exception e) {
            throw new BackendException("Adding role " + name + " failed!", e);
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
            Role result;
            if (isPredefinedRole(roleName)) {
                result = getPredefinedRole(roleName);
            } else {
                result = m_store.getRoleByName(roleName);
            }
            return wireChangeListener(result);
        }
        catch (Exception e) {
            throw new BackendException("Failed to get role by name: " + roleName + "!", e);
        }
    }

    /**
     * Returns a collection with all roles matching a given filter.
     * 
     * @param filter the filter to match the individual roles against, can be <code>null</code> if all roles should be returned.
     * @return a list with all matching roles, can be empty, but never <code>null</code>.
     */
    public List getRoles(String filter) {
        List matchingRoles = new ArrayList();

        try {
            Role[] roles = m_store.getRoles(sanitizeFilter(filter));
            for (int i = 0; i < roles.length; i++) {
                Role role = roles[i];
                if (!isPredefinedRole(role.getName())) {
                    matchingRoles.add(wireChangeListener(role));
                }
            }
        }
        catch (Exception e) {
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
            String criteria = "(".concat(key).concat("=").concat(value).concat(")");

            Role[] roles = m_store.getRoles(criteria);
            for (int i = 0; i < roles.length; i++) {
                Role role = roles[i];
                if (!isPredefinedRole(role.getName())) {
                    matchingRoles.add(wireChangeListener(role));
                }
            }
        }
        catch (Exception e) {
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
    public boolean removeRole(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null!");
        }

        checkPermissions();

        // Cannot remove predefined roles...
        if (isPredefinedRole(name)) {
            return false;
        }

        try {
            Role result = m_store.removeRole(name);
            if (result !=  null) {
            	// FELIX-3755: Remove the role as (required)member from all groups...
            	removeRoleFromAllGroups(result);
            	
                unwireChangeListener(result);
                m_roleChangeReflector.roleRemoved(result);
                
                return true;
            }

            return false;
        }
        catch (Exception e) {
            throw new BackendException("Failed to remove role " + name + "!", e);
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
     * @param roleName the role name to check, may be <code>null</code>.
     * @return <code>true</code> if the given role is predefined, <code>false</code> otherwise.
     */
    private boolean isPredefinedRole(String roleName) {
        return Role.USER_ANYONE.equals(roleName);
    }

    /**
     * Returns the predefined role with the given name.
     * 
     * @param roleName the name of the predefined role to return, cannot be <code>null</code>.
     * @return a predefined role instance, never <code>null</code>.
     * @see #isPredefinedRole(String)
     */
    private Role getPredefinedRole(String roleName) {
        return USER_ANYONE;
    }
    
    /**
     * Removes a given role as (required)member from any groups it is member of.
     * 
	 * @param removedRole the role that is removed from the store already, cannot be <code>null</code>.
	 * @throws BackendException in case of problems accessing the store.
	 */
	private void removeRoleFromAllGroups(Role removedRole) {
        try {
            Role[] roles = m_store.getRoles(null);
            for (int i = 0; i < roles.length; i++) {
                if (roles[i].getType() == Role.GROUP) {
                	Group group = (Group) roles[i];
                	// Don't check whether the given role is actually a member 
                	// of the group, but let the group itself figure this out...
                	group.removeMember(removedRole);
                }
            }
        }
        catch (Exception e) {
            throw new BackendException("Failed to get all roles!", e);
        }
	}
	
	/**
	 * Sanitizes the given filter string.
	 * 
	 * @param filter the filter string to sanitize, can be <code>null</code>.
	 * @return the sanitized filter, or <code>null</code> if the given filter was <code>null</code> or empty.
	 */
	private String sanitizeFilter(String filter) {
	    if (filter == null || "".equals(filter.trim())) {
	        return null;
	    }
	    return filter.trim();
	}

    /**
     * Unwires the given role to this repository so it no longer listens for its changes.
     * 
     * @param role the role to unwire, cannot be <code>null</code>.
     */
    private void unwireChangeListener(Object role) {
        if (role instanceof ObservableRole) {
            ((ObservableRole) role).setRoleChangeListener(null);
        }
    }

    /**
     * Wires the given role to this repository so it can listen for its changes.
     * 
     * @param role the role to listen for its changes, cannot be <code>null</code>.
     * @return the given role.
     */
    private Role wireChangeListener(Role role) {
        Role result = ObservableRole.wrap(role);
        if (result instanceof ObservableRole) {
            // Keep track of all changes made to the given role, to fire the 
            // proper events to everyone interested...
            ((ObservableRole) result).setRoleChangeListener(m_roleChangeReflector);
        }
        return result;
    }
}
