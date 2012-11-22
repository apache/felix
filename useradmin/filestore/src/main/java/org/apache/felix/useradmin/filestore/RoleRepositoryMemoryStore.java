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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.useradmin.RoleFactory;
import org.apache.felix.useradmin.RoleRepositoryStore;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.useradmin.Role;


/**
 * Provides a thread-safe in-memory role repository store.
 */
public class RoleRepositoryMemoryStore implements RoleRepositoryStore {
    
    protected final ConcurrentMap m_entries = new ConcurrentHashMap();

    public Role addRole(String roleName, int type) {
        if (roleName == null) {
            throw new IllegalArgumentException("Name cannot be null!");
        }
        Role role = RoleFactory.createRole(type, roleName);
        Object result = m_entries.putIfAbsent(roleName, role);
        return (result == null) ? role : null;
    }

    public Role[] getRoles(String filterValue) throws InvalidSyntaxException {
        Collection roles = m_entries.values();

        Filter filter = null;
        if (filterValue != null) {
            filter = FrameworkUtil.createFilter(filterValue);
        }

        List matchingRoles = new ArrayList();
        Iterator rolesIter = roles.iterator();
        while (rolesIter.hasNext()) {
            Role role = (Role) rolesIter.next();
            if ((filter == null) || filter.match(role.getProperties())) {
                matchingRoles.add(role);
            }
        }

        Role[] result = new Role[matchingRoles.size()];
        return (Role[]) matchingRoles.toArray(result);
    }

    public Role getRoleByName(String roleName) {
        if (roleName == null) {
            throw new IllegalArgumentException("Role name cannot be null!");
        }
        return (Role) m_entries.get(roleName);
    }

    public Role removeRole(String roleName) {
        if (roleName == null) {
            throw new IllegalArgumentException("Name cannot be null!");
        }
        Role role = getRoleByName(roleName);
        boolean result = m_entries.remove(roleName, role);
        return result ? role : null;
    }
}
