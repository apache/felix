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

import org.osgi.service.useradmin.Role;

/**
 * Provides an abstraction to store and retrieve a role repository.
 * <p>
 * The role object returned by this backend <em>can</em> be a different
 * implementation than <tt>org.apache.felix.useradmin.impl.role.RoleImpl</tt>,
 * in which case the backend is made fully responsible for keeping track of the
 * changes in the role object and persisting them!
 * </p>
 */
public interface RoleRepositoryStore {

    /**
     * Adds a given role to this backend.
     * <p>
     * If the given role is already contained by this backed, this method 
     * should not do anything and return <code>null</code> to denote this.
     * </p>
     * 
     * @param roleName the name of the role to add, cannot be <code>null</code>;
     * @param type the type of role to add, either {@link Role#USER} or {@link Role#GROUP}.
     * @return the role added, or <code>null</code> in case the role already 
     *         exists.
     * @throws IllegalArgumentException in case the given name was <code>null</code>;
     * @throws Exception in case of problems adding the role.
     */
    Role addRole(String roleName, int type) throws Exception;

    /**
     * Returns all roles in this backend matching the given filter criteria.
     * 
     * @param filter the (optional) filter to apply, can be <code>null</code> in which case all roles will be returned.
     * @return an array with all roles, never <code>null</code>, but can be empty.
     * @throws Exception in case of problems retrieving the set of roles.
     */
    Role[] getRoles(String filter) throws Exception;

    /**
     * Returns a {@link Role} by its name.
     * 
     * @param roleName the name of the role to return, cannot be <code>null</code>.
     * @return the role with the given name, or <code>null</code> if no such role exists.
     * @throws IllegalArgumentException in case the given argument was <code>null</code>;
     * @throws Exception in case of problems retrieving the requested role.
     */
    Role getRoleByName(String roleName) throws Exception;

    /**
     * Removes a given role from this backend.
     * <p>
     * If the given role is not contained by this backed, this method 
     * should not do anything and return <code>null</code> to denote this.
     * </p>
     * 
     * @param roleName the name of the role to remove, cannot be <code>null</code>.
     * @return the removed role, if it was successfully removed from this 
     *         backend, or <code>null</code> if the role was not contained by
     *         this backend or could not be removed.
     * @throws IllegalArgumentException in case the given argument was <code>null</code>;
     * @throws Exception in case of problems removing the requested role.
     */
    Role removeRole(String roleName) throws Exception;
}
