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

package org.apache.felix.useradmin.impl.role;

import java.security.Permission;

import junit.framework.TestCase;


import org.apache.felix.useradmin.impl.role.GroupImpl;
import org.apache.felix.useradmin.impl.role.RoleImpl;
import org.osgi.service.useradmin.Role;

/**
 * Security-related test cases for {@link GroupImpl}. 
 */
public class GroupImplSecurityTest extends TestCase {

    private TestSecurityManager m_securityManager;
    private GroupImpl m_group;

    /**
     * Tests that with permission, the {@link GroupImpl#addMember(Role)} method can be accessed.
     */
    public void testAddMemberWithPermissionsOk() throws SecurityException {
        m_securityManager.m_allowed = true;
        
        m_group.addMember(new RoleImpl(Role.USER_ANYONE));
    }

    /**
     * Tests that without permission, the {@link GroupImpl#addMember(Role)} method can not be accessed.
     */
    public void testAddMemberWithoutPermissionsFail() throws SecurityException {
        try {
            m_group.addMember(new RoleImpl(Role.USER_ANYONE));
            
            fail("Security exception expected!");
        } catch (SecurityException e) {
            // Ok; expected 
        }
    }

    /**
     * Tests that with permission, the {@link GroupImpl#addRequiredMember(Role)} method can be accessed.
     */
    public void testAddRequiredMemberWithPermissionsOk() throws SecurityException {
        m_securityManager.m_allowed = true;
        
        m_group.addRequiredMember(new RoleImpl(Role.USER_ANYONE));
    }

    /**
     * Tests that without permission, the {@link GroupImpl#addRequiredMember(Role)} method can not be accessed.
     */
    public void testAddRequiredMemberWithoutPermissionsFail() throws SecurityException {
        try {
            m_group.addRequiredMember(new RoleImpl(Role.USER_ANYONE));
            
            fail("Security exception expected!");
        } catch (SecurityException e) {
            // Ok; expected 
        }
    }

    /**
     * Tests that no special permissions are needed to access the {@link GroupImpl#getMembers()} method.
     */
    public void testGetMembers() throws SecurityException {
        assertNull(m_group.getMembers());
    }

    /**
     * Tests that no special permissions are needed to access the {@link GroupImpl#getRequiredMembers()} method.
     */
    public void testGetRequiredMembers() throws SecurityException {
        assertNull(m_group.getRequiredMembers());
    }

    /**
     * Tests that with permission, the {@link GroupImpl#removeMember(Role)} method can be accessed.
     */
    public void testRemoveMemberWithPermissionsOk() throws SecurityException {
        m_securityManager.m_allowed = true;
        
        assertFalse(m_group.removeMember(new RoleImpl(Role.USER_ANYONE)));
    }

    /**
     * Tests that without permission, the {@link GroupImpl#removeMember(Role)} method can not be accessed.
     */
    public void testRemoveMemberWithoutPermissionsFail() throws SecurityException {
        try {
            assertFalse(m_group.removeMember(new RoleImpl(Role.USER_ANYONE)));

            fail("Security exception expected!");
        } catch (SecurityException e) {
            // Ok; expected
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        super.setUp();
        
        m_securityManager = new TestSecurityManager();
        System.setSecurityManager(m_securityManager);

        m_group = new GroupImpl("group");
    }

    /**
     * {@inheritDoc}
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        
        System.setSecurityManager(null);
    }
    
    /**
     * Provides a test security manager.
     */
    static final class TestSecurityManager extends SecurityManager {
        
        volatile boolean m_allowed = false;

        public void checkPermission(Permission perm) {
            // Cannot use instanceof as it requires a special permission as well...
            if ("UserAdminPermission".equals(perm.getClass().getSimpleName())) {
                String name = perm.getName();
                if ("admin".equals(name) && !m_allowed) {
                    throw new SecurityException("Not allowed!");
                }
            }
            // Do not check for other permissions...
        }

        public void checkPermission(Permission perm, Object context) {
            // Do not check for other permissions...
        }
    }
}
