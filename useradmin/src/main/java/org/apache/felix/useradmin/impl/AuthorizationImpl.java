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

import org.osgi.service.useradmin.Authorization;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/**
 * Provides an implementation for {@link Authorization}.
 */
public class AuthorizationImpl implements Authorization {

    private final String m_name;
    private final User m_user;
    private final RoleRepository m_roleManager;
    private final RoleChecker m_roleChecker;

    /**
     * Creates a new {@link AuthorizationImpl} instance for the given {@link User}.
     * 
     * @param roleManager the role manager to use for obtaining the roles, cannot be <code>null</code>.
     */
    public AuthorizationImpl(RoleRepository roleManager) {
        this(null, roleManager);
    }

    /**
     * Creates a new {@link AuthorizationImpl} instance for the given {@link User}.
     * 
     * @param user the {@link User} to authorize, may be <code>null</code> for the anonymous user;
     * @param roleManager the role manager to use for obtaining the roles, cannot be <code>null</code>.
     */
    public AuthorizationImpl(User user, RoleRepository roleManager) {
        m_user = user;
        m_roleManager = roleManager;
        m_name = (user != null) ? user.getName() : null;
        m_roleChecker = new RoleChecker();
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return m_name;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasRole(String name) {
        Role role = m_roleManager.getRoleByName(name);
        if (role == null) {
            // No role found, so it is never implied...
            return false;
        }
        return m_roleChecker.isImpliedBy(role, m_user);
    }

    /**
     * {@inheritDoc}
     */
    public String[] getRoles() {
        List result = new ArrayList();

        Iterator rolesIter = m_roleManager.getRoles(null /* filter */).iterator();
        while (rolesIter.hasNext()) {
            Role role = (Role) rolesIter.next();
            if (!Role.USER_ANYONE.equals(role.getName()) && m_roleChecker.isImpliedBy(role, m_user)) {
                result.add(role.getName());
            }
        }

        return result.isEmpty() ? null : (String[]) result.toArray(new String[result.size()]);
    }
}
