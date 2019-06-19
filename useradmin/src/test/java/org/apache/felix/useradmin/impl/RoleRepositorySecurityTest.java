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

import java.security.Permission;

import junit.framework.TestCase;

import org.osgi.service.useradmin.Role;

/**
 * Test cases for {@link RoleRepository}.
 */
public class RoleRepositorySecurityTest extends TestCase {

    private static final String TEST_USER_NAME = "testUser";
    
    private RoleRepository m_roleManager;
    private TestSecurityManager m_securityManager;

    /**
     * Tests that adding a role with the right permissions works.
     */
    public void testAddRoleWithPermissionsOk() throws SecurityException {
        m_securityManager.m_allowed = true;

        m_roleManager.addRole(TEST_USER_NAME, Role.USER);
    }

    /**
     * Tests that adding a role without the right permissions does not work.
     */
    public void testAddRoleWithoutPermissionsFails() throws SecurityException {
        try {
            m_roleManager.addRole(TEST_USER_NAME, Role.USER);
            
            fail("Expected SecurityException!");
        } catch (SecurityException e) {
            // Ok; expected...
        }
    }

    /**
     * Tests that getting roles by their name works without any permissions.
     */
    public void testGetRoleByNameOk() throws SecurityException {
        m_roleManager.getRoleByName(Role.USER_ANYONE);
    }

    /**
     * Tests that getting roles by key-value pairs works without any permissions.
     */
    public void testGetRolesWithKeyValueOk() throws SecurityException {
        m_roleManager.getRoles("key", "value");
    }

    /**
     * Tests that getting roles with filters works without any permissions.
     */
    public void testGetRolesWithFilterOk() throws Exception {
        m_roleManager.getRoles("(key=value)");
    }

    /**
     * Tests that removing a role with the right permissions works.
     */
    public void testRemoveRoleWithPermissionsOk() throws SecurityException {
        m_securityManager.m_allowed = true;
        
        m_roleManager.removeRole(TEST_USER_NAME);
    }

    /**
     * Tests that remove a role without the right permissions does not work.
     */
    public void testRemoveRoleWithoutPermissionsFails() throws SecurityException {
        try {
            m_roleManager.removeRole(TEST_USER_NAME);
            
            fail("Expected SecurityException!");
        } catch (SecurityException e) {
            // Ok; expected...
        }
    }
    
    /**
     * {@inheritDoc}
     */
    protected void setUp() throws Exception {
        super.setUp();

        m_roleManager = new RoleRepository(new MemoryRoleRepositoryStore());
        
        m_securityManager = new TestSecurityManager();
        System.setSecurityManager(m_securityManager);
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
