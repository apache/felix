/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.useradmin.impl.role;

import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;

/**
 * Provides an adapter for all {@link Group}s, allowing its changes to be 
 * externally observed.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ObservableGroup extends ObservableUser implements Group {
    
    private static final long serialVersionUID = 4012536225870565500L;
    
    private static final String BASIC_MEMBER = "basicMember";
    private static final String REQUIRED_MEMBER = "requiredMember";
    
    /**
     * Creates a new {@link ObservableGroup} instance.
     * 
     * @param group the group-role to observe for changes, cannot be <code>null</code>.
     * @throws IllegalArgumentException in case the given group-role was <code>null</code>.
     */
    public ObservableGroup(Group group) {
        super(group);
    }

    public boolean addMember(Role role) {
        boolean result = ((Group) m_delegate).addMember(role);
        if (result) {
            // Notify our (optional) listener...
            entryAdded(BASIC_MEMBER, role);
        }
        return result;
    }

    public boolean addRequiredMember(Role role) {
        boolean result = ((Group) m_delegate).addRequiredMember(role);
        if (result) {
            // Notify our (optional) listener...
            entryAdded(REQUIRED_MEMBER, role);
        }
        
        return result;
    }

    public boolean removeMember(Role role) {
        // Take a snapshot of the current set of members...
        Role[] members = getRequiredMembers();
        boolean result = ((Group) m_delegate).removeMember(role);
        if (result) {
            // Notify our (optional) listener...
            String key = BASIC_MEMBER;
            for (int i = 0; (members != null) && (i < members.length); i++) {
                if (members[i].equals(role)) {
                    key = REQUIRED_MEMBER;
                    break;
                }
            }
            entryRemoved(key);
        }
        return result;
    }

    public Role[] getMembers() {
        Role[] members = ((Group) m_delegate).getMembers();
        if (members == null) {
            return null;
        }
        Role[] result = new Role[members.length];
        for (int i = 0; i < members.length; i++) {
            result[i] = ObservableRole.wrap(members[i]);
        }
        return result;
    }

    public Role[] getRequiredMembers() {
        Role[] requiredMembers = ((Group) m_delegate).getRequiredMembers();
        if (requiredMembers == null) {
            return null;
        }
        Role[] result = new Role[requiredMembers.length];
        for (int i = 0; i < requiredMembers.length; i++) {
            result[i] = ObservableRole.wrap(requiredMembers[i]);
        }
        return requiredMembers;
    }
    
    public String toString() {
        return m_delegate.toString();
    }
}
