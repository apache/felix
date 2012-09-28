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
import org.osgi.service.useradmin.Role;

/**
 * Provides a factory for creating the various role instances, can be used by external 
 * implementations to create new role instances.
 */
public final class RoleFactory {

    /**
     * Creates a new instance of {@link RoleFactory}, not used.
     */
    private RoleFactory() {
        // Nop
    }

    /**
     * Creates a new role instance.
     * 
     * @param type the type of the role to create;
     * @param name the name of the role to create.
     * @return a new {@link RoleImpl} instance denoting the requested role, never <code>null</code>.
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
}
