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

import org.osgi.service.useradmin.Role;

/**
 * Provides a callback for listening to role changes.
 */
public interface RoleChangeListener {

    /**
     * Called when a new role is added.
     * 
     * @param role the role that is added.
     */
    void roleAdded(Role role);
    
    /**
     * Called when a role is removed.
     * 
     * @param role the role that is removed.
     */
    void roleRemoved(Role role);
    
    /**
     * Called when a new property-entry is added to a role.
     * 
     * @param role the role that changed;
     * @param key the key of the entry;
     * @param value the value associated to the key.
     */
    void propertyAdded(Role role, Object key, Object value);

    /**
     * Called when an property-entry is removed from a role.
     * 
     * @param role the role that changed;
     * @param key the key of the entry.
     */
    void propertyRemoved(Role role, Object key);

    /**
     * Called when an property-entry is changed for a role.
     * 
     * @param role the role that changed;
     * @param key the key of the entry;
     * @param oldValue the old value associated to the key;
     * @param newValue the new value associated to the key.
     */
    void propertyChanged(Role role, Object key, Object oldValue, Object newValue);
}