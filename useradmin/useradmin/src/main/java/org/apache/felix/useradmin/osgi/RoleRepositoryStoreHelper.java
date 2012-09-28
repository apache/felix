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

import java.io.IOException;

import org.apache.felix.useradmin.RoleRepositoryStore;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.useradmin.Role;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Provides an OSGi service tracker for {@link RoleRepositoryStore}.
 * <p>
 * This helper allows us to use {@link RoleRepositoryStore} without having to 
 * worry about the possible absence of the actual store implementation.
 * </p>
 */
class RoleRepositoryStoreHelper extends ServiceTracker implements RoleRepositoryStore {
    
    /**
     * Creates a new {@link RoleRepositoryStoreHelper} instance.
     * 
     * @param context the bundle context to use, cannot be <code>null</code>.
     */
    public RoleRepositoryStoreHelper(BundleContext context) {
        super(context, RoleRepositoryStore.class.getName(), null /* customizer */);
    }

    public boolean addRole(Role role) throws IOException {
        RoleRepositoryStore store = getStore();
        if (store != null) {
            return store.addRole(role);
        }

        return false;
    }
    
    public synchronized void close() {
        try {
            RoleRepositoryStore store = getStore();
            if (store != null) {
                store.close();
            }
        }
        catch (IOException e) {
            // Ignore; nothing we can do about this here...
        } finally {
            super.close();
        }
    }
    
    public Role[] getAllRoles() throws IOException {
        RoleRepositoryStore store = getStore();
        if (store != null) {
            return store.getAllRoles();
        }

        return new Role[0];
    }

    public Role getRoleByName(String roleName) throws IOException {
        RoleRepositoryStore store = getStore();
        if (store != null) {
            return store.getRoleByName(roleName);
        }

        return null;
    }

    public void initialize() throws IOException {
        RoleRepositoryStore store = getStore();
        if (store != null) {
            store.initialize();
        }
    }

    public void removedService(ServiceReference reference, Object service) {
        RoleRepositoryStore removedStore = (RoleRepositoryStore) service;
        try {
            removedStore.close();
        }
        catch (IOException e) {
            // Ignore; nothing we can do about this here...
        }

        super.removedService(reference, service);
    }

    public boolean removeRole(Role role) throws IOException {
        // and possibly also from our tracked store...
        RoleRepositoryStore store = getStore();
        if (store != null) {
            return store.removeRole(role);
        }

        return false;
    }

    /**
     * Returns the tracked {@link RoleRepositoryStore}.
     * 
     * @return the {@link RoleRepositoryStore}, can be <code>null</code>.
     */
    private RoleRepositoryStore getStore() {
        return (RoleRepositoryStore) getService();
    }
}
