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
import java.util.List;

import org.osgi.service.useradmin.Group;
import org.osgi.service.useradmin.Role;

/**
 * Helper class to check for implied role memberships.
 */
final class RoleChecker {

    /**
     * Verifies whether the given role is implied by the memberships of the given user.
     * 
     * @param user the user to check the roles for, cannot be <code>null</code>;
     * @param impliedRole the implied role to check for, cannot be <code>null</code>.
     * @return <code>true</code> if the given user has the implied role, <code>false</code> otherwise.
     */
    public boolean isImpliedBy(Role role, Role impliedRole) {
        if (role instanceof Group) {
            return isGroupImpliedBy((Group) role, impliedRole, new ArrayList());
        } else /* if ((role instanceof User) || (role instanceof Role)) */ {
            return isRoleImpliedBy(role, impliedRole);
        }
    }

    /**
     * Verifies whether the given group is implied by the given role.
     * 
     * @param group the group to check, cannot be <code>null</code>;
     * @param impliedRole the implied role to check for, cannot be <code>null</code>;
     * @param seenGroups a list of all seen groups, used for detecting cycles in groups, cannot be <code>null</code>.
     * @return <code>true</code> if the given group has the implied role, <code>false</code> otherwise.
     */
    private boolean isGroupImpliedBy(Group group, Role impliedRole, List seenGroups) {
        Role[] basicRoles = group.getMembers();
        Role[] requiredRoles = group.getRequiredMembers();

        boolean isImplied = true;
        
        // Check whether all required roles are implied...
        for (int i = 0; (requiredRoles != null) && isImplied && (i < requiredRoles.length); i++) {
            Role requiredRole = requiredRoles[i];
            if (seenGroups.contains(requiredRole)) {
                // Found a cycle between groups; always yield false!
                return false;
            }
            
            if (requiredRole instanceof Group) {
                seenGroups.add(requiredRole);
                isImplied = isGroupImpliedBy((Group) requiredRole, impliedRole, seenGroups);
            } else /* if ((requiredRole instanceof User) || (requiredRole instanceof Role)) */ {
                isImplied  = isRoleImpliedBy(requiredRole, impliedRole);
            }
        }

        // Required role is not implied by the given role; we can stop now...
        if (!isImplied) {
            return false;
        }

        // Ok; all required roles are implied, let's verify whether a least one basic role is implied...
        isImplied = false;

        // Check whether at least one basic role is implied...
        for (int i = 0; (basicRoles != null) && !isImplied && (i < basicRoles.length); i++) {
            Role basicRole = (Role) basicRoles[i];
            if (seenGroups.contains(basicRole)) {
                // Found a cycle between groups; always yield false!
                return false;
            }

            if (basicRole instanceof Group) {
                seenGroups.add(basicRole);
                isImplied = isGroupImpliedBy((Group) basicRole, impliedRole, seenGroups);
            } else /* if ((basicRole instanceof User) || (basicRole instanceof Role)) */ {
                isImplied = isRoleImpliedBy(basicRole, impliedRole);
            }
        }

        return isImplied;
    }

    /**
     * Verifies whether the given role is implied by the given role.
     * 
     * @param role the role to check, cannot be <code>null</code>;
     * @param impliedRole the implied role to check for, cannot be <code>null</code>;
     * @return <code>true</code> if the given role is implied by the given role, <code>false</code> otherwise.
     */
    private boolean isRoleImpliedBy(Role role, Role impliedRole) {
        return Role.USER_ANYONE.equals(role.getName()) || (impliedRole != null && impliedRole.getName().equals(role.getName()));
    }
}
