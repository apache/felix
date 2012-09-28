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

import java.io.Closeable;
import java.io.IOException;

import org.osgi.service.useradmin.Role;

/**
 * Provides an abstraction to store and retrieve a role repository.
 */
public interface RoleRepositoryStore extends Closeable {
    
    /**
     * Adds a given role to this backend.
     * <p>
     * If the given role is already contained by this backed, this method 
     * should not do anything and return <code>false</code> to denote this.
     * </p>
     * 
     * @param role the role to add, cannot be <code>null</code>.
     * @return <code>true</code> if the role was successfully added, <code>false</code> otherwise.
     * @throws IllegalArgumentException in case the given argument was <code>null</code>;
     * @throws IOException in case of I/O problems.
     */
    boolean addRole(Role role) throws IOException;

    /**
     * Closes this store, allowing implementations to free up resources, close
     * connections, and so on.
     * 
     * @throws IOException in case of I/O problems.
     */
    void close() throws IOException;

    /**
     * Returns all available roles in this backend.
     * 
     * @return an array with all roles, never <code>null</code>, but can be empty.
     * @throws IOException in case of I/O problems.
     */
    Role[] getAllRoles() throws IOException;
    
    /**
     * Returns a {@link Role} by its name.
     * 
     * @param roleName the name of the role to return, cannot be <code>null</code>.
     * @return the role with the given name, or <code>null</code> if no such role exists.
     * @throws IllegalArgumentException in case the given argument was <code>null</code>;
     * @throws IOException in case of I/O problems.
     */
    Role getRoleByName(String roleName) throws IOException;

    /**
     * Called once before any other method of this interface is being called.
     * <p>
     * Implementations can use this method to create a connection to the 
     * backend, or load the initial set of roles, and so on.
     * </p>
     * 
     * @throws IOException in case of I/O problems.
     */
    void initialize() throws IOException;

    /**
     * Removes a given role from this backend.
     * <p>
     * If the given role is not contained by this backed, this method 
     * should not do anything and return <code>false</code> to denote this.
     * </p>
     * 
     * @param role the role to remove, cannot be <code>null</code>.
     * @return <code>true</code> if the role was successfully removed, <code>false</code> otherwise.
     * @throws IllegalArgumentException in case the given argument was <code>null</code>;
     * @throws IOException in case of I/O problems.
     */
    boolean removeRole(Role role) throws IOException;
    
}
