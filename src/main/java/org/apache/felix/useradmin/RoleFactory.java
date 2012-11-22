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
package org.apache.felix.useradmin;


import org.apache.felix.useradmin.impl.role.GroupImpl;
import org.apache.felix.useradmin.impl.role.RoleImpl;
import org.apache.felix.useradmin.impl.role.UserImpl;
import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;
import org.osgi.service.useradmin.User;

/**
 * Provides a factory for creating the various role instances, can be used by external 
 * implementations to create new role instances.
 */
public final class RoleFactory {

    /**
     * Creates a new group-role instance.
     * 
     * @param name the name of the group to create.
     * @return a new {@link Group} instance denoting the requested role, never <code>null</code>.
     */
    public static Group createGroup(String name) {
        return (Group) createRole(Role.GROUP, name);
    }

    /**
     * Creates a new role instance with the given type and name.
     * 
     * @param type the type of the role to create;
     * @param name the name of the role to create.
     * @return a new {@link Role} instance denoting the requested role, never <code>null</code>.
     */
    public static Role createRole(int type, String name) {
        if (type == Role.USER) {
            UserImpl result = new UserImpl(name);
            return result;
        } else if (type == Role.GROUP) {
            GroupImpl result = new GroupImpl(name);
            return result;
        } else {
            RoleImpl result = new RoleImpl(name);
            return result;
        }
    }

    /**
     * Creates a new role instance.
     * 
     * @param type the type of the role to create;
     * @param name the name of the role to create.
     * @return a new {@link Role} instance denoting the requested role, never <code>null</code>.
     */
    public static Role createRole(String name) {
        return createRole(Role.ROLE, name);
    }

    /**
     * Creates a new user-role instance.
     * 
     * @param name the name of the user to create.
     * @return a new {@link User} instance denoting the requested role, never <code>null</code>.
     */
    public static User createUser(String name) {
        return (User) createRole(Role.USER, name);
    }

    /**
     * Creates a new instance of {@link RoleFactory}, not used.
     */
    private RoleFactory() {
        // Nop
    }
}
