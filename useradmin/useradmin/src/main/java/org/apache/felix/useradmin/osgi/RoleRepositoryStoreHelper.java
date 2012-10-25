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
import java.util.concurrent.atomic.AtomicBoolean;

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
	
	private final AtomicBoolean m_initialized = new AtomicBoolean(false);
    
    /**
     * Creates a new {@link RoleRepositoryStoreHelper} instance.
     * 
     * @param context the bundle context to use, cannot be <code>null</code>.
     */
    public RoleRepositoryStoreHelper(BundleContext context) {
        super(context, RoleRepositoryStore.class.getName(), null /* customizer */);
    }

    public Object addingService(ServiceReference reference) {
    	// FELIX-3735: store can also become available *after* this bundle is started...
    	RoleRepositoryStore store = (RoleRepositoryStore) super.addingService(reference);
    	try {
    		initializeStore(store);
		} catch (IOException e) {
            // Ignore; nothing we can do about this here...
		}
    	return store;
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
                closeStore(store);
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
        	initializeStore(store);
        }
    }

    public void removedService(ServiceReference reference, Object service) {
        RoleRepositoryStore removedStore = (RoleRepositoryStore) service;
        try {
        	closeStore(removedStore);
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

	/**
	 * Closes the given store.
	 * 
	 * @param store the store to close, cannot be <code>null</code>.
	 * @throws IOException in case initialization failed.
	 */
	private void closeStore(RoleRepositoryStore store) throws IOException {
		// Only close the store if its initialized...
		boolean initialized = m_initialized.get();
		if (initialized) {
			store.close();

			do {
				initialized = m_initialized.get();
			} while (!m_initialized.compareAndSet(initialized, false));
		}
	}

	/**
	 * Initializes the given store.
	 * 
	 * @param store the store to initialize, cannot be <code>null</code>.
	 * @throws IOException in case initialization failed.
	 */
	private void initializeStore(RoleRepositoryStore store) throws IOException {
		// FELIX-3735: store can also become available *after* this bundle is started; 
		// hence we need to ensure we do not initialize the store twice...
		boolean initialized = m_initialized.get();
		if (!initialized) {
			store.initialize();

			do {
				initialized = m_initialized.get();
			} while (!m_initialized.compareAndSet(initialized, true));
		}
	}
}
