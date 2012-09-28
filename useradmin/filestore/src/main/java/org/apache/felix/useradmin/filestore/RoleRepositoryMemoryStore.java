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
package org.apache.felix.useradmin.filestore;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.useradmin.RoleRepositoryStore;
import org.osgi.service.useradmin.Role;


/**
 * Provides a thread-safe in-memory role repository store.
 */
public class RoleRepositoryMemoryStore implements RoleRepositoryStore {
    
    protected final ConcurrentMap m_entries = new ConcurrentHashMap();

    public boolean addRole(Role role) throws IOException {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null!");
        }
        Object result = m_entries.putIfAbsent(role.getName(), role);
        return result == null;
    }

    public void close() throws IOException {
        // Nop
    }

    public Role[] getAllRoles() throws IOException {
        Collection roles = m_entries.values();
        Role[] result = new Role[roles.size()];
        return (Role[]) roles.toArray(result);
    }

    public Role getRoleByName(String roleName) throws IOException {
        if (roleName == null) {
            throw new IllegalArgumentException("Role name cannot be null!");
        }
        return (Role) m_entries.get(roleName);
    }
    
    public void initialize() throws IOException {
        // Nop
    }

    public boolean removeRole(Role role) throws IOException {
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null!");
        }
        return m_entries.remove(role.getName(), role);
    }
}
